/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.ArchetypeChunk
 *  com.hypixel.hytale.component.CommandBuffer
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.component.dependency.Dependency
 *  com.hypixel.hytale.component.dependency.Order
 *  com.hypixel.hytale.component.dependency.SystemDependency
 *  com.hypixel.hytale.component.dependency.SystemGroupDependency
 *  com.hypixel.hytale.component.query.Query
 *  com.hypixel.hytale.server.core.modules.entity.damage.Damage
 *  com.hypixel.hytale.server.core.modules.entity.damage.Damage$EntitySource
 *  com.hypixel.hytale.server.core.modules.entity.damage.Damage$ProjectileSource
 *  com.hypixel.hytale.server.core.modules.entity.damage.Damage$Source
 *  com.hypixel.hytale.server.core.modules.entity.damage.DamageCalculatorSystems$SequenceModifier
 *  com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem
 *  com.hypixel.hytale.server.core.modules.entity.damage.DamageModule
 *  com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems$ApplyDamage
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 *  javax.annotation.Nonnull
 */
package com.fadingtime.hytalemod.system;

import com.fadingtime.hytalemod.HytaleMod;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.dependency.SystemGroupDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCalculatorSystems;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;

public class PlayerDamageBonusSystem
extends DamageEventSystem {
    @Nonnull
    private static final Query<EntityStore> QUERY = Query.any();
    @Nonnull
    private final Set<Dependency<EntityStore>> dependencies = Set.of(new SystemGroupDependency(Order.AFTER, DamageModule.get().getGatherDamageGroup()), new SystemGroupDependency(Order.AFTER, DamageModule.get().getFilterDamageGroup()), new SystemDependency(Order.AFTER, DamageCalculatorSystems.SequenceModifier.class), new SystemDependency(Order.BEFORE, DamageSystems.ApplyDamage.class));

    public Query<EntityStore> getQuery() {
        return QUERY;
    }

    public Set<Dependency<EntityStore>> getDependencies() {
        return this.dependencies;
    }

    public void handle(int n, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull Damage damage) {
        Ref<EntityStore> ref = this.resolveSourceRef(damage);
        if (ref == null) {
            return;
        }
        PlayerRef playerRef = (PlayerRef)store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }
        HytaleMod hytaleMod = HytaleMod.getInstance();
        if (hytaleMod == null || hytaleMod.getLifeEssenceLevelSystem() == null) {
            return;
        }
        UUID uUID = playerRef.getUuid();
        float f = hytaleMod.getLifeEssenceLevelSystem().getWeaponDamageBonus(uUID, store);
        if (f <= 0.0f) {
            return;
        }
        damage.setAmount(Math.max(0.0f, damage.getAmount() + f));
    }

    private Ref<EntityStore> resolveSourceRef(@Nonnull Damage damage) {
        Damage.Source source = damage.getSource();
        if (source instanceof Damage.ProjectileSource) {
            return ((Damage.ProjectileSource)source).getRef();
        }
        if (source instanceof Damage.EntitySource) {
            return ((Damage.EntitySource)source).getRef();
        }
        return null;
    }
}

