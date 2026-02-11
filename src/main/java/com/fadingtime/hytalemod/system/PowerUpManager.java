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
    // Cache to avoid repeated map lookups on hot combat paths.
    private final ConcurrentMap<UUID, PowerState> powerByPlayer = new ConcurrentHashMap<>();

    public PowerUpManager(
        int maxExtraProjectiles,
        int maxBounceUpgrades,
        int maxWeaponDamageUpgrades,
        int maxHealthUpgrades,
        int maxSpeedUpgrades,
        int maxLuckyUpgrades,
        float fireRateMultiplierPerUpgrade,
        int maxFireRateUpgrades,
        float pickupRangePerUpgrade,
        int maxPickupRangeUpgrades,
        float maxPickupRangeBonus
    ) {
        this.maxExtraProjectiles = Math.max(0, maxExtraProjectiles);
        this.maxBounceUpgrades = Math.max(0, maxBounceUpgrades);
        this.maxWeaponDamageUpgrades = Math.max(0, maxWeaponDamageUpgrades);
        this.maxHealthUpgrades = Math.max(0, maxHealthUpgrades);
        this.maxSpeedUpgrades = Math.max(0, maxSpeedUpgrades);
        this.maxLuckyUpgrades = Math.max(0, maxLuckyUpgrades);
        this.fireRateMultiplierPerUpgrade = fireRateMultiplierPerUpgrade <= 0.0f ? 1.15f : fireRateMultiplierPerUpgrade;
        this.maxFireRateUpgrades = Math.max(0, maxFireRateUpgrades);
        this.maxFireRateMultiplier = (float)Math.pow(this.fireRateMultiplierPerUpgrade, this.maxFireRateUpgrades);
        this.pickupRangePerUpgrade = pickupRangePerUpgrade <= 0.0f ? 10.0f : pickupRangePerUpgrade;
        this.maxPickupRangeUpgrades = Math.max(0, maxPickupRangeUpgrades);
        this.maxPickupRangeBonus = Math.max(0.0f, maxPickupRangeBonus);
    }

    @Nonnull
    public PowerState getOrCreate(@Nonnull UUID playerId) {
        return this.powerByPlayer.computeIfAbsent(playerId, id -> new PowerState());
    }

    @Nullable
    public PowerState get(@Nonnull UUID playerId) {
        return this.powerByPlayer.get(playerId);
    }

    public void put(@Nonnull UUID playerId, @Nonnull PowerState state) {
        this.powerByPlayer.put(playerId, state);
    }

    public void remove(@Nonnull UUID playerId) {
        this.powerByPlayer.remove(playerId);
    }

    public void clear() {
        this.powerByPlayer.clear();
    }

    public boolean applyChoice(@Nonnull PowerState state, @Nonnull String choice) {
        if ("extra_projectile".equals(choice)) {
            state.projectileCount = this.clampProjectileCount(state.projectileCount + 1);
            return false;
        }
        if ("fire_rate".equals(choice)) {
            state.fireRateMultiplier = this.clampFireRate(state.fireRateMultiplier * this.fireRateMultiplierPerUpgrade);
            return false;
        }
        if ("pickup_range".equals(choice)) {
            state.pickupRangeBonus = this.clampPickupRange(state.pickupRangeBonus + this.pickupRangePerUpgrade);
            return false;
        }
        if ("bounce".equals(choice)) {
            state.bounceBonus = this.clampBounceBonus(state.bounceBonus + 1);
            return false;
        }
        if ("weapon_damage".equals(choice)) {
            state.weaponDamageRank = this.clampWeaponDamageRank(state.weaponDamageRank + 1);
            return false;
        }
        if ("max_health".equals(choice)) {
            state.healthRank = this.clampHealthRank(state.healthRank + 1);
            return false;
        }
        if ("move_speed".equals(choice)) {
            state.speedRank = this.clampSpeedRank(state.speedRank + 1);
            return false;
        }
        if ("lucky".equals(choice)) {
            state.luckyRank = this.clampLuckyRank(state.luckyRank + 1);
            return false;
        }
        return "projectile_rain".equals(choice);
    }

    @Nonnull
    public PowerState fromPersisted(@Nonnull PlayerStateStore.PlayerPowerData data) {
        PowerState state = new PowerState();
        state.projectileCount = this.clampProjectileCount(data.projectileCount);
        state.fireRateMultiplier = this.clampFireRate(data.fireRateMultiplier);
        state.pickupRangeBonus = this.clampPickupRange(data.pickupRangeBonus);
        state.bounceBonus = this.clampBounceBonus(data.bounceBonus);
        state.weaponDamageRank = this.clampWeaponDamageRank(data.weaponDamageRank);
        state.healthRank = this.clampHealthRank(data.healthRank);
        state.speedRank = this.clampSpeedRank(data.speedRank);
        state.luckyRank = this.clampLuckyRank(data.luckyRank);
        state.projectileRainUsed = data.projectileRainUsed;
        return state;
    }

    public int clampProjectileCount(int count) {
        return Math.max(0, Math.min(this.maxExtraProjectiles, count));
    }

    public int clampBounceBonus(int bonus) {
        return Math.max(0, Math.min(this.maxBounceUpgrades, bonus));
    }

    public int clampWeaponDamageRank(int rank) {
        return Math.max(0, Math.min(this.maxWeaponDamageUpgrades, rank));
    }

    public int clampHealthRank(int rank) {
        return Math.max(0, Math.min(this.maxHealthUpgrades, rank));
    }

    public int clampSpeedRank(int rank) {
        return Math.max(0, Math.min(this.maxSpeedUpgrades, rank));
    }

    public int clampLuckyRank(int rank) {
        return Math.max(0, Math.min(this.maxLuckyUpgrades, rank));
    }

    public float clampFireRate(float multiplier) {
        if (!Float.isFinite(multiplier) || multiplier <= 0.0f) {
            return 1.0f;
        }
        return Math.min(this.maxFireRateMultiplier, Math.max(0.1f, multiplier));
    }

    public float clampPickupRange(float bonus) {
        if (!Float.isFinite(bonus) || bonus < 0.0f) {
            return 0.0f;
        }
        return Math.min(this.maxPickupRangeBonus, bonus);
    }

    @Deprecated
    public boolean applyLegacyUpgrade(@Nonnull PowerState state, @Nonnull String choice) {
        // Old compatibility shim for command/tests that still call the legacy path.
        return this.applyChoice(state, choice);
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
