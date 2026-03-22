# Simple Sim Game

A top-down 2D settlement-building simulation built with [libGDX](https://libgdx.com/).

**Core Loop:** Explore procedurally generated world → Establish settlements → Build structures → Manage population → Expand your network.

## Features

### Terrain System
- **5 terrain types:** Water, Grass, Forest, Stone, Snow
- **Procedural generation:** Perlin noise with multiple octaves
- **Natural objects:** Trees and boulders spawn based on terrain
- **Infinite world:** Chunk-based loading for unlimited exploration

### Fog of War
- Tiles must be revealed by clicking adjacent to explored areas
- Revealed state persists across sessions
- Toggle fog overlay with `F`
- Return to last revealed tile with `HOME`

### Settlement System
- **4 levels:** Village → Town → City → Metropolis
- **55 building types:** Houses, farms, markets, workshops, temples, castles, and more
- **Population growth:** Buildings add population capacity
- **Upgrade system:** Reach population thresholds to level up settlements

### Controls
| Input | Action |
|-------|--------|
| Left Click | Reveal fog / Select tile |
| Drag | Pan camera |
| Scroll | Zoom in/out |
| `F` | Toggle fog of war |
| `HOME` | Center on last revealed tile |
| `1-9` | Quick select building for selected tile |

## Building

```bash
./gradlew lwjgl3:run    # Run the game
./gradlew lwjgl3:jar     # Build JAR
./gradlew lwjgl3:jarLinux  # Linux JAR
```

## Testing

### Running Tests

```bash
./gradlew core:test       # Run all tests
./gradlew core:test --info # Run with verbose output
```

### Test Structure

Tests live in `core/src/test/java/ch/obermuhlner/sim/game/`:

| Test File | Coverage |
|-----------|----------|
| `SettlementTest.java` | Settlement creation, population, levels, upgrades |
| `SettlementLevelTest.java` | Level enum behavior and thresholds |
| `TileTest.java` | Terrain, buildability, walkability |
| `BuildingTypeTest.java` | Building type IDs, display names, capacities |
| `WorldTest.java` | Chunk loading, fog-of-war, reveals |
| `mode/BuildModeLogicTest.java` | Building placement rules, proximity |

Integration tests in `it/` subdirectory test game flows with simulated input:

| Test File | Coverage |
|-----------|----------|
| `GameFlowIntegrationTest.java` | Mode switching, settlement lifecycle, camera controls |
| `HeadlessGameTest.java` | Base class for headless game tests |

### Testing Philosophy

**Unit tests** cover pure game logic without UI dependencies:
- Settlement state machines and level transitions
- Tile terrain rules and buildability
- Building type configuration
- World chunk management and fog-of-war

**Integration tests** run the actual game in a headless environment:
- Tile selection and reveal
- Input simulation (touch, keyboard, scroll)
- Settlement lifecycle (create, upgrade, remove)
- Camera controls (pan, zoom)

**What we don't test (requires real OpenGL):**
- Rendering layers (textures, sprites)
- UI components with textures (toolbar, panels)
- OpenGL-specific graphics operations

**Why:** libGDX's rendering requires native OpenGL libraries unavailable in headless testing. Tests that create textures are skipped. Visual correctness is verified manually.

## Debug Console

A text-based console for testing game functionality without the GUI.

### GUI + Console Mode

Run with GUI and text commands via stdin or REST API:

```bash
./gradlew lwjgl3:run -PmainClass=ch.obermuhlner.sim.GameWithConsole
```

The game starts with REST API on `http://localhost:8088/cmd`.

### REST API

Send commands via HTTP POST with the command in the request body:

```bash
# Get help
curl -X POST -d "help" http://localhost:8088/cmd

# View game state
curl -X POST -d "state" http://localhost:8088/cmd

# List settlements
curl -X POST -d "settlements" http://localhost:8088/cmd

# View map around coordinates
curl -X POST -d "map 5" http://localhost:8088/cmd

# Reveal fog at coordinates
curl -X POST -d "reveal 0 0" http://localhost:8088/cmd

# Create settlement
curl -X POST -d "spawn 5 5 MyTown" http://localhost:8088/cmd
```

### Commands Reference

| Command | Aliases | Description |
|---------|---------|-------------|
| `help` | | Show all commands |
| `state` | `s` | Full game state |
| `settlements` | `st` | Settlement details |
| `resources` | `r` | Resource summary |
| `tile <x> <y>` | `t` | Single tile info |
| `map [r] [cx] [cy]` | `m` | ASCII map (default 10x10 around 0,0) |
| `reveal <x> <y>` | | Reveal fog around point |
| `spawn <x> <y> [name]` | | Create settlement at coordinates |
| `status` | | Selected tile and available actions |
| `toolbar` | | Available toolbar buttons |

### Map Legend

ASCII map characters:
- `#` = fog (unrevealed)
- `G` / `.` = grass
- `F` / `T` = forest
- `S` / `O` = stone
- `W` / `~` = water
- `X` / `*` = snow
- `@` = settlement center
- `B` = building

### REST API Example Session

```bash
# Start the game with REST API
./gradlew lwjgl3:run -PmainClass=ch.obermuhlner.sim.GameWithConsole

# In another terminal:
curl -X POST -d "state" http://localhost:8088/cmd
# === GAME STATE ===
# --- SETTLEMENTS ---
# [1] Starter Village (Village)
#   Population: 10
#   Position: (-1, 1)
#   Buildings: 0/5
# ...

curl -X POST -d "reveal 8 -3" http://localhost:8088/cmd
# Revealed area around (8, -3)

curl -X POST -d "map 5 8 -3" http://localhost:8088/cmd
# Map around (8, -3), radius 5:
#   (fog=#, grass=G, forest=F, stone=S, water=W, snow=X)
#    2: ###########
#    1: ###########
#    0: #####.#####
#   -1: ###.....###
#   -2: ###.TTTT###
#   -3: ##TTTOTTT##
#   ...

curl -X POST -d "spawn 6 -2 SecondTown" http://localhost:8088/cmd
# Created settlement 'secondtown' at (6, -2)

curl -X POST -d "settlements" http://localhost:8088/cmd
# --- SETTLEMENTS ---
# [1] Starter Village (Village)
#   Population: 10
#   Position: (-1, 1)
#   Buildings: 0/5
# [2] secondtown (Village)
#   Population: 10
#   Position: (6, -2)
#   Buildings: 0/5
```

### UX Flow

The game uses a tile-selection UX model:

1. **Click a tile** - Reveals it if hidden, selects it if visible
2. **Toolbar shows actions** - Available buildings/actions update based on selected tile
3. **Click action in toolbar** - Places building or performs action on selected tile
4. **No mode switching** - Player is always in explore mode

### Programmatic Access

Use `GameDebugger` class to get text representations of game state:

```java
GameDebugger debugger = new GameDebugger(world);
String state = debugger.getState();
String settlements = debugger.getSettlementsInfo();
String map = debugger.getMapAround(0, 0, 10);
String tile = debugger.getTileInfo(5, 5);
```

## Project Structure

```
core/src/main/java/ch/obermuhlner/sim/
├── Main.java              - Entry point
└── game/
    ├── World.java         - Infinite world manager
    ├── Chunk.java         - 16x16 tile chunks
    ├── TerrainType.java   - Terrain enum
    ├── Settlement.java     - Settlement data
    ├── BuildingType.java   - 55 building types
    ├── render/
    │   ├── Renderer.java
    │   ├── TerrainRenderLayer.java
    │   ├── ObjectRenderLayer.java
    │   ├── BuildingRenderLayer.java
    │   ├── SettlementRenderLayer.java
    │   └── FogOfWarRenderLayer.java
    └── mode/
        ├── GameMode.java
        ├── ExploreMode.java
        └── BuildMode.java
```

## Implementation Phases

- [x] Phase 1: Foundation (terrain, fog, camera)
- [x] Phase 2: Settlement Building
- [ ] Phase 3: Road Network
- [ ] Phase 4: Economy
- [ ] Phase 5: Polish
