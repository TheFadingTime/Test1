/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.protocol.MovementSettings
 *  com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager
 *  com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap
 *  com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes
 *  com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier
 *  com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier$ModifierTarget
 *  com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier
 *  com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier$CalculationType
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 *  javax.annotation.Nonnull
 */
package com.fadingtime.hytalemod.system;

import com.fadingtime.hytalemod.component.VampireShooterComponent;
import com.fadingtime.hytalemod.config.LifeEssenceConfig;
import com.fadingtime.hytalemod.persistence.PlayerStateStore;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.MovementSettings;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

public final class PowerUpApplicator {
    private static final String PLAYER_HEALTH_BONUS_MODIFIER_ID = "PlayerHealthBonus";
    private static final String PLAYER_SPEED_STAMINA_BONUS_MODIFIER_ID = "PlayerSpeedStaminaBonus";
    private static final String SKIP_CHOICE = "skip_store";
    private final int maxExtraProjectiles;
    private final int maxBounceUpgrades;
    private final int maxWeaponDamageUpgrades;
    private final int maxHealthUpgrades;
    private final int maxSpeedUpgrades;
    private final int maxLuckyUpgrades;
    private final int maxFireRateUpgrades;
    private final float fireRateMultiplierPerUpgrade;
    private final float maxFireRateMultiplier;
    private final int maxPickupRangeUpgrades;
    private final float pickupRangePerUpgrade;
    private final float maxPickupRangeBonus;
    private final float weaponDamageBonusPerUpgrade;
    private final float healthBonusPerUpgrade;
    private final float speedBonusPerUpgrade;
    private final float maxSpeedMultiplier;
    private final float playerBaseSpeed;
    private final float staminaBonusPerSpeedUpgrade;
    private final int luckyEssenceBonusPerRank;
    private final int maxProjectileRainUpgrades;
    private final int projectileRainBurstsOnPick;

    public PowerUpApplicator(int n, int n2, int n3, int n4, int n5, int n6, int n7, float f, int n8, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, int n9, int n10, int n11) {
        this.maxExtraProjectiles = Math.max(0, n);
        this.maxBounceUpgrades = Math.max(0, n2);
        this.maxWeaponDamageUpgrades = Math.max(0, n3);
        this.maxHealthUpgrades = Math.max(0, n4);
        this.maxSpeedUpgrades = Math.max(0, n5);
        this.maxLuckyUpgrades = Math.max(0, n6);
        this.maxFireRateUpgrades = Math.max(0, n7);
        this.fireRateMultiplierPerUpgrade = f <= 0.0f ? 1.15f : f;
        this.maxFireRateMultiplier = (float)Math.pow(this.fireRateMultiplierPerUpgrade, this.maxFireRateUpgrades);
        this.maxPickupRangeUpgrades = Math.max(0, n8);
        this.pickupRangePerUpgrade = f2 <= 0.0f ? 10.0f : f2;
        this.maxPickupRangeBonus = Math.max(0.0f, f3);
        this.weaponDamageBonusPerUpgrade = Math.max(0.0f, f4);
        this.healthBonusPerUpgrade = Math.max(0.0f, f5);
        this.speedBonusPerUpgrade = Math.max(0.0f, f6);
        this.maxSpeedMultiplier = Math.max(1.0f, f7);
        this.playerBaseSpeed = Math.max(0.01f, f8);
        this.staminaBonusPerSpeedUpgrade = Math.max(0.0f, f9);
        this.luckyEssenceBonusPerRank = Math.max(0, n9);
        this.maxProjectileRainUpgrades = Math.max(0, n10);
        this.projectileRainBurstsOnPick = Math.max(1, n11);
    }

    public static PowerUpApplicator fromConfig(@Nonnull LifeEssenceConfig lifeEssenceConfig) {
        return new PowerUpApplicator(lifeEssenceConfig.maxExtraProjectiles, lifeEssenceConfig.maxBounceUpgrades, lifeEssenceConfig.maxWeaponDamageUpgrades, lifeEssenceConfig.maxHealthUpgrades, lifeEssenceConfig.maxSpeedUpgrades, lifeEssenceConfig.maxLuckyUpgrades, lifeEssenceConfig.maxFireRateUpgrades, lifeEssenceConfig.fireRateMultiplierPerUpgrade, lifeEssenceConfig.maxPickupRangeUpgrades, lifeEssenceConfig.pickupRangePerUpgrade, lifeEssenceConfig.maxPickupRangeBonus, lifeEssenceConfig.weaponDamageBonusPerUpgrade, lifeEssenceConfig.healthBonusPerUpgrade, lifeEssenceConfig.speedBonusPerUpgrade, lifeEssenceConfig.maxSpeedMultiplier, lifeEssenceConfig.playerBaseSpeed, lifeEssenceConfig.staminaBonusPerSpeedUpgrade, lifeEssenceConfig.luckyEssenceBonusPerRank, lifeEssenceConfig.maxProjectileRainUpgrades, lifeEssenceConfig.projectileRainBurstsOnPick);
    }

