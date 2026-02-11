package com.fadingtime.hytalemod.ui;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;

public class BossBarHud
extends CustomUIHud {
    private static final int ESSENCE_PER_LEVEL = 10;
    @Nonnull
    private String bossName;
    private float healthRatio;
    private int level = 1;
    private int essenceCount;
    private int essenceRequired = 10;
    private boolean bossVisible;

    public BossBarHud(@Nonnull PlayerRef playerRef, @Nonnull String bossName, float healthRatio) {
        super(playerRef);
        this.bossName = bossName;
        this.healthRatio = healthRatio;
    }

    public void updateHealth(float healthRatio) {
        this.healthRatio = healthRatio;
        UICommandBuilder builder = new UICommandBuilder();
        builder.set("#ProgressBar.Value", healthRatio);
        this.update(false, builder);
    }

    public void updateBossName(@Nonnull String bossName) {
        this.bossName = bossName;
        UICommandBuilder builder = new UICommandBuilder();
        builder.set("#BossName.TextSpans", Message.raw((String)bossName));
        this.update(false, builder);
    }

    public void updateLevel(int level, int essenceCount, int essenceRequired) {
        this.level = Math.max(1, level);
        this.essenceCount = Math.max(0, essenceCount);
        this.essenceRequired = Math.max(10, essenceRequired);
        UICommandBuilder builder = new UICommandBuilder();
        builder.set("#LevelLabel.Text", String.format("LEVEL: %d ESSENCE: %d / %d", this.level, this.essenceCount, this.essenceRequired));
        builder.set("#LevelProgressBar.Value", this.calculateProgress());
        this.update(false, builder);
    }

    public void setBossVisible(boolean visible) {
        this.bossVisible = visible;
        UICommandBuilder builder = new UICommandBuilder();
        builder.set("#BossBar.Visible", visible);
        this.update(false, builder);
    }

    public void setLevelHudVisible(boolean visible) {
        UICommandBuilder builder = new UICommandBuilder();
        builder.set("#LevelProgressHud.Visible", visible);
        this.update(false, builder);
    }

    public boolean isBossVisible() {
        return this.bossVisible;
    }

    protected void build(@Nonnull UICommandBuilder builder) {
        builder.append("BossBar.ui");
        builder.append("HUD/LevelProgress.ui");
        builder.set("#BossName.TextSpans", Message.raw((String)this.bossName));
        builder.set("#ProgressBar.Value", this.healthRatio);
        builder.set("#LevelLabel.Text", String.format("LEVEL: %d ESSENCE: %d / %d", this.level, this.essenceCount, this.essenceRequired));
        builder.set("#LevelProgressBar.Value", this.calculateProgress());
        builder.set("#BossBar.Visible", this.bossVisible);
        builder.set("#LevelProgressHud.Visible", true);
    }

    private float calculateProgress() {
        if (this.essenceRequired <= 0) {
            return 0.0f;
        }
        return Math.min(1.0f, Math.max(0.0f, (float)this.essenceCount / (float)this.essenceRequired));
    }
}
