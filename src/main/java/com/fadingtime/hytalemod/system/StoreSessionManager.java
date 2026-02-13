/*
 * REFACTOR: Replaced duplicated utility methods with shared WorldUtils and WorldTaskScheduler.
 *
 * WHAT CHANGED:
 *   - Removed private copies of isPlayerInWorld(), executeIfTicking(),
 *     schedulePlayerWorldTask(), trackPlayerTask(), untrackPlayerTask(), cancelPlayerTasks().
 *   - Now uses WorldUtils for static helpers and a WorldTaskScheduler instance for
 *     per-player task lifecycle management.
 *   - Removed the delayedTasksByPlayer ConcurrentMap (now inside WorldTaskScheduler).
 *
 * PRINCIPLE (DRY):
 *   These 6 methods were identical copies from PlayerProgressionManager. The duplication
 *   happened because it was easier to copy than to extract when the code was first written.
 *   Now there's ONE implementation to maintain.
 */
package com.fadingtime.hytalemod.system;

import com.fadingtime.hytalemod.HytaleMod;
import com.fadingtime.hytalemod.ui.PowerUpStorePage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import javax.annotation.Nonnull;

public final class StoreSessionManager {
    private final HytaleMod plugin;
    private final GamePauseController gamePauseController;
    private volatile long storeInputGraceMs;
    private final ConcurrentMap<UUID, ScheduledFuture<?>> storeTimeouts = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Integer> storeSessionByPlayer = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Long> storeOpenedAt = new ConcurrentHashMap<>();
    // Each system gets its own WorldTaskScheduler so cancelling store tasks
    // doesn't interfere with other systems' scheduled tasks.
    private final WorldTaskScheduler taskScheduler;

    public StoreSessionManager(@Nonnull HytaleMod plugin, @Nonnull GamePauseController gamePauseController, long storeInputGraceMs) {
        this.plugin = plugin;
        this.gamePauseController = gamePauseController;
        this.storeInputGraceMs = storeInputGraceMs;
        this.taskScheduler = new WorldTaskScheduler(plugin.getLogger());
    }

    public void updateConfig(long storeInputGraceMs) {
        this.storeInputGraceMs = storeInputGraceMs;
    }

    public void openStoreForPlayer(@Nonnull Ref<EntityStore> playerRef, @Nonnull Store<EntityStore> store, @Nonnull Player playerComponent, @Nonnull PlayerRef playerRefComponent, int level) {
        UUID playerId = playerRefComponent.getUuid();
        World world = ((EntityStore)store.getExternalData()).getWorld();
        if (world == null) {
            return;
        }

        int sessionId = this.storeSessionByPlayer.compute(playerId, (id, value) -> value == null ? 1 : value + 1);
        // Now delegates to the shared WorldTaskScheduler instead of the local copy.
        ScheduledFuture<?> timeout = this.taskScheduler.schedule(world, playerRefComponent, () -> closeStoreInternal(playerRef, store, playerComponent, playerRefComponent, sessionId), 30000L);
        ScheduledFuture<?> existing = this.storeTimeouts.put(playerId, timeout);
        if (existing != null) {
            existing.cancel(false);
        }

        for (int i = 1; i <= 8; ++i) {
            final float factor = this.gamePauseController.computeSlowmoFactor(i, 8);
            long delay = 1800L * i / 8L;
            this.taskScheduler.schedule(world, playerRefComponent, () -> this.gamePauseController.applyTimeScale(store, factor), delay);
        }

        this.taskScheduler.schedule(world, playerRefComponent, () -> {
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
        this.taskScheduler.cancelAll(playerId);
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
}
