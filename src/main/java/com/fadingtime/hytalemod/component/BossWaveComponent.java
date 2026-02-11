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

    public BossWaveComponent(@Nonnull UUID ownerId) {
        this.ownerId = ownerId;
    }

    @Nullable
    public UUID getOwnerId() {
        return this.ownerId;
    }

    public void setOwnerId(@Nonnull UUID ownerId) {
        this.ownerId = ownerId;
    }

    @Nonnull
    public Component<EntityStore> clone() {
        return this.ownerId == null ? new BossWaveComponent() : new BossWaveComponent(this.ownerId);
    }
}

