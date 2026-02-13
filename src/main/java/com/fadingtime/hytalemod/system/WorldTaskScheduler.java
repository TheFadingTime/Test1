/*
 * NEW FILE: Reusable per-player delayed task scheduler extracted from
 * PlayerProgressionManager and StoreSessionManager.
 *
 * PRINCIPLE (Single Responsibility + DRY):
 *   Both PlayerProgressionManager and StoreSessionManager had identical copies of:
 *     - schedulePlayerWorldTask (schedule a delayed action)
 *     - trackPlayerTask (remember the future so we can cancel it)
 *     - untrackPlayerTask (clean up after the task runs)
 *     - cancelPlayerTasks (cancel all pending tasks for a disconnecting player)
 *
 *   These four methods form a cohesive unit — they always appear together and
 *   operate on the same ConcurrentMap. That's a strong signal they belong in
 *   their own class.
 *
 * WHY each caller gets their own instance:
 *   PlayerProgressionManager and StoreSessionManager each create their own
 *   WorldTaskScheduler. This is intentional — their task lifecycles are independent.
 *   Sharing a single scheduler would mean cancelling store tasks also cancels
 *   progression tasks, which would be a bug.
 *
 * BEGINNER SMELL: "Accidental coupling through shared state" — if two systems
 *   share the same ConcurrentMap for unrelated tasks, cancelling one system's
 *   tasks could cancel the other's. Each system needs its own task queue.
 */
package com.fadingtime.hytalemod.system;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import com.hypixel.hytale.logger.HytaleLogger;
import javax.annotation.Nonnull;

public final class WorldTaskScheduler {

    private final HytaleLogger logger;
    private final ConcurrentMap<UUID, ConcurrentLinkedQueue<ScheduledFuture<?>>> delayedTasksByPlayer = new ConcurrentHashMap<>();

    public WorldTaskScheduler(@Nonnull HytaleLogger logger) {
        this.logger = logger;
    }

    /**
     * Schedule a delayed task that runs on the world thread, with automatic
     * player-presence checking and task lifecycle management.
     */
    public ScheduledFuture<?> schedule(@Nonnull World world, @Nonnull PlayerRef playerRefComponent, @Nonnull Runnable action, long delayMillis) {
        UUID playerId = playerRefComponent.getUuid();
        final ScheduledFuture<?>[] holder = new ScheduledFuture<?>[1];
        ScheduledFuture<?> future = HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
            ScheduledFuture<?> self = holder[0];
            if (self != null) {
                untrack(playerId, self);
            }
            if (!WorldUtils.isPlayerInWorld(playerRefComponent, world)) {
                return;
            }
            WorldUtils.executeIfTicking(world, action, this.logger);
        }, delayMillis, TimeUnit.MILLISECONDS);
        holder[0] = future;
        track(playerId, future);
        return future;
    }

    /** Cancel all pending tasks for a player (e.g., on disconnect). */
    public void cancelAll(@Nonnull UUID playerId) {
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

    private void track(@Nonnull UUID playerId, @Nonnull ScheduledFuture<?> future) {
        this.delayedTasksByPlayer.computeIfAbsent(playerId, id -> new ConcurrentLinkedQueue<>()).add(future);
    }

    private void untrack(@Nonnull UUID playerId, @Nonnull ScheduledFuture<?> future) {
        ConcurrentLinkedQueue<ScheduledFuture<?>> tasks = this.delayedTasksByPlayer.get(playerId);
        if (tasks == null) {
            return;
        }
        tasks.remove(future);
        if (tasks.isEmpty()) {
            this.delayedTasksByPlayer.remove(playerId, tasks);
        }
    }
}
