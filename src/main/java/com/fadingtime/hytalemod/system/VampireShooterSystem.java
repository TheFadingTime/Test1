/*
 * DATA-DRIVEN DESIGN: All projectile tuning values now come from ProjectileConfig
 * (loaded from projectile.json) instead of hardcoded static final constants.
 *
 * PRINCIPLE: Separate "what the code does" (logic) from "what values it uses" (data).
 * The code reads values from config; designers tune the game by editing JSON.
 *
 * BEGINNER SMELL: "Magic numbers" — 14 hardcoded constants like FIREBALL_SPEED = 25.0f
 * scattered here required a recompile to change. Now they live in projectile.json.
 */
package com.fadingtime.hytalemod.system;

import com.fadingtime.hytalemod.HytaleMod;
import com.fadingtime.hytalemod.component.VampireShooterComponent;
import com.fadingtime.hytalemod.config.ProjectileConfig;
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
    private volatile ProjectileConfig config;
    private final ComponentType<EntityStore, VampireShooterComponent> vampireShooterComponentType;
    private final ComponentType<EntityStore, Player> playerComponentType;
    private final ComponentType<EntityStore, TransformComponent> transformComponentType;
    private final ComponentType<EntityStore, HeadRotation> headRotationComponentType;
    private final ComponentType<EntityStore, UUIDComponent> uuidComponentType;
    private final Query<EntityStore> query;

    public VampireShooterSystem(ComponentType<EntityStore, VampireShooterComponent> vampireShooterComponentType, ProjectileConfig config) {
        this.vampireShooterComponentType = vampireShooterComponentType;
        this.config = config;
        this.playerComponentType = Player.getComponentType();
        this.transformComponentType = TransformComponent.getComponentType();
        this.headRotationComponentType = HeadRotation.getComponentType();
        this.uuidComponentType = UUIDComponent.getComponentType();
        this.query = Query.and(this.playerComponentType, this.transformComponentType, this.headRotationComponentType, this.vampireShooterComponentType, this.uuidComponentType);
    }

    public void reloadConfig(ProjectileConfig config) {
        this.config = config;
    }

    public Query<EntityStore> getQuery() {
        return this.query;
    }

    public boolean isParallel(int archetypeChunkSize, int taskCount) {
        return false;
    }

    public void tick(float dt, int index, ArchetypeChunk<EntityStore> archetypeChunk, Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {
        var shooterComponent = (VampireShooterComponent)archetypeChunk.getComponent(index, this.vampireShooterComponentType);
        if (shooterComponent == null) {
            return;
        }

        Ref<EntityStore> playerRef = archetypeChunk.getReferenceTo(index);
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
            this.spawnProjectileRain(playerRef, transform.getPosition(), commandBuffer);
            shooterComponent.setProjectileRainCooldown(this.config.rainBurstDelaySeconds);
        }

        shooterComponent.incrementTimer(dt);
        float rate = shooterComponent.getFireRateMultiplier();
        float interval = rate <= 0.0f ? this.config.fireIntervalSeconds : this.config.fireIntervalSeconds / rate;
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
            int count = Math.min(projectileCount, this.config.maxProjectilesPerShot);
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
            int bounceCount = Math.min(this.config.maxBounces, Math.max(0, this.config.baseBounces + shooterComponent.getBounceBonus()));

            ProjectileHelper.fireProjectileWithBounce(playerRef, commandBuffer, this.uuidComponentType, position, rotation, 0.0f, this.config.fireballProjectileId, this.config.fireballSpeed, HytaleMod.getInstance().getProjectileBounceComponentType(), bounceCount, this.config.bounceSpeedMultiplier, this.config.spawnYOffset, this.config.spawnForwardOffset, HytaleMod.LOGGER);
            int remaining = count - 1;
            if (remaining <= 0) {
                return;
            }

            for (int i = 0; i < remaining; ++i) {
                double angle = 2.0 * Math.PI * (double)i / (double)remaining;
                double cos = Math.cos(angle);
                double sin = Math.sin(angle);
                Vector3d spawnBase = position.clone();
                spawnBase.add(forwardFlat.x * cos * this.config.projectileCircleRadius + right.x * sin * this.config.projectileCircleRadius, 0.0, forwardFlat.z * cos * this.config.projectileCircleRadius + right.z * sin * this.config.projectileCircleRadius);
                ProjectileHelper.fireProjectileWithBounce(playerRef, commandBuffer, this.uuidComponentType, spawnBase, rotation, (float)Math.toDegrees(angle), this.config.fireballProjectileId, this.config.fireballSpeed, HytaleMod.getInstance().getProjectileBounceComponentType(), bounceCount, this.config.bounceSpeedMultiplier, this.config.spawnYOffset, this.config.spawnForwardOffset, HytaleMod.LOGGER);
            }
        }
    }

    private void spawnProjectileRain(Ref<EntityStore> ownerRef, Vector3d center, CommandBuffer<EntityStore> commandBuffer) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < this.config.rainCount; ++i) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = Math.sqrt(random.nextDouble()) * this.config.rainRadius;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            Vector3d spawnPos = center.clone();
            spawnPos.add(x, this.config.rainHeight, z);
            Vector3d velocity = new Vector3d(0.0, -this.config.rainSpeed, 0.0);
            ProjectileHelper.fireProjectileWithVelocity(ownerRef, commandBuffer, this.uuidComponentType, spawnPos, velocity, this.config.rainProjectileId, HytaleMod.LOGGER);
        }
    }
}
