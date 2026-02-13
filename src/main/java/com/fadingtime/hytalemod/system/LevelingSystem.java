/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nonnull
 */
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

    public LevelingSystem(int n, int n2, int n3, int n4, int n5, int n6) {
        this.essencePerLevelBase = Math.max(1, n);
        this.essencePerLevelIncrement = Math.max(0, n2);
        this.essencePerLevelMax = Math.max(1, n3);
        this.storeOpenLevelStart = Math.max(1, n4);
        this.storeOpenLevelInterval = Math.max(1, n5);
        this.storeOpenLevelMax = Math.max(this.storeOpenLevelStart, n6);
    }

    public static LevelingSystem fromConfig(@Nonnull LifeEssenceConfig lifeEssenceConfig) {
        if (lifeEssenceConfig == null) {
            throw new IllegalArgumentException("config cannot be null");
        }
        return new LevelingSystem(lifeEssenceConfig.essencePerLevelBase, lifeEssenceConfig.essencePerLevelIncrement, lifeEssenceConfig.essencePerLevelMax, lifeEssenceConfig.storeOpenLevelStart, lifeEssenceConfig.storeOpenLevelInterval, lifeEssenceConfig.storeOpenLevelMax);
    }

    public LevelProgress newProgress() {
        return new LevelProgress();
    }

    public LevelProgress restoreProgress(int n, int n2, int n3) {
        LevelProgress levelProgress = new LevelProgress();
        levelProgress.level = Math.max(1, n);
        levelProgress.essenceProgress = Math.max(0, n2);
        levelProgress.lastStoreLevel = Math.max(0, n3);
        int n4 = this.getEssenceRequiredForLevel(levelProgress.level);
        if (levelProgress.essenceProgress >= n4) {
            levelProgress.essenceProgress = Math.max(0, n4 - 1);
        }
        return levelProgress;
    }

    public int addEssence(@Nonnull LevelProgress levelProgress, int n) {
        if (levelProgress == null) {
            throw new IllegalArgumentException("progress cannot be null");
        }
        if (n <= 0) {
            return levelProgress.level;
        }
        int n2 = levelProgress.level;
        int n3 = levelProgress.essenceProgress + n;
        while (n3 >= this.getEssenceRequiredForLevel(levelProgress.level)) {
            n3 -= this.getEssenceRequiredForLevel(levelProgress.level);
            ++levelProgress.level;
        }
        levelProgress.essenceProgress = n3;
        return n2;
    }

    public boolean shouldOpenStore(@Nonnull LevelProgress levelProgress) {
        if (levelProgress == null) {
            throw new IllegalArgumentException("progress cannot be null");
        }
        return levelProgress.lastStoreLevel < this.getLatestEligibleStoreLevel(levelProgress.level);
    }

    public void markStoreOpened(@Nonnull LevelProgress levelProgress) {
        if (levelProgress == null) {
            throw new IllegalArgumentException("progress cannot be null");
        }
        int n = this.getLatestEligibleStoreLevel(levelProgress.level);
        if (n <= 0) {
            return;
        }
        levelProgress.lastStoreLevel = Math.max(levelProgress.lastStoreLevel, n);
    }

    public int getEssenceRequired(@Nonnull LevelProgress levelProgress) {
        if (levelProgress == null) {
            throw new IllegalArgumentException("progress cannot be null");
        }
        return this.getEssenceRequiredForLevel(levelProgress.level);
    }

    public int getEssenceRequiredForLevel(int n) {
        int n2 = Math.max(1, n);
        return Math.min(this.essencePerLevelMax, this.essencePerLevelBase + (n2 - 1) * this.essencePerLevelIncrement);
    }

    public int getLatestEligibleStoreLevel(int n) {
        if (n < this.storeOpenLevelStart) {
            return 0;
        }
        int n2 = Math.min(n, this.storeOpenLevelMax);
        int n3 = (n2 - this.storeOpenLevelStart) / this.storeOpenLevelInterval;
        return this.storeOpenLevelStart + n3 * this.storeOpenLevelInterval;
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

