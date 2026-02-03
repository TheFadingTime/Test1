package com.fadingtime.hytalemod;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * RefSystem that adds VampireShooterComponent to players when they are added to the world.
 * This ensures all players have the fireball shooting ability.
 */
public class VampireShooterAdder extends RefSystem<EntityStore> {
    
    @Nonnull
    private final ComponentType<EntityStore, VampireShooterComponent> vampireShooterComponentType;
    @Nonnull
    private final Query<EntityStore> query;
    
    public VampireShooterAdder(@Nonnull ComponentType<EntityStore, VampireShooterComponent> vampireShooterComponentType) {
        this.vampireShooterComponentType = vampireShooterComponentType;
        this.query = Query.and(Player.getComponentType());
    }
    
    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return this.query;
    }
    
    @Override
    public void onEntityAdded(
        @Nonnull Ref<EntityStore> ref, 
        @Nonnull AddReason reason, 
        @Nonnull Store<EntityStore> store, 
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        // Only add to player entities
        Player player = commandBuffer.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        
        // Check if component already exists (shouldn't happen, but just in case)
        if (commandBuffer.getComponent(ref, this.vampireShooterComponentType) != null) {
            return;
        }
        
        // Add the VampireShooterComponent to this player
        commandBuffer.addComponent(ref, this.vampireShooterComponentType, new VampireShooterComponent());
        
        HytaleMod.LOGGER.info("Added VampireShooterComponent to player");

        // Enemy spawner removed
    }
    
    @Override
    public void onEntityRemove(
        @Nonnull Ref<EntityStore> ref, 
        @Nonnull RemoveReason reason, 
        @Nonnull Store<EntityStore> store, 
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        // Component will be automatically removed with the entity, no special cleanup needed
    }
}
