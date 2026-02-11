package com.fadingtime.hytalemod.ui;

import com.fadingtime.hytalemod.HytaleMod;
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
        int fireRateMaxRank = HytaleMod.getInstance().getLifeEssenceLevelSystem().getMaxFireRateUpgrades();
        int pickupRangeMaxRank = HytaleMod.getInstance().getLifeEssenceLevelSystem().getMaxPickupRangeUpgrades();
        int extraProjectileMaxRank = HytaleMod.getInstance().getLifeEssenceLevelSystem().getMaxExtraProjectileUpgrades();
        int bounceMaxRank = HytaleMod.getInstance().getLifeEssenceLevelSystem().getMaxBounceUpgrades();
        int weaponDamageMaxRank = HytaleMod.getInstance().getLifeEssenceLevelSystem().getMaxWeaponDamageUpgrades();
        int healthMaxRank = HytaleMod.getInstance().getLifeEssenceLevelSystem().getMaxHealthUpgrades();
        int speedMaxRank = HytaleMod.getInstance().getLifeEssenceLevelSystem().getMaxSpeedUpgrades();
        int luckyMaxRank = HytaleMod.getInstance().getLifeEssenceLevelSystem().getMaxLuckyUpgrades();
        int projectileRainMaxRank = HytaleMod.getInstance().getLifeEssenceLevelSystem().getMaxProjectileRainUpgrades();
        if (playerId != null) {
            fireRateMax = HytaleMod.getInstance().getLifeEssenceLevelSystem().hasFireRatePower(playerId, store);
            pickupRangeMax = HytaleMod.getInstance().getLifeEssenceLevelSystem().hasPickupRangePower(playerId, store);
            extraProjectileMax = HytaleMod.getInstance().getLifeEssenceLevelSystem().hasExtraProjectilePower(playerId, store);
            fireRateRank = HytaleMod.getInstance().getLifeEssenceLevelSystem().getFireRateRank(playerId, store);
            pickupRangeRank = HytaleMod.getInstance().getLifeEssenceLevelSystem().getPickupRangeRank(playerId, store);
            extraProjectileRank = HytaleMod.getInstance().getLifeEssenceLevelSystem().getExtraProjectileRank(playerId, store);
            bounceRank = HytaleMod.getInstance().getLifeEssenceLevelSystem().getBounceRank(playerId, store);
            weaponDamageRank = HytaleMod.getInstance().getLifeEssenceLevelSystem().getWeaponDamageRank(playerId, store);
            healthRank = HytaleMod.getInstance().getLifeEssenceLevelSystem().getHealthRank(playerId, store);
            speedRank = HytaleMod.getInstance().getLifeEssenceLevelSystem().getSpeedRank(playerId, store);
            luckyRank = HytaleMod.getInstance().getLifeEssenceLevelSystem().getLuckyRank(playerId, store);
            projectileRainRank = HytaleMod.getInstance().getLifeEssenceLevelSystem().getProjectileRainRank(playerId, store);
        }
        ArrayList<StoreOption> options = new ArrayList<StoreOption>();
        options.add(new StoreOption("fire_rate", "FIRE RATE", "+15% attack speed", "Store/Potion.png", fireRateRank, fireRateMaxRank, fireRateMax));
        options.add(new StoreOption("pickup_range", "PICKUP RANGE", "+10 pickup range", "Store/LifeEssence.png", pickupRangeRank, pickupRangeMaxRank, pickupRangeMax));
        options.add(new StoreOption("extra_projectile", "EXTRA PROJECTILE", "+1 projectile", "Store/Fireball.png", extraProjectileRank, extraProjectileMaxRank, extraProjectileMax));
        options.add(new StoreOption("bounce", "BOUNCE", "+1 bounce", "Store/Fireball.png", bounceRank, bounceMaxRank, bounceRank >= bounceMaxRank));
        options.add(new StoreOption("weapon_damage", "WEAPON DAMAGE", "+15 Damage", "Store/Weapon_Battleaxe_Mithril.png", weaponDamageRank, weaponDamageMaxRank, weaponDamageRank >= weaponDamageMaxRank));
        options.add(new StoreOption("max_health", "MORE HEALTH", "+20 max health", "Store/Potion_Regen_Health.png", healthRank, healthMaxRank, healthRank >= healthMaxRank));
        options.add(new StoreOption("move_speed", "MOVE SPEED", "+5% movement speed", "Store/Potion_Stamina.png", speedRank, speedMaxRank, speedRank >= speedMaxRank));
        options.add(new StoreOption("lucky", "LUCKY", "+1 life essence gain", "Store/LifeEssence.png", luckyRank, luckyMaxRank, luckyRank >= luckyMaxRank));
        if (projectileRainRank < projectileRainMaxRank) {
            options.add(new StoreOption("projectile_rain", "LAST RESORT...", "One-time sky rain in 25 radius", "Store/Fireball.png", projectileRainRank, projectileRainMaxRank, false));
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
        if (!HytaleMod.getInstance().getLifeEssenceLevelSystem().canAcceptStoreSelection(playerId)) {
            return;
        }
        String choice = doc.getString("choice").getValue();
        HytaleMod.getInstance().getLifeEssenceLevelSystem().applyPowerUp(ref, store, choice);
        HytaleMod.getInstance().getLifeEssenceLevelSystem().closeStoreForPlayer(ref, store);
    }

    public void requestClose() {
        this.close();
    }

    private static class StoreOption {
        private final String choice;
        private final String title;
        private final String description;
        private final String icon;
        private final int rank;
        private final int maxRank;
        private final boolean isMaxed;

        private StoreOption(String choice, String title, String description, String icon, int rank, int maxRank, boolean isMaxed) {
            this.choice = choice;
            this.title = title;
            this.description = description;
            this.icon = icon;
            this.rank = rank;
            this.maxRank = maxRank;
            this.isMaxed = isMaxed;
        }
    }
}
