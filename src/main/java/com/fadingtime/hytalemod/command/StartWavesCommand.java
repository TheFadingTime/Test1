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
import com.fadingtime.hytalemod.spawner.MobWaveSpawner;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class StartWavesCommand
extends AbstractPlayerCommand {
    public StartWavesCommand() {
        super("startwaves", "Start mob waves immediately for your current gameplay session");
        this.addAliases(new String[]{"startwave", "wavesstart", "wavestart"});
        this.setAllowsExtraArguments(false);
        this.setPermissionGroup(GameMode.Creative);
    }

    protected void execute(CommandContext commandContext, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, World world) {
        MobWaveSpawner mobWaveSpawner = HytaleMod.getInstance().getMobWaveSpawner();
        if (mobWaveSpawner == null || ref == null || !ref.isValid()) {
            commandContext.sendMessage(Message.raw((String)"Wave start failed."));
            return;
        }
        mobWaveSpawner.startWavesForPlayer(ref);
        commandContext.sendMessage(Message.raw((String)"Waves started."));
    }
}

