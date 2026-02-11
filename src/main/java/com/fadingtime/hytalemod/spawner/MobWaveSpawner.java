package com.fadingtime.hytalemod.spawner;

import com.fadingtime.hytalemod.HytaleMod;
import com.fadingtime.hytalemod.component.BossWaveComponent;
import com.fadingtime.hytalemod.component.SpawnedByMobWaveComponent;
import com.fadingtime.hytalemod.config.MobSpawnConfig;
import com.fadingtime.hytalemod.persistence.PlayerStateStore;
import com.fadingtime.hytalemod.persistence.PlayerStateStoreManager;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MobWaveSpawner {
    private static final long REJOIN_HOLD_MAX_MS = 20000L;
    private static final double MARKER_REATTACH_RADIUS = 160.0;
    private static final double MARKER_REATTACH_RADIUS_SQ = MARKER_REATTACH_RADIUS * MARKER_REATTACH_RADIUS;
    private final JavaPlugin plugin;
    private final ComponentType<EntityStore, SpawnedByMobWaveComponent> spawnedMarkerType;
    private final ComponentType<EntityStore, BossWaveComponent> bossMarkerType;
    private final PlayerStateStoreManager stateStoreManager;
    private final ConcurrentMap<UUID, ScheduledFuture<?>> spawnTasks = new ConcurrentHashMap();
    private final List<String> spawnableRoles = new ArrayList<String>(MobSpawnConfig.HOSTILE_ROLES);
    private final Set<String> trackedWaveRoles = new HashSet<String>();
    private final ConcurrentMap<UUID, Integer> waveCounts = new ConcurrentHashMap<UUID, Integer>();
    private final ConcurrentMap<UUID, Boolean> bossActive = new ConcurrentHashMap<UUID, Boolean>();
    private final ConcurrentMap<UUID, Ref<EntityStore>> bossRefs = new ConcurrentHashMap<UUID, Ref<EntityStore>>();
    private final ConcurrentMap<UUID, Ref<EntityStore>> playerRefs = new ConcurrentHashMap<UUID, Ref<EntityStore>>();
    private final ConcurrentMap<UUID, UUID> ownerByPlayer = new ConcurrentHashMap<UUID, UUID>();
    private final ConcurrentMap<UUID, Set<UUID>> playersByOwner = new ConcurrentHashMap<UUID, Set<UUID>>();
    private final ConcurrentMap<UUID, Set<UUID>> participantsByOwner = new ConcurrentHashMap<UUID, Set<UUID>>();
    private final ConcurrentMap<UUID, Store<EntityStore>> storeByOwner = new ConcurrentHashMap<UUID, Store<EntityStore>>();
    private final ConcurrentMap<UUID, Boolean> rejoinHold = new ConcurrentHashMap<UUID, Boolean>();
    private final ConcurrentMap<UUID, String> worldByOwner = new ConcurrentHashMap<UUID, String>();
    private final ConcurrentMap<UUID, Long> nextSpawnAt = new ConcurrentHashMap<UUID, Long>();
    private final ConcurrentMap<UUID, Long> disconnectHandledAt = new ConcurrentHashMap<UUID, Long>();
    private final ConcurrentMap<UUID, Long> rejoinHoldUntilByOwner = new ConcurrentHashMap<UUID, Long>();
    private final ConcurrentMap<UUID, Integer> taskGenerationByOwner = new ConcurrentHashMap<UUID, Integer>();

    public MobWaveSpawner(@Nonnull JavaPlugin plugin, @Nonnull ComponentType<EntityStore, SpawnedByMobWaveComponent> spawnedMarkerType, @Nonnull ComponentType<EntityStore, BossWaveComponent> bossMarkerType) {
        this.plugin = plugin;
        this.spawnedMarkerType = spawnedMarkerType;
        this.bossMarkerType = bossMarkerType;
        this.stateStoreManager = HytaleMod.getInstance().getStateStoreManager();
        this.trackedWaveRoles.addAll(this.spawnableRoles);
        this.trackedWaveRoles.add(MobSpawnConfig.BOSS_ROLE);
        this.trackedWaveRoles.add(MobSpawnConfig.TOAD_BOSS_ROLE);
        this.trackedWaveRoles.add(MobSpawnConfig.WRAITH_BOSS_ROLE);
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
                long minNextSpawn;
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
                if (!this.isGameplayWorld((Store<EntityStore>)worldStore)) {
                    this.playerRefs.remove(playerId);
                    UUID oldOwnerId = (UUID)this.ownerByPlayer.remove(playerId);
                    if (oldOwnerId != null && this.removePlayerFromOwner(oldOwnerId, playerId)) {
                        this.clearOwnerRuntimeState(oldOwnerId);
                    }
                    return;
                }
                String worldKey = MobWaveSpawner.getWorldKey((Store<EntityStore>)worldStore);
                UUID ownerId = MobWaveSpawner.getSharedOwnerId(worldKey);
                UUID previousOwnerId = (UUID)this.ownerByPlayer.put(playerId, ownerId);
                if (previousOwnerId != null && !previousOwnerId.equals(ownerId) && this.removePlayerFromOwner(previousOwnerId, playerId)) {
                    this.clearOwnerRuntimeState(previousOwnerId);
                }
                ((Set)this.playersByOwner.computeIfAbsent(ownerId, id -> ConcurrentHashMap.newKeySet())).add(playerId);
                ((Set)this.participantsByOwner.computeIfAbsent(ownerId, id -> ConcurrentHashMap.newKeySet())).add(playerId);
                this.worldByOwner.put(ownerId, worldKey);
                this.storeByOwner.put(ownerId, (Store<EntityStore>)worldStore);
                this.rejoinHold.put(ownerId, true);
                this.rejoinHoldUntilByOwner.put(ownerId, System.currentTimeMillis() + REJOIN_HOLD_MAX_MS);
                PlayerStateStore stateStore = this.stateStoreManager.getStore(worldKey);
                int savedWave = Math.max(0, stateStore.loadWaveCount(ownerId));
                this.waveCounts.put(ownerId, savedWave);
                UUID savedBossId = stateStore.loadBossId(ownerId);
                long savedNextSpawn = stateStore.loadNextSpawnAt(ownerId);
                long now = System.currentTimeMillis();
                if (savedNextSpawn <= 0L) {
                    savedNextSpawn = now + 10000L;
                }
                if (savedNextSpawn < (minNextSpawn = now + 5000L)) {
                    savedNextSpawn = minNextSpawn;
                }
                stateStore.saveNextSpawnAt(ownerId, savedNextSpawn);
                this.nextSpawnAt.put(ownerId, savedNextSpawn);
                if (savedWave <= 0 && savedBossId == null) {
                    this.bossActive.remove(ownerId);
                    this.bossRefs.remove(ownerId);
                    this.rejoinHold.put(ownerId, false);
                    this.rejoinHoldUntilByOwner.remove(ownerId);
                    return;
                }
                if (savedWave <= 0) {
                    this.reattachNearbyWaveMarkers((Store<EntityStore>)worldStore, (Ref<EntityStore>)playerRef);
                    this.despawnTrackedWaveEntities((Store<EntityStore>)worldStore);
                    stateStore.saveBossId(ownerId, null);
                    this.bossActive.remove(ownerId);
                    this.bossRefs.remove(ownerId);
                    this.rejoinHold.put(ownerId, false);
                    this.rejoinHoldUntilByOwner.remove(ownerId);
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
                            if (delayedStore == null || !this.isGameplayWorld((Store<EntityStore>)delayedStore)) {
                                return;
                            }
                            this.reattachNearbyWaveMarkers((Store<EntityStore>)delayedStore, (Ref<EntityStore>)playerRef);
                        });
                    }
                    catch (IllegalThreadStateException ignored) {
                    }
                }, 1500L, TimeUnit.MILLISECONDS);
                this.restoreBossFromSave(ownerId, (Store<EntityStore>)worldStore, stateStore);
                this.syncBossState(ownerId, (Store<EntityStore>)worldStore);
                if (!this.isOwnerTaskRunning(ownerId)) {
                    this.scheduleForOwner(ownerId);
                }
            });
        }
        catch (IllegalThreadStateException ignored) {
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
        if (previousHandledAt != null && now - previousHandledAt.longValue() < 2000L) {
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
            this.resetOwnerProgress(ownerId, (Store<EntityStore>)this.storeByOwner.get(ownerId));
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
        ScheduledFuture<?> runningTask = (ScheduledFuture)this.spawnTasks.get(ownerId);
        if (runningTask != null && !runningTask.isCancelled() && !runningTask.isDone()) {
            Integer existingGeneration = (Integer)this.taskGenerationByOwner.get(ownerId);
            if (existingGeneration == null || existingGeneration <= 0) {
                this.taskGenerationByOwner.putIfAbsent(ownerId, 1);
                existingGeneration = (Integer)this.taskGenerationByOwner.getOrDefault(ownerId, 1);
            }
            return existingGeneration;
        }
        if (runningTask != null) {
            this.spawnTasks.remove(ownerId, runningTask);
        }
        long now = System.currentTimeMillis();
        long target = this.nextSpawnAt.getOrDefault(ownerId, now + 10000L);
        long initialDelay = Math.max(0L, target - now);
        int generation = this.taskGenerationByOwner.merge(ownerId, 1, Integer::sum);
        ScheduledFuture<?> task = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(() -> this.spawnWaveSafe(ownerId, generation), initialDelay, 10000L, TimeUnit.MILLISECONDS);
        ScheduledFuture<?> existing = this.spawnTasks.putIfAbsent(ownerId, task);
        if (existing != null) {
            task.cancel(false);
            return this.taskGenerationByOwner.getOrDefault(ownerId, generation);
        }
        return generation;
    }

    private void cancelTask(UUID ownerId) {
        ScheduledFuture task = (ScheduledFuture)this.spawnTasks.remove(ownerId);
        if (task != null) {
            task.cancel(false);
        }
        this.taskGenerationByOwner.merge(ownerId, 1, Integer::sum);
    }

    private boolean isOwnerTaskRunning(@Nonnull UUID ownerId) {
        ScheduledFuture<?> task = (ScheduledFuture)this.spawnTasks.get(ownerId);
        return task != null && !task.isCancelled() && !task.isDone();
    }

    private void spawnWaveSafe(UUID ownerId, int generation) {
        if (this.taskGenerationByOwner.getOrDefault(ownerId, 0) != generation) {
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
        if (this.taskGenerationByOwner.getOrDefault(ownerId, 0) != generation) {
            return;
        }
        Ref<EntityStore> playerRef = this.getAnyActivePlayerRef(ownerId);
        if (playerRef == null || !playerRef.isValid()) {
            this.clearOwnerRuntimeState(ownerId);
            return;
        }
        if (!playerRef.isValid()) {
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
            if (this.taskGenerationByOwner.getOrDefault(ownerId, 0) != generation) {
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
            if (!this.isGameplayWorld((Store<EntityStore>)worldStore)) {
                this.clearOwnerRuntimeState(ownerId);
                return;
            }
            if (Boolean.TRUE.equals(this.bossActive.get(ownerId))) {
                if (this.isBossAlive(ownerId, (Store<EntityStore>)worldStore)) {
                    return;
                }
                this.bossActive.put(ownerId, false);
                this.bossRefs.remove(ownerId);
            }
            if (Boolean.TRUE.equals(this.rejoinHold.get(ownerId))) {
                long holdUntil = this.rejoinHoldUntilByOwner.getOrDefault(ownerId, 0L);
                long now = System.currentTimeMillis();
                if (this.isBossAlive(ownerId, (Store<EntityStore>)worldStore) || this.hasActiveWaveMobs((Store<EntityStore>)worldStore)) {
                    if (holdUntil <= 0L || now < holdUntil) {
                        return;
                    }
                }
                this.rejoinHold.put(ownerId, false);
                this.rejoinHoldUntilByOwner.remove(ownerId);
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
            int wave = this.waveCounts.merge(ownerId, 1, Integer::sum);
            PlayerStateStore stateStore = this.stateStoreManager.getStore(MobWaveSpawner.getWorldKey((Store<EntityStore>)worldStore));
            stateStore.saveWaveCount(ownerId, wave);
            long nextAt = System.currentTimeMillis() + 10000L;
            this.nextSpawnAt.put(ownerId, nextAt);
            stateStore.saveNextSpawnAt(ownerId, nextAt);
            if (wave == 40 || wave == 50) {
                this.spawnWraithBossWave(ownerId, (Store<EntityStore>)worldStore, world, playerPos, random, wave);
                return;
            }
            if (wave >= 10 && (wave - 10) % 10 == 0) {
                this.spawnToadBossWave(ownerId, (Store<EntityStore>)worldStore, world, playerPos, random, wave);
                return;
            }
            if (wave >= 5 && (wave - 5) % 10 == 0) {
                this.spawnBossWave(ownerId, (Store<EntityStore>)worldStore, world, playerPos, random);
                return;
            }
            int spawnCount = MobSpawnConfig.SPAWN_COUNT + MobSpawnConfig.SPAWN_COUNT_PER_WAVE * Math.max(0, wave - 1);
            spawnCount = Math.min(spawnCount, MobSpawnConfig.MAX_MOBS_PER_WAVE);
            for (int i = 0; i < spawnCount; ++i) {
                Vector3d spawnPos = this.randomSpawnPosition(world, playerPos, random);
                Vector3f rotation = new Vector3f(0.0f, (float)(random.nextDouble() * Math.PI * 2.0), 0.0f);
                String role = roles.get(random.nextInt(roles.size()));
                Pair spawned = NPCPlugin.get().spawnNPC(worldStore, role, null, spawnPos, rotation);
                if (spawned == null || spawned.first() == null || !((Ref)spawned.first()).isValid()) continue;
                worldStore.addComponent((Ref)spawned.first(), this.spawnedMarkerType, new SpawnedByMobWaveComponent());
                this.applyWaveHealthBonus((Ref<EntityStore>)((Ref)spawned.first()), (Store<EntityStore>)worldStore, wave, false);
                this.capMobHealth((Ref<EntityStore>)((Ref)spawned.first()), (Store<EntityStore>)worldStore);
            }
        });
    }

    private void spawnBossWave(UUID ownerId, Store<EntityStore> store, World world, Vector3d playerPos, ThreadLocalRandom random) {
        Vector3d spawnPos = this.randomSpawnPosition(world, playerPos, random);
        Vector3f rotation = new Vector3f(0.0f, (float)(random.nextDouble() * Math.PI * 2.0), 0.0f);
        Pair spawned = NPCPlugin.get().spawnNPC(store, "Skeleton", null, spawnPos, rotation);
        if (spawned == null || spawned.first() == null || !((Ref)spawned.first()).isValid()) {
            return;
        }
        Ref bossRef = (Ref)spawned.first();
        store.addComponent(bossRef, this.spawnedMarkerType, new SpawnedByMobWaveComponent());
        store.addComponent(bossRef, this.bossMarkerType, new BossWaveComponent(ownerId));
        this.bossActive.put(ownerId, true);
        this.bossRefs.put(ownerId, (Ref<EntityStore>)bossRef);
        PlayerStateStore stateStore = this.stateStoreManager.getStore(MobWaveSpawner.getWorldKey(store));
        UUIDComponent uuidComponent = (UUIDComponent)store.getComponent(bossRef, UUIDComponent.getComponentType());
        if (uuidComponent != null) {
            stateStore.saveBossId(ownerId, uuidComponent.getUuid());
        }
        this.applyWaveHealthBonus((Ref<EntityStore>)bossRef, store, this.waveCounts.getOrDefault(ownerId, 1), true);
        this.applyNpcScale((Ref<EntityStore>)bossRef, store, world, 0, 4.0f);
    }

    private void spawnToadBossWave(UUID ownerId, Store<EntityStore> store, World world, Vector3d playerPos, ThreadLocalRandom random, int wave) {
        Vector3d spawnPos = this.randomSpawnPosition(world, playerPos, random);
        Vector3f rotation = new Vector3f(0.0f, (float)(random.nextDouble() * Math.PI * 2.0), 0.0f);
        Pair spawned = NPCPlugin.get().spawnNPC(store, "Toad_Rhino_Magma", null, spawnPos, rotation);
        if (spawned == null || spawned.first() == null || !((Ref)spawned.first()).isValid()) {
            return;
        }
        Ref bossRef = (Ref)spawned.first();
        store.addComponent(bossRef, this.spawnedMarkerType, new SpawnedByMobWaveComponent());
        store.addComponent(bossRef, this.bossMarkerType, new BossWaveComponent(ownerId));
        this.bossActive.put(ownerId, true);
        this.bossRefs.put(ownerId, (Ref<EntityStore>)bossRef);
        PlayerStateStore stateStore = this.stateStoreManager.getStore(MobWaveSpawner.getWorldKey(store));
        UUIDComponent uuidComponent = (UUIDComponent)store.getComponent(bossRef, UUIDComponent.getComponentType());
        if (uuidComponent != null) {
            stateStore.saveBossId(ownerId, uuidComponent.getUuid());
        }
        int spawnIndex = (wave - 10) / 10 + 1;
        spawnIndex = Math.max(1, spawnIndex);
        float targetHealth = 400.0f + 200.0f * (float)(spawnIndex - 1);
        this.applyFixedBossHealth((Ref<EntityStore>)bossRef, store, targetHealth, "ToadBossHealth");
        this.applyNpcScale((Ref<EntityStore>)bossRef, store, world, 0, 2.0f);
    }

    private void spawnWraithBossWave(UUID ownerId, Store<EntityStore> store, World world, Vector3d playerPos, ThreadLocalRandom random, int wave) {
        Vector3d spawnPos = this.randomSpawnPosition(world, playerPos, random);
        Vector3f rotation = new Vector3f(0.0f, (float)(random.nextDouble() * Math.PI * 2.0), 0.0f);
        Pair spawned = NPCPlugin.get().spawnNPC(store, "Wraith", null, spawnPos, rotation);
        if (spawned == null || spawned.first() == null || !((Ref)spawned.first()).isValid()) {
            return;
        }
        Ref bossRef = (Ref)spawned.first();
        store.addComponent(bossRef, this.spawnedMarkerType, new SpawnedByMobWaveComponent());
        store.addComponent(bossRef, this.bossMarkerType, new BossWaveComponent(ownerId));
        this.bossActive.put(ownerId, true);
        this.bossRefs.put(ownerId, (Ref<EntityStore>)bossRef);
        PlayerStateStore stateStore = this.stateStoreManager.getStore(MobWaveSpawner.getWorldKey(store));
        UUIDComponent uuidComponent = (UUIDComponent)store.getComponent(bossRef, UUIDComponent.getComponentType());
        if (uuidComponent != null) {
            stateStore.saveBossId(ownerId, uuidComponent.getUuid());
        }
        int spawnIndex = wave == 50 ? 2 : 1;
        float targetHealth = 1000.0f + 500.0f * (float)(spawnIndex - 1);
        this.applyFixedBossHealth((Ref<EntityStore>)bossRef, store, targetHealth, "WraithBossHealth");
        this.applyNpcScale((Ref<EntityStore>)bossRef, store, world, 0, 2.0f);
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

    private boolean isGameplayWorld(@Nonnull Store<EntityStore> store) {
        return true;
    }

    private void reattachNearbyWaveMarkers(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> playerRef) {
        TransformComponent playerTransform = (TransformComponent)store.getComponent(playerRef, TransformComponent.getComponentType());
        if (playerTransform == null) {
            return;
        }
        Vector3d playerPos = playerTransform.getPosition();
        int[] reattachedCount = new int[]{0};
        store.forEachChunk((Query)Query.and((Query[])new Query[]{NPCEntity.getComponentType(), TransformComponent.getComponentType()}), (chunk, buffer) -> {
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
                if (MobWaveSpawner.distanceSquared(playerPos, npcPos) > MARKER_REATTACH_RADIUS_SQ) continue;
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
        this.bossActive.put(ownerId, false);
        this.bossRefs.remove(ownerId);
        String worldKey = (String)this.worldByOwner.get(ownerId);
        if (worldKey != null) {
            this.stateStoreManager.getStore(worldKey).saveBossId(ownerId, null);
        }
    }

    private void syncBossState(@Nonnull UUID ownerId, @Nonnull Store<EntityStore> store) {
        AtomicReference<Ref<EntityStore>> foundBoss = new AtomicReference<>(null);
        store.forEachChunk((Query)Query.and((Query[])new Query[]{this.bossMarkerType}), (chunk, buffer) -> {
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
        if (bossRef != null && bossRef.isValid()) {
            this.bossActive.put(ownerId, true);
            this.bossRefs.put(ownerId, (Ref<EntityStore>)bossRef);
        } else {
            this.bossActive.put(ownerId, false);
            this.bossRefs.remove(ownerId);
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
            this.bossActive.put(ownerId, true);
            this.bossRefs.put(ownerId, (Ref<EntityStore>)bossRef);
        });
    }

    private boolean hasActiveWaveMobs(@Nonnull Store<EntityStore> store) {
        AtomicBoolean found = new AtomicBoolean(false);
        store.forEachChunk((Query)Query.and((Query[])new Query[]{this.spawnedMarkerType}), (chunk, buffer) -> {
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
        ArrayList<Ref<EntityStore>> refs = new ArrayList<Ref<EntityStore>>();
        Set<UUID> members = (Set)this.playersByOwner.get(ownerId);
        if (members == null || members.isEmpty()) {
            return refs;
        }
        ArrayList<UUID> staleMembers = null;
        for (UUID memberId : members) {
            Ref<EntityStore> memberRef = (Ref)this.playerRefs.get(memberId);
            if (memberRef == null || !memberRef.isValid()) {
                if (staleMembers == null) {
                    staleMembers = new ArrayList<UUID>();
                }
                staleMembers.add(memberId);
                continue;
            }
            Store<EntityStore> memberStore = memberRef.getStore();
            if (memberStore == null || !this.isGameplayWorld(memberStore)) {
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
            if (members.isEmpty()) {
                this.playersByOwner.remove(ownerId, members);
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
                if (worldStore == null || !this.isGameplayWorld(worldStore)) {
                    return;
                }
                playerRefComponent = (PlayerRef)worldStore.getComponent(playerRef, PlayerRef.getComponentType());
                if (playerRefComponent == null) {
                    return;
                }
                UUID playerId = playerRefComponent.getUuid();
                String worldKey = MobWaveSpawner.getWorldKey(worldStore);
                ownerId = MobWaveSpawner.getSharedOwnerId(worldKey);
                this.playerRefs.put(playerId, playerRef);
                UUID previousOwnerId = (UUID)this.ownerByPlayer.put(playerId, ownerId);
                if (previousOwnerId != null && !previousOwnerId.equals(ownerId) && this.removePlayerFromOwner(previousOwnerId, playerId)) {
                    this.clearOwnerRuntimeState(previousOwnerId);
                }
                ((Set)this.playersByOwner.computeIfAbsent(ownerId, id -> ConcurrentHashMap.newKeySet())).add(playerId);
                ((Set)this.participantsByOwner.computeIfAbsent(ownerId, id -> ConcurrentHashMap.newKeySet())).add(playerId);
                this.worldByOwner.put(ownerId, worldKey);
                this.storeByOwner.put(ownerId, worldStore);
                PlayerStateStore stateStore = this.stateStoreManager.getStore(worldKey);
                this.cancelTask(ownerId);
                this.waveCounts.put(ownerId, 0);
                stateStore.saveWaveCount(ownerId, 0);
                stateStore.saveBossId(ownerId, null);
                this.bossActive.remove(ownerId);
                this.bossRefs.remove(ownerId);
                this.despawnTrackedWaveEntities(worldStore);
                this.rejoinHold.put(ownerId, false);
                this.rejoinHoldUntilByOwner.remove(ownerId);
                long now = System.currentTimeMillis();
                long nextAt = now + 10000L;
                this.nextSpawnAt.put(ownerId, nextAt);
                stateStore.saveNextSpawnAt(ownerId, nextAt);
                int generation = this.scheduleForOwner(ownerId);
                this.spawnWave(ownerId, generation);
            });
        }
        catch (IllegalThreadStateException ignored) {
        }
    }

    private static String getWorldKey(@Nonnull Store<EntityStore> store) {
        String displayName;
        World world = ((EntityStore)store.getExternalData()).getWorld();
        if (world == null) {
            return "default";
        }
        if (world.getWorldConfig() != null && world.getWorldConfig().getUuid() != null) {
            return "world-" + world.getWorldConfig().getUuid().toString();
        }
        String string = displayName = world.getWorldConfig() != null ? world.getWorldConfig().getDisplayName() : null;
        if (displayName != null && !displayName.isBlank()) {
            return displayName.trim();
        }
        String name = world.getName();
        if (name != null && !name.isBlank()) {
            return name.trim();
        }
        return "default";
    }

    private List<String> getSpawnableRoles() {
        if (this.spawnableRoles.isEmpty()) {
            this.plugin.getLogger().at(Level.WARNING).log("No spawnable NPC roles configured; mobs will not spawn.");
        }
        return this.spawnableRoles;
    }

    private Vector3d randomSpawnPosition(World world, Vector3d origin, ThreadLocalRandom random) {
        double minRadius = MobSpawnConfig.MIN_SPAWN_RADIUS;
        double maxRadius = Math.max(minRadius, MobSpawnConfig.MAX_SPAWN_RADIUS);
        double radius = minRadius + random.nextDouble() * (maxRadius - minRadius);
        double angle = random.nextDouble() * Math.PI * 2.0;
        double x = origin.getX() + Math.cos(angle) * radius;
        double z = origin.getZ() + Math.sin(angle) * radius;
        double y = origin.getY();
        return new Vector3d(x, y, z);
    }

    private boolean isBossAlive(UUID ownerId, Store<EntityStore> store) {
        Ref bossRef = (Ref)this.bossRefs.get(ownerId);
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

    private boolean removePlayerFromOwner(@Nonnull UUID ownerId, @Nonnull UUID playerId) {
        Set<UUID> members = (Set)this.playersByOwner.get(ownerId);
        if (members == null) {
            return true;
        }
        members.remove(playerId);
        if (!members.isEmpty()) {
            return false;
        }
        this.playersByOwner.remove(ownerId, members);
        return true;
    }

    private void clearOwnerRuntimeState(@Nonnull UUID ownerId) {
        this.cancelTask(ownerId);
        this.waveCounts.remove(ownerId);
        this.bossActive.remove(ownerId);
        this.bossRefs.remove(ownerId);
        this.rejoinHold.remove(ownerId);
        this.rejoinHoldUntilByOwner.remove(ownerId);
        this.worldByOwner.remove(ownerId);
        this.storeByOwner.remove(ownerId);
        this.nextSpawnAt.remove(ownerId);
        this.taskGenerationByOwner.remove(ownerId);
    }

    private void resetOwnerProgress(@Nonnull UUID ownerId, @Nullable Store<EntityStore> store) {
        String worldKey = store != null ? MobWaveSpawner.getWorldKey(store) : (String)this.worldByOwner.get(ownerId);
        Set<UUID> participants = (Set)this.participantsByOwner.remove(ownerId);
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
        this.nextSpawnAt.put(ownerId, 0L);
        if (store == null) {
            Ref<EntityStore> bossRef = (Ref)this.bossRefs.get(ownerId);
            if (bossRef != null && bossRef.isValid()) {
                store = bossRef.getStore();
            }
        }
        if (store != null) {
            World world = ((EntityStore)store.getExternalData()).getWorld();
            if (world != null && world.isTicking()) {
                Store<EntityStore> finalStore = store;
                try {
                    world.execute(() -> this.despawnTrackedWaveEntities(finalStore));
                }
                catch (IllegalThreadStateException ignored) {
                }
            }
        }
        this.clearOwnerRuntimeState(ownerId);
    }

    private void despawnTrackedWaveEntities(@Nonnull Store<EntityStore> store) {
        store.forEachChunk((Query)Query.and((Query[])new Query[]{this.spawnedMarkerType}), (chunk, buffer) -> {
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
        Set<UUID> members = (Set)this.playersByOwner.get(ownerId);
        if (members == null || members.isEmpty()) {
            return null;
        }
        ArrayList<UUID> staleMembers = null;
        for (UUID memberId : members) {
            Ref<EntityStore> memberRef = (Ref)this.playerRefs.get(memberId);
            if (memberRef == null || !memberRef.isValid()) {
                if (staleMembers == null) {
                    staleMembers = new ArrayList<UUID>();
                }
                staleMembers.add(memberId);
                continue;
            }
            Store<EntityStore> memberStore = memberRef.getStore();
            if (memberStore == null || !this.isGameplayWorld(memberStore)) {
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
            if (members.isEmpty()) {
                this.playersByOwner.remove(ownerId, members);
            }
        }
        return null;
    }

    private void applyWaveHealthBonus(@Nonnull Ref<EntityStore> npcRef, @Nonnull Store<EntityStore> store, int wave, boolean isBoss) {
        float bonus;
        if (wave <= 1) {
            return;
        }
        if (isBoss) {
            bonus = 500.0f + 150.0f * (float)(wave - 1);
        } else {
            float perWaveBonus = 10.0f;
            if (perWaveBonus <= 0.0f) {
                return;
            }
            bonus = perWaveBonus * (float)(wave - 1);
        }
        EntityStatMap statMap = (EntityStatMap)store.getComponent(npcRef, EntityStatMap.getComponentType());
        if (statMap == null) {
            return;
        }
        String modifierId = isBoss ? "BossWaveHealth" : "WaveHealth";
        statMap.putModifier(DefaultEntityStatTypes.getHealth(), modifierId, (Modifier)new StaticModifier(Modifier.ModifierTarget.MAX, StaticModifier.CalculationType.ADDITIVE, bonus));
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
        this.applyFixedBossHealth(npcRef, store, MobSpawnConfig.MAX_MOB_HEALTH, "WaveHealthCap");
    }
}
