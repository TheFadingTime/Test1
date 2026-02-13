package com.fadingtime.hytalemod.system;

import com.fadingtime.hytalemod.HytaleMod;
import com.fadingtime.hytalemod.component.BossWaveComponent;
import com.fadingtime.hytalemod.component.LifeEssenceDropComponent;
import com.fadingtime.hytalemod.component.SpawnedByMobWaveComponent;
import com.fadingtime.hytalemod.config.ConfigManager;
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
import com.hypixel.hytale.server.core.asset.type.gameplay.DeathConfig;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.entity.item.PreventPickup;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.systems.NPCDamageSystems;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public class LifeEssenceDropSystem
extends DeathSystems.OnDeathSystem {
    private static final int NORMAL_ESSENCE_DROP_COUNT = 1;
    private static final int BOSS_ESSENCE_DROP_COUNT = 30;
    private final ComponentType<EntityStore, SpawnedByMobWaveComponent> markerType;
    private final ComponentType<EntityStore, BossWaveComponent> bossMarkerType;
    private final ComponentType<EntityStore, LifeEssenceDropComponent> lifeEssenceDropType;
    private final Set<Dependency<EntityStore>> dependencies = Set.of(new SystemDependency(Order.BEFORE, NPCDamageSystems.DropDeathItems.class));

    public LifeEssenceDropSystem(ComponentType<EntityStore, SpawnedByMobWaveComponent> markerType, ComponentType<EntityStore, BossWaveComponent> bossMarkerType, ComponentType<EntityStore, LifeEssenceDropComponent> lifeEssenceDropType) {
        this.markerType = markerType;
        this.bossMarkerType = bossMarkerType;
        this.lifeEssenceDropType = lifeEssenceDropType;
    }

    public Query<EntityStore> getQuery() {
        return Query.and(NPCEntity.getComponentType(), this.markerType, TransformComponent.getComponentType());
    }

    public Set<Dependency<EntityStore>> getDependencies() {
        return this.dependencies;
    }

    public void onComponentAdded(Ref<EntityStore> ref, DeathComponent component, Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {
        if (commandBuffer.getComponent(ref, NPCEntity.getComponentType()) == null) {
            return;
        }

        component.setItemsLossMode(DeathConfig.ItemsLossMode.NONE);
        var transform = (TransformComponent)commandBuffer.getComponent(ref, TransformComponent.getComponentType());
        var headRotation = (HeadRotation)commandBuffer.getComponent(ref, HeadRotation.getComponentType());
        if (transform == null) {
            return;
        }

        boolean isBoss = store.getComponent(ref, this.bossMarkerType) != null || commandBuffer.getComponent(ref, this.bossMarkerType) != null;
        int amount = isBoss ? BOSS_ESSENCE_DROP_COUNT : NORMAL_ESSENCE_DROP_COUNT;
        ItemStack lifeEssence = new ItemStack(ConfigManager.get().lifeEssenceItemId, amount);
        Vector3d dropPosition = transform.getPosition().clone().add(0.0, 1.0, 0.0);
        Vector3f dropRotation = headRotation == null ? new Vector3f(0.0f, 0.0f, 0.0f) : headRotation.getRotation().clone();
        Holder[] drops = ItemComponent.generateItemDrops(store, List.of(lifeEssence), dropPosition, dropRotation);
        if (drops == null || drops.length == 0) {
            HytaleMod.LOGGER.log(Level.WARNING, "No life essence drops generated for npc " + ref);
            return;
        }

        long nowMs = System.currentTimeMillis();
        for (Holder drop : drops) {
            drop.addComponent(this.lifeEssenceDropType, new LifeEssenceDropComponent(nowMs));
            drop.addComponent(PreventPickup.getComponentType(), PreventPickup.INSTANCE);
            drop.addComponent(EntityScaleComponent.getComponentType(), new EntityScaleComponent(3.0f));
        }
        commandBuffer.addEntities(drops, AddReason.SPAWN);
    }
}
