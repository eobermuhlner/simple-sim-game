# Simple Sim Game - Game Design Document

## 1. Game Overview

**Genre:** Top-down 2D settlement-building simulation
**Engine:** libGDX (LWJGL3 desktop backend)
**Core Loop:** Explore procedurally generated world → Establish settlements → Build road networks → Trade resources → Expand infrastructure

**Target Experience:** Relaxed exploration with satisfying progression as players build a thriving network of interconnected settlements.

---

## 2. Visual Specification

### 2.1 Scene Setup
- **View:** Top-down orthographic 2D
- **Camera:** Pan via mouse drag, zoom via scroll wheel (0.25x–4.0x range)
- **Tile Size:** 64×64 pixels
- **Chunk Size:** 16×16 tiles (1024×1024 pixels per chunk)
- **World:** Infinite, procedurally generated in chunks

### 2.2 Terrain Tiles
| Terrain | Tile Index | Description |
|---------|------------|-------------|
| Water | 1 | Deep blue, impassable |
| Grass | 2 | Light green, buildable |
| Forest | 3 | Dark green, resource-rich |
| Stone | 4 | Gray, mineral deposits |
| Snow | 5 | White, harsh but buildable |

### 2.3 Map Texture Atlas
- Source: `assets/64x64/map.png`
- Layout: 4 columns × N rows of 64×64 tiles
- Objects rendered as overlays on terrain tiles

### 2.4 Object Sprites
| Object | Texture | Valid On |
|--------|---------|----------|
| Large Tree | `64x64/objects/tree-large.png` | Grass, Forest |
| Small Tree | `64x64/objects/tree-small.png` | Grass |
| Large Boulder | `64x64/objects/boulder-large.png` | Grass, Forest |
| Small Boulder | `64x64/objects/boulder-small.png` | Grass, Stone |
| Snow Boulder | `64x64/objects/boulder-large-snow.png` | Snow |

### 2.5 Fog of War
- **State:** Revealed / Unrevealed per tile
- **Reveal Mechanic:** Click reveals tile only if adjacent to already-revealed tile
- **Persistence:** Revealed state saved to `chunks/[cx]_[cy].fow`
- **Toggle:** Press `F` key to enable/disable fog overlay
- **Home Return:** Press `HOME` to center camera on last-revealed tile

---

## 3. Simulation Specification

### 3.1 Terrain Generation
- **Algorithm:** Perlin noise with multiple octaves
- **Noise Scale:** 0.04 (controls terrain feature size)
- **Octaves:** 4 layers with 0.5 persistence each
- **Thresholds:**
  - < 0.45 → Water
  - 0.45–0.55 → Grass
  - 0.55–0.60 → Forest
  - 0.60–0.65 → Stone
  - > 0.65 → Snow

### 3.2 Object Spawning (Deterministic)
- Based on chunk seed and terrain type
- Spawn rates:
  - Grass: 50% trees, 10% boulders
  - Forest: 20% boulders
  - Stone: 40% small boulders, 20% large boulders
  - Snow: 20% snow boulders

### 3.3 Settlement System
- **Placement:** On any buildable tile (Grass, Forest, Stone, Snow)
- **Size Levels:** Village → Town → City → Metropolis
- **Upgrades:** Require resources and population thresholds
- **Radius:** Each settlement claims surrounding tiles

### 3.4 Road System
- **Construction:** Player places road tiles between settlements
- **Cost:** 1 stone per tile segment
- **Benefits:** Enables trade routes when connecting two settlements
- **Visual:** Distinct road sprite overlay

### 3.5 Resource System
| Resource | Source | Used For |
|----------|--------|----------|
| Wood | Forests, Grass tiles with trees | Buildings, roads (in early game) |
| Stone | Stone terrain, quarries | Roads, advanced buildings |
| Food | Grass/Forest tiles | Population growth |
| Trade Goods | Produced by settlements | Sold for profit |

### 3.6 Trade System
- **Routes:** Established when road connects two settlements
- **Caravans:** Animated units that travel roads
- **Income:** Based on settlement size and goods produced
- **Frequency:** Increases with settlement levels

---

## 4. Interaction Specification

### 4.1 Controls
| Input | Action |
|-------|--------|
| Left Click (short) | Reveal fog tile |
| Left Click + Drag | Pan camera |
| Mouse Scroll | Zoom in/out |
| `PAGE UP` | Zoom in |
| `PAGE DOWN` | Zoom out |
| `HOME` | Return to last revealed tile |
| `F` | Toggle fog of war |
| `B` | Enter build mode |
| `ESC` | Exit current mode / Pause |

### 4.2 Build Mode UI
- **Trigger:** Press `B`
- **Menu:** Radial or sidebar showing buildable structures
- **Preview:** Ghost image of structure follows cursor
- **Placement:** Left-click to place, right-click to cancel
- **Invalid Placement:** Red tint on tiles where building is not allowed

