package com.fadingtime.hytalemod.command;

import com.fadingtime.hytalemod.HytaleMod;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class ProjectileRainCommand
extends AbstractPlayerCommand {
    public ProjectileRainCommand() {
        super("projectilerain", "Trigger a projectile rain burst on your player");
        this.addAliases(new String[]{"rainburst", "testrain", "rain"});
        this.setAllowsExtraArguments(false);
        this.setPermissionGroup(GameMode.Creative);
    }

    protected void execute(CommandContext context, Store<EntityStore> store, Ref<EntityStore> playerRef, PlayerRef playerRefComponent, World world) {
        boolean queued = HytaleMod.getInstance().getLifeEssenceLevelSystem().triggerProjectileRainNow(playerRef, store);
        if (queued) {
            context.sendMessage(Message.raw("Projectile rain fired."));
        } else {
            context.sendMessage(Message.raw("Projectile rain failed."));
        }
    }
}

