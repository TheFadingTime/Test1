package com.fadingtime.hytalemod.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bson.BsonDocument;

public final class JsonConfigLoader {
    private JsonConfigLoader() {
    }

    public static BsonDocument loadDocument(Path pluginFilePath, String externalFileName, String bundledResourcePath, Logger logger) {
        Path externalPath = JsonConfigLoader.resolveExternalPath(pluginFilePath, externalFileName);
        String bundledJson = JsonConfigLoader.readBundledJson(bundledResourcePath, logger);
        String externalJson = JsonConfigLoader.readExternalJson(externalPath, logger);
        if (externalJson != null) {
            BsonDocument parsedExternal = JsonConfigLoader.tryParse(externalJson, "external config " + externalPath, logger);
            if (parsedExternal != null) {
                return parsedExternal;
            }
            if (bundledJson != null) {
                BsonDocument parsedBundled = JsonConfigLoader.tryParse(bundledJson, "bundled config " + bundledResourcePath, logger);
                if (parsedBundled != null) {
                    return parsedBundled;
                }
            }
            return new BsonDocument();
        }
        if (bundledJson != null) {
            JsonConfigLoader.tryWriteDefaultExternal(externalPath, bundledJson, logger);
            BsonDocument parsedBundled = JsonConfigLoader.tryParse(bundledJson, "bundled config " + bundledResourcePath, logger);
            if (parsedBundled != null) {
                return parsedBundled;
            }
        }
        return new BsonDocument();
    }

    private static Path resolveExternalPath(Path pluginFilePath, String externalFileName) {
        Path pluginDir = pluginFilePath != null ? pluginFilePath.getParent() : null;
        Path baseDir = pluginDir != null ? pluginDir : Path.of(".");
        return baseDir.resolve("config").resolve(externalFileName);
    }

    private static String readExternalJson(Path path, Logger logger) {
        if (!Files.isRegularFile(path)) {
            return null;
        }
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        }
        catch (IOException exception) {
            logger.log(Level.WARNING, "Failed to read config file " + path, exception);
            return null;
        }
    }

    private static String readBundledJson(String resourcePath, Logger logger) {
        try (InputStream in = JsonConfigLoader.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                logger.log(Level.WARNING, "Bundled config not found at " + resourcePath);
                return null;
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        catch (IOException exception) {
            logger.log(Level.WARNING, "Failed to read bundled config " + resourcePath, exception);
            return null;
        }
    }

    private static void tryWriteDefaultExternal(Path path, String json, Logger logger) {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, json, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        }
        catch (IOException ignored) {
            logger.log(Level.FINE, "Skipped writing default config to " + path, ignored);
        }
    }

    private static BsonDocument tryParse(String json, String sourceName, Logger logger) {
        try {
            return BsonDocument.parse(ConfigUtils.stripJsonComments(json));
        }
        catch (Exception exception) {
            logger.log(Level.WARNING, "Failed to parse " + sourceName, exception);
            return null;
        }
    }
}
