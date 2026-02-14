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

    public static void fireProjectileWithBounce(@Nonnull Ref<EntityStore> ownerRef, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull ComponentType<EntityStore, UUIDComponent> uuidComponentType, @Nonnull Vector3d position, @Nonnull Vector3f rotation, float yawOffset, @Nonnull String projectileId, float speed, @Nonnull ComponentType<EntityStore, ProjectileBounceComponent> bounceComponentType, int remainingBounces, float bounceSpeedMultiplier, double spawnYOffset, double spawnForwardOffset, @Nonnull Logger logger) {
        try {
            TimeResource timeResource = (TimeResource)commandBuffer.getResource(TimeResource.getResourceType());
            UUIDComponent uuidComponent = (UUIDComponent)commandBuffer.getComponent(ownerRef, uuidComponentType);
            if (uuidComponent == null) {
                logger.warning("Owner missing UUIDComponent; skipping projectile spawn");
                return;
            }
            float yaw = rotation.getYaw() + yawOffset;
            float pitch = rotation.getPitch();
            Vector3d direction = new Vector3d();
            PhysicsMath.vectorFromAngles((float)yaw, (float)pitch, (Vector3d)direction);
            direction.normalize();
            Vector3d spawnPos = position.clone();
            spawnPos.y += spawnYOffset;
            spawnPos.add(direction.x * spawnForwardOffset, direction.y * spawnForwardOffset, direction.z * spawnForwardOffset);
            Vector3f projectileRotation = new Vector3f();
            projectileRotation.setYaw(yaw);
            projectileRotation.setPitch(pitch);
            projectileRotation.setRoll(0.0f);
            Holder holder = ProjectileComponent.assembleDefaultProjectile((TimeResource)timeResource, (String)projectileId, (Vector3d)spawnPos, (Vector3f)projectileRotation);
            ProjectileComponent projectileComponent = (ProjectileComponent)holder.getComponent(ProjectileComponent.getComponentType());
            if (projectileComponent == null) {
                logger.warning("Failed to create projectile component!");
                return;
            }
            if (remainingBounces > 0) {
                holder.putComponent(bounceComponentType, (Component)new ProjectileBounceComponent(remainingBounces, bounceSpeedMultiplier));
            }
            holder.ensureComponent(Intangible.getComponentType());
            if (!projectileComponent.initialize() || projectileComponent.getProjectile() == null) {
                logger.warning("Failed to initialize projectile!");
                return;
            }
            projectileComponent.shoot(holder, uuidComponent.getUuid(), spawnPos.x, spawnPos.y, spawnPos.z, yaw, pitch);
            direction.scale((double)speed);
            projectileComponent.getSimplePhysicsProvider().setVelocity(direction);
            commandBuffer.addEntity(holder, AddReason.SPAWN);
        }
        catch (Exception e) {
            logger.log(Level.WARNING, "Failed to fire projectile!", e);
        }
    }

    public static boolean fireProjectileWithVelocity(@Nonnull Ref<EntityStore> ownerRef, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull ComponentType<EntityStore, UUIDComponent> uuidComponentType, @Nonnull Vector3d spawnPos, @Nonnull Vector3d velocity, @Nonnull String projectileId, @Nonnull Logger logger) {
        return ProjectileHelper.fireProjectileWithVelocityAndOptionalBounce(ownerRef, commandBuffer, uuidComponentType, spawnPos, velocity, projectileId, null, 0, 1.0f, logger);
    }

    public static boolean fireProjectileWithVelocityAndBounce(@Nonnull Ref<EntityStore> ownerRef, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull ComponentType<EntityStore, UUIDComponent> uuidComponentType, @Nonnull Vector3d spawnPos, @Nonnull Vector3d velocity, @Nonnull String projectileId, @Nonnull ComponentType<EntityStore, ProjectileBounceComponent> bounceComponentType, int remainingBounces, float bounceSpeedMultiplier, @Nonnull Logger logger) {
        return ProjectileHelper.fireProjectileWithVelocityAndOptionalBounce(ownerRef, commandBuffer, uuidComponentType, spawnPos, velocity, projectileId, bounceComponentType, remainingBounces, bounceSpeedMultiplier, logger);
    }

    private static boolean fireProjectileWithVelocityAndOptionalBounce(@Nonnull Ref<EntityStore> ownerRef, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull ComponentType<EntityStore, UUIDComponent> uuidComponentType, @Nonnull Vector3d spawnPos, @Nonnull Vector3d velocity, @Nonnull String projectileId, @Nullable ComponentType<EntityStore, ProjectileBounceComponent> bounceComponentType, int remainingBounces, float bounceSpeedMultiplier, @Nonnull Logger logger) {
        try {
            if (velocity.squaredLength() < 1.0E-6) {
                return false;
            }
            Vector3d direction = velocity.clone().normalize();
            float yaw = PhysicsMath.headingFromDirection((double)direction.x, (double)direction.z);
            float pitch = PhysicsMath.pitchFromDirection((double)direction.x, (double)direction.y, (double)direction.z);
            Vector3f projectileRotation = new Vector3f();
            projectileRotation.setYaw(yaw);
            projectileRotation.setPitch(pitch);
            projectileRotation.setRoll(0.0f);
            TimeResource timeResource = (TimeResource)commandBuffer.getResource(TimeResource.getResourceType());
            Holder holder = ProjectileComponent.assembleDefaultProjectile((TimeResource)timeResource, (String)projectileId, (Vector3d)spawnPos, (Vector3f)projectileRotation);
            ProjectileComponent projectileComponent = (ProjectileComponent)holder.getComponent(ProjectileComponent.getComponentType());
            if (projectileComponent == null) {
                logger.warning("Failed to create projectile component!");
                return false;
            }
            UUIDComponent uuidComponent = (UUIDComponent)commandBuffer.getComponent(ownerRef, uuidComponentType);
            if (uuidComponent == null) {
                logger.warning("Owner missing UUIDComponent; skipping projectile spawn");
                return false;
            }
            if (bounceComponentType != null && remainingBounces > 0) {
                holder.putComponent(bounceComponentType, (Component)new ProjectileBounceComponent(remainingBounces, bounceSpeedMultiplier));
            }
            holder.ensureComponent(Intangible.getComponentType());
            if (!projectileComponent.initialize() || projectileComponent.getProjectile() == null) {
                logger.warning("Failed to initialize projectile!");
                return false;
            }
            projectileComponent.shoot(holder, uuidComponent.getUuid(), spawnPos.x, spawnPos.y, spawnPos.z, yaw, pitch);
            projectileComponent.getSimplePhysicsProvider().setVelocity(velocity);
            commandBuffer.addEntity(holder, AddReason.SPAWN);
            return true;
        }
        catch (Exception e) {
            logger.log(Level.WARNING, "Failed to fire projectile with velocity!", e);
            return false;
        }
    }
}
