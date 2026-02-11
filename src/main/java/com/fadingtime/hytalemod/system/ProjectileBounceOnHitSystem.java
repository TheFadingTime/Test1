package com.fadingtime.hytalemod.system;

import com.fadingtime.hytalemod.HytaleMod;
import com.fadingtime.hytalemod.component.ProjectileBounceComponent;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.vector.Vector3d;
import com.fadingtime.hytalemod.helper.ProjectileHelper;
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

    public ProjectileBounceOnHitSystem(@Nonnull ComponentType<EntityStore, ProjectileBounceComponent> bounceComponentType) {
        this.bounceComponentType = bounceComponentType;
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

    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull Damage damage) {
        Damage.Source source = damage.getSource();
        if (!(source instanceof Damage.ProjectileSource)) {
            return;
        }
        Damage.ProjectileSource projectileSource = (Damage.ProjectileSource)source;
        Ref projectileRef = projectileSource.getProjectile();
        if (projectileRef == null) {
            return;
        }
        ProjectileBounceComponent bounceComponent = (ProjectileBounceComponent)store.getComponent(projectileRef, this.bounceComponentType);
        if (bounceComponent == null || bounceComponent.getRemainingBounces() <= 0) {
            return;
        }
        Ref targetRef = archetypeChunk.getReferenceTo(index);
        if (store.getComponent(targetRef, this.playerComponentType) != null) {
            return;
        }
        ProjectileComponent projectileComponent = (ProjectileComponent)store.getComponent(projectileRef, this.projectileComponentType);
        TransformComponent projectileTransform = (TransformComponent)store.getComponent(projectileRef, this.transformComponentType);
        Velocity velocity = (Velocity)store.getComponent(projectileRef, this.velocityComponentType);
        if (projectileComponent == null || projectileTransform == null || velocity == null) {
            return;
        }
        Vector3d incoming = this.resolveProjectileVelocity(velocity, projectileComponent);
        double speed = incoming.length();
        if (speed < 0.05) {
            return;
        }
        Vector3d targetCenter = this.getEntityCenter(store, (Ref<EntityStore>)targetRef);
        Vector3d projectilePos = projectileTransform.getPosition();
        Vector3d normal = projectilePos.clone().subtract(targetCenter);
        normal.y = 0.0;
        if (normal.squaredLength() < 1.0E-6) {
            normal.assign(incoming);
            normal.y = 0.0;
        }
        if (normal.squaredLength() < 1.0E-6) {
            normal.assign(0.0, 0.0, 1.0);
        } else {
            normal.normalize();
        }
        Vector3d reflectedDir = incoming.clone();
        SimplePhysicsProvider.computeReflectedVector((Vector3d)incoming, (Vector3d)normal, (Vector3d)reflectedDir);
        reflectedDir.y = incoming.y;
        if (reflectedDir.squaredLength() < 1.0E-6) {
            reflectedDir.assign(normal.x, incoming.y, normal.z);
        }
        if (reflectedDir.squaredLength() < 1.0E-6) {
            reflectedDir.assign(0.0, incoming.y, 1.0);
        } else {
            reflectedDir.normalize();
        }
        float speedMultiplier = bounceComponent.getSpeedMultiplier();
        double newSpeed = speed * (double)(Float.isFinite(speedMultiplier) ? speedMultiplier : 1.0f);
        if (newSpeed < 0.05) {
            newSpeed = speed;
        }
        Vector3d bounceVelocity = reflectedDir.clone().scale(newSpeed);
        double targetRadius = this.getEntityRadius(store, (Ref<EntityStore>)targetRef);
        double projectileRadius = this.getProjectileRadius(projectileComponent);
        double spawnDistance = Math.max(0.15, targetRadius + projectileRadius + 0.15);
        Vector3d spawnPos = targetCenter.clone().addScaled(reflectedDir, spawnDistance);
        spawnPos.y += 0.5;
        ProjectileHelper.fireProjectileWithVelocityAndBounce((Ref<EntityStore>)projectileSource.getRef(), commandBuffer, this.uuidComponentType, spawnPos, bounceVelocity, projectileComponent.getProjectileAssetName(), this.bounceComponentType, Math.max(0, bounceComponent.getRemainingBounces() - 1), speedMultiplier, HytaleMod.LOGGER);
        bounceComponent.setRemainingBounces(0);
    }

    @Nonnull
    private Vector3d getEntityCenter(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref) {
        TransformComponent transform = (TransformComponent)store.getComponent(ref, this.transformComponentType);
        if (transform == null) {
            return Vector3d.ZERO;
        }
        BoundingBox boundingBox = (BoundingBox)store.getComponent(ref, this.boundingBoxComponentType);
        if (boundingBox == null) {
            return transform.getPosition();
        }
        Box box = boundingBox.getBoundingBox();
        Vector3d center = transform.getPosition().clone();
        center.add((box.min.x + box.max.x) * 0.5, (box.min.y + box.max.y) * 0.5, (box.min.z + box.max.z) * 0.5);
        return center;
    }

    private double getEntityRadius(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref) {
        BoundingBox boundingBox = (BoundingBox)store.getComponent(ref, this.boundingBoxComponentType);
        if (boundingBox == null) {
            return 0.5;
        }
        Box box = boundingBox.getBoundingBox();
        double maxExtent = 0.0;
        maxExtent = Math.max(maxExtent, Math.abs(box.min.x));
        maxExtent = Math.max(maxExtent, Math.abs(box.max.x));
        maxExtent = Math.max(maxExtent, Math.abs(box.min.y));
        maxExtent = Math.max(maxExtent, Math.abs(box.max.y));
        maxExtent = Math.max(maxExtent, Math.abs(box.min.z));
        maxExtent = Math.max(maxExtent, Math.abs(box.max.z));
        return Math.max(0.25, maxExtent);
    }

    private double getProjectileRadius(@Nonnull ProjectileComponent projectileComponent) {
        if (projectileComponent.getProjectile() == null) {
            return 0.15;
        }
        return Math.max(0.05, projectileComponent.getProjectile().getRadius());
    }

    @Nonnull
    private Vector3d resolveProjectileVelocity(@Nonnull Velocity velocity, @Nonnull ProjectileComponent projectileComponent) {
        Vector3d incoming = velocity.getVelocity().clone();
        if (incoming.squaredLength() < 1.0E-6) {
            incoming.assign(projectileComponent.getSimplePhysicsProvider().getVelocity());
        }
        return incoming;
    }

}
