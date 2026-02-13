package com.fadingtime.hytalemod.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ConfigLoader {
    private static final String BUNDLED_RESOURCE = "config.json";

    private ConfigLoader() {
    }

    public static ConfigManager.SpawnConfig load(Path pluginFilePath, Logger logger, ConfigManager.SpawnConfig defaults) {
        if (logger == null) {
            throw new IllegalArgumentException("Logger is required for config loading.");
        }
        if (defaults == null) {
            throw new IllegalArgumentException("Default config is required.");
        }

        Path externalPath = resolveExternalPath(pluginFilePath);
        JsonObject externalJson = readExternalJson(externalPath, logger);
        if (externalJson != null) {
            ConfigManager.SpawnConfig parsedExternal = parseSpawnConfig(externalJson, defaults, logger, externalPath.toString());
            if (parsedExternal != null) {
                return parsedExternal;
            }
            logger.warning("External spawn config is invalid, trying bundled config.");
        }

        JsonObject bundledJson = readBundledJson(logger);
        ConfigManager.SpawnConfig parsedBundled = parseSpawnConfig(bundledJson, defaults, logger, "bundled:" + BUNDLED_RESOURCE);
        if (parsedBundled != null) {
            if (externalJson == null) {
                tryWriteDefaultExternal(externalPath, bundledJson, logger);
            }
            return parsedBundled;
        }

        logger.warning("config.json parse failed; using in-code defaults.");
        return defaults;
    }

    private static Path resolveExternalPath(Path pluginFilePath) {
        Path pluginDir = pluginFilePath != null ? pluginFilePath.getParent() : null;
        Path baseDir = pluginDir != null ? pluginDir : Path.of(".");
        return baseDir.resolve("config").resolve("config.json");
    }

    private static JsonObject readExternalJson(Path path, Logger logger) {
        if (!Files.isRegularFile(path)) {
            return null;
        }
        try {
            String raw = Files.readString(path, StandardCharsets.UTF_8);
            return parseJsonObject(raw, path.toString(), logger);
        } catch (IOException exception) {
            logger.log(Level.WARNING, "Failed reading " + path, exception);
            return null;
        }
    }

    private static JsonObject readBundledJson(Logger logger) {
        try (InputStream in = ConfigLoader.class.getClassLoader().getResourceAsStream(BUNDLED_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Bundled " + BUNDLED_RESOURCE + " is missing.");
            }
            String raw = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            JsonObject parsed = parseJsonObject(raw, BUNDLED_RESOURCE, logger);
            if (parsed == null) {
                throw new IllegalStateException("Bundled " + BUNDLED_RESOURCE + " is not valid JSON.");
            }
            return parsed;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read bundled " + BUNDLED_RESOURCE, exception);
        }
    }

    private static void tryWriteDefaultExternal(Path path, JsonObject json, Logger logger) {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, json.toString(), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException exception) {
            logger.log(Level.FINE, "Could not write default config to " + path, exception);
        }
    }

    private static JsonObject parseJsonObject(String raw, String source, Logger logger) {
        try {
            String stripped = ConfigUtils.stripJsonComments(raw);
            JsonElement parsed = JsonParser.parseString(stripped);
            if (!parsed.isJsonObject()) {
                logger.warning(source + " is not a JSON object.");
                return null;
            }
            return parsed.getAsJsonObject();
        } catch (JsonParseException exception) {
            logger.warning("Invalid JSON in " + source);
            return null;
        }
    }

    private static ConfigManager.SpawnConfig parseSpawnConfig(
        JsonObject root,
        ConfigManager.SpawnConfig defaults,
        Logger logger,
        String source
    ) {
        if (root == null) {
            return null;
        }

        try {
            JsonObject spawning = getSubObject(root, "spawning");
            JsonObject spawnArea = getSubObject(root, "spawnArea");
            JsonObject mobs = getSubObject(root, "mobs");
            JsonObject reconnect = getSubObject(root, "reconnect");

            int spawnCount = readInt(spawning, "spawnCount", defaults.spawnCount, 0);
            int spawnCountPerWave = readInt(spawning, "spawnCountPerWave", defaults.spawnCountPerWave, 0);
            int maxMobsPerWave = readInt(spawning, "maxMobsPerWave", defaults.maxMobsPerWave, 1);
            long spawnIntervalMs = readLong(spawning, "spawnIntervalMs", defaults.spawnIntervalMs, 500L);
            double minSpawnRadius = readDouble(spawnArea, "minSpawnRadius", defaults.minSpawnRadius, 0.1);
            double maxSpawnRadius = readDouble(spawnArea, "maxSpawnRadius", defaults.maxSpawnRadius, minSpawnRadius);
            double spawnYOffset = readDouble(spawnArea, "spawnYOffset", defaults.spawnYOffset, -1024.0);
            float maxMobHealth = readFloat(mobs, "maxMobHealth", defaults.maxMobHealth, 1.0f);
            float waveHealthBonus = readFloat(mobs, "waveHealthBonus", defaults.waveHealthBonus, 0.0f);
            List<String> hostileRoles = readStringList(mobs, "hostileRoles");
            String lifeEssenceItemId = readString(mobs, "lifeEssenceItemId", defaults.lifeEssenceItemId);
            List<ConfigManager.BossDefinition> bosses = readBosses(root, logger);
            long rejoinHoldMaxMs = readLong(reconnect, "rejoinHoldMaxMs", defaults.rejoinHoldMaxMs, 0L);
            double markerReattachRadius = readDouble(reconnect, "markerReattachRadius", defaults.markerReattachRadius, 0.1);
            long minSpawnGraceMs = readLong(reconnect, "minSpawnGraceMs", defaults.minSpawnGraceMs, 0L);
            long maxSpawnGraceMs = readLong(reconnect, "maxSpawnGraceMs", defaults.maxSpawnGraceMs, 0L);
            long markerReattachDelayMs = readLong(reconnect, "markerReattachDelayMs", defaults.markerReattachDelayMs, 0L);
            long disconnectDedupMs = readLong(reconnect, "disconnectDedupMs", defaults.disconnectDedupMs, 0L);

            return new ConfigManager.SpawnConfig(
                spawnCount,
                spawnCountPerWave,
                maxMobsPerWave,
                spawnIntervalMs,
                minSpawnRadius,
                maxSpawnRadius,
                spawnYOffset,
                maxMobHealth,
                waveHealthBonus,
                hostileRoles,
                lifeEssenceItemId,
                bosses,
                rejoinHoldMaxMs,
                markerReattachRadius,
                minSpawnGraceMs,
                maxSpawnGraceMs,
                markerReattachDelayMs,
                disconnectDedupMs
            );
        } catch (RuntimeException exception) {
            logger.log(Level.WARNING, "Invalid spawn config in " + source, exception);
            return null;
        }
    }

    private static List<ConfigManager.BossDefinition> readBosses(JsonObject root, Logger logger) {
        JsonArray bossesArray = getArray(root, "bosses");
        if (bossesArray == null) {
            return List.of();
        }

        List<ConfigManager.BossDefinition> parsed = new ArrayList<>();
        for (JsonElement element : bossesArray) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject bossJson = element.getAsJsonObject();
            try {
                String id = readStringRequired(bossJson, "id");
                String role = readStringRequired(bossJson, "role");
                String displayName = hasKey(bossJson, "displayName") ? readStringRequired(bossJson, "displayName") : null;
                List<Integer> waves = readIntList(bossJson, "waves");

                int startWave;
                if (hasKey(bossJson, "startWave")) {
                    startWave = readIntRequired(bossJson, "startWave", 1);
                } else if (!waves.isEmpty()) {
                    startWave = waves.get(0);
                } else {
                    startWave = 1;
                }

                int interval = hasKey(bossJson, "interval") ? readIntRequired(bossJson, "interval", 0) : 0;
                float scale = hasKey(bossJson, "scale") ? readFloatRequired(bossJson, "scale", 0.1f) : 1.0f;
                float healthBase = hasKey(bossJson, "healthBase") ? readFloatRequired(bossJson, "healthBase", 0.1f) : 100.0f;
                float healthPerStep = hasKey(bossJson, "healthPerStep") ? readFloatRequired(bossJson, "healthPerStep", 0.0f) : 0.0f;
                String healthModeRaw = hasKey(bossJson, "healthMode") ? readStringRequired(bossJson, "healthMode") : "spawn";
                ConfigManager.HealthMode healthMode = ConfigManager.HealthMode.fromJsonValue(healthModeRaw);
                String modifierId = hasKey(bossJson, "healthModifierId")
                    ? readStringRequired(bossJson, "healthModifierId")
                    : "BossWaveHealth";

                parsed.add(new ConfigManager.BossDefinition(
                    id,
                    role,
                    displayName,
                    startWave,
                    interval,
                    waves,
                    scale,
                    healthBase,
                    healthPerStep,
                    healthMode,
                    modifierId
                ));
            } catch (RuntimeException exception) {
                logger.log(Level.FINE, "Skipped invalid boss entry.", exception);
            }
        }

        return parsed;
    }

    // Default-returning read methods for top-level config fields.
    // Missing keys return the default instead of throwing, preventing one missing
    // field from silently discarding the entire config.

    private static int readInt(JsonObject json, String key, int defaultValue, int min) {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonPrimitive()) {
            return defaultValue;
        }
        try {
            return Math.max(min, value.getAsInt());
        } catch (RuntimeException exception) {
            return defaultValue;
        }
    }

    private static long readLong(JsonObject json, String key, long defaultValue, long min) {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            return defaultValue;
        }
        return Math.max(min, value.getAsLong());
    }

    private static float readFloat(JsonObject json, String key, float defaultValue, float min) {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonPrimitive()) {
            return defaultValue;
        }
        float parsed;
        try {
            parsed = value.getAsFloat();
        } catch (RuntimeException exception) {
            return defaultValue;
        }
        if (!Float.isFinite(parsed)) {
            return defaultValue;
        }
        return Math.max(min, parsed);
    }

    private static double readDouble(JsonObject json, String key, double defaultValue, double min) {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonPrimitive()) {
            return defaultValue;
        }
        double parsed;
        try {
            parsed = value.getAsDouble();
        } catch (RuntimeException exception) {
            return defaultValue;
        }
        if (!Double.isFinite(parsed)) {
            return defaultValue;
        }
        return Math.max(min, parsed);
    }

    private static String readString(JsonObject json, String key, String defaultValue) {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            return defaultValue;
        }
        String parsed = value.getAsString();
        if (parsed == null || parsed.isBlank()) {
            return defaultValue;
        }
        return parsed.trim();
    }

    // Throwing read methods for boss definitions where missing required fields
    // should skip the individual boss entry (caught per-boss).

    private static int readIntRequired(JsonObject json, String key, int min) {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonPrimitive()) {
            throw new IllegalArgumentException("Missing numeric key: " + key);
        }
        try {
            return Math.max(min, value.getAsInt());
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid int key: " + key, exception);
        }
    }

    private static float readFloatRequired(JsonObject json, String key, float min) {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonPrimitive()) {
            throw new IllegalArgumentException("Missing numeric key: " + key);
        }
        float parsed;
        try {
            parsed = value.getAsFloat();
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid float key: " + key, exception);
        }
        if (!Float.isFinite(parsed)) {
            throw new IllegalArgumentException("Non-finite float key: " + key);
        }
        return Math.max(min, parsed);
    }

    private static String readStringRequired(JsonObject json, String key) {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Missing string key: " + key);
        }
        String parsed = value.getAsString();
        if (parsed == null || parsed.isBlank()) {
            throw new IllegalArgumentException("Blank string key: " + key);
        }
        return parsed.trim();
    }

    private static List<String> readStringList(JsonObject json, String key) {
        JsonArray array = getArray(json, key);
        if (array == null) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                continue;
            }
            String item = element.getAsString();
            if (item == null || item.isBlank()) {
                continue;
            }
            values.add(item.trim());
        }
        return List.copyOf(values);
    }

    private static List<Integer> readIntList(JsonObject json, String key) {
        JsonArray array = getArray(json, key);
        if (array == null) {
            return List.of();
        }
        List<Integer> values = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
                continue;
            }
            int parsed = element.getAsInt();
            if (parsed <= 0) {
                continue;
            }
            values.add(parsed);
        }
        Collections.sort(values);
        return values;
    }

    private static JsonObject getSubObject(JsonObject json, String key) {
        JsonElement value = json.get(key);
        if (value != null && value.isJsonObject()) {
            return value.getAsJsonObject();
        }
        return new JsonObject();
    }

    private static JsonArray getArray(JsonObject json, String key) {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonArray()) {
            return null;
        }
        return value.getAsJsonArray();
    }

    private static boolean hasKey(JsonObject json, String key) {
        JsonElement value = json.get(key);
        return value != null && !value.isJsonNull();
    }
}
