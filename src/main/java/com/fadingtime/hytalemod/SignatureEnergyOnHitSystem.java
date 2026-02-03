package com.fadingtime.hytalemod;

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

/**
 * Grants signature energy to the shooter when one of our timed volley projectiles hits a target.
 */
public class SignatureEnergyOnHitSystem extends DamageEventSystem {

    private static final String VOLLEY_PROJECTILE_ID = "Skeleton_Mage_Corruption_Orb";

    @Nonnull
    private static final Query<EntityStore> QUERY = Query.any();

    @Override
    public Query<EntityStore> getQuery() {
        return QUERY;
    }

    @Override
    public void handle(
        int index,
        @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Damage damage
    ) {
        if (!(damage.getSource() instanceof Damage.ProjectileSource projectileSource)) {
            return;
        }

        Ref<EntityStore> shooterRef = projectileSource.getRef();
        Ref<EntityStore> projectileRef = projectileSource.getProjectile();

        if (shooterRef == null || projectileRef == null) {
            return;
        }

        ProjectileComponent projectileComponent = store.getComponent(projectileRef, ProjectileComponent.getComponentType());
        if (projectileComponent == null) {
            return;
        }

        if (!VOLLEY_PROJECTILE_ID.equals(projectileComponent.getProjectileAssetName())) {
            return;
        }

        EntityStatMap statMap = store.getComponent(shooterRef, EntityStatMap.getComponentType());
        if (statMap == null) {
            return;
        }

        statMap.addStatValue(DefaultEntityStatTypes.getSignatureEnergy(), 1.0F);
    }
}
