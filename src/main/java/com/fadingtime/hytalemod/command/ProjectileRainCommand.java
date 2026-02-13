/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.protocol.GameMode
 *  com.hypixel.hytale.server.core.Message
 *  com.hypixel.hytale.server.core.command.system.CommandContext
 *  com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.world.World
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 */
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

    protected void execute(CommandContext commandContext, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, World world) {
        boolean bl = HytaleMod.getInstance().getLifeEssenceLevelSystem().triggerProjectileRainNow(ref, store);
        if (bl) {
            commandContext.sendMessage(Message.raw((String)"Projectile rain fired."));
        } else {
            commandContext.sendMessage(Message.raw((String)"Projectile rain failed."));
        }
    }
}

