/*
 * REFACTOR: Replaced duplicated isPlayerInWorld() and executeIfTicking() with WorldUtils.
 *
 * WHAT CHANGED:
 *   - Deleted the private copies of isPlayerInWorld() and executeIfTicking().
 *   - Now calls WorldUtils.isPlayerInWorld() and WorldUtils.executeIfTicking().
 *
 * PRINCIPLE (DRY): Same utility logic was copied into this file, PlayerProgressionManager,
 *   and StoreSessionManager. Three copies, same code. Now there's one.
 */
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
    private volatile String defaultBossName;
    private volatile long hudReadyDelayMs;
    private final ConcurrentMap<UUID, ScheduledFuture<?>> hudUpdateTasks = new ConcurrentHashMap<>();

    public HudUpdateService(@Nonnull HytaleMod plugin, @Nonnull String defaultBossName, long hudReadyDelayMs) {
        this.plugin = plugin;
        this.defaultBossName = defaultBossName;
        this.hudReadyDelayMs = hudReadyDelayMs;
    }

    public void updateConfig(@Nonnull String defaultBossName, long hudReadyDelayMs) {
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
            // Now uses the shared utility instead of a private copy.
            if (!WorldUtils.isPlayerInWorld(playerRefComponent, world)) {
                return;
            }
            WorldUtils.executeIfTicking(world, action, this.plugin.getLogger());
        }, this.hudReadyDelayMs, TimeUnit.MILLISECONDS);
        this.hudUpdateTasks.put(playerId, future);
    }

    public void cancelHudUpdate(@Nonnull UUID playerId) {
        ScheduledFuture<?> existing = this.hudUpdateTasks.remove(playerId);
        if (existing != null) {
            existing.cancel(false);
        }
    }

    // DELETED: private copies of isPlayerInWorld() and executeIfTicking().
    // They now live in WorldUtils where all three consumers can share them.
}
