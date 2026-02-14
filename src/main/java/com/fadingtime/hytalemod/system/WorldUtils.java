/*
 * NEW FILE: Shared utility methods extracted from PlayerProgressionManager,
 * StoreSessionManager, HudUpdateService, and MobWaveSpawner.
 *
 * PRINCIPLE (DRY — Don't Repeat Yourself):
 *   These methods were copy-pasted identically into 3-4 files. That means:
 *     1. A bug fix in one copy might not reach the others.
 *     2. Readers can't tell if the copies are SUPPOSED to be identical or if
 *        the differences (instance vs static, slightly different logging) are intentional.
 *     3. Every new class that needs this behavior copies it again.
 *
 *   By extracting to a shared utility, there's ONE place to fix bugs, ONE place to
 *   understand the behavior, and new classes just call the shared method.
 *
 * BEGINNER SMELL: "Copy-paste programming" — when you need the same logic in a
 *   second class, the instinct is to copy it. This works short-term but creates
 *   a maintenance trap. Instead, extract to a shared location on the SECOND use
 *   (not the first — don't pre-extract things that might only be used once).
 *
 * WHY this is a static utility class (not an interface or base class):
 *   These methods don't represent a common identity or contract — they're just
 *   shared helper functions. Java's "static utility class" pattern (private constructor,
 *   static methods) is the right tool for that. Using inheritance just to share code
 *   is called "implementation inheritance abuse" and creates tight coupling.
 */
package com.fadingtime.hytalemod.system;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldConfig;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.logger.HytaleLogger;
import java.util.logging.Level;
import javax.annotation.Nonnull;

public final class WorldUtils {

    private WorldUtils() {
        // Static utility class — no instances needed.
    }

    /**
     * Check if a player is still in the expected world. Used as a safety check
     * before executing delayed tasks — the player might have left the world
     * between when the task was scheduled and when it fires.
     */
    public static boolean isPlayerInWorld(@Nonnull PlayerRef playerRefComponent, @Nonnull World world) {
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

    /**
     * Get a stable key for the world a store belongs to, used for per-world state lookups.
     * Falls back through UUID -> display name -> "default" for robustness.
     */
    public static String getWorldKey(@Nonnull Store<EntityStore> store) {
        World world = ((EntityStore)store.getExternalData()).getWorld();
        if (world == null) {
            return "default";
        }
        if (world.getWorldConfig() != null && world.getWorldConfig().getUuid() != null) {
            return "world-" + world.getWorldConfig().getUuid().toString();
        }
        String displayName = world.getWorldConfig() != null ? world.getWorldConfig().getDisplayName() : null;
        if (displayName != null && !displayName.isBlank()) {
            return "world-" + displayName.trim();
        }
        return "default";
    }

    /**
     * Execute an action on a world's thread, but only if the world is still ticking.
     * This prevents IllegalThreadStateException when the world is shutting down.
     *
     * The logger parameter avoids coupling this utility to any specific class.
     */
    public static void executeIfTicking(@Nonnull World world, @Nonnull Runnable action, @Nonnull HytaleLogger logger) {
        if (!world.isTicking()) {
            return;
        }
        try {
            world.execute(action);
        } catch (IllegalThreadStateException exception) {
            logger.at(Level.FINE).log("Skipped world task because world is no longer in a valid ticking state.");
        }
    }
}
