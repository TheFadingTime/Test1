/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.server.core.Message
 *  com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud
 *  com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  javax.annotation.Nonnull
 */
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

    public BossBarHud(@Nonnull PlayerRef playerRef, @Nonnull String string, float f) {
        super(playerRef);
        this.bossName = string;
        this.healthRatio = f;
    }

    public void updateHealth(float f) {
        this.healthRatio = f;
        UICommandBuilder uICommandBuilder = new UICommandBuilder();
        uICommandBuilder.set("#ProgressBar.Value", f);
        this.update(false, uICommandBuilder);
    }

    public void updateBossName(@Nonnull String string) {
        this.bossName = string;
        UICommandBuilder uICommandBuilder = new UICommandBuilder();
        uICommandBuilder.set("#BossName.TextSpans", Message.raw((String)string));
        this.update(false, uICommandBuilder);
    }

    public void updateLevel(int n, int n2, int n3) {
        this.level = Math.max(1, n);
        this.essenceCount = Math.max(0, n2);
        this.essenceRequired = Math.max(10, n3);
        UICommandBuilder uICommandBuilder = new UICommandBuilder();
        uICommandBuilder.set("#LevelLabel.Text", String.format("LEVEL: %d ESSENCE: %d / %d", this.level, this.essenceCount, this.essenceRequired));
        uICommandBuilder.set("#LevelProgressBar.Value", this.calculateProgress());
        this.update(false, uICommandBuilder);
    }

    public void setBossVisible(boolean bl) {
        this.bossVisible = bl;
        UICommandBuilder uICommandBuilder = new UICommandBuilder();
        uICommandBuilder.set("#BossBar.Visible", bl);
        this.update(false, uICommandBuilder);
    }

    public void setLevelHudVisible(boolean bl) {
        UICommandBuilder uICommandBuilder = new UICommandBuilder();
        uICommandBuilder.set("#LevelProgressHud.Visible", bl);
        this.update(false, uICommandBuilder);
    }

    public boolean isBossVisible() {
        return this.bossVisible;
    }

    protected void build(@Nonnull UICommandBuilder uICommandBuilder) {
        uICommandBuilder.append("BossBar.ui");
        uICommandBuilder.append("HUD/LevelProgress.ui");
        uICommandBuilder.set("#BossName.TextSpans", Message.raw((String)this.bossName));
        uICommandBuilder.set("#ProgressBar.Value", this.healthRatio);
        uICommandBuilder.set("#LevelLabel.Text", String.format("LEVEL: %d ESSENCE: %d / %d", this.level, this.essenceCount, this.essenceRequired));
        uICommandBuilder.set("#LevelProgressBar.Value", this.calculateProgress());
        uICommandBuilder.set("#BossBar.Visible", this.bossVisible);
        uICommandBuilder.set("#LevelProgressHud.Visible", true);
    }

    private float calculateProgress() {
        if (this.essenceRequired <= 0) {
            return 0.0f;
        }
        return Math.min(1.0f, Math.max(0.0f, (float)this.essenceCount / (float)this.essenceRequired));
    }
}

