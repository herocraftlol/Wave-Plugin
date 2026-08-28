# 🧟 ZombieWaves - Minecraft Plugin

> **Survive the waves of the undead!** ZombieWaves is a **Call of Duty Zombies** inspired
> wave-survival plugin for Minecraft **1.21**. Team up with friends, fend off endless waves of
> zombies, skeletons and husks, earn gold, and gear up in the shop to push your survival further.

---

## 🆕 What's new in v1.4.2

- **Fully functional shop** 🔧 — the shop GUI now actually works: click an item to buy it,
  with your gold deducted and a purchase sound. Items can no longer be picked up or dragged
  out of the menu.
- **Unified `/wave` command** 🎮 — join, leave, arenas, shop, gold **and** the admin
  subcommands (`reload`, `setwave`, `forcewave`, `stop`) are all in one place.
- **New admin tools** ⚙️ — reload the config on the fly, jump straight to a given wave,
  or force-start the next wave.
- **Bug fixes** 🐛 — arena serialization on startup and various polish fixes.

## ✨ Features

| Feature | Description |
|---------|-------------|
| 🌊 **Wave System** | Survive increasingly difficult waves of mobs |
| 🗺️ **Arena System** | Create custom arenas with spawn points and boundaries |
| 🎮 **Lobby System** | Join arenas with countdown, max 20 players |
| 🚪 **Auto Teleport** | Lobby before game, exit after |
| 🎲 **Random Spawning** | Mobs spawn at random locations with random types |
| 📈 **Difficulty Scaling** | Health, damage, and speed increase each wave |
| 🪙 **Gold System** | Earn gold by killing mobs |
| 🛒 **Shop** | Buy weapons, armor, and upgrades |
| 📋 **Scoreboard** | Side panel with wave, kills, gold, remaining mobs |

## 🎮 Supported Mobs
- **Zombie** (60% spawn rate) - Base enemy
- **Skeleton** (25% spawn rate) - Ranged attacker  
- **Husk** (15% spawn rate) - Tough variant

## 📥 Installation

1. Download the latest JAR from [Releases](../../releases)
2. Place the JAR in your server's `plugins/` folder
3. Restart the server

## 🎯 Quick Setup (Admin)

1. **Set lobby and exit locations** (where players spawn/return):
   ```
   /wave setlobby         # At your location (global lobby)
   /wave setexit          # At your location (return point)
   ```

2. **Create an arena:**
   ```
   /wave createarena mymap
   ```

3. **Set arena lobby** (optional - overrides global):
   ```
   /wave setlobby mymap   # Set arena-specific lobby
   ```

4. **Set arena spawn** (where players go when game starts):
   ```
   /wave setspawn mymap    # At your location
   ```

5. **Set arena boundaries** (optional):
   ```
   /wave setpos1 mymap    # Look at corner 1
   /wave setpos2 mymap    # Look at corner 2
   ```

6. **Add spawn points** (where mobs appear):
   ```
   /wave addspawn mymap   # Look at spawn location, repeat
   ```

## 🎮 Player Commands

| Command | Description |
|---------|-------------|
| `/wave join <arena>` | Join an arena (teleport to lobby) |
| `/wave leave` | Leave the arena (return to exit) |
| `/wave arenas` | List all available arenas |
| `/wave status` | Show game/lobby status |
| `/wave shop` | Open the shop |
| `/wave gold` | Check your gold balance |

## 🛠 Admin Commands

| Command | Description |
|---------|-------------|
| `/wave createarena <name>` | Create a new arena |
| `/wave deletearena <name>` | Delete an arena |
| `/wave infoarena <name>` | Show arena details |
| `/wave setlobby [arena]` | Set lobby location |
| `/wave setspawn <arena>` | Set game spawn point |
| `/wave setexit` | Set exit location |
| `/wave setpos1 <arena>` | Set boundary corner 1 |
| `/wave setpos2 <arena>` | Set boundary corner 2 |
| `/wave addspawn <arena>` | Add mob spawn point |
| `/wave removespawn <arena>` | Remove spawn point |
| `/wave stop` | Force stop the game |

## 🔑 Permissions

- `zombiewaves.admin` - All admin commands
- `zombiewaves.shop` - Access to the shop

## 🎯 How It Works

1. **Players join:** `/wave join mymap`
2. **Teleport to lobby** at the arena's lobby location
3. **Wait for players** (1+ to start, max 20 — solo play supported)
4. **Countdown** starts automatically
5. **Game starts:** Players teleport to game spawn
6. **Survive waves** of zombies, skeletons, and husks
7. **Leave anytime:** `/wave leave` returns you to exit

## 🔨 Building

```bash
mvn clean package
```

JAR in `target/ZombieWaves-1.4.2.jar`

## ⚙️ Configuration (config.yml)

### Wave & Mob Scaling
```yaml
waves:
  base-mobs: 5                    # Base mobs per wave
  mob-increase-per-wave: 3        # Additional mobs per wave
  player-scaling-multiplier: 0.5  # Mob scaling per player (0.5 = +50% per player)
  min-players-for-scaling: 1      # Minimum players for scaling
  spawn-delay: 10                 # Ticks between spawns (20 = 1 second)
  max-active-mobs: 15              # Max mobs alive at once
```

**Mob Scaling Formula:**
```
mobs = (baseMobs + wave * increase) * (1 + multiplier * players)
```

**Example:** `base=5, increase=3, multiplier=0.5`
- Wave 3, 1 player: `(5 + 9) * (1 + 0.5) = 21 mobs`
- Wave 3, 4 players: `(5 + 9) * (1 + 2.0) = 42 mobs`

### Mob Types (% = spawn weight)
```yaml
mob-types:
  zombie:
    entity-type: ZOMBIE
    base-health: 20.0
    gold-per-kill: 5
    spawn-weight: 60    # 60%
  skeleton:
    entity-type: SKELETON
    base-health: 16.0
    gold-per-kill: 7
    spawn-weight: 25    # 25%
  husk:
    entity-type: HUSK
    base-health: 24.0
    gold-per-kill: 6
    spawn-weight: 15    # 15%
```

### Difficulty Scaling
```yaml
difficulty:
  health-multiplier: 0.15    # +15% health per wave
  damage-multiplier: 0.10    # +10% damage per wave
  speed-multiplier: 0.05     # +5% speed per wave (capped)
```