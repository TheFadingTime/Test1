package com.fadingtime.hytalemod.system;

import com.fadingtime.hytalemod.HytaleMod;
import com.fadingtime.hytalemod.ui.PowerUpStorePage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldConfig;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.annotation.Nonnull;

public final class StoreSessionManager {
    private final HytaleMod plugin;
    private final GamePauseController gamePauseController;
    private final long storeInputGraceMs;
    private final ConcurrentMap<UUID, ScheduledFuture<?>> storeTimeouts = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Integer> storeSessionByPlayer = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Long> storeOpenedAt = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, ConcurrentLinkedQueue<ScheduledFuture<?>>> delayedTasksByPlayer = new ConcurrentHashMap<>();

    public StoreSessionManager(@Nonnull HytaleMod plugin, @Nonnull GamePauseController gamePauseController, long storeInputGraceMs) {
        this.plugin = plugin;
        this.gamePauseController = gamePauseController;
        this.storeInputGraceMs = storeInputGraceMs;
    }

    public void openStoreForPlayer(@Nonnull Ref<EntityStore> playerRef, @Nonnull Store<EntityStore> store, @Nonnull Player playerComponent, @Nonnull PlayerRef playerRefComponent, int level) {
        UUID playerId = playerRefComponent.getUuid();
        World world = ((EntityStore)store.getExternalData()).getWorld();
        if (world == null) {
            return;
        }

        int sessionId = this.storeSessionByPlayer.compute(playerId, (id, value) -> value == null ? 1 : value + 1);
        ScheduledFuture<?> timeout = schedulePlayerWorldTask(world, playerRefComponent, () -> closeStoreInternal(playerRef, store, playerComponent, playerRefComponent, sessionId), 30000L);
        ScheduledFuture<?> existing = this.storeTimeouts.put(playerId, timeout);
        if (existing != null) {
            existing.cancel(false);
        }

        for (int i = 1; i <= 8; ++i) {
            final float factor = this.gamePauseController.computeSlowmoFactor(i, 8);
            long delay = 1800L * i / 8L;
            schedulePlayerWorldTask(world, playerRefComponent, () -> this.gamePauseController.applyTimeScale(store, factor), delay);
        }

        schedulePlayerWorldTask(world, playerRefComponent, () -> {
            this.gamePauseController.beginStorePause(world, store);
            this.storeOpenedAt.put(playerId, System.currentTimeMillis());
            playerComponent.getPageManager().openCustomPage(playerRef, store, new PowerUpStorePage(playerRefComponent, level));
        }, 1800L);
    }

    public void closeStoreForPlayer(@Nonnull Ref<EntityStore> playerRef, @Nonnull Store<EntityStore> store) {
        PlayerRef playerRefComponent = (PlayerRef)store.getComponent(playerRef, PlayerRef.getComponentType());
        if (playerRefComponent == null) {
            return;
        }
        Player playerComponent = (Player)store.getComponent(playerRef, Player.getComponentType());
        if (playerComponent == null) {
            return;
        }
        World world = ((EntityStore)store.getExternalData()).getWorld();
        if (world == null) {
            return;
        }
        Integer sessionId = this.storeSessionByPlayer.get(playerRefComponent.getUuid());
        if (sessionId == null) {
            return;
        }
        world.execute(() -> closeStoreInternal(playerRef, store, playerComponent, playerRefComponent, sessionId));
    }

    public boolean canAcceptStoreSelection(@Nonnull UUID playerId) {
        Long openedAt = this.storeOpenedAt.get(playerId);
        if (openedAt == null) {
            return true;
        }
        return System.currentTimeMillis() - openedAt >= this.storeInputGraceMs;
    }

    public void cancelAllScheduledTasks(@Nonnull UUID playerId) {
        ScheduledFuture<?> timeout = this.storeTimeouts.remove(playerId);
        if (timeout != null) {
            timeout.cancel(false);
        }
        cancelPlayerTasks(playerId);
        this.storeSessionByPlayer.remove(playerId);
        this.storeOpenedAt.remove(playerId);
    }

