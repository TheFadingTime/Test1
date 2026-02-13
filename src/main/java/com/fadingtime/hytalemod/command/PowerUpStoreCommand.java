/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.protocol.GameMode
 *  com.hypixel.hytale.server.core.command.system.CommandContext
 *  com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.world.World
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 */
package com.fadingtime.hytalemod.command;

import com.fadingtime.hytalemod.HytaleMod;
import com.fadingtime.hytalemod.ui.PowerUpStorePage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class PowerUpStoreCommand
extends AbstractPlayerCommand {
    public PowerUpStoreCommand() {
        super("powerupstore", "Toggle the power-up store preview UI");
        this.addAliases(new String[]{"store", "powerup", "powerups"});
        this.setAllowsExtraArguments(false);
        this.setPermissionGroup(GameMode.Creative);
    }

    protected void execute(CommandContext commandContext, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, World world) {
        Player player = (Player)store.getComponent(ref, Player.getComponentType());
        if (player == null || playerRef == null) {
            return;
        }
        if (HytaleMod.getInstance() == null || HytaleMod.getInstance().getLifeEssenceLevelSystem() == null) {
            return;
        }
        CustomUIPage customUIPage = player.getPageManager().getCustomPage();
        if (customUIPage instanceof PowerUpStorePage) {
            HytaleMod.getInstance().getLifeEssenceLevelSystem().closeStoreForPlayer(ref, store);
            CustomUIPage customUIPage2 = player.getPageManager().getCustomPage();
            if (customUIPage2 instanceof PowerUpStorePage) {
                ((PowerUpStorePage)customUIPage2).requestClose();
            }
            return;
        }
        HytaleMod.getInstance().getLifeEssenceLevelSystem().openStoreForPlayer(ref, store, player, playerRef, 5);
    }
}

