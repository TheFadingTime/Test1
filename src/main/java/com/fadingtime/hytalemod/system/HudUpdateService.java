package com.fadingtime.hytalemod.system;

import com.fadingtime.hytalemod.HytaleMod;
import com.fadingtime.hytalemod.ui.BossBarHud;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
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
    private final ConcurrentMap<UUID, ScheduledFuture<?>> hudUpdateTasks = new ConcurrentHashMap<>();

    public HudUpdateService(@Nonnull HytaleMod plugin, @Nonnull String defaultBossName, long hudReadyDelayMs) {
        this.plugin = plugin;
        this.defaultBossName = defaultBossName;
        this.hudReadyDelayMs = hudReadyDelayMs;
    }

    public BossBarHud ensureHud(@Nonnull Player playerComponent, @Nonnull PlayerRef playerRefComponent, @Nonnull Store<EntityStore> store, boolean inGameplayWorld) {
        CustomUIHud currentHud = playerComponent.getHudManager().getCustomHud();
        if (currentHud instanceof BossBarHud) {
            if (inGameplayWorld) {
                ((BossBarHud)currentHud).setLevelHudVisible(true);
            }
            return (BossBarHud)currentHud;
        }
        if (currentHud != null) {
            if (!inGameplayWorld) {
                this.plugin.getLogger().at(Level.INFO).log("[HUD_DEBUG] ensureHud blocked: existing=%s player=%s", currentHud.getClass().getSimpleName(), playerRefComponent.getUuid());
                return null;
            }
            this.plugin.getLogger().at(Level.INFO).log("[HUD_DEBUG] ensureHud replace: existing=%s player=%s", currentHud.getClass().getSimpleName(), playerRefComponent.getUuid());
        }
        BossBarHud hud = new BossBarHud(playerRefComponent, this.defaultBossName, 1.0f);
        this.plugin.getLogger().at(Level.INFO).log("[HUD_DEBUG] ensureHud create player=%s", playerRefComponent.getUuid());
        playerComponent.getHudManager().setCustomHud(playerRefComponent, hud);
        return hud;
    }

    public void clearHudForPlayer(@Nonnull Player playerComponent, @Nonnull PlayerRef playerRefComponent, @Nonnull UUID playerId) {
        this.plugin.getLogger().at(Level.INFO).log("[HUD_DEBUG] clearHudForPlayer start player=%s", playerId);
        CustomUIHud currentHud = playerComponent.getHudManager().getCustomHud();
        if (currentHud != null) {
            if (currentHud instanceof BossBarHud) {
                this.plugin.getLogger().at(Level.INFO).log("[HUD_DEBUG] hide BossBarHud player=%s", playerId);
                ((BossBarHud)currentHud).setBossVisible(false);
                ((BossBarHud)currentHud).setLevelHudVisible(false);
            } else {
                this.plugin.getLogger().at(Level.INFO).log("[HUD_DEBUG] resetHud suppressed (to avoid crash) player=%s hud=%s", playerId, currentHud.getClass().getSimpleName());
            }
        } else {
            this.plugin.getLogger().at(Level.INFO).log("[HUD_DEBUG] resetHud suppressed (no currentHud) player=%s", playerId);
        }

        BossHudSystem bossHudSystem = HytaleMod.getInstance().getBossHudSystem();
        if (bossHudSystem != null) {
            this.plugin.getLogger().at(Level.INFO).log("[HUD_DEBUG] clearBossHud call player=%s", playerId);
            bossHudSystem.clearBossHud(playerId);
        }
    }

    public void scheduleHudUpdate(@Nonnull World world, @Nonnull PlayerRef playerRefComponent, @Nonnull Runnable action) {
        UUID playerId = playerRefComponent.getUuid();
        ScheduledFuture<?> existing = this.hudUpdateTasks.remove(playerId);
        if (existing != null) {
            existing.cancel(false);
        }
        ScheduledFuture<?> future = HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
            this.hudUpdateTasks.remove(playerId);
            if (!isPlayerInWorld(playerRefComponent, world)) {
                return;
            }
            executeIfTicking(world, action);
        }, this.hudReadyDelayMs, TimeUnit.MILLISECONDS);
        this.hudUpdateTasks.put(playerId, future);
    }

    public void cancelHudUpdate(@Nonnull UUID playerId) {
        ScheduledFuture<?> existing = this.hudUpdateTasks.remove(playerId);
        if (existing != null) {
            existing.cancel(false);
        }
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
