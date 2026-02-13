package com.fadingtime.hytalemod.command;

import com.fadingtime.hytalemod.HytaleMod;
import com.fadingtime.hytalemod.system.PlayerProgressionManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;

public class AddProjectileCommand
extends AbstractPlayerCommand {
    public AddProjectileCommand() {
        super("addprojectile", "Add +1 extra projectile to your player");
        this.addAliases(new String[]{"plusprojectile", "projplus", "addproj"});
        this.setAllowsExtraArguments(false);
        this.setPermissionGroup(GameMode.Creative);
    }

    protected void execute(CommandContext commandContext, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, World world) {
        PlayerProgressionManager playerProgressionManager = HytaleMod.getInstance().getLifeEssenceLevelSystem();
        if (playerProgressionManager == null || ref == null || !ref.isValid() || playerRef == null) {
            commandContext.sendMessage(Message.raw((String)"Failed to add projectile."));
            return;
        }
        UUID uUID = playerRef.getUuid();
        if (playerProgressionManager.hasExtraProjectilePower(uUID, store)) {
            int n = playerProgressionManager.getExtraProjectileRank(uUID, store);
            commandContext.sendMessage(Message.raw((String)("Projectile already maxed (rank " + n + ").")));
            return;
        }
        int n = playerProgressionManager.getExtraProjectileRank(uUID, store);
        playerProgressionManager.applyPowerUp(ref, store, "extra_projectile");
        int n2 = playerProgressionManager.getExtraProjectileRank(uUID, store);
        if (n2 > n) {
            commandContext.sendMessage(Message.raw((String)("Added +1 projectile. Rank: " + n2)));
        } else {
            commandContext.sendMessage(Message.raw((String)"Projectile was not changed."));
        }
    }
}
