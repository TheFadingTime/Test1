package com.fadingtime.hytalemod;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class SpawnedByMobWaveComponent implements Component<EntityStore> {
    @Override
    public Component<EntityStore> clone() {
        return new SpawnedByMobWaveComponent();
    }
}
