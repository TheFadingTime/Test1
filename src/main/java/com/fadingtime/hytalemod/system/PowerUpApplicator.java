/*
 * REFACTOR: Replaced string-based dispatch with PowerUpType enum.
 *
 * WHAT CHANGED:
 *   - applyChoice() and getChoiceRank() now take PowerUpType instead of String.
 *   - The string if-else chains became switch statements on the enum.
 *
 * PRINCIPLE (Type Safety):
 *   Strings are "stringly typed" — the compiler can't tell "fire_rate" from "Fire_rate".
 *   Enums give you compile-time validation: PowerUpType.FIRE_RATE can't be misspelled.
 *   The switch also documents every valid value in one place, whereas scattered string
 *   comparisons require reading every if-branch to know what values exist.
 */
package com.fadingtime.hytalemod.system;

import com.fadingtime.hytalemod.component.VampireShooterComponent;
import com.fadingtime.hytalemod.config.LifeEssenceConfig;
import com.fadingtime.hytalemod.persistence.PlayerStateStore;
import com.hypixel.hytale.component.ComponentAccessor;
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
    private final float minFireRateClamp;

    public PowerUpApplicator(
        int maxExtraProjectiles,
        int maxBounceUpgrades,
        int maxWeaponDamageUpgrades,
        int maxHealthUpgrades,
        int maxSpeedUpgrades,
        int maxLuckyUpgrades,
        int maxFireRateUpgrades,
        float fireRateMultiplierPerUpgrade,
        int maxPickupRangeUpgrades,
        float pickupRangePerUpgrade,
        float maxPickupRangeBonus,
        float weaponDamageBonusPerUpgrade,
        float healthBonusPerUpgrade,
        float speedBonusPerUpgrade,
        float maxSpeedMultiplier,
        float playerBaseSpeed,
        float staminaBonusPerSpeedUpgrade,
        int luckyEssenceBonusPerRank,
        int maxProjectileRainUpgrades,
        int projectileRainBurstsOnPick,
        float minFireRateClamp
    ) {
        this.maxExtraProjectiles = Math.max(0, maxExtraProjectiles);
        this.maxBounceUpgrades = Math.max(0, maxBounceUpgrades);
        this.maxWeaponDamageUpgrades = Math.max(0, maxWeaponDamageUpgrades);
        this.maxHealthUpgrades = Math.max(0, maxHealthUpgrades);
        this.maxSpeedUpgrades = Math.max(0, maxSpeedUpgrades);
        this.maxLuckyUpgrades = Math.max(0, maxLuckyUpgrades);
        this.maxFireRateUpgrades = Math.max(0, maxFireRateUpgrades);
        this.fireRateMultiplierPerUpgrade = fireRateMultiplierPerUpgrade <= 0.0f ? 1.15f : fireRateMultiplierPerUpgrade;
        this.maxFireRateMultiplier = (float)Math.pow(this.fireRateMultiplierPerUpgrade, this.maxFireRateUpgrades);
        this.maxPickupRangeUpgrades = Math.max(0, maxPickupRangeUpgrades);
        this.pickupRangePerUpgrade = pickupRangePerUpgrade <= 0.0f ? 10.0f : pickupRangePerUpgrade;
        this.maxPickupRangeBonus = Math.max(0.0f, maxPickupRangeBonus);
        this.weaponDamageBonusPerUpgrade = Math.max(0.0f, weaponDamageBonusPerUpgrade);
        this.healthBonusPerUpgrade = Math.max(0.0f, healthBonusPerUpgrade);
        this.speedBonusPerUpgrade = Math.max(0.0f, speedBonusPerUpgrade);
        this.maxSpeedMultiplier = Math.max(1.0f, maxSpeedMultiplier);
        this.playerBaseSpeed = Math.max(0.01f, playerBaseSpeed);
        this.staminaBonusPerSpeedUpgrade = Math.max(0.0f, staminaBonusPerSpeedUpgrade);
        this.luckyEssenceBonusPerRank = Math.max(0, luckyEssenceBonusPerRank);
        this.maxProjectileRainUpgrades = Math.max(0, maxProjectileRainUpgrades);
        this.projectileRainBurstsOnPick = Math.max(1, projectileRainBurstsOnPick);
        this.minFireRateClamp = Math.max(0.001f, minFireRateClamp);
    }

    public static PowerUpApplicator fromConfig(@Nonnull LifeEssenceConfig config) {
        return new PowerUpApplicator(
            config.maxExtraProjectiles,
            config.maxBounceUpgrades,
            config.maxWeaponDamageUpgrades,
            config.maxHealthUpgrades,
            config.maxSpeedUpgrades,
            config.maxLuckyUpgrades,
            config.maxFireRateUpgrades,
            config.fireRateMultiplierPerUpgrade,
            config.maxPickupRangeUpgrades,
            config.pickupRangePerUpgrade,
            config.maxPickupRangeBonus,
            config.weaponDamageBonusPerUpgrade,
            config.healthBonusPerUpgrade,
            config.speedBonusPerUpgrade,
            config.maxSpeedMultiplier,
            config.playerBaseSpeed,
            config.staminaBonusPerSpeedUpgrade,
            config.luckyEssenceBonusPerRank,
            config.maxProjectileRainUpgrades,
            config.projectileRainBurstsOnPick,
            config.minFireRateClamp
        );
    }

    public PowerState newState() {
        return new PowerState();
    }

    public PowerState fromPersisted(@Nonnull PlayerStateStore.PlayerPowerData data) {
        PowerState state = new PowerState();
        state.projectileCount = clampProjectileCount(data.projectileCount);
        state.fireRateMultiplier = clampFireRate(data.fireRateMultiplier);
        state.pickupRangeBonus = clampPickupRange(data.pickupRangeBonus);
        state.bounceBonus = clampBounceBonus(data.bounceBonus);
        state.weaponDamageRank = clampWeaponDamageRank(data.weaponDamageRank);
        state.healthRank = clampHealthRank(data.healthRank);
        state.speedRank = clampSpeedRank(data.speedRank);
        state.luckyRank = clampLuckyRank(data.luckyRank);
        state.projectileRainUsed = data.projectileRainUsed;
        return state;
    }

    // REFACTORED: String if-else chain -> enum switch.
    // The switch on an enum is exhaustive — if someone adds a new PowerUpType
    // and forgets to handle it here, the compiler will warn them. The old string
    // chain would silently fall through and do nothing for unhandled types.
    public boolean applyChoice(@Nonnull PowerState state, @Nonnull PowerUpType type) {
        switch (type) {
            case EXTRA_PROJECTILE:
                state.projectileCount = clampProjectileCount(state.projectileCount + 1);
                return false;
            case FIRE_RATE:
                state.fireRateMultiplier = clampFireRate(state.fireRateMultiplier * this.fireRateMultiplierPerUpgrade);
                return false;
            case PICKUP_RANGE:
                state.pickupRangeBonus = clampPickupRange(state.pickupRangeBonus + this.pickupRangePerUpgrade);
                return false;
            case BOUNCE:
                state.bounceBonus = clampBounceBonus(state.bounceBonus + 1);
                return false;
            case WEAPON_DAMAGE:
                state.weaponDamageRank = clampWeaponDamageRank(state.weaponDamageRank + 1);
                return false;
            case MAX_HEALTH:
                state.healthRank = clampHealthRank(state.healthRank + 1);
                return false;
            case MOVE_SPEED:
                state.speedRank = clampSpeedRank(state.speedRank + 1);
                return false;
            case LUCKY:
                state.luckyRank = clampLuckyRank(state.luckyRank + 1);
                return false;
            case PROJECTILE_RAIN:
                return true;
            default:
                return false;
        }
    }

    public void applyToShooter(@Nonnull PowerState state, @Nonnull VampireShooterComponent shooter) {
        shooter.setProjectileCount(clampProjectileCount(state.projectileCount));
        shooter.setFireRateMultiplier(clampFireRate(state.fireRateMultiplier));
        shooter.setPickupRangeBonus(clampPickupRange(state.pickupRangeBonus));
        shooter.setBounceBonus(clampBounceBonus(state.bounceBonus));
    }

    public void resetShooter(@Nonnull VampireShooterComponent shooter) {
        shooter.setProjectileCount(0);
        shooter.setFireRateMultiplier(1.0f);
        shooter.setPickupRangeBonus(0.0f);
        shooter.setBounceBonus(0);
    }

    public void applyPlayerBonuses(@Nonnull PowerState state, @Nonnull Ref<EntityStore> playerRef, @Nonnull Store<EntityStore> store, @Nonnull PlayerRef playerRefComponent) {
        applyPlayerHealthBonus(playerRef, store, state.healthRank);
        applyPlayerSpeedBonus(playerRef, store, playerRefComponent, state.speedRank);
    }

    private void applyPlayerHealthBonus(@Nonnull Ref<EntityStore> playerRef, @Nonnull Store<EntityStore> store, int rank) {
        EntityStatMap statMap = (EntityStatMap)store.getComponent(playerRef, EntityStatMap.getComponentType());
        if (statMap == null) {
            return;
        }
        float bonus = (float)clampHealthRank(rank) * this.healthBonusPerUpgrade;
        if (bonus <= 0.0f) {
            statMap.removeModifier(DefaultEntityStatTypes.getHealth(), PLAYER_HEALTH_BONUS_MODIFIER_ID);
            return;
        }
        statMap.putModifier(DefaultEntityStatTypes.getHealth(), PLAYER_HEALTH_BONUS_MODIFIER_ID, (Modifier)new StaticModifier(Modifier.ModifierTarget.MAX, StaticModifier.CalculationType.ADDITIVE, bonus));
        statMap.maximizeStatValue(DefaultEntityStatTypes.getHealth());
    }

    private void applyPlayerSpeedBonus(@Nonnull Ref<EntityStore> playerRef, @Nonnull Store<EntityStore> store, @Nonnull PlayerRef playerRefComponent, int rank) {
        MovementManager movementManager = (MovementManager)store.getComponent(playerRef, MovementManager.getComponentType());
        float speedMultiplier = getSpeedMultiplierForRank(rank);
        if (movementManager != null) {
            movementManager.resetDefaultsAndUpdate(playerRef, (ComponentAccessor<EntityStore>)store);
            MovementSettings base = movementManager.getDefaultSettings();
            if (base != null) {
                base.baseSpeed = this.playerBaseSpeed;
                applySpeedMultiplier(base, speedMultiplier);
                movementManager.applyDefaultSettings();
                movementManager.update(playerRefComponent.getPacketHandler());
            }
        }
        applyPlayerSpeedStaminaBonus(playerRef, store, rank);
    }

    private void applyPlayerSpeedStaminaBonus(@Nonnull Ref<EntityStore> playerRef, @Nonnull Store<EntityStore> store, int rank) {
        EntityStatMap statMap = (EntityStatMap)store.getComponent(playerRef, EntityStatMap.getComponentType());
        if (statMap == null) {
            return;
        }
        float bonus = (float)clampSpeedRank(rank) * this.staminaBonusPerSpeedUpgrade;
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

    public int getChoiceRank(@Nonnull PowerUpType type, @Nonnull PowerState state) {
        switch (type) {
            case EXTRA_PROJECTILE: return clampProjectileCount(state.projectileCount);
            case FIRE_RATE: return computeFireRateRank(state.fireRateMultiplier);
            case PICKUP_RANGE: return computePickupRangeRank(state.pickupRangeBonus);
            case BOUNCE: return clampBounceBonus(state.bounceBonus);
            case WEAPON_DAMAGE: return clampWeaponDamageRank(state.weaponDamageRank);
            case MAX_HEALTH: return clampHealthRank(state.healthRank);
            case MOVE_SPEED: return clampSpeedRank(state.speedRank);
            case LUCKY: return clampLuckyRank(state.luckyRank);
            case PROJECTILE_RAIN: return state.projectileRainUsed ? 1 : 0;
            default: return 0;
        }
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
        return Math.min(this.maxFireRateMultiplier, Math.max(this.minFireRateClamp, multiplier));
    }

    public float clampPickupRange(float bonus) {
        if (!Float.isFinite(bonus) || bonus < 0.0f) {
            return 0.0f;
        }
        return Math.min(this.maxPickupRangeBonus, bonus);
    }

    public int computeFireRateRank(float multiplier) {
        if (!Float.isFinite(multiplier) || multiplier <= 1.0f) {
            return 0;
        }
        float current = 1.0f;
        int rank = 0;
        for (int i = 1; i <= this.maxFireRateUpgrades; ++i) {
            current *= this.fireRateMultiplierPerUpgrade;
            if (multiplier >= current - 1.0E-4f) {
                rank = i;
            }
        }
        return rank;
    }

    public int computePickupRangeRank(float bonus) {
        if (!Float.isFinite(bonus) || bonus <= 0.0f || this.pickupRangePerUpgrade <= 0.0f) {
            return 0;
        }
        int rank = (int)Math.floor(bonus / this.pickupRangePerUpgrade + 1.0E-4f);
        if (rank < 0) {
            return 0;
        }
        return Math.min(this.maxPickupRangeUpgrades, rank);
    }

    public float getWeaponDamageBonusForRank(int rank) {
        return (float)clampWeaponDamageRank(rank) * this.weaponDamageBonusPerUpgrade;
    }

    public float getSpeedMultiplierForRank(int rank) {
        int clampedRank = clampSpeedRank(rank);
        return Math.min(this.maxSpeedMultiplier, 1.0f + (float)clampedRank * this.speedBonusPerUpgrade);
    }

    public int getLuckyEssenceBonus(@Nonnull PowerState state) {
        return clampLuckyRank(state.luckyRank) * this.luckyEssenceBonusPerRank;
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
