package com.fadingtime.hytalemod.config;

import java.nio.file.Path;
import java.util.logging.Logger;
import org.bson.BsonDocument;
import org.bson.BsonValue;

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
        String joinForcedWeatherId
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
            "Zone4_Lava_Fields"
        );
    }

    public static LifeEssenceConfig load(Path pluginFilePath, Logger logger) {
        LifeEssenceConfig defaults = LifeEssenceConfig.defaults();
        BsonDocument doc = JsonConfigLoader.loadDocument(pluginFilePath, "life-essence.json", "config/life-essence.json", logger);
        return new LifeEssenceConfig(
            LifeEssenceConfig.readInt(doc, "essencePerLevelBase", defaults.essencePerLevelBase, 1),
            LifeEssenceConfig.readInt(doc, "essencePerLevelIncrement", defaults.essencePerLevelIncrement, 0),
            LifeEssenceConfig.readInt(doc, "essencePerLevelMax", defaults.essencePerLevelMax, 1),
            LifeEssenceConfig.readString(doc, "defaultBossName", defaults.defaultBossName),
            LifeEssenceConfig.readFloat(doc, "storeSlowmoFactor", defaults.storeSlowmoFactor, 0.01f),
            LifeEssenceConfig.readInt(doc, "storeOpenLevelStart", defaults.storeOpenLevelStart, 1),
            LifeEssenceConfig.readInt(doc, "storeOpenLevelInterval", defaults.storeOpenLevelInterval, 1),
            LifeEssenceConfig.readInt(doc, "storeOpenLevelMax", defaults.storeOpenLevelMax, 1),
            LifeEssenceConfig.readInt(doc, "maxFireRateUpgrades", defaults.maxFireRateUpgrades, 0),
            LifeEssenceConfig.readFloat(doc, "fireRateMultiplierPerUpgrade", defaults.fireRateMultiplierPerUpgrade, 0.001f),
            LifeEssenceConfig.readInt(doc, "maxPickupRangeUpgrades", defaults.maxPickupRangeUpgrades, 0),
            LifeEssenceConfig.readFloat(doc, "pickupRangePerUpgrade", defaults.pickupRangePerUpgrade, 0.0f),
            LifeEssenceConfig.readFloat(doc, "maxPickupRangeBonus", defaults.maxPickupRangeBonus, 0.0f),
            LifeEssenceConfig.readInt(doc, "maxExtraProjectiles", defaults.maxExtraProjectiles, 0),
            LifeEssenceConfig.readInt(doc, "maxBounceUpgrades", defaults.maxBounceUpgrades, 0),
            LifeEssenceConfig.readInt(doc, "maxWeaponDamageUpgrades", defaults.maxWeaponDamageUpgrades, 0),
            LifeEssenceConfig.readFloat(doc, "weaponDamageBonusPerUpgrade", defaults.weaponDamageBonusPerUpgrade, 0.0f),
            LifeEssenceConfig.readInt(doc, "maxHealthUpgrades", defaults.maxHealthUpgrades, 0),
            LifeEssenceConfig.readFloat(doc, "healthBonusPerUpgrade", defaults.healthBonusPerUpgrade, 0.0f),
            LifeEssenceConfig.readInt(doc, "maxSpeedUpgrades", defaults.maxSpeedUpgrades, 0),
            LifeEssenceConfig.readInt(doc, "maxProjectileRainUpgrades", defaults.maxProjectileRainUpgrades, 0),
            LifeEssenceConfig.readInt(doc, "maxLuckyUpgrades", defaults.maxLuckyUpgrades, 0),
            LifeEssenceConfig.readInt(doc, "projectileRainBurstsOnPick", defaults.projectileRainBurstsOnPick, 0),
            LifeEssenceConfig.readFloat(doc, "speedBonusPerUpgrade", defaults.speedBonusPerUpgrade, 0.0f),
            LifeEssenceConfig.readFloat(doc, "maxSpeedMultiplier", defaults.maxSpeedMultiplier, 0.1f),
            LifeEssenceConfig.readFloat(doc, "playerBaseSpeed", defaults.playerBaseSpeed, 0.1f),
            LifeEssenceConfig.readFloat(doc, "staminaBonusPerSpeedUpgrade", defaults.staminaBonusPerSpeedUpgrade, 0.0f),
            LifeEssenceConfig.readInt(doc, "luckyEssenceBonusPerRank", defaults.luckyEssenceBonusPerRank, 0),
            LifeEssenceConfig.readLong(doc, "hudReadyDelayMs", defaults.hudReadyDelayMs, 0L),
            LifeEssenceConfig.readLong(doc, "storeInputGraceMs", defaults.storeInputGraceMs, 0L),
            LifeEssenceConfig.readString(doc, "joinWeaponItemId", defaults.joinWeaponItemId),
            LifeEssenceConfig.readString(doc, "joinShortbowItemId", defaults.joinShortbowItemId),
            LifeEssenceConfig.readString(doc, "joinArrowItemId", defaults.joinArrowItemId),
            LifeEssenceConfig.readString(doc, "joinFoodItemId", defaults.joinFoodItemId),
            LifeEssenceConfig.readString(doc, "joinArmorHeadItemId", defaults.joinArmorHeadItemId),
            LifeEssenceConfig.readString(doc, "joinArmorChestItemId", defaults.joinArmorChestItemId),
            LifeEssenceConfig.readString(doc, "joinArmorHandsItemId", defaults.joinArmorHandsItemId),
            LifeEssenceConfig.readString(doc, "joinArmorLegsItemId", defaults.joinArmorLegsItemId),
            LifeEssenceConfig.readInt(doc, "joinArrowQuantity", defaults.joinArrowQuantity, 0),
            LifeEssenceConfig.readInt(doc, "joinFoodQuantity", defaults.joinFoodQuantity, 0),
            LifeEssenceConfig.readInt(doc, "joinDefaultDurability", defaults.joinDefaultDurability, 1),
            LifeEssenceConfig.readString(doc, "joinForcedWeatherId", defaults.joinForcedWeatherId)
        );
    }

    private static int readInt(BsonDocument doc, String key, int defaultValue, int min) {
        BsonValue value = doc.get(key);
        if (value == null || !value.isNumber()) {
            return defaultValue;
        }
        return Math.max(min, value.asNumber().intValue());
    }

    private static long readLong(BsonDocument doc, String key, long defaultValue, long min) {
        BsonValue value = doc.get(key);
        if (value == null || !value.isNumber()) {
            return defaultValue;
        }
        return Math.max(min, value.asNumber().longValue());
    }

    private static float readFloat(BsonDocument doc, String key, float defaultValue, float min) {
        BsonValue value = doc.get(key);
        if (value == null || !value.isNumber()) {
            return defaultValue;
        }
        float parsed = (float)value.asNumber().doubleValue();
        if (!Float.isFinite(parsed)) {
            return defaultValue;
        }
        return Math.max(min, parsed);
    }

    private static String readString(BsonDocument doc, String key, String defaultValue) {
        BsonValue value = doc.get(key);
        if (value == null || !value.isString()) {
            return defaultValue;
        }
        String parsed = value.asString().getValue();
        if (parsed == null || parsed.isBlank()) {
            return defaultValue;
        }
        return parsed.trim();
    }
}
