/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.ArchetypeChunk
 *  com.hypixel.hytale.component.CommandBuffer
 *  com.hypixel.hytale.component.ComponentType
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.component.query.Query
 *  com.hypixel.hytale.math.shape.Box
 *  com.hypixel.hytale.math.vector.Vector3d
 *  com.hypixel.hytale.server.core.entity.UUIDComponent
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.entity.entities.ProjectileComponent
 *  com.hypixel.hytale.server.core.modules.entity.component.BoundingBox
 *  com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
 *  com.hypixel.hytale.server.core.modules.entity.damage.Damage
 *  com.hypixel.hytale.server.core.modules.entity.damage.Damage$ProjectileSource
 *  com.hypixel.hytale.server.core.modules.entity.damage.Damage$Source
 *  com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem
 *  com.hypixel.hytale.server.core.modules.physics.SimplePhysicsProvider
 *  com.hypixel.hytale.server.core.modules.physics.component.Velocity
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 *  javax.annotation.Nonnull
 */
package com.fadingtime.hytalemod.system;

import com.fadingtime.hytalemod.HytaleMod;
import com.fadingtime.hytalemod.component.ProjectileBounceComponent;
import com.fadingtime.hytalemod.helper.ProjectileHelper;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.ProjectileComponent;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.physics.SimplePhysicsProvider;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

