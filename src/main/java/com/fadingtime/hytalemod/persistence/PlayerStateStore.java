/*
 * Decompiled with CFR 0.152.
 */
package com.fadingtime.hytalemod.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
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

    public PlayerStateStore(Path path) {
        this.filePath = path;
        try {
            Path path2 = this.filePath.getParent();
            if (path2 != null) {
                Files.createDirectories(path2, new FileAttribute[0]);
            }
        }
        catch (IOException iOException) {
            LOGGER.log(Level.WARNING, "Failed to create player state directory for " + String.valueOf(this.filePath), iOException);
        }
        this.load();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void load() {
        this.lock.writeLock().lock();
        try {
            this.props.clear();
            if (!Files.exists(this.filePath, new LinkOption[0])) {
                return;
            }
            try (InputStream inputStream = Files.newInputStream(this.filePath, StandardOpenOption.READ);){
                this.props.load(inputStream);
            }
            catch (IOException iOException) {
                LOGGER.log(Level.WARNING, "Failed to load player state file: " + String.valueOf(this.filePath), iOException);
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

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public PlayerLevelData loadLevel(UUID uUID) {
        this.lock.readLock().lock();
        try {
            String string = PlayerStateStore.playerPrefix(uUID);
            int n = this.getInt(string + "level", 1);
            int n2 = this.getInt(string + "essence", 0);
            int n3 = this.getInt(string + "lastStoreLevel", 0);
            PlayerLevelData playerLevelData = new PlayerLevelData(n, n2, n3);
            return playerLevelData;
        }
        finally {
            this.lock.readLock().unlock();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void saveLevel(UUID uUID, int n, int n2, int n3) {
        this.lock.writeLock().lock();
        try {
            String string = PlayerStateStore.playerPrefix(uUID);
            this.props.setProperty(string + "level", Integer.toString(n));
            this.props.setProperty(string + "essence", Integer.toString(n2));
            this.props.setProperty(string + "lastStoreLevel", Integer.toString(n3));
            this.saveUnsafe();
        }
        finally {
            this.lock.writeLock().unlock();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public PlayerPowerData loadPower(UUID uUID) {
        this.lock.readLock().lock();
        try {
            String string = PlayerStateStore.playerPrefix(uUID);
            int n = this.getInt(string + "projectiles", 0);
            float f = this.getFloat(string + "fireRate", 1.0f);
            float f2 = this.getFloat(string + "pickupRange", 0.0f);
            int n2 = this.getInt(string + "bounceBonus", 0);
            int n3 = this.getInt(string + "weaponDamageRank", 0);
            int n4 = this.getInt(string + "healthRank", 0);
            int n5 = this.getInt(string + "speedRank", 0);
            int n6 = this.getInt(string + "luckyRank", 0);
            int n7 = this.getInt(string + "skipRank", 0);
            boolean bl = this.getInt(string + "projectileRainUsed", 0) > 0;
            PlayerPowerData playerPowerData = new PlayerPowerData(n, f, f2, n2, n3, n4, n5, n6, n7, bl);
            return playerPowerData;
        }
        finally {
            this.lock.readLock().unlock();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void savePower(UUID uUID, int n, float f, float f2, int n2, int n3, int n4, int n5, int n6, int n7, boolean bl) {
        this.lock.writeLock().lock();
        try {
            String string = PlayerStateStore.playerPrefix(uUID);
            this.props.setProperty(string + "projectiles", Integer.toString(n));
            this.props.setProperty(string + "fireRate", Float.toString(f));
            this.props.setProperty(string + "pickupRange", Float.toString(f2));
            this.props.setProperty(string + "bounceBonus", Integer.toString(n2));
            this.props.setProperty(string + "weaponDamageRank", Integer.toString(n3));
            this.props.setProperty(string + "healthRank", Integer.toString(n4));
            this.props.setProperty(string + "speedRank", Integer.toString(n5));
            this.props.setProperty(string + "luckyRank", Integer.toString(n6));
            this.props.setProperty(string + "skipRank", Integer.toString(n7));
            this.props.setProperty(string + "projectileRainUsed", bl ? "1" : "0");
            this.saveUnsafe();
        }
        finally {
            this.lock.writeLock().unlock();
        }
    }

    public int loadWaveCount(UUID uUID) {
        this.lock.readLock().lock();
        try {
            int n = this.getInt(PlayerStateStore.waveCountKey(uUID), 0);
            return n;
        }
        finally {
            this.lock.readLock().unlock();
        }
    }

    public void saveWaveCount(UUID uUID, int n) {
        this.lock.writeLock().lock();
        try {
            this.props.setProperty(PlayerStateStore.waveCountKey(uUID), Integer.toString(n));
            this.saveUnsafe();
        }
        finally {
            this.lock.writeLock().unlock();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public long loadNextSpawnAt(UUID uUID) {
        this.lock.readLock().lock();
        try {
            long l = this.getLong(PlayerStateStore.waveNextSpawnKey(uUID), 0L);
            return l;
        }
        finally {
            this.lock.readLock().unlock();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void saveNextSpawnAt(UUID uUID, long l) {
        this.lock.writeLock().lock();
        try {
            this.props.setProperty(PlayerStateStore.waveNextSpawnKey(uUID), Long.toString(l));
            this.saveUnsafe();
        }
        finally {
            this.lock.writeLock().unlock();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public UUID loadBossId(UUID uUID) {
        this.lock.readLock().lock();
        try {
            String string = this.props.getProperty(PlayerStateStore.bossIdKey(uUID));
            if (string == null || string.isBlank()) {
                UUID uUID2 = null;
                return uUID2;
            }
            UUID uUID3 = UUID.fromString(string.trim());
            return uUID3;
        }
        finally {
            this.lock.readLock().unlock();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void saveBossId(UUID uUID, UUID uUID2) {
        this.lock.writeLock().lock();
        try {
            String string = PlayerStateStore.bossIdKey(uUID);
            if (uUID2 == null) {
                this.props.remove(string);
            } else {
                this.props.setProperty(string, uUID2.toString());
            }
            this.saveUnsafe();
        }
        finally {
            this.lock.writeLock().unlock();
        }
    }

    private void saveUnsafe() {
        try {
            Path path = this.filePath.getParent();
            if (path != null) {
                Files.createDirectories(path, new FileAttribute[0]);
            }
            try (OutputStream outputStream = Files.newOutputStream(this.filePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);){
                this.props.store(outputStream, "Hytale Survivors player state");
            }
        }
        catch (IOException iOException) {
            LOGGER.log(Level.WARNING, "Failed to save player state file: " + String.valueOf(this.filePath), iOException);
        }
    }

    private static String playerPrefix(UUID uUID) {
        return "player." + String.valueOf(uUID) + ".";
    }

    private static String waveCountKey(UUID uUID) {
        return "wave." + String.valueOf(uUID);
    }

    private static String waveNextSpawnKey(UUID uUID) {
        return "wave.next." + String.valueOf(uUID);
    }

    private static String bossIdKey(UUID uUID) {
        return "boss." + String.valueOf(uUID);
    }

    private int getInt(String string, int n) {
        String string2 = this.props.getProperty(string);
        if (string2 == null) {
            return n;
        }
        try {
            return Integer.parseInt(string2.trim());
        }
        catch (NumberFormatException numberFormatException) {
            return n;
        }
    }

    private float getFloat(String string, float f) {
        String string2 = this.props.getProperty(string);
        if (string2 == null) {
            return f;
        }
        try {
            return Float.parseFloat(string2.trim());
        }
        catch (NumberFormatException numberFormatException) {
            return f;
        }
    }

    private long getLong(String string, long l) {
        String string2 = this.props.getProperty(string);
        if (string2 == null) {
            return l;
        }
        try {
            return Long.parseLong(string2.trim());
        }
        catch (NumberFormatException numberFormatException) {
            return l;
        }
    }

    public static final class PlayerLevelData {
        public final int level;
        public final int essenceProgress;
        public final int lastStoreLevel;

        public PlayerLevelData(int n, int n2, int n3) {
            this.level = n;
            this.essenceProgress = n2;
            this.lastStoreLevel = n3;
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
        public final int skipRank;
        public final boolean projectileRainUsed;

        public PlayerPowerData(int n, float f, float f2, int n2, int n3, int n4, int n5, int n6, int n7, boolean bl) {
            this.projectileCount = n;
            this.fireRateMultiplier = f;
            this.pickupRangeBonus = f2;
            this.bounceBonus = n2;
            this.weaponDamageRank = n3;
            this.healthRank = n4;
            this.speedRank = n5;
            this.luckyRank = n6;
            this.skipRank = n7;
            this.projectileRainUsed = bl;
        }
    }
}
