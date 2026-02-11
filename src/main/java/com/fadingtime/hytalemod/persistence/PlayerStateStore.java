package com.fadingtime.hytalemod.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Properties;
import java.util.UUID;

public final class PlayerStateStore {
    private final Properties props = new Properties();
    private final Path filePath;

    public PlayerStateStore(Path filePath) {
        this.filePath = filePath;
        try {
            Files.createDirectories(this.filePath.getParent(), new FileAttribute[0]);
        }
        catch (IOException iOException) {
            // empty catch block
        }
        this.load();
    }

    public synchronized void load() {
        if (!Files.exists(this.filePath, new LinkOption[0])) {
            return;
        }
        try (InputStream in = Files.newInputStream(this.filePath, new OpenOption[0]);){
            this.props.load(in);
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }

    public synchronized void save() {
        try (OutputStream out = Files.newOutputStream(this.filePath, new OpenOption[0]);){
            this.props.store(out, "Hytale Surivors player state");
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }

    public synchronized PlayerLevelData loadLevel(UUID playerId) {
        String prefix = "player." + String.valueOf(playerId) + ".";
        int level = this.getInt(prefix + "level", 1);
        int essence = this.getInt(prefix + "essence", 0);
        int lastStoreLevel = this.getInt(prefix + "lastStoreLevel", 0);
        return new PlayerLevelData(level, essence, lastStoreLevel);
    }

    public synchronized void saveLevel(UUID playerId, int level, int essence, int lastStoreLevel) {
        String prefix = "player." + String.valueOf(playerId) + ".";
        this.props.setProperty(prefix + "level", Integer.toString(level));
        this.props.setProperty(prefix + "essence", Integer.toString(essence));
        this.props.setProperty(prefix + "lastStoreLevel", Integer.toString(lastStoreLevel));
        this.save();
    }

    public synchronized PlayerPowerData loadPower(UUID playerId) {
        String prefix = "player." + String.valueOf(playerId) + ".";
        int projectileCount = this.getInt(prefix + "projectiles", 0);
        float fireRate = this.getFloat(prefix + "fireRate", 1.0f);
        float pickup = this.getFloat(prefix + "pickupRange", 0.0f);
        int bounceBonus = this.getInt(prefix + "bounceBonus", 0);
        int weaponDamageRank = this.getInt(prefix + "weaponDamageRank", 0);
        int healthRank = this.getInt(prefix + "healthRank", 0);
        int speedRank = this.getInt(prefix + "speedRank", 0);
        int luckyRank = this.getInt(prefix + "luckyRank", 0);
        boolean projectileRainUsed = this.getInt(prefix + "projectileRainUsed", 0) > 0;
        return new PlayerPowerData(projectileCount, fireRate, pickup, bounceBonus, weaponDamageRank, healthRank, speedRank, luckyRank, projectileRainUsed);
    }

    public synchronized void savePower(UUID playerId, int projectileCount, float fireRate, float pickupRange, int bounceBonus, int weaponDamageRank, int healthRank, int speedRank, int luckyRank, boolean projectileRainUsed) {
        String prefix = "player." + String.valueOf(playerId) + ".";
        this.props.setProperty(prefix + "projectiles", Integer.toString(projectileCount));
        this.props.setProperty(prefix + "fireRate", Float.toString(fireRate));
        this.props.setProperty(prefix + "pickupRange", Float.toString(pickupRange));
        this.props.setProperty(prefix + "bounceBonus", Integer.toString(bounceBonus));
        this.props.setProperty(prefix + "weaponDamageRank", Integer.toString(weaponDamageRank));
        this.props.setProperty(prefix + "healthRank", Integer.toString(healthRank));
        this.props.setProperty(prefix + "speedRank", Integer.toString(speedRank));
        this.props.setProperty(prefix + "luckyRank", Integer.toString(luckyRank));
        this.props.setProperty(prefix + "projectileRainUsed", projectileRainUsed ? "1" : "0");
        this.save();
    }

    public synchronized int loadWaveCount(UUID playerId) {
        return this.getInt("wave." + String.valueOf(playerId), 0);
    }

    public synchronized void saveWaveCount(UUID playerId, int waveCount) {
        this.props.setProperty("wave." + String.valueOf(playerId), Integer.toString(waveCount));
        this.save();
    }

    public synchronized long loadNextSpawnAt(UUID playerId) {
        return this.getLong("wave.next." + String.valueOf(playerId), 0L);
    }

    public synchronized void saveNextSpawnAt(UUID playerId, long nextSpawnAtMillis) {
        this.props.setProperty("wave.next." + String.valueOf(playerId), Long.toString(nextSpawnAtMillis));
        this.save();
    }

    public synchronized UUID loadBossId(UUID playerId) {
        String value = this.props.getProperty("boss." + String.valueOf(playerId));
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        }
        catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public synchronized void saveBossId(UUID playerId, UUID bossId) {
        if (bossId == null) {
            this.props.remove("boss." + String.valueOf(playerId));
        } else {
            this.props.setProperty("boss." + String.valueOf(playerId), bossId.toString());
        }
        this.save();
    }

    private int getInt(String key, int fallback) {
        String value = this.props.getProperty(key);
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        }
        catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private float getFloat(String key, float fallback) {
        String value = this.props.getProperty(key);
        if (value == null) {
            return fallback;
        }
        try {
            return Float.parseFloat(value.trim());
        }
        catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private long getLong(String key, long fallback) {
        String value = this.props.getProperty(key);
        if (value == null) {
            return fallback;
        }
        try {
            return Long.parseLong(value.trim());
        }
        catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public static final class PlayerLevelData {
        public final int level;
        public final int essenceProgress;
        public final int lastStoreLevel;

        public PlayerLevelData(int level, int essenceProgress, int lastStoreLevel) {
            this.level = level;
            this.essenceProgress = essenceProgress;
            this.lastStoreLevel = lastStoreLevel;
        }
    }

    public static final class PlayerPowerData {
        public final int projectileCount;
        public final float fireRateMultiplier;
        public final float pickupRangeBonus;
        public final int bounceBonus;
        public final int weaponDamageRank;
        public final int healthRank;
        public final int speedRank;
        public final int luckyRank;
        public final boolean projectileRainUsed;

        public PlayerPowerData(int projectileCount, float fireRateMultiplier, float pickupRangeBonus, int bounceBonus, int weaponDamageRank, int healthRank, int speedRank, int luckyRank, boolean projectileRainUsed) {
            this.projectileCount = projectileCount;
            this.fireRateMultiplier = fireRateMultiplier;
            this.pickupRangeBonus = pickupRangeBonus;
            this.bounceBonus = bounceBonus;
            this.weaponDamageRank = weaponDamageRank;
            this.healthRank = healthRank;
            this.speedRank = speedRank;
            this.luckyRank = luckyRank;
            this.projectileRainUsed = projectileRainUsed;
        }
    }
}
