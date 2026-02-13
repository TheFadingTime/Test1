package com.fadingtime.hytalemod.config;

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
    private static final AtomicReference<SpawnConfig> CURRENT = new AtomicReference<>(DEFAULTS);

    private ConfigManager() {
    }

    public static synchronized void load(Path pluginFilePath, Logger logger) {
        SpawnConfig loadedConfig = DEFAULTS;
        try {
            loadedConfig = ConfigLoader.load(pluginFilePath, logger, DEFAULTS);
        }
        catch (IllegalArgumentException exception) {
            throw exception;
        }
        catch (RuntimeException exception) {
            logger.log(Level.WARNING, "Spawn config load failed, using defaults.", exception);
        }
        if (loadedConfig == null) {
            logger.warning("Spawn config loader returned null. Defaults applied.");
            loadedConfig = DEFAULTS;
        }
        apply(loadedConfig);
        // TODO: Hot-reload would save us from restart spam while balancing.
    }

    @Deprecated
    public static synchronized void initialize(Path pluginFilePath, Logger logger) {
        // Migration note: old code called MobSpawnConfig.initialize(...). Use ConfigManager.load(...) for new code.
        load(pluginFilePath, logger);
    }

    public static SpawnConfig get() {
        return CURRENT.get();
    }

    public static List<BossDefinition> getBossDefinitions() {
        return CURRENT.get().bosses;
    }

    @Nullable
    public static BossDefinition findBossForWave(int wave) {
        for (BossDefinition definition : CURRENT.get().bosses) {
            if (definition.shouldSpawnAtWave(wave)) {
                return definition;
            }
        }
        return null;
    }

    private static void apply(SpawnConfig config) {
        CURRENT.set(config);
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
        public final float waveHealthBonus;
        public final List<String> hostileRoles;
        public final String lifeEssenceItemId;
        public final List<BossDefinition> bosses;

        SpawnConfig(
            int spawnCount,
            int spawnCountPerWave,
            int maxMobsPerWave,
            long spawnIntervalMs,
            double minSpawnRadius,
            double maxSpawnRadius,
            double spawnYOffset,
            float maxMobHealth,
            float waveHealthBonus,
            List<String> hostileRoles,
            String lifeEssenceItemId,
            List<BossDefinition> bosses
        ) {
            this.spawnCount = spawnCount;
            this.spawnCountPerWave = spawnCountPerWave;
            this.maxMobsPerWave = maxMobsPerWave;
            this.spawnIntervalMs = spawnIntervalMs;
            this.minSpawnRadius = minSpawnRadius;
            this.maxSpawnRadius = maxSpawnRadius;
            this.spawnYOffset = spawnYOffset;
            this.maxMobHealth = maxMobHealth;
            this.waveHealthBonus = waveHealthBonus;
            this.hostileRoles = hostileRoles == null ? List.of() : hostileRoles;
            this.lifeEssenceItemId = lifeEssenceItemId == null || lifeEssenceItemId.isBlank() ? "Ingredient_Life_Essence" : lifeEssenceItemId;
            this.bosses = bosses == null ? List.of() : bosses;
        }

        static SpawnConfig defaults() {
            return new SpawnConfig(
                5,
                5,
                20,
                10000L,
                10.0,
                20.0,
                0.5,
                50.0f,
                // Small non-boss scaling keeps early waves readable for solo players.
                10.0f,
                List.of("Spirit_Ember", "Skeleton", "Zombie"),
                "Ingredient_Life_Essence",
                defaultBosses()
            );
        }
    }

    private static List<BossDefinition> defaultBosses() {
        List<BossDefinition> defaults = new ArrayList<>();
        defaults.add(new BossDefinition(
            "skeleton",
            "Skeleton",
            5,
            10,
            List.of(),
            4.0f,
            500.0f,
            150.0f,
            HealthMode.WAVE_NUMBER,
            "BossWaveHealth"
        ));
        defaults.add(new BossDefinition(
            "toad",
            "Toad_Rhino_Magma",
            10,
            10,
            List.of(),
            2.0f,
            400.0f,
            200.0f,
            HealthMode.SPAWN_INDEX,
            "ToadBossHealth"
        ));
        defaults.add(new BossDefinition(
            "wraith",
            "Wraith",
            40,
            0,
            List.of(40, 50),
            2.0f,
            1000.0f,
            500.0f,
            HealthMode.SPAWN_INDEX,
            "WraithBossHealth"
        ));
        return defaults;
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

        BossDefinition(
            String id,
            String role,
            int startWave,
            int interval,
            List<Integer> explicitWaves,
            float scale,
            float healthBase,
            float healthPerStep,
            HealthMode healthMode,
            String healthModifierId
        ) {
            this.id = id == null || id.isBlank() ? "boss" : id.trim().toLowerCase(Locale.ROOT);
            this.role = role == null || role.isBlank() ? "Skeleton" : role.trim();
            this.startWave = startWave <= 0 ? 1 : startWave;
            this.interval = interval < 0 ? 0 : interval;
            this.explicitWaves = explicitWaves == null ? List.of() : explicitWaves;
            this.scale = scale > 0.0f ? scale : 1.0f;
            this.healthBase = healthBase > 0.0f ? healthBase : 100.0f;
            // assuming these are valid from ConfigLoader
            this.healthPerStep = healthPerStep;
            this.healthMode = healthMode == null ? HealthMode.SPAWN_INDEX : healthMode;
            this.healthModifierId = healthModifierId;
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

        public boolean shouldSpawnAtWave(int wave) {
            if (wave <= 0) {
                return false;
            }
            if (!this.explicitWaves.isEmpty()) {
                return this.explicitWaves.contains(wave);
            }
            if (wave < this.startWave) {
                return false;
            }
            if (this.interval <= 0) {
                return wave == this.startWave;
            }
            return (wave - this.startWave) % this.interval == 0;
        }

        public int getSpawnIndex(int wave) {
            if (!this.explicitWaves.isEmpty()) {
                int index = this.explicitWaves.indexOf(wave);
                return index >= 0 ? index + 1 : 1;
            }
            if (this.interval <= 0) {
                return 1;
            }
            return Math.max(1, (wave - this.startWave) / this.interval + 1);
        }

        public float getTargetHealth(int wave) {
            int stepIndex = this.healthMode == HealthMode.WAVE_NUMBER ? Math.max(0, wave - 1) : Math.max(0, this.getSpawnIndex(wave) - 1);
            return this.healthBase + this.healthPerStep * (float)stepIndex;
        }
    }

    public enum HealthMode {
        WAVE_NUMBER("wave"),
        SPAWN_INDEX("spawn");

        private final String jsonValue;

        HealthMode(String jsonValue) {
            this.jsonValue = jsonValue;
        }

        static HealthMode fromJsonValue(String raw) {
            if (raw == null || raw.isBlank()) {
                return SPAWN_INDEX;
            }
            String normalized = raw.trim().toLowerCase(Locale.ROOT);
            if ("wave".equals(normalized) || "wave_number".equals(normalized)) {
                return WAVE_NUMBER;
            }
            return SPAWN_INDEX;
        }

        String jsonValue() {
            return this.jsonValue;
        }
    }
}
