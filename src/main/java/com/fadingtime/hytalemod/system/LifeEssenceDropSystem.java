/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.AddReason
 *  com.hypixel.hytale.component.CommandBuffer
 *  com.hypixel.hytale.component.Component
 *  com.hypixel.hytale.component.ComponentType
 *  com.hypixel.hytale.component.Holder
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.component.dependency.Dependency
 *  com.hypixel.hytale.component.dependency.Order
 *  com.hypixel.hytale.component.dependency.SystemDependency
 *  com.hypixel.hytale.component.query.Query
 *  com.hypixel.hytale.math.vector.Vector3d
 *  com.hypixel.hytale.math.vector.Vector3f
 *  com.hypixel.hytale.server.core.asset.type.gameplay.DeathConfig$ItemsLossMode
 *  com.hypixel.hytale.server.core.inventory.ItemStack
 *  com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent
 *  com.hypixel.hytale.server.core.modules.entity.component.HeadRotation
 *  com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
 *  com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent
 *  com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems$OnDeathSystem
 *  com.hypixel.hytale.server.core.modules.entity.item.ItemComponent
 *  com.hypixel.hytale.server.core.modules.entity.item.PreventPickup
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 *  com.hypixel.hytale.server.npc.entities.NPCEntity
 *  com.hypixel.hytale.server.npc.systems.NPCDamageSystems$DropDeathItems
 *  javax.annotation.Nonnull
 */
package com.fadingtime.hytalemod.system;

import com.fadingtime.hytalemod.HytaleMod;
import com.fadingtime.hytalemod.component.BossWaveComponent;
import com.fadingtime.hytalemod.component.LifeEssenceDropComponent;
import com.fadingtime.hytalemod.component.SpawnedByMobWaveComponent;
import com.fadingtime.hytalemod.config.ConfigManager;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Component;
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
import javax.annotation.Nonnull;

public class LifeEssenceDropSystem
extends DeathSystems.OnDeathSystem {
    private static final int NORMAL_ESSENCE_DROP_COUNT = 1;
    private static final int BOSS_ESSENCE_DROP_COUNT = 30;
    private final ComponentType<EntityStore, SpawnedByMobWaveComponent> markerType;
    private final ComponentType<EntityStore, BossWaveComponent> bossMarkerType;
    private final ComponentType<EntityStore, LifeEssenceDropComponent> lifeEssenceDropType;
    private final Set<Dependency<EntityStore>> dependencies = Set.of(new SystemDependency(Order.BEFORE, NPCDamageSystems.DropDeathItems.class));

    public LifeEssenceDropSystem(@Nonnull ComponentType<EntityStore, SpawnedByMobWaveComponent> componentType, @Nonnull ComponentType<EntityStore, BossWaveComponent> componentType2, @Nonnull ComponentType<EntityStore, LifeEssenceDropComponent> componentType3) {
        this.markerType = componentType;
        this.bossMarkerType = componentType2;
        this.lifeEssenceDropType = componentType3;
    }

    @Nonnull
    public Query<EntityStore> getQuery() {
        return Query.and((Query[])new Query[]{NPCEntity.getComponentType(), this.markerType, TransformComponent.getComponentType()});
    }

    @Nonnull
    public Set<Dependency<EntityStore>> getDependencies() {
        return this.dependencies;
    }

    public void onComponentAdded(@Nonnull Ref<EntityStore> ref, @Nonnull DeathComponent deathComponent, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        NPCEntity nPCEntity = (NPCEntity)commandBuffer.getComponent(ref, NPCEntity.getComponentType());
        if (nPCEntity == null) {
            return;
        }
        String string = nPCEntity.getRole() != null ? nPCEntity.getRole().getRoleName() : nPCEntity.getRoleName();
        deathComponent.setItemsLossMode(DeathConfig.ItemsLossMode.NONE);
        TransformComponent transformComponent = (TransformComponent)commandBuffer.getComponent(ref, TransformComponent.getComponentType());
        HeadRotation headRotation = (HeadRotation)commandBuffer.getComponent(ref, HeadRotation.getComponentType());
        if (transformComponent == null) {
            return;
        }
        boolean bl = commandBuffer.getComponent(ref, this.bossMarkerType) != null || store.getComponent(ref, this.bossMarkerType) != null;
        int n = bl ? 30 : 1;
        ItemStack itemStack = new ItemStack(ConfigManager.get().lifeEssenceItemId, n);
        Vector3d vector3d = transformComponent.getPosition().clone().add(0.0, 1.0, 0.0);
        Vector3f vector3f = headRotation != null ? headRotation.getRotation().clone() : new Vector3f(0.0f, 0.0f, 0.0f);
        Holder[] holderArray = ItemComponent.generateItemDrops(store, List.of(itemStack), (Vector3d)vector3d, (Vector3f)vector3f);
        if (holderArray == null || holderArray.length == 0) {
            HytaleMod.getInstance().getLogger().at(Level.WARNING).log("No life essence drop for " + ref + " (" + string + ")");
            return;
        }
        long l = System.currentTimeMillis();
        for (Holder holder : holderArray) {
            holder.addComponent(this.lifeEssenceDropType, (Component)new LifeEssenceDropComponent(l));
            holder.addComponent(PreventPickup.getComponentType(), (Component)PreventPickup.INSTANCE);
            holder.addComponent(EntityScaleComponent.getComponentType(), (Component)new EntityScaleComponent(3.0f));
        }
        commandBuffer.addEntities(holderArray, AddReason.SPAWN);
    }
}
