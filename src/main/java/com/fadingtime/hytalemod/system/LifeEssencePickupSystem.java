package com.fadingtime.hytalemod.system;

import com.fadingtime.hytalemod.component.LifeEssenceDropComponent;
import com.fadingtime.hytalemod.component.VampireShooterComponent;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.concurrent.atomic.AtomicBoolean;

public class LifeEssencePickupSystem
extends EntityTickingSystem<EntityStore> {
    private static final double DEFAULT_PICKUP_RADIUS = 2.0;
    private static final double ATTRACT_SPEED = 7.0;
    private final ComponentType<EntityStore, LifeEssenceDropComponent> lifeEssenceDropType;
    private final ComponentType<EntityStore, TransformComponent> transformType;
    private final ComponentType<EntityStore, ItemComponent> itemType;
    private final ComponentType<EntityStore, Player> playerType;
    private final ComponentType<EntityStore, PlayerRef> playerRefType;
    private final ComponentType<EntityStore, VampireShooterComponent> shooterType;
    private final Query<EntityStore> query;
    private final Query<EntityStore> playerQuery;
    private final Query<EntityStore> itemQuery;
    private final PlayerProgressionManager levelSystem;

    public LifeEssencePickupSystem(ComponentType<EntityStore, LifeEssenceDropComponent> lifeEssenceDropType, ComponentType<EntityStore, VampireShooterComponent> shooterType, PlayerProgressionManager levelSystem) {
        this.lifeEssenceDropType = lifeEssenceDropType;
        this.transformType = TransformComponent.getComponentType();
        this.itemType = ItemComponent.getComponentType();
        this.playerType = Player.getComponentType();
        this.playerRefType = PlayerRef.getComponentType();
        this.shooterType = shooterType;
        this.playerQuery = Query.and(this.playerType, this.playerRefType, this.transformType);
        this.itemQuery = Query.and(this.lifeEssenceDropType, this.transformType);
        this.query = this.playerQuery;
        this.levelSystem = levelSystem;
    }

    public Query<EntityStore> getQuery() {
        return this.query;
    }

    public boolean isParallel(int archetypeChunkSize, int taskCount) {
        return false;
    }

    public void tick(float dt, int index, ArchetypeChunk<EntityStore> archetypeChunk, Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {
        Player player = (Player)archetypeChunk.getComponent(index, this.playerType);
        PlayerRef playerRefComponent = (PlayerRef)archetypeChunk.getComponent(index, this.playerRefType);
        TransformComponent playerTransform = (TransformComponent)archetypeChunk.getComponent(index, this.transformType);
        if (player == null || playerRefComponent == null || playerTransform == null) {
            return;
        }

        Ref playerRef = archetypeChunk.getReferenceTo(index);
        VampireShooterComponent shooter = null;
        if (playerRef != null && playerRef.isValid()) {
            shooter = (VampireShooterComponent)store.getComponent(playerRef, this.shooterType);
            if (shooter == null) {
                shooter = (VampireShooterComponent)commandBuffer.getComponent(playerRef, this.shooterType);
            }
        }

        double bonus = shooter != null ? Math.max(0.0, (double)shooter.getPickupRangeBonus()) : Math.max(0.0, this.levelSystem.getPickupRangeBonus(playerRefComponent.getUuid()));
        double effectiveRadiusSq = DEFAULT_PICKUP_RADIUS * DEFAULT_PICKUP_RADIUS;
        Vector3d playerPos = playerTransform.getPosition();
        double attractRadius = bonus > 0.0 ? Math.max(DEFAULT_PICKUP_RADIUS, bonus) : 0.0;
        double attractRadiusSq = attractRadius * attractRadius;
        AtomicBoolean pickedUp = new AtomicBoolean(false);

        store.forEachChunk(this.itemQuery, (itemChunk, itemBuffer) -> {
            if (pickedUp.get()) {
                return;
            }

            int size = itemChunk.size();
            for (int i = 0; i < size; ++i) {
                LifeEssenceDropComponent dropComponent = (LifeEssenceDropComponent)itemChunk.getComponent(i, this.lifeEssenceDropType);
                TransformComponent itemTransform = (TransformComponent)itemChunk.getComponent(i, this.transformType);
                if (dropComponent == null || itemTransform == null) {
                    continue;
                }

                int essenceAmount = 1;
                ItemComponent itemComponent = (ItemComponent)itemChunk.getComponent(i, this.itemType);
                ItemStack stack = itemComponent != null ? itemComponent.getItemStack() : null;
                if (stack != null && !stack.isEmpty()) {
                    essenceAmount = Math.max(1, stack.getQuantity());
                }

                Vector3d itemPos = itemTransform.getPosition();
                double dx = playerPos.getX() - itemPos.getX();
                double dy = playerPos.getY() - itemPos.getY();
                double dz = playerPos.getZ() - itemPos.getZ();
                double distSq = dx * dx + dy * dy + dz * dz;

                if (bonus > 0.0 && distSq <= attractRadiusSq && distSq > 0.01) {
                    double dist = Math.sqrt(distSq);
                    double vx = dx / dist * ATTRACT_SPEED;
                    double vy = dy / dist * ATTRACT_SPEED;
                    double vz = dz / dist * ATTRACT_SPEED;
                    Vector3d newPos = itemPos.clone().add(vx * dt, vy * dt, vz * dt);
                    itemTransform.setPosition(newPos);
                    itemTransform.markChunkDirty((ComponentAccessor)store);
                }

                if (distSq > effectiveRadiusSq) {
                    continue;
                }
                Ref itemRef = itemChunk.getReferenceTo(i);
                if (itemRef == null || !itemRef.isValid()) {
                    continue;
                }

                this.levelSystem.grantEssence(playerRefComponent, player, store, essenceAmount);
                commandBuffer.removeEntity(itemRef, RemoveReason.REMOVE);
                pickedUp.set(true);
                return;
            }
        });
    }
}
