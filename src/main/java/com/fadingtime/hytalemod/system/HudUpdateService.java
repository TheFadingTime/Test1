/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.logger.HytaleLogger$Api
 *  com.hypixel.hytale.server.core.HytaleServer
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.world.World
 *  com.hypixel.hytale.server.core.universe.world.WorldConfig
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 *  javax.annotation.Nonnull
 */
package com.fadingtime.hytalemod.system;

import com.fadingtime.hytalemod.HytaleMod;
import com.fadingtime.hytalemod.system.BossHudSystem;
import com.fadingtime.hytalemod.ui.BossBarHud;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldConfig;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.annotation.Nonnull;

public final class HudUpdateService {
    private final HytaleMod plugin;
    private final String defaultBossName;
    private final long hudReadyDelayMs;
    private final ConcurrentMap<UUID, ScheduledFuture<?>> hudUpdateTasks = new ConcurrentHashMap();

    public HudUpdateService(@Nonnull HytaleMod hytaleMod, @Nonnull String string, long l) {
        this.plugin = hytaleMod;
        this.defaultBossName = string;
        this.hudReadyDelayMs = l;
    }

    public BossBarHud ensureHud(@Nonnull Player player, @Nonnull PlayerRef playerRef, @Nonnull Store<EntityStore> store, boolean bl) {
        CustomUIHud customUIHud = player.getHudManager().getCustomHud();
        if (customUIHud instanceof BossBarHud) {
            if (bl) {
                ((BossBarHud)customUIHud).setLevelHudVisible(true);
            }
            return (BossBarHud)customUIHud;
        }
        if (customUIHud != null) {
            if (!bl) {
                return null;
            }
        }
        BossBarHud bossBarHud = new BossBarHud(playerRef, this.defaultBossName, 1.0f);
        player.getHudManager().setCustomHud(playerRef, (CustomUIHud)bossBarHud);
        return bossBarHud;
    }

    public void clearHudForPlayer(@Nonnull Player player, @Nonnull PlayerRef playerRef, @Nonnull UUID uUID) {
        CustomUIHud customUIHud = player.getHudManager().getCustomHud();
        if (customUIHud != null) {
            if (customUIHud instanceof BossBarHud) {
                ((BossBarHud)customUIHud).setBossVisible(false);
                ((BossBarHud)customUIHud).setLevelHudVisible(false);
            } else {
            }
        } else {
        }
        BossHudSystem bossHudSystem = HytaleMod.getInstance().getBossHudSystem();
        if (bossHudSystem != null) {
            bossHudSystem.clearBossHud(uUID);
        }
    }

    public void scheduleHudUpdate(@Nonnull World world, @Nonnull PlayerRef playerRef, @Nonnull Runnable runnable) {
        UUID uUID = playerRef.getUuid();
        ScheduledFuture scheduledFuture = (ScheduledFuture)this.hudUpdateTasks.remove(uUID);
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
        ScheduledFuture<?> scheduledFuture2 = HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
            this.hudUpdateTasks.remove(uUID);
            if (!HudUpdateService.isPlayerInWorld(playerRef, world)) {
                return;
            }
            this.executeIfTicking(world, runnable);
        }, this.hudReadyDelayMs, TimeUnit.MILLISECONDS);
        this.hudUpdateTasks.put(uUID, scheduledFuture2);
    }

    public void cancelHudUpdate(@Nonnull UUID uUID) {
        ScheduledFuture scheduledFuture = (ScheduledFuture)this.hudUpdateTasks.remove(uUID);
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
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

