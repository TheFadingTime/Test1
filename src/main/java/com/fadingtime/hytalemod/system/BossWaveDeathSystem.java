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

    public BossWaveDeathSystem(@Nonnull ComponentType<EntityStore, BossWaveComponent> bossComponentType) {
        this.bossComponentType = bossComponentType;
    }

    @Nonnull
    public Query<EntityStore> getQuery() {
        return Query.and((Query[])new Query[]{this.bossComponentType});
    }

    public void onComponentAdded(@Nonnull Ref<EntityStore> ref, @Nonnull DeathComponent component, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        BossWaveComponent bossComponent = (BossWaveComponent)commandBuffer.getComponent(ref, this.bossComponentType);
        if (bossComponent == null || bossComponent.getOwnerId() == null) {
            return;
        }
        UUID ownerId = bossComponent.getOwnerId();
        MobWaveSpawner spawner = HytaleMod.getInstance().getMobWaveSpawner();
        if (spawner != null) {
            spawner.notifyBossDefeated(ownerId);
        }
        if (HytaleMod.getInstance().getBossHudSystem() != null) {
            HytaleMod.getInstance().getBossHudSystem().clearBossHud(ownerId);
        }
        commandBuffer.removeComponent(ref, this.bossComponentType);
    }
}

