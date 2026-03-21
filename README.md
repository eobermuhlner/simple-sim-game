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
