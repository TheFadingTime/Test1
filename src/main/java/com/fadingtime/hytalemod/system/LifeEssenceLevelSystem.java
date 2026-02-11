package com.fadingtime.hytalemod.system;

import com.fadingtime.hytalemod.HytaleMod;
import com.fadingtime.hytalemod.component.VampireShooterComponent;
import com.fadingtime.hytalemod.persistence.PlayerStateStore;
import com.fadingtime.hytalemod.persistence.PlayerStateStoreManager;
import com.fadingtime.hytalemod.spawner.MobWaveSpawner;
import com.fadingtime.hytalemod.ui.BossBarHud;
import com.fadingtime.hytalemod.ui.PowerUpStorePage;
import com.hypixel.hytale.builtin.weather.resources.WeatherResource;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.AttachedToType;
import com.hypixel.hytale.protocol.ClientCameraView;
import com.hypixel.hytale.protocol.Direction;
import com.hypixel.hytale.protocol.MouseInputTargetType;
import com.hypixel.hytale.protocol.MouseInputType;
import com.hypixel.hytale.protocol.MovementForceRotationType;
import com.hypixel.hytale.protocol.Position;
import com.hypixel.hytale.protocol.PositionDistanceOffsetType;
import com.hypixel.hytale.protocol.PositionType;
import com.hypixel.hytale.protocol.RotationType;
import com.hypixel.hytale.protocol.ServerCameraSettings;
import com.hypixel.hytale.protocol.Vector3f;
import com.hypixel.hytale.protocol.MovementSettings;
import com.hypixel.hytale.protocol.packets.camera.SetServerCamera;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.event.events.entity.LivingEntityInventoryChangeEvent;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.DrainPlayerFromWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.SlotTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.Transaction;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldConfig;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import org.bson.BsonDocument;

public class LifeEssenceLevelSystem {
    private static final int ESSENCE_PER_LEVEL_BASE = 15;
    private static final int ESSENCE_PER_LEVEL_INCREMENT = 5;
    private static final int ESSENCE_PER_LEVEL_MAX = 15;
    private static final String DEFAULT_BOSS_NAME = "Giant Skeleton";
    private static final float STORE_SLOWMO_FACTOR = 0.2f;
    private static final int STORE_OPEN_LEVEL_START = 5;
    private static final int STORE_OPEN_LEVEL_INTERVAL = 5;
    private static final int STORE_OPEN_LEVEL_MAX = 50;
    private static final int MAX_FIRE_RATE_UPGRADES = 4;
    private static final float FIRE_RATE_MULTIPLIER_PER_UPGRADE = 1.15f;
    private static final float MAX_FIRE_RATE_MULTIPLIER = (float)Math.pow(1.15f, 4.0);
    private static final int MAX_PICKUP_RANGE_UPGRADES = 5;
    private static final float PICKUP_RANGE_PER_UPGRADE = 10.0f;
    private static final float MAX_PICKUP_RANGE_BONUS = 50.0f;
    private static final int MAX_EXTRA_PROJECTILES = 10;
    private static final int MAX_BOUNCE_UPGRADES = 3;
    private static final int MAX_WEAPON_DAMAGE_UPGRADES = 2;
    private static final float WEAPON_DAMAGE_BONUS_PER_UPGRADE = 15.0f;
    private static final int MAX_HEALTH_UPGRADES = 3;
    private static final float HEALTH_BONUS_PER_UPGRADE = 20.0f;
    private static final int MAX_SPEED_UPGRADES = 1;
    private static final int MAX_PROJECTILE_RAIN_UPGRADES = 1;
    private static final int MAX_LUCKY_UPGRADES = 5;
    private static final int PROJECTILE_RAIN_BURSTS_ON_PICK = 2;
    private static final float SPEED_BONUS_PER_UPGRADE = 0.05f;
    private static final float MAX_SPEED_MULTIPLIER = 1.05f;
    private static final float PLAYER_BASE_SPEED = 10.0f;
    private static final String PLAYER_HEALTH_BONUS_MODIFIER_ID = "PlayerHealthBonus";
    private static final String PLAYER_SPEED_STAMINA_BONUS_MODIFIER_ID = "PlayerSpeedStaminaBonus";
    private static final float STAMINA_BONUS_PER_SPEED_UPGRADE = 20.0f;
    private static final int LUCKY_ESSENCE_BONUS_PER_RANK = 1;
    private static final long HUD_READY_DELAY_MS = 500L;
    private static final long STORE_INPUT_GRACE_MS = 400L;
    private static final String JOIN_WEAPON_ITEM_ID = "Weapon_Battleaxe_Mithril";
    private static final String JOIN_WEAPON_ITEM_ID_FALLBACK = "Weapon_BattleAxe_Mithril";
    private static final String JOIN_SHORTBOW_ITEM_ID = "Weapon_Shortbow_Mithril";
    private static final String JOIN_SHORTBOW_ITEM_ID_FALLBACK = "Weapon_ShortBow_Mithril";
    private static final String JOIN_ARROW_ITEM_ID = "Weapon_Arrow_Crude";
    private static final String JOIN_FOOD_ITEM_ID = "Food_Bread";
    private static final String JOIN_ARMOR_HEAD_ITEM_ID = "Armor_Mithril_Head";
    private static final String JOIN_ARMOR_CHEST_ITEM_ID = "Armor_Mithril_Chest";
    private static final String JOIN_ARMOR_HANDS_ITEM_ID = "Armor_Mithril_Hands";
    private static final String JOIN_ARMOR_HANDS_ITEM_ID_FALLBACK = "Armor_Mithril_Hand";
    private static final String JOIN_ARMOR_LEGS_ITEM_ID = "Armor_Mithril_Legs";
    private static final String JOIN_ARMOR_LEGS_ITEM_ID_FALLBACK = "Armor_Mithril_legs";
    private static final int JOIN_ARROW_QUANTITY = 200;
    private static final int JOIN_FOOD_QUANTITY = 25;
    private static final int JOIN_DEFAULT_DURABILITY = 5000;
    private static final String JOIN_FORCED_WEATHER_ID = "Zone4_Lava_Fields";
    private final HytaleMod plugin;
    private final PlayerStateStoreManager stateStoreManager;
    private final ConcurrentMap<UUID, LevelState> levelByPlayer = new ConcurrentHashMap<UUID, LevelState>();
    private final ConcurrentMap<UUID, PowerState> powerByPlayer = new ConcurrentHashMap<UUID, PowerState>();
    private final ConcurrentMap<UUID, ScheduledFuture<?>> storeTimeouts = new ConcurrentHashMap();
    private final AtomicInteger storePauseCount = new AtomicInteger(0);
    private final ConcurrentMap<UUID, Integer> storeSessionByPlayer = new ConcurrentHashMap();
    private final ConcurrentMap<UUID, Long> storeOpenedAt = new ConcurrentHashMap();
    private final ConcurrentMap<UUID, String> worldByPlayer = new ConcurrentHashMap<UUID, String>();
    private final ConcurrentMap<UUID, Boolean> lastWasGameplayWorld = new ConcurrentHashMap<UUID, Boolean>();
    private final ConcurrentMap<UUID, ConcurrentMap<String, InventorySnapshot>> inventoriesByPlayer = new ConcurrentHashMap();
    private final ConcurrentMap<UUID, String> inventoryWorldByPlayer = new ConcurrentHashMap<UUID, String>();
    private final ConcurrentMap<UUID, Boolean> pendingHudClear = new ConcurrentHashMap<UUID, Boolean>();
    private final ConcurrentMap<UUID, ScheduledFuture<?>> hudUpdateTasks = new ConcurrentHashMap();
    private final ConcurrentMap<UUID, ConcurrentLinkedQueue<ScheduledFuture<?>>> delayedTasksByPlayer = new ConcurrentHashMap();
    private final ConcurrentMap<UUID, Long> disconnectHandledAt = new ConcurrentHashMap<UUID, Long>();

