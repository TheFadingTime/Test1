/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Component
 *  com.hypixel.hytale.component.ComponentType
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.RemoveReason
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.component.query.Query
 *  com.hypixel.hytale.logger.HytaleLogger$Api
 *  com.hypixel.hytale.math.vector.Vector3d
 *  com.hypixel.hytale.math.vector.Vector3f
 *  com.hypixel.hytale.server.core.HytaleServer
 *  com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType
 *  com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset
 *  com.hypixel.hytale.server.core.entity.UUIDComponent
 *  com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent
 *  com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent
 *  com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
 *  com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap
 *  com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue
 *  com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes
 *  com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier
 *  com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier$ModifierTarget
 *  com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier
 *  com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier$CalculationType
 *  com.hypixel.hytale.server.core.plugin.JavaPlugin
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.world.World
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 *  com.hypixel.hytale.server.npc.NPCPlugin
 *  com.hypixel.hytale.server.npc.entities.NPCEntity
 *  it.unimi.dsi.fastutil.Pair
 *  javax.annotation.Nonnull
 *  javax.annotation.Nullable
 */
package com.fadingtime.hytalemod.spawner;

import com.fadingtime.hytalemod.HytaleMod;
import com.fadingtime.hytalemod.component.BossWaveComponent;
import com.fadingtime.hytalemod.component.SpawnedByMobWaveComponent;
import com.fadingtime.hytalemod.config.ConfigManager;
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
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
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
    private static final double MARKER_REATTACH_RADIUS_SQ = 25600.0;
    private static final String BLOCKED_SPAWN_BLOCK_ID = "Plant_Moss_Block_Green";
    private static final int MAX_SPAWN_POSITION_ATTEMPTS = 24;
    private final JavaPlugin plugin;
    private final ComponentType<EntityStore, SpawnedByMobWaveComponent> spawnedMarkerType;
    private final ComponentType<EntityStore, BossWaveComponent> bossMarkerType;
    private final PlayerStateStoreManager stateStoreManager;
    private final ConcurrentMap<UUID, ScheduledFuture<?>> spawnTasks = new ConcurrentHashMap();
    private final List<String> spawnableRoles = new ArrayList<String>();
    private final Set<String> trackedWaveRoles = new HashSet<String>();
    private final ConcurrentMap<UUID, Object> ownerLocks = new ConcurrentHashMap<UUID, Object>();
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

    public MobWaveSpawner(@Nonnull JavaPlugin javaPlugin, @Nonnull ComponentType<EntityStore, SpawnedByMobWaveComponent> componentType, @Nonnull ComponentType<EntityStore, BossWaveComponent> componentType2) {
        this.plugin = javaPlugin;
        this.spawnedMarkerType = componentType;
        this.bossMarkerType = componentType2;
        this.stateStoreManager = HytaleMod.getInstance().getStateStoreManager();
        ConfigManager.SpawnConfig spawnConfig = ConfigManager.get();
        this.spawnableRoles.addAll(spawnConfig.hostileRoles);
        this.trackedWaveRoles.addAll(this.spawnableRoles);
        for (ConfigManager.BossDefinition bossDefinition : spawnConfig.bosses) {
            this.trackedWaveRoles.add(bossDefinition.role());
        }
        javaPlugin.getEventRegistry().registerGlobal(PlayerReadyEvent.class, this::onPlayerReady);
        javaPlugin.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, this::onPlayerDisconnect);
    }

    public void shutdown() {
        this.spawnTasks.values().forEach(scheduledFuture -> scheduledFuture.cancel(false));
        this.spawnTasks.clear();
    }

    private void onPlayerReady(@Nonnull PlayerReadyEvent playerReadyEvent) {
        Ref ref = playerReadyEvent.getPlayerRef();
        if (ref == null || !ref.isValid()) {
            return;
        }
        Store store = ref.getStore();
        if (store == null) {
            return;
        }
        World world = ((EntityStore)store.getExternalData()).getWorld();
        if (world == null || !world.isTicking()) {
            return;
        }
        try {
            world.execute(() -> {
                long l;
                long l2;
                if (!ref.isValid()) {
                    return;
                }
                Store store2 = ref.getStore();
                if (store2 == null) {
                    return;
                }
                PlayerRef playerRef = (PlayerRef)store2.getComponent(ref, PlayerRef.getComponentType());
                if (playerRef == null) {
                    return;
                }
                UUID uUID = playerRef.getUuid();
                this.disconnectHandledAt.remove(uUID);
                this.playerRefs.put(uUID, (Ref<EntityStore>)ref);
                if (!this.isGameplayWorld((Store<EntityStore>)store2)) {
                    this.playerRefs.remove(uUID);
                    UUID uUID2 = (UUID)this.ownerByPlayer.remove(uUID);
                    if (uUID2 != null && this.removePlayerFromOwner(uUID2, uUID)) {
                        this.clearOwnerRuntimeState(uUID2);
                    }
                    return;
                }
                String string = MobWaveSpawner.getWorldKey((Store<EntityStore>)store2);
                UUID uUID3 = MobWaveSpawner.getSharedOwnerId(string);
                this.assignPlayerToOwner(uUID, uUID3);
                Object object = this.getOwnerLock(uUID3);
                synchronized (object) {
                    this.worldByOwner.put(uUID3, string);
                    this.storeByOwner.put(uUID3, (Store<EntityStore>)store2);
                    this.rejoinHold.put(uUID3, true);
                    this.rejoinHoldUntilByOwner.put(uUID3, System.currentTimeMillis() + 20000L);
                }
                object = this.stateStoreManager.getStore(string);
                int n = Math.max(0, ((PlayerStateStore)object).loadWaveCount(uUID3));
                this.waveCounts.put(uUID3, n);
                UUID uUID4 = ((PlayerStateStore)object).loadBossId(uUID3);
                long l3 = ((PlayerStateStore)object).loadNextSpawnAt(uUID3);
                long l4 = System.currentTimeMillis();
                ConfigManager.SpawnConfig spawnConfig = ConfigManager.get();
                if (l3 <= 0L) {
                    l3 = l4 + spawnConfig.spawnIntervalMs;
                }
                if (l3 < (l2 = l4 + (l = Math.max(1000L, Math.min(5000L, spawnConfig.spawnIntervalMs / 2L))))) {
                    l3 = l2;
                }
                ((PlayerStateStore)object).saveNextSpawnAt(uUID3, l3);
                this.nextSpawnAt.put(uUID3, l3);
                if (n <= 0 && uUID4 == null) {
                    Object object2 = this.getOwnerLock(uUID3);
                    synchronized (object2) {
                        this.bossActive.remove(uUID3);
                        this.bossRefs.remove(uUID3);
                        this.rejoinHold.put(uUID3, false);
                        this.rejoinHoldUntilByOwner.remove(uUID3);
                    }
                    return;
                }
                if (n <= 0) {
                    this.reattachNearbyWaveMarkers((Store<EntityStore>)store2, (Ref<EntityStore>)ref);
                    this.despawnTrackedWaveEntities((Store<EntityStore>)store2);
                    ((PlayerStateStore)object).saveBossId(uUID3, null);
                    Object object3 = this.getOwnerLock(uUID3);
                    synchronized (object3) {
                        this.bossActive.remove(uUID3);
                        this.bossRefs.remove(uUID3);
                        this.rejoinHold.put(uUID3, false);
                        this.rejoinHoldUntilByOwner.remove(uUID3);
                    }
                    return;
                }
                this.reattachNearbyWaveMarkers((Store<EntityStore>)store2, (Ref<EntityStore>)ref);
                HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
                    try {
                        world.execute(() -> {
                            if (!ref.isValid()) {
                                return;
                            }
                            Store store3 = ref.getStore();
                            if (store3 == null || !this.isGameplayWorld((Store<EntityStore>)store3)) {
                                return;
                            }
                            this.reattachNearbyWaveMarkers((Store<EntityStore>)store3, (Ref<EntityStore>)ref);
                        });
                    }
                    catch (IllegalThreadStateException illegalThreadStateException) {
                    }
                }, 1500L, TimeUnit.MILLISECONDS);
                this.restoreBossFromSave(uUID3, (Store<EntityStore>)store2, (PlayerStateStore)object);
                this.syncBossState(uUID3, (Store<EntityStore>)store2);
                if (!this.isOwnerTaskRunning(uUID3)) {
                    this.scheduleForOwner(uUID3);
                }
            });
        }
        catch (IllegalThreadStateException illegalThreadStateException) {
        }
    }

    private void onPlayerDisconnect(@Nonnull PlayerDisconnectEvent playerDisconnectEvent) {
        long l;
        PlayerRef playerRef = playerDisconnectEvent.getPlayerRef();
        if (playerRef == null) {
            return;
        }
        UUID uUID = playerRef.getUuid();
        Long l2 = this.disconnectHandledAt.put(uUID, l = System.currentTimeMillis());
        if (l2 != null && l - l2 < 2000L) {
            return;
        }
        this.playerRefs.remove(uUID);
        UUID uUID2 = (UUID)this.ownerByPlayer.remove(uUID);
        if (uUID2 == null) {
            if (HytaleMod.getInstance().getBossHudSystem() != null) {
                HytaleMod.getInstance().getBossHudSystem().clearBossHud(uUID);
            }
            return;
        }
        boolean bl = this.removePlayerFromOwner(uUID2, uUID);
        if (bl) {
            this.resetOwnerProgress(uUID2, (Store<EntityStore>)((Store)this.storeByOwner.get(uUID2)));
            if (HytaleMod.getInstance().getBossHudSystem() != null) {
                HytaleMod.getInstance().getBossHudSystem().clearBossHud(uUID2);
            }
            return;
        }
        if (HytaleMod.getInstance().getBossHudSystem() != null) {
            HytaleMod.getInstance().getBossHudSystem().clearBossHud(uUID);
        }
    }

    private int scheduleForOwner(UUID uUID) {
        ScheduledFuture scheduledFuture = (ScheduledFuture)this.spawnTasks.get(uUID);
        if (scheduledFuture != null && !scheduledFuture.isCancelled() && !scheduledFuture.isDone()) {
            Integer n = (Integer)this.taskGenerationByOwner.get(uUID);
            if (n == null || n <= 0) {
                this.taskGenerationByOwner.putIfAbsent(uUID, 1);
                n = this.taskGenerationByOwner.getOrDefault(uUID, 1);
            }
            return n;
        }
        if (scheduledFuture != null) {
            this.spawnTasks.remove(uUID, scheduledFuture);
        }
        long l = System.currentTimeMillis();
        ConfigManager.SpawnConfig spawnConfig = ConfigManager.get();
        long l2 = this.nextSpawnAt.getOrDefault(uUID, l + spawnConfig.spawnIntervalMs);
        long l3 = Math.max(0L, l2 - l);
        int n = this.taskGenerationByOwner.merge(uUID, 1, Integer::sum);
        ScheduledFuture<?> scheduledFuture2 = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(() -> this.spawnWaveSafe(uUID, n), l3, spawnConfig.spawnIntervalMs, TimeUnit.MILLISECONDS);
        ScheduledFuture<?> scheduledFuture3 = this.spawnTasks.putIfAbsent(uUID, scheduledFuture2);
        if (scheduledFuture3 != null) {
            scheduledFuture2.cancel(false);
            return this.taskGenerationByOwner.getOrDefault(uUID, n);
        }
        return n;
    }

    private void cancelTask(UUID uUID) {
        ScheduledFuture scheduledFuture = (ScheduledFuture)this.spawnTasks.remove(uUID);
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
        this.taskGenerationByOwner.merge(uUID, 1, Integer::sum);
    }

    private boolean isOwnerTaskRunning(@Nonnull UUID uUID) {
        ScheduledFuture scheduledFuture = (ScheduledFuture)this.spawnTasks.get(uUID);
        return scheduledFuture != null && !scheduledFuture.isCancelled() && !scheduledFuture.isDone();
    }

    private void spawnWaveSafe(UUID uUID, int n) {
        if (this.taskGenerationByOwner.getOrDefault(uUID, 0) != n) {
            return;
        }
        try {
            this.spawnWave(uUID, n);
        }
        catch (Exception exception) {
            ((HytaleLogger.Api)this.plugin.getLogger().at(Level.WARNING).withCause((Throwable)exception)).log("Mob spawn tick failed for shared owner %s", (Object)uUID);
        }
    }

    private void spawnWave(UUID uUID, int n) {
        if (this.taskGenerationByOwner.getOrDefault(uUID, 0) != n) {
            return;
        }
        Ref<EntityStore> ref = this.getAnyActivePlayerRef(uUID);
        if (ref == null || !ref.isValid()) {
            this.clearOwnerRuntimeState(uUID);
            return;
        }
        if (!ref.isValid()) {
            this.clearOwnerRuntimeState(uUID);
            return;
        }
        Store store = ref.getStore();
        if (store == null) {
            this.clearOwnerRuntimeState(uUID);
            return;
        }
        World world = ((EntityStore)store.getExternalData()).getWorld();
        world.execute(() -> {
            if (this.taskGenerationByOwner.getOrDefault(uUID, 0) != n) {
                return;
            }
            Ref<EntityStore> activePlayerRef = this.getAnyActivePlayerRef(uUID);
            if (activePlayerRef == null || !activePlayerRef.isValid()) {
                this.clearOwnerRuntimeState(uUID);
                return;
            }
            Store store2 = activePlayerRef.getStore();
            if (store2 == null) {
                this.clearOwnerRuntimeState(uUID);
                return;
            }
            if (!this.isGameplayWorld((Store<EntityStore>)store2)) {
                this.clearOwnerRuntimeState(uUID);
                return;
            }
            if (Boolean.TRUE.equals(this.bossActive.get(uUID))) {
                if (this.isBossAlive(uUID, (Store<EntityStore>)store)) {
                    return;
                }
                this.bossActive.put(uUID, false);
                this.bossRefs.remove(uUID);
            }
            if (Boolean.TRUE.equals(this.rejoinHold.get(uUID))) {
                long l = this.rejoinHoldUntilByOwner.getOrDefault(uUID, 0L);
                long l2 = System.currentTimeMillis();
                if ((this.isBossAlive(uUID, (Store<EntityStore>)store) || this.hasActiveWaveMobs((Store<EntityStore>)store)) && (l <= 0L || l2 < l)) {
                    return;
                }
                this.rejoinHold.put(uUID, false);
                this.rejoinHoldUntilByOwner.remove(uUID);
                return;
            }
            TransformComponent transformComponent = (TransformComponent)store2.getComponent(activePlayerRef, TransformComponent.getComponentType());
            if (transformComponent == null) {
                return;
            }
            List<String> list = this.getSpawnableRoles();
            if (list.isEmpty()) {
                return;
            }
            Vector3d vector3d = transformComponent.getPosition();
            ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
            int n2 = this.waveCounts.merge(uUID, 1, Integer::sum);
            PlayerStateStore playerStateStore = this.stateStoreManager.getStore(MobWaveSpawner.getWorldKey((Store<EntityStore>)store2));
            playerStateStore.saveWaveCount(uUID, n2);
            ConfigManager.SpawnConfig spawnConfig = ConfigManager.get();
            long l = System.currentTimeMillis() + spawnConfig.spawnIntervalMs;
            this.nextSpawnAt.put(uUID, l);
            playerStateStore.saveNextSpawnAt(uUID, l);
            ConfigManager.BossDefinition bossDefinition = this.findBossForWave(spawnConfig, n2);
            if (bossDefinition != null) {
                this.spawnConfiguredBossWave(uUID, (Store<EntityStore>)store2, world, vector3d, threadLocalRandom, n2, bossDefinition);
                return;
            }
            int n3 = spawnConfig.spawnCount + spawnConfig.spawnCountPerWave * Math.max(0, n2 - 1);
            n3 = Math.min(n3, spawnConfig.maxMobsPerWave);
            for (int i = 0; i < n3; ++i) {
                Vector3d vector3d2 = this.randomSpawnPosition(world, vector3d, threadLocalRandom);
                if (vector3d2 == null) continue;
                Vector3f vector3f = new Vector3f(0.0f, (float)(threadLocalRandom.nextDouble() * Math.PI * 2.0), 0.0f);
                String string = list.get(threadLocalRandom.nextInt(list.size()));
                Pair pair = NPCPlugin.get().spawnNPC(store2, string, null, vector3d2, vector3f);
                if (pair == null || pair.first() == null || !((Ref)pair.first()).isValid()) continue;
                store2.addComponent((Ref)pair.first(), this.spawnedMarkerType, new SpawnedByMobWaveComponent());
            }
        });
    }

    private void spawnConfiguredBossWave(UUID uUID, Store<EntityStore> store, World world, Vector3d vector3d, ThreadLocalRandom threadLocalRandom, int n, ConfigManager.BossDefinition bossDefinition) {
        Vector3d vector3d2 = this.randomSpawnPosition(world, vector3d, threadLocalRandom);
        if (vector3d2 == null) {
            return;
        }
        Vector3f vector3f = new Vector3f(0.0f, (float)(threadLocalRandom.nextDouble() * Math.PI * 2.0), 0.0f);
        Pair pair = NPCPlugin.get().spawnNPC(store, bossDefinition.role(), null, vector3d2, vector3f);
        if (pair == null || pair.first() == null || !((Ref)pair.first()).isValid()) {
            return;
        }
        Ref ref = (Ref)pair.first();
        store.addComponent(ref, this.spawnedMarkerType, new SpawnedByMobWaveComponent());
        store.addComponent(ref, this.bossMarkerType, new BossWaveComponent(uUID));
        this.bossActive.put(uUID, true);
        this.bossRefs.put(uUID, (Ref<EntityStore>)ref);
        PlayerStateStore playerStateStore = this.stateStoreManager.getStore(MobWaveSpawner.getWorldKey(store));
        UUIDComponent uUIDComponent = (UUIDComponent)store.getComponent(ref, UUIDComponent.getComponentType());
        if (uUIDComponent != null) {
            playerStateStore.saveBossId(uUID, uUIDComponent.getUuid());
        }
        float f = bossDefinition.getTargetHealth(n);
        this.applyFixedBossHealth((Ref<EntityStore>)ref, store, f, bossDefinition.healthModifierId());
        this.applyNpcScale((Ref<EntityStore>)ref, store, world, 0, bossDefinition.scale());
    }

    private void applyNpcScale(Ref<EntityStore> ref, Store<EntityStore> store, World world, int n, float f) {
        if (!ref.isValid()) {
            return;
        }
        NPCEntity nPCEntity = (NPCEntity)store.getComponent(ref, NPCEntity.getComponentType());
        if (nPCEntity == null || nPCEntity.getRole() == null) {
            this.retryNpcScale(ref, store, world, n, f);
            return;
        }
        String string = nPCEntity.getRole().getAppearanceName();
        if (string == null || string.isEmpty()) {
            this.retryNpcScale(ref, store, world, n, f);
            return;
        }
        ModelAsset modelAsset = (ModelAsset)ModelAsset.getAssetMap().getAsset(string);
        if (modelAsset == null) {
            this.retryNpcScale(ref, store, world, n, f);
            return;
        }
        nPCEntity.setInitialModelScale(f);
        nPCEntity.setAppearance(ref, modelAsset, store);
    }

    private boolean isGameplayWorld(@Nonnull Store<EntityStore> store) {
        return true;
    }

    private void reattachNearbyWaveMarkers(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref) {
        TransformComponent transformComponent = (TransformComponent)store.getComponent(ref, TransformComponent.getComponentType());
        if (transformComponent == null) {
            return;
        }
        Vector3d vector3d = transformComponent.getPosition();
        int[] nArray = new int[]{0};
        store.forEachChunk((Query)Query.and((Query[])new Query[]{NPCEntity.getComponentType(), TransformComponent.getComponentType()}), (archetypeChunk, commandBuffer) -> {
            int n = archetypeChunk.size();
            for (int i = 0; i < n; ++i) {
                Vector3d vector3d2;
                String string;
                Ref npcRef = archetypeChunk.getReferenceTo(i);
                if (npcRef == null || !npcRef.isValid() || archetypeChunk.getComponent(i, this.spawnedMarkerType) != null) continue;
                NPCEntity nPCEntity = (NPCEntity)archetypeChunk.getComponent(i, NPCEntity.getComponentType());
                TransformComponent npcTransform = (TransformComponent)archetypeChunk.getComponent(i, TransformComponent.getComponentType());
                if (nPCEntity == null || npcTransform == null) continue;
                String string2 = string = nPCEntity.getRole() != null ? nPCEntity.getRole().getRoleName() : nPCEntity.getRoleName();
                if (string == null || !this.trackedWaveRoles.contains(string) || MobWaveSpawner.distanceSquared(vector3d, vector3d2 = npcTransform.getPosition()) > 25600.0) continue;
                commandBuffer.addComponent(npcRef, this.spawnedMarkerType, new SpawnedByMobWaveComponent());
                nArray[0] = nArray[0] + 1;
            }
        });
        this.plugin.getLogger().at(Level.INFO).log("Wave marker reattach scan complete for player %s: added=%d", ref, (Object)nArray[0]);
    }

    private static double distanceSquared(@Nonnull Vector3d vector3d, @Nonnull Vector3d vector3d2) {
        double d = vector3d.getX() - vector3d2.getX();
        double d2 = vector3d.getY() - vector3d2.getY();
        double d3 = vector3d.getZ() - vector3d2.getZ();
        return d * d + d2 * d2 + d3 * d3;
    }

    private void retryNpcScale(Ref<EntityStore> ref, Store<EntityStore> store, World world, int n, float f) {
        if (n >= 3) {
            return;
        }
        HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> world.execute(() -> this.applyNpcScale(ref, store, world, n + 1, f)), 50L, TimeUnit.MILLISECONDS);
    }

    public void notifyBossDefeated(UUID uUID) {
        this.bossActive.put(uUID, false);
        this.bossRefs.remove(uUID);
        String string = (String)this.worldByOwner.get(uUID);
        if (string != null) {
            this.stateStoreManager.getStore(string).saveBossId(uUID, null);
        }
    }

    private void syncBossState(@Nonnull UUID uUID, @Nonnull Store<EntityStore> store) {
        AtomicReference<Ref<EntityStore>> atomicReference = new AtomicReference<Ref<EntityStore>>(null);
        store.forEachChunk((Query)Query.and((Query[])new Query[]{this.bossMarkerType}), (archetypeChunk, commandBuffer) -> {
            if (atomicReference.get() != null) {
                return;
            }
            int n = archetypeChunk.size();
            for (int i = 0; i < n; ++i) {
                Ref ref;
                BossWaveComponent bossWaveComponent = (BossWaveComponent)archetypeChunk.getComponent(i, this.bossMarkerType);
                if (bossWaveComponent == null || !uUID.equals(bossWaveComponent.getOwnerId()) || (ref = archetypeChunk.getReferenceTo(i)) == null || !ref.isValid()) continue;
                atomicReference.set(ref);
                return;
            }
        });
        Ref<EntityStore> ref = atomicReference.get();
        if (ref != null && ref.isValid()) {
            this.bossActive.put(uUID, true);
            this.bossRefs.put(uUID, (Ref<EntityStore>)ref);
        } else {
            this.bossActive.put(uUID, false);
            this.bossRefs.remove(uUID);
        }
    }

    private void restoreBossFromSave(@Nonnull UUID uUID, @Nonnull Store<EntityStore> store, @Nonnull PlayerStateStore playerStateStore) {
        UUID uUID2 = playerStateStore.loadBossId(uUID);
        if (uUID2 == null) {
            return;
        }
        World world = ((EntityStore)store.getExternalData()).getWorld();
        if (world == null) {
            return;
        }
        Ref ref = world.getEntityRef(uUID2);
        if (ref == null || !ref.isValid()) {
            playerStateStore.saveBossId(uUID, null);
            return;
        }
        world.execute(() -> {
            Store store2 = ref.getStore();
            if (store2 == null || !ref.isValid()) {
                playerStateStore.saveBossId(uUID, null);
                return;
            }
            if (store2.getComponent(ref, this.bossMarkerType) == null) {
                store2.addComponent(ref, this.bossMarkerType, new BossWaveComponent(uUID));
            }
            if (store2.getComponent(ref, this.spawnedMarkerType) == null) {
                store2.addComponent(ref, this.spawnedMarkerType, new SpawnedByMobWaveComponent());
            }
            this.bossActive.put(uUID, true);
            this.bossRefs.put(uUID, (Ref<EntityStore>)ref);
        });
    }

    private boolean hasActiveWaveMobs(@Nonnull Store<EntityStore> store) {
        AtomicBoolean atomicBoolean = new AtomicBoolean(false);
        store.forEachChunk((Query)Query.and((Query[])new Query[]{this.spawnedMarkerType}), (archetypeChunk, commandBuffer) -> {
            if (atomicBoolean.get()) {
                return;
            }
            int n = archetypeChunk.size();
            for (int i = 0; i < n; ++i) {
                Ref ref = archetypeChunk.getReferenceTo(i);
                if (ref == null || !ref.isValid()) continue;
                atomicBoolean.set(true);
                return;
            }
        });
        return atomicBoolean.get();
    }

    @Nullable
    public Ref<EntityStore> getPlayerRef(@Nonnull UUID uUID) {
        Ref ref = (Ref)this.playerRefs.get(uUID);
        if (ref != null && ref.isValid()) {
            return ref;
        }
        UUID uUID2 = (UUID)this.ownerByPlayer.get(uUID);
        if (uUID2 != null) {
            return this.getAnyActivePlayerRef(uUID2);
        }
        return this.getAnyActivePlayerRef(uUID);
    }

    @Nonnull
    public List<Ref<EntityStore>> getPlayerRefsForOwner(@Nonnull UUID uUID) {
        ArrayList<Ref<EntityStore>> arrayList = new ArrayList<Ref<EntityStore>>();
        Set<UUID> set = (Set<UUID>)this.playersByOwner.get(uUID);
        if (set == null || set.isEmpty()) {
            return arrayList;
        }
        ArrayList<UUID> arrayList2 = null;
        for (UUID uUID2 : set) {
            Ref ref = (Ref)this.playerRefs.get(uUID2);
            if (ref == null || !ref.isValid()) {
                if (arrayList2 == null) {
                    arrayList2 = new ArrayList<UUID>();
                }
                arrayList2.add(uUID2);
                continue;
            }
            Store store = ref.getStore();
            if (store == null || !this.isGameplayWorld((Store<EntityStore>)store)) continue;
            arrayList.add((Ref<EntityStore>)ref);
        }
        if (arrayList2 != null) {
            for (UUID uUID2 : arrayList2) {
                set.remove(uUID2);
                this.playerRefs.remove(uUID2);
                this.ownerByPlayer.remove(uUID2, uUID);
            }
            if (set.isEmpty()) {
                this.playersByOwner.remove(uUID, set);
            }
        }
        return arrayList;
    }

    @Nonnull
    public List<Ref<EntityStore>> getPlayerRefs(@Nonnull UUID uUID) {
        ArrayList<Ref<EntityStore>> arrayList = new ArrayList<Ref<EntityStore>>();
        if (this.isTrackedPlayer(uUID)) {
            Ref ref = (Ref)this.playerRefs.get(uUID);
            if (ref != null && ref.isValid()) {
                arrayList.add((Ref<EntityStore>)ref);
            }
            return arrayList;
        }
        UUID uUID2 = this.resolveOwnerId(uUID);
        arrayList.addAll(this.getPlayerRefsForOwner(uUID2));
        return arrayList;
    }

    public boolean isTrackedPlayer(@Nonnull UUID uUID) {
        return this.playerRefs.containsKey(uUID) || this.ownerByPlayer.containsKey(uUID);
    }

    @Nonnull
    public UUID resolveOwnerId(@Nonnull UUID uUID) {
        UUID uUID2 = (UUID)this.ownerByPlayer.get(uUID);
        return uUID2 != null ? uUID2 : uUID;
    }

    public void startWavesForPlayer(@Nonnull Ref<EntityStore> ref) {
        if (!ref.isValid()) {
            return;
        }
        Store store = ref.getStore();
        if (store == null) {
            return;
        }
        World world = ((EntityStore)store.getExternalData()).getWorld();
        if (world == null || !world.isTicking()) {
            return;
        }
        try {
            world.execute(() -> {
                if (!ref.isValid()) {
                    return;
                }
                Store store2 = ref.getStore();
                if (store2 == null || !this.isGameplayWorld((Store<EntityStore>)store2)) {
                    return;
                }
                PlayerRef playerRef = (PlayerRef)store2.getComponent(ref, PlayerRef.getComponentType());
                if (playerRef == null) {
                    return;
                }
                UUID uUID = playerRef.getUuid();
                String string = MobWaveSpawner.getWorldKey((Store<EntityStore>)store2);
                UUID uUID2 = MobWaveSpawner.getSharedOwnerId(string);
                this.playerRefs.put(uUID, ref);
                this.assignPlayerToOwner(uUID, uUID2);
                Object object = this.getOwnerLock(uUID2);
                synchronized (object) {
                    this.worldByOwner.put(uUID2, string);
                    this.storeByOwner.put(uUID2, (Store<EntityStore>)store2);
                }
                object = this.stateStoreManager.getStore(string);
                this.cancelTask(uUID2);
                this.waveCounts.put(uUID2, 0);
                ((PlayerStateStore)object).saveWaveCount(uUID2, 0);
                ((PlayerStateStore)object).saveBossId(uUID2, null);
                this.bossActive.remove(uUID2);
                this.bossRefs.remove(uUID2);
                this.despawnTrackedWaveEntities((Store<EntityStore>)store2);
                this.rejoinHold.put(uUID2, false);
                this.rejoinHoldUntilByOwner.remove(uUID2);
                long l = System.currentTimeMillis();
                long l2 = l + ConfigManager.get().spawnIntervalMs;
                this.nextSpawnAt.put(uUID2, l2);
                ((PlayerStateStore)object).saveNextSpawnAt(uUID2, l2);
                int n = this.scheduleForOwner(uUID2);
                this.spawnWave(uUID2, n);
            });
        }
        catch (IllegalThreadStateException illegalThreadStateException) {
        }
    }

    private static String getWorldKey(@Nonnull Store<EntityStore> store) {
        String string;
        World world = ((EntityStore)store.getExternalData()).getWorld();
        if (world == null) {
            return "default";
        }
        if (world.getWorldConfig() != null && world.getWorldConfig().getUuid() != null) {
            return "world-" + world.getWorldConfig().getUuid().toString();
        }
        String string2 = string = world.getWorldConfig() != null ? world.getWorldConfig().getDisplayName() : null;
        if (string != null && !string.isBlank()) {
            return string.trim();
        }
        String string3 = world.getName();
        if (string3 != null && !string3.isBlank()) {
            return string3.trim();
        }
        return "default";
    }

    private List<String> getSpawnableRoles() {
        if (this.spawnableRoles.isEmpty()) {
            this.plugin.getLogger().at(Level.WARNING).log("No spawnable NPC roles configured; mobs will not spawn.");
        }
        return this.spawnableRoles;
    }

    @Nullable
    private Vector3d randomSpawnPosition(World world, Vector3d vector3d, ThreadLocalRandom threadLocalRandom) {
        ConfigManager.SpawnConfig spawnConfig = ConfigManager.get();
        double d = spawnConfig.minSpawnRadius;
        double d2 = Math.max(d, spawnConfig.maxSpawnRadius);
        for (int i = 0; i < 24; ++i) {
            double d3 = d + threadLocalRandom.nextDouble() * (d2 - d);
            double d4 = threadLocalRandom.nextDouble() * Math.PI * 2.0;
            double d5 = vector3d.getX() + Math.cos(d4) * d3;
            double d6 = vector3d.getZ() + Math.sin(d4) * d3;
            double d7 = vector3d.getY() + spawnConfig.spawnYOffset;
            Vector3d vector3d2 = new Vector3d(d5, d7, d6);
            if (this.isBlockedSpawnPosition(world, vector3d2)) continue;
            return vector3d2;
        }
        return null;
    }

    private boolean isBlockedSpawnPosition(@Nonnull World world, @Nonnull Vector3d vector3d) {
        int n;
        int n2;
        int n3 = (int)Math.floor(vector3d.getX());
        return this.isBlockedBlockType(world, n3, n2 = (int)Math.floor(vector3d.getY()), n = (int)Math.floor(vector3d.getZ())) || this.isBlockedBlockType(world, n3, n2 - 1, n);
    }

    private boolean isBlockedBlockType(@Nonnull World world, int n, int n2, int n3) {
        try {
            BlockType blockType = world.getBlockType(n, n2, n3);
            if (blockType == null) {
                return false;
            }
            String string = blockType.getId();
            return BLOCKED_SPAWN_BLOCK_ID.equalsIgnoreCase(string);
        }
        catch (RuntimeException runtimeException) {
            return false;
        }
    }

    private boolean isBossAlive(UUID uUID, Store<EntityStore> store) {
        Ref ref = (Ref)this.bossRefs.get(uUID);
        if (ref == null || !ref.isValid()) {
            return false;
        }
        NPCEntity nPCEntity = (NPCEntity)store.getComponent(ref, NPCEntity.getComponentType());
        return nPCEntity != null;
    }

    @Nonnull
    private static UUID getSharedOwnerId(@Nonnull String string) {
        return UUID.nameUUIDFromBytes(("shared-wave-owner:" + string).getBytes(StandardCharsets.UTF_8));
    }

    @Nonnull
    private Object getOwnerLock(@Nonnull UUID uUID2) {
        return this.ownerLocks.computeIfAbsent(uUID2, uUID -> new Object());
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void assignPlayerToOwner(@Nonnull UUID uUID2, @Nonnull UUID uUID3) {
        UUID uUID4 = this.ownerByPlayer.put(uUID2, uUID3);
        if (uUID4 != null && !uUID4.equals(uUID3) && this.removePlayerFromOwner(uUID4, uUID2)) {
            this.clearOwnerRuntimeState(uUID4);
        }
        Object object = this.getOwnerLock(uUID3);
        synchronized (object) {
            this.playersByOwner.computeIfAbsent(uUID3, uUID -> ConcurrentHashMap.newKeySet()).add(uUID2);
            this.participantsByOwner.computeIfAbsent(uUID3, uUID -> ConcurrentHashMap.newKeySet()).add(uUID2);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private boolean removePlayerFromOwner(@Nonnull UUID uUID, @Nonnull UUID uUID2) {
        Object object = this.getOwnerLock(uUID);
        synchronized (object) {
            Set<UUID> set = (Set<UUID>)this.playersByOwner.get(uUID);
            if (set == null) {
                return true;
            }
            set.remove(uUID2);
            if (!set.isEmpty()) {
                return false;
            }
            this.playersByOwner.remove(uUID, set);
            return true;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void clearOwnerRuntimeState(@Nonnull UUID uUID) {
        this.cancelTask(uUID);
        Object object = this.getOwnerLock(uUID);
        synchronized (object) {
            this.waveCounts.remove(uUID);
            this.bossActive.remove(uUID);
            this.bossRefs.remove(uUID);
            this.rejoinHold.remove(uUID);
            this.rejoinHoldUntilByOwner.remove(uUID);
            this.worldByOwner.remove(uUID);
            this.storeByOwner.remove(uUID);
            this.nextSpawnAt.remove(uUID);
            this.taskGenerationByOwner.remove(uUID);
        }
        this.ownerLocks.remove(uUID);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void resetOwnerProgress(@Nonnull UUID uUID, @Nullable Store<EntityStore> store) {
        Set<UUID> set;
        String string = store != null ? MobWaveSpawner.getWorldKey(store) : null;
        Ref<EntityStore> ref = null;
        Object object = this.getOwnerLock(uUID);
        synchronized (object) {
            if (string == null) {
                string = (String)this.worldByOwner.get(uUID);
            }
            set = (Set<UUID>)this.participantsByOwner.remove(uUID);
            if (store == null) {
                ref = (Ref<EntityStore>)this.bossRefs.get(uUID);
            }
        }
        if (string != null && set != null && !set.isEmpty() && HytaleMod.getInstance().getLifeEssenceLevelSystem() != null) {
            HytaleMod.getInstance().getLifeEssenceLevelSystem().resetProgressForPlayers(set, string);
        }
        if (string != null) {
            PlayerStateStore playerStateStore = this.stateStoreManager.getStore(string);
            playerStateStore.saveWaveCount(uUID, 0);
            playerStateStore.saveNextSpawnAt(uUID, 0L);
            playerStateStore.saveBossId(uUID, null);
        }
        this.nextSpawnAt.put(uUID, 0L);
        if (store == null && ref != null && ref.isValid()) {
            store = ref.getStore();
        }
        World world = store != null ? ((EntityStore)store.getExternalData()).getWorld() : null;
        if (world != null && world.isTicking()) {
            Store store2 = store;
            try {
                world.execute(() -> this.despawnTrackedWaveEntities((Store<EntityStore>)store2));
            }
            catch (IllegalThreadStateException illegalThreadStateException) {
            }
        }
        this.clearOwnerRuntimeState(uUID);
    }

    private void despawnTrackedWaveEntities(@Nonnull Store<EntityStore> store) {
        store.forEachChunk((Query)Query.and((Query[])new Query[]{this.spawnedMarkerType}), (archetypeChunk, commandBuffer) -> {
            int n = archetypeChunk.size();
            for (int i = 0; i < n; ++i) {
                Ref ref = archetypeChunk.getReferenceTo(i);
                if (ref == null || !ref.isValid()) continue;
                commandBuffer.removeEntity(ref, RemoveReason.REMOVE);
            }
        });
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Nullable
    private Ref<EntityStore> getAnyActivePlayerRef(@Nonnull UUID uUID) {
        Object object = this.getOwnerLock(uUID);
        synchronized (object) {
            Set<UUID> set = (Set<UUID>)this.playersByOwner.get(uUID);
            if (set == null || set.isEmpty()) {
                return null;
            }
            ArrayList<UUID> arrayList = null;
            for (UUID uUID2 : set) {
                Ref<EntityStore> ref = (Ref<EntityStore>)this.playerRefs.get(uUID2);
                if (ref == null || !ref.isValid()) {
                    if (arrayList == null) {
                        arrayList = new ArrayList<UUID>();
                    }
                    arrayList.add(uUID2);
                    continue;
                }
                Store store = ref.getStore();
                if (store == null || !this.isGameplayWorld((Store<EntityStore>)store)) continue;
                if (arrayList != null) {
                    for (UUID uUID3 : arrayList) {
                        set.remove(uUID3);
                        this.playerRefs.remove(uUID3);
                        this.ownerByPlayer.remove(uUID3, uUID);
                    }
                }
                return ref;
            }
            if (arrayList != null) {
                for (UUID uUID2 : arrayList) {
                    set.remove(uUID2);
                    this.playerRefs.remove(uUID2);
                    this.ownerByPlayer.remove(uUID2, uUID);
                }
                if (set.isEmpty()) {
                    this.playersByOwner.remove(uUID, set);
                }
            }
            return null;
        }
    }

    private void applyFixedBossHealth(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, float f, @Nonnull String string) {
        EntityStatMap entityStatMap = (EntityStatMap)store.getComponent(ref, EntityStatMap.getComponentType());
        if (entityStatMap == null) {
            return;
        }
        EntityStatValue entityStatValue = entityStatMap.get(DefaultEntityStatTypes.getHealth());
        if (entityStatValue == null) {
            return;
        }
        float f2 = entityStatValue.getMax();
        float f3 = f - f2;
        if (Math.abs(f3) < 0.01f) {
            return;
        }
        entityStatMap.putModifier(DefaultEntityStatTypes.getHealth(), string, (Modifier)new StaticModifier(Modifier.ModifierTarget.MAX, StaticModifier.CalculationType.ADDITIVE, f3));
        entityStatMap.maximizeStatValue(DefaultEntityStatTypes.getHealth());
    }

    @Nullable
    private ConfigManager.BossDefinition findBossForWave(@Nonnull ConfigManager.SpawnConfig spawnConfig, int n) {
        for (ConfigManager.BossDefinition bossDefinition : spawnConfig.bosses) {
            if (!bossDefinition.shouldSpawnAtWave(n)) continue;
            return bossDefinition;
        }
        return null;
    }
}
