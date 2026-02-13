/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nonnull
 *  javax.annotation.Nullable
 */
package com.fadingtime.hytalemod.system;

import com.fadingtime.hytalemod.persistence.PlayerStateStore;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class PowerUpManager {
    private final int maxExtraProjectiles;
    private final int maxBounceUpgrades;
    private final int maxWeaponDamageUpgrades;
    private final int maxHealthUpgrades;
    private final int maxSpeedUpgrades;
    private final int maxLuckyUpgrades;
    private final float fireRateMultiplierPerUpgrade;
    private final int maxFireRateUpgrades;
    private final float maxFireRateMultiplier;
    private final float pickupRangePerUpgrade;
    private final int maxPickupRangeUpgrades;
    private final float maxPickupRangeBonus;
    private final ConcurrentMap<UUID, PowerState> powerByPlayer = new ConcurrentHashMap<UUID, PowerState>();

    public PowerUpManager(int n, int n2, int n3, int n4, int n5, int n6, float f, int n7, float f2, int n8, float f3) {
        this.maxExtraProjectiles = Math.max(0, n);
        this.maxBounceUpgrades = Math.max(0, n2);
        this.maxWeaponDamageUpgrades = Math.max(0, n3);
        this.maxHealthUpgrades = Math.max(0, n4);
        this.maxSpeedUpgrades = Math.max(0, n5);
        this.maxLuckyUpgrades = Math.max(0, n6);
        this.fireRateMultiplierPerUpgrade = f <= 0.0f ? 1.15f : f;
        this.maxFireRateUpgrades = Math.max(0, n7);
        this.maxFireRateMultiplier = (float)Math.pow(this.fireRateMultiplierPerUpgrade, this.maxFireRateUpgrades);
        this.pickupRangePerUpgrade = f2 <= 0.0f ? 10.0f : f2;
        this.maxPickupRangeUpgrades = Math.max(0, n8);
        this.maxPickupRangeBonus = Math.max(0.0f, f3);
    }

    @Nonnull
    public PowerState getOrCreate(@Nonnull UUID uUID2) {
        return this.powerByPlayer.computeIfAbsent(uUID2, uUID -> new PowerState());
    }

    @Nullable
    public PowerState get(@Nonnull UUID uUID) {
        return (PowerState)this.powerByPlayer.get(uUID);
    }

    public void put(@Nonnull UUID uUID, @Nonnull PowerState powerState) {
        this.powerByPlayer.put(uUID, powerState);
    }

    public void remove(@Nonnull UUID uUID) {
        this.powerByPlayer.remove(uUID);
    }

    public void clear() {
        this.powerByPlayer.clear();
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
        return "projectile_rain".equals(string);
    }

    @Nonnull
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
        powerState.projectileRainUsed = playerPowerData.projectileRainUsed;
        return powerState;
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
        return !Float.isFinite(f) || f <= 0.0f ? 1.0f : Math.min(this.maxFireRateMultiplier, Math.max(0.1f, f));
    }

    public float clampPickupRange(float f) {
        return !Float.isFinite(f) || f < 0.0f ? 0.0f : Math.min(this.maxPickupRangeBonus, f);
    }

    @Deprecated
    public boolean applyLegacyUpgrade(@Nonnull PowerState powerState, @Nonnull String string) {
        return this.applyChoice(powerState, string);
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

        public boolean projectileRainUsed() {
            return this.projectileRainUsed;
        }
    }
}
