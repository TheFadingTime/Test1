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
 *  com.hypixel.hytale.component.system.tick.EntityTickingSystem
 *  com.hypixel.hytale.math.vector.Vector3d
 *  com.hypixel.hytale.math.vector.Vector3f
 *  com.hypixel.hytale.server.core.entity.UUIDComponent
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.modules.entity.component.HeadRotation
 *  com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
 *  com.hypixel.hytale.server.core.modules.physics.util.PhysicsMath
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 *  javax.annotation.Nonnull
 */
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
import javax.annotation.Nonnull;

public class VampireShooterSystem
extends EntityTickingSystem<EntityStore> {
    private static final float FIRE_INTERVAL_SECONDS = 10.0f;
    private static final double PROJECTILE_CIRCLE_RADIUS = 0.9;
    private static final String PROJECTILE_ORB_PROJECTILE_ID = "Skeleton_Mage_Corruption_Orb";
    private static final float PROJECTILE_ORB_SPEED = 25.0f;
    private static final int BASE_BOUNCES = 0;
    private static final int MAX_BOUNCES = 3;
    private static final float PROJECTILE_ORB_BOUNCE_SPEED_MULTIPLIER = 0.85f;
    private static final int MAX_PROJECTILES = 10;
    private static final int FRONT_PROJECTILE_THRESHOLD = 3;
    private static final double FRONT_ARC_DEGREES = 50.0;
    private static final double ARC_GROWTH_EXPONENT = 1.7;
    private static final String RAIN_PROJECTILE_PRIMARY_ID = "Fireball";
    private static final int PROJECTILE_RAIN_COUNT = 50;
    private static final double PROJECTILE_RAIN_RADIUS = 25.0;
    private static final double PROJECTILE_RAIN_HEIGHT = 40.0;
    private static final double PROJECTILE_RAIN_SPEED = 20.0;
    private static final float PROJECTILE_RAIN_BURST_DELAY_SECONDS = 0.6f;
    @Nonnull
    private final ComponentType<EntityStore, VampireShooterComponent> vampireShooterComponentType;
    @Nonnull
    private final ComponentType<EntityStore, Player> playerComponentType;
    @Nonnull
    private final ComponentType<EntityStore, TransformComponent> transformComponentType;
    @Nonnull
    private final ComponentType<EntityStore, HeadRotation> headRotationComponentType;
    @Nonnull
    private final ComponentType<EntityStore, UUIDComponent> uuidComponentType;
    @Nonnull
    private final Query<EntityStore> query;

    public VampireShooterSystem(@Nonnull ComponentType<EntityStore, VampireShooterComponent> componentType) {
        this.vampireShooterComponentType = componentType;
        this.playerComponentType = Player.getComponentType();
        this.transformComponentType = TransformComponent.getComponentType();
        this.headRotationComponentType = HeadRotation.getComponentType();
        this.uuidComponentType = UUIDComponent.getComponentType();
        this.query = Query.and((Query[])new Query[]{this.playerComponentType, this.transformComponentType, this.headRotationComponentType, this.vampireShooterComponentType, this.uuidComponentType});
    }

    @Nonnull
    public Query<EntityStore> getQuery() {
        return this.query;
    }

    public boolean isParallel(int n, int n2) {
        return false;
    }

    public void tick(float f, int n, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        float f2;
        VampireShooterComponent vampireShooterComponent = (VampireShooterComponent)archetypeChunk.getComponent(n, this.vampireShooterComponentType);
        if (vampireShooterComponent == null) {
            return;
        }
        Ref ref = archetypeChunk.getReferenceTo(n);
        if (ref == null || !ref.isValid()) {
            return;
        }
        TransformComponent transformComponent = (TransformComponent)archetypeChunk.getComponent(n, this.transformComponentType);
        HeadRotation headRotation = (HeadRotation)archetypeChunk.getComponent(n, this.headRotationComponentType);
        if (transformComponent == null || headRotation == null) {
            return;
        }
        vampireShooterComponent.tickProjectileRainCooldown(f);
        if (vampireShooterComponent.getProjectileRainCooldown() <= 0.0f && vampireShooterComponent.consumeProjectileRain()) {
            this.spawnProjectileRain((Ref<EntityStore>)ref, transformComponent.getPosition(), commandBuffer);
            vampireShooterComponent.setProjectileRainCooldown(0.6f);
        }
        vampireShooterComponent.incrementTimer(f);
        float f3 = vampireShooterComponent.getFireRateMultiplier();
        float f4 = f2 = f3 <= 0.0f ? 10.0f : 10.0f / f3;
        if (vampireShooterComponent.getTimer() >= f2) {
            float[] fArray;
            vampireShooterComponent.resetTimer();
            Player player = (Player)archetypeChunk.getComponent(n, this.playerComponentType);
            if (player == null) {
                return;
            }
            int n2 = vampireShooterComponent.getProjectileCount();
            if (n2 <= 0) {
                return;
            }
            int n3 = Math.min(n2, 10);
            Vector3d vector3d = transformComponent.getPosition();
            Vector3f vector3f = headRotation.getRotation();
            Vector3d vector3d2 = new Vector3d();
            PhysicsMath.vectorFromAngles((float)vector3f.getYaw(), (float)vector3f.getPitch(), (Vector3d)vector3d2);
            vector3d2.normalize();
            Vector3d vector3d3 = new Vector3d(vector3d2.x, 0.0, vector3d2.z);
            double d = Math.sqrt(vector3d3.x * vector3d3.x + vector3d3.z * vector3d3.z);
            if (d > 1.0E-4) {
                vector3d3.x /= d;
                vector3d3.z /= d;
            } else {
                vector3d3.x = 0.0;
                vector3d3.z = 1.0;
            }
            Vector3d vector3d4 = new Vector3d(vector3d3.z, 0.0, -vector3d3.x);
            int n4 = Math.min(3, Math.max(0, 0 + vampireShooterComponent.getBounceBonus()));
            for (float f5 : fArray = this.buildYawOffsets(n3)) {
                double d2 = Math.toRadians(f5);
                double d3 = Math.cos(d2);
                double d4 = Math.sin(d2);
                Vector3d vector3d5 = vector3d.clone();
                vector3d5.add(vector3d3.x * d3 * 0.9 + vector3d4.x * d4 * 0.9, 0.0, vector3d3.z * d3 * 0.9 + vector3d4.z * d4 * 0.9);
                ProjectileHelper.fireProjectileWithBounce((Ref<EntityStore>)ref, commandBuffer, this.uuidComponentType, vector3d5, vector3f, f5, PROJECTILE_ORB_PROJECTILE_ID, PROJECTILE_ORB_SPEED, HytaleMod.getInstance().getProjectileBounceComponentType(), n4, PROJECTILE_ORB_BOUNCE_SPEED_MULTIPLIER, HytaleMod.LOGGER);
            }
        }
    }

    @Nonnull
    private float[] buildYawOffsets(int n) {
        int n2 = Math.max(1, Math.min(n, 10));
        float[] fArray = new float[n2];
        if (n2 == 1) {
            fArray[0] = 0.0f;
            return fArray;
        }
        double d = this.resolveArcDegrees(n2);
        if (d >= 359.999) {
            for (int i = 0; i < n2; ++i) {
                fArray[i] = (float)(360.0 * (double)i / (double)n2);
            }
            return fArray;
        }
        double d2 = d / (double)(n2 - 1);
        double d3 = -d * 0.5;
        for (int i = 0; i < n2; ++i) {
            fArray[i] = (float)(d3 + d2 * (double)i);
        }
        return fArray;
    }

    private double resolveArcDegrees(int n) {
        if (n <= 3) {
            return 50.0;
        }
        if (n >= 10) {
            return 360.0;
        }
        double d = (double)(n - 3) / 7.0;
        double d2 = Math.pow(d, 1.7);
        return 50.0 + d2 * 310.0;
    }

    private void spawnProjectileRain(@Nonnull Ref<EntityStore> ref, @Nonnull Vector3d vector3d, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
        for (int i = 0; i < 50; ++i) {
            double d = threadLocalRandom.nextDouble() * Math.PI * 2.0;
            double d2 = Math.sqrt(threadLocalRandom.nextDouble()) * 25.0;
            double d3 = Math.cos(d) * d2;
            double d4 = Math.sin(d) * d2;
            Vector3d vector3d2 = vector3d.clone();
            vector3d2.add(d3, 40.0, d4);
            Vector3d vector3d3 = new Vector3d(0.0, -20.0, 0.0);
            ProjectileHelper.fireProjectileWithVelocity(ref, commandBuffer, this.uuidComponentType, vector3d2, vector3d3, RAIN_PROJECTILE_PRIMARY_ID, HytaleMod.LOGGER);
        }
    }
}
