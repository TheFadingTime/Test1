package com.fadingtime.hytalemod;

import com.fadingtime.hytalemod.command.PowerUpStoreCommand;
import com.fadingtime.hytalemod.command.ProjectileRainCommand;
import com.fadingtime.hytalemod.command.StartWavesCommand;
import com.fadingtime.hytalemod.component.BossWaveComponent;
import com.fadingtime.hytalemod.component.LifeEssenceDropComponent;
import com.fadingtime.hytalemod.component.ProjectileBounceComponent;
import com.fadingtime.hytalemod.component.SpawnedByMobWaveComponent;
import com.fadingtime.hytalemod.component.VampireShooterComponent;
import com.fadingtime.hytalemod.config.ConfigManager;
import com.fadingtime.hytalemod.persistence.PlayerStateStoreManager;
import com.fadingtime.hytalemod.spawner.MobWaveSpawner;
import com.fadingtime.hytalemod.system.BossHudSystem;
import com.fadingtime.hytalemod.system.BossWaveDeathSystem;
import com.fadingtime.hytalemod.system.LifeEssenceDropSystem;
import com.fadingtime.hytalemod.system.LifeEssencePickupSystem;
import com.fadingtime.hytalemod.system.PlayerProgressionManager;
import com.fadingtime.hytalemod.system.PlayerDamageBonusSystem;
import com.fadingtime.hytalemod.system.ProjectileBounceOnHitSystem;
import com.fadingtime.hytalemod.system.SignatureEnergyOnHitSystem;
import com.fadingtime.hytalemod.system.VampireShooterAdder;
import com.fadingtime.hytalemod.system.VampireShooterSystem;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.system.ISystem;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

public class HytaleMod
extends JavaPlugin {
    public static final Logger LOGGER = Logger.getLogger("Hytale Survivors");
    private static HytaleMod INSTANCE;
    private ComponentType<EntityStore, VampireShooterComponent> vampireShooterComponentType;
    private ComponentType<EntityStore, SpawnedByMobWaveComponent> mobWaveMarkerComponentType;
    private ComponentType<EntityStore, BossWaveComponent> bossWaveComponentType;
    private ComponentType<EntityStore, LifeEssenceDropComponent> lifeEssenceDropComponentType;
    private ComponentType<EntityStore, ProjectileBounceComponent> projectileBounceComponentType;
    private MobWaveSpawner mobWaveSpawner;
    private BossHudSystem bossHudSystem;
    private PlayerProgressionManager lifeEssenceLevelSystem;
    private PlayerStateStoreManager stateStoreManager;

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

    public BossHudSystem getBossHudSystem() {
        return this.bossHudSystem;
    }

    public ComponentType<EntityStore, VampireShooterComponent> getVampireShooterComponentType() {
        return this.vampireShooterComponentType;
    }

    public ComponentType<EntityStore, ProjectileBounceComponent> getProjectileBounceComponentType() {
        return this.projectileBounceComponentType;
    }

    public PlayerProgressionManager getLifeEssenceLevelSystem() {
        return this.lifeEssenceLevelSystem;
    }

    public PlayerStateStoreManager getStateStoreManager() {
        return this.stateStoreManager;
    }

    protected void setup() {
        this.getLogger().at(Level.INFO).log("Hytale Survivors is setting up...");
        ConfigManager.load(this.getFile(), LOGGER);
        ComponentRegistryProxy entityStoreRegistry = this.getEntityStoreRegistry();
        this.vampireShooterComponentType = entityStoreRegistry.registerComponent(VampireShooterComponent.class, VampireShooterComponent::new);
        this.mobWaveMarkerComponentType = entityStoreRegistry.registerComponent(SpawnedByMobWaveComponent.class, SpawnedByMobWaveComponent::new);
        this.bossWaveComponentType = entityStoreRegistry.registerComponent(BossWaveComponent.class, BossWaveComponent::new);
        this.lifeEssenceDropComponentType = entityStoreRegistry.registerComponent(LifeEssenceDropComponent.class, LifeEssenceDropComponent::new);
        this.projectileBounceComponentType = entityStoreRegistry.registerComponent(ProjectileBounceComponent.class, ProjectileBounceComponent::new);
        this.stateStoreManager = new PlayerStateStoreManager(this.getFile());
        this.mobWaveSpawner = new MobWaveSpawner(this, this.mobWaveMarkerComponentType, this.bossWaveComponentType);
        this.lifeEssenceLevelSystem = new PlayerProgressionManager(this);
        entityStoreRegistry.registerSystem((ISystem)new VampireShooterAdder(this.vampireShooterComponentType));
        entityStoreRegistry.registerSystem((ISystem)new VampireShooterSystem(this.vampireShooterComponentType));
        entityStoreRegistry.registerSystem((ISystem)new SignatureEnergyOnHitSystem());
        entityStoreRegistry.registerSystem((ISystem)new PlayerDamageBonusSystem());
        entityStoreRegistry.registerSystem((ISystem)new ProjectileBounceOnHitSystem(this.projectileBounceComponentType));
        entityStoreRegistry.registerSystem((ISystem)new LifeEssenceDropSystem(this.mobWaveMarkerComponentType, this.bossWaveComponentType, this.lifeEssenceDropComponentType));
        entityStoreRegistry.registerSystem((ISystem)new LifeEssencePickupSystem(this.lifeEssenceDropComponentType, this.vampireShooterComponentType, this.lifeEssenceLevelSystem));
        entityStoreRegistry.registerSystem((ISystem)new BossWaveDeathSystem(this.bossWaveComponentType));
        this.bossHudSystem = new BossHudSystem(this.bossWaveComponentType, this.mobWaveSpawner);
        entityStoreRegistry.registerSystem((ISystem)this.bossHudSystem);
        this.getLogger().at(Level.INFO).log("Hytale Survivors setup complete!");
    }

    protected void start() {
        this.getLogger().at(Level.INFO).log("Hytale Survivors started successfully!");
        this.getCommandRegistry().registerCommand((AbstractCommand)new PowerUpStoreCommand());
        this.getCommandRegistry().registerCommand((AbstractCommand)new StartWavesCommand());
        this.getCommandRegistry().registerCommand((AbstractCommand)new ProjectileRainCommand());
    }

    protected void shutdown() {
        this.getLogger().at(Level.INFO).log("HytaleMod is shutting down...");
        if (this.mobWaveSpawner != null) {
            this.mobWaveSpawner.shutdown();
        }
    }
}
