package com.fadingtime.hytalemod.config;

import java.nio.file.Path;
import java.util.logging.Logger;
import org.bson.BsonDocument;

public final class LifeEssenceConfig {
    public final int essencePerLevelBase;
    public final int essencePerLevelIncrement;
    public final int essencePerLevelMax;
    public final String defaultBossName;
    public final float storeSlowmoFactor;
    public final int storeOpenLevelStart;
    public final int storeOpenLevelInterval;
    public final int storeOpenLevelMax;
    public final int maxFireRateUpgrades;
    public final float fireRateMultiplierPerUpgrade;
    public final int maxPickupRangeUpgrades;
    public final float pickupRangePerUpgrade;
    public final float maxPickupRangeBonus;
    public final int maxExtraProjectiles;
    public final int maxBounceUpgrades;
    public final int maxWeaponDamageUpgrades;
    public final float weaponDamageBonusPerUpgrade;
    public final int maxHealthUpgrades;
    public final float healthBonusPerUpgrade;
    public final int maxSpeedUpgrades;
    public final int maxProjectileRainUpgrades;
    public final int maxLuckyUpgrades;
    public final int projectileRainBurstsOnPick;
    public final float speedBonusPerUpgrade;
    public final float maxSpeedMultiplier;
    public final float playerBaseSpeed;
    public final float staminaBonusPerSpeedUpgrade;
    public final int luckyEssenceBonusPerRank;
    public final long hudReadyDelayMs;
    public final long storeInputGraceMs;
    public final String joinWeaponItemId;
    public final String joinShortbowItemId;
    public final String joinArrowItemId;
    public final String joinFoodItemId;
    public final String joinArmorHeadItemId;
    public final String joinArmorChestItemId;
    public final String joinArmorHandsItemId;
    public final String joinArmorLegsItemId;
    public final int joinArrowQuantity;
    public final int joinFoodQuantity;
    public final int joinDefaultDurability;
    public final String joinForcedWeatherId;
    public final float cameraDistance;
    public final float cameraPositionLerpSpeed;
    public final float cameraRotationLerpSpeed;
    public final float cameraPitch;
    public final long disconnectGracePeriodMs;
    public final long powerBonusEarlyDelayMs;
    public final long powerBonusLateDelayMs;
    public final float speedLerpFactor;
    public final float minFireRateClamp;

