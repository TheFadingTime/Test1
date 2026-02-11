package com.fadingtime.hytalemod.config;

import java.util.List;

public final class MobSpawnConfig {
    public static final int SPAWN_COUNT = 5;
    public static final int SPAWN_COUNT_PER_WAVE = 5;
    public static final int MAX_MOBS_PER_WAVE = 20;
    public static final long SPAWN_INTERVAL_MS = 10000L;
    public static final double MIN_SPAWN_RADIUS = 10.0;
    public static final double MAX_SPAWN_RADIUS = 20.0;
    public static final double SPAWN_Y_OFFSET = 0.5;
    public static final float MAX_MOB_HEALTH = 50.0f;
    public static final int BOSS_WAVE_INTERVAL = 10;
    public static final String BOSS_ROLE = "Skeleton";
    public static final float BOSS_SCALE = 4.0f;
    public static final float BOSS_HEALTH_BASE = 500.0f;
    public static final float BOSS_HEALTH_PER_WAVE = 150.0f;
    public static final int TOAD_BOSS_WAVE_START = 10;
    public static final int TOAD_BOSS_WAVE_INTERVAL = 10;
    public static final String TOAD_BOSS_ROLE = "Toad_Rhino_Magma";
    public static final float TOAD_BOSS_SCALE = 2.0f;
    public static final float TOAD_BOSS_HEALTH_BASE = 200.0f;
    public static final float TOAD_BOSS_HEALTH_PER_SPAWN = 200.0f;
    public static final int WRAITH_BOSS_WAVE_FIRST = 40;
    public static final int WRAITH_BOSS_WAVE_SECOND = 50;
    public static final String WRAITH_BOSS_ROLE = "Wraith";
    public static final float WRAITH_BOSS_SCALE = 2.0f;
    public static final float WRAITH_BOSS_HEALTH_BASE = 1000.0f;
    public static final float WRAITH_BOSS_HEALTH_PER_SPAWN = 500.0f;
    public static final float WAVE_HEALTH_BONUS = 10.0f;
    public static final List<String> HOSTILE_ROLES = List.of("Spirit_Ember", "Skeleton", "Zombie");
    public static final String LIFE_ESSENCE_ITEM_ID = "Ingredient_Life_Essence";

    private MobSpawnConfig() {
    }
}
