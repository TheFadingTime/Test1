/*
 * Decompiled with CFR 0.152.
 */
package com.fadingtime.hytalemod.persistence;

import com.fadingtime.hytalemod.persistence.PlayerStateStore;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
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

    public PlayerStateStoreManager(Path path) {
        Path path2;
        Path path3;
        Path path4 = path.getParent();
        Path path5 = path3 = path4 != null ? path4 : path.toAbsolutePath().getParent();
        if (path3 == null) {
            path3 = Path.of(".", new String[0]);
        }
        this.baseDir = (path2 = path3.getParent()) != null ? path2.resolve(MOD_DATA_ROOT_DIR).resolve(MOD_DATA_DIR).resolve(WORLDS_DIR) : path3.resolve(LEGACY_DATA_DIR).resolve(WORLDS_DIR);
        Path path6 = path4 != null ? path4.resolve(LEGACY_DATA_DIR).resolve(WORLDS_DIR) : null;
        Path path7 = path2 != null ? path2.resolve(MOD_DATA_ROOT_DIR).resolve(LEGACY_MOD_DATA_DIR).resolve(WORLDS_DIR) : null;
        PlayerStateStoreManager.migrateLegacyData(path6, this.baseDir);
        PlayerStateStoreManager.migrateLegacyData(path7, this.baseDir);
    }

    public PlayerStateStore getStore(String string2) {
        String string3 = PlayerStateStoreManager.sanitize(string2);
        return this.stores.computeIfAbsent(string3, string -> new PlayerStateStore(this.baseDir.resolve((String)string).resolve("player_state.properties")));
    }

    public void resetPlayerEverywhere(UUID uUID) {
        this.stores.values().forEach(playerStateStore -> {
            playerStateStore.saveLevel(uUID, 1, 0, 0);
            playerStateStore.savePower(uUID, 0, 1.0f, 0.0f, 0, 0, 0, 0, 0, 0, false);
        });
        if (!Files.isDirectory(this.baseDir, new LinkOption[0])) {
            return;
        }
        try (Stream<Path> stream = Files.list(this.baseDir);){
            stream.filter(path -> Files.isDirectory(path, new LinkOption[0])).forEach(path -> {
                String string = path.getFileName().toString();
                PlayerStateStore playerStateStore = this.getStore(string);
                playerStateStore.saveLevel(uUID, 1, 0, 0);
                playerStateStore.savePower(uUID, 0, 1.0f, 0.0f, 0, 0, 0, 0, 0, 0, false);
            });
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }

    private static String sanitize(String string) {
        if (string == null || string.isBlank()) {
            return "default";
        }
        String string2 = string.trim();
        String string3 = string2.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (string3.isBlank()) {
            return "default";
        }
        return string3;
    }

    private static void migrateLegacyData(final Path path, final Path path2) {
        if (path == null || path2 == null) {
            return;
        }
        if (path.equals(path2) || !Files.isDirectory(path, new LinkOption[0])) {
            return;
        }
        try {
            Files.createDirectories(path2, new FileAttribute[0]);
            Files.walkFileTree(path, (FileVisitor<? super Path>)new SimpleFileVisitor<Path>(){

                @Override
                public FileVisitResult preVisitDirectory(Path path3, BasicFileAttributes basicFileAttributes) throws IOException {
                    Path path22 = path.relativize(path3);
                    Files.createDirectories(path2.resolve(path22), new FileAttribute[0]);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path path4, BasicFileAttributes basicFileAttributes) throws IOException {
                    Path path22 = path.relativize(path4);
                    Path path3 = path2.resolve(path22);
                    if (!Files.exists(path3, new LinkOption[0])) {
                        Files.copy(path4, path3, new CopyOption[0]);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }
}

