/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.ArchetypeChunk
 *  com.hypixel.hytale.component.CommandBuffer
 *  com.hypixel.hytale.component.ComponentAccessor
 *  com.hypixel.hytale.component.ComponentType
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.RemoveReason
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.component.query.Query
 *  com.hypixel.hytale.component.system.tick.EntityTickingSystem
 *  com.hypixel.hytale.math.vector.Vector3d
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.inventory.ItemStack
 *  com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
 *  com.hypixel.hytale.server.core.modules.entity.item.ItemComponent
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 *  javax.annotation.Nonnull
 */
package com.fadingtime.hytalemod.system;

import com.fadingtime.hytalemod.component.LifeEssenceDropComponent;
import com.fadingtime.hytalemod.component.VampireShooterComponent;
import com.fadingtime.hytalemod.system.PlayerProgressionManager;
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
import javax.annotation.Nonnull;

public class LifeEssencePickupSystem
extends EntityTickingSystem<EntityStore> {
    private static final double DEFAULT_PICKUP_RADIUS = 2.0;
    private static final double ATTRACT_SPEED = 7.0;
    @Nonnull
    private final ComponentType<EntityStore, LifeEssenceDropComponent> lifeEssenceDropType;
    @Nonnull
    private final ComponentType<EntityStore, TransformComponent> transformType;
    @Nonnull
    private final ComponentType<EntityStore, ItemComponent> itemType;
    @Nonnull
    private final ComponentType<EntityStore, Player> playerType;
    @Nonnull
    private final ComponentType<EntityStore, PlayerRef> playerRefType;
    @Nonnull
    private final ComponentType<EntityStore, VampireShooterComponent> shooterType;
    @Nonnull
    private final Query<EntityStore> query;
    @Nonnull
    private final Query<EntityStore> playerQuery;
    @Nonnull
    private final Query<EntityStore> itemQuery;
    @Nonnull
    private final PlayerProgressionManager levelSystem;

    public LifeEssencePickupSystem(@Nonnull ComponentType<EntityStore, LifeEssenceDropComponent> componentType, @Nonnull ComponentType<EntityStore, VampireShooterComponent> componentType2, @Nonnull PlayerProgressionManager playerProgressionManager) {
        this.lifeEssenceDropType = componentType;
        this.transformType = TransformComponent.getComponentType();
        this.itemType = ItemComponent.getComponentType();
        this.playerType = Player.getComponentType();
        this.playerRefType = PlayerRef.getComponentType();
        this.shooterType = componentType2;
        this.playerQuery = Query.and((Query[])new Query[]{this.playerType, this.playerRefType, this.transformType});
        this.itemQuery = Query.and((Query[])new Query[]{this.lifeEssenceDropType, this.transformType});
        this.query = this.playerQuery;
        this.levelSystem = playerProgressionManager;
    }

    @Nonnull
    public Query<EntityStore> getQuery() {
        return this.query;
    }

    public boolean isParallel(int n, int n2) {
        return false;
    }

    public void tick(float f, int n, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk2, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        Player player = (Player)archetypeChunk2.getComponent(n, this.playerType);
        PlayerRef playerRef = (PlayerRef)archetypeChunk2.getComponent(n, this.playerRefType);
        TransformComponent transformComponent = (TransformComponent)archetypeChunk2.getComponent(n, this.transformType);
        if (player == null || playerRef == null || transformComponent == null) {
            return;
        }
        Ref ref = archetypeChunk2.getReferenceTo(n);
        VampireShooterComponent vampireShooterComponent = null;
        if (ref != null && ref.isValid() && (vampireShooterComponent = (VampireShooterComponent)store.getComponent(ref, this.shooterType)) == null) {
            vampireShooterComponent = (VampireShooterComponent)commandBuffer.getComponent(ref, this.shooterType);
        }
        double d = vampireShooterComponent != null ? Math.max(0.0, (double)vampireShooterComponent.getPickupRangeBonus()) : Math.max(0.0, this.levelSystem.getPickupRangeBonus(playerRef.getUuid()));
        double d2 = 2.0;
        double d3 = d2 * d2;
        Vector3d vector3d = transformComponent.getPosition();
        double d4 = d > 0.0 ? Math.max(2.0, d) : 0.0;
        double d5 = d4 * d4;
        double d6 = d3;
        AtomicBoolean atomicBoolean = new AtomicBoolean(false);
        store.forEachChunk(this.itemQuery, (archetypeChunk, commandBuffer2) -> {
            if (atomicBoolean.get()) {
                return;
            }
            int n2 = archetypeChunk.size();
            for (int i = 0; i < n2; ++i) {
                Ref ref2;
                ItemStack itemStack;
                TransformComponent transformComponent2;
                LifeEssenceDropComponent lifeEssenceDropComponent = (LifeEssenceDropComponent)archetypeChunk.getComponent(i, this.lifeEssenceDropType);
                if (lifeEssenceDropComponent == null || (transformComponent2 = (TransformComponent)archetypeChunk.getComponent(i, this.transformType)) == null) continue;
                int n3 = 1;
                ItemComponent itemComponent = (ItemComponent)archetypeChunk.getComponent(i, this.itemType);
                if (itemComponent != null && (itemStack = itemComponent.getItemStack()) != null && !itemStack.isEmpty()) {
                    n3 = Math.max(1, itemStack.getQuantity());
                }
                Vector3d vector3d2 = transformComponent2.getPosition();
                double d7 = vector3d.getX() - vector3d2.getX();
                double d8 = vector3d.getY() - vector3d2.getY();
                double d9 = vector3d.getZ() - vector3d2.getZ();
                double d10 = d7 * d7 + d8 * d8 + d9 * d9;
                if (d4 > 0.0 && d10 <= d5 && d10 > 0.01) {
                    double d11 = Math.sqrt(d10);
                    double d12 = d7 / d11 * 7.0;
                    double d13 = d8 / d11 * 7.0;
                    double d14 = d9 / d11 * 7.0;
                    Vector3d vector3d3 = vector3d2.clone().add(d12 * (double)f, d13 * (double)f, d14 * (double)f);
                    transformComponent2.setPosition(vector3d3);
                    transformComponent2.markChunkDirty((ComponentAccessor)store);
                }
                if (d10 > d6 || (ref2 = archetypeChunk.getReferenceTo(i)) == null || !ref2.isValid()) continue;
                this.levelSystem.grantEssence(playerRef, player, store, n3);
                commandBuffer.removeEntity(ref2, RemoveReason.REMOVE);
                atomicBoolean.set(true);
                return;
            }
        });
    }
}
