package com.fadingtime.hytalemod;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.asset.type.gameplay.DeathConfig;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * Drops life essence when a mob spawned by our wave spawner dies.
 */
public class LifeEssenceDropSystem extends DeathSystems.OnDeathSystem {

    private final ComponentType<EntityStore, SpawnedByMobWaveComponent> markerType;
    private final Set<Dependency<EntityStore>> dependencies = Set.of(
        new SystemDependency<>(Order.BEFORE, com.hypixel.hytale.server.npc.systems.NPCDamageSystems.DropDeathItems.class)
    );

    public LifeEssenceDropSystem(
        @Nonnull ComponentType<EntityStore, SpawnedByMobWaveComponent> markerType
    ) {
        this.markerType = markerType;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
            NPCEntity.getComponentType(),
            this.markerType,
            TransformComponent.getComponentType(),
            HeadRotation.getComponentType()
        );
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return this.dependencies;
    }

    @Override
    public void onComponentAdded(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull DeathComponent component,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        NPCEntity npc = commandBuffer.getComponent(ref, NPCEntity.getComponentType());
        if (npc == null || !MobSpawnConfig.HOSTILE_ROLES.contains(npc.getRoleName())) {
            return;
        }

        component.setItemsLossMode(DeathConfig.ItemsLossMode.NONE);

        TransformComponent transform = commandBuffer.getComponent(ref, TransformComponent.getComponentType());
        HeadRotation headRotation = commandBuffer.getComponent(ref, HeadRotation.getComponentType());
        if (transform == null || headRotation == null) {
            return;
        }

        ItemStack lifeEssence = new ItemStack(MobSpawnConfig.LIFE_ESSENCE_ITEM_ID, 1);
        Vector3d dropPosition = transform.getPosition().clone().add(0.0, 1.0, 0.0);
        Vector3f dropRotation = headRotation.getRotation().clone();

        Holder<EntityStore>[] drops = ItemComponent.generateItemDrops(
            store,
            List.of(lifeEssence),
            dropPosition,
            dropRotation
        );
        commandBuffer.addEntities(drops, AddReason.SPAWN);
    }
}
