package com.fadingtime.hytalemod.system;

import com.fadingtime.hytalemod.component.BossWaveComponent;
import com.fadingtime.hytalemod.spawner.MobWaveSpawner;
import com.fadingtime.hytalemod.ui.BossBarHud;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nullable;

public class BossHudSystem extends EntityTickingSystem<EntityStore> {
    private static final float UPDATE_EPSILON = 0.001f;

    private static final String DEFAULT_BOSS_NAME = "Giant Skeleton";
    private static final String TOAD_ROLE_NAME = "Toad_Rhino_Magma";
    private static final String TOAD_BOSS_NAME = "Magma Toad";
    private static final String SKELETON_ROLE_NAME = "Skeleton";
    private static final String WRAITH_BOSS_NAME = "Wraith";

    private final ComponentType<EntityStore, BossWaveComponent> bossComponentType;
    private final ComponentType<EntityStore, EntityStatMap> entityStatMapComponentType;
    private final ComponentType<EntityStore, DeathComponent> deathComponentType;
    private final Query<EntityStore> query;
    private final MobWaveSpawner mobWaveSpawner;

    private final ConcurrentMap<UUID, BossHudState> hudByPlayer = new ConcurrentHashMap<>();
    private final Set<UUID> suppressedPlayers = ConcurrentHashMap.newKeySet();

    public BossHudSystem(ComponentType<EntityStore, BossWaveComponent> bossComponentType, MobWaveSpawner mobWaveSpawner) {
        this.bossComponentType = bossComponentType;
        this.entityStatMapComponentType = EntityStatMap.getComponentType();
        this.deathComponentType = DeathComponent.getComponentType();
        this.query = Query.and(this.bossComponentType, this.entityStatMapComponentType);
        this.mobWaveSpawner = mobWaveSpawner;
    }

    public Query<EntityStore> getQuery() {
        return this.query;
    }

    public boolean isParallel(int archetypeChunkSize, int taskCount) {
        return false;
    }