public class ProjectileBounceOnHitSystem
extends DamageEventSystem {
    private static final double MIN_SPEED = 0.05;
    private static final double SPAWN_OFFSET = 0.15;
    private static final double MIN_NORMAL_SQUARED = 1.0E-6;
    private static final double MIN_SPEED_SQUARED = 1.0E-6;
    @Nonnull
    private static final Query<EntityStore> QUERY = Query.any();
    @Nonnull
    private final ComponentType<EntityStore, ProjectileBounceComponent> bounceComponentType;
    @Nonnull
    private final ComponentType<EntityStore, ProjectileComponent> projectileComponentType;
    @Nonnull
    private final ComponentType<EntityStore, Velocity> velocityComponentType;
    @Nonnull
    private final ComponentType<EntityStore, TransformComponent> transformComponentType;
    @Nonnull
    private final ComponentType<EntityStore, UUIDComponent> uuidComponentType;
    @Nonnull
    private final ComponentType<EntityStore, Player> playerComponentType;
    @Nonnull
    private final ComponentType<EntityStore, BoundingBox> boundingBoxComponentType;

    public ProjectileBounceOnHitSystem(@Nonnull ComponentType<EntityStore, ProjectileBounceComponent> componentType) {
        this.bounceComponentType = componentType;
        this.projectileComponentType = ProjectileComponent.getComponentType();
        this.velocityComponentType = Velocity.getComponentType();
        this.transformComponentType = TransformComponent.getComponentType();
        this.uuidComponentType = UUIDComponent.getComponentType();
        this.playerComponentType = Player.getComponentType();
        this.boundingBoxComponentType = BoundingBox.getComponentType();
    }

    @Nonnull
    public Query<EntityStore> getQuery() {
        return QUERY;
    }

    public void handle(int n, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull Damage damage) {
        Damage.Source source = damage.getSource();
        if (!(source instanceof Damage.ProjectileSource)) {
            return;
        }
        Damage.ProjectileSource projectileSource = (Damage.ProjectileSource)source;
        Ref ref = projectileSource.getProjectile();
        if (ref == null) {
            return;
        }
        ProjectileBounceComponent projectileBounceComponent = (ProjectileBounceComponent)store.getComponent(ref, this.bounceComponentType);
        if (projectileBounceComponent == null || projectileBounceComponent.getRemainingBounces() <= 0) {
            return;
        }
        Ref ref2 = archetypeChunk.getReferenceTo(n);
        if (store.getComponent(ref2, this.playerComponentType) != null) {
            return;
        }
        ProjectileComponent projectileComponent = (ProjectileComponent)store.getComponent(ref, this.projectileComponentType);
        TransformComponent transformComponent = (TransformComponent)store.getComponent(ref, this.transformComponentType);
        Velocity velocity = (Velocity)store.getComponent(ref, this.velocityComponentType);
        if (projectileComponent == null || transformComponent == null || velocity == null) {
            return;
        }
        Vector3d vector3d = this.resolveProjectileVelocity(velocity, projectileComponent);
        double d = vector3d.length();
        if (d < 0.05) {
            return;
        }
        Vector3d vector3d2 = this.getEntityCenter(store, (Ref<EntityStore>)ref2);
        Vector3d vector3d3 = transformComponent.getPosition();
        Vector3d vector3d4 = vector3d3.clone().subtract(vector3d2);
        vector3d4.y = 0.0;
        if (vector3d4.squaredLength() < 1.0E-6) {
            vector3d4.assign(vector3d);
            vector3d4.y = 0.0;
        }
        if (vector3d4.squaredLength() < 1.0E-6) {
            vector3d4.assign(0.0, 0.0, 1.0);
        } else {
            vector3d4.normalize();
        }
        Vector3d vector3d5 = vector3d.clone();
        SimplePhysicsProvider.computeReflectedVector((Vector3d)vector3d, (Vector3d)vector3d4, (Vector3d)vector3d5);
        vector3d5.y = vector3d.y;
        if (vector3d5.squaredLength() < 1.0E-6) {
            vector3d5.assign(vector3d4.x, vector3d.y, vector3d4.z);
        }
        if (vector3d5.squaredLength() < 1.0E-6) {
            vector3d5.assign(0.0, vector3d.y, 1.0);
        } else {
            vector3d5.normalize();
        }
        float f = projectileBounceComponent.getSpeedMultiplier();
        double d2 = d * (double)(Float.isFinite(f) ? f : 1.0f);
        if (d2 < 0.05) {
            d2 = d;
        }
        Vector3d vector3d6 = vector3d5.clone().scale(d2);
        double d3 = this.getEntityRadius(store, (Ref<EntityStore>)ref2);
        double d4 = this.getProjectileRadius(projectileComponent);
        double d5 = Math.max(0.15, d3 + d4 + 0.15);
        Vector3d vector3d7 = vector3d2.clone().addScaled(vector3d5, d5);
        vector3d7.y += 0.5;
        ProjectileHelper.fireProjectileWithVelocityAndBounce((Ref<EntityStore>)projectileSource.getRef(), commandBuffer, this.uuidComponentType, vector3d7, vector3d6, projectileComponent.getProjectileAssetName(), this.bounceComponentType, Math.max(0, projectileBounceComponent.getRemainingBounces() - 1), f, HytaleMod.LOGGER);
        projectileBounceComponent.setRemainingBounces(0);
    }

    @Nonnull
    private Vector3d getEntityCenter(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref) {
        TransformComponent transformComponent = (TransformComponent)store.getComponent(ref, this.transformComponentType);
        if (transformComponent == null) {
            return Vector3d.ZERO;
        }
        BoundingBox boundingBox = (BoundingBox)store.getComponent(ref, this.boundingBoxComponentType);
        if (boundingBox == null) {
            return transformComponent.getPosition();
        }
        Box box = boundingBox.getBoundingBox();
        Vector3d vector3d = transformComponent.getPosition().clone();
        vector3d.add((box.min.x + box.max.x) * 0.5, (box.min.y + box.max.y) * 0.5, (box.min.z + box.max.z) * 0.5);
        return vector3d;
    }

    private double getEntityRadius(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref) {
        BoundingBox boundingBox = (BoundingBox)store.getComponent(ref, this.boundingBoxComponentType);
        if (boundingBox == null) {
            return 0.5;
        }
        Box box = boundingBox.getBoundingBox();
        double d = 0.0;
        d = Math.max(d, Math.abs(box.min.x));
        d = Math.max(d, Math.abs(box.max.x));
        d = Math.max(d, Math.abs(box.min.y));
        d = Math.max(d, Math.abs(box.max.y));
        d = Math.max(d, Math.abs(box.min.z));
        d = Math.max(d, Math.abs(box.max.z));
        return Math.max(0.25, d);
    }

    private double getProjectileRadius(@Nonnull ProjectileComponent projectileComponent) {
        if (projectileComponent.getProjectile() == null) {
            return 0.15;
        }
        return Math.max(0.05, projectileComponent.getProjectile().getRadius());
    }

    @Nonnull
    private Vector3d resolveProjectileVelocity(@Nonnull Velocity velocity, @Nonnull ProjectileComponent projectileComponent) {
        Vector3d vector3d = velocity.getVelocity().clone();
        if (vector3d.squaredLength() < 1.0E-6) {
            vector3d.assign(projectileComponent.getSimplePhysicsProvider().getVelocity());
        }
        return vector3d;
    }
}