    private LifeEssenceConfig(
        int essencePerLevelBase,
        int essencePerLevelIncrement,
        int essencePerLevelMax,
        String defaultBossName,
        float storeSlowmoFactor,
        int storeOpenLevelStart,
        int storeOpenLevelInterval,
        int storeOpenLevelMax,
        int maxFireRateUpgrades,
        float fireRateMultiplierPerUpgrade,
        int maxPickupRangeUpgrades,
        float pickupRangePerUpgrade,
        float maxPickupRangeBonus,
        int maxExtraProjectiles,
        int maxBounceUpgrades,
        int maxWeaponDamageUpgrades,
        float weaponDamageBonusPerUpgrade,
        int maxHealthUpgrades,
        float healthBonusPerUpgrade,
        int maxSpeedUpgrades,
        int maxProjectileRainUpgrades,
        int maxLuckyUpgrades,
        int projectileRainBurstsOnPick,
        float speedBonusPerUpgrade,
        float maxSpeedMultiplier,
        float playerBaseSpeed,
        float staminaBonusPerSpeedUpgrade,
        int luckyEssenceBonusPerRank,
        long hudReadyDelayMs,
        long storeInputGraceMs,
        String joinWeaponItemId,
        String joinShortbowItemId,
        String joinArrowItemId,
        String joinFoodItemId,
        String joinArmorHeadItemId,
        String joinArmorChestItemId,
        String joinArmorHandsItemId,
        String joinArmorLegsItemId,
        int joinArrowQuantity,
        int joinFoodQuantity,
        int joinDefaultDurability,
        String joinForcedWeatherId,
        float cameraDistance,
        float cameraPositionLerpSpeed,
        float cameraRotationLerpSpeed,
        float cameraPitch,
        long disconnectGracePeriodMs,
        long powerBonusEarlyDelayMs,
        long powerBonusLateDelayMs,
        float speedLerpFactor,
        float minFireRateClamp
    ) {
        this.essencePerLevelBase = essencePerLevelBase;
        this.essencePerLevelIncrement = essencePerLevelIncrement;
        this.essencePerLevelMax = essencePerLevelMax;
        this.defaultBossName = defaultBossName;
        this.storeSlowmoFactor = storeSlowmoFactor;
        this.storeOpenLevelStart = storeOpenLevelStart;
        this.storeOpenLevelInterval = storeOpenLevelInterval;
        this.storeOpenLevelMax = storeOpenLevelMax;
        this.maxFireRateUpgrades = maxFireRateUpgrades;
        this.fireRateMultiplierPerUpgrade = fireRateMultiplierPerUpgrade;
        this.maxPickupRangeUpgrades = maxPickupRangeUpgrades;
        this.pickupRangePerUpgrade = pickupRangePerUpgrade;
        this.maxPickupRangeBonus = maxPickupRangeBonus;
        this.maxExtraProjectiles = maxExtraProjectiles;
        this.maxBounceUpgrades = maxBounceUpgrades;
        this.maxWeaponDamageUpgrades = maxWeaponDamageUpgrades;
        this.weaponDamageBonusPerUpgrade = weaponDamageBonusPerUpgrade;
        this.maxHealthUpgrades = maxHealthUpgrades;
        this.healthBonusPerUpgrade = healthBonusPerUpgrade;
        this.maxSpeedUpgrades = maxSpeedUpgrades;
        this.maxProjectileRainUpgrades = maxProjectileRainUpgrades;
        this.maxLuckyUpgrades = maxLuckyUpgrades;
        this.projectileRainBurstsOnPick = projectileRainBurstsOnPick;
        this.speedBonusPerUpgrade = speedBonusPerUpgrade;
        this.maxSpeedMultiplier = maxSpeedMultiplier;
        this.playerBaseSpeed = playerBaseSpeed;
        this.staminaBonusPerSpeedUpgrade = staminaBonusPerSpeedUpgrade;
        this.luckyEssenceBonusPerRank = luckyEssenceBonusPerRank;
        this.hudReadyDelayMs = hudReadyDelayMs;
        this.storeInputGraceMs = storeInputGraceMs;
        this.joinWeaponItemId = joinWeaponItemId;
        this.joinShortbowItemId = joinShortbowItemId;
        this.joinArrowItemId = joinArrowItemId;
        this.joinFoodItemId = joinFoodItemId;
        this.joinArmorHeadItemId = joinArmorHeadItemId;
        this.joinArmorChestItemId = joinArmorChestItemId;
        this.joinArmorHandsItemId = joinArmorHandsItemId;
        this.joinArmorLegsItemId = joinArmorLegsItemId;
        this.joinArrowQuantity = joinArrowQuantity;
        this.joinFoodQuantity = joinFoodQuantity;
        this.joinDefaultDurability = joinDefaultDurability;
        this.joinForcedWeatherId = joinForcedWeatherId;
        this.cameraDistance = cameraDistance;
        this.cameraPositionLerpSpeed = cameraPositionLerpSpeed;
        this.cameraRotationLerpSpeed = cameraRotationLerpSpeed;
        this.cameraPitch = cameraPitch;
        this.disconnectGracePeriodMs = disconnectGracePeriodMs;
        this.powerBonusEarlyDelayMs = powerBonusEarlyDelayMs;
        this.powerBonusLateDelayMs = powerBonusLateDelayMs;
        this.speedLerpFactor = speedLerpFactor;
        this.minFireRateClamp = minFireRateClamp;
    }

