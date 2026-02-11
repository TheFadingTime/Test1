# Hytale Survivors Mod

A Hytale server mod that adds wave spawning, a boss HUD, a leveling system based on Life Essence drops, and a power-up store UI.

**Quick Start**
1. Build: `./gradlew.bat build`
2. Copy jar: `build\libs\HytaleMod-1.0.0.jar` to `C:\Users\fadin\AppData\Roaming\Hytale\UserData\Mods`
3. Launch Hytale server and load the mod

**Key Features**
- Wave spawning with boss waves
- Boss health HUD
- Life Essence leveling
- Power-up store UI
- Projectile volley system

**Commands**
- `powerupstore` opens or closes the power-up store page

**Project Structure**
- `HytaleMod.java` plugin entry point
- `command/` player commands
- `component/` ECS components
- `config/` configs and constants
- `helper/` reusable helpers
- `spawner/` wave and boss spawners
- `system/` ECS systems
- `ui/` custom HUD and page classes

See `docs/GAME_SYSTEMS.md` and `docs/ARCHITECTURE.md` for details.
