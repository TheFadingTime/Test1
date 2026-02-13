/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 */
package com.fadingtime.hytalemod.config;

import com.fadingtime.hytalemod.config.ConfigLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

public final class ConfigManager {
    private static final SpawnConfig DEFAULTS = SpawnConfig.defaults();
    private static final AtomicReference<SpawnConfig> CURRENT = new AtomicReference<SpawnConfig>(DEFAULTS);

    private ConfigManager() {
    }

    public static synchronized void load(Path path, Logger logger) {
        SpawnConfig spawnConfig = DEFAULTS;
        try {
            spawnConfig = ConfigLoader.load(path, logger, DEFAULTS);
        }
        catch (IllegalArgumentException illegalArgumentException) {
            throw illegalArgumentException;
        }
        catch (RuntimeException runtimeException) {
            logger.log(Level.WARNING, "Spawn config load failed, using defaults.", runtimeException);
        }
        if (spawnConfig == null) {
            logger.warning("Spawn config loader returned null. Defaults applied.");
            spawnConfig = DEFAULTS;
        }
        ConfigManager.apply(spawnConfig);
    }

    @Deprecated
    public static synchronized void initialize(Path path, Logger logger) {
        ConfigManager.load(path, logger);
    }

    public static SpawnConfig get() {
        return CURRENT.get();
    }

    public static List<BossDefinition> getBossDefinitions() {
        return ConfigManager.CURRENT.get().bosses;
    }

    @Nullable
    public static BossDefinition findBossForWave(int n) {
        for (BossDefinition bossDefinition : ConfigManager.CURRENT.get().bosses) {
            if (!bossDefinition.shouldSpawnAtWave(n)) continue;
            return bossDefinition;
        }
        return null;
    }

    private static void apply(SpawnConfig spawnConfig) {
        CURRENT.set(spawnConfig);
    }

    private static List<BossDefinition> defaultBosses() {
        ArrayList<BossDefinition> arrayList = new ArrayList<BossDefinition>();
        arrayList.add(new BossDefinition("skeleton", "Boss_Skeleton", 5, 10, List.of(), 4.0f, 500.0f, 150.0f, HealthMode.WAVE_NUMBER, "BossWaveHealth"));
        arrayList.add(new BossDefinition("toad", "Boss_Toad_Rhino_Magma", 10, 10, List.of(), 2.0f, 400.0f, 200.0f, HealthMode.SPAWN_INDEX, "ToadBossHealth"));
        arrayList.add(new BossDefinition("wraith", "Boss_Wraith", 40, 0, List.of(Integer.valueOf(40), Integer.valueOf(50)), 2.0f, 1000.0f, 500.0f, HealthMode.SPAWN_INDEX, "WraithBossHealth"));
        return arrayList;
    }

    public static final class SpawnConfig {
        public final int spawnCount;
        public final int spawnCountPerWave;
        public final int maxMobsPerWave;
        public final long spawnIntervalMs;
        public final double minSpawnRadius;
        public final double maxSpawnRadius;
        public final double spawnYOffset;
        public final float maxMobHealth;
        public final List<String> hostileRoles;
        public final String lifeEssenceItemId;
        public final List<BossDefinition> bosses;

        SpawnConfig(int n, int n2, int n3, long l, double d, double d2, double d3, float f, List<String> list, String string, List<BossDefinition> list2) {
            this.spawnCount = n;
            this.spawnCountPerWave = n2;
            this.maxMobsPerWave = n3;
            this.spawnIntervalMs = l;
            this.minSpawnRadius = d;
            this.maxSpawnRadius = d2;
            this.spawnYOffset = d3;
            this.maxMobHealth = f;
            this.hostileRoles = list == null ? List.of() : list;
            this.lifeEssenceItemId = string == null || string.isBlank() ? "Ingredient_Life_Essence" : string;
            this.bosses = list2 == null ? List.of() : list2;
        }

        static SpawnConfig defaults() {
            return new SpawnConfig(5, 5, 20, 10000L, 10.0, 20.0, 0.5, 126.0f, List.of("Wave_Skeleton", "Wave_Zombie_Burnt"), "Ingredient_Life_Essence", ConfigManager.defaultBosses());
        }
    }

    public static final class BossDefinition {
        private final String id;
        private final String role;
        private final int startWave;
        private final int interval;
        private final List<Integer> explicitWaves;
        private final float scale;
        private final float healthBase;
        private final float healthPerStep;
        private final HealthMode healthMode;
        private final String healthModifierId;

        BossDefinition(String string, String string2, int n, int n2, List<Integer> list, float f, float f2, float f3, HealthMode healthMode, String string3) {
            this.id = string == null || string.isBlank() ? "boss" : string.trim().toLowerCase(Locale.ROOT);
            this.role = string2 == null || string2.isBlank() ? "Skeleton" : string2.trim();
            this.startWave = n <= 0 ? 1 : n;
            this.interval = n2 < 0 ? 0 : n2;
            this.explicitWaves = list == null ? List.of() : list;
            this.scale = f > 0.0f ? f : 1.0f;
            this.healthBase = f2 > 0.0f ? f2 : 100.0f;
            this.healthPerStep = f3;
            this.healthMode = healthMode == null ? HealthMode.SPAWN_INDEX : healthMode;
            this.healthModifierId = string3;
        }

        public String id() {
            return this.id;
        }

        public String role() {
            return this.role;
        }

        public int startWave() {
            return this.startWave;
        }

        public int interval() {
            return this.interval;
        }

        public List<Integer> explicitWaves() {
            return this.explicitWaves;
        }

        public float scale() {
            return this.scale;
        }

        public float healthBase() {
            return this.healthBase;
        }

        public float healthPerStep() {
            return this.healthPerStep;
        }

        public HealthMode healthMode() {
            return this.healthMode;
        }

        public String healthModifierId() {
            return this.healthModifierId;
        }

        public boolean shouldSpawnAtWave(int n) {
            if (n <= 0) {
                return false;
            }
            if (!this.explicitWaves.isEmpty()) {
                return this.explicitWaves.contains(n);
            }
            if (n < this.startWave) {
                return false;
            }
            if (this.interval <= 0) {
                return n == this.startWave;
            }
            return (n - this.startWave) % this.interval == 0;
        }

        public int getSpawnIndex(int n) {
            if (!this.explicitWaves.isEmpty()) {
                int n2 = this.explicitWaves.indexOf(n);
                return n2 >= 0 ? n2 + 1 : 1;
            }
            if (this.interval <= 0) {
                return 1;
            }
            return Math.max(1, (n - this.startWave) / this.interval + 1);
        }

        public float getTargetHealth(int n) {
            int n2 = this.healthMode == HealthMode.WAVE_NUMBER ? Math.max(0, n - 1) : Math.max(0, this.getSpawnIndex(n) - 1);
            return this.healthBase + this.healthPerStep * (float)n2;
        }
    }

    public static enum HealthMode {
        WAVE_NUMBER("wave"),
        SPAWN_INDEX("spawn");

        private final String jsonValue;

        private HealthMode(String string2) {
            this.jsonValue = string2;
        }

        static HealthMode fromJsonValue(String string) {
            if (string == null || string.isBlank()) {
                return SPAWN_INDEX;
            }
            String string2 = string.trim().toLowerCase(Locale.ROOT);
            if ("wave".equals(string2) || "wave_number".equals(string2)) {
                return WAVE_NUMBER;
            }
            return SPAWN_INDEX;
        }

        String jsonValue() {
            return this.jsonValue;
        }
    }
}