    public static LifeEssenceConfig defaults() {
        return new LifeEssenceConfig(
            15,
            5,
            15,
            "Giant Skeleton",
            0.2f,
            5,
            5,
            50,
            4,
            1.15f,
            5,
            10.0f,
            50.0f,
            10,
            3,
            2,
            15.0f,
            3,
            20.0f,
            1,
            1,
            5,
            2,
            0.05f,
            1.05f,
            10.0f,
            20.0f,
            1,
            500L,
            400L,
            "Weapon_Battleaxe_Mithril",
            "Weapon_Shortbow_Mithril",
            "Weapon_Arrow_Crude",
            "Food_Bread",
            "Armor_Mithril_Head",
            "Armor_Mithril_Chest",
            "Armor_Mithril_Hands",
            "Armor_Mithril_Legs",
            200,
            25,
            5000,
            "Zone4_Lava_Fields",
            20.0f,
            0.2f,
            0.2f,
            -1.5707964f,
            1200L,
            300L,
            1200L,
            0.2f,
            0.1f
        );
    }

    public static LifeEssenceConfig load(Path pluginFilePath, Logger logger) {
        LifeEssenceConfig defaults = LifeEssenceConfig.defaults();
        BsonDocument doc = JsonConfigLoader.loadDocument(pluginFilePath, "life-essence.json", "config/life-essence.json", logger);
        BsonDocument essence = ConfigUtils.getSubDocument(doc, "essence");
        BsonDocument store = ConfigUtils.getSubDocument(doc, "store");
        BsonDocument upgrades = ConfigUtils.getSubDocument(doc, "upgrades");
        BsonDocument player = ConfigUtils.getSubDocument(doc, "player");
        BsonDocument joinLoadout = ConfigUtils.getSubDocument(doc, "joinLoadout");
        BsonDocument camera = ConfigUtils.getSubDocument(doc, "camera");
        BsonDocument ui = ConfigUtils.getSubDocument(doc, "ui");
        return new LifeEssenceConfig(
            ConfigUtils.readInt(essence, "perLevelBase", defaults.essencePerLevelBase, 1),
            ConfigUtils.readInt(essence, "perLevelIncrement", defaults.essencePerLevelIncrement, 0),
            ConfigUtils.readInt(essence, "perLevelMax", defaults.essencePerLevelMax, 1),
            ConfigUtils.readString(ui, "defaultBossName", defaults.defaultBossName),
            ConfigUtils.readFloat(store, "slowmoFactor", defaults.storeSlowmoFactor, 0.01f),
            ConfigUtils.readInt(store, "openLevelStart", defaults.storeOpenLevelStart, 1),
            ConfigUtils.readInt(store, "openLevelInterval", defaults.storeOpenLevelInterval, 1),
            ConfigUtils.readInt(store, "openLevelMax", defaults.storeOpenLevelMax, 1),
            ConfigUtils.readInt(upgrades, "maxFireRate", defaults.maxFireRateUpgrades, 0),
            ConfigUtils.readFloat(upgrades, "fireRateMultiplierPerUpgrade", defaults.fireRateMultiplierPerUpgrade, 0.001f),
            ConfigUtils.readInt(upgrades, "maxPickupRange", defaults.maxPickupRangeUpgrades, 0),
            ConfigUtils.readFloat(upgrades, "pickupRangePerUpgrade", defaults.pickupRangePerUpgrade, 0.0f),
            ConfigUtils.readFloat(upgrades, "maxPickupRangeBonus", defaults.maxPickupRangeBonus, 0.0f),
            ConfigUtils.readInt(upgrades, "maxExtraProjectiles", defaults.maxExtraProjectiles, 0),
            ConfigUtils.readInt(upgrades, "maxBounce", defaults.maxBounceUpgrades, 0),
            ConfigUtils.readInt(upgrades, "maxWeaponDamage", defaults.maxWeaponDamageUpgrades, 0),
            ConfigUtils.readFloat(upgrades, "weaponDamageBonusPerUpgrade", defaults.weaponDamageBonusPerUpgrade, 0.0f),
            ConfigUtils.readInt(upgrades, "maxHealth", defaults.maxHealthUpgrades, 0),
            ConfigUtils.readFloat(upgrades, "healthBonusPerUpgrade", defaults.healthBonusPerUpgrade, 0.0f),
            ConfigUtils.readInt(upgrades, "maxSpeed", defaults.maxSpeedUpgrades, 0),
            ConfigUtils.readInt(upgrades, "maxProjectileRain", defaults.maxProjectileRainUpgrades, 0),
            ConfigUtils.readInt(upgrades, "maxLucky", defaults.maxLuckyUpgrades, 0),
            ConfigUtils.readInt(upgrades, "projectileRainBurstsOnPick", defaults.projectileRainBurstsOnPick, 0),
            ConfigUtils.readFloat(upgrades, "speedBonusPerUpgrade", defaults.speedBonusPerUpgrade, 0.0f),
            ConfigUtils.readFloat(upgrades, "maxSpeedMultiplier", defaults.maxSpeedMultiplier, 0.1f),
            ConfigUtils.readFloat(player, "baseSpeed", defaults.playerBaseSpeed, 0.1f),
            ConfigUtils.readFloat(upgrades, "staminaBonusPerSpeedUpgrade", defaults.staminaBonusPerSpeedUpgrade, 0.0f),
            ConfigUtils.readInt(upgrades, "luckyEssenceBonusPerRank", defaults.luckyEssenceBonusPerRank, 0),
            ConfigUtils.readLong(ui, "hudReadyDelayMs", defaults.hudReadyDelayMs, 0L),
            ConfigUtils.readLong(store, "inputGraceMs", defaults.storeInputGraceMs, 0L),
            ConfigUtils.readString(joinLoadout, "weaponItemId", defaults.joinWeaponItemId),
            ConfigUtils.readString(joinLoadout, "shortbowItemId", defaults.joinShortbowItemId),
            ConfigUtils.readString(joinLoadout, "arrowItemId", defaults.joinArrowItemId),
            ConfigUtils.readString(joinLoadout, "foodItemId", defaults.joinFoodItemId),
            ConfigUtils.readString(joinLoadout, "armorHeadItemId", defaults.joinArmorHeadItemId),
            ConfigUtils.readString(joinLoadout, "armorChestItemId", defaults.joinArmorChestItemId),
            ConfigUtils.readString(joinLoadout, "armorHandsItemId", defaults.joinArmorHandsItemId),
            ConfigUtils.readString(joinLoadout, "armorLegsItemId", defaults.joinArmorLegsItemId),
            ConfigUtils.readInt(joinLoadout, "arrowQuantity", defaults.joinArrowQuantity, 0),
            ConfigUtils.readInt(joinLoadout, "foodQuantity", defaults.joinFoodQuantity, 0),
            ConfigUtils.readInt(joinLoadout, "defaultDurability", defaults.joinDefaultDurability, 1),
            ConfigUtils.readString(joinLoadout, "forcedWeatherId", defaults.joinForcedWeatherId),
            ConfigUtils.readFloat(camera, "distance", defaults.cameraDistance, 1.0f),
            ConfigUtils.readFloat(camera, "positionLerpSpeed", defaults.cameraPositionLerpSpeed, 0.01f),
            ConfigUtils.readFloat(camera, "rotationLerpSpeed", defaults.cameraRotationLerpSpeed, 0.01f),
            ConfigUtils.readFloatUnbounded(camera, "pitch", defaults.cameraPitch),
            ConfigUtils.readLong(player, "disconnectGracePeriodMs", defaults.disconnectGracePeriodMs, 0L),
            ConfigUtils.readLong(player, "powerBonusEarlyDelayMs", defaults.powerBonusEarlyDelayMs, 0L),
            ConfigUtils.readLong(player, "powerBonusLateDelayMs", defaults.powerBonusLateDelayMs, 0L),
            ConfigUtils.readFloat(player, "speedLerpFactor", defaults.speedLerpFactor, 0.01f),
            ConfigUtils.readFloat(upgrades, "minFireRateClamp", defaults.minFireRateClamp, 0.001f)
        );
    }
}
