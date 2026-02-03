package com.fadingtime.hytalemod;

import java.util.List;

public final class MobSpawnConfig {
    public static final int SPAWN_COUNT = 5;
    public static final long SPAWN_INTERVAL_MS = 30_000L;
    public static final double MIN_SPAWN_RADIUS = 8.0;
    public static final double MAX_SPAWN_RADIUS = 16.0;
    public static final double SPAWN_Y_OFFSET = 0.5;
    public static final int BOSS_WAVE_INTERVAL = 3;
    public static final String BOSS_ROLE = "Skeleton";
    public static final float BOSS_SCALE = 4.0F;

    public static final List<String> HOSTILE_ROLES = List.of(
        "Skeleton_Burnt_Wizard",
        "Skeleton",
        "Zombie"
    );

    public static final String LIFE_ESSENCE_ITEM_ID = "Ingredient_Life_Essence";

    private MobSpawnConfig() {
    }
}
