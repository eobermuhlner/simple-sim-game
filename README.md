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
| Left Click | Reveal fog / Place |
| Drag | Pan camera |
| Scroll | Zoom in/out |
| `B` | Enter build mode |
| `S` | Start settlement placement (in build mode) |
| `1-9` | Select building type |
| `ESC` | Exit build mode |
| `F` | Toggle fog of war |
| `HOME` | Center on last revealed tile |

## Building

```bash
./gradlew lwjgl3:run    # Run the game
./gradlew lwjgl3:jar     # Build JAR
./gradlew lwjgl3:jarLinux  # Linux JAR
```

## Debug Console

A text-based console for testing game functionality without the GUI.

```bash
./gradlew core:run -PmainClass=ch.obermuhlner.sim.GameConsole
```

### Commands
| Command | Description |
|---------|-------------|
| `help` | Show available commands |
| `state` (s) | Full game state |
| `settlements` (st) | Settlement details |
| `resources` (r) | Resource summary |
| `tile <x> <y>` | Single tile info |
| `map [r] [cx] [cy]` | ASCII map (default 10x10) |
| `reveal <x> <y>` | Reveal area around point |
| `spawn <x> <y> [name]` | Create settlement |
| `build <x> <y> [id]` | Place building on tile |

### Example Session
```
> state
=== GAME STATE ===
--- SETTLEMENTS ---
No settlements.

--- RESOURCES ---
Settlements: 0
Total Population: 0
Total Buildings: 0

> spawn 5 5 MyTown
Created settlement 'MyTown' at (5, 5)

> map 5
Map around (5, 5), radius 5:
  (fog=#, grass=G, forest=F, stone=S, water=W, snow=X)
  10: ##########~####
   9: ##########~####
   8: #######~~.~~##
   7: ######~~...~##
   6: #####~..T..~##
   5: ####~..T@...##
   4: ####~...FF..##
   3: #####~~.FF.~~#
   2: ######~~...~##
   1: #######~~..~~#
      0123456789
```

### Programmatic Access

Use `GameDebugger` class to get text representations of game state:

```java
GameDebugger debugger = new GameDebugger(world);
String state = debugger.getState();
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
