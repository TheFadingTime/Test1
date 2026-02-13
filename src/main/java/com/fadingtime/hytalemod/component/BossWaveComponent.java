/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Component
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 *  javax.annotation.Nonnull
 *  javax.annotation.Nullable
 */
package com.fadingtime.hytalemod.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BossWaveComponent
implements Component<EntityStore> {
    @Nullable
    private UUID ownerId;

    public BossWaveComponent() {
    }

    public BossWaveComponent(@Nonnull UUID uUID) {
        this.ownerId = uUID;
    }

    @Nullable
    public UUID getOwnerId() {
        return this.ownerId;
    }

    public void setOwnerId(@Nonnull UUID uUID) {
        this.ownerId = uUID;
    }

    @Nonnull
    public Component<EntityStore> clone() {
        return this.ownerId == null ? new BossWaveComponent() : new BossWaveComponent(this.ownerId);
    }
}

