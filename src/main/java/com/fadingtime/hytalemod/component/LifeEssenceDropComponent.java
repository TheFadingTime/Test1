package com.fadingtime.hytalemod.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

public class LifeEssenceDropComponent
implements Component<EntityStore> {
    private final long spawnTimeMs;

    public LifeEssenceDropComponent() {
        this(System.currentTimeMillis());
    }

    public LifeEssenceDropComponent(long spawnTimeMs) {
        this.spawnTimeMs = spawnTimeMs;
    }

    public long getSpawnTimeMs() {
        return this.spawnTimeMs;
    }

    @Nonnull
    public Component<EntityStore> clone() {
        return new LifeEssenceDropComponent(this.spawnTimeMs);
    }
}

