package com.fadingtime.hytalemod.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

public class ProjectileBounceComponent
implements Component<EntityStore> {
    private int remainingBounces;
    private float speedMultiplier;

    public ProjectileBounceComponent() {
        this(1, 0.85f);
    }

    public ProjectileBounceComponent(int remainingBounces, float speedMultiplier) {
        this.remainingBounces = Math.max(0, remainingBounces);
        this.speedMultiplier = Float.isFinite(speedMultiplier) ? Math.max(0.05f, speedMultiplier) : 0.85f;
    }

    public int getRemainingBounces() {
        return this.remainingBounces;
    }

    public void setRemainingBounces(int remainingBounces) {
        this.remainingBounces = Math.max(0, remainingBounces);
    }

    public float getSpeedMultiplier() {
        return this.speedMultiplier;
    }

    public void setSpeedMultiplier(float speedMultiplier) {
        if (!Float.isFinite(speedMultiplier) || speedMultiplier <= 0.0f) {
            return;
        }
        this.speedMultiplier = Math.max(0.05f, speedMultiplier);
    }

    @Nonnull
    public Component<EntityStore> clone() {
        return new ProjectileBounceComponent(this.remainingBounces, this.speedMultiplier);
    }
}

