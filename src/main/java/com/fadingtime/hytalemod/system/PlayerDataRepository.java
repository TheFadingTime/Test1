package com.fadingtime.hytalemod.system;

import com.fadingtime.hytalemod.persistence.PlayerStateStore;
import com.fadingtime.hytalemod.persistence.PlayerStateStoreManager;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class PlayerDataRepository {
    private final PlayerStateStoreManager stateStoreManager;
    private volatile LevelingSystem levelingSystem;
    private volatile PowerUpApplicator powerUpApplicator;
    private final ConcurrentMap<UUID, LevelingSystem.LevelProgress> levelByPlayer = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, PowerUpApplicator.PowerState> powerByPlayer = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, String> worldByPlayer = new ConcurrentHashMap<>();

    public PlayerDataRepository(
        @Nonnull PlayerStateStoreManager stateStoreManager,
        @Nonnull LevelingSystem levelingSystem,
        @Nonnull PowerUpApplicator powerUpApplicator
    ) {
        this.stateStoreManager = stateStoreManager;
        this.levelingSystem = levelingSystem;
        this.powerUpApplicator = powerUpApplicator;
    }

    /**
     * Swaps the formula holders used when restoring/creating player state.
     * Called from the config file watcher thread after a hot-reload.
     * volatile fields guarantee the game thread sees the new references.
     */
    public void updateSystems(@Nonnull LevelingSystem levelingSystem, @Nonnull PowerUpApplicator powerUpApplicator) {
        this.levelingSystem = levelingSystem;
        this.powerUpApplicator = powerUpApplicator;
    }

    public void loadStateIfMissing(@Nonnull UUID playerId, @Nullable String worldKey) {
        String safeWorldKey = worldKey == null ? "default" : worldKey;
        String currentWorld = this.worldByPlayer.get(playerId);

        if (currentWorld == null || !currentWorld.equals(safeWorldKey)) {
            PlayerStateStore store = this.stateStoreManager.getStore(safeWorldKey);
            PlayerStateStore.PlayerLevelData levelData = store.loadLevel(playerId);
            PlayerStateStore.PlayerPowerData powerData = store.loadPower(playerId);
            this.levelByPlayer.put(playerId, this.levelingSystem.restoreProgress(levelData.level, levelData.essenceProgress, levelData.lastStoreLevel));
            this.powerByPlayer.put(playerId, this.powerUpApplicator.fromPersisted(powerData));
            this.worldByPlayer.put(playerId, safeWorldKey);
            return;
        }

        if (!this.levelByPlayer.containsKey(playerId)) {
            PlayerStateStore.PlayerLevelData levelData = this.stateStoreManager.getStore(safeWorldKey).loadLevel(playerId);
            this.levelByPlayer.put(playerId, this.levelingSystem.restoreProgress(levelData.level, levelData.essenceProgress, levelData.lastStoreLevel));
        }
        if (!this.powerByPlayer.containsKey(playerId)) {
            PlayerStateStore.PlayerPowerData powerData = this.stateStoreManager.getStore(safeWorldKey).loadPower(playerId);
            this.powerByPlayer.put(playerId, this.powerUpApplicator.fromPersisted(powerData));
        }
    }

    public void savePlayerState(@Nonnull UUID playerId, @Nonnull String worldKey) {
        PlayerStateStore store = this.stateStoreManager.getStore(worldKey);
        LevelingSystem.LevelProgress level = this.levelByPlayer.get(playerId);
        if (level != null) {
            store.saveLevel(playerId, level.level(), level.essenceProgress(), level.lastStoreLevel());
        }
        PowerUpApplicator.PowerState power = this.powerByPlayer.get(playerId);
        if (power != null) {
            store.savePower(
                playerId,
                power.projectileCount(),
                power.fireRateMultiplier(),
                power.pickupRangeBonus(),
                power.bounceBonus(),
                power.weaponDamageRank(),
                power.healthRank(),
                power.speedRank(),
                power.luckyRank(),
                power.projectileRainUsed()
            );
        }
    }

    @Nonnull
    public PlayerStateStore.PlayerLevelData loadLevelData(@Nonnull UUID playerId, @Nonnull String worldKey) {
        return this.stateStoreManager.getStore(worldKey).loadLevel(playerId);
    }

    @Nonnull
    public PlayerStateStore.PlayerPowerData loadPowerData(@Nonnull UUID playerId, @Nonnull String worldKey) {
        return this.stateStoreManager.getStore(worldKey).loadPower(playerId);
    }

    public void saveLevelData(@Nonnull UUID playerId, @Nonnull String worldKey, int level, int essenceProgress, int lastStoreLevel) {
        this.stateStoreManager.getStore(worldKey).saveLevel(playerId, level, essenceProgress, lastStoreLevel);
    }

    public void savePowerData(
        @Nonnull UUID playerId,
        @Nonnull String worldKey,
        int projectileCount,
        float fireRateMultiplier,
        float pickupRangeBonus,
        int bounceBonus,
        int weaponDamageRank,
        int healthRank,
        int speedRank,
        int luckyRank,
        boolean projectileRainUsed
    ) {
        this.stateStoreManager.getStore(worldKey).savePower(
            playerId,
            projectileCount,
            fireRateMultiplier,
            pickupRangeBonus,
            bounceBonus,
            weaponDamageRank,
            healthRank,
            speedRank,
            luckyRank,
            projectileRainUsed
        );
    }

    @Nonnull
    public LevelingSystem.LevelProgress getOrCreateLevel(@Nonnull UUID playerId) {
        return this.levelByPlayer.computeIfAbsent(playerId, id -> this.levelingSystem.newProgress());
    }

    @Nonnull
    public PowerUpApplicator.PowerState getOrCreatePower(@Nonnull UUID playerId) {
        return this.powerByPlayer.computeIfAbsent(playerId, id -> this.powerUpApplicator.newState());
    }

    @Nullable
    public LevelingSystem.LevelProgress getLevel(@Nonnull UUID playerId) {
        return this.levelByPlayer.get(playerId);
    }

    @Nullable
    public PowerUpApplicator.PowerState getPower(@Nonnull UUID playerId) {
        return this.powerByPlayer.get(playerId);
    }

    @Nullable
    public String getLoadedWorld(@Nonnull UUID playerId) {
        return this.worldByPlayer.get(playerId);
    }

    public void clearTransient(@Nonnull UUID playerId) {
        this.levelByPlayer.remove(playerId);
        this.powerByPlayer.remove(playerId);
        this.worldByPlayer.remove(playerId);
    }

    public void resetProgressForPlayers(@Nonnull Iterable<UUID> playerIds, @Nonnull String worldKey) {
        PlayerStateStore store = this.stateStoreManager.getStore(worldKey);
        for (UUID playerId : playerIds) {
            if (playerId == null) {
                continue;
            }
            clearTransient(playerId);
            store.saveLevel(playerId, 1, 0, 0);
            store.savePower(playerId, 0, 1.0f, 0.0f, 0, 0, 0, 0, 0, false);
        }
    }
}
