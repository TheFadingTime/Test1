/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.ArchetypeChunk
 *  com.hypixel.hytale.component.CommandBuffer
 *  com.hypixel.hytale.component.ComponentType
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.component.query.Query
 *  com.hypixel.hytale.component.system.tick.EntityTickingSystem
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud
 *  com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent
 *  com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap
 *  com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue
 *  com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.world.World
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 *  com.hypixel.hytale.server.npc.entities.NPCEntity
 *  javax.annotation.Nonnull
 *  javax.annotation.Nullable
 */
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

    public BossHudSystem(@Nonnull ComponentType<EntityStore, BossWaveComponent> componentType, @Nonnull MobWaveSpawner mobWaveSpawner) {
        this.bossComponentType = componentType;
        this.entityStatMapComponentType = EntityStatMap.getComponentType();
        this.deathComponentType = DeathComponent.getComponentType();
        this.query = Query.and((Query[])new Query[]{this.bossComponentType, this.entityStatMapComponentType});
        this.mobWaveSpawner = mobWaveSpawner;
    }

    @Nonnull
    public Query<EntityStore> getQuery() {
        return this.query;
    }

    public boolean isParallel(int n, int n2) {
        return false;
    }

    public void tick(float f, int n, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        List<Ref<EntityStore>> list;
        World world = ((EntityStore)store.getExternalData()).getWorld();
        if (world == null || !world.isTicking()) {
            return;
        }
        BossWaveComponent bossWaveComponent = (BossWaveComponent)archetypeChunk.getComponent(n, this.bossComponentType);
        if (bossWaveComponent == null || bossWaveComponent.getOwnerId() == null) {
            return;
        }
        UUID uUID = this.normalizeOwnerId(bossWaveComponent.getOwnerId());
        Ref ref = archetypeChunk.getReferenceTo(n);
        if (ref != null && (store.getComponent(ref, this.deathComponentType) != null || commandBuffer.getComponent(ref, this.deathComponentType) != null)) {
            this.clearBossHud(uUID);
            return;
        }
        EntityStatMap entityStatMap = (EntityStatMap)archetypeChunk.getComponent(n, this.entityStatMapComponentType);
        if (entityStatMap == null) {
            return;
        }
        EntityStatValue entityStatValue = entityStatMap.get(DefaultEntityStatTypes.getHealth());
        if (entityStatValue == null) {
            return;
        }
        float f2 = BossHudSystem.clamp01(entityStatValue.asPercentage());
        String string = BossHudSystem.resolveBossName(store, (Ref<EntityStore>)ref);
        if (string == null || string.isBlank()) {
            string = DEFAULT_BOSS_NAME;
        }
        if ((list = this.mobWaveSpawner.getPlayerRefsForOwner(uUID)).isEmpty()) {
            return;
        }
        for (Ref<EntityStore> ref2 : list) {
            this.updateHudForPlayer(uUID, ref2, string, f2);
        }
    }

    private void updateHudForPlayer(@Nonnull UUID uUID2, @Nonnull Ref<EntityStore> ref, @Nonnull String string, float f) {
        if (!ref.isValid()) {
            return;
        }
        Store store = ref.getStore();
        if (store == null) {
            return;
        }
        Player player = (Player)store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        PlayerRef playerRef = (PlayerRef)store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }
        UUID uUID3 = playerRef.getUuid();
        if (this.suppressedPlayers.contains(uUID3)) {
            return;
        }
        BossHudState bossHudState = this.hudByPlayer.computeIfAbsent(uUID3, uUID -> new BossHudState());
        bossHudState.ownerId = uUID2;
        CustomUIHud customUIHud = player.getHudManager().getCustomHud();
        if (customUIHud instanceof BossBarHud) {
            if (bossHudState.hud != customUIHud) {
                bossHudState.hud = (BossBarHud)customUIHud;
                bossHudState.lastRatio = -1.0f;
                bossHudState.lastName = null;
                bossHudState.hidden = false;
            }
        } else if (bossHudState.hud != null && customUIHud != bossHudState.hud) {
            bossHudState.hud = null;
            bossHudState.lastRatio = -1.0f;
            bossHudState.lastName = null;
            bossHudState.hidden = false;
        }
        if (bossHudState.hud == null) {
            if (customUIHud != null && !(customUIHud instanceof BossBarHud)) {
                return;
            }
            bossHudState.hud = new BossBarHud(playerRef, string, f);
            bossHudState.lastRatio = f;
            bossHudState.lastName = string;
            bossHudState.hidden = false;
            this.logHud(uUID2, "HUD_CREATE player=%s name=%s ratio=%.3f world=%s", uUID3, string, Float.valueOf(f), BossHudSystem.getWorldName((Store<EntityStore>)store));
            player.getHudManager().setCustomHud(playerRef, (CustomUIHud)bossHudState.hud);
            bossHudState.hud.setBossVisible(true);
            this.logHud(uUID2, "HUD_VISIBLE true player=%s (create)", uUID3);
            return;
        }
        if (!bossHudState.hud.isBossVisible()) {
            bossHudState.hud.setBossVisible(true);
            this.logHud(uUID2, "HUD_VISIBLE true player=%s", uUID3);
        }
        if (bossHudState.hidden) {
            bossHudState.hud.setBossVisible(true);
            bossHudState.hud.updateBossName(string);
            bossHudState.hud.updateHealth(f);
            bossHudState.lastRatio = f;
            bossHudState.lastName = string;
            bossHudState.hidden = false;
            this.logHud(uUID2, "HUD_SHOW player=%s name=%s ratio=%.3f", uUID3, string, Float.valueOf(f));
            return;
        }
        if (bossHudState.lastName == null || !bossHudState.lastName.equals(string)) {
            bossHudState.hud.updateBossName(string);
            bossHudState.lastName = string;
            this.logHud(uUID2, "HUD_NAME player=%s name=%s", uUID3, string);
        }
        if (Math.abs(f - bossHudState.lastRatio) >= 0.001f) {
            bossHudState.hud.updateHealth(f);
            bossHudState.lastRatio = f;
            this.logHudThrottled(uUID2, "HUD_HEALTH player=%s ratio=%.3f", uUID3, Float.valueOf(f));
        }
    }

    public void hideBossHud(@Nonnull UUID uUID) {
        UUID uUID2 = this.normalizeOwnerId(uUID);
        if (this.mobWaveSpawner.isTrackedPlayer(uUID)) {
            Ref<EntityStore> ref = this.mobWaveSpawner.getPlayerRef(uUID);
            if (ref != null) {
                this.hideBossHudForPlayer(uUID2, ref);
            }
            return;
        }
        for (Ref<EntityStore> ref : this.mobWaveSpawner.getPlayerRefsForOwner(uUID2)) {
            this.hideBossHudForPlayer(uUID2, ref);
        }
    }

    private void hideBossHudForPlayer(@Nonnull UUID uUID, @Nonnull Ref<EntityStore> ref) {
        if (!ref.isValid()) {
            return;
        }
        Store store = ref.getStore();
        if (store == null) {
            return;
        }
        PlayerRef playerRef = (PlayerRef)store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }
        UUID uUID2 = playerRef.getUuid();
        BossHudState bossHudState = (BossHudState)this.hudByPlayer.get(uUID2);
        if (bossHudState == null || bossHudState.hud == null) {
            return;
        }
        Player player = (Player)store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        if (player.getHudManager().getCustomHud() != bossHudState.hud) {
            return;
        }
        bossHudState.hud.setBossVisible(false);
        bossHudState.hud.setLevelHudVisible(false);
        bossHudState.hidden = true;
        this.logHud(uUID, "HUD_HIDE applied player=%s", uUID2);
    }

    public void clearBossHud(@Nonnull UUID uUID) {
        UUID uUID2 = this.normalizeOwnerId(uUID);
        this.logHud(uUID2, "HUD_CLEAR request", new Object[0]);
        if (this.mobWaveSpawner.isTrackedPlayer(uUID)) {
            Ref<EntityStore> ref = this.mobWaveSpawner.getPlayerRef(uUID);
            if (ref != null) {
                this.clearBossHudForPlayer(uUID2, ref);
            } else {
                this.hudByPlayer.remove(uUID);
            }
            return;
        }
        List<Ref<EntityStore>> list = this.mobWaveSpawner.getPlayerRefsForOwner(uUID2);
        if (list.isEmpty()) {
            for (Map.Entry entry : this.hudByPlayer.entrySet()) {
                BossHudState bossHudState = (BossHudState)entry.getValue();
                if (bossHudState == null || bossHudState.ownerId == null || !uUID2.equals(bossHudState.ownerId)) continue;
                this.hudByPlayer.remove(entry.getKey(), bossHudState);
            }
            return;
        }
        for (Ref<EntityStore> ref : list) {
            this.clearBossHudForPlayer(uUID2, ref);
        }
    }

    private void clearBossHudForPlayer(@Nonnull UUID uUID, @Nonnull Ref<EntityStore> ref) {
        if (!ref.isValid()) {
            return;
        }
        Store store = ref.getStore();
        if (store == null) {
            return;
        }
        PlayerRef playerRef = (PlayerRef)store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }
        UUID uUID2 = playerRef.getUuid();
        BossHudState bossHudState = (BossHudState)this.hudByPlayer.remove(uUID2);
        Player player = (Player)store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        if (bossHudState != null && bossHudState.hud != null) {
            if (player.getHudManager().getCustomHud() == bossHudState.hud) {
                bossHudState.hud.setBossVisible(false);
                bossHudState.hud.setLevelHudVisible(false);
                this.logHud(uUID, "HUD_CLEAR applied state.hud player=%s", uUID2);
            }
            return;
        }
        CustomUIHud customUIHud = player.getHudManager().getCustomHud();
        if (customUIHud instanceof BossBarHud) {
            ((BossBarHud)customUIHud).setBossVisible(false);
            ((BossBarHud)customUIHud).setLevelHudVisible(false);
            this.logHud(uUID, "HUD_CLEAR applied currentHud player=%s", uUID2);
        }
    }

    public void setHudSuppressed(@Nonnull UUID uUID, boolean bl) {
        if (this.mobWaveSpawner.isTrackedPlayer(uUID)) {
            if (bl) {
                this.suppressedPlayers.add(uUID);
            } else {
                this.suppressedPlayers.remove(uUID);
            }
            return;
        }
        UUID uUID2 = this.normalizeOwnerId(uUID);
        for (Ref<EntityStore> ref : this.mobWaveSpawner.getPlayerRefsForOwner(uUID2)) {
            PlayerRef playerRef;
            Store store = ref.getStore();
            if (store == null || (playerRef = (PlayerRef)store.getComponent(ref, PlayerRef.getComponentType())) == null) continue;
            UUID uUID3 = playerRef.getUuid();
            if (bl) {
                this.suppressedPlayers.add(uUID3);
                continue;
            }
            this.suppressedPlayers.remove(uUID3);
        }
    }

    private void logHud(@Nonnull UUID uUID, @Nonnull String string, Object ... objectArray) {
    }

    private void logHudThrottled(@Nonnull UUID uUID, @Nonnull String string, Object ... objectArray) {
    }

    @Nonnull
    private UUID normalizeOwnerId(@Nonnull UUID uUID) {
        return this.mobWaveSpawner.resolveOwnerId(uUID);
    }

    private static String getWorldName(@Nullable Store<EntityStore> store) {
        if (store == null) {
            return "null";
        }
        World world = ((EntityStore)store.getExternalData()).getWorld();
        if (world == null) {
            return "null";
        }
        String string = world.getName();
        if (string != null && !string.isBlank()) {
            return string;
        }
        return Objects.toString(world.getWorldConfig() != null ? world.getWorldConfig().getDisplayName() : null, "unknown");
    }

    private static float clamp01(float f) {
        if (!Float.isFinite(f)) {
            return 0.0f;
        }
        if (f < 0.0f) {
            return 0.0f;
        }
        if (f > 1.0f) {
            return 1.0f;
        }
        return f;
    }

    @Nullable
    private static String resolveBossName(@Nonnull Store<EntityStore> store, @Nullable Ref<EntityStore> ref) {
        if (ref == null || !ref.isValid()) {
            return null;
        }
        NPCEntity nPCEntity = (NPCEntity)store.getComponent(ref, NPCEntity.getComponentType());
        if (nPCEntity == null) {
            return null;
        }
        String string = nPCEntity.getRoleName();
        if (TOAD_ROLE_NAME.equals(string)) {
            return TOAD_BOSS_NAME;
        }
        if (WRAITH_BOSS_NAME.equals(string)) {
            return WRAITH_BOSS_NAME;
        }
        if (SKELETON_ROLE_NAME.equals(string)) {
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