    public LifeEssenceLevelSystem(@Nonnull HytaleMod plugin) {
        this.plugin = plugin;
        this.stateStoreManager = plugin.getStateStoreManager();
        plugin.getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, this::onPlayerAddedToWorld);
        plugin.getEventRegistry().registerGlobal(DrainPlayerFromWorldEvent.class, this::onPlayerDrain);
        plugin.getEventRegistry().registerGlobal(PlayerReadyEvent.class, this::onPlayerReady);
        plugin.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, this::onPlayerDisconnect);
        plugin.getEventRegistry().registerGlobal(LivingEntityInventoryChangeEvent.class, this::onInventoryChange);
    }

    private void onPlayerAddedToWorld(@Nonnull AddPlayerToWorldEvent event) {
        Holder<EntityStore> holder = event.getHolder();
        if (holder == null) {
            return;
        }
        PlayerRef playerRefComponent = (PlayerRef)holder.getComponent(PlayerRef.getComponentType());
        if (playerRefComponent == null) {
            return;
        }
        Ref playerRef = playerRefComponent.getReference();
        if (playerRef == null || !playerRef.isValid()) {
            return;
        }
        Store store = playerRef.getStore();
        if (store == null) {
            return;
        }
        World world = event.getWorld();
        if (world == null) {
            return;
        }
        boolean inGameplayWorld = true;
        Player playerComponent = (Player)store.getComponent(playerRef, Player.getComponentType());
        if (playerComponent == null) {
            return;
        }
        UUID playerId = playerRefComponent.getUuid();
        String worldKey = LifeEssenceLevelSystem.getWorldKey((Store<EntityStore>)store);
        if (!inGameplayWorld) {
            VampireShooterComponent shooter = (VampireShooterComponent)store.getComponent(playerRef, HytaleMod.getInstance().getVampireShooterComponentType());
            if (shooter != null) {
                this.resetPowerState(shooter);
            }
            BossHudSystem bossHudSystem = HytaleMod.getInstance().getBossHudSystem();
            if (bossHudSystem != null) {
                bossHudSystem.setHudSuppressed(playerId, true);
            }
            this.scheduleHudUpdate(world, playerRefComponent, () -> {
                if (!playerRef.isValid()) {
                    return;
                }
                Player readyPlayer = (Player)store.getComponent(playerRef, Player.getComponentType());
                if (readyPlayer == null) {
                    return;
                }
                this.clearHudForPlayer(readyPlayer, playerRefComponent, playerId);
            });
            return;
        }
        // Inventory handling for gameplay worlds is done in onPlayerReady after the
        // previous world's snapshot is saved. Don't clear here.
        BossHudSystem bossHudSystem = HytaleMod.getInstance().getBossHudSystem();
        if (bossHudSystem != null) {
            bossHudSystem.setHudSuppressed(playerId, false);
        }
        this.pendingHudClear.remove(playerId);
        this.scheduleHudUpdate(world, playerRefComponent, () -> {
            if (!this.isGameplayWorld((Store<EntityStore>)store)) {
                return;
            }
            this.loadStateIfMissing(playerId, worldKey);
            LevelState state = this.levelByPlayer.computeIfAbsent(playerId, id -> new LevelState());
            BossBarHud hud = this.ensureHud(playerComponent, playerRefComponent, (Store<EntityStore>)store);
            if (hud != null) {
                hud.setLevelHudVisible(true);
                this.plugin.getLogger().at(Level.INFO).log("[HUD_DEBUG] updateLevel (add) player=%s world=%s level=%d essence=%d/%d", (Object)playerId, (Object)worldKey, (Object)state.level, (Object)state.essenceProgress, (Object)state.getEssenceRequired());
                hud.updateLevel(state.level, state.essenceProgress, state.getEssenceRequired());
            } else {
                this.plugin.getLogger().at(Level.INFO).log("[HUD_DEBUG] updateLevel skipped (add) player=%s world=%s reason=ensureHud null", (Object)playerId, (Object)worldKey);
            }
        });
    }

    private void onPlayerDrain(@Nonnull DrainPlayerFromWorldEvent event) {
        Holder<EntityStore> holder = event.getHolder();
        if (holder == null) {
            return;
        }
        PlayerRef playerRefComponent = (PlayerRef)holder.getComponent(PlayerRef.getComponentType());
        if (playerRefComponent == null) {
            return;
        }
        Ref playerRef = playerRefComponent.getReference();
        if (playerRef == null || !playerRef.isValid()) {
            return;
        }
        Store store = playerRef.getStore();
        if (store == null) {
            return;
        }
        World world = event.getWorld();
        if (world == null) {
            return;
        }
        UUID playerId = playerRefComponent.getUuid();
        this.plugin.getLogger().at(Level.INFO).log("[HUD_DEBUG] onPlayerDrain player=%s world=%s", (Object)playerId, (Object)world.getName());
        this.pendingHudClear.put(playerId, true);
        this.cancelAllScheduledTasks(playerId);
        BossHudSystem bossHudSystem = HytaleMod.getInstance().getBossHudSystem();
        if (bossHudSystem != null) {
            bossHudSystem.setHudSuppressed(playerId, true);
        }
        this.plugin.getLogger().at(Level.INFO).log("[HUD_DEBUG] clearHudForPlayer deferred (drain) player=%s", (Object)playerId);
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
        World world = (World)((EntityStore)store.getExternalData()).getWorld();
        if (world == null) {
            return;
        }
        this.executeIfTicking(world, () -> {
            VampireShooterComponent shooter;
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
            Player playerComponent = (Player)worldStore.getComponent(playerRef, Player.getComponentType());
            if (playerComponent == null) {
                return;
            }
            UUID playerId = playerRefComponent.getUuid();
            this.disconnectHandledAt.remove(playerId);
            String worldKey = LifeEssenceLevelSystem.getWorldKey((Store<EntityStore>)worldStore);
            boolean inGameplayWorld = this.isGameplayWorld((Store<EntityStore>)worldStore);
            World currentWorld = (World)((EntityStore)worldStore.getExternalData()).getWorld();
            if (currentWorld == null) {
                return;
            }
            BossHudSystem bossHudSystem = HytaleMod.getInstance().getBossHudSystem();
            if (bossHudSystem != null) {
                bossHudSystem.setHudSuppressed(playerId, !inGameplayWorld);
            }
            this.switchInventoryForWorld(playerId, worldKey, playerComponent);
            if (!inGameplayWorld) {
                this.scheduleHudUpdate(currentWorld, playerRefComponent, () -> {
                    boolean wasGameplayWorld = Boolean.TRUE.equals(this.lastWasGameplayWorld.remove(playerId));
                    boolean clearHud = Boolean.TRUE.equals(this.pendingHudClear.remove(playerId)) || wasGameplayWorld;
                    if (wasGameplayWorld) {
                        this.resetCamera(playerRefComponent);
                    }
                    VampireShooterComponent nonGameplayShooter = (VampireShooterComponent)worldStore.getComponent(playerRef, HytaleMod.getInstance().getVampireShooterComponentType());
                    if (nonGameplayShooter != null) {
                        this.resetPowerState(nonGameplayShooter);
                    }
                    CustomUIHud currentHud = playerComponent.getHudManager().getCustomHud();
                    if (clearHud || currentHud instanceof BossBarHud) {
                        this.clearHudForPlayer(playerComponent, playerRefComponent, playerId);
                    }
                });
                return;
            }
            if (!Boolean.TRUE.equals(this.lastWasGameplayWorld.put(playerId, true))) {
                this.forceTopdownCameraDirect(playerRefComponent);
                Inventory inventory = playerComponent.getInventory();
                if (inventory != null) {
                    this.clearInventory(inventory);
                    this.saveInventorySnapshot(playerId, worldKey, playerComponent);
                }
            }
            this.loadStateIfMissing(playerId, worldKey);
            LevelState state = this.levelByPlayer.computeIfAbsent(playerId, id -> new LevelState());
            this.applyPlayerPowerBonuses(playerId, (Ref<EntityStore>)playerRef, worldStore, playerRefComponent, worldKey);
            this.scheduleHudUpdate(currentWorld, playerRefComponent, () -> {
                BossBarHud hud = this.ensureHud(playerComponent, playerRefComponent, worldStore);
                if (hud != null) {
                    this.plugin.getLogger().at(Level.INFO).log("[HUD_DEBUG] updateLevel (ready) player=%s world=%s level=%d essence=%d/%d", (Object)playerId, (Object)worldKey, (Object)state.level, (Object)state.essenceProgress, (Object)state.getEssenceRequired());
                    hud.updateLevel(state.level, state.essenceProgress, state.getEssenceRequired());
                } else {
                    this.plugin.getLogger().at(Level.INFO).log("[HUD_DEBUG] updateLevel skipped (ready) player=%s world=%s reason=ensureHud null", (Object)playerId, (Object)worldKey);
                }
                VampireShooterComponent delayedShooter = (VampireShooterComponent)worldStore.getComponent(playerRef, HytaleMod.getInstance().getVampireShooterComponentType());
                if (delayedShooter != null) {
                    this.restorePowerState(playerId, delayedShooter, worldKey);
                }
                this.applyPlayerPowerBonuses(playerId, (Ref<EntityStore>)playerRef, worldStore, playerRefComponent, worldKey);
            });
            this.schedulePlayerWorldTask(currentWorld, playerRefComponent, () -> this.applyPlayerPowerBonuses(playerId, (Ref<EntityStore>)playerRef, worldStore, playerRefComponent, worldKey), 300L);
            this.schedulePlayerWorldTask(currentWorld, playerRefComponent, () -> this.applyPlayerPowerBonuses(playerId, (Ref<EntityStore>)playerRef, worldStore, playerRefComponent, worldKey), 1200L);
            this.applyJoinDefaults((Ref<EntityStore>)playerRef, (Store<EntityStore>)worldStore, currentWorld);
        });
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
        this.plugin.getLogger().at(Level.INFO).log("[HUD_DEBUG] onPlayerDisconnect player=%s", (Object)playerId);
        this.cancelAllScheduledTasks(playerId);
        this.resetTransientPlayerState(playerId);
        Ref playerRef = playerRefComponent.getReference();
        if (playerRef == null || !playerRef.isValid()) {
            return;
        }
        Store store = playerRef.getStore();
        if (store == null) {
            return;
        }
        World world = ((EntityStore)store.getExternalData()).getWorld();
        if (world == null) {
            return;
        }
        this.executeIfTicking(world, () -> {
            if (!playerRef.isValid()) {
                return;
            }
            Player playerComponent;
            try {
                playerComponent = (Player)store.getComponent(playerRef, Player.getComponentType());
            }
            catch (IllegalStateException ignored) {
                return;
            }
            if (playerComponent == null) {
                return;
            }
            String inventoryWorldKey = (String)this.inventoryWorldByPlayer.get(playerId);
            if (inventoryWorldKey == null) {
                inventoryWorldKey = LifeEssenceLevelSystem.getWorldKey((Store<EntityStore>)store);
            }
            this.saveInventorySnapshot(playerId, inventoryWorldKey, playerComponent);
        });
    }

    private void onInventoryChange(@Nonnull LivingEntityInventoryChangeEvent event) {
        LivingEntity entity = (LivingEntity)event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        Ref playerRef = entity.getReference();
        if (playerRef == null || !playerRef.isValid()) {
            return;
        }
        Store store = playerRef.getStore();
        if (store == null) {
            return;
        }
        if (!this.isGameplayWorld((Store<EntityStore>)store)) {
            return;
        }
        PlayerRef playerRefComponent = (PlayerRef)store.getComponent(playerRef, PlayerRef.getComponentType());
        if (playerRefComponent == null) {
            return;
        }
        Player playerComponent = (Player)store.getComponent(playerRef, Player.getComponentType());
        if (playerComponent == null) {
            return;
        }
        int delta = this.getLifeEssenceDelta(event.getTransaction());
        if (delta <= 0) {
            return;
        }
        this.grantEssence(playerRefComponent, playerComponent, (Store<EntityStore>)store, delta);
    }

    public void grantEssence(@Nonnull PlayerRef playerRefComponent, @Nonnull Player playerComponent, @Nonnull Store<EntityStore> store, int amount) {
        BossBarHud hud;
        if (amount <= 0) {
            return;
        }
        if (!this.isGameplayWorld(store)) {
            return;
        }
        UUID playerId = playerRefComponent.getUuid();
        String worldKey = LifeEssenceLevelSystem.getWorldKey(store);
        this.loadStateIfMissing(playerId, worldKey);
        PowerState powerState = this.powerByPlayer.computeIfAbsent(playerId, id -> new PowerState());
        int bonus = LifeEssenceLevelSystem.clampLuckyRank(powerState.luckyRank) * LUCKY_ESSENCE_BONUS_PER_RANK;
        int effectiveAmount = Math.max(1, amount + bonus);
        LevelState state = this.levelByPlayer.computeIfAbsent(playerId, id -> new LevelState());
        int previousLevel = state.addEssence(effectiveAmount);
        if (state.level != previousLevel) {
            this.plugin.getLogger().at(Level.INFO).log("Level up for %s: %d -> %d (lastStoreLevel=%d, shouldOpenStore=%s)", (Object)playerId, (Object)previousLevel, (Object)state.level, (Object)state.lastStoreLevel, (Object)state.shouldOpenStore(state.level));
        }
        if ((hud = this.ensureHud(playerComponent, playerRefComponent, store)) != null) {
            this.plugin.getLogger().at(Level.INFO).log("[HUD_DEBUG] updateLevel (essence) player=%s world=%s level=%d essence=%d/%d", (Object)playerId, (Object)worldKey, (Object)state.level, (Object)state.essenceProgress, (Object)state.getEssenceRequired());
            hud.updateLevel(state.level, state.essenceProgress, state.getEssenceRequired());
        } else {
            this.plugin.getLogger().at(Level.INFO).log("[HUD_DEBUG] updateLevel skipped (essence) player=%s world=%s reason=ensureHud null", (Object)playerId, (Object)worldKey);
        }
        if (state.shouldOpenStore(state.level)) {
            Store playerStore;
            state.markStoreOpened(state.level);
            this.savePlayerState(playerId, worldKey);
            Ref playerRef = playerRefComponent.getReference();
            if (playerRef != null && playerRef.isValid() && (playerStore = playerRef.getStore()) != null) {
                this.openStoreForPlayer((Ref<EntityStore>)playerRef, (Store<EntityStore>)playerStore, playerComponent, playerRefComponent, state.level);
            }
        } else {
            this.savePlayerState(playerId, worldKey);
        }
    }

    public void applyPowerUp(@Nonnull Ref<EntityStore> playerRef, @Nonnull Store<EntityStore> store, @Nonnull String choice) {
        boolean triggerProjectileRain;
        if (!this.isGameplayWorld(store)) {
            return;
        }
        PlayerRef playerRefComponent = (PlayerRef)store.getComponent(playerRef, PlayerRef.getComponentType());
        if (playerRefComponent == null) {
            return;
        }
        UUID playerId = playerRefComponent.getUuid();
        String worldKey = LifeEssenceLevelSystem.getWorldKey(store);
        this.loadStateIfMissing(playerId, worldKey);
        PowerState state = this.powerByPlayer.computeIfAbsent(playerId, id -> new PowerState());
        String choiceLabel = this.getPowerChoiceLabel(choice);
        if (choiceLabel == null) {
            return;
        }
        triggerProjectileRain = false;
        if ("extra_projectile".equals(choice)) {
            state.projectileCount = clampProjectileCount(state.projectileCount + 1);
        } else if ("fire_rate".equals(choice)) {
            state.fireRateMultiplier = LifeEssenceLevelSystem.clampFireRate(state.fireRateMultiplier * 1.15f);
        } else if ("pickup_range".equals(choice)) {
            state.pickupRangeBonus = LifeEssenceLevelSystem.clampPickupRange(state.pickupRangeBonus + 10.0f);
        } else if ("bounce".equals(choice)) {
            state.bounceBonus = LifeEssenceLevelSystem.clampBounceBonus(state.bounceBonus + 1);
        } else if ("weapon_damage".equals(choice)) {
            state.weaponDamageRank = LifeEssenceLevelSystem.clampWeaponDamageRank(state.weaponDamageRank + 1);
        } else if ("max_health".equals(choice)) {
            state.healthRank = LifeEssenceLevelSystem.clampHealthRank(state.healthRank + 1);
        } else if ("move_speed".equals(choice)) {
            state.speedRank = LifeEssenceLevelSystem.clampSpeedRank(state.speedRank + 1);
        } else if ("lucky".equals(choice)) {
            state.luckyRank = LifeEssenceLevelSystem.clampLuckyRank(state.luckyRank + 1);
        } else if ("projectile_rain".equals(choice)) {
            if (this.isProjectileRainUsedForActiveGroup(playerId, store)) {
                return;
            }
            triggerProjectileRain = true;
        }
        VampireShooterComponent shooter = (VampireShooterComponent)store.getComponent(playerRef, HytaleMod.getInstance().getVampireShooterComponentType());
        if (triggerProjectileRain) {
            if (shooter == null) {
                return;
            }
            for (int i = 0; i < PROJECTILE_RAIN_BURSTS_ON_PICK; ++i) {
                shooter.queueProjectileRain();
            }
            this.markProjectileRainUsedForActiveGroup(playerId, store, worldKey);
        }
        if (shooter != null) {
            this.restorePowerState(playerId, shooter, worldKey);
        }
        this.applyPlayerPowerBonuses(playerId, playerRef, store, playerRefComponent, worldKey);
        this.savePlayerState(playerId, worldKey);
        int rank = this.getPowerChoiceRank(choice, state);
        this.announcePowerChoice(playerRef, store, playerId, choiceLabel, rank);
    }

    public boolean triggerProjectileRainNow(@Nonnull Ref<EntityStore> playerRef, @Nonnull Store<EntityStore> store) {
        if (!this.isGameplayWorld(store) || playerRef == null || !playerRef.isValid()) {
            return false;
        }
        VampireShooterComponent shooter = (VampireShooterComponent)store.getComponent(playerRef, HytaleMod.getInstance().getVampireShooterComponentType());
        if (shooter == null) {
            return false;
        }
        for (int i = 0; i < PROJECTILE_RAIN_BURSTS_ON_PICK; ++i) {
            shooter.queueProjectileRain();
        }
        return true;
    }

    private void announcePowerChoice(@Nonnull Ref<EntityStore> chooserRef, @Nonnull Store<EntityStore> store, @Nonnull UUID chooserId, @Nonnull String choiceLabel, int rank) {
        String chooserName;
        Player chooserPlayer = (Player)store.getComponent(chooserRef, Player.getComponentType());
        if (chooserPlayer != null && chooserPlayer.getDisplayName() != null && !chooserPlayer.getDisplayName().isBlank()) {
            chooserName = chooserPlayer.getDisplayName().trim();
        } else {
            chooserName = chooserId.toString();
        }
        chooserName = chooserName.toUpperCase(Locale.ROOT);
        Message message = Message.join((Message[])new Message[]{Message.raw(chooserName + " "), Message.raw("Selected: ").color("#f0c766").bold(true), Message.raw(choiceLabel + " ").color("#f0c766").bold(true), Message.raw("- Rank " + rank).color("#4f8f5f").bold(true)});
        MobWaveSpawner spawner = HytaleMod.getInstance().getMobWaveSpawner();
        if (spawner != null) {
            UUID ownerId = spawner.resolveOwnerId(chooserId);
            List<Ref<EntityStore>> refs = spawner.getPlayerRefsForOwner(ownerId);
            if (!refs.isEmpty()) {
                for (Ref<EntityStore> ref : refs) {
                    if (ref == null || !ref.isValid()) {
                        continue;
                    }
                    Store<EntityStore> memberStore = ref.getStore();
                    if (memberStore == null) {
                        continue;
                    }
                    Player memberPlayer = (Player)memberStore.getComponent(ref, Player.getComponentType());
                    if (memberPlayer == null) {
                        continue;
                    }
                    memberPlayer.sendMessage(message);
                }
                return;
            }
        }
        if (chooserPlayer != null) {
            chooserPlayer.sendMessage(message);
        }
    }

    private int getPowerChoiceRank(@Nonnull String choice, @Nonnull PowerState state) {
        if ("extra_projectile".equals(choice)) {
            return LifeEssenceLevelSystem.clampProjectileCount(state.projectileCount);
        }
        if ("fire_rate".equals(choice)) {
            return LifeEssenceLevelSystem.computeFireRateRank(state.fireRateMultiplier);
        }
        if ("pickup_range".equals(choice)) {
            return LifeEssenceLevelSystem.computePickupRangeRank(state.pickupRangeBonus);
        }
        if ("bounce".equals(choice)) {
            return LifeEssenceLevelSystem.clampBounceBonus(state.bounceBonus);
        }
        if ("weapon_damage".equals(choice)) {
            return LifeEssenceLevelSystem.clampWeaponDamageRank(state.weaponDamageRank);
        }
        if ("max_health".equals(choice)) {
            return LifeEssenceLevelSystem.clampHealthRank(state.healthRank);
        }
        if ("move_speed".equals(choice)) {
            return LifeEssenceLevelSystem.clampSpeedRank(state.speedRank);
        }
        if ("lucky".equals(choice)) {
            return LifeEssenceLevelSystem.clampLuckyRank(state.luckyRank);
        }
        if ("projectile_rain".equals(choice)) {
            return state.projectileRainUsed ? 1 : 0;
        }
        return 0;
    }

    private String getPowerChoiceLabel(@Nonnull String choice) {
        if ("extra_projectile".equals(choice)) {
            return "EXTRA PROJECTILE";
        }
        if ("fire_rate".equals(choice)) {
            return "FIRE RATE";
        }
        if ("pickup_range".equals(choice)) {
            return "PICKUP RANGE";
        }
        if ("bounce".equals(choice)) {
            return "BOUNCE";
        }
        if ("weapon_damage".equals(choice)) {
            return "WEAPON DAMAGE";
        }
        if ("max_health".equals(choice)) {
            return "MORE HEALTH";
        }
        if ("move_speed".equals(choice)) {
            return "MOVE SPEED";
        }
        if ("lucky".equals(choice)) {
            return "LUCKY";
        }
        if ("projectile_rain".equals(choice)) {
            return "LAST RESORT...";
        }
        return null;
    }

    public void restorePowerState(@Nonnull UUID playerId, @Nonnull VampireShooterComponent shooter) {
        this.restorePowerState(playerId, shooter, null);
    }

    private void restorePowerState(@Nonnull UUID playerId, @Nonnull VampireShooterComponent shooter, String worldKey) {
        PowerState state;
        if (worldKey != null) {
            this.loadStateIfMissing(playerId, worldKey);
        }
        if ((state = (PowerState)this.powerByPlayer.get(playerId)) == null) {
            return;
        }
        shooter.setProjectileCount(LifeEssenceLevelSystem.clampProjectileCount(state.projectileCount));
        shooter.setFireRateMultiplier(LifeEssenceLevelSystem.clampFireRate(state.fireRateMultiplier));
        shooter.setPickupRangeBonus(LifeEssenceLevelSystem.clampPickupRange(state.pickupRangeBonus));
        shooter.setBounceBonus(LifeEssenceLevelSystem.clampBounceBonus(state.bounceBonus));
    }

    private void resetPowerState(@Nonnull VampireShooterComponent shooter) {
        shooter.setProjectileCount(0);
        shooter.setFireRateMultiplier(1.0f);
        shooter.setPickupRangeBonus(0.0f);
        shooter.setBounceBonus(0);
    }

    private void applyPlayerPowerBonuses(@Nonnull UUID playerId, @Nonnull Ref<EntityStore> playerRef, @Nonnull Store<EntityStore> store, @Nonnull PlayerRef playerRefComponent, String worldKey) {
        PowerState state;
        if (worldKey != null) {
            this.loadStateIfMissing(playerId, worldKey);
        }
        if ((state = (PowerState)this.powerByPlayer.get(playerId)) == null) {
            return;
        }
        this.applyPlayerHealthBonus(playerRef, store, state.healthRank);
        this.applyPlayerSpeedBonus(playerRef, store, playerRefComponent, state.speedRank);
    }

    private void applyPlayerHealthBonus(@Nonnull Ref<EntityStore> playerRef, @Nonnull Store<EntityStore> store, int rank) {
        EntityStatMap statMap = (EntityStatMap)store.getComponent(playerRef, EntityStatMap.getComponentType());
        if (statMap == null) {
            return;
        }
        float bonus = (float)LifeEssenceLevelSystem.clampHealthRank(rank) * HEALTH_BONUS_PER_UPGRADE;
        if (bonus <= 0.0f) {
            statMap.removeModifier(DefaultEntityStatTypes.getHealth(), PLAYER_HEALTH_BONUS_MODIFIER_ID);
            return;
        }
        statMap.putModifier(DefaultEntityStatTypes.getHealth(), PLAYER_HEALTH_BONUS_MODIFIER_ID, (Modifier)new StaticModifier(Modifier.ModifierTarget.MAX, StaticModifier.CalculationType.ADDITIVE, bonus));
        statMap.maximizeStatValue(DefaultEntityStatTypes.getHealth());
    }

    private void applyPlayerSpeedBonus(@Nonnull Ref<EntityStore> playerRef, @Nonnull Store<EntityStore> store, @Nonnull PlayerRef playerRefComponent, int rank) {
        MovementManager movementManager = (MovementManager)store.getComponent(playerRef, MovementManager.getComponentType());
        float speedMultiplier = LifeEssenceLevelSystem.getSpeedMultiplierForRank(rank);
        if (movementManager != null) {
            movementManager.resetDefaultsAndUpdate(playerRef, (ComponentAccessor<EntityStore>)store);
            MovementSettings base = movementManager.getDefaultSettings();
            if (base != null) {
                base.baseSpeed = PLAYER_BASE_SPEED;
                LifeEssenceLevelSystem.applySpeedMultiplier(base, speedMultiplier);
                movementManager.applyDefaultSettings();
                movementManager.update(playerRefComponent.getPacketHandler());
            }
        }
        this.applyPlayerSpeedStaminaBonus(playerRef, store, rank);
    }

    private void applyPlayerSpeedStaminaBonus(@Nonnull Ref<EntityStore> playerRef, @Nonnull Store<EntityStore> store, int rank) {
        EntityStatMap statMap = (EntityStatMap)store.getComponent(playerRef, EntityStatMap.getComponentType());
        if (statMap == null) {
            return;
        }
        float bonus = (float)LifeEssenceLevelSystem.clampSpeedRank(rank) * STAMINA_BONUS_PER_SPEED_UPGRADE;
        if (bonus <= 0.0f) {
            statMap.removeModifier(DefaultEntityStatTypes.getStamina(), PLAYER_SPEED_STAMINA_BONUS_MODIFIER_ID);
            return;
        }
        statMap.putModifier(DefaultEntityStatTypes.getStamina(), PLAYER_SPEED_STAMINA_BONUS_MODIFIER_ID, (Modifier)new StaticModifier(Modifier.ModifierTarget.MAX, StaticModifier.CalculationType.ADDITIVE, bonus));
        statMap.maximizeStatValue(DefaultEntityStatTypes.getStamina());
    }

    private static void applySpeedMultiplier(@Nonnull MovementSettings settings, float speedMultiplier) {
        settings.baseSpeed = Math.max(0.01f, settings.baseSpeed * speedMultiplier);
        settings.climbSpeed = Math.max(0.01f, settings.climbSpeed * speedMultiplier);
        settings.climbSpeedLateral = Math.max(0.01f, settings.climbSpeedLateral * speedMultiplier);
        settings.climbUpSprintSpeed = Math.max(0.01f, settings.climbUpSprintSpeed * speedMultiplier);
        settings.climbDownSprintSpeed = Math.max(0.01f, settings.climbDownSprintSpeed * speedMultiplier);
        settings.horizontalFlySpeed = Math.max(0.01f, settings.horizontalFlySpeed * speedMultiplier);
        settings.verticalFlySpeed = Math.max(0.01f, settings.verticalFlySpeed * speedMultiplier);
    }

    public double getPickupRangeBonus(@Nonnull UUID playerId) {
        PowerState state = (PowerState)this.powerByPlayer.get(playerId);
        if (state == null) {
            return 0.0;
        }
        return Math.max(0.0, (double)state.pickupRangeBonus);
    }

    public boolean hasFireRatePower(@Nonnull UUID playerId, @Nonnull Store<EntityStore> store) {
        this.loadStateIfMissing(playerId, LifeEssenceLevelSystem.getWorldKey(store));
        PowerState state = (PowerState)this.powerByPlayer.get(playerId);
        return state != null && state.fireRateMultiplier >= MAX_FIRE_RATE_MULTIPLIER - 1.0E-4f;
    }

    public int getFireRateRank(@Nonnull UUID playerId, @Nonnull Store<EntityStore> store) {
        this.loadStateIfMissing(playerId, LifeEssenceLevelSystem.getWorldKey(store));
        PowerState state = (PowerState)this.powerByPlayer.get(playerId);
        if (state == null) {
            return 0;
        }
        return LifeEssenceLevelSystem.computeFireRateRank(state.fireRateMultiplier);
    }

    public boolean hasPickupRangePower(@Nonnull UUID playerId, @Nonnull Store<EntityStore> store) {
        this.loadStateIfMissing(playerId, LifeEssenceLevelSystem.getWorldKey(store));
        PowerState state = (PowerState)this.powerByPlayer.get(playerId);
        return state != null && state.pickupRangeBonus >= 49.9999f;
    }

    public int getPickupRangeRank(@Nonnull UUID playerId, @Nonnull Store<EntityStore> store) {
        this.loadStateIfMissing(playerId, LifeEssenceLevelSystem.getWorldKey(store));
        PowerState state = (PowerState)this.powerByPlayer.get(playerId);
        if (state == null) {
            return 0;
        }
        return LifeEssenceLevelSystem.computePickupRangeRank(state.pickupRangeBonus);
    }

    public boolean hasExtraProjectilePower(@Nonnull UUID playerId, @Nonnull Store<EntityStore> store) {
        this.loadStateIfMissing(playerId, LifeEssenceLevelSystem.getWorldKey(store));
        PowerState state = (PowerState)this.powerByPlayer.get(playerId);
        return state != null && state.projectileCount >= 10;
    }

    public int getExtraProjectileRank(@Nonnull UUID playerId, @Nonnull Store<EntityStore> store) {
        this.loadStateIfMissing(playerId, LifeEssenceLevelSystem.getWorldKey(store));
        PowerState state = (PowerState)this.powerByPlayer.get(playerId);
        if (state == null) {
            return 0;
        }
        return LifeEssenceLevelSystem.clampProjectileCount(state.projectileCount);
    }

    public int getBounceRank(@Nonnull UUID playerId, @Nonnull Store<EntityStore> store) {
        this.loadStateIfMissing(playerId, LifeEssenceLevelSystem.getWorldKey(store));
        PowerState state = (PowerState)this.powerByPlayer.get(playerId);
        if (state == null) {
            return 0;
        }
        return LifeEssenceLevelSystem.clampBounceBonus(state.bounceBonus);
    }

    public int getWeaponDamageRank(@Nonnull UUID playerId, @Nonnull Store<EntityStore> store) {
        this.loadStateIfMissing(playerId, LifeEssenceLevelSystem.getWorldKey(store));
        PowerState state = (PowerState)this.powerByPlayer.get(playerId);
        if (state == null) {
            return 0;
        }
        return LifeEssenceLevelSystem.clampWeaponDamageRank(state.weaponDamageRank);
    }

    public float getWeaponDamageBonus(@Nonnull UUID playerId, @Nonnull Store<EntityStore> store) {
        return LifeEssenceLevelSystem.getWeaponDamageBonusForRank(this.getWeaponDamageRank(playerId, store));
    }

    public int getHealthRank(@Nonnull UUID playerId, @Nonnull Store<EntityStore> store) {
        this.loadStateIfMissing(playerId, LifeEssenceLevelSystem.getWorldKey(store));
        PowerState state = (PowerState)this.powerByPlayer.get(playerId);
        if (state == null) {
            return 0;
        }
        return LifeEssenceLevelSystem.clampHealthRank(state.healthRank);
    }

    public int getSpeedRank(@Nonnull UUID playerId, @Nonnull Store<EntityStore> store) {
        this.loadStateIfMissing(playerId, LifeEssenceLevelSystem.getWorldKey(store));
        PowerState state = (PowerState)this.powerByPlayer.get(playerId);
        if (state == null) {
            return 0;
        }
        return LifeEssenceLevelSystem.clampSpeedRank(state.speedRank);
    }

    public int getProjectileRainRank(@Nonnull UUID playerId, @Nonnull Store<EntityStore> store) {
        return this.isProjectileRainUsedForActiveGroup(playerId, store) ? 1 : 0;
    }

    public int getLuckyRank(@Nonnull UUID playerId, @Nonnull Store<EntityStore> store) {
        this.loadStateIfMissing(playerId, LifeEssenceLevelSystem.getWorldKey(store));
        PowerState state = (PowerState)this.powerByPlayer.get(playerId);
        if (state == null) {
            return 0;
        }
        return LifeEssenceLevelSystem.clampLuckyRank(state.luckyRank);
    }

    public int getMaxFireRateUpgrades() {
        return MAX_FIRE_RATE_UPGRADES;
    }

    public int getMaxPickupRangeUpgrades() {
        return MAX_PICKUP_RANGE_UPGRADES;
    }

    public int getMaxExtraProjectileUpgrades() {
        return MAX_EXTRA_PROJECTILES;
    }

    public int getMaxBounceUpgrades() {
        return MAX_BOUNCE_UPGRADES;
    }

    public int getMaxWeaponDamageUpgrades() {
        return MAX_WEAPON_DAMAGE_UPGRADES;
    }

    public int getMaxHealthUpgrades() {
        return MAX_HEALTH_UPGRADES;
    }

    public int getMaxSpeedUpgrades() {
        return MAX_SPEED_UPGRADES;
    }

    public int getMaxProjectileRainUpgrades() {
        return MAX_PROJECTILE_RAIN_UPGRADES;
    }

    public int getMaxLuckyUpgrades() {
        return MAX_LUCKY_UPGRADES;
    }

    private boolean isProjectileRainUsedForActiveGroup(@Nonnull UUID playerId, @Nonnull Store<EntityStore> store) {
        String worldKey = LifeEssenceLevelSystem.getWorldKey(store);
        this.loadStateIfMissing(playerId, worldKey);
        PowerState selfState = this.powerByPlayer.computeIfAbsent(playerId, id -> new PowerState());
        if (selfState.projectileRainUsed) {
            return true;
        }
        MobWaveSpawner spawner = HytaleMod.getInstance().getMobWaveSpawner();
        if (spawner == null) {
            return false;
        }
        UUID ownerId = spawner.resolveOwnerId(playerId);
        List<Ref<EntityStore>> refs = spawner.getPlayerRefsForOwner(ownerId);
        for (Ref<EntityStore> memberRef : refs) {
            if (memberRef == null || !memberRef.isValid()) {
                continue;
            }
            Store<EntityStore> memberStore = memberRef.getStore();
            if (memberStore == null) {
                continue;
            }
            PlayerRef memberPlayerRef = (PlayerRef)memberStore.getComponent(memberRef, PlayerRef.getComponentType());
            if (memberPlayerRef == null) {
                continue;
            }
            UUID memberId = memberPlayerRef.getUuid();
            this.loadStateIfMissing(memberId, worldKey);
            PowerState memberState = this.powerByPlayer.computeIfAbsent(memberId, id -> new PowerState());
            if (!memberState.projectileRainUsed) continue;
            return true;
        }
        return false;
    }

    private void markProjectileRainUsedForActiveGroup(@Nonnull UUID playerId, @Nonnull Store<EntityStore> store, @Nonnull String worldKey) {
        this.loadStateIfMissing(playerId, worldKey);
        PowerState selfState = this.powerByPlayer.computeIfAbsent(playerId, id -> new PowerState());
        if (!selfState.projectileRainUsed) {
            selfState.projectileRainUsed = true;
        }
        this.savePlayerState(playerId, worldKey);
        MobWaveSpawner spawner = HytaleMod.getInstance().getMobWaveSpawner();
        if (spawner == null) {
            return;
        }
        UUID ownerId = spawner.resolveOwnerId(playerId);
        List<Ref<EntityStore>> refs = spawner.getPlayerRefsForOwner(ownerId);
        for (Ref<EntityStore> memberRef : refs) {
            if (memberRef == null || !memberRef.isValid()) {
                continue;
            }
            Store<EntityStore> memberStore = memberRef.getStore();
            if (memberStore == null) {
                continue;
            }
            PlayerRef memberPlayerRef = (PlayerRef)memberStore.getComponent(memberRef, PlayerRef.getComponentType());
            if (memberPlayerRef == null) {
                continue;
            }
            UUID memberId = memberPlayerRef.getUuid();
            if (memberId.equals(playerId)) {
                continue;
            }
            this.loadStateIfMissing(memberId, worldKey);
            PowerState memberState = this.powerByPlayer.computeIfAbsent(memberId, id -> new PowerState());
            if (!memberState.projectileRainUsed) {
                memberState.projectileRainUsed = true;
                this.savePlayerState(memberId, worldKey);
            }
        }
    }

    private void loadStateIfMissing(@Nonnull UUID playerId, String worldKey) {
        Object data;
        String currentWorld;
        if (worldKey == null) {
            worldKey = "default";
        }
        if ((currentWorld = (String)this.worldByPlayer.get(playerId)) == null || !currentWorld.equals(worldKey)) {
            PlayerStateStore store = this.stateStoreManager.getStore(worldKey);
            PlayerStateStore.PlayerLevelData levelData = store.loadLevel(playerId);
            LevelState level = new LevelState();
            level.level = Math.max(1, levelData.level);
            level.essenceProgress = Math.max(0, levelData.essenceProgress);
            level.lastStoreLevel = Math.max(0, levelData.lastStoreLevel);
            this.levelByPlayer.put(playerId, level);
            PlayerStateStore.PlayerPowerData powerData = store.loadPower(playerId);
            PowerState power = new PowerState();
            power.projectileCount = LifeEssenceLevelSystem.clampProjectileCount(powerData.projectileCount);
            power.fireRateMultiplier = LifeEssenceLevelSystem.clampFireRate(powerData.fireRateMultiplier);
            power.pickupRangeBonus = LifeEssenceLevelSystem.clampPickupRange(powerData.pickupRangeBonus);
            power.bounceBonus = LifeEssenceLevelSystem.clampBounceBonus(powerData.bounceBonus);
            power.weaponDamageRank = LifeEssenceLevelSystem.clampWeaponDamageRank(powerData.weaponDamageRank);
            power.healthRank = LifeEssenceLevelSystem.clampHealthRank(powerData.healthRank);
            power.speedRank = LifeEssenceLevelSystem.clampSpeedRank(powerData.speedRank);
            power.luckyRank = LifeEssenceLevelSystem.clampLuckyRank(powerData.luckyRank);
            power.projectileRainUsed = powerData.projectileRainUsed;
            this.powerByPlayer.put(playerId, power);
            this.worldByPlayer.put(playerId, worldKey);
            return;
        }
        if (!this.levelByPlayer.containsKey(playerId)) {
            data = this.stateStoreManager.getStore(worldKey).loadLevel(playerId);
            LevelState state = new LevelState();
            state.level = Math.max(1, ((PlayerStateStore.PlayerLevelData)data).level);
            state.essenceProgress = Math.max(0, ((PlayerStateStore.PlayerLevelData)data).essenceProgress);
            state.lastStoreLevel = Math.max(0, ((PlayerStateStore.PlayerLevelData)data).lastStoreLevel);
            this.levelByPlayer.put(playerId, state);
        }
        if (!this.powerByPlayer.containsKey(playerId)) {
            data = this.stateStoreManager.getStore(worldKey).loadPower(playerId);
            PowerState power = new PowerState();
            power.projectileCount = LifeEssenceLevelSystem.clampProjectileCount(((PlayerStateStore.PlayerPowerData)data).projectileCount);
            power.fireRateMultiplier = LifeEssenceLevelSystem.clampFireRate(((PlayerStateStore.PlayerPowerData)data).fireRateMultiplier);
            power.pickupRangeBonus = LifeEssenceLevelSystem.clampPickupRange(((PlayerStateStore.PlayerPowerData)data).pickupRangeBonus);
            power.bounceBonus = LifeEssenceLevelSystem.clampBounceBonus(((PlayerStateStore.PlayerPowerData)data).bounceBonus);
            power.weaponDamageRank = LifeEssenceLevelSystem.clampWeaponDamageRank(((PlayerStateStore.PlayerPowerData)data).weaponDamageRank);
            power.healthRank = LifeEssenceLevelSystem.clampHealthRank(((PlayerStateStore.PlayerPowerData)data).healthRank);
            power.speedRank = LifeEssenceLevelSystem.clampSpeedRank(((PlayerStateStore.PlayerPowerData)data).speedRank);
            power.luckyRank = LifeEssenceLevelSystem.clampLuckyRank(((PlayerStateStore.PlayerPowerData)data).luckyRank);
            power.projectileRainUsed = ((PlayerStateStore.PlayerPowerData)data).projectileRainUsed;
            this.powerByPlayer.put(playerId, power);
        }
    }

    private void savePlayerState(@Nonnull UUID playerId, @Nonnull String worldKey) {
        PowerState power;
        PlayerStateStore store = this.stateStoreManager.getStore(worldKey);
        LevelState level = (LevelState)this.levelByPlayer.get(playerId);
        if (level != null) {
            store.saveLevel(playerId, level.level, level.essenceProgress, level.lastStoreLevel);
        }
        if ((power = (PowerState)this.powerByPlayer.get(playerId)) != null) {
            store.savePower(playerId, power.projectileCount, power.fireRateMultiplier, power.pickupRangeBonus, power.bounceBonus, power.weaponDamageRank, power.healthRank, power.speedRank, power.luckyRank, power.projectileRainUsed);
        }
    }

    private void resetTransientPlayerState(@Nonnull UUID playerId) {
        this.levelByPlayer.remove(playerId);
        this.powerByPlayer.remove(playerId);
        this.storeSessionByPlayer.remove(playerId);
        this.storeOpenedAt.remove(playerId);
        this.worldByPlayer.remove(playerId);
        this.lastWasGameplayWorld.remove(playerId);
        this.pendingHudClear.remove(playerId);
    }

    public void resetProgressForPlayers(@Nonnull Iterable<UUID> playerIds, @Nonnull String worldKey) {
        PlayerStateStore store = this.stateStoreManager.getStore(worldKey);
        for (UUID playerId : playerIds) {
            if (playerId == null) {
                continue;
            }
            this.cancelAllScheduledTasks(playerId);
            this.resetTransientPlayerState(playerId);
            store.saveLevel(playerId, 1, 0, 0);
            store.savePower(playerId, 0, 1.0f, 0.0f, 0, 0, 0, 0, 0, false);
        }
    }

    private boolean isGameplayWorld(@Nonnull Store<EntityStore> store) {
        return true;
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

    private BossBarHud ensureHud(@Nonnull Player playerComponent, @Nonnull PlayerRef playerRefComponent, @Nonnull Store<EntityStore> store) {
        CustomUIHud currentHud = playerComponent.getHudManager().getCustomHud();
        boolean inGameplayWorld = this.isGameplayWorld(store);
        if (currentHud instanceof BossBarHud) {
            if (inGameplayWorld) {
                ((BossBarHud)currentHud).setLevelHudVisible(true);
            }
            return (BossBarHud)currentHud;
        }
        if (currentHud != null) {
            if (!inGameplayWorld) {
                this.plugin.getLogger().at(Level.INFO).log("[HUD_DEBUG] ensureHud blocked: existing=%s player=%s", (Object)currentHud.getClass().getSimpleName(), (Object)playerRefComponent.getUuid());
                return null;
            }
            this.plugin.getLogger().at(Level.INFO).log("[HUD_DEBUG] ensureHud replace: existing=%s player=%s", (Object)currentHud.getClass().getSimpleName(), (Object)playerRefComponent.getUuid());
        }
        BossBarHud hud = new BossBarHud(playerRefComponent, DEFAULT_BOSS_NAME, 1.0f);
        this.plugin.getLogger().at(Level.INFO).log("[HUD_DEBUG] ensureHud create player=%s", (Object)playerRefComponent.getUuid());
        playerComponent.getHudManager().setCustomHud(playerRefComponent, (CustomUIHud)hud);
        return hud;
    }

    private void clearHudForPlayer(@Nonnull Player playerComponent, @Nonnull PlayerRef playerRefComponent, @Nonnull UUID playerId) {
        this.plugin.getLogger().at(Level.INFO).log("[HUD_DEBUG] clearHudForPlayer start player=%s", (Object)playerId);
        CustomUIHud currentHud = playerComponent.getHudManager().getCustomHud();
        if (currentHud != null) {
            if (currentHud instanceof BossBarHud) {
                this.plugin.getLogger().at(Level.INFO).log("[HUD_DEBUG] hide BossBarHud player=%s", (Object)playerId);
                ((BossBarHud)currentHud).setBossVisible(false);
                ((BossBarHud)currentHud).setLevelHudVisible(false);
            } else {
                this.plugin.getLogger().at(Level.INFO).log("[HUD_DEBUG] resetHud suppressed (to avoid crash) player=%s hud=%s", (Object)playerId, (Object)currentHud.getClass().getSimpleName());
            }
        } else {
            this.plugin.getLogger().at(Level.INFO).log("[HUD_DEBUG] resetHud suppressed (no currentHud) player=%s", (Object)playerId);
        }
        BossHudSystem bossHudSystem = HytaleMod.getInstance().getBossHudSystem();
        if (bossHudSystem != null) {
            this.plugin.getLogger().at(Level.INFO).log("[HUD_DEBUG] clearBossHud call player=%s", (Object)playerId);
            bossHudSystem.clearBossHud(playerId);
        }
    }

    private void applyJoinDefaults(@Nonnull Ref<EntityStore> playerRef, @Nonnull Store<EntityStore> store, @Nonnull World world) {
        if (!playerRef.isValid()) {
            return;
        }
        Player playerComponent = (Player)store.getComponent(playerRef, Player.getComponentType());
        if (playerComponent != null && this.isGameplayWorld(store)) {
            this.giveJoinWeapon(playerComponent);
        }
        this.forceWeather(world, store);
    }

    private void executeIfTicking(@Nonnull World world, @Nonnull Runnable action) {
        if (!world.isTicking()) {
            return;
        }
        try {
            world.execute(action);
        }
        catch (IllegalThreadStateException ignored) {
        }
    }

    private void scheduleHudUpdate(@Nonnull World world, @Nonnull PlayerRef playerRefComponent, @Nonnull Runnable action) {
        UUID playerId = playerRefComponent.getUuid();
        ScheduledFuture<?> existing = (ScheduledFuture)this.hudUpdateTasks.remove(playerId);
        if (existing != null) {
            existing.cancel(false);
        }
        ScheduledFuture<?> future = HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
            this.hudUpdateTasks.remove(playerId);
            if (!this.isPlayerInWorld(playerRefComponent, world)) {
                return;
            }
            this.executeIfTicking(world, action);
        }, HUD_READY_DELAY_MS, TimeUnit.MILLISECONDS);
        this.hudUpdateTasks.put(playerId, future);
    }

    private ScheduledFuture<?> schedulePlayerWorldTask(@Nonnull World world, @Nonnull PlayerRef playerRefComponent, @Nonnull Runnable action, long delayMillis) {
        UUID playerId = playerRefComponent.getUuid();
        final ScheduledFuture<?>[] holder = new ScheduledFuture<?>[1];
        ScheduledFuture<?> future = HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
            ScheduledFuture<?> self = holder[0];
            if (self != null) {
                this.untrackPlayerTask(playerId, self);
            }
            if (!this.isPlayerInWorld(playerRefComponent, world)) {
                return;
            }
            this.executeIfTicking(world, action);
        }, delayMillis, TimeUnit.MILLISECONDS);
        holder[0] = future;
        this.trackPlayerTask(playerId, future);
        return future;
    }

    private void cancelHudUpdate(@Nonnull UUID playerId) {
        ScheduledFuture<?> existing = (ScheduledFuture)this.hudUpdateTasks.remove(playerId);
        if (existing != null) {
            existing.cancel(false);
        }
    }

    private void trackPlayerTask(@Nonnull UUID playerId, @Nonnull ScheduledFuture<?> future) {
        this.delayedTasksByPlayer.computeIfAbsent(playerId, id -> new ConcurrentLinkedQueue()).add(future);
    }

    private void untrackPlayerTask(@Nonnull UUID playerId, @Nonnull ScheduledFuture<?> future) {
        ConcurrentLinkedQueue<ScheduledFuture<?>> tasks = (ConcurrentLinkedQueue)this.delayedTasksByPlayer.get(playerId);
        if (tasks == null) {
            return;
        }
        tasks.remove(future);
        if (tasks.isEmpty()) {
            this.delayedTasksByPlayer.remove(playerId, tasks);
        }
    }

    private void cancelPlayerTasks(@Nonnull UUID playerId) {
        ConcurrentLinkedQueue<ScheduledFuture<?>> tasks = (ConcurrentLinkedQueue)this.delayedTasksByPlayer.remove(playerId);
        if (tasks == null) {
            return;
        }
        for (ScheduledFuture<?> task : tasks) {
            if (task == null) {
                continue;
            }
            task.cancel(false);
        }
    }

    private void cancelAllScheduledTasks(@Nonnull UUID playerId) {
        this.cancelHudUpdate(playerId);
        this.cancelPlayerTasks(playerId);
        ScheduledFuture<?> timeout = (ScheduledFuture)this.storeTimeouts.remove(playerId);
        if (timeout != null) {
            timeout.cancel(false);
        }
    }

    private boolean isPlayerInWorld(@Nonnull PlayerRef playerRefComponent, @Nonnull World world) {
        Ref playerRef = playerRefComponent.getReference();
        if (playerRef == null || !playerRef.isValid()) {
            return false;
        }
        Store store = playerRef.getStore();
        if (store == null) {
            return false;
        }
        World currentWorld = ((EntityStore)store.getExternalData()).getWorld();
        if (currentWorld == null) {
            return false;
        }
        WorldConfig currentConfig = currentWorld.getWorldConfig();
        WorldConfig targetConfig = world.getWorldConfig();
        if (currentConfig != null && targetConfig != null && currentConfig.getUuid() != null && targetConfig.getUuid() != null) {
            return currentConfig.getUuid().equals(targetConfig.getUuid());
        }
        String currentName = currentWorld.getName();
        String targetName = world.getName();
        if (currentName != null && targetName != null) {
            return currentName.equals(targetName);
        }
        return currentWorld == world;
    }

    private void switchInventoryForWorld(@Nonnull UUID playerId, String worldKey, @Nonnull Player playerComponent) {
        Inventory inventory = playerComponent.getInventory();
        if (inventory == null) {
            return;
        }
        if (worldKey == null) {
            worldKey = "default";
        }
        String previousWorld = (String)this.inventoryWorldByPlayer.get(playerId);
        if (worldKey.equals(previousWorld)) {
            return;
        }
        if (previousWorld != null) {
            this.saveInventorySnapshot(playerId, previousWorld, playerComponent);
        }
        ConcurrentMap<String, InventorySnapshot> perPlayer = (ConcurrentMap)this.inventoriesByPlayer.computeIfAbsent(playerId, id -> new ConcurrentHashMap());
        InventorySnapshot snapshot = (InventorySnapshot)perPlayer.get(worldKey);
        if (snapshot != null) {
            snapshot.restore(inventory);
            this.inventoryWorldByPlayer.put(playerId, worldKey);
            return;
        }
        if (previousWorld == null) {
            this.saveInventorySnapshot(playerId, worldKey, playerComponent);
            this.inventoryWorldByPlayer.put(playerId, worldKey);
            return;
        }
        this.clearInventory(inventory);
        this.saveInventorySnapshot(playerId, worldKey, playerComponent);
        this.inventoryWorldByPlayer.put(playerId, worldKey);
    }

    private void saveInventorySnapshot(@Nonnull UUID playerId, String worldKey, @Nonnull Player playerComponent) {
        Inventory inventory = playerComponent.getInventory();
        if (inventory == null) {
            return;
        }
        if (worldKey == null) {
            worldKey = (String)this.inventoryWorldByPlayer.get(playerId);
        }
        if (worldKey == null) {
            worldKey = "default";
        }
        InventorySnapshot snapshot = InventorySnapshot.capture(inventory);
        if (snapshot == null) {
            return;
        }
        ConcurrentMap<String, InventorySnapshot> perPlayer = (ConcurrentMap)this.inventoriesByPlayer.computeIfAbsent(playerId, id -> new ConcurrentHashMap());
        perPlayer.put(worldKey, snapshot);
    }

    private void clearInventory(@Nonnull Inventory inventory) {
        inventory.clear();
        inventory.markChanged();
    }

    private void resetCamera(@Nonnull PlayerRef playerRefComponent) {
        playerRefComponent.getPacketHandler().writeNoCache(new SetServerCamera(ClientCameraView.FirstPerson, false, null));
    }

    private void forceTopdownCameraDirect(@Nonnull PlayerRef playerRefComponent) {
        ServerCameraSettings settings = new ServerCameraSettings();
        settings.attachedToType = AttachedToType.LocalPlayer;
        settings.positionType = PositionType.AttachedToPlusOffset;
        settings.positionOffset = new Position(0.0, 0.0, 0.0);
        settings.positionLerpSpeed = 0.2f;
        settings.rotationLerpSpeed = 0.2f;
        settings.distance = 20.0f;
        settings.displayCursor = true;
        settings.isFirstPerson = false;
        settings.allowPitchControls = false;
        settings.movementForceRotationType = MovementForceRotationType.Custom;
        settings.eyeOffset = true;
        settings.positionDistanceOffsetType = PositionDistanceOffsetType.DistanceOffset;
        settings.rotationType = RotationType.Custom;
        settings.rotation = new Direction(0.0f, -1.5707964f, 0.0f);
        settings.mouseInputType = MouseInputType.LookAtPlane;
        settings.mouseInputTargetType = MouseInputTargetType.None;
        settings.sendMouseMotion = false;
        settings.planeNormal = new Vector3f(0.0f, 1.0f, 0.0f);
        playerRefComponent.getPacketHandler().writeNoCache(new SetServerCamera(ClientCameraView.Custom, true, settings));
    }

    private void giveJoinWeapon(@Nonnull Player playerComponent) {
        Inventory inventory = playerComponent.getInventory();
        if (inventory == null) {
            return;
        }
        boolean changed = false;
        changed |= this.ensureJoinItem(inventory, this.resolveFirstExistingItemId("battleaxe", JOIN_WEAPON_ITEM_ID, JOIN_WEAPON_ITEM_ID_FALLBACK), 1, true);
        changed |= this.ensureJoinItem(inventory, this.resolveFirstExistingItemId("shortbow", JOIN_SHORTBOW_ITEM_ID, JOIN_SHORTBOW_ITEM_ID_FALLBACK), 1, true);
        changed |= this.ensureJoinItem(inventory, this.resolveFirstExistingItemId("arrows", JOIN_ARROW_ITEM_ID), JOIN_ARROW_QUANTITY, false);
        changed |= this.ensureJoinItem(inventory, this.resolveFirstExistingItemId("food", JOIN_FOOD_ITEM_ID), JOIN_FOOD_QUANTITY, false);
        changed |= this.ensureJoinItem(inventory, this.resolveFirstExistingItemId("helmet", JOIN_ARMOR_HEAD_ITEM_ID), 1, true);
        changed |= this.ensureJoinItem(inventory, this.resolveFirstExistingItemId("chest", JOIN_ARMOR_CHEST_ITEM_ID), 1, true);
        changed |= this.ensureJoinItem(inventory, this.resolveFirstExistingItemId("hands", JOIN_ARMOR_HANDS_ITEM_ID, JOIN_ARMOR_HANDS_ITEM_ID_FALLBACK), 1, true);
        changed |= this.ensureJoinItem(inventory, this.resolveFirstExistingItemId("legs", JOIN_ARMOR_LEGS_ITEM_ID, JOIN_ARMOR_LEGS_ITEM_ID_FALLBACK), 1, true);
        if (changed) {
            inventory.markChanged();
        }
    }

    private boolean ensureJoinItem(@Nonnull Inventory inventory, String itemId, int minimumQuantity, boolean durable) {
        if (itemId == null || minimumQuantity <= 0) {
            return false;
        }
        CombinedItemContainer allItems = inventory.getCombinedEverything();
        int existingQuantity = this.getTotalItemQuantity(allItems, itemId);
        if (existingQuantity >= minimumQuantity) {
            return false;
        }
        int quantityToAdd = minimumQuantity - existingQuantity;
        CombinedItemContainer target = inventory.getCombinedHotbarFirst();
        if (target == null) {
            return false;
        }
        target.addItemStack(this.createJoinItemStack(itemId, quantityToAdd, durable));
        return true;
    }

    private int getTotalItemQuantity(ItemContainer container, @Nonnull String itemId) {
        if (container == null) {
            return 0;
        }
        int total = 0;
        int capacity = Math.max(0, container.getCapacity());
        for (short i = 0; i < capacity; i = (short)(i + 1)) {
            ItemStack stack = container.getItemStack(i);
            if (stack == null || stack.isEmpty() || !itemId.equals(stack.getItemId())) continue;
            total += Math.max(0, stack.getQuantity());
        }
        return total;
    }

    private ItemStack createJoinItemStack(@Nonnull String itemId, int quantity, boolean durable) {
        if (durable) {
            return new ItemStack(itemId, quantity, JOIN_DEFAULT_DURABILITY, JOIN_DEFAULT_DURABILITY, null);
        }
        return new ItemStack(itemId, quantity);
    }

    private String resolveFirstExistingItemId(@Nonnull String label, @Nonnull String ... itemIds) {
        for (String itemId : itemIds) {
            if (itemId == null || itemId.isBlank() || Item.getAssetMap().getAsset(itemId) == null) continue;
            return itemId;
        }
        this.plugin.getLogger().at(Level.WARNING).log("Join loadout item not found for %s: %s", (Object)label, (Object)String.join(", ", itemIds));
        return null;
    }

    private void forceWeather(@Nonnull World world, @Nonnull Store<EntityStore> store) {
        WeatherResource weather = (WeatherResource)store.getResource(WeatherResource.getResourceType());
        if (weather != null) {
            weather.setForcedWeather(JOIN_FORCED_WEATHER_ID);
        }
    }

    private int getLifeEssenceDelta(@Nonnull Transaction transaction) {
        if (transaction instanceof ItemStackTransaction) {
            ItemStackTransaction itemTransaction = (ItemStackTransaction)transaction;
            if (!itemTransaction.succeeded()) {
                return 0;
            }
            int delta = 0;
            for (ItemStackSlotTransaction slotTransaction : itemTransaction.getSlotTransactions()) {
                delta += this.getDeltaFromSlot((SlotTransaction)slotTransaction);
            }
            return delta;
        }
        if (transaction instanceof SlotTransaction) {
            return this.getDeltaFromSlot((SlotTransaction)transaction);
        }
        return 0;
    }

    private int getDeltaFromSlot(@Nonnull SlotTransaction slotTransaction) {
        ItemStack before = slotTransaction.getSlotBefore();
        ItemStack after = slotTransaction.getSlotAfter();
        return this.getLifeEssenceCount(after) - this.getLifeEssenceCount(before);
    }

    private int getLifeEssenceCount(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        if (!"Ingredient_Life_Essence".equals(stack.getItemId())) {
            return 0;
        }
        return Math.max(0, stack.getQuantity());
    }

    private static int clampProjectileCount(int count) {
        return Math.max(0, Math.min(MAX_EXTRA_PROJECTILES, count));
    }

    private static int clampBounceBonus(int bonus) {
        return Math.max(0, Math.min(MAX_BOUNCE_UPGRADES, bonus));
    }

    private static int clampWeaponDamageRank(int rank) {
        return Math.max(0, Math.min(MAX_WEAPON_DAMAGE_UPGRADES, rank));
    }

    private static int clampHealthRank(int rank) {
        return Math.max(0, Math.min(MAX_HEALTH_UPGRADES, rank));
    }

    private static int clampSpeedRank(int rank) {
        return Math.max(0, Math.min(MAX_SPEED_UPGRADES, rank));
    }

    private static int clampLuckyRank(int rank) {
        return Math.max(0, Math.min(MAX_LUCKY_UPGRADES, rank));
    }

    private static float getWeaponDamageBonusForRank(int rank) {
        return (float)LifeEssenceLevelSystem.clampWeaponDamageRank(rank) * WEAPON_DAMAGE_BONUS_PER_UPGRADE;
    }

    private static float getSpeedMultiplierForRank(int rank) {
        int clampedRank = LifeEssenceLevelSystem.clampSpeedRank(rank);
        return Math.min(MAX_SPEED_MULTIPLIER, 1.0f + (float)clampedRank * SPEED_BONUS_PER_UPGRADE);
    }

    private static int computeFireRateRank(float multiplier) {
        if (!Float.isFinite(multiplier) || multiplier <= 1.0f) {
            return 0;
        }
        float current = 1.0f;
        int rank = 0;
        for (int i = 1; i <= 4; ++i) {
            if (!(multiplier >= (current *= 1.15f) - 1.0E-4f)) continue;
            rank = i;
        }
        return rank;
    }

    private static int computePickupRangeRank(float bonus) {
        if (!Float.isFinite(bonus) || bonus <= 0.0f) {
            return 0;
        }
        int rank = (int)Math.floor(bonus / 10.0f + 1.0E-4f);
        if (rank < 0) {
            return 0;
        }
        return Math.min(MAX_PICKUP_RANGE_UPGRADES, rank);
    }

    private static float clampFireRate(float multiplier) {
        if (!Float.isFinite(multiplier) || multiplier <= 0.0f) {
            return 1.0f;
        }
        return Math.min(MAX_FIRE_RATE_MULTIPLIER, Math.max(0.1f, multiplier));
    }

    private static float clampPickupRange(float bonus) {
        if (!Float.isFinite(bonus) || bonus < 0.0f) {
            return 0.0f;
        }
        return Math.min(MAX_PICKUP_RANGE_BONUS, bonus);
    }

    private static ItemStack cloneItemStack(@Nonnull ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        BsonDocument metadata = stack.getMetadata();
        BsonDocument cloned = metadata == null ? null : metadata.clone();
        return new ItemStack(stack.getItemId(), stack.getQuantity(), stack.getDurability(), stack.getMaxDurability(), cloned);
    }

    private static ItemStack[] snapshotContainer(@Nonnull ItemContainer container) {
        int capacity = Math.max(0, container.getCapacity());
        ItemStack[] items = new ItemStack[capacity];
        for (short i = 0; i < capacity; i = (short)(i + 1)) {
            ItemStack stack = container.getItemStack(i);
            if (stack == null || stack.isEmpty()) {
                items[i] = ItemStack.EMPTY;
                continue;
            }
            items[i] = LifeEssenceLevelSystem.cloneItemStack(stack);
        }
        return items;
    }

    private static void restoreContainer(@Nonnull ItemContainer container, ItemStack[] items) {
        container.clear();
        if (items == null || items.length == 0) {
            return;
        }
        int limit = Math.min(container.getCapacity(), items.length);
        for (short i = 0; i < limit; i = (short)(i + 1)) {
            ItemStack stack = items[i];
            if (stack == null || stack.isEmpty()) {
                container.setItemStackForSlot(i, ItemStack.EMPTY);
                continue;
            }
            container.setItemStackForSlot(i, stack);
        }
    }

    private void openStoreForPlayer(@Nonnull Ref<EntityStore> playerRef, @Nonnull Store<EntityStore> store, @Nonnull Player playerComponent, @Nonnull PlayerRef playerRefComponent, int level) {
        if (!this.isGameplayWorld(store)) {
            return;
        }
        UUID playerId = playerRefComponent.getUuid();
        World world = ((EntityStore)store.getExternalData()).getWorld();
        if (world == null) {
            return;
        }
        int sessionId = ((Integer)this.storeSessionByPlayer.compute(playerId, (id, value) -> value == null ? 1 : value + 1)).intValue();
        ScheduledFuture<?> timeout = this.schedulePlayerWorldTask(world, playerRefComponent, () -> this.closeStoreInternal(playerRef, store, playerComponent, playerRefComponent, sessionId), 30000L);
        ScheduledFuture<?> existing = (ScheduledFuture)this.storeTimeouts.put(playerId, timeout);
        if (existing != null) {
            existing.cancel(false);
        }
        for (int i = 1; i <= 8; ++i) {
            float t = (float)i / 8.0f;
            float factor = 1.0f + -0.8f * t;
            long delay = 1800L * (long)i / 8L;
            this.schedulePlayerWorldTask(world, playerRefComponent, () -> World.setTimeDilation((float)factor, (ComponentAccessor)store), delay);
        }
        this.schedulePlayerWorldTask(world, playerRefComponent, () -> {
            World.setTimeDilation((float)1.0f, (ComponentAccessor)store);
            if (this.storePauseCount.getAndIncrement() == 0) {
                world.setPaused(true);
            }
            this.storeOpenedAt.put(playerId, System.currentTimeMillis());
            playerComponent.getPageManager().openCustomPage(playerRef, store, (CustomUIPage)new PowerUpStorePage(playerRefComponent, level));
        }, 1800L);
    }

    public void closeStoreForPlayer(@Nonnull Ref<EntityStore> playerRef, @Nonnull Store<EntityStore> store) {
        PlayerRef playerRefComponent = (PlayerRef)store.getComponent(playerRef, PlayerRef.getComponentType());
        if (playerRefComponent == null) {
            return;
        }
        Player playerComponent = (Player)store.getComponent(playerRef, Player.getComponentType());
        if (playerComponent == null) {
            return;
        }
        World world = ((EntityStore)store.getExternalData()).getWorld();
        if (world == null) {
            return;
        }
        Integer sessionId = (Integer)this.storeSessionByPlayer.get(playerRefComponent.getUuid());
        if (sessionId == null) {
            return;
        }
        world.execute(() -> this.closeStoreInternal(playerRef, store, playerComponent, playerRefComponent, sessionId.intValue()));
    }

    public boolean canAcceptStoreSelection(@Nonnull UUID playerId) {
        Long openedAt = (Long)this.storeOpenedAt.get(playerId);
        if (openedAt == null) {
            return true;
        }
        return System.currentTimeMillis() - openedAt.longValue() >= STORE_INPUT_GRACE_MS;
    }

    private void closeStoreInternal(@Nonnull Ref<EntityStore> playerRef, @Nonnull Store<EntityStore> store, @Nonnull Player playerComponent, @Nonnull PlayerRef playerRefComponent, int sessionId) {
        int remaining;
        UUID playerId = playerRefComponent.getUuid();
        Integer currentSession = (Integer)this.storeSessionByPlayer.get(playerId);
        if (currentSession == null || currentSession.intValue() != sessionId) {
            return;
        }
        ScheduledFuture timeout = (ScheduledFuture)this.storeTimeouts.remove(playerId);
        if (timeout == null) {
            return;
        }
        timeout.cancel(false);
        this.storeSessionByPlayer.remove(playerId, sessionId);
        this.storeOpenedAt.remove(playerId);
        CustomUIPage currentPage = playerComponent.getPageManager().getCustomPage();
        if (currentPage instanceof PowerUpStorePage) {
            ((PowerUpStorePage)currentPage).requestClose();
        }
        if ((remaining = this.storePauseCount.decrementAndGet()) <= 0) {
            this.storePauseCount.set(0);
            World world = ((EntityStore)store.getExternalData()).getWorld();
            if (world != null) {
                World.setTimeDilation((float)1.0f, store);
                world.setPaused(false);
            }
        }
    }

    private static class LevelState {
        private int level = 1;
        private int essenceProgress;
        private int lastStoreLevel = 0;

        private LevelState() {
        }

        private int addEssence(int amount) {
            if (amount <= 0) {
                return this.level;
            }
            int previousLevel = this.level;
            int total = this.essenceProgress + amount;
            while (total >= LevelState.getEssenceRequiredForLevel(this.level)) {
                total -= LevelState.getEssenceRequiredForLevel(this.level);
                ++this.level;
            }
            this.essenceProgress = total;
            return previousLevel;
        }

        private boolean shouldOpenStore(int level) {
            return this.lastStoreLevel < LevelState.getLatestEligibleStoreLevel(level);
        }

        private void markStoreOpened(int level) {
            int latestEligible = LevelState.getLatestEligibleStoreLevel(level);
            if (latestEligible <= 0) {
                return;
            }
            this.lastStoreLevel = Math.max(this.lastStoreLevel, latestEligible);
        }

        private int getEssenceRequired() {
            return LevelState.getEssenceRequiredForLevel(this.level);
        }

        private static int getEssenceRequiredForLevel(int level) {
            int safeLevel = Math.max(1, level);
            return Math.min(ESSENCE_PER_LEVEL_MAX, ESSENCE_PER_LEVEL_BASE + (safeLevel - 1) * ESSENCE_PER_LEVEL_INCREMENT);
        }

        private static int getLatestEligibleStoreLevel(int level) {
            if (level < STORE_OPEN_LEVEL_START) {
                return 0;
            }
            int cappedLevel = Math.min(level, STORE_OPEN_LEVEL_MAX);
            int steps = (cappedLevel - STORE_OPEN_LEVEL_START) / STORE_OPEN_LEVEL_INTERVAL;
            return STORE_OPEN_LEVEL_START + steps * STORE_OPEN_LEVEL_INTERVAL;
        }
    }

    private static class PowerState {
        private int projectileCount;
        private float fireRateMultiplier = 1.0f;
        private float pickupRangeBonus = 0.0f;
        private int bounceBonus;
        private int weaponDamageRank;
        private int healthRank;
        private int speedRank;
        private int luckyRank;
        private boolean projectileRainUsed;

        private PowerState() {
        }
    }

    private static final class InventorySnapshot {
        private final ItemStack[] hotbar;
        private final ItemStack[] storage;
        private final ItemStack[] armor;
        private final ItemStack[] utility;
        private final ItemStack[] tools;
        private final ItemStack[] backpack;

        private InventorySnapshot(ItemStack[] hotbar, ItemStack[] storage, ItemStack[] armor, ItemStack[] utility, ItemStack[] tools, ItemStack[] backpack) {
            this.hotbar = hotbar;
            this.storage = storage;
            this.armor = armor;
            this.utility = utility;
            this.tools = tools;
            this.backpack = backpack;
        }

        private static InventorySnapshot capture(@Nonnull Inventory inventory) {
            ItemContainer hotbar = inventory.getHotbar();
            ItemContainer storage = inventory.getStorage();
            ItemContainer armor = inventory.getArmor();
            ItemContainer utility = inventory.getUtility();
            ItemContainer tools = inventory.getTools();
            ItemContainer backpack = inventory.getBackpack();
            return new InventorySnapshot(hotbar != null ? LifeEssenceLevelSystem.snapshotContainer(hotbar) : new ItemStack[0], storage != null ? LifeEssenceLevelSystem.snapshotContainer(storage) : new ItemStack[0], armor != null ? LifeEssenceLevelSystem.snapshotContainer(armor) : new ItemStack[0], utility != null ? LifeEssenceLevelSystem.snapshotContainer(utility) : new ItemStack[0], tools != null ? LifeEssenceLevelSystem.snapshotContainer(tools) : new ItemStack[0], backpack != null ? LifeEssenceLevelSystem.snapshotContainer(backpack) : new ItemStack[0]);
        }

        private void restore(@Nonnull Inventory inventory) {
            ItemContainer hotbar = inventory.getHotbar();
            ItemContainer storage = inventory.getStorage();
            ItemContainer armor = inventory.getArmor();
            ItemContainer utility = inventory.getUtility();
            ItemContainer tools = inventory.getTools();
            ItemContainer backpack = inventory.getBackpack();
            if (hotbar != null) {
                LifeEssenceLevelSystem.restoreContainer(hotbar, this.hotbar);
            }
            if (storage != null) {
                LifeEssenceLevelSystem.restoreContainer(storage, this.storage);
            }
            if (armor != null) {
                LifeEssenceLevelSystem.restoreContainer(armor, this.armor);
            }
            if (utility != null) {
                LifeEssenceLevelSystem.restoreContainer(utility, this.utility);
            }
            if (tools != null) {
                LifeEssenceLevelSystem.restoreContainer(tools, this.tools);
            }
            if (backpack != null) {
                LifeEssenceLevelSystem.restoreContainer(backpack, this.backpack);
            }
            inventory.markChanged();
        }
    }
}
