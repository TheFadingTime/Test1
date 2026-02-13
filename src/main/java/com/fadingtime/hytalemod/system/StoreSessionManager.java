/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.logger.HytaleLogger$Api
 *  com.hypixel.hytale.server.core.HytaleServer
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.world.World
 *  com.hypixel.hytale.server.core.universe.world.WorldConfig
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 *  javax.annotation.Nonnull
 */
package com.fadingtime.hytalemod.system;

import com.fadingtime.hytalemod.HytaleMod;
import com.fadingtime.hytalemod.system.GamePauseController;
import com.fadingtime.hytalemod.ui.PowerUpStorePage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
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
    private final ConcurrentMap<UUID, ScheduledFuture<?>> storeTimeouts = new ConcurrentHashMap();
    private final ConcurrentMap<UUID, Integer> storeSessionByPlayer = new ConcurrentHashMap<UUID, Integer>();
    private final ConcurrentMap<UUID, Long> storeOpenedAt = new ConcurrentHashMap<UUID, Long>();
    private final ConcurrentMap<UUID, ConcurrentLinkedQueue<ScheduledFuture<?>>> delayedTasksByPlayer = new ConcurrentHashMap();

    public StoreSessionManager(@Nonnull HytaleMod hytaleMod, @Nonnull GamePauseController gamePauseController, long l) {
        this.plugin = hytaleMod;
        this.gamePauseController = gamePauseController;
        this.storeInputGraceMs = l;
    }

    public void openStoreForPlayer(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull Player player, @Nonnull PlayerRef playerRef, int n2) {
        UUID uUID2 = playerRef.getUuid();
        World world = ((EntityStore)store.getExternalData()).getWorld();
        if (world == null) {
            return;
        }
        this.cancelPlayerTasks(uUID2);
        int n3 = this.storeSessionByPlayer.compute(uUID2, (uUID, n) -> n == null ? 1 : n + 1);
        ScheduledFuture<?> scheduledFuture = this.schedulePlayerWorldTask(world, playerRef, () -> this.closeStoreInternal(ref, store, player, playerRef, n3), 30000L);
        ScheduledFuture<?> scheduledFuture2 = this.storeTimeouts.put(uUID2, scheduledFuture);
        if (scheduledFuture2 != null) {
            scheduledFuture2.cancel(false);
        }
        for (int i = 1; i <= 8; ++i) {
            float f = this.gamePauseController.computeSlowmoFactor(i, 8);
            long l = 1800L * (long)i / 8L;
            this.schedulePlayerWorldTask(world, playerRef, () -> {
                Integer sessionId = (Integer)this.storeSessionByPlayer.get(uUID2);
                if (sessionId == null || sessionId != n3) {
                    return;
                }
                this.gamePauseController.applyTimeScale(store, f);
            }, l);
        }
        this.schedulePlayerWorldTask(world, playerRef, () -> {
            Integer currentSession = (Integer)this.storeSessionByPlayer.get(uUID2);
            if (currentSession == null || currentSession != n3) {
                return;
            }
            try {
                this.gamePauseController.beginStorePause(world, store);
                this.storeOpenedAt.put(uUID2, System.currentTimeMillis());
                player.getPageManager().openCustomPage(ref, store, (CustomUIPage)new PowerUpStorePage(playerRef, n2));
            }
            catch (RuntimeException runtimeException) {
                ((HytaleLogger.Api)this.plugin.getLogger().at(Level.WARNING).withCause((Throwable)runtimeException)).log("Store failed for " + uUID2);
                ScheduledFuture<?> timeoutTask = this.storeTimeouts.remove(uUID2);
                if (timeoutTask != null) {
                    timeoutTask.cancel(false);
                }
                this.storeSessionByPlayer.remove(uUID2, currentSession);
                this.storeOpenedAt.remove(uUID2);
                this.cancelPlayerTasks(uUID2);
                this.gamePauseController.forceResume(world, store);
            }
        }, 1800L);
    }

    public void closeStoreForPlayer(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        PlayerRef playerRef = (PlayerRef)store.getComponent(ref, PlayerRef.getComponentType());
        Player player = (Player)store.getComponent(ref, Player.getComponentType());
        if (playerRef == null || player == null) {
            return;
        }
        World world = ((EntityStore)store.getExternalData()).getWorld();
        if (world == null) {
            return;
        }
        Integer n = (Integer)this.storeSessionByPlayer.get(playerRef.getUuid());
        if (n == null) {
            return;
        }
        world.execute(() -> this.closeStoreInternal(ref, store, player, playerRef, n));
    }

    public boolean canAcceptStoreSelection(@Nonnull UUID uUID) {
        Long l = (Long)this.storeOpenedAt.get(uUID);
        if (l == null) {
            return true;
        }
        return System.currentTimeMillis() - l >= this.storeInputGraceMs;
    }

    public void cancelAllScheduledTasks(@Nonnull UUID uUID) {
        ScheduledFuture scheduledFuture = (ScheduledFuture)this.storeTimeouts.remove(uUID);
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
        this.cancelPlayerTasks(uUID);
        this.storeSessionByPlayer.remove(uUID);
        this.storeOpenedAt.remove(uUID);
    }

    private void closeStoreInternal(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull Player player, @Nonnull PlayerRef playerRef, int n) {
        World world;
        UUID uUID = playerRef.getUuid();
        Integer n2 = (Integer)this.storeSessionByPlayer.get(uUID);
        if (n2 == null || n2 != n) {
            return;
        }
        ScheduledFuture scheduledFuture = (ScheduledFuture)this.storeTimeouts.remove(uUID);
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
        this.storeSessionByPlayer.remove(uUID, n);
        this.storeOpenedAt.remove(uUID);
        this.cancelPlayerTasks(uUID);
        CustomUIPage customUIPage = player.getPageManager().getCustomPage();
        if (customUIPage instanceof PowerUpStorePage) {
            ((PowerUpStorePage)customUIPage).requestClose();
        }
        if ((world = ((EntityStore)store.getExternalData()).getWorld()) != null) {
            this.gamePauseController.endStorePause(world, store);
        }
    }

    private ScheduledFuture<?> schedulePlayerWorldTask(@Nonnull World world, @Nonnull PlayerRef playerRef, @Nonnull Runnable runnable, long l) {
        ScheduledFuture<?>[] scheduledFutureArray = new ScheduledFuture[1];
        UUID uUID = playerRef.getUuid();
        ScheduledFuture<?> scheduledFuture = HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
            ScheduledFuture<?> trackedFuture = scheduledFutureArray[0];
            if (trackedFuture != null) {
                this.untrackPlayerTask(uUID, trackedFuture);
            }
            if (!StoreSessionManager.isPlayerInWorld(playerRef, world)) {
                return;
            }
            this.executeIfTicking(world, runnable);
        }, l, TimeUnit.MILLISECONDS);
        scheduledFutureArray[0] = scheduledFuture;
        this.trackPlayerTask(uUID, scheduledFuture);
        return scheduledFuture;
    }

    private void executeIfTicking(@Nonnull World world, @Nonnull Runnable runnable) {
        if (!world.isTicking()) {
            return;
        }
        try {
            world.execute(runnable);
        }
        catch (IllegalThreadStateException illegalThreadStateException) {
        }
    }

    private void trackPlayerTask(@Nonnull UUID uUID2, @Nonnull ScheduledFuture<?> scheduledFuture) {
        this.delayedTasksByPlayer.computeIfAbsent(uUID2, uUID -> new ConcurrentLinkedQueue()).add(scheduledFuture);
    }

    private void untrackPlayerTask(@Nonnull UUID uUID, @Nonnull ScheduledFuture<?> scheduledFuture) {
        ConcurrentLinkedQueue<ScheduledFuture<?>> concurrentLinkedQueue = this.delayedTasksByPlayer.get(uUID);
        if (concurrentLinkedQueue == null) {
            return;
        }
        concurrentLinkedQueue.remove(scheduledFuture);
        if (concurrentLinkedQueue.isEmpty()) {
            this.delayedTasksByPlayer.remove(uUID, concurrentLinkedQueue);
        }
    }

    private void cancelPlayerTasks(@Nonnull UUID uUID) {
        ConcurrentLinkedQueue<ScheduledFuture<?>> concurrentLinkedQueue = this.delayedTasksByPlayer.remove(uUID);
        if (concurrentLinkedQueue == null) {
            return;
        }
        for (ScheduledFuture<?> scheduledFuture : concurrentLinkedQueue) {
            if (scheduledFuture == null) continue;
            scheduledFuture.cancel(false);
        }
    }

    private static boolean isPlayerInWorld(@Nonnull PlayerRef playerRef, @Nonnull World world) {
        Ref ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return false;
        }
        Store store = ref.getStore();
        if (store == null) {
            return false;
        }
        World world2 = ((EntityStore)store.getExternalData()).getWorld();
        if (world2 == null) {
            return false;
        }
        WorldConfig worldConfig = world2.getWorldConfig();
        WorldConfig worldConfig2 = world.getWorldConfig();
        if (worldConfig != null && worldConfig2 != null && worldConfig.getUuid() != null && worldConfig2.getUuid() != null) {
            return worldConfig.getUuid().equals(worldConfig2.getUuid());
        }
        String string = world2.getName();
        String string2 = world.getName();
        if (string != null && string2 != null) {
            return string.equals(string2);
        }
        return world2 == world;
    }
}
