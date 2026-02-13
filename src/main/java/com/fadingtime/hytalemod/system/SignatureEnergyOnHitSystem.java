/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.ArchetypeChunk
 *  com.hypixel.hytale.component.CommandBuffer
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.component.query.Query
 *  com.hypixel.hytale.server.core.entity.entities.ProjectileComponent
 *  com.hypixel.hytale.server.core.modules.entity.damage.Damage
 *  com.hypixel.hytale.server.core.modules.entity.damage.Damage$ProjectileSource
 *  com.hypixel.hytale.server.core.modules.entity.damage.Damage$Source
 *  com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem
 *  com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap
 *  com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 *  javax.annotation.Nonnull
 */
package com.fadingtime.hytalemod.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.entities.ProjectileComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

public class SignatureEnergyOnHitSystem
extends DamageEventSystem {
    private static final String VOLLEY_PROJECTILE_ID = "Skeleton_Mage_Corruption_Orb";
    @Nonnull
    private static final Query<EntityStore> QUERY = Query.any();

    public Query<EntityStore> getQuery() {
        return QUERY;
    }

    public void handle(int n, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull Damage damage) {
        Damage.Source source = damage.getSource();
        if (!(source instanceof Damage.ProjectileSource)) {
            return;
        }
        Damage.ProjectileSource projectileSource = (Damage.ProjectileSource)source;
        Ref ref = projectileSource.getRef();
        Ref ref2 = projectileSource.getProjectile();
        if (ref == null || ref2 == null) {
            return;
        }
        ProjectileComponent projectileComponent = (ProjectileComponent)store.getComponent(ref2, ProjectileComponent.getComponentType());
        if (projectileComponent == null) {
            return;
        }
        if (!VOLLEY_PROJECTILE_ID.equals(projectileComponent.getProjectileAssetName())) {
            return;
        }
        EntityStatMap entityStatMap = (EntityStatMap)store.getComponent(ref, EntityStatMap.getComponentType());
        if (entityStatMap == null) {
            return;
        }
        entityStatMap.addStatValue(DefaultEntityStatTypes.getSignatureEnergy(), 1.0f);
    }
}

