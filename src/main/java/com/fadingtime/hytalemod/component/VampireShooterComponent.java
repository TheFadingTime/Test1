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

    public void setMobSpawnTimer(float mobSpawnTimer) {
        this.mobSpawnTimer = mobSpawnTimer;
    }

    public float getNextSpawnDelay() {
        return this.nextSpawnDelay;
    }

    public void setNextSpawnDelay(float nextSpawnDelay) {
        this.nextSpawnDelay = nextSpawnDelay;
    }

    public List<Ref<EntityStore>> getSpawnedMobs() {
        return this.spawnedMobs;
    }

    public int getProjectileCount() {
        return this.projectileCount;
    }

    public void setProjectileCount(int projectileCount) {
        this.projectileCount = Math.max(0, projectileCount);
    }

    public void addProjectiles(int amount) {
        if (amount <= 0) {
            return;
        }
        this.projectileCount = Math.max(0, this.projectileCount + amount);
    }

    public float getFireRateMultiplier() {
        return this.fireRateMultiplier;
    }

    public void setFireRateMultiplier(float fireRateMultiplier) {
        if (!Float.isFinite(fireRateMultiplier) || fireRateMultiplier <= 0.0f) {
            return;
        }
        this.fireRateMultiplier = Math.max(0.1f, fireRateMultiplier);
    }

    public void addFireRatePercent(float percent) {
        if (!Float.isFinite(percent) || percent <= 0.0f) {
            return;
        }
        this.fireRateMultiplier = Math.max(0.1f, this.fireRateMultiplier * (1.0f + percent / 100.0f));
    }

    public float getPickupRangeBonus() {
        return this.pickupRangeBonus;
    }

    public void setPickupRangeBonus(float amount) {
        if (!Float.isFinite(amount) || amount < 0.0f) {
            return;
        }
        this.pickupRangeBonus = amount;
    }

    public void addPickupRangeBonus(float amount) {
        if (!Float.isFinite(amount) || amount <= 0.0f) {
            return;
        }
        this.pickupRangeBonus += amount;
    }

    public int getBounceBonus() {
        return this.bounceBonus;
    }

    public void setBounceBonus(int bounceBonus) {
        this.bounceBonus = Math.max(0, bounceBonus);
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

    public void setProjectileRainCooldown(float projectileRainCooldown) {
        if (!Float.isFinite(projectileRainCooldown) || projectileRainCooldown <= 0.0f) {
            this.projectileRainCooldown = 0.0f;
            return;
        }
        this.projectileRainCooldown = projectileRainCooldown;
    }

    public void tickProjectileRainCooldown(float dt) {
        if (!Float.isFinite(dt) || dt <= 0.0f || this.projectileRainCooldown <= 0.0f) {
            return;
        }
        this.projectileRainCooldown = Math.max(0.0f, this.projectileRainCooldown - dt);
    }

    public void setTimer(float timer) {
        this.timer = timer;
    }

    public void incrementTimer(float dt) {
        this.timer += dt;
    }

    public void resetTimer() {
        this.timer = 0.0f;
    }

    @Nonnull
    public Component<EntityStore> clone() {
        VampireShooterComponent copy = new VampireShooterComponent();
        copy.timer = this.timer;
        copy.mobSpawnTimer = this.mobSpawnTimer;
        copy.nextSpawnDelay = this.nextSpawnDelay;
        copy.spawnedMobs.addAll(this.spawnedMobs);
        copy.projectileCount = this.projectileCount;
        copy.fireRateMultiplier = this.fireRateMultiplier;
        copy.pickupRangeBonus = this.pickupRangeBonus;
        copy.bounceBonus = this.bounceBonus;
        copy.projectileRainCharges = this.projectileRainCharges;
        copy.projectileRainCooldown = this.projectileRainCooldown;
        return copy;
    }
}
