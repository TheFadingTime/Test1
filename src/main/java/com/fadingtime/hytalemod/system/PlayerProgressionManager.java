/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.builtin.weather.resources.WeatherResource
 *  com.hypixel.hytale.component.Holder
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.server.core.HytaleServer
 *  com.hypixel.hytale.server.core.Message
 *  com.hypixel.hytale.server.core.asset.type.item.config.Item
 *  com.hypixel.hytale.server.core.entity.LivingEntity
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud
 *  com.hypixel.hytale.server.core.event.events.entity.LivingEntityInventoryChangeEvent
 *  com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent
 *  com.hypixel.hytale.server.core.event.events.player.DrainPlayerFromWorldEvent
 *  com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent
 *  com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent
 *  com.hypixel.hytale.server.core.inventory.Inventory
 *  com.hypixel.hytale.server.core.inventory.ItemStack
 *  com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer
 *  com.hypixel.hytale.server.core.inventory.container.ItemContainer
 *  com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction
 *  com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction
 *  com.hypixel.hytale.server.core.inventory.transaction.SlotTransaction
 *  com.hypixel.hytale.server.core.inventory.transaction.Transaction
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.world.World
 *  com.hypixel.hytale.server.core.universe.world.WorldConfig
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 *  javax.annotation.Nonnull
 */
package com.fadingtime.hytalemod.system;

