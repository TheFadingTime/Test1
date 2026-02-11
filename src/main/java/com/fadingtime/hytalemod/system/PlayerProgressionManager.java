package com.fadingtime.hytalemod.system;

import com.fadingtime.hytalemod.HytaleMod;
import com.fadingtime.hytalemod.component.VampireShooterComponent;
import com.fadingtime.hytalemod.config.LifeEssenceConfig;
import com.fadingtime.hytalemod.config.ConfigManager;
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

public class PlayerProgressionManager {
    private static volatile int ESSENCE_PER_LEVEL_BASE = 15;
    private static volatile int ESSENCE_PER_LEVEL_INCREMENT = 5;
    private static volatile int ESSENCE_PER_LEVEL_MAX = 15;
    private static volatile String DEFAULT_BOSS_NAME = "Giant Skeleton";
    private static volatile float STORE_SLOWMO_FACTOR = 0.2f;
    private static volatile int STORE_OPEN_LEVEL_START = 5;
    private static volatile int STORE_OPEN_LEVEL_INTERVAL = 5;
    private static volatile int STORE_OPEN_LEVEL_MAX = 50;
    private static volatile int MAX_FIRE_RATE_UPGRADES = 4;
    private static volatile float FIRE_RATE_MULTIPLIER_PER_UPGRADE = 1.15f;
    private static volatile float MAX_FIRE_RATE_MULTIPLIER = (float)Math.pow(1.15f, 4.0);
    private static volatile int MAX_PICKUP_RANGE_UPGRADES = 5;
    private static volatile float PICKUP_RANGE_PER_UPGRADE = 10.0f;
    private static volatile float MAX_PICKUP_RANGE_BONUS = 50.0f;
    private static volatile int MAX_EXTRA_PROJECTILES = 10;
    private static volatile int MAX_BOUNCE_UPGRADES = 3;
    private static volatile int MAX_WEAPON_DAMAGE_UPGRADES = 2;
    private static volatile float WEAPON_DAMAGE_BONUS_PER_UPGRADE = 15.0f;
    private static volatile int MAX_HEALTH_UPGRADES = 3;
    private static volatile float HEALTH_BONUS_PER_UPGRADE = 20.0f;
    private static volatile int MAX_SPEED_UPGRADES = 1;
    private static volatile int MAX_PROJECTILE_RAIN_UPGRADES = 1;
    private static volatile int MAX_LUCKY_UPGRADES = 5;
    private static volatile int PROJECTILE_RAIN_BURSTS_ON_PICK = 2;
    private static volatile float SPEED_BONUS_PER_UPGRADE = 0.05f;
    private static volatile float MAX_SPEED_MULTIPLIER = 1.05f;
    private static volatile float PLAYER_BASE_SPEED = 10.0f;
    private static final String PLAYER_HEALTH_BONUS_MODIFIER_ID = "PlayerHealthBonus";
    private static final String PLAYER_SPEED_STAMINA_BONUS_MODIFIER_ID = "PlayerSpeedStaminaBonus";
    private static volatile float STAMINA_BONUS_PER_SPEED_UPGRADE = 20.0f;
    private static volatile int LUCKY_ESSENCE_BONUS_PER_RANK = 1;
    private static volatile long HUD_READY_DELAY_MS = 500L;
    private static volatile long STORE_INPUT_GRACE_MS = 400L;
    private static volatile String JOIN_WEAPON_ITEM_ID = "Weapon_Battleaxe_Mithril";
    private static volatile String JOIN_SHORTBOW_ITEM_ID = "Weapon_Shortbow_Mithril";
    private static volatile String JOIN_ARROW_ITEM_ID = "Weapon_Arrow_Crude";
    private static volatile String JOIN_FOOD_ITEM_ID = "Food_Bread";
    private static volatile String JOIN_ARMOR_HEAD_ITEM_ID = "Armor_Mithril_Head";
    private static volatile String JOIN_ARMOR_CHEST_ITEM_ID = "Armor_Mithril_Chest";
    private static volatile String JOIN_ARMOR_HANDS_ITEM_ID = "Armor_Mithril_Hands";
    private static volatile String JOIN_ARMOR_LEGS_ITEM_ID = "Armor_Mithril_Legs";
    private static volatile int JOIN_ARROW_QUANTITY = 200;
    private static volatile int JOIN_FOOD_QUANTITY = 25;
    private static volatile int JOIN_DEFAULT_DURABILITY = 5000;
    private static volatile String JOIN_FORCED_WEATHER_ID = "Zone4_Lava_Fields";
    private final HytaleMod plugin;
    private final LevelingSystem levelingSystem;
    private final PowerUpApplicator powerUpApplicator;
    private final PlayerDataRepository playerDataRepository;
    private final InventoryStateManager inventoryStateManager;
    private final GamePauseController gamePauseController;
    private final HudUpdateService hudUpdateService;
    private final StoreSessionManager storeSessionManager;
    private final ConcurrentMap<UUID, Boolean> lastWasGameplayWorld = new ConcurrentHashMap<UUID, Boolean>();
    private final ConcurrentMap<UUID, Boolean> pendingHudClear = new ConcurrentHashMap<UUID, Boolean>();
    private final ConcurrentMap<UUID, ConcurrentLinkedQueue<ScheduledFuture<?>>> delayedTasksByPlayer = new ConcurrentHashMap();
    private final ConcurrentMap<UUID, Long> disconnectHandledAt = new ConcurrentHashMap<UUID, Long>();

