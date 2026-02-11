package com.fadingtime.hytalemod.system;

import com.fadingtime.hytalemod.HytaleMod;
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
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BossHudSystem
extends EntityTickingSystem<EntityStore> {
    private static final float UPDATE_EPSILON = 0.001f;
    private static final String DEFAULT_BOSS_NAME = "Giant Skeleton";
    private static final String TOAD_ROLE_NAME = "Toad_Rhino_Magma";
    private static final String TOAD_BOSS_NAME = "Magma Toad";
    private static final String SKELETON_ROLE_NAME = "Skeleton";
    private static final String WRAITH_BOSS_NAME = "Wraith";
    private static final boolean HUD_DEBUG = true;
    private static final long HUD_LOG_INTERVAL_MS = 500L;
    @Nonnull
    private final ComponentType<EntityStore, BossWaveComponent> bossComponentType;
    @Nonnull
    private final ComponentType<EntityStore, EntityStatMap> entityStatMapComponentType;
    @Nonnull
    private final ComponentType<EntityStore, DeathComponent> deathComponentType;
    @Nonnull
    private final Query<EntityStore> query;
    @Nonnull
    private final MobWaveSpawner mobWaveSpawner;
    @Nonnull
    private final ConcurrentMap<UUID, BossHudState> hudByPlayer = new ConcurrentHashMap<UUID, BossHudState>();
    @Nonnull
    private final Set<UUID> suppressedPlayers = ConcurrentHashMap.newKeySet();
    @Nonnull
    private final ConcurrentMap<UUID, Long> lastHudLog = new ConcurrentHashMap<UUID, Long>();

    public BossHudSystem(@Nonnull ComponentType<EntityStore, BossWaveComponent> bossComponentType, @Nonnull MobWaveSpawner mobWaveSpawner) {
        this.bossComponentType = bossComponentType;
        this.entityStatMapComponentType = EntityStatMap.getComponentType();
        this.deathComponentType = DeathComponent.getComponentType();
        this.query = Query.and((Query[])new Query[]{this.bossComponentType, this.entityStatMapComponentType});
        this.mobWaveSpawner = mobWaveSpawner;
    }

    @Nonnull
    public Query<EntityStore> getQuery() {
        return this.query;
    }

    public boolean isParallel(int archetypeChunkSize, int taskCount) {
        return false;
    }

    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        World world = store.getExternalData().getWorld();
        if (world == null || !world.isTicking()) {
            return;
        }
        BossWaveComponent bossComponent = (BossWaveComponent)archetypeChunk.getComponent(index, this.bossComponentType);
        if (bossComponent == null || bossComponent.getOwnerId() == null) {
            return;
        }
        UUID ownerId = this.normalizeOwnerId(bossComponent.getOwnerId());
        Ref<EntityStore> bossRef = archetypeChunk.getReferenceTo(index);
        if (bossRef != null && (store.getComponent(bossRef, this.deathComponentType) != null || commandBuffer.getComponent(bossRef, this.deathComponentType) != null)) {
            this.clearBossHud(ownerId);
            return;
        }
        EntityStatMap statMap = (EntityStatMap)archetypeChunk.getComponent(index, this.entityStatMapComponentType);
        if (statMap == null) {
            return;
        }
        EntityStatValue healthStat = statMap.get(DefaultEntityStatTypes.getHealth());
        if (healthStat == null) {
            return;
        }
        float ratio = BossHudSystem.clamp01(healthStat.asPercentage());
        String bossName = BossHudSystem.resolveBossName(store, bossRef);
        if (bossName == null || bossName.isBlank()) {
            bossName = DEFAULT_BOSS_NAME;
        }
        List<Ref<EntityStore>> playerRefs = this.mobWaveSpawner.getPlayerRefsForOwner(ownerId);
        if (playerRefs.isEmpty()) {
            return;
        }
        for (Ref<EntityStore> playerRef : playerRefs) {
            this.updateHudForPlayer(ownerId, playerRef, bossName, ratio);
        }
    }

    private void updateHudForPlayer(@Nonnull UUID ownerId, @Nonnull Ref<EntityStore> playerRef, @Nonnull String bossName, float ratio) {
        if (!playerRef.isValid()) {
            return;
        }
        Store<EntityStore> playerStore = (Store<EntityStore>)playerRef.getStore();
        if (playerStore == null) {
            return;
        }
        Player playerComponent = (Player)playerStore.getComponent(playerRef, Player.getComponentType());
        if (playerComponent == null) {
            return;
        }
        PlayerRef playerRefComponent = (PlayerRef)playerStore.getComponent(playerRef, PlayerRef.getComponentType());
        if (playerRefComponent == null) {
            return;
        }
        UUID playerId = playerRefComponent.getUuid();
        if (this.suppressedPlayers.contains(playerId)) {
            return;
        }
        BossHudState state = this.hudByPlayer.computeIfAbsent(playerId, id -> new BossHudState());
        state.ownerId = ownerId;
        CustomUIHud currentHud = playerComponent.getHudManager().getCustomHud();
        if (currentHud instanceof BossBarHud) {
            if (state.hud != currentHud) {
                state.hud = (BossBarHud)currentHud;
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
            if (currentHud != null && !(currentHud instanceof BossBarHud)) {
                return;
            }
            state.hud = new BossBarHud(playerRefComponent, bossName, ratio);
            state.lastRatio = ratio;
            state.lastName = bossName;
            state.hidden = false;
            this.logHud(ownerId, "HUD_CREATE player=%s name=%s ratio=%.3f world=%s", playerId, bossName, Float.valueOf(ratio), BossHudSystem.getWorldName(playerStore));
            playerComponent.getHudManager().setCustomHud(playerRefComponent, (CustomUIHud)state.hud);
            state.hud.setBossVisible(true);
            this.logHud(ownerId, "HUD_VISIBLE true player=%s (create)", playerId);
            return;
        }
        if (!state.hud.isBossVisible()) {
            state.hud.setBossVisible(true);
            this.logHud(ownerId, "HUD_VISIBLE true player=%s", playerId);
        }
        if (state.hidden) {
            state.hud.setBossVisible(true);
            state.hud.updateBossName(bossName);
            state.hud.updateHealth(ratio);
            state.lastRatio = ratio;
            state.lastName = bossName;
            state.hidden = false;
            this.logHud(ownerId, "HUD_SHOW player=%s name=%s ratio=%.3f", playerId, bossName, Float.valueOf(ratio));
            return;
        }
        if (state.lastName == null || !state.lastName.equals(bossName)) {
            state.hud.updateBossName(bossName);
            state.lastName = bossName;
            this.logHud(ownerId, "HUD_NAME player=%s name=%s", playerId, bossName);
        }
        if (Math.abs(ratio - state.lastRatio) >= UPDATE_EPSILON) {
            state.hud.updateHealth(ratio);
            state.lastRatio = ratio;
            this.logHudThrottled(ownerId, "HUD_HEALTH player=%s ratio=%.3f", playerId, Float.valueOf(ratio));
        }
    }

    public void hideBossHud(@Nonnull UUID ownerOrPlayerId) {
        UUID ownerId = this.normalizeOwnerId(ownerOrPlayerId);
        if (this.mobWaveSpawner.isTrackedPlayer(ownerOrPlayerId)) {
            Ref<EntityStore> playerRef = this.mobWaveSpawner.getPlayerRef(ownerOrPlayerId);
            if (playerRef != null) {
                this.hideBossHudForPlayer(ownerId, playerRef);
            }
            return;
        }
        for (Ref<EntityStore> playerRef : this.mobWaveSpawner.getPlayerRefsForOwner(ownerId)) {
            this.hideBossHudForPlayer(ownerId, playerRef);
        }
    }

    private void hideBossHudForPlayer(@Nonnull UUID ownerId, @Nonnull Ref<EntityStore> playerRef) {
        if (!playerRef.isValid()) {
            return;
        }
        Store<EntityStore> store = (Store<EntityStore>)playerRef.getStore();
        if (store == null) {
            return;
        }
        PlayerRef playerRefComponent = (PlayerRef)store.getComponent(playerRef, PlayerRef.getComponentType());
        if (playerRefComponent == null) {
            return;
        }
        UUID playerId = playerRefComponent.getUuid();
        BossHudState state = (BossHudState)this.hudByPlayer.get(playerId);
        if (state == null || state.hud == null) {
            return;
        }
        Player playerComponent = (Player)store.getComponent(playerRef, Player.getComponentType());
        if (playerComponent == null) {
            return;
        }
        if (playerComponent.getHudManager().getCustomHud() != state.hud) {
            return;
        }
        state.hud.setBossVisible(false);
        state.hud.setLevelHudVisible(false);
        state.hidden = true;
        this.logHud(ownerId, "HUD_HIDE applied player=%s", playerId);
    }

    public void clearBossHud(@Nonnull UUID ownerOrPlayerId) {
        UUID ownerId = this.normalizeOwnerId(ownerOrPlayerId);
        this.logHud(ownerId, "HUD_CLEAR request");
        if (this.mobWaveSpawner.isTrackedPlayer(ownerOrPlayerId)) {
            Ref<EntityStore> playerRef = this.mobWaveSpawner.getPlayerRef(ownerOrPlayerId);
            if (playerRef != null) {
                this.clearBossHudForPlayer(ownerId, playerRef);
            } else {
                this.hudByPlayer.remove(ownerOrPlayerId);
            }
            return;
        }
        List<Ref<EntityStore>> playerRefs = this.mobWaveSpawner.getPlayerRefsForOwner(ownerId);
        if (playerRefs.isEmpty()) {
            for (Map.Entry<UUID, BossHudState> entry : this.hudByPlayer.entrySet()) {
                BossHudState state = entry.getValue();
                if (state == null || state.ownerId == null || !ownerId.equals(state.ownerId)) continue;
                this.hudByPlayer.remove(entry.getKey(), state);
            }
            return;
        }
        for (Ref<EntityStore> playerRef : playerRefs) {
            this.clearBossHudForPlayer(ownerId, playerRef);
        }
    }

    private void clearBossHudForPlayer(@Nonnull UUID ownerId, @Nonnull Ref<EntityStore> playerRef) {
        if (!playerRef.isValid()) {
            return;
        }
        Store<EntityStore> store = (Store<EntityStore>)playerRef.getStore();
        if (store == null) {
            return;
        }
        PlayerRef playerRefComponent = (PlayerRef)store.getComponent(playerRef, PlayerRef.getComponentType());
        if (playerRefComponent == null) {
            return;
        }
        UUID playerId = playerRefComponent.getUuid();
        BossHudState state = (BossHudState)this.hudByPlayer.remove(playerId);
        Player playerComponent = (Player)store.getComponent(playerRef, Player.getComponentType());
        if (playerComponent == null) {
            return;
        }
        if (state != null && state.hud != null) {
            if (playerComponent.getHudManager().getCustomHud() == state.hud) {
                state.hud.setBossVisible(false);
                state.hud.setLevelHudVisible(false);
                this.logHud(ownerId, "HUD_CLEAR applied state.hud player=%s", playerId);
            }
            return;
        }
        CustomUIHud currentHud = playerComponent.getHudManager().getCustomHud();
        if (currentHud instanceof BossBarHud) {
            ((BossBarHud)currentHud).setBossVisible(false);
            ((BossBarHud)currentHud).setLevelHudVisible(false);
            this.logHud(ownerId, "HUD_CLEAR applied currentHud player=%s", playerId);
        }
    }

    public void setHudSuppressed(@Nonnull UUID ownerOrPlayerId, boolean suppressed) {
        if (this.mobWaveSpawner.isTrackedPlayer(ownerOrPlayerId)) {
            if (suppressed) {
                this.suppressedPlayers.add(ownerOrPlayerId);
            } else {
                this.suppressedPlayers.remove(ownerOrPlayerId);
            }
            return;
        }
        UUID ownerId = this.normalizeOwnerId(ownerOrPlayerId);
        for (Ref<EntityStore> playerRef : this.mobWaveSpawner.getPlayerRefsForOwner(ownerId)) {
            Store<EntityStore> store = (Store<EntityStore>)playerRef.getStore();
            if (store == null) continue;
            PlayerRef playerRefComponent = (PlayerRef)store.getComponent(playerRef, PlayerRef.getComponentType());
            if (playerRefComponent == null) continue;
            UUID playerId = playerRefComponent.getUuid();
            if (suppressed) {
                this.suppressedPlayers.add(playerId);
                continue;
            }
            this.suppressedPlayers.remove(playerId);
        }
    }

    private void logHud(@Nonnull UUID ownerId, @Nonnull String format, Object ... args) {
        if (!HUD_DEBUG) {
            return;
        }
        HytaleMod.LOGGER.log(Level.INFO, "[HUD_DEBUG][" + ownerId + "] " + String.format(format, args));
    }

    private void logHudThrottled(@Nonnull UUID ownerId, @Nonnull String format, Object ... args) {
        if (!this.shouldLog(ownerId, HUD_LOG_INTERVAL_MS)) {
            return;
        }
        this.logHud(ownerId, format, args);
    }

    private boolean shouldLog(@Nonnull UUID ownerId, long intervalMs) {
        long now = System.currentTimeMillis();
        Long last = this.lastHudLog.get(ownerId);
        if (last == null || now - last.longValue() >= intervalMs) {
            this.lastHudLog.put(ownerId, now);
            return true;
        }
        return false;
    }

    @Nonnull
    private UUID normalizeOwnerId(@Nonnull UUID ownerOrPlayerId) {
        return this.mobWaveSpawner.resolveOwnerId(ownerOrPlayerId);
    }

    private static String getWorldName(@Nullable Store<EntityStore> store) {
        if (store == null) {
            return "null";
        }
        World world = store.getExternalData().getWorld();
        if (world == null) {
            return "null";
        }
        String name = world.getName();
        if (name != null && !name.isBlank()) {
            return name;
        }
        return Objects.toString(world.getWorldConfig() != null ? world.getWorldConfig().getDisplayName() : null, "unknown");
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) {
            return 0.0f;
        }
        if (value < 0.0f) {
            return 0.0f;
        }
        if (value > 1.0f) {
            return 1.0f;
        }
        return value;
    }

    @Nullable
    private static String resolveBossName(@Nonnull Store<EntityStore> store, @Nullable Ref<EntityStore> bossRef) {
        if (bossRef == null || !bossRef.isValid()) {
            return null;
        }
        NPCEntity npc = (NPCEntity)store.getComponent(bossRef, NPCEntity.getComponentType());
        if (npc == null) {
            return null;
        }
        String roleName = npc.getRoleName();
        if (TOAD_ROLE_NAME.equals(roleName)) {
            return TOAD_BOSS_NAME;
        }
        if (WRAITH_BOSS_NAME.equals(roleName)) {
            return WRAITH_BOSS_NAME;
        }
        if (SKELETON_ROLE_NAME.equals(roleName)) {
            return DEFAULT_BOSS_NAME;
        }
        return null;
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

        private BossHudState() {
        }
    }
}
