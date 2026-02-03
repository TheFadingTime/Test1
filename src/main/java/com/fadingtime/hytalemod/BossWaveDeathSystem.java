package com.fadingtime.hytalemod;

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

/**
 * Clears the boss lock when the boss dies.
 */
public class BossWaveDeathSystem extends DeathSystems.OnDeathSystem {
    private final ComponentType<EntityStore, BossWaveComponent> bossComponentType;

    public BossWaveDeathSystem(@Nonnull ComponentType<EntityStore, BossWaveComponent> bossComponentType) {
        this.bossComponentType = bossComponentType;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(this.bossComponentType);
    }

    @Override
    public void onComponentAdded(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull DeathComponent component,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        BossWaveComponent bossComponent = commandBuffer.getComponent(ref, this.bossComponentType);
        if (bossComponent == null || bossComponent.getOwnerId() == null) {
            return;
        }

        UUID ownerId = bossComponent.getOwnerId();
        MobWaveSpawner spawner = HytaleMod.getInstance().getMobWaveSpawner();
        if (spawner != null) {
            spawner.notifyBossDefeated(ownerId);
        }
    }
}
