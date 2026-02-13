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

public class BossWaveDeathSystem
extends DeathSystems.OnDeathSystem {
    private final ComponentType<EntityStore, BossWaveComponent> bossComponentType;

    public BossWaveDeathSystem(ComponentType<EntityStore, BossWaveComponent> bossComponentType) {
        this.bossComponentType = bossComponentType;
    }

    public Query<EntityStore> getQuery() {
        return Query.and(this.bossComponentType);
    }

    public void onComponentAdded(Ref<EntityStore> ref, DeathComponent component, Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {
        var bossComponent = (BossWaveComponent)commandBuffer.getComponent(ref, this.bossComponentType);
        if (bossComponent == null || bossComponent.getOwnerId() == null) {
            return;
        }

        UUID ownerId = bossComponent.getOwnerId();
        HytaleMod mod = HytaleMod.getInstance();
        MobWaveSpawner spawner = mod.getMobWaveSpawner();
        if (spawner != null) {
            spawner.notifyBossDefeated(ownerId);
        }

        var bossHudSystem = mod.getBossHudSystem();
        if (bossHudSystem != null) {
            bossHudSystem.clearBossHud(ownerId);
        }
        commandBuffer.removeComponent(ref, this.bossComponentType);
    }
}

