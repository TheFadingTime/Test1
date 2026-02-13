/*
 * NEW FILE: Type-safe enum replacing string-based power-up dispatch.
 *
 * PRINCIPLE (Type Safety):
 *   Before this change, power-up types were raw strings like "fire_rate" scattered
 *   across 4 files. If you misspelled one — say "fire_Rate" or "firerate" — the
 *   compiler wouldn't catch it. The bug would silently do nothing at runtime.
 *
 *   With an enum, the compiler catches typos at compile time. You also get:
 *     - IDE autocomplete (no more guessing the exact string)
 *     - Exhaustive switch checks (compiler warns if you forget a case)
 *     - A single place to see ALL valid power-up types
 *
 * BEGINNER SMELL: "String-typed enumerations" — using raw strings like
 *   "fire_rate", "bounce", "lucky" where a fixed set of values is known at
 *   compile time. This is one of the most common anti-patterns in beginner code
 *   and AI-generated code. If you find yourself writing if/else chains comparing
 *   the same string variable, that's a strong signal you need an enum.
 *
 * WHY key() exists:
 *   The UI sends power-up choices over the wire as strings (via BsonDocument).
 *   We can't change the wire format, but we CAN parse the string into an enum
 *   at the boundary (where it enters our code) and use the enum everywhere else.
 *   key() preserves wire compatibility while giving us type safety internally.
 */
package com.fadingtime.hytalemod.system;

import java.util.Locale;
import javax.annotation.Nullable;

public enum PowerUpType {
    EXTRA_PROJECTILE("extra_projectile", "EXTRA PROJECTILE"),
    FIRE_RATE("fire_rate", "FIRE RATE"),
    PICKUP_RANGE("pickup_range", "PICKUP RANGE"),
    BOUNCE("bounce", "BOUNCE"),
    WEAPON_DAMAGE("weapon_damage", "WEAPON DAMAGE"),
    MAX_HEALTH("max_health", "MORE HEALTH"),
    MOVE_SPEED("move_speed", "MOVE SPEED"),
    LUCKY("lucky", "LUCKY"),
    PROJECTILE_RAIN("projectile_rain", "LAST RESORT...");

    private final String key;
    private final String label;

    PowerUpType(String key, String label) {
        this.key = key;
        this.label = label;
    }

    /** The wire-format string sent to/from the UI. */
    public String key() {
        return this.key;
    }

    /** The human-readable display label for the store UI. */
    public String label() {
        return this.label;
    }

    /**
     * Parse a wire-format string into a PowerUpType.
     * Returns null for unrecognized values — this is the boundary where
     * untrusted input enters our type-safe world.
     */
    @Nullable
    public static PowerUpType fromKey(@Nullable String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        // Linear scan is fine for 9 values. Don't over-engineer with a Map
        // unless you've measured a performance problem. Premature optimization
        // is another common beginner trap.
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        for (PowerUpType type : values()) {
            if (type.key.equals(normalized)) {
                return type;
            }
        }
        return null;
    }
}