    public PlayerProgressionManager(@Nonnull HytaleMod plugin) {
        this.plugin = plugin;
        LifeEssenceConfig config = LifeEssenceConfig.load(plugin.getFile(), HytaleMod.LOGGER);
        PlayerProgressionManager.applyConfig(config);
        this.levelingSystem = LevelingSystem.fromConfig(config);
        this.powerUpApplicator = PowerUpApplicator.fromConfig(config);
        this.playerDataRepository = new PlayerDataRepository(plugin.getStateStoreManager(), this.levelingSystem, this.powerUpApplicator);
        this.inventoryStateManager = new InventoryStateManager();
        this.gamePauseController = new GamePauseController(config.storeSlowmoFactor);
        this.hudUpdateService = new HudUpdateService(plugin, config.defaultBossName, config.hudReadyDelayMs);
        this.storeSessionManager = new StoreSessionManager(plugin, this.gamePauseController, config.storeInputGraceMs);
        plugin.getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, this::onPlayerAddedToWorld);
        plugin.getEventRegistry().registerGlobal(DrainPlayerFromWorldEvent.class, this::onPlayerDrain);
        plugin.getEventRegistry().registerGlobal(PlayerReadyEvent.class, this::onPlayerReady);
        plugin.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, this::onPlayerDisconnect);
        plugin.getEventRegistry().registerGlobal(LivingEntityInventoryChangeEvent.class, this::onInventoryChange);
    }

    private static synchronized void applyConfig(@Nonnull LifeEssenceConfig config) {
        ESSENCE_PER_LEVEL_BASE = config.essencePerLevelBase;
        ESSENCE_PER_LEVEL_INCREMENT = config.essencePerLevelIncrement;
        ESSENCE_PER_LEVEL_MAX = config.essencePerLevelMax;
        DEFAULT_BOSS_NAME = config.defaultBossName;
        STORE_SLOWMO_FACTOR = config.storeSlowmoFactor;
        STORE_OPEN_LEVEL_START = config.storeOpenLevelStart;
        STORE_OPEN_LEVEL_INTERVAL = config.storeOpenLevelInterval;
        STORE_OPEN_LEVEL_MAX = config.storeOpenLevelMax;
        MAX_FIRE_RATE_UPGRADES = config.maxFireRateUpgrades;
        FIRE_RATE_MULTIPLIER_PER_UPGRADE = config.fireRateMultiplierPerUpgrade;
        MAX_FIRE_RATE_MULTIPLIER = (float)Math.pow(FIRE_RATE_MULTIPLIER_PER_UPGRADE, MAX_FIRE_RATE_UPGRADES);
        MAX_PICKUP_RANGE_UPGRADES = config.maxPickupRangeUpgrades;
        PICKUP_RANGE_PER_UPGRADE = config.pickupRangePerUpgrade;
        MAX_PICKUP_RANGE_BONUS = config.maxPickupRangeBonus;
        MAX_EXTRA_PROJECTILES = config.maxExtraProjectiles;
        MAX_BOUNCE_UPGRADES = config.maxBounceUpgrades;
        MAX_WEAPON_DAMAGE_UPGRADES = config.maxWeaponDamageUpgrades;
        WEAPON_DAMAGE_BONUS_PER_UPGRADE = config.weaponDamageBonusPerUpgrade;
        MAX_HEALTH_UPGRADES = config.maxHealthUpgrades;
        HEALTH_BONUS_PER_UPGRADE = config.healthBonusPerUpgrade;
        MAX_SPEED_UPGRADES = config.maxSpeedUpgrades;
        MAX_PROJECTILE_RAIN_UPGRADES = config.maxProjectileRainUpgrades;
        MAX_LUCKY_UPGRADES = config.maxLuckyUpgrades;
        PROJECTILE_RAIN_BURSTS_ON_PICK = config.projectileRainBurstsOnPick;
        SPEED_BONUS_PER_UPGRADE = config.speedBonusPerUpgrade;
        MAX_SPEED_MULTIPLIER = config.maxSpeedMultiplier;
        PLAYER_BASE_SPEED = config.playerBaseSpeed;
        STAMINA_BONUS_PER_SPEED_UPGRADE = config.staminaBonusPerSpeedUpgrade;
        LUCKY_ESSENCE_BONUS_PER_RANK = config.luckyEssenceBonusPerRank;
        HUD_READY_DELAY_MS = config.hudReadyDelayMs;
        STORE_INPUT_GRACE_MS = config.storeInputGraceMs;
        JOIN_WEAPON_ITEM_ID = config.joinWeaponItemId;
        JOIN_SHORTBOW_ITEM_ID = config.joinShortbowItemId;
        JOIN_ARROW_ITEM_ID = config.joinArrowItemId;
        JOIN_FOOD_ITEM_ID = config.joinFoodItemId;
        JOIN_ARMOR_HEAD_ITEM_ID = config.joinArmorHeadItemId;
        JOIN_ARMOR_CHEST_ITEM_ID = config.joinArmorChestItemId;
        JOIN_ARMOR_HANDS_ITEM_ID = config.joinArmorHandsItemId;
        JOIN_ARMOR_LEGS_ITEM_ID = config.joinArmorLegsItemId;
        JOIN_ARROW_QUANTITY = config.joinArrowQuantity;
        JOIN_FOOD_QUANTITY = config.joinFoodQuantity;
        JOIN_DEFAULT_DURABILITY = config.joinDefaultDurability;
        JOIN_FORCED_WEATHER_ID = config.joinForcedWeatherId;
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
        String worldKey = PlayerProgressionManager.getWorldKey((Store<EntityStore>)store);
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
            LevelingSystem.LevelProgress state = this.playerDataRepository.getOrCreateLevel(playerId);
            BossBarHud hud = this.ensureHud(playerComponent, playerRefComponent, (Store<EntityStore>)store);
            if (hud != null) {
                hud.setLevelHudVisible(true);
                this.plugin.getLogger().at(Level.INFO).log("[HUD_DEBUG] updateLevel (add) player=%s world=%s level=%d essence=%d/%d", (Object)playerId, (Object)worldKey, (Object)state.level(), (Object)state.essenceProgress(), (Object)this.levelingSystem.getEssenceRequired(state));
                hud.updateLevel(state.level(), state.essenceProgress(), this.levelingSystem.getEssenceRequired(state));
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
            String worldKey = PlayerProgressionManager.getWorldKey((Store<EntityStore>)worldStore);
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
            LevelingSystem.LevelProgress state = this.playerDataRepository.getOrCreateLevel(playerId);
            this.applyPlayerPowerBonuses(playerId, (Ref<EntityStore>)playerRef, worldStore, playerRefComponent, worldKey);
            this.scheduleHudUpdate(currentWorld, playerRefComponent, () -> {
                BossBarHud hud = this.ensureHud(playerComponent, playerRefComponent, worldStore);
                if (hud != null) {
                    this.plugin.getLogger().at(Level.INFO).log("[HUD_DEBUG] updateLevel (ready) player=%s world=%s level=%d essence=%d/%d", (Object)playerId, (Object)worldKey, (Object)state.level(), (Object)state.essenceProgress(), (Object)this.levelingSystem.getEssenceRequired(state));
                    hud.updateLevel(state.level(), state.essenceProgress(), this.levelingSystem.getEssenceRequired(state));
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
            catch (IllegalStateException exception) {
                this.plugin.getLogger().at(Level.FINE).log("Skipping inventory snapshot because player store state changed during world transfer.");
                return;
            }
            if (playerComponent == null) {
                return;
            }
            String inventoryWorldKey = PlayerProgressionManager.getWorldKey((Store<EntityStore>)store);
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
        String worldKey = PlayerProgressionManager.getWorldKey(store);
        this.loadStateIfMissing(playerId, worldKey);
        PowerUpApplicator.PowerState powerState = this.playerDataRepository.getOrCreatePower(playerId);
        int bonus = this.powerUpApplicator.getLuckyEssenceBonus(powerState);
        int effectiveAmount = Math.max(1, amount + bonus);
        LevelingSystem.LevelProgress state = this.playerDataRepository.getOrCreateLevel(playerId);
        int previousLevel = this.levelingSystem.addEssence(state, effectiveAmount);
        if (state.level() != previousLevel) {
            this.plugin.getLogger().at(Level.INFO).log("Level up for %s: %d -> %d (lastStoreLevel=%d, shouldOpenStore=%s)", (Object)playerId, (Object)previousLevel, (Object)state.level(), (Object)state.lastStoreLevel(), (Object)this.levelingSystem.shouldOpenStore(state));
        }
        if ((hud = this.ensureHud(playerComponent, playerRefComponent, store)) != null) {
            this.plugin.getLogger().at(Level.INFO).log("[HUD_DEBUG] updateLevel (essence) player=%s world=%s level=%d essence=%d/%d", (Object)playerId, (Object)worldKey, (Object)state.level(), (Object)state.essenceProgress(), (Object)this.levelingSystem.getEssenceRequired(state));
            hud.updateLevel(state.level(), state.essenceProgress(), this.levelingSystem.getEssenceRequired(state));
        } else {
            this.plugin.getLogger().at(Level.INFO).log("[HUD_DEBUG] updateLevel skipped (essence) player=%s world=%s reason=ensureHud null", (Object)playerId, (Object)worldKey);
        }
        if (this.levelingSystem.shouldOpenStore(state)) {
            Store playerStore;
            this.levelingSystem.markStoreOpened(state);
            this.savePlayerState(playerId, worldKey);
            Ref playerRef = playerRefComponent.getReference();
            if (playerRef != null && playerRef.isValid() && (playerStore = playerRef.getStore()) != null) {
                this.openStoreForPlayer((Ref<EntityStore>)playerRef, (Store<EntityStore>)playerStore, playerComponent, playerRefComponent, state.level());
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
        String worldKey = PlayerProgressionManager.getWorldKey(store);
        this.loadStateIfMissing(playerId, worldKey);
        PowerUpApplicator.PowerState state = this.playerDataRepository.getOrCreatePower(playerId);
        String choiceLabel = this.getPowerChoiceLabel(choice);
        if (choiceLabel == null) {
            return;
        }
        triggerProjectileRain = this.powerUpApplicator.applyChoice(state, choice);
        if (triggerProjectileRain && this.isProjectileRainUsedForActiveGroup(playerId, store)) {
            return;
        }
        VampireShooterComponent shooter = (VampireShooterComponent)store.getComponent(playerRef, HytaleMod.getInstance().getVampireShooterComponentType());
        if (triggerProjectileRain) {
            if (shooter == null) {
                return;
            }
            for (int i = 0; i < this.powerUpApplicator.getProjectileRainBurstsOnPick(); ++i) {
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
        for (int i = 0; i < this.powerUpApplicator.getProjectileRainBurstsOnPick(); ++i) {
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

    private int getPowerChoiceRank(@Nonnull String choice, @Nonnull PowerUpApplicator.PowerState state) {
        return this.powerUpApplicator.getChoiceRank(choice, state);
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
        PowerUpApplicator.PowerState state;
        if (worldKey != null) {
            this.loadStateIfMissing(playerId, worldKey);
        }
        if ((state = this.playerDataRepository.getPower(playerId)) == null) {
            return;
        }
        this.powerUpApplicator.applyToShooter(state, shooter);
    }

    private void resetPowerState(@Nonnull VampireShooterComponent shooter) {
        this.powerUpApplicator.resetShooter(shooter);
    }

    private void applyPlayerPowerBonuses(@Nonnull UUID playerId, @Nonnull Ref<EntityStore> playerRef, @Nonnull Store<EntityStore> store, @Nonnull PlayerRef playerRefComponent, String worldKey) {
        PowerUpApplicator.PowerState state;
        if (worldKey != null) {
            this.loadStateIfMissing(playerId, worldKey);
        }
        if ((state = this.playerDataRepository.getPower(playerId)) == null) {
            return;
        }
        this.powerUpApplicator.applyPlayerBonuses(state, playerRef, store, playerRefComponent);
    }

    public double getPickupRangeBonus(@Nonnull UUID playerId) {
        PowerUpApplicator.PowerState state = this.playerDataRepository.getPower(playerId);
        if (state == null) {
            return 0.0;
        }
        return Math.max(0.0, (double)state.pickupRangeBonus());
    }

    public boolean hasFireRatePower(@Nonnull UUID playerId, @Nonnull Store<EntityStore> store) {
        this.loadStateIfMissing(playerId, PlayerProgressionManager.getWorldKey(store));
        PowerUpApplicator.PowerState state = this.playerDataRepository.getPower(playerId);
        return state != null && this.powerUpApplicator.computeFireRateRank(state.fireRateMultiplier()) >= this.powerUpApplicator.getMaxFireRateUpgrades();
    }

    public int getFireRateRank(@Nonnull UUID playerId, @Nonnull Store<EntityStore> store) {
        this.loadStateIfMissing(playerId, PlayerProgressionManager.getWorldKey(store));
        PowerUpApplicator.PowerState state = this.playerDataRepository.getPower(playerId);
        if (state == null) {
            return 0;
        }
        return this.powerUpApplicator.computeFireRateRank(state.fireRateMultiplier());
    }

    public boolean hasPickupRangePower(@Nonnull UUID playerId, @Nonnull Store<EntityStore> store) {
        this.loadStateIfMissing(playerId, PlayerProgressionManager.getWorldKey(store));
        PowerUpApplicator.PowerState state = this.playerDataRepository.getPower(playerId);
        return state != null && this.powerUpApplicator.computePickupRangeRank(state.pickupRangeBonus()) >= this.powerUpApplicator.getMaxPickupRangeUpgrades();
    }

    public int getPickupRangeRank(@Nonnull UUID playerId, @Nonnull Store<EntityStore> store) {
        this.loadStateIfMissing(playerId, PlayerProgressionManager.getWorldKey(store));
        PowerUpApplicator.PowerState state = this.playerDataRepository.getPower(playerId);
        if (state == null) {
            return 0;
        }
        return this.powerUpApplicator.computePickupRangeRank(state.pickupRangeBonus());
    }

    public boolean hasExtraProjectilePower(@Nonnull UUID playerId, @Nonnull Store<EntityStore> store) {
        this.loadStateIfMissing(playerId, PlayerProgressionManager.getWorldKey(store));
        PowerUpApplicator.PowerState state = this.playerDataRepository.getPower(playerId);
        return state != null && this.powerUpApplicator.clampProjectileCount(state.projectileCount()) >= this.powerUpApplicator.getMaxExtraProjectileUpgrades();
    }

    public int getExtraProjectileRank(@Nonnull UUID playerId, @Nonnull Store<EntityStore> store) {
        this.loadStateIfMissing(playerId, PlayerProgressionManager.getWorldKey(store));
        PowerUpApplicator.PowerState state = this.playerDataRepository.getPower(playerId);
        if (state == null) {
            return 0;
        }
        return this.powerUpApplicator.clampProjectileCount(state.projectileCount());
    }

    public int getBounceRank(@Nonnull UUID playerId, @Nonnull Store<EntityStore> store) {
        this.loadStateIfMissing(playerId, PlayerProgressionManager.getWorldKey(store));
        PowerUpApplicator.PowerState state = this.playerDataRepository.getPower(playerId);
        if (state == null) {
            return 0;
        }
        return this.powerUpApplicator.clampBounceBonus(state.bounceBonus());
    }

    public int getWeaponDamageRank(@Nonnull UUID playerId, @Nonnull Store<EntityStore> store) {
        this.loadStateIfMissing(playerId, PlayerProgressionManager.getWorldKey(store));
        PowerUpApplicator.PowerState state = this.playerDataRepository.getPower(playerId);
        if (state == null) {
            return 0;
        }
        return this.powerUpApplicator.clampWeaponDamageRank(state.weaponDamageRank());
    }

    public float getWeaponDamageBonus(@Nonnull UUID playerId, @Nonnull Store<EntityStore> store) {
        return this.powerUpApplicator.getWeaponDamageBonusForRank(this.getWeaponDamageRank(playerId, store));
    }

    public int getHealthRank(@Nonnull UUID playerId, @Nonnull Store<EntityStore> store) {
        this.loadStateIfMissing(playerId, PlayerProgressionManager.getWorldKey(store));
        PowerUpApplicator.PowerState state = this.playerDataRepository.getPower(playerId);
        if (state == null) {
            return 0;
        }
        return this.powerUpApplicator.clampHealthRank(state.healthRank());
    }

    public int getSpeedRank(@Nonnull UUID playerId, @Nonnull Store<EntityStore> store) {
        this.loadStateIfMissing(playerId, PlayerProgressionManager.getWorldKey(store));
        PowerUpApplicator.PowerState state = this.playerDataRepository.getPower(playerId);
        if (state == null) {
            return 0;
        }
        return this.powerUpApplicator.clampSpeedRank(state.speedRank());
    }

    public int getProjectileRainRank(@Nonnull UUID playerId, @Nonnull Store<EntityStore> store) {
        return this.isProjectileRainUsedForActiveGroup(playerId, store) ? 1 : 0;
    }

    public int getLuckyRank(@Nonnull UUID playerId, @Nonnull Store<EntityStore> store) {
        this.loadStateIfMissing(playerId, PlayerProgressionManager.getWorldKey(store));
        PowerUpApplicator.PowerState state = this.playerDataRepository.getPower(playerId);
        if (state == null) {
            return 0;
        }
        return this.powerUpApplicator.clampLuckyRank(state.luckyRank());
    }

    public int getMaxFireRateUpgrades() {
        return this.powerUpApplicator.getMaxFireRateUpgrades();
    }

    public int getMaxPickupRangeUpgrades() {
        return this.powerUpApplicator.getMaxPickupRangeUpgrades();
    }

    public int getMaxExtraProjectileUpgrades() {
        return this.powerUpApplicator.getMaxExtraProjectileUpgrades();
    }

    public int getMaxBounceUpgrades() {
        return this.powerUpApplicator.getMaxBounceUpgrades();
    }

    public int getMaxWeaponDamageUpgrades() {
        return this.powerUpApplicator.getMaxWeaponDamageUpgrades();
    }

    public int getMaxHealthUpgrades() {
        return this.powerUpApplicator.getMaxHealthUpgrades();
    }

    public int getMaxSpeedUpgrades() {
        return this.powerUpApplicator.getMaxSpeedUpgrades();
    }

    public int getMaxProjectileRainUpgrades() {
        return this.powerUpApplicator.getMaxProjectileRainUpgrades();
    }

    public int getMaxLuckyUpgrades() {
        return this.powerUpApplicator.getMaxLuckyUpgrades();
    }

    private boolean isProjectileRainUsedForActiveGroup(@Nonnull UUID playerId, @Nonnull Store<EntityStore> store) {
        String worldKey = PlayerProgressionManager.getWorldKey(store);
        this.loadStateIfMissing(playerId, worldKey);
        PowerUpApplicator.PowerState selfState = this.playerDataRepository.getOrCreatePower(playerId);
        if (selfState.projectileRainUsed()) {
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
            PowerUpApplicator.PowerState memberState = this.playerDataRepository.getOrCreatePower(memberId);
            if (!memberState.projectileRainUsed()) continue;
            return true;
        }
        return false;
    }

    private void markProjectileRainUsedForActiveGroup(@Nonnull UUID playerId, @Nonnull Store<EntityStore> store, @Nonnull String worldKey) {
        this.loadStateIfMissing(playerId, worldKey);
        PowerUpApplicator.PowerState selfState = this.playerDataRepository.getOrCreatePower(playerId);
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
            PowerUpApplicator.PowerState memberState = this.playerDataRepository.getOrCreatePower(memberId);
            if (!memberState.projectileRainUsed) {
                memberState.projectileRainUsed = true;
                this.savePlayerState(memberId, worldKey);
            }
        }
    }

    private void loadStateIfMissing(@Nonnull UUID playerId, String worldKey) {
        this.playerDataRepository.loadStateIfMissing(playerId, worldKey);
    }

    private void savePlayerState(@Nonnull UUID playerId, @Nonnull String worldKey) {
        this.playerDataRepository.savePlayerState(playerId, worldKey);
    }

    private void resetTransientPlayerState(@Nonnull UUID playerId) {
        this.playerDataRepository.clearTransient(playerId);
        this.lastWasGameplayWorld.remove(playerId);
        this.pendingHudClear.remove(playerId);
    }

    public void resetProgressForPlayers(@Nonnull Iterable<UUID> playerIds, @Nonnull String worldKey) {
        this.playerDataRepository.resetProgressForPlayers(playerIds, worldKey);
        for (UUID playerId : playerIds) {
            if (playerId == null) {
                continue;
            }
            this.cancelAllScheduledTasks(playerId);
            this.resetTransientPlayerState(playerId);
            this.inventoryStateManager.resetPlayer(playerId);
            this.hudUpdateService.cancelHudUpdate(playerId);
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
        return this.hudUpdateService.ensureHud(playerComponent, playerRefComponent, store, this.isGameplayWorld(store));
    }

    private void clearHudForPlayer(@Nonnull Player playerComponent, @Nonnull PlayerRef playerRefComponent, @Nonnull UUID playerId) {
        this.hudUpdateService.clearHudForPlayer(playerComponent, playerRefComponent, playerId);
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
        catch (IllegalThreadStateException exception) {
            this.plugin.getLogger().at(Level.FINE).log("Skipped world task because world is no longer in a valid ticking state.");
        }
    }

    private void scheduleHudUpdate(@Nonnull World world, @Nonnull PlayerRef playerRefComponent, @Nonnull Runnable action) {
        this.hudUpdateService.scheduleHudUpdate(world, playerRefComponent, action);
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
        this.hudUpdateService.cancelHudUpdate(playerId);
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
        this.storeSessionManager.cancelAllScheduledTasks(playerId);
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
        this.inventoryStateManager.switchInventoryForWorld(playerId, worldKey, playerComponent);
    }

    private void saveInventorySnapshot(@Nonnull UUID playerId, String worldKey, @Nonnull Player playerComponent) {
        this.inventoryStateManager.saveInventorySnapshot(playerId, worldKey, playerComponent);
    }

    private void clearInventory(@Nonnull Inventory inventory) {
        this.inventoryStateManager.clearInventory(inventory);
    }

    private void resetCamera(@Nonnull PlayerRef playerRefComponent) {
        this.gamePauseController.resetCamera(playerRefComponent);
    }

    private void forceTopdownCameraDirect(@Nonnull PlayerRef playerRefComponent) {
        this.gamePauseController.forceTopdownCameraDirect(playerRefComponent);
    }

    private void giveJoinWeapon(@Nonnull Player playerComponent) {
        Inventory inventory = playerComponent.getInventory();
        if (inventory == null) {
            return;
        }
        boolean changed = false;
        changed |= this.ensureJoinItem(inventory, this.resolveExistingItemId("battleaxe", JOIN_WEAPON_ITEM_ID), 1, true);
        changed |= this.ensureJoinItem(inventory, this.resolveExistingItemId("shortbow", JOIN_SHORTBOW_ITEM_ID), 1, true);
        changed |= this.ensureJoinItem(inventory, this.resolveExistingItemId("arrows", JOIN_ARROW_ITEM_ID), JOIN_ARROW_QUANTITY, false);
        changed |= this.ensureJoinItem(inventory, this.resolveExistingItemId("food", JOIN_FOOD_ITEM_ID), JOIN_FOOD_QUANTITY, false);
        changed |= this.ensureJoinItem(inventory, this.resolveExistingItemId("helmet", JOIN_ARMOR_HEAD_ITEM_ID), 1, true);
        changed |= this.ensureJoinItem(inventory, this.resolveExistingItemId("chest", JOIN_ARMOR_CHEST_ITEM_ID), 1, true);
        changed |= this.ensureJoinItem(inventory, this.resolveExistingItemId("hands", JOIN_ARMOR_HANDS_ITEM_ID), 1, true);
        changed |= this.ensureJoinItem(inventory, this.resolveExistingItemId("legs", JOIN_ARMOR_LEGS_ITEM_ID), 1, true);
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

    private String resolveExistingItemId(@Nonnull String label, String itemId) {
        if (itemId != null && !itemId.isBlank() && Item.getAssetMap().getAsset(itemId) != null) {
            return itemId;
        }
        this.plugin.getLogger().at(Level.WARNING).log("Join loadout item not found for %s: %s", (Object)label, (Object)itemId);
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
        if (!ConfigManager.get().lifeEssenceItemId.equals(stack.getItemId())) {
            return 0;
        }
        return Math.max(0, stack.getQuantity());
    }

    private void openStoreForPlayer(@Nonnull Ref<EntityStore> playerRef, @Nonnull Store<EntityStore> store, @Nonnull Player playerComponent, @Nonnull PlayerRef playerRefComponent, int level) {
        if (!this.isGameplayWorld(store)) {
            return;
        }
        this.storeSessionManager.openStoreForPlayer(playerRef, store, playerComponent, playerRefComponent, level);
    }

    public void closeStoreForPlayer(@Nonnull Ref<EntityStore> playerRef, @Nonnull Store<EntityStore> store) {
        this.storeSessionManager.closeStoreForPlayer(playerRef, store);
    }

    public boolean canAcceptStoreSelection(@Nonnull UUID playerId) {
        return this.storeSessionManager.canAcceptStoreSelection(playerId);
    }
}