    private void closeStoreInternal(@Nonnull Ref<EntityStore> playerRef, @Nonnull Store<EntityStore> store, @Nonnull Player playerComponent, @Nonnull PlayerRef playerRefComponent, int sessionId) {
        UUID playerId = playerRefComponent.getUuid();
        Integer currentSession = this.storeSessionByPlayer.get(playerId);
        if (currentSession == null || currentSession != sessionId) {
            return;
        }
        ScheduledFuture<?> timeout = this.storeTimeouts.remove(playerId);
        if (timeout == null) {
            return;
        }
        timeout.cancel(false);
        this.storeSessionByPlayer.remove(playerId, sessionId);
        this.storeOpenedAt.remove(playerId);

        CustomUIPage currentPage = playerComponent.getPageManager().getCustomPage();
        if (currentPage instanceof PowerUpStorePage) {
            ((PowerUpStorePage)currentPage).requestClose();
        }

        World world = ((EntityStore)store.getExternalData()).getWorld();
        if (world != null) {
            this.gamePauseController.endStorePause(world, store);
        }
    }

    private ScheduledFuture<?> schedulePlayerWorldTask(@Nonnull World world, @Nonnull PlayerRef playerRefComponent, @Nonnull Runnable action, long delayMillis) {
        UUID playerId = playerRefComponent.getUuid();
        final ScheduledFuture<?>[] holder = new ScheduledFuture<?>[1];
        ScheduledFuture<?> future = HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
            ScheduledFuture<?> self = holder[0];
            if (self != null) {
                untrackPlayerTask(playerId, self);
            }
            if (!isPlayerInWorld(playerRefComponent, world)) {
                return;
            }
            executeIfTicking(world, action);
        }, delayMillis, TimeUnit.MILLISECONDS);
        holder[0] = future;
        trackPlayerTask(playerId, future);
        return future;
    }

    private void executeIfTicking(@Nonnull World world, @Nonnull Runnable action) {
        if (!world.isTicking()) {
            return;
        }
        try {
            world.execute(action);
        } catch (IllegalThreadStateException exception) {
            this.plugin.getLogger().at(Level.FINE).log("Skipped world task because world is no longer in a valid ticking state.");
        }
    }

    private void trackPlayerTask(@Nonnull UUID playerId, @Nonnull ScheduledFuture<?> future) {
        this.delayedTasksByPlayer.computeIfAbsent(playerId, id -> new ConcurrentLinkedQueue<>()).add(future);
    }

    private void untrackPlayerTask(@Nonnull UUID playerId, @Nonnull ScheduledFuture<?> future) {
        ConcurrentLinkedQueue<ScheduledFuture<?>> tasks = this.delayedTasksByPlayer.get(playerId);
        if (tasks == null) {
            return;
        }
        tasks.remove(future);
        if (tasks.isEmpty()) {
            this.delayedTasksByPlayer.remove(playerId, tasks);
        }
    }

    private void cancelPlayerTasks(@Nonnull UUID playerId) {
        ConcurrentLinkedQueue<ScheduledFuture<?>> tasks = this.delayedTasksByPlayer.remove(playerId);
        if (tasks == null) {
            return;
        }
        for (ScheduledFuture<?> task : tasks) {
            if (task != null) {
                task.cancel(false);
            }
        }
    }

    private static boolean isPlayerInWorld(@Nonnull PlayerRef playerRefComponent, @Nonnull World world) {
        Ref<EntityStore> playerRef = playerRefComponent.getReference();
        if (playerRef == null || !playerRef.isValid()) {
            return false;
        }
        Store<EntityStore> store = playerRef.getStore();
        if (store == null) {
            return false;
        }
        World currentWorld = ((EntityStore)store.getExternalData()).getWorld();
        if (currentWorld == null) {
            return false;
        }
        WorldConfig currentConfig = currentWorld.getWorldConfig();
        WorldConfig targetConfig = world.getWorldConfig();
        if (currentConfig != null && targetConfig != null && currentConfig.getUuid() != null && targetConfig.getUuid() != null) {
            return currentConfig.getUuid().equals(targetConfig.getUuid());
        }
        String currentName = currentWorld.getName();
        String targetName = world.getName();
        if (currentName != null && targetName != null) {
            return currentName.equals(targetName);
        }
        return currentWorld == world;
    }
}
