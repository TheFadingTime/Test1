/*
 * DATA-DRIVEN DESIGN: Projectile and shooter tuning values, loaded from JSON.
 *
 * PRINCIPLE: Gameplay values belong in config files, not source code. Designers
 * can tweak projectile speeds, counts, and offsets by editing projectile.json
 * without recompiling. This follows Hytale's Data Asset philosophy.
 *
 * BEGINNER SMELL: "Magic numbers" — hardcoded constants like 25.0f for speed
 * or 0.9 for radius scattered across source files. Moving them here makes the
 * game balance visible in one place and editable without code changes.
 */
package com.fadingtime.hytalemod.config;

import java.nio.file.Path;
import java.util.logging.Logger;
import org.bson.BsonDocument;

public final class ProjectileConfig {
    public final float fireIntervalSeconds;
    public final double projectileCircleRadius;
    public final String fireballProjectileId;
    public final float fireballSpeed;
    public final int baseBounces;
    public final int maxBounces;
    public final float bounceSpeedMultiplier;
    public final int maxProjectilesPerShot;
    public final String rainProjectileId;
    public final int rainCount;
    public final double rainRadius;
    public final double rainHeight;
    public final double rainSpeed;
    public final float rainBurstDelaySeconds;
    public final double spawnYOffset;
    public final double spawnForwardOffset;

    private ProjectileConfig(
        float fireIntervalSeconds,
        double projectileCircleRadius,
        String fireballProjectileId,
        float fireballSpeed,
        int baseBounces,
        int maxBounces,
        float bounceSpeedMultiplier,
        int maxProjectilesPerShot,
        String rainProjectileId,
        int rainCount,
        double rainRadius,
        double rainHeight,
        double rainSpeed,
        float rainBurstDelaySeconds,
        double spawnYOffset,
        double spawnForwardOffset
    ) {
        this.fireIntervalSeconds = fireIntervalSeconds;
        this.projectileCircleRadius = projectileCircleRadius;
        this.fireballProjectileId = fireballProjectileId;
        this.fireballSpeed = fireballSpeed;
        this.baseBounces = baseBounces;
        this.maxBounces = maxBounces;
        this.bounceSpeedMultiplier = bounceSpeedMultiplier;
        this.maxProjectilesPerShot = maxProjectilesPerShot;
        this.rainProjectileId = rainProjectileId;
        this.rainCount = rainCount;
        this.rainRadius = rainRadius;
        this.rainHeight = rainHeight;
        this.rainSpeed = rainSpeed;
        this.rainBurstDelaySeconds = rainBurstDelaySeconds;
        this.spawnYOffset = spawnYOffset;
        this.spawnForwardOffset = spawnForwardOffset;
    }

    public static ProjectileConfig defaults() {
        return new ProjectileConfig(
            10.0f,
            0.9,
            "Skeleton_Mage_Corruption_Orb",
            25.0f,
            0,
            3,
            0.85f,
            10,
            "Fireball",
            50,
            25.0,
            40.0,
            20.0,
            0.6f,
            1.0,
            0.5
        );
    }

    public static ProjectileConfig load(Path pluginFilePath, Logger logger) {
        ProjectileConfig defaults = ProjectileConfig.defaults();
        BsonDocument doc = JsonConfigLoader.loadDocument(pluginFilePath, "projectile.json", "config/projectile.json", logger);
        BsonDocument shooting = ConfigUtils.getSubDocument(doc, "shooting");
        BsonDocument fireball = ConfigUtils.getSubDocument(doc, "fireball");
        BsonDocument bounce = ConfigUtils.getSubDocument(doc, "bounce");
        BsonDocument rain = ConfigUtils.getSubDocument(doc, "rain");
        return new ProjectileConfig(
            ConfigUtils.readFloat(shooting, "fireIntervalSeconds", defaults.fireIntervalSeconds, 0.1f),
            ConfigUtils.readDouble(shooting, "projectileCircleRadius", defaults.projectileCircleRadius, 0.0),
            ConfigUtils.readString(fireball, "projectileId", defaults.fireballProjectileId),
            ConfigUtils.readFloat(fireball, "speed", defaults.fireballSpeed, 0.1f),
            ConfigUtils.readInt(bounce, "baseBounces", defaults.baseBounces, 0),
            ConfigUtils.readInt(bounce, "maxBounces", defaults.maxBounces, 0),
            ConfigUtils.readFloat(bounce, "speedMultiplier", defaults.bounceSpeedMultiplier, 0.01f),
            ConfigUtils.readInt(shooting, "maxProjectilesPerShot", defaults.maxProjectilesPerShot, 1),
            ConfigUtils.readString(rain, "projectileId", defaults.rainProjectileId),
            ConfigUtils.readInt(rain, "count", defaults.rainCount, 0),
            ConfigUtils.readDouble(rain, "radius", defaults.rainRadius, 0.1),
            ConfigUtils.readDouble(rain, "height", defaults.rainHeight, 0.0),
            ConfigUtils.readDouble(rain, "speed", defaults.rainSpeed, 0.1),
            ConfigUtils.readFloat(rain, "burstDelaySeconds", defaults.rainBurstDelaySeconds, 0.0f),
            ConfigUtils.readDouble(shooting, "spawnYOffset", defaults.spawnYOffset, -1024.0),
            ConfigUtils.readDouble(shooting, "spawnForwardOffset", defaults.spawnForwardOffset, 0.0)
        );
    }
}
