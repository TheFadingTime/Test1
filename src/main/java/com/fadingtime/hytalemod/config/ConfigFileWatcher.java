/*
 * HOT-RELOAD: Watches the external config directory for JSON file changes.
 *
 * DESIGN NOTES:
 * - Uses java.nio.file.WatchService — the JDK's native filesystem event API.
 *   No busy-loop polling needed; the OS notifies us of changes.
 * - Debounces with 500ms delay: editors often save files in multiple steps
 *   (write temp file → rename over original), which fires 2-3 events in rapid
 *   succession. The delay batches these into a single reload.
 * - Runs on a daemon thread so it won't prevent JVM shutdown.
 * - If a reload callback throws (e.g. JSON syntax error), we log the error and
 *   keep the previous config. The server stays up with the last-known-good values.
 */
package com.fadingtime.hytalemod.config;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ConfigFileWatcher {
    private static final long DEBOUNCE_MS = 500;

    private final Path configDir;
    private final Logger logger;
    private final Map<String, Runnable> reloadCallbacks = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> pendingReloads = new ConcurrentHashMap<>();
    private final ScheduledExecutorService debounceExecutor;
    private volatile WatchService watchService;
    private volatile Thread watchThread;

    public ConfigFileWatcher(Path configDir, Logger logger) {
        this.configDir = configDir;
        this.logger = logger;
        this.debounceExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ConfigFileWatcher-Debounce");
            t.setDaemon(true);
            return t;
        });
    }

    public void register(String filename, Runnable callback) {
        this.reloadCallbacks.put(filename, callback);
    }

    public void start() {
        if (!Files.isDirectory(this.configDir)) {
            this.logger.warning("Config directory does not exist, file watcher not started: " + this.configDir);
            return;
        }
        try {
            this.watchService = this.configDir.getFileSystem().newWatchService();
            this.configDir.register(
                this.watchService,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_CREATE
            );
        } catch (IOException e) {
            this.logger.log(Level.WARNING, "Failed to start config file watcher", e);
            return;
        }
        this.watchThread = new Thread(this::watchLoop, "ConfigFileWatcher");
        this.watchThread.setDaemon(true);
        this.watchThread.start();
        this.logger.info("Config file watcher started for: " + this.configDir);
    }

    public void shutdown() {
        Thread thread = this.watchThread;
        if (thread != null) {
            thread.interrupt();
        }
        WatchService service = this.watchService;
        if (service != null) {
            try {
                service.close();
            } catch (IOException ignored) {
            }
        }
        this.debounceExecutor.shutdownNow();
        this.logger.info("Config file watcher stopped.");
    }

    private void watchLoop() {
        WatchService service = this.watchService;
        while (!Thread.currentThread().isInterrupted()) {
            WatchKey key;
            try {
                key = service.take();
            } catch (InterruptedException | ClosedWatchServiceException e) {
                break;
            }
            for (WatchEvent<?> event : key.pollEvents()) {
                if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }
                Path changed = (Path) event.context();
                if (changed == null) {
                    continue;
                }
                String filename = changed.getFileName().toString();
                if (!filename.endsWith(".json")) {
                    continue;
                }
                Runnable callback = this.reloadCallbacks.get(filename);
                if (callback == null) {
                    continue;
                }
                this.scheduleDebounced(filename, callback);
            }
            if (!key.reset()) {
                this.logger.warning("Config watch key invalidated, stopping watcher.");
                break;
            }
        }
    }

    /**
     * Debounce: cancel any pending reload for this file, then schedule a new one.
     * This batches rapid events (write temp → rename) into a single reload.
     */
    private void scheduleDebounced(String filename, Runnable callback) {
        ScheduledFuture<?> future = this.debounceExecutor.schedule(() -> {
            this.pendingReloads.remove(filename);
            try {
                callback.run();
                this.logger.info("Reloaded " + filename);
            } catch (Exception e) {
                this.logger.log(Level.WARNING, "Failed to reload " + filename + " — keeping previous config", e);
            }
        }, DEBOUNCE_MS, TimeUnit.MILLISECONDS);

        ScheduledFuture<?> previous = this.pendingReloads.put(filename, future);
        if (previous != null) {
            previous.cancel(false);
        }
    }
}
