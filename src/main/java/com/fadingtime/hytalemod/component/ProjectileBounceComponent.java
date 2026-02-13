/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Component
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 *  javax.annotation.Nonnull
 */
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

    public ProjectileBounceComponent(int n, float f) {
        this.remainingBounces = Math.max(0, n);
        this.speedMultiplier = Float.isFinite(f) ? Math.max(0.05f, f) : 0.85f;
    }

    public int getRemainingBounces() {
        return this.remainingBounces;
    }

    public void setRemainingBounces(int n) {
        this.remainingBounces = Math.max(0, n);
    }

    public float getSpeedMultiplier() {
        return this.speedMultiplier;
    }

    public void setSpeedMultiplier(float f) {
        if (!Float.isFinite(f) || f <= 0.0f) {
            return;
        }
        this.speedMultiplier = Math.max(0.05f, f);
    }

    @Nonnull
    public Component<EntityStore> clone() {
        return new ProjectileBounceComponent(this.remainingBounces, this.speedMultiplier);
    }
}

