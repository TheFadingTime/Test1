package com.fadingtime.hytalemod.command;

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

    protected void execute(CommandContext context, Store<EntityStore> store, Ref<EntityStore> playerRef, PlayerRef playerRefComponent, World world) {
        Player playerComponent = (Player)store.getComponent(playerRef, Player.getComponentType());
        if (playerComponent == null || playerRefComponent == null) {
            return;
        }
        CustomUIPage currentPage = playerComponent.getPageManager().getCustomPage();
        if (currentPage instanceof PowerUpStorePage) {
            ((PowerUpStorePage)currentPage).requestClose();
            return;
        }
        playerComponent.getPageManager().openCustomPage(playerRef, store, (CustomUIPage)new PowerUpStorePage(playerRefComponent, 5));
    }
}