    public PowerState newState() {
        return new PowerState();
    }

    public PowerState fromPersisted(@Nonnull PlayerStateStore.PlayerPowerData playerPowerData) {
        PowerState powerState = new PowerState();
        powerState.projectileCount = this.clampProjectileCount(playerPowerData.projectileCount);
        powerState.fireRateMultiplier = this.clampFireRate(playerPowerData.fireRateMultiplier);
        powerState.pickupRangeBonus = this.clampPickupRange(playerPowerData.pickupRangeBonus);
        powerState.bounceBonus = this.clampBounceBonus(playerPowerData.bounceBonus);
        powerState.weaponDamageRank = this.clampWeaponDamageRank(playerPowerData.weaponDamageRank);
        powerState.healthRank = this.clampHealthRank(playerPowerData.healthRank);
        powerState.speedRank = this.clampSpeedRank(playerPowerData.speedRank);
        powerState.luckyRank = this.clampLuckyRank(playerPowerData.luckyRank);
        powerState.skipRank = Math.max(0, playerPowerData.skipRank);
        powerState.projectileRainUsed = playerPowerData.projectileRainUsed;
        return powerState;
    }

    public boolean applyChoice(@Nonnull PowerState powerState, @Nonnull String string) {
        if ("extra_projectile".equals(string)) {
            powerState.projectileCount = this.clampProjectileCount(powerState.projectileCount + 1);
            return false;
        }
        if ("fire_rate".equals(string)) {
            powerState.fireRateMultiplier = this.clampFireRate(powerState.fireRateMultiplier * this.fireRateMultiplierPerUpgrade);
            return false;
        }
        if ("pickup_range".equals(string)) {
            powerState.pickupRangeBonus = this.clampPickupRange(powerState.pickupRangeBonus + this.pickupRangePerUpgrade);
            return false;
        }
        if ("bounce".equals(string)) {
            powerState.bounceBonus = this.clampBounceBonus(powerState.bounceBonus + 1);
            return false;
        }
        if ("weapon_damage".equals(string)) {
            powerState.weaponDamageRank = this.clampWeaponDamageRank(powerState.weaponDamageRank + 1);
            return false;
        }
        if ("max_health".equals(string)) {
            powerState.healthRank = this.clampHealthRank(powerState.healthRank + 1);
            return false;
        }
        if ("move_speed".equals(string)) {
            powerState.speedRank = this.clampSpeedRank(powerState.speedRank + 1);
            return false;
        }
        if ("lucky".equals(string)) {
            powerState.luckyRank = this.clampLuckyRank(powerState.luckyRank + 1);
            return false;
        }
        if (SKIP_CHOICE.equals(string)) {
            if (powerState.skipRank < Integer.MAX_VALUE) {
                ++powerState.skipRank;
            }
            return false;
        }
        return "projectile_rain".equals(string);
    }

    public void applyToShooter(@Nonnull PowerState powerState, @Nonnull VampireShooterComponent vampireShooterComponent) {
        vampireShooterComponent.setProjectileCount(this.clampProjectileCount(powerState.projectileCount));
        vampireShooterComponent.setFireRateMultiplier(this.clampFireRate(powerState.fireRateMultiplier));
        vampireShooterComponent.setPickupRangeBonus(this.clampPickupRange(powerState.pickupRangeBonus));
        vampireShooterComponent.setBounceBonus(this.clampBounceBonus(powerState.bounceBonus));
    }

    public void resetShooter(@Nonnull VampireShooterComponent vampireShooterComponent) {
        vampireShooterComponent.setProjectileCount(0);
        vampireShooterComponent.setFireRateMultiplier(1.0f);
        vampireShooterComponent.setPickupRangeBonus(0.0f);
        vampireShooterComponent.setBounceBonus(0);
    }

    public void applyPlayerBonuses(@Nonnull PowerState powerState, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PlayerRef playerRef) {
        this.applyPlayerHealthBonus(ref, store, powerState.healthRank);
        this.applyPlayerSpeedBonus(ref, store, playerRef, powerState.speedRank);
    }

