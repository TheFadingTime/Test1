/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.CommandBuffer
 *  com.hypixel.hytale.component.ComponentType
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.component.query.Query
 *  com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent
 *  com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems$OnDeathSystem
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 *  javax.annotation.Nonnull
 */
package com.fadingtime.hytalemod.system;

import com.fadingtime.hytalemod.HytaleMod;
import com.fadingtime.hytalemod.component.BossWaveComponent;
import com.fadingtime.hytalemod.spawner.MobWaveSpawner;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import javax.annotation.Nonnull;

public class BossWaveDeathSystem
extends DeathSystems.OnDeathSystem {
    private final ComponentType<EntityStore, BossWaveComponent> bossComponentType;

    public BossWaveDeathSystem(@Nonnull ComponentType<EntityStore, BossWaveComponent> componentType) {
        this.bossComponentType = componentType;
    }

    @Nonnull
    public Query<EntityStore> getQuery() {
        return Query.and((Query[])new Query[]{this.bossComponentType});
    }

    public void onComponentAdded(@Nonnull Ref<EntityStore> ref, @Nonnull DeathComponent deathComponent, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        BossWaveComponent bossWaveComponent = (BossWaveComponent)commandBuffer.getComponent(ref, this.bossComponentType);
        if (bossWaveComponent == null || bossWaveComponent.getOwnerId() == null) {
            return;
        }
        UUID uUID = bossWaveComponent.getOwnerId();
        MobWaveSpawner mobWaveSpawner = HytaleMod.getInstance().getMobWaveSpawner();
        if (mobWaveSpawner != null) {
            mobWaveSpawner.notifyBossDefeated(uUID);
        }
        if (HytaleMod.getInstance().getBossHudSystem() != null) {
            HytaleMod.getInstance().getBossHudSystem().clearBossHud(uUID);
        }
        commandBuffer.removeComponent(ref, this.bossComponentType);
    }
}