### 4.3 Settlement Management
- **Select:** Click on settlement center tile
- **Info Panel:** Shows name, population, resources, buildings
- **Build Menu:** Options to add structures within settlement

---

## 5. UI Specification

### 5.1 HUD Elements
- **Top-Left:** Current resources (Wood, Stone, Food, Gold)
- **Top-Right:** Minimap (toggleable)
- **Bottom:** Build toolbar (when in build mode)

### 5.2 Settlement Panel
- Appears when settlement selected
- Displays:
  - Settlement name
  - Population count
  - Resource production rates
  - Building list with upgrade buttons
  - Trade route status

### 5.3 Visual Style
- **Color Palette:** Earth tones with vibrant accent colors
- **Font:** Monospace for UI (pixel aesthetic)
- **Icons:** 16×16 inline sprites for resources

---

## 6. Technical Architecture

### 6.1 Module Structure
```
core/src/main/java/ch/obermuhlner/sim/
├── Main.java                 - Entry point, application lifecycle
├── game/
│   ├── TerrainType.java      - Terrain enum with properties
│   ├── TileObject.java       - Interface for tile objects
│   ├── TileObjectType.java   - Object type enum
│   ├── TileObjectRegistry.java - Object ID registry
│   ├── Tile.java             - Tile data container
│   ├── Chunk.java            - 16x16 tile chunk with fog
│   ├── World.java            - Infinite world manager
│   ├── TerrainGenerator.java - Perlin noise terrain generation
│   ├── render/
│   │   ├── RenderLayer.java  - Layer interface for rendering
│   │   ├── Renderer.java     - Orchestrates render layers
│   │   ├── TerrainRenderLayer.java
│   │   ├── ObjectRenderLayer.java
│   │   └── FogOfWarRenderLayer.java
│   └── mode/
│       ├── GameMode.java     - Interface for game modes
│       └── ExploreMode.java  - Exploration mode implementation
```

### 6.2 Extension Points

**Adding a new terrain type:**
1. Add entry to `TerrainType.java` with tile index and properties
2. Update `TerrainGenerator.getTerrainFromNoise()` thresholds
3. Optionally override `TerrainGenerator.generateNaturalObjects()`

**Adding a new object:**
1. Add constant to `TileObjectRegistry.java`
2. Register in `TileObjectRegistry.init()` with terrain placement rules
3. Add texture loading in `ObjectRenderLayer.loadAssets()`

**Adding a render layer:**
1. Implement `RenderLayer` interface
2. Add via `renderer.addLayer(new YourLayer(world))`
3. Set `getOrder()` to control draw order

**Adding a game mode:**
1. Implement `GameMode` interface
2. Register via `main.setGameMode(new YourMode())`
3. Modes can be switched at runtime (explore, build, trade, etc.)

### 6.3 Data Persistence
- **Fog of War:** Per-chunk binary files (`chunks/[cx]_[cy].fow`)
- **Game State:** JSON file for settlements, roads, resources
- **Save Location:** `Gdx.files.local()` directory

---

## 7. Implementation Phases

### Phase 1: Foundation (Complete)
- [x] Tile rendering with texture atlas
- [x] Perlin noise terrain generation
- [x] Chunk-based world loading
- [x] Fog of war system
- [x] Camera pan/zoom controls

### Phase 2: Settlement Building
- [ ] Settlement placement system
- [ ] Settlement selection and info panel
- [ ] Building placement mechanics
- [ ] Population system

### Phase 3: Road Network
- [ ] Road tile rendering
- [ ] Road placement tool
- [ ] Path connectivity detection
- [ ] Road-based route visualization

### Phase 4: Economy
- [ ] Resource gathering from tiles
- [ ] Resource storage
- [ ] Trade route establishment
- [ ] Caravan animation

### Phase 5: Polish
- [ ] Sound effects
- [ ] Particle effects for building
- [ ] Minimap
- [ ] Save/Load system

---

## 8. Game Balance

### 8.1 Starting Conditions
- Begin at tile (0,0) revealed
- No resources, no settlements
- Tutorial prompts guide first settlement

### 8.2 Progression Curve
- **Villages:** 1–50 population, basic production
- **Towns:** 51–200 population, can build roads
- **Cities:** 201–500 population, trade routes unlock
- **Metropolis:** 500+ population, all upgrades available

### 8.3 Win Condition
Sandbox mode — no forced win. Player sets goals:
- Build 10 settlements
- Connect all settlements with roads
- Achieve highest population settlement

---

## 9. Glossary

| Term | Definition |
|------|------------|
| Chunk | 16×16 tile grid unit loaded/unloaded as camera moves |
| Fog of War | Visibility state; undiscovered tiles are hidden |
| Settlement | Player-established city with population and buildings |
| Road | Improved path tile enabling trade routes |
| Trade Route | Connection between two settlements via roads |
| Caravan | Traveling unit that moves goods between settlements |

