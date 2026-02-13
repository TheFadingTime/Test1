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
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
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

    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull Damage damage) {
        Ref<EntityStore> attackerRef = this.resolveSourceRef(damage);
        if (attackerRef == null) {
            return;
        }
        PlayerRef playerRefComponent = (PlayerRef)store.getComponent(attackerRef, PlayerRef.getComponentType());
        if (playerRefComponent == null) {
            return;
        }
        HytaleMod mod = HytaleMod.getInstance();
        if (mod == null || mod.getLifeEssenceLevelSystem() == null) {
            return;
        }
        UUID playerId = playerRefComponent.getUuid();
        float bonus = mod.getLifeEssenceLevelSystem().getWeaponDamageBonus(playerId, store);
        if (bonus <= 0.0f) {
            return;
        }
        damage.setAmount(Math.max(0.0f, damage.getAmount() + bonus));
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
