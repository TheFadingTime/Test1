package com.fadingtime.hytalemod.system;

import com.fadingtime.hytalemod.HytaleMod;
import com.fadingtime.hytalemod.component.VampireShooterComponent;
import com.fadingtime.hytalemod.helper.ProjectileHelper;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.physics.util.PhysicsMath;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.concurrent.ThreadLocalRandom;

public class VampireShooterSystem
extends EntityTickingSystem<EntityStore> {
    private static final float FIRE_INTERVAL_SECONDS = 10.0f;
    private static final double PROJECTILE_CIRCLE_RADIUS = 0.9;
    private static final String FIREBALL_PROJECTILE_ID = "Skeleton_Mage_Corruption_Orb";
    private static final float FIREBALL_SPEED = 25.0f;
    private static final int BASE_BOUNCES = 0;
    private static final int MAX_BOUNCES = 3;
    private static final float FIREBALL_BOUNCE_SPEED_MULTIPLIER = 0.85f;
    private static final int MAX_PROJECTILES = 10;
    private static final String RAIN_PROJECTILE_PRIMARY_ID = "Fireball";
    private static final int PROJECTILE_RAIN_COUNT = 50;
    private static final double PROJECTILE_RAIN_RADIUS = 25.0;
    private static final double PROJECTILE_RAIN_HEIGHT = 40.0;
    private static final double PROJECTILE_RAIN_SPEED = 20.0;
    private static final float PROJECTILE_RAIN_BURST_DELAY_SECONDS = 0.6f;
    private final ComponentType<EntityStore, VampireShooterComponent> vampireShooterComponentType;
    private final ComponentType<EntityStore, Player> playerComponentType;
    private final ComponentType<EntityStore, TransformComponent> transformComponentType;
    private final ComponentType<EntityStore, HeadRotation> headRotationComponentType;
    private final ComponentType<EntityStore, UUIDComponent> uuidComponentType;
    private final Query<EntityStore> query;

    public VampireShooterSystem(ComponentType<EntityStore, VampireShooterComponent> vampireShooterComponentType) {
        this.vampireShooterComponentType = vampireShooterComponentType;
        this.playerComponentType = Player.getComponentType();
        this.transformComponentType = TransformComponent.getComponentType();
        this.headRotationComponentType = HeadRotation.getComponentType();
        this.uuidComponentType = UUIDComponent.getComponentType();
        this.query = Query.and(this.playerComponentType, this.transformComponentType, this.headRotationComponentType, this.vampireShooterComponentType, this.uuidComponentType);
    }

    public Query<EntityStore> getQuery() {
        return this.query;
    }

    public boolean isParallel(int archetypeChunkSize, int taskCount) {
        return false;
    }

    public void tick(float dt, int index, ArchetypeChunk<EntityStore> archetypeChunk, Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {
        VampireShooterComponent shooterComponent = (VampireShooterComponent)archetypeChunk.getComponent(index, this.vampireShooterComponentType);
        if (shooterComponent == null) {
            return;
        }
        Ref playerRef = archetypeChunk.getReferenceTo(index);
        if (playerRef == null || !playerRef.isValid()) {
            return;
        }
        TransformComponent transform = (TransformComponent)archetypeChunk.getComponent(index, this.transformComponentType);
        HeadRotation headRotation = (HeadRotation)archetypeChunk.getComponent(index, this.headRotationComponentType);
        if (transform == null || headRotation == null) {
            return;
        }
        shooterComponent.tickProjectileRainCooldown(dt);
        if (shooterComponent.getProjectileRainCooldown() <= 0.0f && shooterComponent.consumeProjectileRain()) {
            this.spawnProjectileRain((Ref<EntityStore>)playerRef, transform.getPosition(), commandBuffer);
            shooterComponent.setProjectileRainCooldown(PROJECTILE_RAIN_BURST_DELAY_SECONDS);
        }
        shooterComponent.incrementTimer(dt);
        float rate = shooterComponent.getFireRateMultiplier();
        float interval = rate <= 0.0f ? FIRE_INTERVAL_SECONDS : FIRE_INTERVAL_SECONDS / rate;
        if (shooterComponent.getTimer() >= interval) {
            shooterComponent.resetTimer();
            Player player = (Player)archetypeChunk.getComponent(index, this.playerComponentType);
            if (player == null) {
                return;
            }
            int projectileCount = shooterComponent.getProjectileCount();
            if (projectileCount <= 0) {
                return;
            }
            int count = Math.min(projectileCount, MAX_PROJECTILES);
            Vector3d position = transform.getPosition();
            Vector3f rotation = headRotation.getRotation();
            Vector3d forward = new Vector3d();
            PhysicsMath.vectorFromAngles((float)rotation.getYaw(), (float)rotation.getPitch(), (Vector3d)forward);
            forward.normalize();
            Vector3d forwardFlat = new Vector3d(forward.x, 0.0, forward.z);
            double forwardLen = Math.sqrt(forwardFlat.x * forwardFlat.x + forwardFlat.z * forwardFlat.z);
            if (forwardLen > 1.0E-4) {
                forwardFlat.x /= forwardLen;
                forwardFlat.z /= forwardLen;
            } else {
                forwardFlat.x = 0.0;
                forwardFlat.z = 1.0;
            }
            Vector3d right = new Vector3d(forwardFlat.z, 0.0, -forwardFlat.x);
            int bounceCount = Math.min(MAX_BOUNCES, Math.max(0, BASE_BOUNCES + shooterComponent.getBounceBonus()));
            ProjectileHelper.fireProjectileWithBounce((Ref<EntityStore>)playerRef, commandBuffer, this.uuidComponentType, position, rotation, 0.0f, FIREBALL_PROJECTILE_ID, FIREBALL_SPEED, HytaleMod.getInstance().getProjectileBounceComponentType(), bounceCount, FIREBALL_BOUNCE_SPEED_MULTIPLIER, HytaleMod.LOGGER);
            int remaining = count - 1;
            if (remaining <= 0) {
                return;
            }
            for (int i = 0; i < remaining; ++i) {
                double angle = Math.PI * 2 * (double)i / (double)remaining;
                double cos = Math.cos(angle);
                double sin = Math.sin(angle);
                Vector3d spawnBase = position.clone();
                spawnBase.add(forwardFlat.x * cos * 0.9 + right.x * sin * 0.9, 0.0, forwardFlat.z * cos * 0.9 + right.z * sin * 0.9);
                ProjectileHelper.fireProjectileWithBounce((Ref<EntityStore>)playerRef, commandBuffer, this.uuidComponentType, spawnBase, rotation, (float)Math.toDegrees(angle), FIREBALL_PROJECTILE_ID, FIREBALL_SPEED, HytaleMod.getInstance().getProjectileBounceComponentType(), bounceCount, FIREBALL_BOUNCE_SPEED_MULTIPLIER, HytaleMod.LOGGER);
            }
        }
    }

    private void spawnProjectileRain(Ref<EntityStore> ownerRef, Vector3d center, CommandBuffer<EntityStore> commandBuffer) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < PROJECTILE_RAIN_COUNT; ++i) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = Math.sqrt(random.nextDouble()) * PROJECTILE_RAIN_RADIUS;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            Vector3d spawnPos = center.clone();
            spawnPos.add(x, PROJECTILE_RAIN_HEIGHT, z);
            Vector3d velocity = new Vector3d(0.0, -PROJECTILE_RAIN_SPEED, 0.0);
            ProjectileHelper.fireProjectileWithVelocity(ownerRef, commandBuffer, this.uuidComponentType, spawnPos, velocity, RAIN_PROJECTILE_PRIMARY_ID, HytaleMod.LOGGER);
        }
    }
}