import com.fadingtime.hytalemod.HytaleMod;
import com.fadingtime.hytalemod.component.VampireShooterComponent;
import com.fadingtime.hytalemod.config.ConfigManager;
import com.fadingtime.hytalemod.config.LifeEssenceConfig;
import com.fadingtime.hytalemod.spawner.MobWaveSpawner;
import com.fadingtime.hytalemod.system.BossHudSystem;
import com.fadingtime.hytalemod.system.GamePauseController;
import com.fadingtime.hytalemod.system.HudUpdateService;
import com.fadingtime.hytalemod.system.InventoryStateManager;
import com.fadingtime.hytalemod.system.LevelingSystem;
import com.fadingtime.hytalemod.system.PlayerDataRepository;
import com.fadingtime.hytalemod.system.PowerUpApplicator;
import com.fadingtime.hytalemod.system.StoreSessionManager;
import com.fadingtime.hytalemod.ui.BossBarHud;
import com.hypixel.hytale.builtin.weather.resources.WeatherResource;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.event.events.entity.LivingEntityInventoryChangeEvent;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.DrainPlayerFromWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.SlotTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.Transaction;
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
import java.util.logging.Level;
import javax.annotation.Nonnull;

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

    public PlayerProgressionManager(@Nonnull HytaleMod hytaleMod) {
        this.plugin = hytaleMod;
        LifeEssenceConfig lifeEssenceConfig = LifeEssenceConfig.load(hytaleMod.getFile(), HytaleMod.LOGGER);
        PlayerProgressionManager.applyConfig(lifeEssenceConfig);
        this.levelingSystem = LevelingSystem.fromConfig(lifeEssenceConfig);
        this.powerUpApplicator = PowerUpApplicator.fromConfig(lifeEssenceConfig);
        this.playerDataRepository = new PlayerDataRepository(hytaleMod.getStateStoreManager(), this.levelingSystem, this.powerUpApplicator);
        this.inventoryStateManager = new InventoryStateManager();
        this.gamePauseController = new GamePauseController(lifeEssenceConfig.storeSlowmoFactor);
        this.hudUpdateService = new HudUpdateService(hytaleMod, lifeEssenceConfig.defaultBossName, lifeEssenceConfig.hudReadyDelayMs);
        this.storeSessionManager = new StoreSessionManager(hytaleMod, this.gamePauseController, lifeEssenceConfig.storeInputGraceMs);
        hytaleMod.getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, this::onPlayerAddedToWorld);
        hytaleMod.getEventRegistry().registerGlobal(DrainPlayerFromWorldEvent.class, this::onPlayerDrain);
        hytaleMod.getEventRegistry().registerGlobal(PlayerReadyEvent.class, this::onPlayerReady);
        hytaleMod.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, this::onPlayerDisconnect);
        hytaleMod.getEventRegistry().registerGlobal(LivingEntityInventoryChangeEvent.class, this::onInventoryChange);
    }

    private static synchronized void applyConfig(@Nonnull LifeEssenceConfig lifeEssenceConfig) {
        ESSENCE_PER_LEVEL_BASE = lifeEssenceConfig.essencePerLevelBase;
        ESSENCE_PER_LEVEL_INCREMENT = lifeEssenceConfig.essencePerLevelIncrement;
        ESSENCE_PER_LEVEL_MAX = lifeEssenceConfig.essencePerLevelMax;
        DEFAULT_BOSS_NAME = lifeEssenceConfig.defaultBossName;
        STORE_SLOWMO_FACTOR = lifeEssenceConfig.storeSlowmoFactor;
        STORE_OPEN_LEVEL_START = lifeEssenceConfig.storeOpenLevelStart;
        STORE_OPEN_LEVEL_INTERVAL = lifeEssenceConfig.storeOpenLevelInterval;
        STORE_OPEN_LEVEL_MAX = lifeEssenceConfig.storeOpenLevelMax;
        MAX_FIRE_RATE_UPGRADES = lifeEssenceConfig.maxFireRateUpgrades;
        FIRE_RATE_MULTIPLIER_PER_UPGRADE = lifeEssenceConfig.fireRateMultiplierPerUpgrade;
        MAX_FIRE_RATE_MULTIPLIER = (float)Math.pow(FIRE_RATE_MULTIPLIER_PER_UPGRADE, MAX_FIRE_RATE_UPGRADES);
        MAX_PICKUP_RANGE_UPGRADES = lifeEssenceConfig.maxPickupRangeUpgrades;
        PICKUP_RANGE_PER_UPGRADE = lifeEssenceConfig.pickupRangePerUpgrade;
        MAX_PICKUP_RANGE_BONUS = lifeEssenceConfig.maxPickupRangeBonus;
        MAX_EXTRA_PROJECTILES = lifeEssenceConfig.maxExtraProjectiles;
        MAX_BOUNCE_UPGRADES = lifeEssenceConfig.maxBounceUpgrades;
        MAX_WEAPON_DAMAGE_UPGRADES = lifeEssenceConfig.maxWeaponDamageUpgrades;
        WEAPON_DAMAGE_BONUS_PER_UPGRADE = lifeEssenceConfig.weaponDamageBonusPerUpgrade;
        MAX_HEALTH_UPGRADES = lifeEssenceConfig.maxHealthUpgrades;
        HEALTH_BONUS_PER_UPGRADE = lifeEssenceConfig.healthBonusPerUpgrade;
        MAX_SPEED_UPGRADES = lifeEssenceConfig.maxSpeedUpgrades;
        MAX_PROJECTILE_RAIN_UPGRADES = lifeEssenceConfig.maxProjectileRainUpgrades;
        MAX_LUCKY_UPGRADES = lifeEssenceConfig.maxLuckyUpgrades;
        PROJECTILE_RAIN_BURSTS_ON_PICK = lifeEssenceConfig.projectileRainBurstsOnPick;
        SPEED_BONUS_PER_UPGRADE = lifeEssenceConfig.speedBonusPerUpgrade;
        MAX_SPEED_MULTIPLIER = lifeEssenceConfig.maxSpeedMultiplier;
        PLAYER_BASE_SPEED = lifeEssenceConfig.playerBaseSpeed;
        STAMINA_BONUS_PER_SPEED_UPGRADE = lifeEssenceConfig.staminaBonusPerSpeedUpgrade;
        LUCKY_ESSENCE_BONUS_PER_RANK = lifeEssenceConfig.luckyEssenceBonusPerRank;
        HUD_READY_DELAY_MS = lifeEssenceConfig.hudReadyDelayMs;
        STORE_INPUT_GRACE_MS = lifeEssenceConfig.storeInputGraceMs;
        JOIN_WEAPON_ITEM_ID = lifeEssenceConfig.joinWeaponItemId;
        JOIN_SHORTBOW_ITEM_ID = lifeEssenceConfig.joinShortbowItemId;
        JOIN_ARROW_ITEM_ID = lifeEssenceConfig.joinArrowItemId;
        JOIN_FOOD_ITEM_ID = lifeEssenceConfig.joinFoodItemId;
        JOIN_ARMOR_HEAD_ITEM_ID = lifeEssenceConfig.joinArmorHeadItemId;
        JOIN_ARMOR_CHEST_ITEM_ID = lifeEssenceConfig.joinArmorChestItemId;
        JOIN_ARMOR_HANDS_ITEM_ID = lifeEssenceConfig.joinArmorHandsItemId;
        JOIN_ARMOR_LEGS_ITEM_ID = lifeEssenceConfig.joinArmorLegsItemId;
        JOIN_ARROW_QUANTITY = lifeEssenceConfig.joinArrowQuantity;
        JOIN_FOOD_QUANTITY = lifeEssenceConfig.joinFoodQuantity;
        JOIN_DEFAULT_DURABILITY = lifeEssenceConfig.joinDefaultDurability;
        JOIN_FORCED_WEATHER_ID = lifeEssenceConfig.joinForcedWeatherId;
    }

    private void onPlayerAddedToWorld(@Nonnull AddPlayerToWorldEvent addPlayerToWorldEvent) {
        Holder holder = addPlayerToWorldEvent.getHolder();
        if (holder == null) {
            return;
        }
        PlayerRef playerRef = (PlayerRef)holder.getComponent(PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }
        Ref ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return;
        }
        Store store = ref.getStore();
        if (store == null) {
            return;
        }
        World world = addPlayerToWorldEvent.getWorld();
        if (world == null) {
            return;
        }
        boolean bl = true;
        Player player = (Player)store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        UUID uUID = playerRef.getUuid();
        String string = PlayerProgressionManager.getWorldKey((Store<EntityStore>)store);
        if (!bl) {
            BossHudSystem bossHudSystem;
            VampireShooterComponent vampireShooterComponent = (VampireShooterComponent)store.getComponent(ref, HytaleMod.getInstance().getVampireShooterComponentType());
            if (vampireShooterComponent != null) {
                this.resetPowerState(vampireShooterComponent);
            }
            if ((bossHudSystem = HytaleMod.getInstance().getBossHudSystem()) != null) {
                bossHudSystem.setHudSuppressed(uUID, true);
            }
            this.scheduleHudUpdate(world, playerRef, () -> {
                if (!ref.isValid()) {
                    return;
                }
                Player player2 = (Player)store.getComponent(ref, Player.getComponentType());
                if (player2 == null) {
                    return;
                }
                this.clearHudForPlayer(player2, playerRef, uUID);
            });
            return;
        }
        BossHudSystem bossHudSystem = HytaleMod.getInstance().getBossHudSystem();
        if (bossHudSystem != null) {
            bossHudSystem.setHudSuppressed(uUID, false);
        }
        this.pendingHudClear.remove(uUID);
        this.scheduleHudUpdate(world, playerRef, () -> {
            if (!this.isGameplayWorld((Store<EntityStore>)store)) {
                return;
            }
            this.loadStateIfMissing(uUID, string);
            LevelingSystem.LevelProgress levelProgress = this.playerDataRepository.getOrCreateLevel(uUID);
            BossBarHud bossBarHud = this.ensureHud(player, playerRef, (Store<EntityStore>)store);
            if (bossBarHud != null) {
                bossBarHud.setLevelHudVisible(true);
                bossBarHud.updateLevel(levelProgress.level(), levelProgress.essenceProgress(), this.levelingSystem.getEssenceRequired(levelProgress));
            } else {
            }
        });
    }

    private void onPlayerDrain(@Nonnull DrainPlayerFromWorldEvent drainPlayerFromWorldEvent) {
        Holder holder = drainPlayerFromWorldEvent.getHolder();
        if (holder == null) {
            return;
        }
        PlayerRef playerRef = (PlayerRef)holder.getComponent(PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }
        Ref ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return;
        }
        Store store = ref.getStore();
        if (store == null) {
            return;
        }
        World world = drainPlayerFromWorldEvent.getWorld();
        if (world == null) {
            return;
        }
        UUID uUID = playerRef.getUuid();
        this.pendingHudClear.put(uUID, true);
        this.cancelAllScheduledTasks(uUID);
        BossHudSystem bossHudSystem = HytaleMod.getInstance().getBossHudSystem();
        if (bossHudSystem != null) {
            bossHudSystem.setHudSuppressed(uUID, true);
        }
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
        if (world == null) {
            return;
        }
        this.executeIfTicking(world, () -> {
            Object object;
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
            Player player = (Player)store2.getComponent(ref, Player.getComponentType());
            if (player == null) {
                return;
            }
            UUID uUID = playerRef.getUuid();
            this.disconnectHandledAt.remove(uUID);
            String string = PlayerProgressionManager.getWorldKey((Store<EntityStore>)store2);
            boolean bl = this.isGameplayWorld((Store<EntityStore>)store2);
            World world2 = ((EntityStore)store2.getExternalData()).getWorld();
            if (world2 == null) {
                return;
            }
            BossHudSystem bossHudSystem = HytaleMod.getInstance().getBossHudSystem();
            if (bossHudSystem != null) {
                bossHudSystem.setHudSuppressed(uUID, !bl);
            }
            this.switchInventoryForWorld(uUID, string, player);
            if (!bl) {
                this.scheduleHudUpdate(world2, playerRef, () -> {
                    VampireShooterComponent vampireShooterComponent;
                    boolean bl2 = Boolean.TRUE.equals(this.lastWasGameplayWorld.remove(uUID));
                    boolean bl3 = Boolean.TRUE.equals(this.pendingHudClear.remove(uUID)) || bl2;
                    if (bl2) {
                        this.resetCamera(playerRef);
                    }
                    if ((vampireShooterComponent = (VampireShooterComponent)store2.getComponent(ref, HytaleMod.getInstance().getVampireShooterComponentType())) != null) {
                        this.resetPowerState(vampireShooterComponent);
                    }
                    CustomUIHud customUIHud = player.getHudManager().getCustomHud();
                    if (bl3 || customUIHud instanceof BossBarHud) {
                        this.clearHudForPlayer(player, playerRef, uUID);
                    }
                });
                return;
            }
            if (!Boolean.TRUE.equals(this.lastWasGameplayWorld.put(uUID, true))) {
                this.forceTopdownCameraDirect(playerRef);
                object = player.getInventory();
                if (object != null) {
                    this.clearInventory((Inventory)object);
                    this.saveInventorySnapshot(uUID, string, player);
                }
            }
            this.loadStateIfMissing(uUID, string);
            object = this.playerDataRepository.getOrCreateLevel(uUID);
            final LevelingSystem.LevelProgress levelProgress = (LevelingSystem.LevelProgress)object;
            this.applyPlayerPowerBonuses(uUID, (Ref<EntityStore>)ref, (Store<EntityStore>)store2, playerRef, string);
            this.scheduleHudUpdate(world2, playerRef, () -> this.updateHudOnPlayerReady(player, playerRef, store2, uUID, string, levelProgress, ref));
            this.schedulePlayerWorldTask(world2, playerRef, () -> this.applyPlayerPowerBonuses(uUID, (Ref<EntityStore>)ref, (Store<EntityStore>)store2, playerRef, string), 300L);
            this.schedulePlayerWorldTask(world2, playerRef, () -> this.applyPlayerPowerBonuses(uUID, (Ref<EntityStore>)ref, (Store<EntityStore>)store2, playerRef, string), 1200L);
            this.applyJoinDefaults((Ref<EntityStore>)ref, (Store<EntityStore>)store2, world2);
        });
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
        this.cancelAllScheduledTasks(uUID);
        this.resetTransientPlayerState(uUID);
        Ref ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return;
        }
        Store store = ref.getStore();
        if (store == null) {
            return;
        }
        World world = ((EntityStore)store.getExternalData()).getWorld();
        if (world == null) {
            return;
        }
        this.executeIfTicking(world, () -> {
            Player player;
            if (!ref.isValid()) {
                return;
            }
            try {
                player = (Player)store.getComponent(ref, Player.getComponentType());
            }
            catch (IllegalStateException illegalStateException) {
                return;
            }
            if (player == null) {
                return;
            }
            String string = PlayerProgressionManager.getWorldKey((Store<EntityStore>)store);
            this.saveInventorySnapshot(uUID, string, player);
        });
    }

    private void onInventoryChange(@Nonnull LivingEntityInventoryChangeEvent livingEntityInventoryChangeEvent) {
        LivingEntity livingEntity = (LivingEntity)livingEntityInventoryChangeEvent.getEntity();
        if (!(livingEntity instanceof Player)) {
            return;
        }
        Ref ref = livingEntity.getReference();
        if (ref == null || !ref.isValid()) {
            return;
        }
        Store store = ref.getStore();
        if (store == null) {
            return;
        }
        if (!this.isGameplayWorld((Store<EntityStore>)store)) {
            return;
        }
        PlayerRef playerRef = (PlayerRef)store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }
        Player player = (Player)store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        int n = this.getLifeEssenceDelta(livingEntityInventoryChangeEvent.getTransaction());
        if (n <= 0) {
            return;
        }
        this.grantEssence(playerRef, player, (Store<EntityStore>)store, n);
    }

    public void grantEssence(@Nonnull PlayerRef playerRef, @Nonnull Player player, @Nonnull Store<EntityStore> store, int n) {
        BossBarHud bossBarHud;
        if (n <= 0) {
            return;
        }
        if (!this.isGameplayWorld(store)) {
            return;
        }
        UUID uUID = playerRef.getUuid();
        String string = PlayerProgressionManager.getWorldKey(store);
        this.loadStateIfMissing(uUID, string);
        PowerUpApplicator.PowerState powerState = this.playerDataRepository.getOrCreatePower(uUID);
        int n2 = this.powerUpApplicator.getLuckyEssenceBonus(powerState);
        int n3 = Math.max(1, n + n2);
        LevelingSystem.LevelProgress levelProgress = this.playerDataRepository.getOrCreateLevel(uUID);
        int n4 = this.levelingSystem.addEssence(levelProgress, n3);
        if (levelProgress.level() != n4) {
            this.plugin.getLogger().at(Level.INFO).log("Level up for %s: %d -> %d (lastStoreLevel=%d, shouldOpenStore=%s)", (Object)uUID, (Object)n4, (Object)levelProgress.level(), (Object)levelProgress.lastStoreLevel(), (Object)this.levelingSystem.shouldOpenStore(levelProgress));
        }
        if ((bossBarHud = this.ensureHud(player, playerRef, store)) != null) {
            bossBarHud.updateLevel(levelProgress.level(), levelProgress.essenceProgress(), this.levelingSystem.getEssenceRequired(levelProgress));
        } else {
        }
        if (this.levelingSystem.shouldOpenStore(levelProgress)) {
            Store store2;
            this.levelingSystem.markStoreOpened(levelProgress);
            this.savePlayerState(uUID, string);
            Ref ref = playerRef.getReference();
            if (ref != null && ref.isValid() && (store2 = ref.getStore()) != null) {
                this.openStoreForPlayer((Ref<EntityStore>)ref, (Store<EntityStore>)store2, player, playerRef, levelProgress.level());
            }
        } else {
            this.savePlayerState(uUID, string);
        }
    }

    public void applyPowerUp(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull String string) {
        int n;
        if (!this.isGameplayWorld(store)) {
            return;
        }
        PlayerRef playerRef = (PlayerRef)store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }
        UUID uUID = playerRef.getUuid();
        String string2 = PlayerProgressionManager.getWorldKey(store);
        this.loadStateIfMissing(uUID, string2);
        PowerUpApplicator.PowerState powerState = this.playerDataRepository.getOrCreatePower(uUID);
        String string3 = this.getPowerChoiceLabel(string);
        if (string3 == null) {
            return;
        }
        boolean bl = this.powerUpApplicator.applyChoice(powerState, string);
        if (bl && this.isProjectileRainUsedForActiveGroup(uUID, store)) {
            return;
        }
        VampireShooterComponent vampireShooterComponent = (VampireShooterComponent)store.getComponent(ref, HytaleMod.getInstance().getVampireShooterComponentType());
        if (bl) {
            if (vampireShooterComponent == null) {
                return;
            }
            for (n = 0; n < this.powerUpApplicator.getProjectileRainBurstsOnPick(); ++n) {
                vampireShooterComponent.queueProjectileRain();
            }
            this.markProjectileRainUsedForActiveGroup(uUID, store, string2);
        }
        if (vampireShooterComponent != null) {
            this.restorePowerState(uUID, vampireShooterComponent, string2);
        }
        this.applyPlayerPowerBonuses(uUID, ref, store, playerRef, string2);
        this.savePlayerState(uUID, string2);
        n = this.getPowerChoiceRank(string, powerState);
        this.announcePowerChoice(ref, store, uUID, string3, n);
    }

    public boolean triggerProjectileRainNow(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        if (!this.isGameplayWorld(store) || ref == null || !ref.isValid()) {
            return false;
        }
        VampireShooterComponent vampireShooterComponent = (VampireShooterComponent)store.getComponent(ref, HytaleMod.getInstance().getVampireShooterComponentType());
        if (vampireShooterComponent == null) {
            return false;
        }
        for (int i = 0; i < this.powerUpApplicator.getProjectileRainBurstsOnPick(); ++i) {
            vampireShooterComponent.queueProjectileRain();
        }
        return true;
    }

    private void announcePowerChoice(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull UUID uUID, @Nonnull String string, int n) {
        UUID uUID2;
        List<Ref<EntityStore>> list;
        Player player = (Player)store.getComponent(ref, Player.getComponentType());
        String string2 = player != null && player.getDisplayName() != null && !player.getDisplayName().isBlank() ? player.getDisplayName().trim() : uUID.toString();
        string2 = string2.toUpperCase(Locale.ROOT);
        Message message = Message.join((Message[])new Message[]{Message.raw((String)(string2 + " ")), Message.raw((String)"Selected: ").color("#f0c766").bold(true), Message.raw((String)(string + " ")).color("#f0c766").bold(true), Message.raw((String)("- Rank " + n)).color("#4f8f5f").bold(true)});
        MobWaveSpawner mobWaveSpawner = HytaleMod.getInstance().getMobWaveSpawner();
        if (mobWaveSpawner != null && !(list = mobWaveSpawner.getPlayerRefsForOwner(uUID2 = mobWaveSpawner.resolveOwnerId(uUID))).isEmpty()) {
            for (Ref<EntityStore> ref2 : list) {
                Player player2;
                Store store2;
                if (ref2 == null || !ref2.isValid() || (store2 = ref2.getStore()) == null || (player2 = (Player)store2.getComponent(ref2, Player.getComponentType())) == null) continue;
                player2.sendMessage(message);
            }
            return;
        }
        if (player != null) {
            player.sendMessage(message);
        }
    }

    private int getPowerChoiceRank(@Nonnull String string, @Nonnull PowerUpApplicator.PowerState powerState) {
        return this.powerUpApplicator.getChoiceRank(string, powerState);
    }

    private String getPowerChoiceLabel(@Nonnull String string) {
        if ("extra_projectile".equals(string)) {
            return "EXTRA PROJECTILE";
        }
        if ("fire_rate".equals(string)) {
            return "FIRE RATE";
        }
        if ("pickup_range".equals(string)) {
            return "PICKUP RANGE";
        }
        if ("bounce".equals(string)) {
            return "BOUNCE";
        }
        if ("weapon_damage".equals(string)) {
            return "WEAPON DAMAGE";
        }
        if ("max_health".equals(string)) {
            return "MORE HEALTH";
        }
        if ("move_speed".equals(string)) {
            return "MOVE SPEED";
        }
        if ("lucky".equals(string)) {
            return "LUCKY";
        }
        if ("skip_store".equals(string)) {
            return "SKIP";
        }
        if ("projectile_rain".equals(string)) {
            return "LAST RESORT...";
        }
        return null;
    }

    public void restorePowerState(@Nonnull UUID uUID, @Nonnull VampireShooterComponent vampireShooterComponent) {
        this.restorePowerState(uUID, vampireShooterComponent, null);
    }

    private void restorePowerState(@Nonnull UUID uUID, @Nonnull VampireShooterComponent vampireShooterComponent, String string) {
        PowerUpApplicator.PowerState powerState;
        if (string != null) {
            this.loadStateIfMissing(uUID, string);
        }
        if ((powerState = this.playerDataRepository.getPower(uUID)) == null) {
            return;
        }
        this.powerUpApplicator.applyToShooter(powerState, vampireShooterComponent);
    }

    private void resetPowerState(@Nonnull VampireShooterComponent vampireShooterComponent) {
        this.powerUpApplicator.resetShooter(vampireShooterComponent);
    }

    private void applyPlayerPowerBonuses(@Nonnull UUID uUID, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PlayerRef playerRef, String string) {
        PowerUpApplicator.PowerState powerState;
        if (string != null) {
            this.loadStateIfMissing(uUID, string);
        }
        if ((powerState = this.playerDataRepository.getPower(uUID)) == null) {
            return;
        }
        this.powerUpApplicator.applyPlayerBonuses(powerState, ref, store, playerRef);
    }

    public double getPickupRangeBonus(@Nonnull UUID uUID) {
        PowerUpApplicator.PowerState powerState = this.playerDataRepository.getPower(uUID);
        if (powerState == null) {
            return 0.0;
        }
        return Math.max(0.0, (double)powerState.pickupRangeBonus());
    }

    public boolean hasFireRatePower(@Nonnull UUID uUID, @Nonnull Store<EntityStore> store) {
        this.loadStateIfMissing(uUID, PlayerProgressionManager.getWorldKey(store));
        PowerUpApplicator.PowerState powerState = this.playerDataRepository.getPower(uUID);
        return powerState != null && this.powerUpApplicator.computeFireRateRank(powerState.fireRateMultiplier()) >= this.powerUpApplicator.getMaxFireRateUpgrades();
    }

    public int getFireRateRank(@Nonnull UUID uUID, @Nonnull Store<EntityStore> store) {
        this.loadStateIfMissing(uUID, PlayerProgressionManager.getWorldKey(store));
        PowerUpApplicator.PowerState powerState = this.playerDataRepository.getPower(uUID);
        if (powerState == null) {
            return 0;
        }
        return this.powerUpApplicator.computeFireRateRank(powerState.fireRateMultiplier());
    }

    public boolean hasPickupRangePower(@Nonnull UUID uUID, @Nonnull Store<EntityStore> store) {
        this.loadStateIfMissing(uUID, PlayerProgressionManager.getWorldKey(store));
        PowerUpApplicator.PowerState powerState = this.playerDataRepository.getPower(uUID);
        return powerState != null && this.powerUpApplicator.computePickupRangeRank(powerState.pickupRangeBonus()) >= this.powerUpApplicator.getMaxPickupRangeUpgrades();
    }

    public int getPickupRangeRank(@Nonnull UUID uUID, @Nonnull Store<EntityStore> store) {
        this.loadStateIfMissing(uUID, PlayerProgressionManager.getWorldKey(store));
        PowerUpApplicator.PowerState powerState = this.playerDataRepository.getPower(uUID);
        if (powerState == null) {
            return 0;
        }
        return this.powerUpApplicator.computePickupRangeRank(powerState.pickupRangeBonus());
    }

    public boolean hasExtraProjectilePower(@Nonnull UUID uUID, @Nonnull Store<EntityStore> store) {
        this.loadStateIfMissing(uUID, PlayerProgressionManager.getWorldKey(store));
        PowerUpApplicator.PowerState powerState = this.playerDataRepository.getPower(uUID);
        return powerState != null && this.powerUpApplicator.clampProjectileCount(powerState.projectileCount()) >= this.powerUpApplicator.getMaxExtraProjectileUpgrades();
    }

    public int getExtraProjectileRank(@Nonnull UUID uUID, @Nonnull Store<EntityStore> store) {
        this.loadStateIfMissing(uUID, PlayerProgressionManager.getWorldKey(store));
        PowerUpApplicator.PowerState powerState = this.playerDataRepository.getPower(uUID);
        if (powerState == null) {
            return 0;
        }
        return this.powerUpApplicator.clampProjectileCount(powerState.projectileCount());
    }

    public int getBounceRank(@Nonnull UUID uUID, @Nonnull Store<EntityStore> store) {
        this.loadStateIfMissing(uUID, PlayerProgressionManager.getWorldKey(store));
        PowerUpApplicator.PowerState powerState = this.playerDataRepository.getPower(uUID);
        if (powerState == null) {
            return 0;
        }
        return this.powerUpApplicator.clampBounceBonus(powerState.bounceBonus());
    }

    public int getWeaponDamageRank(@Nonnull UUID uUID, @Nonnull Store<EntityStore> store) {
        this.loadStateIfMissing(uUID, PlayerProgressionManager.getWorldKey(store));
        PowerUpApplicator.PowerState powerState = this.playerDataRepository.getPower(uUID);
        if (powerState == null) {
            return 0;
        }
        return this.powerUpApplicator.clampWeaponDamageRank(powerState.weaponDamageRank());
    }

    public float getWeaponDamageBonus(@Nonnull UUID uUID, @Nonnull Store<EntityStore> store) {
        return this.powerUpApplicator.getWeaponDamageBonusForRank(this.getWeaponDamageRank(uUID, store));
    }

    public int getHealthRank(@Nonnull UUID uUID, @Nonnull Store<EntityStore> store) {
        this.loadStateIfMissing(uUID, PlayerProgressionManager.getWorldKey(store));
        PowerUpApplicator.PowerState powerState = this.playerDataRepository.getPower(uUID);
        if (powerState == null) {
            return 0;
        }
        return this.powerUpApplicator.clampHealthRank(powerState.healthRank());
    }

    public int getSpeedRank(@Nonnull UUID uUID, @Nonnull Store<EntityStore> store) {
        this.loadStateIfMissing(uUID, PlayerProgressionManager.getWorldKey(store));
        PowerUpApplicator.PowerState powerState = this.playerDataRepository.getPower(uUID);
        if (powerState == null) {
            return 0;
        }
        return this.powerUpApplicator.clampSpeedRank(powerState.speedRank());
    }

    public int getProjectileRainRank(@Nonnull UUID uUID, @Nonnull Store<EntityStore> store) {
        return this.isProjectileRainUsedForActiveGroup(uUID, store) ? 1 : 0;
    }

    public int getLuckyRank(@Nonnull UUID uUID, @Nonnull Store<EntityStore> store) {
        this.loadStateIfMissing(uUID, PlayerProgressionManager.getWorldKey(store));
        PowerUpApplicator.PowerState powerState = this.playerDataRepository.getPower(uUID);
        if (powerState == null) {
            return 0;
        }
        return this.powerUpApplicator.clampLuckyRank(powerState.luckyRank());
    }

    public int getSkipRank(@Nonnull UUID uUID, @Nonnull Store<EntityStore> store) {
        this.loadStateIfMissing(uUID, PlayerProgressionManager.getWorldKey(store));
        PowerUpApplicator.PowerState powerState = this.playerDataRepository.getPower(uUID);
        if (powerState == null) {
            return 0;
        }
        return Math.max(0, powerState.skipRank());
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

    private boolean isProjectileRainUsedForActiveGroup(@Nonnull UUID uUID, @Nonnull Store<EntityStore> store) {
        String string = PlayerProgressionManager.getWorldKey(store);
        this.loadStateIfMissing(uUID, string);
        PowerUpApplicator.PowerState powerState = this.playerDataRepository.getOrCreatePower(uUID);
        if (powerState.projectileRainUsed()) {
            return true;
        }
        MobWaveSpawner mobWaveSpawner = HytaleMod.getInstance().getMobWaveSpawner();
        if (mobWaveSpawner == null) {
            return false;
        }
        UUID uUID2 = mobWaveSpawner.resolveOwnerId(uUID);
        List<Ref<EntityStore>> list = mobWaveSpawner.getPlayerRefsForOwner(uUID2);
        for (Ref<EntityStore> ref : list) {
            PlayerRef playerRef;
            Store store2;
            if (ref == null || !ref.isValid() || (store2 = ref.getStore()) == null || (playerRef = (PlayerRef)store2.getComponent(ref, PlayerRef.getComponentType())) == null) continue;
            UUID uUID3 = playerRef.getUuid();
            this.loadStateIfMissing(uUID3, string);
            PowerUpApplicator.PowerState powerState2 = this.playerDataRepository.getOrCreatePower(uUID3);
            if (!powerState2.projectileRainUsed()) continue;
            return true;
        }
        return false;
    }

    private void markProjectileRainUsedForActiveGroup(@Nonnull UUID uUID, @Nonnull Store<EntityStore> store, @Nonnull String string) {
        this.loadStateIfMissing(uUID, string);
        PowerUpApplicator.PowerState powerState = this.playerDataRepository.getOrCreatePower(uUID);
        if (!powerState.projectileRainUsed) {
            powerState.projectileRainUsed = true;
        }
        this.savePlayerState(uUID, string);
        MobWaveSpawner mobWaveSpawner = HytaleMod.getInstance().getMobWaveSpawner();
        if (mobWaveSpawner == null) {
            return;
        }
        UUID uUID2 = mobWaveSpawner.resolveOwnerId(uUID);
        List<Ref<EntityStore>> list = mobWaveSpawner.getPlayerRefsForOwner(uUID2);
        for (Ref<EntityStore> ref : list) {
            UUID uUID3;
            PlayerRef playerRef;
            Store store2;
            if (ref == null || !ref.isValid() || (store2 = ref.getStore()) == null || (playerRef = (PlayerRef)store2.getComponent(ref, PlayerRef.getComponentType())) == null || (uUID3 = playerRef.getUuid()).equals(uUID)) continue;
            this.loadStateIfMissing(uUID3, string);
            PowerUpApplicator.PowerState powerState2 = this.playerDataRepository.getOrCreatePower(uUID3);
            if (powerState2.projectileRainUsed) continue;
            powerState2.projectileRainUsed = true;
            this.savePlayerState(uUID3, string);
        }
    }

    private void loadStateIfMissing(@Nonnull UUID uUID, String string) {
        this.playerDataRepository.loadStateIfMissing(uUID, string);
    }

    private void savePlayerState(@Nonnull UUID uUID, @Nonnull String string) {
        this.playerDataRepository.savePlayerState(uUID, string);
    }

    private void resetTransientPlayerState(@Nonnull UUID uUID) {
        this.playerDataRepository.clearTransient(uUID);
        this.lastWasGameplayWorld.remove(uUID);
        this.pendingHudClear.remove(uUID);
    }

    public void resetProgressForPlayers(@Nonnull Iterable<UUID> iterable, @Nonnull String string) {
        this.playerDataRepository.resetProgressForPlayers(iterable, string);
        for (UUID uUID : iterable) {
            if (uUID == null) continue;
            this.cancelAllScheduledTasks(uUID);
            this.resetTransientPlayerState(uUID);
            this.inventoryStateManager.resetPlayer(uUID);
            this.hudUpdateService.cancelHudUpdate(uUID);
        }
    }

    private boolean isGameplayWorld(@Nonnull Store<EntityStore> store) {
        return true;
    }

    private static String getWorldKey(@Nonnull Store<EntityStore> store) {
        World world = ((EntityStore)store.getExternalData()).getWorld();
        if (world == null) {
            return "default";
        }
        if (world.getWorldConfig() != null && world.getWorldConfig().getUuid() != null) {
            return "world-" + world.getWorldConfig().getUuid().toString();
        }
        String string = world.getWorldConfig() != null ? world.getWorldConfig().getDisplayName() : null;
        String string2 = string;
        if (string != null && !string.isBlank()) {
            return string.trim();
        }
        String string3 = world.getName();
        if (string3 != null && !string3.isBlank()) {
            return string3.trim();
        }
        return "default";
    }

    private BossBarHud ensureHud(@Nonnull Player player, @Nonnull PlayerRef playerRef, @Nonnull Store<EntityStore> store) {
        return this.hudUpdateService.ensureHud(player, playerRef, store, this.isGameplayWorld(store));
    }

    private void clearHudForPlayer(@Nonnull Player player, @Nonnull PlayerRef playerRef, @Nonnull UUID uUID) {
        this.hudUpdateService.clearHudForPlayer(player, playerRef, uUID);
    }

    private void applyJoinDefaults(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull World world) {
        if (!ref.isValid()) {
            return;
        }
        Player player = (Player)store.getComponent(ref, Player.getComponentType());
        if (player != null && this.isGameplayWorld(store)) {
            this.giveJoinWeapon(player);
        }
        this.forceWeather(world, store);
    }

    private void executeIfTicking(@Nonnull World world, @Nonnull Runnable runnable) {
        if (!world.isTicking()) {
            return;
        }
        try {
            world.execute(runnable);
        }
        catch (IllegalThreadStateException illegalThreadStateException) {
        }
    }

    private void scheduleHudUpdate(@Nonnull World world, @Nonnull PlayerRef playerRef, @Nonnull Runnable runnable) {
        this.hudUpdateService.scheduleHudUpdate(world, playerRef, runnable);
    }

    private ScheduledFuture<?> schedulePlayerWorldTask(@Nonnull World world, @Nonnull PlayerRef playerRef, @Nonnull Runnable runnable, long l) {
        ScheduledFuture<?>[] scheduledFutureArray = new ScheduledFuture[1];
        UUID uUID = playerRef.getUuid();
        ScheduledFuture<?> scheduledFuture = HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
            ScheduledFuture<?> trackedFuture = scheduledFutureArray[0];
            if (trackedFuture != null) {
                this.untrackPlayerTask(uUID, trackedFuture);
            }
            if (!this.isPlayerInWorld(playerRef, world)) {
                return;
            }
            this.executeIfTicking(world, runnable);
        }, l, TimeUnit.MILLISECONDS);
        scheduledFutureArray[0] = scheduledFuture;
        this.trackPlayerTask(uUID, scheduledFuture);
        return scheduledFuture;
    }

    private void cancelHudUpdate(@Nonnull UUID uUID) {
        this.hudUpdateService.cancelHudUpdate(uUID);
    }

    private void trackPlayerTask(@Nonnull UUID uUID2, @Nonnull ScheduledFuture<?> scheduledFuture) {
        this.delayedTasksByPlayer.computeIfAbsent(uUID2, uUID -> new ConcurrentLinkedQueue()).add(scheduledFuture);
    }

    private void untrackPlayerTask(@Nonnull UUID uUID, @Nonnull ScheduledFuture<?> scheduledFuture) {
        ConcurrentLinkedQueue<ScheduledFuture<?>> concurrentLinkedQueue = this.delayedTasksByPlayer.get(uUID);
        if (concurrentLinkedQueue == null) {
            return;
        }
        concurrentLinkedQueue.remove(scheduledFuture);
        if (concurrentLinkedQueue.isEmpty()) {
            this.delayedTasksByPlayer.remove(uUID, concurrentLinkedQueue);
        }
    }

    private void cancelPlayerTasks(@Nonnull UUID uUID) {
        ConcurrentLinkedQueue<ScheduledFuture<?>> concurrentLinkedQueue = this.delayedTasksByPlayer.remove(uUID);
        if (concurrentLinkedQueue == null) {
            return;
        }
        for (ScheduledFuture<?> scheduledFuture : concurrentLinkedQueue) {
            if (scheduledFuture == null) continue;
            scheduledFuture.cancel(false);
        }
    }

    private void cancelAllScheduledTasks(@Nonnull UUID uUID) {
        this.cancelHudUpdate(uUID);
        this.cancelPlayerTasks(uUID);
        this.storeSessionManager.cancelAllScheduledTasks(uUID);
    }

    private boolean isPlayerInWorld(@Nonnull PlayerRef playerRef, @Nonnull World world) {
        Ref ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return false;
        }
        Store store = ref.getStore();
        if (store == null) {
            return false;
        }
        World world2 = ((EntityStore)store.getExternalData()).getWorld();
        if (world2 == null) {
            return false;
        }
        WorldConfig worldConfig = world2.getWorldConfig();
        WorldConfig worldConfig2 = world.getWorldConfig();
        if (worldConfig != null && worldConfig2 != null && worldConfig.getUuid() != null && worldConfig2.getUuid() != null) {
            return worldConfig.getUuid().equals(worldConfig2.getUuid());
        }
        String string = world2.getName();
        String string2 = world.getName();
        if (string != null && string2 != null) {
            return string.equals(string2);
        }
        return world2 == world;
    }

    private void switchInventoryForWorld(@Nonnull UUID uUID, String string, @Nonnull Player player) {
        this.inventoryStateManager.switchInventoryForWorld(uUID, string, player);
    }

    private void saveInventorySnapshot(@Nonnull UUID uUID, String string, @Nonnull Player player) {
        this.inventoryStateManager.saveInventorySnapshot(uUID, string, player);
    }

    private void clearInventory(@Nonnull Inventory inventory) {
        this.inventoryStateManager.clearInventory(inventory);
    }

    private void resetCamera(@Nonnull PlayerRef playerRef) {
        this.gamePauseController.resetCamera(playerRef);
    }

    private void forceTopdownCameraDirect(@Nonnull PlayerRef playerRef) {
        this.gamePauseController.forceTopdownCameraDirect(playerRef);
    }

    private void giveJoinWeapon(@Nonnull Player player) {
        Inventory inventory = player.getInventory();
        if (inventory == null) {
            return;
        }
        boolean bl = false;
        bl |= this.ensureJoinItem(inventory, this.resolveExistingItemId("battleaxe", JOIN_WEAPON_ITEM_ID), 1, true);
        bl |= this.ensureJoinItem(inventory, this.resolveExistingItemId("shortbow", JOIN_SHORTBOW_ITEM_ID), 1, true);
        bl |= this.ensureJoinItem(inventory, this.resolveExistingItemId("arrows", JOIN_ARROW_ITEM_ID), JOIN_ARROW_QUANTITY, false);
        bl |= this.ensureJoinItem(inventory, this.resolveExistingItemId("food", JOIN_FOOD_ITEM_ID), JOIN_FOOD_QUANTITY, false);
        bl |= this.ensureJoinItem(inventory, this.resolveExistingItemId("helmet", JOIN_ARMOR_HEAD_ITEM_ID), 1, true);
        bl |= this.ensureJoinItem(inventory, this.resolveExistingItemId("chest", JOIN_ARMOR_CHEST_ITEM_ID), 1, true);
        bl |= this.ensureJoinItem(inventory, this.resolveExistingItemId("hands", JOIN_ARMOR_HANDS_ITEM_ID), 1, true);
        if (bl |= this.ensureJoinItem(inventory, this.resolveExistingItemId("legs", JOIN_ARMOR_LEGS_ITEM_ID), 1, true)) {
            inventory.markChanged();
        }
    }

    private boolean ensureJoinItem(@Nonnull Inventory inventory, String string, int n, boolean bl) {
        if (string == null || n <= 0) {
            return false;
        }
        CombinedItemContainer combinedItemContainer = inventory.getCombinedEverything();
        int n2 = this.getTotalItemQuantity((ItemContainer)combinedItemContainer, string);
        if (n2 >= n) {
            return false;
        }
        int n3 = n - n2;
        CombinedItemContainer combinedItemContainer2 = inventory.getCombinedHotbarFirst();
        if (combinedItemContainer2 == null) {
            return false;
        }
        combinedItemContainer2.addItemStack(this.createJoinItemStack(string, n3, bl));
        return true;
    }

    private int getTotalItemQuantity(ItemContainer itemContainer, @Nonnull String string) {
        if (itemContainer == null) {
            return 0;
        }
        int n = 0;
        int n2 = Math.max(0, itemContainer.getCapacity());
        for (int i = 0; i < n2; ++i) {
            ItemStack itemStack = itemContainer.getItemStack((short)i);
            if (itemStack == null || itemStack.isEmpty() || !string.equals(itemStack.getItemId())) continue;
            n += Math.max(0, itemStack.getQuantity());
        }
        return n;
    }

    private ItemStack createJoinItemStack(@Nonnull String string, int n, boolean bl) {
        if (bl) {
            return new ItemStack(string, n, (double)JOIN_DEFAULT_DURABILITY, (double)JOIN_DEFAULT_DURABILITY, null);
        }
        return new ItemStack(string, n);
    }

    private String resolveExistingItemId(@Nonnull String string, String string2) {
        if (string2 != null && !string2.isBlank() && Item.getAssetMap().getAsset(string2) != null) {
            return string2;
        }
        this.plugin.getLogger().at(Level.WARNING).log("Join loadout item not found for %s: %s", (Object)string, (Object)string2);
        return null;
    }

    private void forceWeather(@Nonnull World world, @Nonnull Store<EntityStore> store) {
        WeatherResource weatherResource = (WeatherResource)store.getResource(WeatherResource.getResourceType());
        if (weatherResource != null) {
            weatherResource.setForcedWeather(JOIN_FORCED_WEATHER_ID);
        }
    }

    private int getLifeEssenceDelta(@Nonnull Transaction transaction) {
        if (transaction instanceof ItemStackTransaction) {
            ItemStackTransaction itemStackTransaction = (ItemStackTransaction)transaction;
            if (!itemStackTransaction.succeeded()) {
                return 0;
            }
            int n = 0;
            for (ItemStackSlotTransaction itemStackSlotTransaction : itemStackTransaction.getSlotTransactions()) {
                n += this.getDeltaFromSlot((SlotTransaction)itemStackSlotTransaction);
            }
            return n;
        }
        if (transaction instanceof SlotTransaction) {
            return this.getDeltaFromSlot((SlotTransaction)transaction);
        }
        return 0;
    }

    private int getDeltaFromSlot(@Nonnull SlotTransaction slotTransaction) {
        ItemStack itemStack = slotTransaction.getSlotBefore();
        ItemStack itemStack2 = slotTransaction.getSlotAfter();
        return this.getLifeEssenceCount(itemStack2) - this.getLifeEssenceCount(itemStack);
    }

    private int getLifeEssenceCount(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return 0;
        }
        if (!ConfigManager.get().lifeEssenceItemId.equals(itemStack.getItemId())) {
            return 0;
        }
        return Math.max(0, itemStack.getQuantity());
    }

    public void openStoreForPlayer(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull Player player, @Nonnull PlayerRef playerRef, int n) {
        if (!this.isGameplayWorld(store)) {
            return;
        }
        this.storeSessionManager.openStoreForPlayer(ref, store, player, playerRef, n);
    }

    public void closeStoreForPlayer(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        this.storeSessionManager.closeStoreForPlayer(ref, store);
    }

    public boolean canAcceptStoreSelection(@Nonnull UUID uUID) {
        return this.storeSessionManager.canAcceptStoreSelection(uUID);
    }

    private void updateHudOnPlayerReady(Player player, PlayerRef playerRef, Store store, UUID uUID, String string, LevelingSystem.LevelProgress levelProgress, Ref ref) {
        BossBarHud bossBarHud = this.ensureHud(player, playerRef, (Store<EntityStore>)store);
        if (bossBarHud != null) {
            bossBarHud.updateLevel(levelProgress.level(), levelProgress.essenceProgress(), this.levelingSystem.getEssenceRequired(levelProgress));
        } else {
        }
        VampireShooterComponent vampireShooterComponent = (VampireShooterComponent)store.getComponent(ref, HytaleMod.getInstance().getVampireShooterComponentType());
        if (vampireShooterComponent != null) {
            this.restorePowerState(uUID, vampireShooterComponent, string);
        }
        this.applyPlayerPowerBonuses(uUID, (Ref<EntityStore>)ref, (Store<EntityStore>)store, playerRef, string);
    }
}

