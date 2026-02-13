/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.AddReason
 *  com.hypixel.hytale.component.CommandBuffer
 *  com.hypixel.hytale.component.Component
 *  com.hypixel.hytale.component.ComponentType
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.RemoveReason
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.component.query.Query
 *  com.hypixel.hytale.component.system.RefSystem
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 *  javax.annotation.Nonnull
 */
package com.fadingtime.hytalemod.system;

import com.fadingtime.hytalemod.HytaleMod;
import com.fadingtime.hytalemod.component.VampireShooterComponent;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

public class VampireShooterAdder
extends RefSystem<EntityStore> {
    @Nonnull
    private final ComponentType<EntityStore, VampireShooterComponent> vampireShooterComponentType;
    @Nonnull
    private final Query<EntityStore> query;

    public VampireShooterAdder(@Nonnull ComponentType<EntityStore, VampireShooterComponent> componentType) {
        this.vampireShooterComponentType = componentType;
        this.query = Query.and((Query[])new Query[]{Player.getComponentType()});
    }

    @Nonnull
    public Query<EntityStore> getQuery() {
        return this.query;
    }

    public void onEntityAdded(@Nonnull Ref<EntityStore> ref, @Nonnull AddReason addReason, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        Player player = (Player)commandBuffer.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        if (commandBuffer.getComponent(ref, this.vampireShooterComponentType) != null) {
            return;
        }
        VampireShooterComponent vampireShooterComponent = new VampireShooterComponent();
        PlayerRef playerRef = (PlayerRef)store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            playerRef = (PlayerRef)commandBuffer.getComponent(ref, PlayerRef.getComponentType());
        }
        if (playerRef != null) {
            HytaleMod.getInstance().getLifeEssenceLevelSystem().restorePowerState(playerRef.getUuid(), vampireShooterComponent);
        }
        commandBuffer.addComponent(ref, this.vampireShooterComponentType, vampireShooterComponent);
    }

    public void onEntityRemove(@Nonnull Ref<EntityStore> ref, @Nonnull RemoveReason removeReason, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
    }
}
