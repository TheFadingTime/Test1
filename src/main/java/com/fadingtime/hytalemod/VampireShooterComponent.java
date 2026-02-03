package com.fadingtime.hytalemod;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

/**
 * Component that stores the fireball timer for each player.
 * This is attached to player entities to track when to fire the next volley of fireballs.
 */
public class VampireShooterComponent implements Component<EntityStore> {
    
    private float timer = 0.0F;
    private float mobSpawnTimer = 0.0F;
    private float nextSpawnDelay = 2.5F;
    private final java.util.List<Ref<EntityStore>> spawnedMobs = new java.util.ArrayList<>();
    
    public VampireShooterComponent() {
    }
    
    /**
     * Gets the current timer value in seconds.
     * @return The timer value
     */
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

    public java.util.List<Ref<EntityStore>> getSpawnedMobs() {
        return this.spawnedMobs;
    }
    
    /**
     * Sets the timer value.
     * @param timer The new timer value in seconds
     */
    public void setTimer(float timer) {
        this.timer = timer;
    }
    
    /**
     * Increments the timer by the given delta time.
     * @param dt The delta time in seconds
     */
    public void incrementTimer(float dt) {
        this.timer += dt;
    }
    
    /**
     * Resets the timer to zero.
     */
    public void resetTimer() {
        this.timer = 0.0F;
    }
    
    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        VampireShooterComponent copy = new VampireShooterComponent();
        copy.timer = this.timer;
        copy.mobSpawnTimer = this.mobSpawnTimer;
        copy.nextSpawnDelay = this.nextSpawnDelay;
        copy.spawnedMobs.addAll(this.spawnedMobs);
        return copy;
    }
}
