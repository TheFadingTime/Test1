/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime
 *  com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage
 *  com.hypixel.hytale.server.core.ui.builder.EventData
 *  com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
 *  com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 *  javax.annotation.Nonnull
 *  org.bson.BsonDocument
 *  org.bson.BsonInvalidOperationException
 *  org.bson.BsonSerializationException
 *  org.bson.json.JsonParseException
 */
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
import org.bson.BsonInvalidOperationException;
import org.bson.BsonSerializationException;
import org.bson.json.JsonParseException;

public class PowerUpStorePage
extends CustomUIPage {
    private static final String UI_PATH = "Pages/PowerUpStore.ui";
    private static final String SKIP_CHOICE = "skip_store";
    private static final Logger LOGGER = Logger.getLogger(PowerUpStorePage.class.getName());
    private final int level;

    public PowerUpStorePage(@Nonnull PlayerRef playerRef, int n) {
        super(playerRef, CustomPageLifetime.CantClose);
        this.level = n;
    }

    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uICommandBuilder, @Nonnull UIEventBuilder uIEventBuilder, @Nonnull Store<EntityStore> store) {
        uICommandBuilder.append(UI_PATH);
        uICommandBuilder.set("#StoreTitle.Text", "LEVEL " + this.level + " - CHOOSE A POWER-UP!");
        PlayerRef playerRef = (PlayerRef)store.getComponent(ref, PlayerRef.getComponentType());
        UUID uUID = playerRef != null ? playerRef.getUuid() : null;
        boolean bl = false;
        boolean bl2 = false;
        boolean bl3 = false;
        int n = 0;
        int n2 = 0;
        int n3 = 0;
        int n4 = 0;
        int n5 = 0;
        int n6 = 0;
        int n7 = 0;
        int n8 = 0;
        int n9 = 0;
        int n11 = HytaleMod.getInstance().getLifeEssenceLevelSystem().getMaxFireRateUpgrades();
        int n12 = HytaleMod.getInstance().getLifeEssenceLevelSystem().getMaxPickupRangeUpgrades();
        int n13 = HytaleMod.getInstance().getLifeEssenceLevelSystem().getMaxExtraProjectileUpgrades();
        int n14 = HytaleMod.getInstance().getLifeEssenceLevelSystem().getMaxBounceUpgrades();
        int n15 = HytaleMod.getInstance().getLifeEssenceLevelSystem().getMaxWeaponDamageUpgrades();
        int n16 = HytaleMod.getInstance().getLifeEssenceLevelSystem().getMaxHealthUpgrades();
        int n17 = HytaleMod.getInstance().getLifeEssenceLevelSystem().getMaxSpeedUpgrades();
        int n18 = HytaleMod.getInstance().getLifeEssenceLevelSystem().getMaxLuckyUpgrades();
        int n19 = HytaleMod.getInstance().getLifeEssenceLevelSystem().getMaxProjectileRainUpgrades();
        if (uUID != null) {
            bl = HytaleMod.getInstance().getLifeEssenceLevelSystem().hasFireRatePower(uUID, store);
            bl2 = HytaleMod.getInstance().getLifeEssenceLevelSystem().hasPickupRangePower(uUID, store);
            bl3 = HytaleMod.getInstance().getLifeEssenceLevelSystem().hasExtraProjectilePower(uUID, store);
            n = HytaleMod.getInstance().getLifeEssenceLevelSystem().getFireRateRank(uUID, store);
            n2 = HytaleMod.getInstance().getLifeEssenceLevelSystem().getPickupRangeRank(uUID, store);
            n3 = HytaleMod.getInstance().getLifeEssenceLevelSystem().getExtraProjectileRank(uUID, store);
            n4 = HytaleMod.getInstance().getLifeEssenceLevelSystem().getBounceRank(uUID, store);
            n5 = HytaleMod.getInstance().getLifeEssenceLevelSystem().getWeaponDamageRank(uUID, store);
            n6 = HytaleMod.getInstance().getLifeEssenceLevelSystem().getHealthRank(uUID, store);
            n7 = HytaleMod.getInstance().getLifeEssenceLevelSystem().getSpeedRank(uUID, store);
            n8 = HytaleMod.getInstance().getLifeEssenceLevelSystem().getLuckyRank(uUID, store);
            n9 = HytaleMod.getInstance().getLifeEssenceLevelSystem().getProjectileRainRank(uUID, store);
        }
        ArrayList<StoreOption> arrayList = new ArrayList<StoreOption>();
        arrayList.add(new StoreOption("fire_rate", "FIRE RATE", "+15% attack speed", "Store/Potion.png", n, n11, bl));
        arrayList.add(new StoreOption("pickup_range", "PICKUP RANGE", "+10 pickup range", "Store/LifeEssence.png", n2, n12, bl2));
        arrayList.add(new StoreOption("extra_projectile", "EXTRA PROJECTILE", "+1 projectile", "Store/Fireball.png", n3, n13, bl3));
        arrayList.add(new StoreOption("bounce", "BOUNCE", "+1 bounce", "Store/Weapon_Staff_Crystal_Flame.png", n4, n14, n4 >= n14));
        arrayList.add(new StoreOption("weapon_damage", "WEAPON DAMAGE", "+15 Damage", "Store/Weapon_Battleaxe_Mithril.png", n5, n15, n5 >= n15));
        arrayList.add(new StoreOption("max_health", "MORE HEALTH", "+20 max health", "Store/Potion_Regen_Health.png", n6, n16, n6 >= n16));
        arrayList.add(new StoreOption("move_speed", "MOVE SPEED", "+5% movement speed", "Store/Potion_Stamina.png", n7, n17, n7 >= n17));
        arrayList.add(new StoreOption("lucky", "LUCKY", "+1 life essence gain", "Store/Ingredient_Lightning_Essence.png", n8, n18, n8 >= n18));
        if (n9 < n19) {
            arrayList.add(new StoreOption("projectile_rain", "LAST RESORT...", "One-time sky rain in 25 radius", "Store/Ingredient_Crystal_Fragments_Blue.png", n9, n19, false));
        }
        Collections.shuffle(arrayList, ThreadLocalRandom.current());
        while (arrayList.size() > 3) {
            arrayList.remove(arrayList.size() - 1);
        }
        PowerUpStorePage.bindOption(uICommandBuilder, uIEventBuilder, (StoreOption)arrayList.get(0), "#StoreCardLeft");
        PowerUpStorePage.bindOption(uICommandBuilder, uIEventBuilder, (StoreOption)arrayList.get(1), "#StoreCardCenter");
        PowerUpStorePage.bindOption(uICommandBuilder, uIEventBuilder, (StoreOption)arrayList.get(2), "#StoreCardRight");
        PowerUpStorePage.bindOption(uICommandBuilder, uIEventBuilder, new StoreOption(SKIP_CHOICE, "SKIP", "Skip this shop", "Store/Potion_Purify.png", 0, 0, false, false, null), "#StoreCardSkip");
    }

    private static void bindOption(@Nonnull UICommandBuilder uICommandBuilder, @Nonnull UIEventBuilder uIEventBuilder, @Nonnull StoreOption storeOption, @Nonnull String string) {
        uICommandBuilder.set(string + "Title.Text", storeOption.title);
        uICommandBuilder.set(string + "Desc.Text", storeOption.description);
        if (storeOption.showRank) {
            if (storeOption.rankLabel != null && !storeOption.rankLabel.isBlank()) {
                uICommandBuilder.set(string + "Rank.Text", storeOption.rankLabel);
            } else {
                uICommandBuilder.set(string + "Rank.Text", "RANK " + storeOption.rank + " OF " + storeOption.maxRank);
            }
            uICommandBuilder.set(string + "Rank.Visible", true);
        } else {
            uICommandBuilder.set(string + "Rank.Visible", false);
        }
        uICommandBuilder.set(string + "IconImage.Background", storeOption.icon);
        uICommandBuilder.set(string + "DisabledOverlay.Visible", false);
        uICommandBuilder.set(string + "MaxRankInline.Visible", false);
        if (!storeOption.isMaxed) {
            uIEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, string + "Button", EventData.of((String)"choice", (String)storeOption.choice));
        } else {
            uICommandBuilder.set(string + "DisabledOverlay.Visible", true);
            uICommandBuilder.set(string + "MaxRankInline.Visible", true);
        }
    }

    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull String string) {
        BsonDocument bsonDocument;
        if (string.isBlank()) {
            return;
        }
        try {
            bsonDocument = BsonDocument.parse((String)string);
        }
        catch (BsonInvalidOperationException | BsonSerializationException | JsonParseException throwable) {
            LOGGER.log(Level.WARNING, "Failed to parse store selection payload: " + string, throwable);
            return;
        }
        if (!bsonDocument.containsKey((Object)"choice")) {
            return;
        }
        Player player = (Player)store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = (PlayerRef)store.getComponent(ref, PlayerRef.getComponentType());
        if (player == null || playerRef == null) {
            return;
        }
        UUID uUID = playerRef.getUuid();
        if (!HytaleMod.getInstance().getLifeEssenceLevelSystem().canAcceptStoreSelection(uUID)) {
            return;
        }
        String string2 = bsonDocument.getString((Object)"choice").getValue();
        HytaleMod.getInstance().getLifeEssenceLevelSystem().applyPowerUp(ref, store, string2);
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
        private final boolean showRank;
        private final String rankLabel;

        private StoreOption(String string, String string2, String string3, String string4, int n, int n2, boolean bl) {
            this(string, string2, string3, string4, n, n2, bl, true, null);
        }

        private StoreOption(String string, String string2, String string3, String string4, int n, int n2, boolean bl, boolean bl2) {
            this(string, string2, string3, string4, n, n2, bl, bl2, null);
        }

        private StoreOption(String string, String string2, String string3, String string4, int n, int n2, boolean bl, boolean bl2, String string5) {
            this.choice = string;
            this.title = string2;
            this.description = string3;
            this.icon = string4;
            this.rank = n;
            this.maxRank = n2;
            this.isMaxed = bl;
            this.showRank = bl2;
            this.rankLabel = string5;
        }
    }
}
