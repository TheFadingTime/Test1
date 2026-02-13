/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nonnull
 *  javax.annotation.Nullable
 */
package com.fadingtime.hytalemod.system;

import com.fadingtime.hytalemod.persistence.PlayerStateStore;
import com.fadingtime.hytalemod.persistence.PlayerStateStoreManager;
import com.fadingtime.hytalemod.system.LevelingSystem;
import com.fadingtime.hytalemod.system.PowerUpApplicator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class PlayerDataRepository {
    private final PlayerStateStoreManager stateStoreManager;
    private final LevelingSystem levelingSystem;
    private final PowerUpApplicator powerUpApplicator;
    private final ConcurrentMap<UUID, LevelingSystem.LevelProgress> levelByPlayer = new ConcurrentHashMap<UUID, LevelingSystem.LevelProgress>();
    private final ConcurrentMap<UUID, PowerUpApplicator.PowerState> powerByPlayer = new ConcurrentHashMap<UUID, PowerUpApplicator.PowerState>();
    private final ConcurrentMap<UUID, String> worldByPlayer = new ConcurrentHashMap<UUID, String>();

    public PlayerDataRepository(@Nonnull PlayerStateStoreManager playerStateStoreManager, @Nonnull LevelingSystem levelingSystem, @Nonnull PowerUpApplicator powerUpApplicator) {
        this.stateStoreManager = playerStateStoreManager;
        this.levelingSystem = levelingSystem;
        this.powerUpApplicator = powerUpApplicator;
    }

    public void loadStateIfMissing(@Nonnull UUID uUID, @Nullable String string) {
        Object object;
        String string2 = string == null ? "default" : string;
        String string3 = (String)this.worldByPlayer.get(uUID);
        if (string3 == null || !string3.equals(string2)) {
            PlayerStateStore playerStateStore = this.stateStoreManager.getStore(string2);
            PlayerStateStore.PlayerLevelData playerLevelData = playerStateStore.loadLevel(uUID);
            PlayerStateStore.PlayerPowerData playerPowerData = playerStateStore.loadPower(uUID);
            this.levelByPlayer.put(uUID, this.levelingSystem.restoreProgress(playerLevelData.level, playerLevelData.essenceProgress, playerLevelData.lastStoreLevel));
            this.powerByPlayer.put(uUID, this.powerUpApplicator.fromPersisted(playerPowerData));
            this.worldByPlayer.put(uUID, string2);
            return;
        }
        if (!this.levelByPlayer.containsKey(uUID)) {
            object = this.stateStoreManager.getStore(string2).loadLevel(uUID);
            this.levelByPlayer.put(uUID, this.levelingSystem.restoreProgress(((PlayerStateStore.PlayerLevelData)object).level, ((PlayerStateStore.PlayerLevelData)object).essenceProgress, ((PlayerStateStore.PlayerLevelData)object).lastStoreLevel));
        }
        if (!this.powerByPlayer.containsKey(uUID)) {
            object = this.stateStoreManager.getStore(string2).loadPower(uUID);
            this.powerByPlayer.put(uUID, this.powerUpApplicator.fromPersisted((PlayerStateStore.PlayerPowerData)object));
        }
    }

    public void savePlayerState(@Nonnull UUID uUID, @Nonnull String string) {
        PowerUpApplicator.PowerState powerState;
        PlayerStateStore playerStateStore = this.stateStoreManager.getStore(string);
        LevelingSystem.LevelProgress levelProgress = (LevelingSystem.LevelProgress)this.levelByPlayer.get(uUID);
        if (levelProgress != null) {
            playerStateStore.saveLevel(uUID, levelProgress.level(), levelProgress.essenceProgress(), levelProgress.lastStoreLevel());
        }
        if ((powerState = (PowerUpApplicator.PowerState)this.powerByPlayer.get(uUID)) != null) {
            playerStateStore.savePower(uUID, powerState.projectileCount(), powerState.fireRateMultiplier(), powerState.pickupRangeBonus(), powerState.bounceBonus(), powerState.weaponDamageRank(), powerState.healthRank(), powerState.speedRank(), powerState.luckyRank(), powerState.skipRank(), powerState.projectileRainUsed());
        }
    }

    @Nonnull
    public PlayerStateStore.PlayerLevelData loadLevelData(@Nonnull UUID uUID, @Nonnull String string) {
        return this.stateStoreManager.getStore(string).loadLevel(uUID);
    }

    @Nonnull
    public PlayerStateStore.PlayerPowerData loadPowerData(@Nonnull UUID uUID, @Nonnull String string) {
        return this.stateStoreManager.getStore(string).loadPower(uUID);
    }

    public void saveLevelData(@Nonnull UUID uUID, @Nonnull String string, int n, int n2, int n3) {
        this.stateStoreManager.getStore(string).saveLevel(uUID, n, n2, n3);
    }

    public void savePowerData(@Nonnull UUID uUID, @Nonnull String string, int n, float f, float f2, int n2, int n3, int n4, int n5, int n6, int n7, boolean bl) {
        this.stateStoreManager.getStore(string).savePower(uUID, n, f, f2, n2, n3, n4, n5, n6, n7, bl);
    }

    @Nonnull
    public LevelingSystem.LevelProgress getOrCreateLevel(@Nonnull UUID uUID2) {
        return this.levelByPlayer.computeIfAbsent(uUID2, uUID -> this.levelingSystem.newProgress());
    }

    @Nonnull
    public PowerUpApplicator.PowerState getOrCreatePower(@Nonnull UUID uUID2) {
        return this.powerByPlayer.computeIfAbsent(uUID2, uUID -> this.powerUpApplicator.newState());
    }

    @Nullable
    public LevelingSystem.LevelProgress getLevel(@Nonnull UUID uUID) {
        return (LevelingSystem.LevelProgress)this.levelByPlayer.get(uUID);
    }

    @Nullable
    public PowerUpApplicator.PowerState getPower(@Nonnull UUID uUID) {
        return (PowerUpApplicator.PowerState)this.powerByPlayer.get(uUID);
    }

    @Nullable
    public String getLoadedWorld(@Nonnull UUID uUID) {
        return (String)this.worldByPlayer.get(uUID);
    }

    public void clearTransient(@Nonnull UUID uUID) {
        this.levelByPlayer.remove(uUID);
        this.powerByPlayer.remove(uUID);
        this.worldByPlayer.remove(uUID);
    }

    public void resetProgressForPlayers(@Nonnull Iterable<UUID> iterable, @Nonnull String string) {
        PlayerStateStore playerStateStore = this.stateStoreManager.getStore(string);
        for (UUID uUID : iterable) {
            if (uUID == null) continue;
            this.clearTransient(uUID);
            playerStateStore.saveLevel(uUID, 1, 0, 0);
            playerStateStore.savePower(uUID, 0, 1.0f, 0.0f, 0, 0, 0, 0, 0, 0, false);
        }
    }
}

