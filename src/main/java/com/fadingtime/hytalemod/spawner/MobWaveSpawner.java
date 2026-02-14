package com.fadingtime.hytalemod.spawner;

import com.fadingtime.hytalemod.HytaleMod;
import com.fadingtime.hytalemod.component.BossWaveComponent;
import com.fadingtime.hytalemod.component.SpawnedByMobWaveComponent;
import com.fadingtime.hytalemod.config.ConfigManager;
import com.fadingtime.hytalemod.persistence.PlayerStateStore;
import com.fadingtime.hytalemod.persistence.PlayerStateStoreManager;
import com.fadingtime.hytalemod.system.WorldUtils;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import it.unimi.dsi.fastutil.Pair;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/*
 * REFACTOR: Grouped 11 per-owner ConcurrentHashMaps + ownerLocks into a single
 * OwnerWaveState object, stored in one ConcurrentMap<UUID, OwnerWaveState>.
 *
 * WHAT CHANGED:
 *   - Deleted 11 separate ConcurrentHashMaps (waveCounts, bossActive, bossRefs,
 *     rejoinHold, rejoinHoldUntilByOwner, worldByOwner, storeByOwner, nextSpawnAt,
 *     taskGenerationByOwner, playersByOwner, participantsByOwner) + ownerLocks.
 *   - Created OwnerWaveState inner class holding all per-owner mutable fields.
 *   - clearOwnerRuntimeState() went from 10 lines to 2 — just remove the entry.
 *   - Synchronization now uses the state object itself instead of a separate lock map.
 *
 * PRINCIPLE (State Cohesion):
 *   When N maps are all keyed by the same ID and always read/written together,
 *   they should be ONE object in ONE map. Benefits:
 *     1. Can't forget to update one of N maps when adding/removing state.
 *     2. Cleanup is atomic: remove the object, not 11 individual entries.
 *     3. Self-documenting: "what state does an owner have?" is answered by one class.
 *
 * BEGINNER SMELL: "Parallel maps" — multiple ConcurrentHashMaps keyed by the same ID.
 *   This is extremely common in AI-generated code. The AI sees "I need a wave count
 *   per owner" and writes a new ConcurrentHashMap. Then "I need a boss ref per owner"
 *   — another map. Before long you have 17 maps and clearOwnerRuntimeState() has to
 *   manually remove from each one. Miss one and you have a memory leak.
 */
public class MobWaveSpawner {

    /*
     * All mutable per-owner wave state, grouped into one object.
     *
     * BEFORE: 11 separate ConcurrentHashMaps (waveCounts, bossActive, bossRefs, etc.)
     * AFTER:  1 ConcurrentHashMap<UUID, OwnerWaveState>
     *
     * WHY volatile on primitive fields:
     *   These fields are written on the world thread and read from scheduled executor
     *   threads. volatile ensures visibility across threads. AtomicInteger is used for
     *   fields that need atomic read-modify-write (increment).
     *
     * WHY this object also serves as the synchronization lock:
     *   The old code had a separate ownerLocks map just for locking. But the state
     *   object IS the thing we're protecting, so synchronizing on it directly is simpler
     *   and removes an entire map.
     */
    static final class OwnerWaveState {
        final AtomicInteger waveCount = new AtomicInteger(0);
        volatile boolean bossActive;
        volatile Ref<EntityStore> bossRef;
        volatile boolean rejoinHold;
        volatile long rejoinHoldUntil;
        volatile String worldKey;
        volatile Store<EntityStore> store;
        volatile long nextSpawnAt;
        final AtomicInteger taskGeneration = new AtomicInteger(0);
        // ConcurrentHashMap.newKeySet() — thread-safe set for concurrent iteration.
        final Set<UUID> activeMembers = ConcurrentHashMap.newKeySet();
        final Set<UUID> allParticipants = ConcurrentHashMap.newKeySet();
    }

    private final JavaPlugin plugin;
    private final ComponentType<EntityStore, SpawnedByMobWaveComponent> spawnedMarkerType;
    private final ComponentType<EntityStore, BossWaveComponent> bossMarkerType;
    private final PlayerStateStoreManager stateStoreManager;
    private final ConcurrentMap<UUID, ScheduledFuture<?>> spawnTasks = new ConcurrentHashMap<>();
    private final List<String> spawnableRoles = new ArrayList<>();
    private final Set<String> trackedWaveRoles = new HashSet<>();
    // 11 per-owner maps + ownerLocks → 1 map of state objects.
    private final ConcurrentMap<UUID, OwnerWaveState> ownerStates = new ConcurrentHashMap<>();
    // Per-player lookups — these have independent lifecycles so they stay as simple maps.
    private final ConcurrentMap<UUID, Ref<EntityStore>> playerRefs = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, UUID> ownerByPlayer = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Long> disconnectHandledAt = new ConcurrentHashMap<>();

