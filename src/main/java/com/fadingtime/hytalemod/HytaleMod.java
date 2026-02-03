package com.fadingtime.hytalemod;

import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.logging.Level;
import javax.annotation.Nonnull;

/**
 * Hytale Surivors - A Hytale server mod that automatically fires fireballs.
 *
 * This mod gives all players a passive ability that fires three fireballs
 * in a triangular spread pattern every 10 seconds.
 *
 * @author FadingTime
 * @version 1.0.0
 */
public class HytaleMod extends JavaPlugin {

    public static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger("Hytale Surivors");
    private static HytaleMod INSTANCE;
    private ComponentType<EntityStore, VampireShooterComponent> vampireShooterComponentType;
    private ComponentType<EntityStore, SpawnedByMobWaveComponent> mobWaveMarkerComponentType;
    private ComponentType<EntityStore, BossWaveComponent> bossWaveComponentType;
    private MobWaveSpawner mobWaveSpawner;
    // SignatureEnergySystem removed

    public HytaleMod(@Nonnull JavaPluginInit init) {
        super(init);
        INSTANCE = this;
    }

    public static HytaleMod getInstance() {
        return INSTANCE;
    }

    public MobWaveSpawner getMobWaveSpawner() {
        return this.mobWaveSpawner;
    }

    @Override
    protected void setup() {
        // Called during plugin setup phase
        // Register codecs, configs here
        getLogger().at(Level.INFO).log("Hytale Surivors is setting up...");
        
        // Register the VampireShooterComponent
        ComponentRegistryProxy<EntityStore> entityStoreRegistry = this.getEntityStoreRegistry();
        this.vampireShooterComponentType = entityStoreRegistry.registerComponent(
            VampireShooterComponent.class, 
            VampireShooterComponent::new
        );
        this.mobWaveMarkerComponentType = entityStoreRegistry.registerComponent(
            SpawnedByMobWaveComponent.class,
            SpawnedByMobWaveComponent::new
        );
        this.bossWaveComponentType = entityStoreRegistry.registerComponent(
            BossWaveComponent.class,
            BossWaveComponent::new
        );

        // Register systems
        entityStoreRegistry.registerSystem(new VampireShooterAdder(this.vampireShooterComponentType));
        entityStoreRegistry.registerSystem(new VampireShooterSystem(this.vampireShooterComponentType));
        entityStoreRegistry.registerSystem(new SignatureEnergyOnHitSystem());
        entityStoreRegistry.registerSystem(new LifeEssenceDropSystem(this.mobWaveMarkerComponentType));
        entityStoreRegistry.registerSystem(new BossWaveDeathSystem(this.bossWaveComponentType));

        this.mobWaveSpawner = new MobWaveSpawner(
            this,
            this.mobWaveMarkerComponentType,
            this.bossWaveComponentType
        );

        getLogger().at(Level.INFO).log("Hytale Surivors setup complete!");
    }

    @Override
    protected void start() {
        // Called when plugin starts
        // Register events, commands, entities, blocks here
        getLogger().at(Level.INFO).log("Hytale Surivors started successfully!");
        
        // SignatureEnergySystem removed
    }

    @Override
    protected void shutdown() {
        // Called when plugin shuts down
        // Cleanup resources here
        getLogger().at(Level.INFO).log("HytaleMod is shutting down...");

        if (this.mobWaveSpawner != null) {
            this.mobWaveSpawner.shutdown();
        }

        // SignatureEnergySystem removed
    }
}
