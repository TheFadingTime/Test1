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

    public VampireShooterAdder(@Nonnull ComponentType<EntityStore, VampireShooterComponent> vampireShooterComponentType) {
        this.vampireShooterComponentType = vampireShooterComponentType;
        this.query = Query.and((Query[])new Query[]{Player.getComponentType()});
    }

    @Nonnull
    public Query<EntityStore> getQuery() {
        return this.query;
    }

    public void onEntityAdded(@Nonnull Ref<EntityStore> ref, @Nonnull AddReason reason, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        Player player = (Player)commandBuffer.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        if (commandBuffer.getComponent(ref, this.vampireShooterComponentType) != null) {
            return;
        }
        VampireShooterComponent component = new VampireShooterComponent();
        PlayerRef playerRefComponent = (PlayerRef)store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRefComponent == null) {
            playerRefComponent = (PlayerRef)commandBuffer.getComponent(ref, PlayerRef.getComponentType());
        }
        if (playerRefComponent != null) {
            HytaleMod.getInstance().getLifeEssenceLevelSystem().restorePowerState(playerRefComponent.getUuid(), component);
        }
        commandBuffer.addComponent(ref, this.vampireShooterComponentType, component);
    }

    public void onEntityRemove(@Nonnull Ref<EntityStore> ref, @Nonnull RemoveReason reason, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
    }
}