    public MobWaveSpawner(@Nonnull JavaPlugin plugin, @Nonnull ComponentType<EntityStore, SpawnedByMobWaveComponent> spawnedMarkerType, @Nonnull ComponentType<EntityStore, BossWaveComponent> bossMarkerType) {
        this.plugin = plugin;
        this.spawnedMarkerType = spawnedMarkerType;
        this.bossMarkerType = bossMarkerType;
        this.stateStoreManager = HytaleMod.getInstance().getStateStoreManager();
        ConfigManager.SpawnConfig cfg = ConfigManager.get();
        this.spawnableRoles.addAll(cfg.hostileRoles);
        this.trackedWaveRoles.addAll(this.spawnableRoles);
        for (ConfigManager.BossDefinition definition : cfg.bosses) {
            this.trackedWaveRoles.add(definition.role());
        }
        plugin.getEventRegistry().registerGlobal(PlayerReadyEvent.class, this::onPlayerReady);
        plugin.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, this::onPlayerDisconnect);
    }

    public void shutdown() {
        this.spawnTasks.values().forEach(task -> task.cancel(false));
        this.spawnTasks.clear();
    }

    private void onPlayerReady(@Nonnull PlayerReadyEvent event) {
        Ref playerRef = event.getPlayerRef();
        if (playerRef == null || !playerRef.isValid()) {
            return;
        }
        Store store = playerRef.getStore();
        if (store == null) {
            return;
        }
        World world = ((EntityStore)store.getExternalData()).getWorld();
        if (world == null || !world.isTicking()) {
            return;
        }
        try {
            world.execute(() -> {
                if (!playerRef.isValid()) {
                    return;
                }
                Store worldStore = playerRef.getStore();
                if (worldStore == null) {
                    return;
                }
                PlayerRef playerRefComponent = (PlayerRef)worldStore.getComponent(playerRef, PlayerRef.getComponentType());
                if (playerRefComponent == null) {
                    return;
                }
                UUID playerId = playerRefComponent.getUuid();
                this.disconnectHandledAt.remove(playerId);
                this.playerRefs.put(playerId, (Ref<EntityStore>)playerRef);
                String worldKey = WorldUtils.getWorldKey((Store<EntityStore>)worldStore);
                UUID ownerId = MobWaveSpawner.getSharedOwnerId(worldKey);
                this.assignPlayerToOwner(playerId, ownerId);
                // Synchronize on the state object itself — no separate lock map needed.
                OwnerWaveState ownerState = this.getOrCreateOwnerState(ownerId);
                ConfigManager.SpawnConfig cfg = ConfigManager.get();
                synchronized (ownerState) {
                    ownerState.worldKey = worldKey;
                    ownerState.store = (Store<EntityStore>)worldStore;
                    ownerState.rejoinHold = true;
                    ownerState.rejoinHoldUntil = System.currentTimeMillis() + cfg.rejoinHoldMaxMs;
                }
                PlayerStateStore stateStore = this.stateStoreManager.getStore(worldKey);
                int savedWave = Math.max(0, stateStore.loadWaveCount(ownerId));
                ownerState.waveCount.set(savedWave);
                UUID savedBossId = stateStore.loadBossId(ownerId);
                long savedNextSpawn = stateStore.loadNextSpawnAt(ownerId);
                long now = System.currentTimeMillis();
                if (savedNextSpawn <= 0L) {
                    savedNextSpawn = now + cfg.spawnIntervalMs;
                }
                long spawnGraceMs = Math.max(cfg.minSpawnGraceMs, Math.min(cfg.maxSpawnGraceMs, cfg.spawnIntervalMs / 2L));
                long minNextSpawn = now + spawnGraceMs;
                if (savedNextSpawn < minNextSpawn) {
                    savedNextSpawn = minNextSpawn;
                }
                stateStore.saveNextSpawnAt(ownerId, savedNextSpawn);
                ownerState.nextSpawnAt = savedNextSpawn;
                if (savedWave <= 0 && savedBossId == null) {
                    synchronized (ownerState) {
                        ownerState.bossActive = false;
                        ownerState.bossRef = null;
                        ownerState.rejoinHold = false;
                        ownerState.rejoinHoldUntil = 0L;
                    }
                    return;
                }
                if (savedWave <= 0) {
                    this.reattachNearbyWaveMarkers((Store<EntityStore>)worldStore, (Ref<EntityStore>)playerRef);
                    this.despawnTrackedWaveEntities((Store<EntityStore>)worldStore);
                    stateStore.saveBossId(ownerId, null);
                    synchronized (ownerState) {
                        ownerState.bossActive = false;
                        ownerState.bossRef = null;
                        ownerState.rejoinHold = false;
                        ownerState.rejoinHoldUntil = 0L;
                    }
                    return;
                }
                this.reattachNearbyWaveMarkers((Store<EntityStore>)worldStore, (Ref<EntityStore>)playerRef);
                HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
                    try {
                        world.execute(() -> {
                            if (!playerRef.isValid()) {
                                return;
                            }
                            Store delayedStore = playerRef.getStore();
                            if (delayedStore == null) {
                                return;
                            }
                            this.reattachNearbyWaveMarkers((Store<EntityStore>)delayedStore, (Ref<EntityStore>)playerRef);
                        });
                    }
                    catch (IllegalThreadStateException exception) {
                        HytaleMod.LOGGER.log(Level.FINE, "Skipped delayed marker reattach because world thread state changed.", exception);
                    }
                }, cfg.markerReattachDelayMs, TimeUnit.MILLISECONDS);
                this.restoreBossFromSave(ownerId, (Store<EntityStore>)worldStore, stateStore);
                this.syncBossState(ownerId, (Store<EntityStore>)worldStore);
                if (!this.isOwnerTaskRunning(ownerId)) {
                    this.scheduleForOwner(ownerId);
                }
            });
        }
        catch (IllegalThreadStateException exception) {
            HytaleMod.LOGGER.log(Level.FINE, "Skipped player-ready world execute because world thread state changed.", exception);
        }
    }

    private void onPlayerDisconnect(@Nonnull PlayerDisconnectEvent event) {
        PlayerRef playerRefComponent = event.getPlayerRef();
        if (playerRefComponent == null) {
            return;
        }
        UUID playerId = playerRefComponent.getUuid();
        long now = System.currentTimeMillis();
        Long previousHandledAt = (Long)this.disconnectHandledAt.put(playerId, now);
        if (previousHandledAt != null && now - previousHandledAt.longValue() < ConfigManager.get().disconnectDedupMs) {
            return;
        }
        this.playerRefs.remove(playerId);
        UUID ownerId = (UUID)this.ownerByPlayer.remove(playerId);
        if (ownerId == null) {
            if (HytaleMod.getInstance().getBossHudSystem() != null) {
                HytaleMod.getInstance().getBossHudSystem().clearBossHud(playerId);
            }
            return;
        }
        boolean ownerEmpty = this.removePlayerFromOwner(ownerId, playerId);
        if (ownerEmpty) {
            OwnerWaveState ownerState = this.getOwnerState(ownerId);
            this.resetOwnerProgress(ownerId, ownerState != null ? ownerState.store : null);
            if (HytaleMod.getInstance().getBossHudSystem() != null) {
                HytaleMod.getInstance().getBossHudSystem().clearBossHud(ownerId);
            }
            return;
        }
        if (HytaleMod.getInstance().getBossHudSystem() != null) {
            HytaleMod.getInstance().getBossHudSystem().clearBossHud(playerId);
        }
    }

    private int scheduleForOwner(UUID ownerId) {
        ScheduledFuture<?> runningTask = this.spawnTasks.get(ownerId);
        OwnerWaveState ownerState = this.getOrCreateOwnerState(ownerId);
        if (runningTask != null && !runningTask.isCancelled() && !runningTask.isDone()) {
            int existingGeneration = ownerState.taskGeneration.get();
            if (existingGeneration <= 0) {
                ownerState.taskGeneration.compareAndSet(0, 1);
                existingGeneration = ownerState.taskGeneration.get();
            }
            return existingGeneration;
        }
        if (runningTask != null) {
            this.spawnTasks.remove(ownerId, runningTask);
        }
        long now = System.currentTimeMillis();
        ConfigManager.SpawnConfig cfg = ConfigManager.get();
        long target = ownerState.nextSpawnAt > 0 ? ownerState.nextSpawnAt : now + cfg.spawnIntervalMs;
        long initialDelay = Math.max(0L, target - now);
        int generation = ownerState.taskGeneration.incrementAndGet();
        ScheduledFuture<?> task = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(() -> this.spawnWaveSafe(ownerId, generation), initialDelay, cfg.spawnIntervalMs, TimeUnit.MILLISECONDS);
        ScheduledFuture<?> existing = this.spawnTasks.putIfAbsent(ownerId, task);
        if (existing != null) {
            task.cancel(false);
            return ownerState.taskGeneration.get();
        }
        return generation;
    }

    private void cancelTask(UUID ownerId) {
        ScheduledFuture task = this.spawnTasks.remove(ownerId);
        if (task != null) {
            task.cancel(false);
        }
        OwnerWaveState ownerState = this.getOwnerState(ownerId);
        if (ownerState != null) {
            ownerState.taskGeneration.incrementAndGet();
        }
    }

    private boolean isOwnerTaskRunning(@Nonnull UUID ownerId) {
        ScheduledFuture<?> task = (ScheduledFuture)this.spawnTasks.get(ownerId);
        return task != null && !task.isCancelled() && !task.isDone();
    }

    private void spawnWaveSafe(UUID ownerId, int generation) {
        if (this.getTaskGeneration(ownerId) != generation) {
            return;
        }
        try {
            this.spawnWave(ownerId, generation);
        }
        catch (Exception e) {
            ((HytaleLogger.Api)this.plugin.getLogger().at(Level.WARNING).withCause((Throwable)e)).log("Mob spawn tick failed for shared owner %s", (Object)ownerId);
        }
    }

    private void spawnWave(UUID ownerId, int generation) {
        if (this.getTaskGeneration(ownerId) != generation) {
            return;
        }
        Ref<EntityStore> playerRef = this.getAnyActivePlayerRef(ownerId);
        if (playerRef == null || !playerRef.isValid()) {
            this.clearOwnerRuntimeState(ownerId);
            return;
        }
        Store store = playerRef.getStore();
        if (store == null) {
            this.clearOwnerRuntimeState(ownerId);
            return;
        }
        World world = ((EntityStore)store.getExternalData()).getWorld();
        world.execute(() -> {
            if (this.getTaskGeneration(ownerId) != generation) {
                return;
            }
            Ref<EntityStore> activeRef = this.getAnyActivePlayerRef(ownerId);
            if (activeRef == null || !activeRef.isValid()) {
                this.clearOwnerRuntimeState(ownerId);
                return;
            }
            Store worldStore = activeRef.getStore();
            if (worldStore == null) {
                this.clearOwnerRuntimeState(ownerId);
                return;
            }
            OwnerWaveState ownerState = this.getOwnerState(ownerId);
            if (ownerState == null) {
                this.clearOwnerRuntimeState(ownerId);
                return;
            }
            if (ownerState.bossActive) {
                if (this.isBossAlive(ownerId, (Store<EntityStore>)worldStore)) {
                    return;
                }
                ownerState.bossActive = false;
                ownerState.bossRef = null;
            }
            if (ownerState.rejoinHold) {
                long holdUntil = ownerState.rejoinHoldUntil;
                long now = System.currentTimeMillis();
                if (this.isBossAlive(ownerId, (Store<EntityStore>)worldStore) || this.hasActiveWaveMobs((Store<EntityStore>)worldStore)) {
                    if (holdUntil <= 0L || now < holdUntil) {
                        return;
                    }
                }
                ownerState.rejoinHold = false;
                ownerState.rejoinHoldUntil = 0L;
                return;
            }
            TransformComponent transform = (TransformComponent)worldStore.getComponent(activeRef, TransformComponent.getComponentType());
            if (transform == null) {
                return;
            }
            List<String> roles = this.getSpawnableRoles();
            if (roles.isEmpty()) {
                return;
            }
            Vector3d playerPos = transform.getPosition();
            ThreadLocalRandom random = ThreadLocalRandom.current();
            int wave = ownerState.waveCount.incrementAndGet();
            PlayerStateStore stateStore = this.stateStoreManager.getStore(WorldUtils.getWorldKey((Store<EntityStore>)worldStore));
            stateStore.saveWaveCount(ownerId, wave);
            ConfigManager.SpawnConfig cfg = ConfigManager.get();
            long nextAt = System.currentTimeMillis() + cfg.spawnIntervalMs;
            ownerState.nextSpawnAt = nextAt;
            stateStore.saveNextSpawnAt(ownerId, nextAt);
            ConfigManager.BossDefinition bossDefinition = this.findBossForWave(cfg, wave);
            if (bossDefinition != null) {
                this.spawnConfiguredBossWave(ownerId, (Store<EntityStore>)worldStore, world, playerPos, random, wave, bossDefinition);
                return;
            }
            int spawnCount = cfg.spawnCount + cfg.spawnCountPerWave * Math.max(0, wave - 1);
            spawnCount = Math.min(spawnCount, cfg.maxMobsPerWave);
            for (int i = 0; i < spawnCount; ++i) {
                Vector3d spawnPos = this.randomSpawnPosition(world, playerPos, random);
                Vector3f rotation = new Vector3f(0.0f, (float)(random.nextDouble() * Math.PI * 2.0), 0.0f);
                String role = roles.get(random.nextInt(roles.size()));
                Pair spawned = NPCPlugin.get().spawnNPC(worldStore, role, null, spawnPos, rotation);
                if (spawned == null || spawned.first() == null || !((Ref)spawned.first()).isValid()) continue;
                worldStore.addComponent((Ref)spawned.first(), this.spawnedMarkerType, new SpawnedByMobWaveComponent());
                this.applyWaveHealthBonus((Ref<EntityStore>)((Ref)spawned.first()), (Store<EntityStore>)worldStore, wave);
                this.capMobHealth((Ref<EntityStore>)((Ref)spawned.first()), (Store<EntityStore>)worldStore);
            }
        });
    }

    private void spawnConfiguredBossWave(
        UUID ownerId,
        Store<EntityStore> store,
        World world,
        Vector3d playerPos,
        ThreadLocalRandom random,
        int wave,
        ConfigManager.BossDefinition bossDefinition
    ) {
        Vector3d spawnPos = this.randomSpawnPosition(world, playerPos, random);
        Vector3f rotation = new Vector3f(0.0f, (float)(random.nextDouble() * Math.PI * 2.0), 0.0f);
        Pair spawned = NPCPlugin.get().spawnNPC(store, bossDefinition.role(), null, spawnPos, rotation);
        if (spawned == null || spawned.first() == null || !((Ref)spawned.first()).isValid()) {
            return;
        }
        Ref bossRef = (Ref)spawned.first();
        store.addComponent(bossRef, this.spawnedMarkerType, new SpawnedByMobWaveComponent());
        store.addComponent(bossRef, this.bossMarkerType, new BossWaveComponent(ownerId));
        OwnerWaveState ownerState = this.getOrCreateOwnerState(ownerId);
        ownerState.bossActive = true;
        ownerState.bossRef = (Ref<EntityStore>)bossRef;
        PlayerStateStore stateStore = this.stateStoreManager.getStore(WorldUtils.getWorldKey(store));
        UUIDComponent uuidComponent = (UUIDComponent)store.getComponent(bossRef, UUIDComponent.getComponentType());
        if (uuidComponent != null) {
            stateStore.saveBossId(ownerId, uuidComponent.getUuid());
        }
        float targetHealth = bossDefinition.getTargetHealth(wave);
        this.applyFixedBossHealth((Ref<EntityStore>)bossRef, store, targetHealth, bossDefinition.healthModifierId());
        this.applyNpcScale((Ref<EntityStore>)bossRef, store, world, 0, bossDefinition.scale());
    }

    private void applyNpcScale(Ref<EntityStore> npcRef, Store<EntityStore> store, World world, int attempt, float scale) {
        if (!npcRef.isValid()) {
            return;
        }
        NPCEntity npc = (NPCEntity)store.getComponent(npcRef, NPCEntity.getComponentType());
        if (npc == null || npc.getRole() == null) {
            this.retryNpcScale(npcRef, store, world, attempt, scale);
            return;
        }
        String appearance = npc.getRole().getAppearanceName();
        if (appearance == null || appearance.isEmpty()) {
            this.retryNpcScale(npcRef, store, world, attempt, scale);
            return;
        }
        ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(appearance);
        if (modelAsset == null) {
            this.retryNpcScale(npcRef, store, world, attempt, scale);
            return;
        }
        npc.setInitialModelScale(scale);
        npc.setAppearance(npcRef, modelAsset, store);
    }

    // DELETED: isGameplayWorld() — always returned true. See PlayerProgressionManager
    // for the same deletion and the teaching explanation about feature flag stubs.

    private void reattachNearbyWaveMarkers(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> playerRef) {
        TransformComponent playerTransform = (TransformComponent)store.getComponent(playerRef, TransformComponent.getComponentType());
        if (playerTransform == null) {
            return;
        }
        Vector3d playerPos = playerTransform.getPosition();
        int[] reattachedCount = new int[]{0};
        store.forEachChunk((Query)Query.and(NPCEntity.getComponentType(), TransformComponent.getComponentType()), (chunk, buffer) -> {
            int size = chunk.size();
            for (int i = 0; i < size; ++i) {
                Ref ref = chunk.getReferenceTo(i);
                if (ref == null || !ref.isValid() || chunk.getComponent(i, this.spawnedMarkerType) != null) continue;
                NPCEntity npc = (NPCEntity)chunk.getComponent(i, NPCEntity.getComponentType());
                TransformComponent npcTransform = (TransformComponent)chunk.getComponent(i, TransformComponent.getComponentType());
                if (npc == null || npcTransform == null) continue;
                String roleName = npc.getRole() != null ? npc.getRole().getRoleName() : npc.getRoleName();
                if (roleName == null || !this.trackedWaveRoles.contains(roleName)) continue;
                Vector3d npcPos = npcTransform.getPosition();
                double reattachRadius = ConfigManager.get().markerReattachRadius;
                if (MobWaveSpawner.distanceSquared(playerPos, npcPos) > reattachRadius * reattachRadius) continue;
                buffer.addComponent(ref, this.spawnedMarkerType, new SpawnedByMobWaveComponent());
                ++reattachedCount[0];
            }
        });
        this.plugin.getLogger().at(Level.INFO).log("Wave marker reattach scan complete for player %s: added=%d", playerRef, (Object)reattachedCount[0]);
    }

    private static double distanceSquared(@Nonnull Vector3d a, @Nonnull Vector3d b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private void retryNpcScale(Ref<EntityStore> npcRef, Store<EntityStore> store, World world, int attempt, float scale) {
        if (attempt >= 3) {
            return;
        }
        HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> world.execute(() -> this.applyNpcScale(npcRef, store, world, attempt + 1, scale)), 50L, TimeUnit.MILLISECONDS);
    }

    public void notifyBossDefeated(UUID ownerId) {
        OwnerWaveState ownerState = this.getOwnerState(ownerId);
        if (ownerState == null) {
            return;
        }
        ownerState.bossActive = false;
        ownerState.bossRef = null;
        if (ownerState.worldKey != null) {
            this.stateStoreManager.getStore(ownerState.worldKey).saveBossId(ownerId, null);
        }
    }

    private void syncBossState(@Nonnull UUID ownerId, @Nonnull Store<EntityStore> store) {
        AtomicReference<Ref<EntityStore>> foundBoss = new AtomicReference<>(null);
        store.forEachChunk((Query)Query.and(this.bossMarkerType), (chunk, buffer) -> {
            if (foundBoss.get() != null) {
                return;
            }
            int size = chunk.size();
            for (int i = 0; i < size; ++i) {
                Ref bossRef;
                BossWaveComponent bossComponent = (BossWaveComponent)chunk.getComponent(i, this.bossMarkerType);
                if (bossComponent == null || !ownerId.equals(bossComponent.getOwnerId()) || (bossRef = chunk.getReferenceTo(i)) == null || !bossRef.isValid()) continue;
                foundBoss.set(bossRef);
                return;
            }
        });
        Ref<EntityStore> bossRef = foundBoss.get();
        OwnerWaveState ownerState = this.getOrCreateOwnerState(ownerId);
        if (bossRef != null && bossRef.isValid()) {
            ownerState.bossActive = true;
            ownerState.bossRef = bossRef;
        } else {
            ownerState.bossActive = false;
            ownerState.bossRef = null;
        }
    }

    private void restoreBossFromSave(@Nonnull UUID ownerId, @Nonnull Store<EntityStore> store, @Nonnull PlayerStateStore stateStore) {
        UUID bossId = stateStore.loadBossId(ownerId);
        if (bossId == null) {
            return;
        }
        World world = ((EntityStore)store.getExternalData()).getWorld();
        if (world == null) {
            return;
        }
        Ref bossRef = world.getEntityRef(bossId);
        if (bossRef == null || !bossRef.isValid()) {
            stateStore.saveBossId(ownerId, null);
            return;
        }
        world.execute(() -> {
            Store worldStore = bossRef.getStore();
            if (worldStore == null || !bossRef.isValid()) {
                stateStore.saveBossId(ownerId, null);
                return;
            }
            if (worldStore.getComponent(bossRef, this.bossMarkerType) == null) {
                worldStore.addComponent(bossRef, this.bossMarkerType, new BossWaveComponent(ownerId));
            }
            if (worldStore.getComponent(bossRef, this.spawnedMarkerType) == null) {
                worldStore.addComponent(bossRef, this.spawnedMarkerType, new SpawnedByMobWaveComponent());
            }
            OwnerWaveState ownerState = this.getOrCreateOwnerState(ownerId);
            ownerState.bossActive = true;
            ownerState.bossRef = (Ref<EntityStore>)bossRef;
        });
    }

    private boolean hasActiveWaveMobs(@Nonnull Store<EntityStore> store) {
        AtomicBoolean found = new AtomicBoolean(false);
        store.forEachChunk((Query)Query.and(this.spawnedMarkerType), (chunk, buffer) -> {
            if (found.get()) {
                return;
            }
            int size = chunk.size();
            for (int i = 0; i < size; ++i) {
                Ref ref = chunk.getReferenceTo(i);
                if (ref == null || !ref.isValid()) continue;
                found.set(true);
                return;
            }
        });
        return found.get();
    }

    @Nullable
    public Ref<EntityStore> getPlayerRef(@Nonnull UUID playerOrOwnerId) {
        Ref<EntityStore> directRef = (Ref)this.playerRefs.get(playerOrOwnerId);
        if (directRef != null && directRef.isValid()) {
            return directRef;
        }
        UUID ownerId = (UUID)this.ownerByPlayer.get(playerOrOwnerId);
        if (ownerId != null) {
            return this.getAnyActivePlayerRef(ownerId);
        }
        return this.getAnyActivePlayerRef(playerOrOwnerId);
    }

    @Nonnull
    public List<Ref<EntityStore>> getPlayerRefsForOwner(@Nonnull UUID ownerId) {
        ArrayList<Ref<EntityStore>> refs = new ArrayList<>();
        OwnerWaveState ownerState = this.getOwnerState(ownerId);
        if (ownerState == null) {
            return refs;
        }
        Set<UUID> members = ownerState.activeMembers;
        if (members.isEmpty()) {
            return refs;
        }
        ArrayList<UUID> staleMembers = null;
        for (UUID memberId : members) {
            Ref<EntityStore> memberRef = this.playerRefs.get(memberId);
            if (memberRef == null || !memberRef.isValid()) {
                if (staleMembers == null) {
                    staleMembers = new ArrayList<>();
                }
                staleMembers.add(memberId);
                continue;
            }
            Store<EntityStore> memberStore = memberRef.getStore();
            if (memberStore == null) {
                continue;
            }
            refs.add(memberRef);
        }
        if (staleMembers != null) {
            for (UUID staleId : staleMembers) {
                members.remove(staleId);
                this.playerRefs.remove(staleId);
                this.ownerByPlayer.remove(staleId, ownerId);
            }
        }
        return refs;
    }

    @Nonnull
    public List<Ref<EntityStore>> getPlayerRefs(@Nonnull UUID playerOrOwnerId) {
        ArrayList<Ref<EntityStore>> refs = new ArrayList<Ref<EntityStore>>();
        if (this.isTrackedPlayer(playerOrOwnerId)) {
            Ref<EntityStore> playerRef = (Ref)this.playerRefs.get(playerOrOwnerId);
            if (playerRef != null && playerRef.isValid()) {
                refs.add(playerRef);
            }
            return refs;
        }
        UUID ownerId = this.resolveOwnerId(playerOrOwnerId);
        refs.addAll(this.getPlayerRefsForOwner(ownerId));
        return refs;
    }

    public boolean isTrackedPlayer(@Nonnull UUID playerId) {
        return this.playerRefs.containsKey(playerId) || this.ownerByPlayer.containsKey(playerId);
    }

    @Nonnull
    public UUID resolveOwnerId(@Nonnull UUID playerOrOwnerId) {
        UUID ownerId = (UUID)this.ownerByPlayer.get(playerOrOwnerId);
        return ownerId != null ? ownerId : playerOrOwnerId;
    }

    public void startWavesForPlayer(@Nonnull Ref<EntityStore> playerRef) {
        if (!playerRef.isValid()) {
            return;
        }
        Store<EntityStore> store = playerRef.getStore();
        if (store == null) {
            return;
        }
        World world = ((EntityStore)store.getExternalData()).getWorld();
        if (world == null || !world.isTicking()) {
            return;
        }
        try {
            world.execute(() -> {
                Store<EntityStore> worldStore;
                UUID ownerId;
                PlayerRef playerRefComponent;
                if (!playerRef.isValid()) {
                    return;
                }
                worldStore = playerRef.getStore();
                if (worldStore == null) {
                    return;
                }
                playerRefComponent = (PlayerRef)worldStore.getComponent(playerRef, PlayerRef.getComponentType());
                if (playerRefComponent == null) {
                    return;
                }
                UUID playerId = playerRefComponent.getUuid();
                String worldKey = WorldUtils.getWorldKey(worldStore);
                ownerId = MobWaveSpawner.getSharedOwnerId(worldKey);
                this.playerRefs.put(playerId, playerRef);
                this.assignPlayerToOwner(playerId, ownerId);
                OwnerWaveState ownerState = this.getOrCreateOwnerState(ownerId);
                synchronized (ownerState) {
                    ownerState.worldKey = worldKey;
                    ownerState.store = worldStore;
                }
                PlayerStateStore stateStore = this.stateStoreManager.getStore(worldKey);
                this.cancelTask(ownerId);
                ownerState.waveCount.set(0);
                stateStore.saveWaveCount(ownerId, 0);
                stateStore.saveBossId(ownerId, null);
                ownerState.bossActive = false;
                ownerState.bossRef = null;
                this.despawnTrackedWaveEntities(worldStore);
                ownerState.rejoinHold = false;
                ownerState.rejoinHoldUntil = 0L;
                long now = System.currentTimeMillis();
                long nextAt = now + ConfigManager.get().spawnIntervalMs;
                ownerState.nextSpawnAt = nextAt;
                stateStore.saveNextSpawnAt(ownerId, nextAt);
                int generation = this.scheduleForOwner(ownerId);
                this.spawnWave(ownerId, generation);
            });
        }
        catch (IllegalThreadStateException exception) {
            HytaleMod.LOGGER.log(Level.FINE, "Skipped startNowForPlayer world execute because world thread state changed.", exception);
        }
    }

    // DELETED: getWorldKey() — moved to WorldUtils.getWorldKey() to eliminate
    // duplication with PlayerProgressionManager's identical copy.

    private List<String> getSpawnableRoles() {
        if (this.spawnableRoles.isEmpty()) {
            this.plugin.getLogger().at(Level.WARNING).log("No spawnable NPC roles configured; mobs will not spawn.");
        }
        return this.spawnableRoles;
    }

    private Vector3d randomSpawnPosition(World world, Vector3d origin, ThreadLocalRandom random) {
        ConfigManager.SpawnConfig cfg = ConfigManager.get();
        double minRadius = cfg.minSpawnRadius;
        double maxRadius = Math.max(minRadius, cfg.maxSpawnRadius);
        double radius = minRadius + random.nextDouble() * (maxRadius - minRadius);
        double angle = random.nextDouble() * Math.PI * 2.0;
        double x = origin.getX() + Math.cos(angle) * radius;
        double z = origin.getZ() + Math.sin(angle) * radius;
        double y = origin.getY() + cfg.spawnYOffset;
        return new Vector3d(x, y, z);
    }

    private boolean isBossAlive(UUID ownerId, Store<EntityStore> store) {
        OwnerWaveState ownerState = this.getOwnerState(ownerId);
        if (ownerState == null) {
            return false;
        }
        Ref<EntityStore> bossRef = ownerState.bossRef;
        if (bossRef == null || !bossRef.isValid()) {
            return false;
        }
        NPCEntity npc = (NPCEntity)store.getComponent(bossRef, NPCEntity.getComponentType());
        return npc != null;
    }

    @Nonnull
    private static UUID getSharedOwnerId(@Nonnull String worldKey) {
        return UUID.nameUUIDFromBytes(("shared-wave-owner:" + worldKey).getBytes(StandardCharsets.UTF_8));
    }

    @Nonnull
    private OwnerWaveState getOrCreateOwnerState(@Nonnull UUID ownerId) {
        return this.ownerStates.computeIfAbsent(ownerId, id -> new OwnerWaveState());
    }

    @Nullable
    private OwnerWaveState getOwnerState(@Nonnull UUID ownerId) {
        return this.ownerStates.get(ownerId);
    }

    private int getTaskGeneration(@Nonnull UUID ownerId) {
        OwnerWaveState state = this.ownerStates.get(ownerId);
        return state != null ? state.taskGeneration.get() : 0;
    }

    private void assignPlayerToOwner(@Nonnull UUID playerId, @Nonnull UUID ownerId) {
        UUID previousOwnerId = (UUID)this.ownerByPlayer.put(playerId, ownerId);
        if (previousOwnerId != null && !previousOwnerId.equals(ownerId) && this.removePlayerFromOwner(previousOwnerId, playerId)) {
            this.clearOwnerRuntimeState(previousOwnerId);
        }
        OwnerWaveState ownerState = this.getOrCreateOwnerState(ownerId);
        // Synchronized to atomically add to both sets — prevents a window where
        // a player appears in activeMembers but not allParticipants.
        synchronized (ownerState) {
            ownerState.activeMembers.add(playerId);
            ownerState.allParticipants.add(playerId);
        }
    }

    private boolean removePlayerFromOwner(@Nonnull UUID ownerId, @Nonnull UUID playerId) {
        OwnerWaveState ownerState = this.getOwnerState(ownerId);
        if (ownerState == null) {
            return true;
        }
        synchronized (ownerState) {
            ownerState.activeMembers.remove(playerId);
            return ownerState.activeMembers.isEmpty();
        }
    }

    // BEFORE: 10 lines manually removing ownerId from 9 separate maps inside a
    //         synchronized block, then removing the lock itself.
    // AFTER:  2 lines. Remove the state object and everything goes with it.
    //
    // THIS is the payoff of grouping state into one object. You can't forget a
    // map, you can't leave stale entries, and you can't introduce a memory leak
    // by adding a 12th map and forgetting to update this method.
    private void clearOwnerRuntimeState(@Nonnull UUID ownerId) {
        this.cancelTask(ownerId);
        this.ownerStates.remove(ownerId);
    }

    private void resetOwnerProgress(@Nonnull UUID ownerId, @Nullable Store<EntityStore> store) {
        String worldKey = store != null ? WorldUtils.getWorldKey(store) : null;
        Ref<EntityStore> bossRef = null;
        Set<UUID> participants = null;
        OwnerWaveState ownerState = this.getOwnerState(ownerId);
        if (ownerState != null) {
            synchronized (ownerState) {
                if (worldKey == null) {
                    worldKey = ownerState.worldKey;
                }
                // Snapshot participants before cleanup discards the state.
                if (!ownerState.allParticipants.isEmpty()) {
                    participants = new HashSet<>(ownerState.allParticipants);
                }
                if (store == null) {
                    bossRef = ownerState.bossRef;
                }
            }
        }
        if (worldKey != null && participants != null && !participants.isEmpty()) {
            if (HytaleMod.getInstance().getLifeEssenceLevelSystem() != null) {
                HytaleMod.getInstance().getLifeEssenceLevelSystem().resetProgressForPlayers(participants, worldKey);
            }
        }
        if (worldKey != null) {
            PlayerStateStore stateStore = this.stateStoreManager.getStore(worldKey);
            stateStore.saveWaveCount(ownerId, 0);
            stateStore.saveNextSpawnAt(ownerId, 0L);
            stateStore.saveBossId(ownerId, null);
        }
        if (ownerState != null) {
            ownerState.nextSpawnAt = 0L;
        }
        if (store == null && bossRef != null && bossRef.isValid()) {
            store = bossRef.getStore();
        }
        if (store != null) {
            World world = ((EntityStore)store.getExternalData()).getWorld();
            if (world != null && world.isTicking()) {
                Store<EntityStore> finalStore = store;
                try {
                    world.execute(() -> this.despawnTrackedWaveEntities(finalStore));
                }
                catch (IllegalThreadStateException exception) {
                    HytaleMod.LOGGER.log(Level.FINE, "Skipped despawn on reset because world thread state changed.", exception);
                }
            }
        }
        this.clearOwnerRuntimeState(ownerId);
    }

    private void despawnTrackedWaveEntities(@Nonnull Store<EntityStore> store) {
        store.forEachChunk((Query)Query.and(this.spawnedMarkerType), (chunk, buffer) -> {
            int size = chunk.size();
            for (int i = 0; i < size; ++i) {
                Ref<EntityStore> ref = chunk.getReferenceTo(i);
                if (ref == null || !ref.isValid()) continue;
                buffer.removeEntity(ref, RemoveReason.REMOVE);
            }
        });
    }

    @Nullable
    private Ref<EntityStore> getAnyActivePlayerRef(@Nonnull UUID ownerId) {
        OwnerWaveState ownerState = this.getOwnerState(ownerId);
        if (ownerState == null) {
            return null;
        }
        synchronized (ownerState) {
            Set<UUID> members = ownerState.activeMembers;
            if (members.isEmpty()) {
                return null;
            }
            ArrayList<UUID> staleMembers = null;
            for (UUID memberId : members) {
                Ref<EntityStore> memberRef = this.playerRefs.get(memberId);
                if (memberRef == null || !memberRef.isValid()) {
                    if (staleMembers == null) {
                        staleMembers = new ArrayList<>();
                    }
                    staleMembers.add(memberId);
                    continue;
                }
                Store<EntityStore> memberStore = memberRef.getStore();
                if (memberStore == null) {
                    continue;
                }
                if (staleMembers != null) {
                    for (UUID staleId : staleMembers) {
                        members.remove(staleId);
                        this.playerRefs.remove(staleId);
                        this.ownerByPlayer.remove(staleId, ownerId);
                    }
                }
                return memberRef;
            }
            if (staleMembers != null) {
                for (UUID staleId : staleMembers) {
                    members.remove(staleId);
                    this.playerRefs.remove(staleId);
                    this.ownerByPlayer.remove(staleId, ownerId);
                }
            }
            return null;
        }
    }

    private void applyWaveHealthBonus(@Nonnull Ref<EntityStore> npcRef, @Nonnull Store<EntityStore> store, int wave) {
        if (wave <= 1) {
            return;
        }
        float perWaveBonus = ConfigManager.get().waveHealthBonus;
        if (perWaveBonus <= 0.0f) {
            return;
        }
        float bonus = perWaveBonus * (float)(wave - 1);
        EntityStatMap statMap = (EntityStatMap)store.getComponent(npcRef, EntityStatMap.getComponentType());
        if (statMap == null) {
            return;
        }
        statMap.putModifier(DefaultEntityStatTypes.getHealth(), "WaveHealth", (Modifier)new StaticModifier(Modifier.ModifierTarget.MAX, StaticModifier.CalculationType.ADDITIVE, bonus));
        statMap.maximizeStatValue(DefaultEntityStatTypes.getHealth());
    }

    private void applyFixedBossHealth(@Nonnull Ref<EntityStore> npcRef, @Nonnull Store<EntityStore> store, float targetMaxHealth, @Nonnull String modifierId) {
        EntityStatMap statMap = (EntityStatMap)store.getComponent(npcRef, EntityStatMap.getComponentType());
        if (statMap == null) {
            return;
        }
        EntityStatValue healthStat = statMap.get(DefaultEntityStatTypes.getHealth());
        if (healthStat == null) {
            return;
        }
        float currentMax = healthStat.getMax();
        float delta = targetMaxHealth - currentMax;
        if (Math.abs(delta) < 0.01f) {
            return;
        }
        statMap.putModifier(DefaultEntityStatTypes.getHealth(), modifierId, (Modifier)new StaticModifier(Modifier.ModifierTarget.MAX, StaticModifier.CalculationType.ADDITIVE, delta));
        statMap.maximizeStatValue(DefaultEntityStatTypes.getHealth());
    }

    private void capMobHealth(@Nonnull Ref<EntityStore> npcRef, @Nonnull Store<EntityStore> store) {
        this.applyFixedBossHealth(npcRef, store, ConfigManager.get().maxMobHealth, "WaveHealthCap");
    }

    @Nullable
    private ConfigManager.BossDefinition findBossForWave(@Nonnull ConfigManager.SpawnConfig config, int wave) {
        for (ConfigManager.BossDefinition definition : config.bosses) {
            if (definition.shouldSpawnAtWave(wave)) {
                return definition;
            }
        }
        return null;
    }
}
