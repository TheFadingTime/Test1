/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Component
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 */
package com.fadingtime.hytalemod.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class SpawnedByMobWaveComponent
implements Component<EntityStore> {
    public Component<EntityStore> clone() {
        return new SpawnedByMobWaveComponent();
    }
}

