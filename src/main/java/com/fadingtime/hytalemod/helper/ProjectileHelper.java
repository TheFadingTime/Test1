/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.AddReason
 *  com.hypixel.hytale.component.CommandBuffer
 *  com.hypixel.hytale.component.Component
 *  com.hypixel.hytale.component.ComponentType
 *  com.hypixel.hytale.component.Holder
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.math.vector.Vector3d
 *  com.hypixel.hytale.math.vector.Vector3f
 *  com.hypixel.hytale.server.core.entity.UUIDComponent
 *  com.hypixel.hytale.server.core.entity.entities.ProjectileComponent
 *  com.hypixel.hytale.server.core.modules.entity.component.Intangible
 *  com.hypixel.hytale.server.core.modules.physics.util.PhysicsMath
 *  com.hypixel.hytale.server.core.modules.time.TimeResource
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 *  javax.annotation.Nonnull
 *  javax.annotation.Nullable
 */
package com.fadingtime.hytalemod.helper;

import com.fadingtime.hytalemod.component.ProjectileBounceComponent;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.ProjectileComponent;
import com.hypixel.hytale.server.core.modules.entity.component.Intangible;
import com.hypixel.hytale.server.core.modules.physics.util.PhysicsMath;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ProjectileHelper {
    private ProjectileHelper() {
    }

    public static void fireProjectileWithBounce(@Nonnull Ref<EntityStore> ref, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull ComponentType<EntityStore, UUIDComponent> componentType, @Nonnull Vector3d vector3d, @Nonnull Vector3f vector3f, float f, @Nonnull String string, float f2, @Nonnull ComponentType<EntityStore, ProjectileBounceComponent> componentType2, int n, float f3, @Nonnull Logger logger) {
        try {
            TimeResource timeResource = (TimeResource)commandBuffer.getResource(TimeResource.getResourceType());
            UUIDComponent uUIDComponent = (UUIDComponent)commandBuffer.getComponent(ref, componentType);
            if (uUIDComponent == null) {
                logger.warning("Owner missing UUIDComponent; skipping projectile spawn");
                return;
            }
            float f4 = vector3f.getYaw() + f;
            float f5 = vector3f.getPitch();
            Vector3d vector3d2 = new Vector3d();
            PhysicsMath.vectorFromAngles((float)f4, (float)f5, (Vector3d)vector3d2);
            vector3d2.normalize();
            Vector3d vector3d3 = vector3d.clone();
            vector3d3.y += 1.0;
            vector3d3.add(vector3d2.x * 0.5, vector3d2.y * 0.5, vector3d2.z * 0.5);
            Vector3f vector3f2 = new Vector3f();
            vector3f2.setYaw(f4);
            vector3f2.setPitch(f5);
            vector3f2.setRoll(0.0f);
            Holder holder = ProjectileComponent.assembleDefaultProjectile((TimeResource)timeResource, (String)string, (Vector3d)vector3d3, (Vector3f)vector3f2);
            ProjectileComponent projectileComponent = (ProjectileComponent)holder.getComponent(ProjectileComponent.getComponentType());
            if (projectileComponent == null) {
                logger.warning("Failed to create projectile component!");
                return;
            }
            if (n > 0) {
                holder.putComponent(componentType2, (Component)new ProjectileBounceComponent(n, f3));
            }
            holder.ensureComponent(Intangible.getComponentType());
            if (!projectileComponent.initialize() || projectileComponent.getProjectile() == null) {
                logger.warning("Failed to initialize projectile!");
                return;
            }
            projectileComponent.shoot(holder, uUIDComponent.getUuid(), vector3d3.x, vector3d3.y, vector3d3.z, f4, f5);
            vector3d2.scale((double)f2);
            projectileComponent.getSimplePhysicsProvider().setVelocity(vector3d2);
            commandBuffer.addEntity(holder, AddReason.SPAWN);
        }
        catch (RuntimeException runtimeException) {
            logger.log(Level.WARNING, "Failed to fire projectile!", runtimeException);
        }
    }

    public static boolean fireProjectileWithVelocity(@Nonnull Ref<EntityStore> ref, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull ComponentType<EntityStore, UUIDComponent> componentType, @Nonnull Vector3d vector3d, @Nonnull Vector3d vector3d2, @Nonnull String string, @Nonnull Logger logger) {
        return ProjectileHelper.fireProjectileWithVelocityAndOptionalBounce(ref, commandBuffer, componentType, vector3d, vector3d2, string, null, 0, 1.0f, logger);
    }

    public static boolean fireProjectileWithVelocityAndBounce(@Nonnull Ref<EntityStore> ref, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull ComponentType<EntityStore, UUIDComponent> componentType, @Nonnull Vector3d vector3d, @Nonnull Vector3d vector3d2, @Nonnull String string, @Nonnull ComponentType<EntityStore, ProjectileBounceComponent> componentType2, int n, float f, @Nonnull Logger logger) {
        return ProjectileHelper.fireProjectileWithVelocityAndOptionalBounce(ref, commandBuffer, componentType, vector3d, vector3d2, string, componentType2, n, f, logger);
    }

    private static boolean fireProjectileWithVelocityAndOptionalBounce(@Nonnull Ref<EntityStore> ref, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull ComponentType<EntityStore, UUIDComponent> componentType, @Nonnull Vector3d vector3d, @Nonnull Vector3d vector3d2, @Nonnull String string, @Nullable ComponentType<EntityStore, ProjectileBounceComponent> componentType2, int n, float f, @Nonnull Logger logger) {
        try {
            if (vector3d2.squaredLength() < 1.0E-6) {
                return false;
            }
            Vector3d vector3d3 = vector3d2.clone().normalize();
            float f2 = PhysicsMath.headingFromDirection((double)vector3d3.x, (double)vector3d3.z);
            float f3 = PhysicsMath.pitchFromDirection((double)vector3d3.x, (double)vector3d3.y, (double)vector3d3.z);
            Vector3f vector3f = new Vector3f();
            vector3f.setYaw(f2);
            vector3f.setPitch(f3);
            vector3f.setRoll(0.0f);
            TimeResource timeResource = (TimeResource)commandBuffer.getResource(TimeResource.getResourceType());
            Holder holder = ProjectileComponent.assembleDefaultProjectile((TimeResource)timeResource, (String)string, (Vector3d)vector3d, (Vector3f)vector3f);
            ProjectileComponent projectileComponent = (ProjectileComponent)holder.getComponent(ProjectileComponent.getComponentType());
            if (projectileComponent == null) {
                logger.warning("Failed to create projectile component!");
                return false;
            }
            UUIDComponent uUIDComponent = (UUIDComponent)commandBuffer.getComponent(ref, componentType);
            if (uUIDComponent == null) {
                logger.warning("Owner missing UUIDComponent; skipping projectile spawn");
                return false;
            }
            if (componentType2 != null && n > 0) {
                holder.putComponent(componentType2, (Component)new ProjectileBounceComponent(n, f));
            }
            holder.ensureComponent(Intangible.getComponentType());
            if (!projectileComponent.initialize() || projectileComponent.getProjectile() == null) {
                logger.warning("Failed to initialize projectile!");
                return false;
            }
            projectileComponent.shoot(holder, uUIDComponent.getUuid(), vector3d.x, vector3d.y, vector3d.z, f2, f3);
            projectileComponent.getSimplePhysicsProvider().setVelocity(vector3d2);
            commandBuffer.addEntity(holder, AddReason.SPAWN);
            return true;
        }
        catch (RuntimeException runtimeException) {
            logger.log(Level.WARNING, "Failed to fire projectile with velocity!", runtimeException);
            return false;
        }
    }
}

