/*
 * REFACTOR: Cached repeated singleton lookups into a local variable.
 *
 * WHAT CHANGED:
 *   - HytaleMod.getInstance().getLifeEssenceLevelSystem() was called 25 times
 *     in build(). Now it's called once and stored in a local variable.
 *   - Same treatment for handleDataEvent() (3 calls -> 1).
 *
 * PRINCIPLE (DRY — Don't Repeat Yourself):
 *   When you see the same long expression repeated many times, it's a sign you
 *   should store the result in a local variable. This is NOT premature optimization —
 *   it's a readability improvement. The reader shouldn't have to parse
 *   "HytaleMod.getInstance().getLifeEssenceLevelSystem()" 25 times to understand
 *   that it's always the same object.
 *
 * BEGINNER SMELL: "Shotgun access" — calling a long chain of getters repeatedly
 *   instead of caching the result. This is extremely common in AI-generated code
 *   because each line is generated independently without awareness of what came before.
 */
package com.fadingtime.hytalemod.ui;

import com.fadingtime.hytalemod.HytaleMod;
import com.fadingtime.hytalemod.system.PlayerProgressionManager;
import com.fadingtime.hytalemod.system.PowerUpType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.bson.BsonDocument;

public class PowerUpStorePage
extends CustomUIPage {
    private static final String UI_PATH = "Pages/PowerUpStore.ui";
    private static final Logger LOGGER = Logger.getLogger(PowerUpStorePage.class.getName());
    private final int level;

    public PowerUpStorePage(@Nonnull PlayerRef playerRef, int level) {
        super(playerRef, CustomPageLifetime.CantClose);
        this.level = level;
    }

    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder builder, @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        builder.append(UI_PATH);
        builder.set("#StoreTitle.Text", "LEVEL " + this.level + " - CHOOSE A POWER-UP!");
        PlayerRef playerRefComponent = (PlayerRef)store.getComponent(ref, PlayerRef.getComponentType());
        UUID playerId = playerRefComponent != null ? playerRefComponent.getUuid() : null;

        // Cache the progression manager reference once instead of calling the
        // getInstance().getLifeEssenceLevelSystem() chain 25 times.
        // This makes the code dramatically easier to read AND avoids redundant lookups.
        PlayerProgressionManager progression = HytaleMod.getInstance().getLifeEssenceLevelSystem();

        boolean fireRateMax = false;
        boolean pickupRangeMax = false;
        boolean extraProjectileMax = false;
        int fireRateRank = 0;
        int pickupRangeRank = 0;
        int extraProjectileRank = 0;
        int bounceRank = 0;
        int weaponDamageRank = 0;
        int healthRank = 0;
        int speedRank = 0;
        int luckyRank = 0;
        int projectileRainRank = 0;
        int fireRateMaxRank = progression.getMaxFireRateUpgrades();
        int pickupRangeMaxRank = progression.getMaxPickupRangeUpgrades();
        int extraProjectileMaxRank = progression.getMaxExtraProjectileUpgrades();
        int bounceMaxRank = progression.getMaxBounceUpgrades();
        int weaponDamageMaxRank = progression.getMaxWeaponDamageUpgrades();
        int healthMaxRank = progression.getMaxHealthUpgrades();
        int speedMaxRank = progression.getMaxSpeedUpgrades();
        int luckyMaxRank = progression.getMaxLuckyUpgrades();
        int projectileRainMaxRank = progression.getMaxProjectileRainUpgrades();
        if (playerId != null) {
            fireRateMax = progression.hasFireRatePower(playerId, store);
            pickupRangeMax = progression.hasPickupRangePower(playerId, store);
            extraProjectileMax = progression.hasExtraProjectilePower(playerId, store);
            fireRateRank = progression.getFireRateRank(playerId, store);
            pickupRangeRank = progression.getPickupRangeRank(playerId, store);
            extraProjectileRank = progression.getExtraProjectileRank(playerId, store);
            bounceRank = progression.getBounceRank(playerId, store);
            weaponDamageRank = progression.getWeaponDamageRank(playerId, store);
            healthRank = progression.getHealthRank(playerId, store);
            speedRank = progression.getSpeedRank(playerId, store);
            luckyRank = progression.getLuckyRank(playerId, store);
            projectileRainRank = progression.getProjectileRainRank(playerId, store);
        }
        // Use PowerUpType enum constants instead of raw strings. The key() method
        // returns the wire-format string the UI expects, and label() gives the display name.
        // This means the StoreOption definitions are now checked at compile time.
        ArrayList<StoreOption> options = new ArrayList<StoreOption>();
        options.add(new StoreOption(PowerUpType.FIRE_RATE, "+15% attack speed", "Store/Potion.png", fireRateRank, fireRateMaxRank, fireRateMax));
        options.add(new StoreOption(PowerUpType.PICKUP_RANGE, "+10 pickup range", "Store/LifeEssence.png", pickupRangeRank, pickupRangeMaxRank, pickupRangeMax));
        options.add(new StoreOption(PowerUpType.EXTRA_PROJECTILE, "+1 projectile", "Store/Fireball.png", extraProjectileRank, extraProjectileMaxRank, extraProjectileMax));
        options.add(new StoreOption(PowerUpType.BOUNCE, "+1 bounce", "Store/Fireball.png", bounceRank, bounceMaxRank, bounceRank >= bounceMaxRank));
        options.add(new StoreOption(PowerUpType.WEAPON_DAMAGE, "+15 Damage", "Store/Weapon_Battleaxe_Mithril.png", weaponDamageRank, weaponDamageMaxRank, weaponDamageRank >= weaponDamageMaxRank));
        options.add(new StoreOption(PowerUpType.MAX_HEALTH, "+20 max health", "Store/Potion_Regen_Health.png", healthRank, healthMaxRank, healthRank >= healthMaxRank));
        options.add(new StoreOption(PowerUpType.MOVE_SPEED, "+5% movement speed", "Store/Potion_Stamina.png", speedRank, speedMaxRank, speedRank >= speedMaxRank));
        options.add(new StoreOption(PowerUpType.LUCKY, "+1 life essence gain", "Store/LifeEssence.png", luckyRank, luckyMaxRank, luckyRank >= luckyMaxRank));
        if (projectileRainRank < projectileRainMaxRank) {
            options.add(new StoreOption(PowerUpType.PROJECTILE_RAIN, "One-time sky rain in 25 radius", "Store/Fireball.png", projectileRainRank, projectileRainMaxRank, false));
        }
        Collections.shuffle(options, ThreadLocalRandom.current());
        while (options.size() > 3) {
            options.remove(options.size() - 1);
        }
        PowerUpStorePage.bindOption(builder, events, (StoreOption)options.get(0), "#StoreCardLeft");
        PowerUpStorePage.bindOption(builder, events, (StoreOption)options.get(1), "#StoreCardCenter");
        PowerUpStorePage.bindOption(builder, events, (StoreOption)options.get(2), "#StoreCardRight");
    }

    private static void bindOption(@Nonnull UICommandBuilder builder, @Nonnull UIEventBuilder events, @Nonnull StoreOption option, @Nonnull String prefix) {
        builder.set(prefix + "Title.Text", option.title);
        builder.set(prefix + "Desc.Text", option.description);
        builder.set(prefix + "Rank.Text", "RANK " + option.rank + " OF " + option.maxRank);
        builder.set(prefix + "IconImage.Background", option.icon);
        if (!option.isMaxed) {
            events.addEventBinding(CustomUIEventBindingType.Activating, prefix + "Button", EventData.of("choice", option.choice));
        } else {
            builder.set(prefix + "DisabledOverlay.Visible", true);
            builder.set(prefix + "MaxRankInline.Visible", true);
        }
    }

    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull String data) {
        if (data.isBlank()) {
            return;
        }
        BsonDocument doc;
        try {
            doc = BsonDocument.parse(data);
        }
        catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to parse store selection payload: " + data, exception);
            return;
        }
        if (!doc.containsKey("choice")) {
            return;
        }
        Player playerComponent = (Player)store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRefComponent = (PlayerRef)store.getComponent(ref, PlayerRef.getComponentType());
        if (playerComponent == null || playerRefComponent == null) {
            return;
        }
        UUID playerId = playerRefComponent.getUuid();
        // Same pattern: cache the reference to avoid repeating the chain 3 times.
        PlayerProgressionManager progression = HytaleMod.getInstance().getLifeEssenceLevelSystem();
        if (!progression.canAcceptStoreSelection(playerId)) {
            return;
        }
        String choice = doc.getString("choice").getValue();
        progression.applyPowerUp(ref, store, choice);
        progression.closeStoreForPlayer(ref, store);
        // Fallback: if no StoreSessionManager session exists (e.g., opened via /powerupstore
        // command), closeStoreForPlayer silently no-ops. Close the page directly to avoid a freeze.
        this.requestClose();
    }

    public void requestClose() {
        this.close();
    }

    private static class StoreOption {
        // Now stores a PowerUpType instead of separate choice/title strings.
        // The enum holds both the wire key and the display label, so we
        // eliminated two separate string fields that had to stay in sync.
        private final PowerUpType type;
        private final String choice;
        private final String title;
        private final String description;
        private final String icon;
        private final int rank;
        private final int maxRank;
        private final boolean isMaxed;

        private StoreOption(PowerUpType type, String description, String icon, int rank, int maxRank, boolean isMaxed) {
            this.type = type;
            this.choice = type.key();
            this.title = type.label();
            this.description = description;
            this.icon = icon;
            this.rank = rank;
            this.maxRank = maxRank;
            this.isMaxed = isMaxed;
        }
    }
}
