# Architecture

## Package Layout
- `com.fadingtime.hytalemod` plugin entry point and wiring
- `command` commands that open UI or toggle systems
- `component` ECS components used by systems
- `config` configuration constants and IDs
- `helper` shared utilities for projectiles or other helpers
- `spawner` wave and boss spawning logic
- `system` ECS systems for gameplay
- `ui` custom HUD and custom page classes

## Data Flow
1. `MobWaveSpawner` spawns enemies and bosses.
2. `BossHudSystem` reads boss health and updates the HUD for the owning player.
3. `LifeEssenceDropSystem` creates Life Essence drops from enemies.
4. `LifeEssencePickupSystem` detects pickups and increments leveling.
5. `LifeEssenceLevelSystem` tracks level progress and opens the store at Level 5, then every 5 levels up to Level 50.
6. `PowerUpStorePage` applies a chosen power-up and closes.
7. `VampireShooterSystem` fires based on projectile count.

## UI Assets
- `Common/UI/Custom/BossBar.ui` boss HUD
- `Common/UI/Custom/HUD/LevelProgress.ui` leveling HUD
- `Common/UI/Custom/Pages/PowerUpStore.ui` power-up store

## Notes
- The store uses a custom page so it can receive mouse input.
- World pause/unpause is reference-counted to avoid desync if multiple stores open.
