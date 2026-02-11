package com.fadingtime.hytalemod.system;

import com.fadingtime.hytalemod.config.LifeEssenceConfig;
import javax.annotation.Nonnull;

public final class LevelingSystem {
    private final int essencePerLevelBase;
    private final int essencePerLevelIncrement;
    private final int essencePerLevelMax;
    private final int storeOpenLevelStart;
    private final int storeOpenLevelInterval;
    private final int storeOpenLevelMax;

    public LevelingSystem(
        int essencePerLevelBase,
        int essencePerLevelIncrement,
        int essencePerLevelMax,
        int storeOpenLevelStart,
        int storeOpenLevelInterval,
        int storeOpenLevelMax
    ) {
        // Keep this forgiving so bad config data still results in playable progression.
        this.essencePerLevelBase = Math.max(1, essencePerLevelBase);
        this.essencePerLevelIncrement = Math.max(0, essencePerLevelIncrement);
        this.essencePerLevelMax = Math.max(1, essencePerLevelMax);
        this.storeOpenLevelStart = Math.max(1, storeOpenLevelStart);
        this.storeOpenLevelInterval = Math.max(1, storeOpenLevelInterval);
        this.storeOpenLevelMax = Math.max(this.storeOpenLevelStart, storeOpenLevelMax);
    }

    public static LevelingSystem fromConfig(@Nonnull LifeEssenceConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config cannot be null");
        }
        return new LevelingSystem(
            config.essencePerLevelBase,
            config.essencePerLevelIncrement,
            config.essencePerLevelMax,
            config.storeOpenLevelStart,
            config.storeOpenLevelInterval,
            config.storeOpenLevelMax
        );
    }

    public LevelProgress newProgress() {
        return new LevelProgress();
    }

    public LevelProgress restoreProgress(int level, int essenceProgress, int lastStoreLevel) {
        LevelProgress state = new LevelProgress();
        state.level = Math.max(1, level);
        state.essenceProgress = Math.max(0, essenceProgress);
        state.lastStoreLevel = Math.max(0, lastStoreLevel);
        // Keep persisted progress sane if a bad value sneaks into storage.
        int required = getEssenceRequiredForLevel(state.level);
        if (state.essenceProgress >= required) {
            state.essenceProgress = Math.max(0, required - 1);
        }
        return state;
    }

    public int addEssence(@Nonnull LevelProgress progress, int amount) {
        if (progress == null) {
            throw new IllegalArgumentException("progress cannot be null");
        }
        if (amount <= 0) {
            return progress.level;
        }

        int previousLevel = progress.level;
        int total = progress.essenceProgress + amount;

        while (total >= getEssenceRequiredForLevel(progress.level)) {
            total -= getEssenceRequiredForLevel(progress.level);
            progress.level++;
        }
        progress.essenceProgress = total;
        return previousLevel;
    }

    public boolean shouldOpenStore(@Nonnull LevelProgress progress) {
        if (progress == null) {
            throw new IllegalArgumentException("progress cannot be null");
        }
        return progress.lastStoreLevel < getLatestEligibleStoreLevel(progress.level);
    }

    public void markStoreOpened(@Nonnull LevelProgress progress) {
        if (progress == null) {
            throw new IllegalArgumentException("progress cannot be null");
        }
        int latestEligible = getLatestEligibleStoreLevel(progress.level);
        if (latestEligible <= 0) {
            return;
        }
        // Store unlocks at checkpoints, not every level, so this stores only the last checkpoint.
        progress.lastStoreLevel = Math.max(progress.lastStoreLevel, latestEligible);
    }

    public int getEssenceRequired(@Nonnull LevelProgress progress) {
        if (progress == null) {
            throw new IllegalArgumentException("progress cannot be null");
        }
        return getEssenceRequiredForLevel(progress.level);
    }

    public int getEssenceRequiredForLevel(int level) {
        int safeLevel = Math.max(1, level);
        return Math.min(this.essencePerLevelMax, this.essencePerLevelBase + (safeLevel - 1) * this.essencePerLevelIncrement);
    }

    public int getLatestEligibleStoreLevel(int level) {
        if (level < this.storeOpenLevelStart) {
            return 0;
        }
        int cappedLevel = Math.min(level, this.storeOpenLevelMax);
        int steps = (cappedLevel - this.storeOpenLevelStart) / this.storeOpenLevelInterval;
        return this.storeOpenLevelStart + steps * this.storeOpenLevelInterval;
    }

    public static final class LevelProgress {
        private int level = 1;
        private int essenceProgress = 0;
        private int lastStoreLevel = 0;

        public int level() {
            return this.level;
        }

        public int essenceProgress() {
            return this.essenceProgress;
        }

        public int lastStoreLevel() {
            return this.lastStoreLevel;
        }
    }
}
