/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Component
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 *  javax.annotation.Nonnull
 */
package com.fadingtime.hytalemod.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

public class VampireShooterComponent
implements Component<EntityStore> {
    private float timer = 0.0f;
    private float mobSpawnTimer = 0.0f;
    private float nextSpawnDelay = 2.5f;
    private final List<Ref<EntityStore>> spawnedMobs = new ArrayList<Ref<EntityStore>>();
    private int projectileCount;
    private float fireRateMultiplier = 1.0f;
    private float pickupRangeBonus = 0.0f;
    private int bounceBonus;
    private int projectileRainCharges;
    private float projectileRainCooldown;

    public float getTimer() {
        return this.timer;
    }

    public float getMobSpawnTimer() {
        return this.mobSpawnTimer;
    }

    public void setMobSpawnTimer(float f) {
        this.mobSpawnTimer = f;
    }

    public float getNextSpawnDelay() {
        return this.nextSpawnDelay;
    }

    public void setNextSpawnDelay(float f) {
        this.nextSpawnDelay = f;
    }

    public List<Ref<EntityStore>> getSpawnedMobs() {
        return this.spawnedMobs;
    }

    public int getProjectileCount() {
        return this.projectileCount;
    }

    public void setProjectileCount(int n) {
        this.projectileCount = Math.max(0, n);
    }

    public void addProjectiles(int n) {
        if (n <= 0) {
            return;
        }
        this.projectileCount = Math.max(0, this.projectileCount + n);
    }

    public float getFireRateMultiplier() {
        return this.fireRateMultiplier;
    }

    public void setFireRateMultiplier(float f) {
        if (!Float.isFinite(f) || f <= 0.0f) {
            return;
        }
        this.fireRateMultiplier = Math.max(0.1f, f);
    }

    public void addFireRatePercent(float f) {
        if (!Float.isFinite(f) || f <= 0.0f) {
            return;
        }
        this.fireRateMultiplier = Math.max(0.1f, this.fireRateMultiplier * (1.0f + f / 100.0f));
    }

    public float getPickupRangeBonus() {
        return this.pickupRangeBonus;
    }

    public void setPickupRangeBonus(float f) {
        if (!Float.isFinite(f) || f < 0.0f) {
            return;
        }
        this.pickupRangeBonus = f;
    }

    public void addPickupRangeBonus(float f) {
        if (!Float.isFinite(f) || f <= 0.0f) {
            return;
        }
        this.pickupRangeBonus += f;
    }

    public int getBounceBonus() {
        return this.bounceBonus;
    }

    public void setBounceBonus(int n) {
        this.bounceBonus = Math.max(0, n);
    }

    public void queueProjectileRain() {
        this.projectileRainCharges = Math.max(1, this.projectileRainCharges + 1);
    }

    public boolean consumeProjectileRain() {
        if (this.projectileRainCharges <= 0) {
            return false;
        }
        --this.projectileRainCharges;
        return true;
    }

    public float getProjectileRainCooldown() {
        return this.projectileRainCooldown;
    }

    public void setProjectileRainCooldown(float f) {
        if (!Float.isFinite(f) || f <= 0.0f) {
            this.projectileRainCooldown = 0.0f;
            return;
        }
        this.projectileRainCooldown = f;
    }

    public void tickProjectileRainCooldown(float f) {
        if (!Float.isFinite(f) || f <= 0.0f || this.projectileRainCooldown <= 0.0f) {
            return;
        }
        this.projectileRainCooldown = Math.max(0.0f, this.projectileRainCooldown - f);
    }

    public void setTimer(float f) {
        this.timer = f;
    }

    public void incrementTimer(float f) {
        this.timer += f;
    }

    public void resetTimer() {
        this.timer = 0.0f;
    }

    @Nonnull
    public Component<EntityStore> clone() {
        VampireShooterComponent vampireShooterComponent = new VampireShooterComponent();
        vampireShooterComponent.timer = this.timer;
        vampireShooterComponent.mobSpawnTimer = this.mobSpawnTimer;
        vampireShooterComponent.nextSpawnDelay = this.nextSpawnDelay;
        vampireShooterComponent.spawnedMobs.addAll(this.spawnedMobs);
        vampireShooterComponent.projectileCount = this.projectileCount;
        vampireShooterComponent.fireRateMultiplier = this.fireRateMultiplier;
        vampireShooterComponent.pickupRangeBonus = this.pickupRangeBonus;
        vampireShooterComponent.bounceBonus = this.bounceBonus;
        vampireShooterComponent.projectileRainCharges = this.projectileRainCharges;
        vampireShooterComponent.projectileRainCooldown = this.projectileRainCooldown;
        return vampireShooterComponent;
    }
}