    private void applyPlayerHealthBonus(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, int n) {
        EntityStatMap entityStatMap = (EntityStatMap)store.getComponent(ref, EntityStatMap.getComponentType());
        if (entityStatMap == null) {
            return;
        }
        float f = (float)this.clampHealthRank(n) * this.healthBonusPerUpgrade;
        if (f <= 0.0f) {
            entityStatMap.removeModifier(DefaultEntityStatTypes.getHealth(), PLAYER_HEALTH_BONUS_MODIFIER_ID);
            return;
        }
        entityStatMap.putModifier(DefaultEntityStatTypes.getHealth(), PLAYER_HEALTH_BONUS_MODIFIER_ID, (Modifier)new StaticModifier(Modifier.ModifierTarget.MAX, StaticModifier.CalculationType.ADDITIVE, f));
        entityStatMap.maximizeStatValue(DefaultEntityStatTypes.getHealth());
    }

    private void applyPlayerSpeedBonus(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PlayerRef playerRef, int n) {
        MovementManager movementManager = (MovementManager)store.getComponent(ref, MovementManager.getComponentType());
        float f = this.getSpeedMultiplierForRank(n);
        if (movementManager != null) {
            movementManager.resetDefaultsAndUpdate(ref, store);
            MovementSettings movementSettings = movementManager.getDefaultSettings();
            if (movementSettings != null) {
                movementSettings.baseSpeed = this.playerBaseSpeed;
                PowerUpApplicator.applySpeedMultiplier(movementSettings, f);
                movementManager.applyDefaultSettings();
                movementManager.update(playerRef.getPacketHandler());
            }
        }
        this.applyPlayerSpeedStaminaBonus(ref, store, n);
    }

    private void applyPlayerSpeedStaminaBonus(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, int n) {
        EntityStatMap entityStatMap = (EntityStatMap)store.getComponent(ref, EntityStatMap.getComponentType());
        if (entityStatMap == null) {
            return;
        }
        float f = (float)this.clampSpeedRank(n) * this.staminaBonusPerSpeedUpgrade;
        if (f <= 0.0f) {
            entityStatMap.removeModifier(DefaultEntityStatTypes.getStamina(), PLAYER_SPEED_STAMINA_BONUS_MODIFIER_ID);
            return;
        }
        entityStatMap.putModifier(DefaultEntityStatTypes.getStamina(), PLAYER_SPEED_STAMINA_BONUS_MODIFIER_ID, (Modifier)new StaticModifier(Modifier.ModifierTarget.MAX, StaticModifier.CalculationType.ADDITIVE, f));
        entityStatMap.maximizeStatValue(DefaultEntityStatTypes.getStamina());
    }

    private static void applySpeedMultiplier(@Nonnull MovementSettings movementSettings, float f) {
        movementSettings.baseSpeed = Math.max(0.01f, movementSettings.baseSpeed * f);
        movementSettings.climbSpeed = Math.max(0.01f, movementSettings.climbSpeed * f);
        movementSettings.climbSpeedLateral = Math.max(0.01f, movementSettings.climbSpeedLateral * f);
        movementSettings.climbUpSprintSpeed = Math.max(0.01f, movementSettings.climbUpSprintSpeed * f);
        movementSettings.climbDownSprintSpeed = Math.max(0.01f, movementSettings.climbDownSprintSpeed * f);
        movementSettings.horizontalFlySpeed = Math.max(0.01f, movementSettings.horizontalFlySpeed * f);
        movementSettings.verticalFlySpeed = Math.max(0.01f, movementSettings.verticalFlySpeed * f);
    }

    public int getChoiceRank(@Nonnull String string, @Nonnull PowerState powerState) {
        if ("extra_projectile".equals(string)) {
            return this.clampProjectileCount(powerState.projectileCount);
        }
        if ("fire_rate".equals(string)) {
            return this.computeFireRateRank(powerState.fireRateMultiplier);
        }
        if ("pickup_range".equals(string)) {
            return this.computePickupRangeRank(powerState.pickupRangeBonus);
        }
        if ("bounce".equals(string)) {
            return this.clampBounceBonus(powerState.bounceBonus);
        }
        if ("weapon_damage".equals(string)) {
            return this.clampWeaponDamageRank(powerState.weaponDamageRank);
        }
        if ("max_health".equals(string)) {
            return this.clampHealthRank(powerState.healthRank);
        }
        if ("move_speed".equals(string)) {
            return this.clampSpeedRank(powerState.speedRank);
        }
        if ("lucky".equals(string)) {
            return this.clampLuckyRank(powerState.luckyRank);
        }
        if (SKIP_CHOICE.equals(string)) {
            return Math.max(0, powerState.skipRank);
        }
        if ("projectile_rain".equals(string)) {
            return powerState.projectileRainUsed ? 1 : 0;
        }
        return 0;
    }

