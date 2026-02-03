package com.fadingtime.hytalemod;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.util.MathUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import it.unimi.dsi.fastutil.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.annotation.Nonnull;

/**
 * Spawns a wave of random NPCs around each player every 30 seconds.
 */
public class MobWaveSpawner {

    private final JavaPlugin plugin;
    private final com.hypixel.hytale.component.ComponentType<EntityStore, SpawnedByMobWaveComponent> spawnedMarkerType;
    private final com.hypixel.hytale.component.ComponentType<EntityStore, BossWaveComponent> bossMarkerType;
    private final ConcurrentMap<UUID, ScheduledFuture<?>> spawnTasks = new ConcurrentHashMap<>();
    private final List<String> spawnableRoles = new ArrayList<>(MobSpawnConfig.HOSTILE_ROLES);
    private final ConcurrentMap<UUID, Integer> waveCounts = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Boolean> bossActive = new ConcurrentHashMap<>();

    public MobWaveSpawner(
        @Nonnull JavaPlugin plugin,
        @Nonnull com.hypixel.hytale.component.ComponentType<EntityStore, SpawnedByMobWaveComponent> spawnedMarkerType,
        @Nonnull com.hypixel.hytale.component.ComponentType<EntityStore, BossWaveComponent> bossMarkerType
    ) {
        this.plugin = plugin;
        this.spawnedMarkerType = spawnedMarkerType;
        this.bossMarkerType = bossMarkerType;
        plugin.getEventRegistry().registerGlobal(PlayerReadyEvent.class, this::onPlayerReady);
        plugin.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, this::onPlayerDisconnect);
    }

    public void shutdown() {
        spawnTasks.values().forEach(task -> task.cancel(false));
        spawnTasks.clear();
    }

    private void onPlayerReady(@Nonnull PlayerReadyEvent event) {
        Ref<EntityStore> playerRef = event.getPlayerRef();
        UUID playerId = event.getPlayer().getUuid();
        Store<EntityStore> store = playerRef.getStore();
        if (store != null) {
            PlayerRef playerRefComponent = store.getComponent(playerRef, PlayerRef.getComponentType());
            if (playerRefComponent != null) {
                playerId = playerRefComponent.getUuid();
            }
        }
        scheduleForPlayer(playerId, playerRef);
        waveCounts.putIfAbsent(playerId, 0);
        bossActive.putIfAbsent(playerId, false);
    }

    private void onPlayerDisconnect(@Nonnull PlayerDisconnectEvent event) {
        UUID playerId = event.getPlayerRef().getUuid();
        cancelTask(playerId);
        waveCounts.remove(playerId);
        bossActive.remove(playerId);
    }

    private void scheduleForPlayer(UUID playerId, Ref<EntityStore> playerRef) {
        ScheduledFuture<?> task = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(
            () -> spawnWaveSafe(playerId, playerRef),
            0L,
            MobSpawnConfig.SPAWN_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        );

        ScheduledFuture<?> existing = spawnTasks.putIfAbsent(playerId, task);
        if (existing != null) {
            task.cancel(false);
        }
    }

    private void cancelTask(UUID playerId) {
        ScheduledFuture<?> task = spawnTasks.remove(playerId);
        if (task != null) {
            task.cancel(false);
        }
    }

    private void spawnWaveSafe(UUID playerId, Ref<EntityStore> playerRef) {
        try {
            spawnWave(playerId, playerRef);
        } catch (Exception e) {
            plugin.getLogger().at(Level.WARNING).withCause(e).log("Mob spawn tick failed for player %s", playerId);
        }
    }

    private void spawnWave(UUID playerId, Ref<EntityStore> playerRef) {
        if (!playerRef.isValid()) {
            cancelTask(playerId);
            return;
        }

        if (Boolean.TRUE.equals(bossActive.get(playerId))) {
            return;
        }

        Store<EntityStore> store = playerRef.getStore();
        if (store == null) {
            cancelTask(playerId);
            return;
        }

        World world = store.getExternalData().getWorld();
        world.execute(() -> {
            if (!playerRef.isValid()) {
                cancelTask(playerId);
                return;
            }

            TransformComponent transform = store.getComponent(playerRef, TransformComponent.getComponentType());
            if (transform == null) {
                return;
            }

            List<String> roles = getSpawnableRoles();
            if (roles.isEmpty()) {
                return;
            }

            Vector3d playerPos = transform.getPosition();
            ThreadLocalRandom random = ThreadLocalRandom.current();

            int wave = waveCounts.merge(playerId, 1, Integer::sum);
            if (wave % MobSpawnConfig.BOSS_WAVE_INTERVAL == 0) {
                spawnBossWave(playerId, store, world, playerPos, random);
                return;
            }

            for (int i = 0; i < MobSpawnConfig.SPAWN_COUNT; i++) {
                Vector3d spawnPos = randomSpawnPosition(world, playerPos, random);
                Vector3f rotation = new Vector3f(0.0F, (float) (random.nextDouble() * Math.PI * 2.0), 0.0F);
                String role = roles.get(random.nextInt(roles.size()));

                Pair<Ref<EntityStore>, ?> spawned = NPCPlugin.get().spawnNPC(store, role, null, spawnPos, rotation);
                if (spawned != null && spawned.first() != null && spawned.first().isValid()) {
                    store.addComponent(spawned.first(), this.spawnedMarkerType, new SpawnedByMobWaveComponent());
                }
            }
        });
    }

    private void spawnBossWave(UUID playerId, Store<EntityStore> store, World world, Vector3d playerPos, ThreadLocalRandom random) {
        Vector3d spawnPos = randomSpawnPosition(world, playerPos, random);
        Vector3f rotation = new Vector3f(0.0F, (float) (random.nextDouble() * Math.PI * 2.0), 0.0F);

        Pair<Ref<EntityStore>, ?> spawned = NPCPlugin.get().spawnNPC(store, MobSpawnConfig.BOSS_ROLE, null, spawnPos, rotation);
        if (spawned == null || spawned.first() == null || !spawned.first().isValid()) {
            return;
        }

        Ref<EntityStore> bossRef = spawned.first();
        store.addComponent(bossRef, this.spawnedMarkerType, new SpawnedByMobWaveComponent());
        store.addComponent(bossRef, this.bossMarkerType, new BossWaveComponent(playerId));
        bossActive.put(playerId, true);

        applyBossScale(bossRef, store, world, 0);
    }

    private void applyBossScale(Ref<EntityStore> bossRef, Store<EntityStore> store, World world, int attempt) {
        if (!bossRef.isValid()) {
            return;
        }

        NPCEntity npc = store.getComponent(bossRef, NPCEntity.getComponentType());
        if (npc == null || npc.getRole() == null) {
            retryBossScale(bossRef, store, world, attempt);
            return;
        }

        String appearance = npc.getRole().getAppearanceName();
        if (appearance == null || appearance.isEmpty()) {
            retryBossScale(bossRef, store, world, attempt);
            return;
        }

        ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(appearance);
        if (modelAsset == null) {
            retryBossScale(bossRef, store, world, attempt);
            return;
        }

        npc.setInitialModelScale(MobSpawnConfig.BOSS_SCALE);
        npc.setAppearance(bossRef, modelAsset, store);
    }

    private void retryBossScale(Ref<EntityStore> bossRef, Store<EntityStore> store, World world, int attempt) {
        if (attempt >= 3) {
            return;
        }
        HytaleServer.SCHEDULED_EXECUTOR.schedule(
            () -> world.execute(() -> applyBossScale(bossRef, store, world, attempt + 1)),
            50L,
            TimeUnit.MILLISECONDS
        );
    }

    public void notifyBossDefeated(UUID playerId) {
        bossActive.put(playerId, false);
    }

    private List<String> getSpawnableRoles() {
        if (this.spawnableRoles.isEmpty()) {
            plugin.getLogger().at(Level.WARNING).log("No spawnable NPC roles configured; mobs will not spawn.");
        }
        return this.spawnableRoles;
    }

    private Vector3d randomSpawnPosition(World world, Vector3d origin, ThreadLocalRandom random) {
        double radius = MobSpawnConfig.MIN_SPAWN_RADIUS
            + random.nextDouble() * (MobSpawnConfig.MAX_SPAWN_RADIUS - MobSpawnConfig.MIN_SPAWN_RADIUS);
        double angle = random.nextDouble() * Math.PI * 2.0;
        double x = origin.getX() + Math.cos(angle) * radius;
        double z = origin.getZ() + Math.sin(angle) * radius;
        double y = origin.getY() + MobSpawnConfig.SPAWN_Y_OFFSET;

        WorldChunk chunk = world.getNonTickingChunk(ChunkUtil.indexChunkFromBlock(x, z));
        if (chunk != null) {
            int bx = MathUtil.floor(x);
            int bz = MathUtil.floor(z);
            y = chunk.getHeight(bx, bz) + 1.0;
        }

        return new Vector3d(x, y, z);
    }
}
