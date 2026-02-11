# Game Systems Overview

## Wave Spawning System
The `MobWaveSpawner` handles periodic enemy spawns around each player. It runs timed cycles, spawns standard waves, and schedules boss waves. Boss health scales by wave count.

## Boss HUD System
The `BossHudSystem` drives the boss health bar UI. It reads the boss entity health and updates the custom HUD for the owning player. The HUD is hidden when the boss dies.

## Life Essence Drops
The `LifeEssenceDropSystem` spawns Life Essence items from defeated enemies. These are visual drops used for leveling.

## Life Essence Pickup and Leveling
The `LifeEssencePickupSystem` handles custom pickup logic. The item does not enter the inventory or play a pickup sound, but it still counts for leveling.

The `LifeEssenceLevelSystem` tracks per-player level and progress using this curve:
- `essenceRequired = 10 + 5 * (level - 1)`

When the player reaches Level 5, the power-up store opens for 30 seconds and the world is paused.
The store then reopens every 5 levels up to Level 50 (Level 5, Level 10, Level 15, Level 20, Level 25, Level 30, Level 35, Level 40, Level 45, Level 50).

## Power-Up Store UI
The store is a `CustomUIPage` with three selectable cards. Clicking a card applies a power-up and closes the store. The `+1 Projectile` choice increases projectile count by one.

## Projectile Volley System
The `VampireShooterSystem` fires projectiles on a timer for each player. The direction uses the camera (head rotation). Projectile count determines how many shots are fired:
- 0: no shots
- 1: center shot
- 2: center + left
- 3: center + left + right

## Signature Energy On Hit
`SignatureEnergyOnHitSystem` awards signature energy when a volley projectile hits a target.