    public int clampProjectileCount(int n) {
        return Math.max(0, Math.min(this.maxExtraProjectiles, n));
    }

    public int clampBounceBonus(int n) {
        return Math.max(0, Math.min(this.maxBounceUpgrades, n));
    }

    public int clampWeaponDamageRank(int n) {
        return Math.max(0, Math.min(this.maxWeaponDamageUpgrades, n));
    }

    public int clampHealthRank(int n) {
        return Math.max(0, Math.min(this.maxHealthUpgrades, n));
    }

    public int clampSpeedRank(int n) {
        return Math.max(0, Math.min(this.maxSpeedUpgrades, n));
    }

    public int clampLuckyRank(int n) {
        return Math.max(0, Math.min(this.maxLuckyUpgrades, n));
    }

    public float clampFireRate(float f) {
        if (!Float.isFinite(f) || f <= 0.0f) {
            return 1.0f;
        }
        return Math.min(this.maxFireRateMultiplier, Math.max(0.1f, f));
    }

    public float clampPickupRange(float f) {
        if (!Float.isFinite(f) || f < 0.0f) {
            return 0.0f;
        }
        return Math.min(this.maxPickupRangeBonus, f);
    }

    public int computeFireRateRank(float f) {
        if (!Float.isFinite(f) || f <= 1.0f) {
            return 0;
        }
        float f2 = 1.0f;
        int n = 0;
        for (int i = 1; i <= this.maxFireRateUpgrades; ++i) {
            if (!(f >= (f2 *= this.fireRateMultiplierPerUpgrade) - 1.0E-4f)) continue;
            n = i;
        }
        return n;
    }

    public int computePickupRangeRank(float f) {
        if (!Float.isFinite(f) || f <= 0.0f || this.pickupRangePerUpgrade <= 0.0f) {
            return 0;
        }
        int n = (int)Math.floor(f / this.pickupRangePerUpgrade + 1.0E-4f);
        if (n < 0) {
            return 0;
        }
        return Math.min(this.maxPickupRangeUpgrades, n);
    }

    public float getWeaponDamageBonusForRank(int n) {
        return (float)this.clampWeaponDamageRank(n) * this.weaponDamageBonusPerUpgrade;
    }

    public float getSpeedMultiplierForRank(int n) {
        int n2 = this.clampSpeedRank(n);
        return Math.min(this.maxSpeedMultiplier, 1.0f + (float)n2 * this.speedBonusPerUpgrade);
    }

    public int getLuckyEssenceBonus(@Nonnull PowerState powerState) {
        return this.clampLuckyRank(powerState.luckyRank) * this.luckyEssenceBonusPerRank;
    }

    public int getProjectileRainBurstsOnPick() {
        return this.projectileRainBurstsOnPick;
    }

    public int getMaxFireRateUpgrades() {
        return this.maxFireRateUpgrades;
    }

    public int getMaxPickupRangeUpgrades() {
        return this.maxPickupRangeUpgrades;
    }

    public int getMaxExtraProjectileUpgrades() {
        return this.maxExtraProjectiles;
    }

    public int getMaxBounceUpgrades() {
        return this.maxBounceUpgrades;
    }

    public int getMaxWeaponDamageUpgrades() {
        return this.maxWeaponDamageUpgrades;
    }

    public int getMaxHealthUpgrades() {
        return this.maxHealthUpgrades;
    }

    public int getMaxSpeedUpgrades() {
        return this.maxSpeedUpgrades;
    }

    public int getMaxProjectileRainUpgrades() {
        return this.maxProjectileRainUpgrades;
    }

    public int getMaxLuckyUpgrades() {
        return this.maxLuckyUpgrades;
    }

    public static final class PowerState {
        int projectileCount;
        float fireRateMultiplier = 1.0f;
        float pickupRangeBonus = 0.0f;
        int bounceBonus;
        int weaponDamageRank;
        int healthRank;
        int speedRank;
        int luckyRank;
        int skipRank;
        boolean projectileRainUsed;

        public int projectileCount() {
            return this.projectileCount;
        }

        public float fireRateMultiplier() {
            return this.fireRateMultiplier;
        }

        public float pickupRangeBonus() {
            return this.pickupRangeBonus;
        }

        public int bounceBonus() {
            return this.bounceBonus;
        }

        public int weaponDamageRank() {
            return this.weaponDamageRank;
        }

        public int healthRank() {
            return this.healthRank;
        }

        public int speedRank() {
            return this.speedRank;
        }

        public int luckyRank() {
            return this.luckyRank;
        }

        public int skipRank() {
            return this.skipRank;
        }

        public boolean projectileRainUsed() {
            return this.projectileRainUsed;
        }
    }
}

