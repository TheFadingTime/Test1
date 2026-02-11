package com.fadingtime.hytalemod.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PlayerStateStore {
    private static final Logger LOGGER = Logger.getLogger(PlayerStateStore.class.getName());
    private final Properties props = new Properties();
    private final Path filePath;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public PlayerStateStore(Path filePath) {
        this.filePath = filePath;
        try {
            Path parent = this.filePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        }
        catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Failed to create player state directory for " + this.filePath, exception);
        }
        this.load();
    }

    public void load() {
        this.lock.writeLock().lock();
        try {
            this.props.clear();
            if (!Files.exists(this.filePath)) {
                return;
            }
            try (InputStream in = Files.newInputStream(this.filePath, StandardOpenOption.READ);){
                this.props.load(in);
            }
            catch (IOException exception) {
                LOGGER.log(Level.WARNING, "Failed to load player state file: " + this.filePath, exception);
            }
        }
        finally {
            this.lock.writeLock().unlock();
        }
    }

    public void save() {
        this.lock.writeLock().lock();
        try {
            this.saveUnsafe();
        }
        finally {
            this.lock.writeLock().unlock();
        }
    }

    public PlayerLevelData loadLevel(UUID playerId) {
        this.lock.readLock().lock();
        try {
            String prefix = PlayerStateStore.playerPrefix(playerId);
            int level = this.getInt(prefix + "level", 1);
            int essence = this.getInt(prefix + "essence", 0);
            int lastStoreLevel = this.getInt(prefix + "lastStoreLevel", 0);
            return new PlayerLevelData(level, essence, lastStoreLevel);
        }
        finally {
            this.lock.readLock().unlock();
        }
    }

    public void saveLevel(UUID playerId, int level, int essence, int lastStoreLevel) {
        this.lock.writeLock().lock();
        try {
            String prefix = PlayerStateStore.playerPrefix(playerId);
            this.props.setProperty(prefix + "level", Integer.toString(level));
            this.props.setProperty(prefix + "essence", Integer.toString(essence));
            this.props.setProperty(prefix + "lastStoreLevel", Integer.toString(lastStoreLevel));
            this.saveUnsafe();
        }
        finally {
            this.lock.writeLock().unlock();
        }
    }

    public PlayerPowerData loadPower(UUID playerId) {
        this.lock.readLock().lock();
        try {
            String prefix = PlayerStateStore.playerPrefix(playerId);
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
        finally {
            this.lock.readLock().unlock();
        }
    }

    public void savePower(UUID playerId, int projectileCount, float fireRate, float pickupRange, int bounceBonus, int weaponDamageRank, int healthRank, int speedRank, int luckyRank, boolean projectileRainUsed) {
        this.lock.writeLock().lock();
        try {
            String prefix = PlayerStateStore.playerPrefix(playerId);
            this.props.setProperty(prefix + "projectiles", Integer.toString(projectileCount));
            this.props.setProperty(prefix + "fireRate", Float.toString(fireRate));
            this.props.setProperty(prefix + "pickupRange", Float.toString(pickupRange));
            this.props.setProperty(prefix + "bounceBonus", Integer.toString(bounceBonus));
            this.props.setProperty(prefix + "weaponDamageRank", Integer.toString(weaponDamageRank));
            this.props.setProperty(prefix + "healthRank", Integer.toString(healthRank));
            this.props.setProperty(prefix + "speedRank", Integer.toString(speedRank));
            this.props.setProperty(prefix + "luckyRank", Integer.toString(luckyRank));
            this.props.setProperty(prefix + "projectileRainUsed", projectileRainUsed ? "1" : "0");
            this.saveUnsafe();
        }
        finally {
            this.lock.writeLock().unlock();
        }
    }

    public int loadWaveCount(UUID playerId) {
        this.lock.readLock().lock();
        try {
            return this.getInt(PlayerStateStore.waveCountKey(playerId), 0);
        }
        finally {
            this.lock.readLock().unlock();
        }
    }

    public void saveWaveCount(UUID playerId, int waveCount) {
        this.lock.writeLock().lock();
        try {
            this.props.setProperty(PlayerStateStore.waveCountKey(playerId), Integer.toString(waveCount));
            this.saveUnsafe();
        }
        finally {
            this.lock.writeLock().unlock();
        }
    }

    public long loadNextSpawnAt(UUID playerId) {
        this.lock.readLock().lock();
        try {
            return this.getLong(PlayerStateStore.waveNextSpawnKey(playerId), 0L);
        }
        finally {
            this.lock.readLock().unlock();
        }
    }

    public void saveNextSpawnAt(UUID playerId, long nextSpawnAtMillis) {
        this.lock.writeLock().lock();
        try {
            this.props.setProperty(PlayerStateStore.waveNextSpawnKey(playerId), Long.toString(nextSpawnAtMillis));
            this.saveUnsafe();
        }
        finally {
            this.lock.writeLock().unlock();
        }
    }

    public UUID loadBossId(UUID playerId) {
        this.lock.readLock().lock();
        try {
            String value = this.props.getProperty(PlayerStateStore.bossIdKey(playerId));
            if (value == null || value.isBlank()) {
                return null;
            }
            try {
                return UUID.fromString(value.trim());
            }
            catch (IllegalArgumentException exception) {
                LOGGER.log(Level.FINE, "Invalid boss UUID for player " + playerId + ": " + value, exception);
                return null;
            }
        }
        finally {
            this.lock.readLock().unlock();
        }
    }

    public void saveBossId(UUID playerId, UUID bossId) {
        this.lock.writeLock().lock();
        try {
            String key = PlayerStateStore.bossIdKey(playerId);
            if (bossId == null) {
                this.props.remove(key);
            } else {
                this.props.setProperty(key, bossId.toString());
            }
            this.saveUnsafe();
        }
        finally {
            this.lock.writeLock().unlock();
        }
    }

    private void saveUnsafe() {
        try {
            Path parent = this.filePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (OutputStream out = Files.newOutputStream(this.filePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);){
                this.props.store(out, "Hytale Survivors player state");
            }
        }
        catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Failed to save player state file: " + this.filePath, exception);
        }
    }

    private static String playerPrefix(UUID playerId) {
        return "player." + playerId + ".";
    }

    private static String waveCountKey(UUID playerId) {
        return "wave." + playerId;
    }

    private static String waveNextSpawnKey(UUID playerId) {
        return "wave.next." + playerId;
    }

    private static String bossIdKey(UUID playerId) {
        return "boss." + playerId;
    }

    private int getInt(String key, int defaultValue) {
        String value = this.props.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        }
        catch (NumberFormatException exception) {
            LOGGER.log(Level.FINE, "Invalid int for key '" + key + "': " + value, exception);
            return defaultValue;
        }
    }

    private float getFloat(String key, float defaultValue) {
        String value = this.props.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Float.parseFloat(value.trim());
        }
        catch (NumberFormatException exception) {
            LOGGER.log(Level.FINE, "Invalid float for key '" + key + "': " + value, exception);
            return defaultValue;
        }
    }

    private long getLong(String key, long defaultValue) {
        String value = this.props.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        }
        catch (NumberFormatException exception) {
            LOGGER.log(Level.FINE, "Invalid long for key '" + key + "': " + value, exception);
            return defaultValue;
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
