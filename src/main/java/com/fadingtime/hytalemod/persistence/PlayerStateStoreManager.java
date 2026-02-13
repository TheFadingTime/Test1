package com.fadingtime.hytalemod.persistence;

import com.fadingtime.hytalemod.persistence.PlayerStateStore;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

public final class PlayerStateStoreManager {
    private static final String LEGACY_DATA_DIR = "HytaleMod-data";
    private static final String MOD_DATA_ROOT_DIR = "ModData";
    private static final String MOD_DATA_DIR = "HytaleSurvivors";
    private static final String LEGACY_MOD_DATA_DIR = "HytaleSurivors";
    private static final String WORLDS_DIR = "worlds";
    private final Path baseDir;
    private final ConcurrentMap<String, PlayerStateStore> stores = new ConcurrentHashMap<String, PlayerStateStore>();

    public PlayerStateStoreManager(Path pluginFilePath) {
        Path pluginDir = pluginFilePath.getParent();
        Path baseCandidate = pluginDir != null ? pluginDir : pluginFilePath.toAbsolutePath().getParent();
        if (baseCandidate == null) {
            baseCandidate = Path.of(".");
        }
        Path userDataDir = baseCandidate.getParent();
        this.baseDir = userDataDir != null
            ? userDataDir.resolve(MOD_DATA_ROOT_DIR).resolve(MOD_DATA_DIR).resolve(WORLDS_DIR)
            : baseCandidate.resolve(LEGACY_DATA_DIR).resolve(WORLDS_DIR);
        Path legacyPluginWorldDir = pluginDir != null ? pluginDir.resolve(LEGACY_DATA_DIR).resolve(WORLDS_DIR) : null;
        Path legacyModDataWorldDir = userDataDir != null
            ? userDataDir.resolve(MOD_DATA_ROOT_DIR).resolve(LEGACY_MOD_DATA_DIR).resolve(WORLDS_DIR)
            : null;
        PlayerStateStoreManager.migrateLegacyData(legacyPluginWorldDir, this.baseDir);
        PlayerStateStoreManager.migrateLegacyData(legacyModDataWorldDir, this.baseDir);
    }

    public PlayerStateStore getStore(String worldName) {
        String safe = PlayerStateStoreManager.sanitize(worldName);
        return this.stores.computeIfAbsent(safe, name -> new PlayerStateStore(this.baseDir.resolve((String)name).resolve("player_state.properties")));
    }

    public void resetPlayerEverywhere(UUID playerId) {
        this.stores.values().forEach(store -> {
            store.saveLevel(playerId, 1, 0, 0);
            store.savePower(playerId, 0, 1.0f, 0.0f, 0, 0, 0, 0, 0, false);
        });
        if (!Files.isDirectory(this.baseDir)) {
            return;
        }
        try (Stream<Path> worlds = Files.list(this.baseDir);){
            worlds.filter(Files::isDirectory).forEach(worldDir -> {
                String worldKey = worldDir.getFileName().toString();
                PlayerStateStore store = this.getStore(worldKey);
                store.saveLevel(playerId, 1, 0, 0);
                store.savePower(playerId, 0, 1.0f, 0.0f, 0, 0, 0, 0, 0, false);
            });
        }
        catch (IOException iOException) {
            // Ignore reset sweep errors; normal world-key reset path still applies.
        }
    }

    private static String sanitize(String worldName) {
        if (worldName == null || worldName.isBlank()) {
            return "default";
        }
        String trimmed = worldName.trim();
        String safe = trimmed.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (safe.isBlank()) {
            return "default";
        }
        return safe;
    }

    private static void migrateLegacyData(Path legacyBaseDir, Path targetBaseDir) {
        if (legacyBaseDir == null || targetBaseDir == null) {
            return;
        }
        if (legacyBaseDir.equals(targetBaseDir) || !Files.isDirectory(legacyBaseDir)) {
            return;
        }
        try {
            Files.createDirectories(targetBaseDir);
            Files.walkFileTree(legacyBaseDir, new SimpleFileVisitor<Path>(){

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Path relative = legacyBaseDir.relativize(dir);
                    Files.createDirectories(targetBaseDir.resolve(relative));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path relative = legacyBaseDir.relativize(file);
                    Path targetFile = targetBaseDir.resolve(relative);
                    if (!Files.exists(targetFile)) {
                        Files.copy(file, targetFile);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        catch (IOException iOException) {
            // Ignore migration errors; runtime persistence will still use the new location.
        }
    }
}
