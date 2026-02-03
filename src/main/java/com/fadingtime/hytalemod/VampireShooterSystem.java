package com.fadingtime.hytalemod;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.ProjectileComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.Intangible;
import com.hypixel.hytale.server.core.modules.physics.util.PhysicsMath;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

/**
 * System that handles firing three fireballs in a triangular spread every 10 seconds.
 * This system ticks for each player entity with a VampireShooterComponent.
 */
public class VampireShooterSystem extends EntityTickingSystem<EntityStore> {
    
    private static final float FIRE_INTERVAL_SECONDS = 10.0F;
    private static final float SPREAD_ANGLE_DEGREES = 15.0F;
    // Use an existing projectile asset ID so initialization succeeds.
    private static final String FIREBALL_PROJECTILE_ID = "Skeleton_Mage_Corruption_Orb";
    private static final float FIREBALL_SPEED = 25.0F;
    
    @Nonnull
    private final ComponentType<EntityStore, VampireShooterComponent> vampireShooterComponentType;
    @Nonnull
    private final ComponentType<EntityStore, Player> playerComponentType;
    @Nonnull
    private final ComponentType<EntityStore, TransformComponent> transformComponentType;
    @Nonnull
    private final ComponentType<EntityStore, UUIDComponent> uuidComponentType;
    @Nonnull
    private final Query<EntityStore> query;
    
    public VampireShooterSystem(@Nonnull ComponentType<EntityStore, VampireShooterComponent> vampireShooterComponentType) {
        this.vampireShooterComponentType = vampireShooterComponentType;
        this.playerComponentType = Player.getComponentType();
        this.transformComponentType = TransformComponent.getComponentType();
        this.uuidComponentType = UUIDComponent.getComponentType();
        this.query = Query.and(this.playerComponentType, this.transformComponentType, this.vampireShooterComponentType, this.uuidComponentType);
    }
    
    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return this.query;
    }
    
    @Override
    public boolean isParallel(int archetypeChunkSize, int taskCount) {
        return false;
    }
    
    @Override
    public void tick(
        float dt,
        int index,
        @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        VampireShooterComponent shooterComponent = archetypeChunk.getComponent(index, this.vampireShooterComponentType);
        
        if (shooterComponent == null) {
            return;
        }

        TransformComponent transform = archetypeChunk.getComponent(index, this.transformComponentType);
        if (transform == null) {
            return;
        }
        
        // Increment timer
        shooterComponent.incrementTimer(dt);
        
        // Check if it's time to fire
        if (shooterComponent.getTimer() >= FIRE_INTERVAL_SECONDS) {
            shooterComponent.resetTimer();
            
            Ref<EntityStore> playerRef = archetypeChunk.getReferenceTo(index);
            Player player = archetypeChunk.getComponent(index, this.playerComponentType);
            
            if (player == null) {
                return;
            }
            
            // Get player position and rotation
            Vector3d position = transform.getPosition();
            Vector3f rotation = transform.getRotation();
            
            // Fire three fireballs in a triangular spread pattern
            fireFireball(playerRef, commandBuffer, position, rotation, 0.0F);           // Center
            fireFireball(playerRef, commandBuffer, position, rotation, -SPREAD_ANGLE_DEGREES); // Left
            fireFireball(playerRef, commandBuffer, position, rotation, SPREAD_ANGLE_DEGREES);  // Right
        }
    }

    
    /**
     * Fires a single fireball projectile from the given position with the specified yaw offset.
     * 
     * @param playerRef The player reference (owner of the projectile)
     * @param commandBuffer The command buffer for spawning entities
     * @param position The spawn position
     * @param rotation The base rotation (player's look direction)
     * @param yawOffset The yaw offset in degrees for the spread pattern
     */
    private void fireFireball(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Vector3d position,
        @Nonnull Vector3f rotation,
        float yawOffset
    ) {
        try {
            TimeResource timeResource = commandBuffer.getResource(TimeResource.getResourceType());
            UUIDComponent uuidComponent = commandBuffer.getComponent(playerRef, this.uuidComponentType);

            if (uuidComponent == null) {
                HytaleMod.LOGGER.warning("Player missing UUIDComponent; skipping fireball spawn");
                return;
            }
            
            // Calculate spawn position (slightly in front of player to avoid collision)
            float yaw = rotation.getYaw() + yawOffset;
            float pitch = rotation.getPitch();
            
            // Calculate forward direction
            Vector3d direction = new Vector3d();
            PhysicsMath.vectorFromAngles(yaw, pitch, direction);
            direction.normalize();
            
            // Spawn position is slightly in front of player at eye level
            Vector3d spawnPos = position.clone();
            spawnPos.y += 1.5; // Eye height offset
            spawnPos.add(direction.x * 0.5, direction.y * 0.5, direction.z * 0.5);
            
            // Create rotation for the projectile
            Vector3f projectileRotation = new Vector3f();
            projectileRotation.setYaw(yaw);
            projectileRotation.setPitch(pitch);
            projectileRotation.setRoll(0.0F);
            
            // Create the projectile holder
            Holder<EntityStore> holder = ProjectileComponent.assembleDefaultProjectile(
                timeResource, 
                FIREBALL_PROJECTILE_ID, 
                spawnPos, 
                projectileRotation
            );
            
            ProjectileComponent projectileComponent = holder.getComponent(ProjectileComponent.getComponentType());
            
            if (projectileComponent == null) {
                HytaleMod.LOGGER.warning("Failed to create fireball projectile component!");
                return;
            }

            // Initialize the projectile
            holder.ensureComponent(Intangible.getComponentType());

            if (!projectileComponent.initialize() || projectileComponent.getProjectile() == null) {
                HytaleMod.LOGGER.warning("Failed to initialize fireball projectile!");
                return;
            }
            
            // Attach creator and set initial velocity using default projectile behavior
            projectileComponent.shoot(
                holder,
                uuidComponent.getUuid(),
                spawnPos.x,
                spawnPos.y,
                spawnPos.z,
                yaw,
                pitch
            );

            // Override muzzle velocity to the requested speed
            direction.scale(FIREBALL_SPEED);
            projectileComponent.getSimplePhysicsProvider().setVelocity(direction);
            
            // Spawn the entity
            commandBuffer.addEntity(holder, com.hypixel.hytale.component.AddReason.SPAWN);
            
        } catch (Exception e) {
            HytaleMod.LOGGER.log(java.util.logging.Level.WARNING, "Failed to fire fireball!", e);
        }
    }
}
