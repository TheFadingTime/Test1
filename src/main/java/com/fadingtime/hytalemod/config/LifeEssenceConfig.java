/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bson.BsonDocument
 *  org.bson.BsonValue
 */
package com.fadingtime.hytalemod.config;

import com.fadingtime.hytalemod.config.JsonConfigLoader;
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

    private LifeEssenceConfig(int n, int n2, int n3, String string, float f, int n4, int n5, int n6, int n7, float f2, int n8, float f3, float f4, int n9, int n10, int n11, float f5, int n12, float f6, int n13, int n14, int n15, int n16, float f7, float f8, float f9, float f10, int n17, long l, long l2, String string2, String string3, String string4, String string5, String string6, String string7, String string8, String string9, int n18, int n19, int n20, String string10) {
        this.essencePerLevelBase = n;
        this.essencePerLevelIncrement = n2;
        this.essencePerLevelMax = n3;
        this.defaultBossName = string;
        this.storeSlowmoFactor = f;
        this.storeOpenLevelStart = n4;
        this.storeOpenLevelInterval = n5;
        this.storeOpenLevelMax = n6;
        this.maxFireRateUpgrades = n7;
        this.fireRateMultiplierPerUpgrade = f2;
        this.maxPickupRangeUpgrades = n8;
        this.pickupRangePerUpgrade = f3;
        this.maxPickupRangeBonus = f4;
        this.maxExtraProjectiles = n9;
        this.maxBounceUpgrades = n10;
        this.maxWeaponDamageUpgrades = n11;
        this.weaponDamageBonusPerUpgrade = f5;
        this.maxHealthUpgrades = n12;
        this.healthBonusPerUpgrade = f6;
        this.maxSpeedUpgrades = n13;
        this.maxProjectileRainUpgrades = n14;
        this.maxLuckyUpgrades = n15;
        this.projectileRainBurstsOnPick = n16;
        this.speedBonusPerUpgrade = f7;
        this.maxSpeedMultiplier = f8;
        this.playerBaseSpeed = f9;
        this.staminaBonusPerSpeedUpgrade = f10;
        this.luckyEssenceBonusPerRank = n17;
        this.hudReadyDelayMs = l;
        this.storeInputGraceMs = l2;
        this.joinWeaponItemId = string2;
        this.joinShortbowItemId = string3;
        this.joinArrowItemId = string4;
        this.joinFoodItemId = string5;
        this.joinArmorHeadItemId = string6;
        this.joinArmorChestItemId = string7;
        this.joinArmorHandsItemId = string8;
        this.joinArmorLegsItemId = string9;
        this.joinArrowQuantity = n18;
        this.joinFoodQuantity = n19;
        this.joinDefaultDurability = n20;
        this.joinForcedWeatherId = string10;
    }

    public static LifeEssenceConfig defaults() {
        return new LifeEssenceConfig(15, 5, 15, "Giant Skeleton", 0.2f, 5, 5, 50, 4, 1.15f, 5, 10.0f, 50.0f, 10, 3, 2, 15.0f, 3, 20.0f, 1, 1, 5, 2, 0.05f, 1.05f, 10.0f, 20.0f, 1, 500L, 400L, "Weapon_Battleaxe_Mithril", "Weapon_Shortbow_Mithril", "Weapon_Arrow_Crude", "Food_Bread", "Armor_Mithril_Head", "Armor_Mithril_Chest", "Armor_Mithril_Hands", "Armor_Mithril_Legs", 200, 25, 5000, "Zone4_Lava_Fields");
    }

    public static LifeEssenceConfig load(Path path, Logger logger) {
        LifeEssenceConfig lifeEssenceConfig = LifeEssenceConfig.defaults();
        BsonDocument bsonDocument = JsonConfigLoader.loadDocument(path, "life-essence.json", "config/life-essence.json", logger);
        return new LifeEssenceConfig(LifeEssenceConfig.readInt(bsonDocument, "essencePerLevelBase", lifeEssenceConfig.essencePerLevelBase, 1), LifeEssenceConfig.readInt(bsonDocument, "essencePerLevelIncrement", lifeEssenceConfig.essencePerLevelIncrement, 0), LifeEssenceConfig.readInt(bsonDocument, "essencePerLevelMax", lifeEssenceConfig.essencePerLevelMax, 1), LifeEssenceConfig.readString(bsonDocument, "defaultBossName", lifeEssenceConfig.defaultBossName), LifeEssenceConfig.readFloat(bsonDocument, "storeSlowmoFactor", lifeEssenceConfig.storeSlowmoFactor, 0.01f), LifeEssenceConfig.readInt(bsonDocument, "storeOpenLevelStart", lifeEssenceConfig.storeOpenLevelStart, 1), LifeEssenceConfig.readInt(bsonDocument, "storeOpenLevelInterval", lifeEssenceConfig.storeOpenLevelInterval, 1), LifeEssenceConfig.readInt(bsonDocument, "storeOpenLevelMax", lifeEssenceConfig.storeOpenLevelMax, 1), LifeEssenceConfig.readInt(bsonDocument, "maxFireRateUpgrades", lifeEssenceConfig.maxFireRateUpgrades, 0), LifeEssenceConfig.readFloat(bsonDocument, "fireRateMultiplierPerUpgrade", lifeEssenceConfig.fireRateMultiplierPerUpgrade, 0.001f), LifeEssenceConfig.readInt(bsonDocument, "maxPickupRangeUpgrades", lifeEssenceConfig.maxPickupRangeUpgrades, 0), LifeEssenceConfig.readFloat(bsonDocument, "pickupRangePerUpgrade", lifeEssenceConfig.pickupRangePerUpgrade, 0.0f), LifeEssenceConfig.readFloat(bsonDocument, "maxPickupRangeBonus", lifeEssenceConfig.maxPickupRangeBonus, 0.0f), LifeEssenceConfig.readInt(bsonDocument, "maxExtraProjectiles", lifeEssenceConfig.maxExtraProjectiles, 0), LifeEssenceConfig.readInt(bsonDocument, "maxBounceUpgrades", lifeEssenceConfig.maxBounceUpgrades, 0), LifeEssenceConfig.readInt(bsonDocument, "maxWeaponDamageUpgrades", lifeEssenceConfig.maxWeaponDamageUpgrades, 0), LifeEssenceConfig.readFloat(bsonDocument, "weaponDamageBonusPerUpgrade", lifeEssenceConfig.weaponDamageBonusPerUpgrade, 0.0f), LifeEssenceConfig.readInt(bsonDocument, "maxHealthUpgrades", lifeEssenceConfig.maxHealthUpgrades, 0), LifeEssenceConfig.readFloat(bsonDocument, "healthBonusPerUpgrade", lifeEssenceConfig.healthBonusPerUpgrade, 0.0f), LifeEssenceConfig.readInt(bsonDocument, "maxSpeedUpgrades", lifeEssenceConfig.maxSpeedUpgrades, 0), LifeEssenceConfig.readInt(bsonDocument, "maxProjectileRainUpgrades", lifeEssenceConfig.maxProjectileRainUpgrades, 0), LifeEssenceConfig.readInt(bsonDocument, "maxLuckyUpgrades", lifeEssenceConfig.maxLuckyUpgrades, 0), LifeEssenceConfig.readInt(bsonDocument, "projectileRainBurstsOnPick", lifeEssenceConfig.projectileRainBurstsOnPick, 0), LifeEssenceConfig.readFloat(bsonDocument, "speedBonusPerUpgrade", lifeEssenceConfig.speedBonusPerUpgrade, 0.0f), LifeEssenceConfig.readFloat(bsonDocument, "maxSpeedMultiplier", lifeEssenceConfig.maxSpeedMultiplier, 0.1f), LifeEssenceConfig.readFloat(bsonDocument, "playerBaseSpeed", lifeEssenceConfig.playerBaseSpeed, 0.1f), LifeEssenceConfig.readFloat(bsonDocument, "staminaBonusPerSpeedUpgrade", lifeEssenceConfig.staminaBonusPerSpeedUpgrade, 0.0f), LifeEssenceConfig.readInt(bsonDocument, "luckyEssenceBonusPerRank", lifeEssenceConfig.luckyEssenceBonusPerRank, 0), LifeEssenceConfig.readLong(bsonDocument, "hudReadyDelayMs", lifeEssenceConfig.hudReadyDelayMs, 0L), LifeEssenceConfig.readLong(bsonDocument, "storeInputGraceMs", lifeEssenceConfig.storeInputGraceMs, 0L), LifeEssenceConfig.readString(bsonDocument, "joinWeaponItemId", lifeEssenceConfig.joinWeaponItemId), LifeEssenceConfig.readString(bsonDocument, "joinShortbowItemId", lifeEssenceConfig.joinShortbowItemId), LifeEssenceConfig.readString(bsonDocument, "joinArrowItemId", lifeEssenceConfig.joinArrowItemId), LifeEssenceConfig.readString(bsonDocument, "joinFoodItemId", lifeEssenceConfig.joinFoodItemId), LifeEssenceConfig.readString(bsonDocument, "joinArmorHeadItemId", lifeEssenceConfig.joinArmorHeadItemId), LifeEssenceConfig.readString(bsonDocument, "joinArmorChestItemId", lifeEssenceConfig.joinArmorChestItemId), LifeEssenceConfig.readString(bsonDocument, "joinArmorHandsItemId", lifeEssenceConfig.joinArmorHandsItemId), LifeEssenceConfig.readString(bsonDocument, "joinArmorLegsItemId", lifeEssenceConfig.joinArmorLegsItemId), LifeEssenceConfig.readInt(bsonDocument, "joinArrowQuantity", lifeEssenceConfig.joinArrowQuantity, 0), LifeEssenceConfig.readInt(bsonDocument, "joinFoodQuantity", lifeEssenceConfig.joinFoodQuantity, 0), LifeEssenceConfig.readInt(bsonDocument, "joinDefaultDurability", lifeEssenceConfig.joinDefaultDurability, 1), LifeEssenceConfig.readString(bsonDocument, "joinForcedWeatherId", lifeEssenceConfig.joinForcedWeatherId));
    }

    private static int readInt(BsonDocument bsonDocument, String string, int n, int n2) {
        BsonValue bsonValue = bsonDocument.get((Object)string);
        if (bsonValue == null || !bsonValue.isNumber()) {
            return n;
        }
        return Math.max(n2, bsonValue.asNumber().intValue());
    }

    private static long readLong(BsonDocument bsonDocument, String string, long l, long l2) {
        BsonValue bsonValue = bsonDocument.get((Object)string);
        if (bsonValue == null || !bsonValue.isNumber()) {
            return l;
        }
        return Math.max(l2, bsonValue.asNumber().longValue());
    }

    private static float readFloat(BsonDocument bsonDocument, String string, float f, float f2) {
        BsonValue bsonValue = bsonDocument.get((Object)string);
        if (bsonValue == null || !bsonValue.isNumber()) {
            return f;
        }
        float f3 = (float)bsonValue.asNumber().doubleValue();
        if (!Float.isFinite(f3)) {
            return f;
        }
        return Math.max(f2, f3);
    }

    private static String readString(BsonDocument bsonDocument, String string, String string2) {
        BsonValue bsonValue = bsonDocument.get((Object)string);
        if (bsonValue == null || !bsonValue.isString()) {
            return string2;
        }
        String string3 = bsonValue.asString().getValue();
        if (string3 == null || string3.isBlank()) {
            return string2;
        }
        return string3.trim();
    }
}

