/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParseException
 *  com.google.gson.JsonParser
 */
package com.fadingtime.hytalemod.config;

import com.fadingtime.hytalemod.config.ConfigManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ConfigLoader {
    private static final String BUNDLED_RESOURCE = "config.json";

    private ConfigLoader() {
    }

    public static ConfigManager.SpawnConfig load(Path path, Logger logger, ConfigManager.SpawnConfig spawnConfig) {
        if (logger == null) {
            throw new IllegalArgumentException("Logger is required for config loading.");
        }
        if (spawnConfig == null) {
            throw new IllegalArgumentException("Default config is required.");
        }
        Path path2 = ConfigLoader.resolveExternalPath(path);
        JsonObject jsonObject = ConfigLoader.readExternalJson(path2, logger);
        if (jsonObject != null) {
            ConfigManager.SpawnConfig spawnConfig2 = ConfigLoader.parseSpawnConfig(jsonObject, logger, path2.toString());
            if (spawnConfig2 != null) {
                return spawnConfig2;
            }
            logger.warning("External spawn config is invalid, trying bundled config.");
        }
        JsonObject jsonObject2 = ConfigLoader.readBundledJson(logger);
        ConfigManager.SpawnConfig spawnConfig3 = ConfigLoader.parseSpawnConfig(jsonObject2, logger, "bundled:config.json");
        if (spawnConfig3 != null) {
            if (jsonObject == null) {
                ConfigLoader.tryWriteDefaultExternal(path2, jsonObject2, logger);
            }
            return spawnConfig3;
        }
        logger.warning("config.json parse failed; using in-code defaults.");
        return spawnConfig;
    }

    private static Path resolveExternalPath(Path path) {
        Path path2 = path != null ? path.getParent() : null;
        Path path3 = path2 != null ? path2 : Path.of(".", new String[0]);
        return path3.resolve("config").resolve(BUNDLED_RESOURCE);
    }

    private static JsonObject readExternalJson(Path path, Logger logger) {
        if (!Files.isRegularFile(path, new LinkOption[0])) {
            return null;
        }
        try {
            String string = Files.readString(path, StandardCharsets.UTF_8);
            return ConfigLoader.parseJsonObject(string, path.toString(), logger);
        }
        catch (IOException iOException) {
            logger.log(Level.WARNING, "Failed reading " + String.valueOf(path), iOException);
            return null;
        }
    }

    private static JsonObject readBundledJson(Logger logger) {
        try (InputStream inputStream = ConfigLoader.class.getClassLoader().getResourceAsStream(BUNDLED_RESOURCE)) {
            if (inputStream == null) {
                throw new IllegalStateException("Bundled config.json is missing.");
            }
            String string = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            JsonObject jsonObject = ConfigLoader.parseJsonObject(string, BUNDLED_RESOURCE, logger);
            if (jsonObject == null) {
                throw new IllegalStateException("Bundled config.json is not valid JSON.");
            }
            return jsonObject;
        }
        catch (IOException iOException) {
            throw new IllegalStateException("Could not read bundled config.json", iOException);
        }
    }

    private static void tryWriteDefaultExternal(Path path, JsonObject jsonObject, Logger logger) {
        try {
            Path path2 = path.getParent();
            if (path2 != null) {
                Files.createDirectories(path2, new FileAttribute[0]);
            }
            Files.writeString(path, (CharSequence)jsonObject.toString(), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        }
        catch (IOException iOException) {
        }
    }

    private static JsonObject parseJsonObject(String string, String string2, Logger logger) {
        try {
            JsonElement jsonElement = JsonParser.parseString((String)string);
            if (!jsonElement.isJsonObject()) {
                logger.warning(string2 + " is not a JSON object.");
                return null;
            }
            return jsonElement.getAsJsonObject();
        }
        catch (JsonParseException jsonParseException) {
            logger.warning("Invalid JSON in " + string2);
            return null;
        }
    }

    private static ConfigManager.SpawnConfig parseSpawnConfig(JsonObject jsonObject, Logger logger, String string) {
        if (jsonObject == null) {
            return null;
        }
        try {
            int n = ConfigLoader.readInt(jsonObject, "spawnCount", 0);
            int n2 = ConfigLoader.readInt(jsonObject, "spawnCountPerWave", 0);
            int n3 = ConfigLoader.readInt(jsonObject, "maxMobsPerWave", 1);
            long l = ConfigLoader.readLong(jsonObject, "spawnIntervalMs", 500L);
            double d = ConfigLoader.readDouble(jsonObject, "minSpawnRadius", 0.1);
            double d2 = ConfigLoader.readDouble(jsonObject, "maxSpawnRadius", d);
            double d3 = ConfigLoader.readDouble(jsonObject, "spawnYOffset", -1024.0);
            float f = ConfigLoader.readFloat(jsonObject, "maxMobHealth", 1.0f);
            List<String> list = ConfigLoader.readStringList(jsonObject, "hostileRoles");
            String string2 = ConfigLoader.readString(jsonObject, "lifeEssenceItemId");
            List<ConfigManager.BossDefinition> list2 = ConfigLoader.readBosses(jsonObject, logger);
            return new ConfigManager.SpawnConfig(n, n2, n3, l, d, d2, d3, f, list, string2, list2);
        }
        catch (IllegalArgumentException illegalArgumentException) {
            logger.log(Level.WARNING, "Invalid spawn config in " + string, illegalArgumentException);
            return null;
        }
    }

    private static List<ConfigManager.BossDefinition> readBosses(JsonObject jsonObject, Logger logger) {
        JsonArray jsonArray = ConfigLoader.getArray(jsonObject, "bosses");
        if (jsonArray == null) {
            return List.of();
        }
        ArrayList<ConfigManager.BossDefinition> arrayList = new ArrayList<ConfigManager.BossDefinition>();
        for (JsonElement jsonElement : jsonArray) {
            if (!jsonElement.isJsonObject()) continue;
            JsonObject jsonObject2 = jsonElement.getAsJsonObject();
            try {
                String string = ConfigLoader.readString(jsonObject2, "id");
                String string2 = ConfigLoader.readString(jsonObject2, "role");
                List<Integer> list = ConfigLoader.readIntList(jsonObject2, "waves");
                int n = ConfigLoader.hasKey(jsonObject2, "startWave") ? ConfigLoader.readInt(jsonObject2, "startWave", 1) : (!list.isEmpty() ? list.get(0) : 1);
                int n2 = ConfigLoader.hasKey(jsonObject2, "interval") ? ConfigLoader.readInt(jsonObject2, "interval", 0) : 0;
                float f = ConfigLoader.hasKey(jsonObject2, "scale") ? ConfigLoader.readFloat(jsonObject2, "scale", 0.1f) : 1.0f;
                float f2 = ConfigLoader.hasKey(jsonObject2, "healthBase") ? ConfigLoader.readFloat(jsonObject2, "healthBase", 0.1f) : 100.0f;
                float f3 = ConfigLoader.hasKey(jsonObject2, "healthPerStep") ? ConfigLoader.readFloat(jsonObject2, "healthPerStep", 0.0f) : 0.0f;
                String string3 = ConfigLoader.hasKey(jsonObject2, "healthMode") ? ConfigLoader.readString(jsonObject2, "healthMode") : "spawn";
                ConfigManager.HealthMode healthMode = ConfigManager.HealthMode.fromJsonValue(string3);
                String string4 = ConfigLoader.hasKey(jsonObject2, "healthModifierId") ? ConfigLoader.readString(jsonObject2, "healthModifierId") : "BossWaveHealth";
                arrayList.add(new ConfigManager.BossDefinition(string, string2, n, n2, list, f, f2, f3, healthMode, string4));
            }
            catch (IllegalArgumentException illegalArgumentException) {
            }
        }
        return arrayList;
    }

    private static int readInt(JsonObject jsonObject, String string, int n) {
        JsonElement jsonElement = jsonObject.get(string);
        if (jsonElement == null || !jsonElement.isJsonPrimitive()) {
            throw new IllegalArgumentException("Missing numeric key: " + string);
        }
        try {
            return Math.max(n, jsonElement.getAsInt());
        }
        catch (NumberFormatException | UnsupportedOperationException runtimeException) {
            throw new IllegalArgumentException("Invalid int key: " + string, runtimeException);
        }
    }

    private static long readLong(JsonObject jsonObject, String string, long l) {
        JsonElement jsonElement = jsonObject.get(string);
        if (jsonElement == null || !jsonElement.isJsonPrimitive() || !jsonElement.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("Missing numeric key: " + string);
        }
        try {
            return Math.max(l, jsonElement.getAsLong());
        }
        catch (NumberFormatException | UnsupportedOperationException runtimeException) {
            throw new IllegalArgumentException("Invalid long key: " + string, runtimeException);
        }
    }

    private static float readFloat(JsonObject jsonObject, String string, float f) {
        float f2;
        JsonElement jsonElement = jsonObject.get(string);
        if (jsonElement == null || !jsonElement.isJsonPrimitive()) {
            throw new IllegalArgumentException("Missing numeric key: " + string);
        }
        try {
            f2 = jsonElement.getAsFloat();
        }
        catch (NumberFormatException | UnsupportedOperationException runtimeException) {
            throw new IllegalArgumentException("Invalid float key: " + string, runtimeException);
        }
        if (!Float.isFinite(f2)) {
            throw new IllegalArgumentException("Non-finite float key: " + string);
        }
        return Math.max(f, f2);
    }

    private static double readDouble(JsonObject jsonObject, String string, double d) {
        double d2;
        JsonElement jsonElement = jsonObject.get(string);
        if (jsonElement == null || !jsonElement.isJsonPrimitive()) {
            throw new IllegalArgumentException("Missing numeric key: " + string);
        }
        try {
            d2 = jsonElement.getAsDouble();
        }
        catch (NumberFormatException | UnsupportedOperationException runtimeException) {
            throw new IllegalArgumentException("Invalid double key: " + string, runtimeException);
        }
        if (!Double.isFinite(d2)) {
            throw new IllegalArgumentException("Non-finite double key: " + string);
        }
        return Math.max(d, d2);
    }

    private static String readString(JsonObject jsonObject, String string) {
        JsonElement jsonElement = jsonObject.get(string);
        if (jsonElement == null || !jsonElement.isJsonPrimitive() || !jsonElement.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Missing string key: " + string);
        }
        String string2 = jsonElement.getAsString();
        if (string2 == null || string2.isBlank()) {
            throw new IllegalArgumentException("Blank string key: " + string);
        }
        return string2.trim();
    }

    private static List<String> readStringList(JsonObject jsonObject, String string) {
        JsonArray jsonArray = ConfigLoader.getArray(jsonObject, string);
        if (jsonArray == null) {
            return List.of();
        }
        ArrayList<String> arrayList = new ArrayList<String>();
        for (JsonElement jsonElement : jsonArray) {
            String string2;
            if (!jsonElement.isJsonPrimitive() || !jsonElement.getAsJsonPrimitive().isString() || (string2 = jsonElement.getAsString()) == null || string2.isBlank()) continue;
            arrayList.add(string2.trim());
        }
        return List.copyOf(arrayList);
    }

    private static List<Integer> readIntList(JsonObject jsonObject, String string) {
        JsonArray jsonArray = ConfigLoader.getArray(jsonObject, string);
        if (jsonArray == null) {
            return List.of();
        }
        ArrayList<Integer> arrayList = new ArrayList<Integer>();
        for (JsonElement jsonElement : jsonArray) {
            int n;
            if (!jsonElement.isJsonPrimitive() || !jsonElement.getAsJsonPrimitive().isNumber() || (n = jsonElement.getAsInt()) <= 0) continue;
            arrayList.add(n);
        }
        Collections.sort(arrayList);
        return arrayList;
    }

    private static JsonArray getArray(JsonObject jsonObject, String string) {
        JsonElement jsonElement = jsonObject.get(string);
        if (jsonElement == null || !jsonElement.isJsonArray()) {
            return null;
        }
        return jsonElement.getAsJsonArray();
    }

    private static boolean hasKey(JsonObject jsonObject, String string) {
        JsonElement jsonElement = jsonObject.get(string);
        return jsonElement != null && !jsonElement.isJsonNull();
    }
}