    public void tick(float dt, int index, ArchetypeChunk<EntityStore> archetypeChunk, Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {
        World world = store.getExternalData().getWorld();
        if (world == null || !world.isTicking()) return;

        BossWaveComponent boss = (BossWaveComponent)archetypeChunk.getComponent(index, this.bossComponentType);
        if (boss == null || boss.getOwnerId() == null) return;

        var ownerId = this.mobWaveSpawner.resolveOwnerId(boss.getOwnerId());
        var bossRef = archetypeChunk.getReferenceTo(index);
        if (bossRef != null && (store.getComponent(bossRef, this.deathComponentType) != null || commandBuffer.getComponent(bossRef, this.deathComponentType) != null)) {
            this.clearBossHud(ownerId);
            return;
        }

        var stats = (EntityStatMap)archetypeChunk.getComponent(index, this.entityStatMapComponentType);
        var hp = stats != null ? stats.get(DefaultEntityStatTypes.getHealth()) : null;
        if (hp == null) return;

        var ratio = hp.asPercentage();
        ratio = Float.isFinite(ratio) ? Math.max(0.0f, Math.min(1.0f, ratio)) : 0.0f;
        var bossName = resolveBossName(store, bossRef);
        if (bossName == null || bossName.isBlank()) bossName = DEFAULT_BOSS_NAME;

        var players = this.mobWaveSpawner.getPlayerRefsForOwner(ownerId);
        if (players.isEmpty()) return;
        for (var playerRef : players) {
            this.updateHudForPlayer(ownerId, playerRef, bossName, ratio);
        }
    }

    private void updateHudForPlayer(UUID ownerId, Ref<EntityStore> playerRef, String bossName, float ratio) {
        if (!playerRef.isValid()) return;
        var playerStore = (Store<EntityStore>)playerRef.getStore();
        if (playerStore == null) return;
        var playerComponent = (Player)playerStore.getComponent(playerRef, Player.getComponentType());
        var playerInfo = (PlayerRef)playerStore.getComponent(playerRef, PlayerRef.getComponentType());
        if (playerComponent == null || playerInfo == null) return;

        var playerId = playerInfo.getUuid();
        if (this.suppressedPlayers.contains(playerId)) return;

        var state = this.hudByPlayer.computeIfAbsent(playerId, id -> new BossHudState());
        state.ownerId = ownerId;

        var currentHud = playerComponent.getHudManager().getCustomHud();
        if (currentHud instanceof BossBarHud bossHud) {
            if (state.hud != bossHud) {
                state.hud = bossHud;
                state.lastRatio = -1.0f;
                state.lastName = null;
                state.hidden = false;
            }
        } else if (state.hud != null && currentHud != state.hud) {
            state.hud = null;
            state.lastRatio = -1.0f;
            state.lastName = null;
            state.hidden = false;
        }

        if (state.hud == null) {
            if (currentHud != null && !(currentHud instanceof BossBarHud)) return;
            state.hud = new BossBarHud(playerInfo, bossName, ratio);
            state.lastRatio = ratio;
            state.lastName = bossName;
            state.hidden = false;
            playerComponent.getHudManager().setCustomHud(playerInfo, state.hud);
            state.hud.setBossVisible(true);
            return;
        }

        if (!state.hud.isBossVisible()) {
            state.hud.setBossVisible(true);
        }
        if (state.hidden) {
            state.hud.setBossVisible(true);
            state.hud.updateBossName(bossName);
            state.hud.updateHealth(ratio);
            state.lastRatio = ratio;
            state.lastName = bossName;
            state.hidden = false;
            return;
        }
        if (state.lastName == null || !state.lastName.equals(bossName)) {
            state.hud.updateBossName(bossName);
            state.lastName = bossName;
        }
        if (Math.abs(ratio - state.lastRatio) >= UPDATE_EPSILON) {
            state.hud.updateHealth(ratio);
            state.lastRatio = ratio;
        }
    }

    public void hideBossHud(UUID ownerOrPlayerId) {
        if (this.mobWaveSpawner.isTrackedPlayer(ownerOrPlayerId)) {
            Ref<EntityStore> playerRef = this.mobWaveSpawner.getPlayerRef(ownerOrPlayerId);
            if (playerRef != null) {
                this.hideBossHudForPlayer(playerRef);
            }
            return;
        }

        UUID ownerId = this.mobWaveSpawner.resolveOwnerId(ownerOrPlayerId);
        for (Ref<EntityStore> playerRef : this.mobWaveSpawner.getPlayerRefsForOwner(ownerId)) {
            this.hideBossHudForPlayer(playerRef);
        }
    }

    private void hideBossHudForPlayer(Ref<EntityStore> playerRef) {
        if (!playerRef.isValid()) return;
        var store = (Store<EntityStore>)playerRef.getStore();
        if (store == null) return;
        var playerRefComponent = (PlayerRef)store.getComponent(playerRef, PlayerRef.getComponentType());
        if (playerRefComponent == null) return;
        var playerId = playerRefComponent.getUuid();
        var state = this.hudByPlayer.get(playerId);
        var playerComponent = (Player)store.getComponent(playerRef, Player.getComponentType());
        if (state == null || state.hud == null || playerComponent == null) return;
        if (playerComponent.getHudManager().getCustomHud() != state.hud) return;
        state.hud.setBossVisible(false);
        state.hud.setLevelHudVisible(false);
        state.hidden = true;
    }

    public void clearBossHud(UUID ownerOrPlayerId) {
        if (this.mobWaveSpawner.isTrackedPlayer(ownerOrPlayerId)) {
            Ref<EntityStore> playerRef = this.mobWaveSpawner.getPlayerRef(ownerOrPlayerId);
            if (playerRef != null) {
                this.clearBossHudForPlayer(playerRef);
            } else {
                this.hudByPlayer.remove(ownerOrPlayerId);
            }
            return;
        }

        UUID ownerId = this.mobWaveSpawner.resolveOwnerId(ownerOrPlayerId);
        List<Ref<EntityStore>> playerRefs = this.mobWaveSpawner.getPlayerRefsForOwner(ownerId);
        if (playerRefs.isEmpty()) {
            for (var entry : this.hudByPlayer.entrySet()) {
                var state = entry.getValue();
                if (state == null || state.ownerId == null || !ownerId.equals(state.ownerId)) {
                    continue;
                }
                this.hudByPlayer.remove(entry.getKey(), state);
            }
            return;
        }

        for (var playerRef : playerRefs) {
            this.clearBossHudForPlayer(playerRef);
        }
    }

    private void clearBossHudForPlayer(Ref<EntityStore> playerRef) {
        if (!playerRef.isValid()) return;
        var store = (Store<EntityStore>)playerRef.getStore();
        if (store == null) return;
        var playerRefComponent = (PlayerRef)store.getComponent(playerRef, PlayerRef.getComponentType());
        if (playerRefComponent == null) return;
        var playerId = playerRefComponent.getUuid();
        var state = this.hudByPlayer.remove(playerId);
        var playerComponent = (Player)store.getComponent(playerRef, Player.getComponentType());
        if (playerComponent == null) return;
        if (state != null && state.hud != null) {
            if (playerComponent.getHudManager().getCustomHud() == state.hud) {
                state.hud.setBossVisible(false);
                state.hud.setLevelHudVisible(false);
            }
            return;
        }

        var currentHud = playerComponent.getHudManager().getCustomHud();
        if (currentHud instanceof BossBarHud bossHud) {
            bossHud.setBossVisible(false);
            bossHud.setLevelHudVisible(false);
        }
    }

    public void setHudSuppressed(UUID ownerOrPlayerId, boolean suppressed) {
        if (this.mobWaveSpawner.isTrackedPlayer(ownerOrPlayerId)) {
            if (suppressed) {
                this.suppressedPlayers.add(ownerOrPlayerId);
            } else {
                this.suppressedPlayers.remove(ownerOrPlayerId);
            }
            return;
        }
        var ownerId = this.mobWaveSpawner.resolveOwnerId(ownerOrPlayerId);
        for (var playerRef : this.mobWaveSpawner.getPlayerRefsForOwner(ownerId)) {
            var store = (Store<EntityStore>)playerRef.getStore();
            if (store == null) {
                continue;
            }
            var playerRefComponent = (PlayerRef)store.getComponent(playerRef, PlayerRef.getComponentType());
            if (playerRefComponent == null) {
                continue;
            }
            var playerId = playerRefComponent.getUuid();
            if (suppressed) {
                this.suppressedPlayers.add(playerId);
            } else {
                this.suppressedPlayers.remove(playerId);
            }
        }
    }

    @Nullable
    private static String resolveBossName(Store<EntityStore> store, @Nullable Ref<EntityStore> bossRef) {
        if (bossRef == null || !bossRef.isValid()) return null;
        var npc = (NPCEntity)store.getComponent(bossRef, NPCEntity.getComponentType());
        if (npc == null) return null;
        var role = npc.getRoleName();
        if (role == null) return null;
        return switch (role) {
            case TOAD_ROLE_NAME -> TOAD_BOSS_NAME;
            case WRAITH_BOSS_NAME -> WRAITH_BOSS_NAME;
            case SKELETON_ROLE_NAME -> DEFAULT_BOSS_NAME;
            default -> null;
        };
    }

    private static class BossHudState {
        @Nullable
        private BossBarHud hud;
        @Nullable
        private UUID ownerId;
        private float lastRatio = -1.0f;
        @Nullable
        private String lastName;
        private boolean hidden;
    }
}
