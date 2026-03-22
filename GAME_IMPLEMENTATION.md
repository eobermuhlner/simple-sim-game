# Simple Sim Game - Implementation Reference

## 1. Game Overview

**Genre:** Top-down 2D settlement-building & logistics simulation
**Engine:** libGDX (LWJGL3 desktop backend)

**Core Loop:** Start with a settlement → Explore outward → Discover resources and strategic locations → Found specialized settlements → Build road networks → Establish trade routes → Research new tech → Expand network

**Early Game:** Player begins with a single settlement (one hut) auto-placed on suitable terrain (grass, large area, adjacent to multiple terrain types). Settlement produces resources slowly. Player explores to find resource caches and promising locations for next settlement. The game truly begins when the second settlement is founded and trade becomes possible.

**Key Moment — First Trade Connection:** When the second settlement connects via road:
1. First caravan spawns with visible animation
2. Gold income begins
3. UI highlights trade activation with notification
4. **Brief time slowdown (0.5s)** to let player register the event
5. Tutorial hint: "Trade routes generate income based on caravan deliveries"

**Design Pillars:**
- Simplicity over complexity
- Meaningful decisions through constraints
- Visible, living systems
- Relaxed but engaging gameplay

For details on the design refer to [GAME_DESIGN.md](GAME_DESIGN.md).

---

## 2. Visual Specification

### 2.1 Scene Setup
- **View:** Top-down orthographic 2D
- **Camera:** Pan via mouse drag, zoom via scroll wheel (0.25x–4.0x range)
- **Tile Size:** 64×64 pixels
- **Chunk Size:** 16×16 tiles (1024×1024 pixels per chunk)
- **World:** Infinite, procedurally generated in chunks

### 2.2 Terrain Tiles
| Terrain | Tile Index | Effect |
|---------|------------|--------|
| Water (Deep Sea) | 1 | Impassable, open ocean (requires Large Ships) |
| Shallow Sea | 2 | Coastal waters, explorable with Harbor + Small Ship |
| Grass | 3 | Balanced, good for food, buildable |
| Forest | 4 | High wood, harder road building, buildable |
| Stone | 5 | High stone, buildable |
| Snow | 6 | Slow growth, rare bonuses, buildable |

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
- **Sea Exploration:** Only 1 adjacent sea tile can be revealed from land (coastline visibility)

---

## 3. Simulation Specification

### 3.0 Simulation Loop

The simulation runs on a **fixed tick system** to ensure deterministic behavior and consistent timing.

#### Tick System
- **Tick Rate:** 1 tick per second (1 Hz)
- **Update Order:** Resources → Population → Caravan Movement → **Trade** → **Pricing** → Upkeep
- **Rendering:** Interpolated between ticks for smooth visuals at 60 FPS
- **Note:** Trade (delivery resolution) runs before Pricing so prices reflect latest deliveries

#### Per-Tick Updates
| System | Update |
|--------|--------|
| Resource Production | Calculate production based on terrain, buildings, specialization |
| Population Growth | Apply growth formula based on food surplus/deficit |
| Caravan Movement | Advance caravans along routes (fractional tile movement) |
| **Trade** | Process completed deliveries, apply export backlog pressure, spawn new caravans |
| **Dynamic Pricing** | Recalculate local prices based on smoothed supply/demand |
| Upkeep | Deduct gold for roads, caravans, buildings |

#### Determinism & Precision
- **Quantize critical values:** Resource amounts, positions, and time use integer or fixed-point representation where possible
- **Smoothed values:** Use `lerp(prev, current, 0.1)` to prevent floating-point drift and price oscillation
- **Batch updates:** Process all entities of one type before moving to the next system

---

### 3.1 Terrain Generation
- **Algorithm:** Perlin noise with multiple octaves
- **Noise Scale:** 0.04 (controls terrain feature size)
- **Octaves:** 4 layers with 0.5 persistence each
- **Thresholds:**
  - < 0.40 → Deep Sea (Water)
  - 0.40–0.45 → Shallow Sea
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

#### Auto-placed Starter Settlement
- Game auto-places first settlement (one hut) on suitable terrain at game start
- Criteria: Grass terrain, large enough area, adjacent to multiple terrain types
- Player begins with this single settlement producing resources slowly

#### Growth Levels
| Level | Population | Unlocks |
|-------|------------|---------|
| Village | 1–50 | Basic production |
| Town | 51–200 | Road building, specialization choice |
| City | 201–500 | Trade optimization |
| Metropolis | 500+ | Advanced upgrades |

#### Specialization System
Settlements start generic and specialize at the Village → Town upgrade:

| Specialization | Production Bonus | Limits | Unlocks Tech Branch |
|----------------|------------------|--------|---------------------|
| Logging Camp | High wood | Other production types | Forestry upgrades, advanced wood buildings, tree farming |
| Mining Town | High stone | Other production types | Deep mining, stone processing, quarry upgrades |
| Farming Village | High food | Other production types | Irrigation, crop variety, food storage |
| Trade Hub | Trade income boost, faster caravans | — | Caravan improvements, road upgrades, large ships |

**Specialization bonuses:**
- Grants production bonuses and unlocks a tech branch
- Limits other production types

**Trade Hub Diminishing Returns:** Trade income bonus scales with `sqrt(connections)` instead of linearly:
```
tradeBonus = baseBonus * sqrt(activeRoutes)
```
- 1 route = 1.0x bonus
- 4 routes = 2.0x bonus (vs 4.0x linear)
- 9 routes = 3.0x bonus (vs 9.0x linear)
- This prevents single central hub from dominating while keeping the fantasy intact

**Re-specialization:** Settlement can re-specialize, but drops one level (Town → Village) and must upgrade again. Cost scales naturally — changing a City hurts more than changing a Town.

#### Population Growth Formula
- **Food Consumption:** `foodConsumed = population * 0.15` per tick (tuned for slower early growth)
- **Growth Condition:**
  - `surplus = foodProduction - foodConsumed`
  - If `surplus > 0`: Growth = `surplus * growthRate * levelMultiplier`
  - If `surplus < 0`: Starvation = `-surplus * starvationRate`

- **Growth Rate:** 0.01 (1% of surplus per tick)
- **Starvation Rate:** 0.02 (2% of deficit per tick, capped to prevent instant death)
- **Level Multiplier:** Slows as settlement grows
  - Village: 1.0x
  - Town: 0.8x
  - City: 0.6x
  - Metropolis: 0.4x

- **Growth Limits:** Cannot exceed level cap
  - Village: max 50
  - Town: max 200
  - City: max 500
  - Metropolis: no limit

**Starvation Behavior:** Population decreases but never below 1. Visual degradation appears when population declining.

**Balance Note:** 1 grass tile produces ~0.5 food/tick, with 0.15 per pop this supports ~3-4 population per food tile (reduced from 5). This creates meaningful early survival pressure.

### 3.4 Resource System

#### Core Resources
| Resource | Base Price | Source | Used For |
|----------|------------|--------|----------|
| Wood | 3 gold | Forests, trees on Grass tiles | Buildings, roads |
| Stone | 4 gold | Stone terrain, quarries | Roads, buildings |
| Food | 5 gold | Grass/Forest tiles | Population growth |
| Trade Goods | 10 gold | Produced by settlements | Sold for gold via trade |
| Gold | — | Trade income, taxes, caches | Expansion, upgrades, research |

#### Dependencies
- Food → required for population growth
- Population → produces goods
- Goods → generate gold via trade
- Gold → used for expansion and upgrades

#### Resource Storage & Export Backlog
- **Storage:** Infinite (no hard caps)
- **Export Backlog Pressure:** Soft pressure prevents logistics from becoming irrelevant:
  - Each settlement tracks `exportBacklog[resource]` = resources waiting for transport
  - If `backlog > 0`: Effective export price reduced by `backlog * penaltyFactor`
  - Formula: `effectivePrice = basePrice * max(0.5, 1.0 - backlog * 0.01)`
  - Backlog grows when production exceeds caravan transport capacity
  - Backlog shrinks when caravans deliver goods

**Why This Works:**
- Infinite storage prevents frustration from lost resources
- Export backlog creates soft pressure to expand transport (caravans, roads)
- Logistics remains the bottleneck without hard caps
- Player sees backlog growing → knows to invest in transport

**Visual Indicator:** Settlement panel shows backlog status: "Export backlog: 50 wood waiting"

#### Resource Production Formula
- **Base Rate:** Determined by terrain and objects
  - Forest tile: +2 Wood/tick
  - Stone tile: +1 Stone/tick
  - Grass tile: +0.5 Food/tick
- **Specialization Multiplier:**
  - Logging Camp: Wood ×2.0
  - Mining Town: Stone ×2.0
  - Farming Village: Food ×2.0
- **Building Bonuses:** Additive flat bonuses per building type

### 3.5 Road System

#### Road Types
Each road type enables a new transport class. Higher types support all lower tiers.

| Type | Cost | Upkeep | Transport Enabled | Tech Required |
|------|------|--------|-------------------|---------------|
| Dirt Road | Low | None | Pedestrians only | General (from start) |
| Stone Road | Medium | Low | Simple carts | Mining Town branch |
| Cobblestone | High | Medium | Large carts | Mining Town (advanced) |
| Roman Road | Very high | High | Fastest, max cargo | Trade Hub branch |

**Best roads require both Mining Town (materials) and Trade Hub (engineering).**

#### Traffic & Upgrades
- Each road type has traffic capacity
- When traffic exceeds capacity, caravans slow down
- **Solution:** Upgrade road type (pay cost difference, in-place upgrade)
- **Alternative:** Build parallel routes (slightly less efficient)

#### Placement
- Drag to place roads
- Snap-to-grid connections
- Clear placement feedback (ghost image, valid/invalid tint)

### 3.6 Caravan System

- Visible moving units along roads
- Carry goods between settlements
- Frequency increases with settlement size

#### Pathfinding
- **Graph Model:** Settlements = nodes, roads = edges, road tiles = edge weight
- **Algorithm:** Dijkstra (or A*) for shortest path
- **Route Recalculation:** Only when network topology changes (new/removed road or settlement)
- **Congestion:** Ignored for routing (only affects speed)

#### Caravan Spawn Logic
- **Spawn Interval:** Deterministic per route, scaled by combined settlement sizes
- **Formula (flattened):** `interval = baseInterval / sqrt(sizeA + sizeB)`
  - Prevents rapid saturation of 3-caravan cap
  - Example: size 50+200 = 250 → factor ~16 (vs 100x with product formula)
- **Max Caravans:** 3 per route (prevents spam, ensures traffic variety)
- **Spawn Rate Scaling:**
  - Combined settlement size increases spawn frequency
  - Road capacity limits effective throughput

#### Movement & Speed
- **Base Speed:** 1 tile per tick on Dirt Road
- **Speed by Road Type:**
  | Road Type | Speed Multiplier | Capacity (caravans/tile) |
  |-----------|------------------|--------------------------|
  | Dirt Road | 1.0x | 2 |
  | Stone Road | 1.5x | 4 |
  | Cobblestone | 2.0x | 6 |
  | Roman Road | 3.0x | 10 |

#### Congestion System
- Speed reduction when traffic exceeds capacity:
  ```
  speedMultiplier = 1.0 - (traffic - capacity) / capacity * 0.5
  speedMultiplier clamped to [0.2, 1.0]
  ```
- Caravans queue at bottlenecks (visual only, no blocking)
- **Congestion Formula:** Predictable, smooth slowdown based on traffic ratio

#### Non-linear Upkeep
- Caravans cost gold while traveling
- `upkeep = baseCost * days^1.5`
- Days calculated as: `tileCount / speedMultiplier`
- Faster roads reduce travel days → reduce upkeep

#### Relay Settlements
- **Rules (both required):**
  1. Settlement must be Town level or higher
  2. Settlement must have active trade participation (imports OR exports in the last 30 ticks)
- **Effect:** Route splits into segments; each segment incurs separate upkeep
- **Benefit:** Shorter legs = cheaper total upkeep despite extra caravans
- **Anti-exploit:** Prevents building minimal Towns purely as structural relays without economic integration

**Visual Feedback:**
- Busy roads = thriving economy
- Idle roads = inefficiency
- Caravans visibly queue at bottlenecks

### 3.7 Trade System

#### Trade Route Definition
- **One route per settlement pair** (A ↔ B), regardless of direction
- Multiple resources flow per route (direction emerges from supply/demand)
- Route active when path exists through connected road network

#### Trade Route Lifecycle
1. Road connects two settlements → route created
2. TradeSystem evaluates supply/demand at each endpoint
3. Caravans spawn carrying excess resources toward shortages
4. Delivery triggers price adjustment at destination
5. Route persists until connection severed

#### Trade Policies (MVP: Basic Priorities)
- Per settlement, per resource: Import / Export / Neutral
- Policies filter which resources flow (MVP: "export surplus" / "import needed")

#### Dynamic Pricing Formula (Stabilized)
- **Base Price:** Fixed per resource (e.g., Food=5, Wood=3, Stone=4, Goods=10 gold)

- **Price Calculation (per settlement, per resource):**
  ```
  // Use smoothed production rate, not volatile stock
  avgProduction = lerp(prevProduction, currentProduction, 0.1)
  
  // Production vs demand ratio (not stock-based)
  productionPerCapita = avgProduction / population
  demandPerCapita = baseDemand * demandMultiplier
  ratio = productionPerCapita / demandPerCapita
  
  // Smoothed price multiplier prevents oscillation
  priceMultiplier = lerp(prevMultiplier, clamp(ratio, 0.5, 2.0), 0.2)
  currentPrice = basePrice * priceMultiplier
  ```

- **Why Production-Based:**
  - Stock swings from single caravan deliveries no longer cause price spikes
  - Prices reflect sustainable economics, not inventory fluctuations
  - Lerp smoothing (0.1 for production, 0.2 for price) prevents flicker

- **Demand is Autonomous:**
  - `demand = baseDemandPerCapita * population * demandMultiplier`
  - Growing populations increase demand automatically
  - Essential resources (food) have minimum demand floor

- **Supply is Player-Controlled:**
  - Production rate from terrain, buildings, specialization
  - Trade imports add to effective production rate
  - Direct player resource management

- **Price Update:** Every tick (smoothed values update continuously)
- **Clamping:** Prices bounded to [50%, 200%] of base price

**Price Modifiers Display:**
- Show supply/demand modifiers (e.g., "Food shortage +40% price", "Oversupply -25% price")
- Simple ↑/↓ indicators per resource

**Optional Future:** External NPC traders occasionally arrive offering buy/sell at their own prices.

---

## 4. Exploration System

### 4.1 Exploration Rewards (Tiered by Tech Progression)

**Early Game (always visible):**
- **Resource caches** → one-time pickups (wood, stone, food)
- **Terrain variety** → signals where to specialize next

**Mid Game (requires tech):**
- **Fertile Land** → food production bonus
- **Rich Deposits** → increased stone yield

**Late Game (requires advanced tech):**
- **Trade Crossroads** → caravan speed bonus
- **Ancient Ruins** → one-time large rewards

**Reward Mechanics:** Location rewards give flat bonuses independent of settlement type. A Farming Village on Rich Deposits gets food production AND stone bonus. Matching settlement (e.g., Mining Town on Rich Deposits) benefits more naturally since higher base production makes flat bonus more impactful.

### 4.2 Sea Exploration

**Progression:** Water barrier → coastline → shallow exploration → deep sea trade

- **Harbor:** Building for coastal settlements (general tech)
- Does not auto-reveal sea tiles
- Allows exploring sea tiles within radius determined by ship size
- Explored sea tiles must connect back to harbor

**Ship Tiers:**
| Ship | Sea Access | Exploration Radius | Tech Required |
|------|------------|-------------------|----------------|
| Small Ship | Shallow Sea | Short | General tech |
| Large Ship | Deep Sea | Long | Trade Hub branch |

**Sea Trade Routes:** Two coastal settlements with harbors can trade by sea, bypassing land routes.

---

## 5. Progression & Tech Tree

### 5.1 Research System
- **Global research queue** — one active research at a time
- **Fueled by Gold** — functioning trade economy required to advance
- **Gated by specialization** — tech branches only appear with matching settlement type
- **Discovery-based UI** — players see only what they can currently research; locked tech shows hints (e.g., "Requires Mining Town")

### 5.2 General Tech (available to all)
- Bridges (cross water)
- Basic road building
- Exploration improvements
- Harbor construction (requires coastal settlement)
- Small ships (shallow sea exploration and trade)

### 5.3 Specialization Tech Branches

**Logging Camp branch:**
- Forestry upgrades
- Advanced wood buildings
- Tree farming

**Mining Town branch:**
- Deep mining
- Stone processing
- Quarry upgrades

**Farming Village branch:**
- Irrigation
- Crop variety
- Food storage

**Trade Hub branch:**
- Caravan improvements
- Road upgrades (Cobblestone, Roman Road)
- Trade route optimization
- Large ships (deep sea)

---

## 6. Upkeep & Pressure

**Downward pressure through existing mechanics:**
- **Road upkeep** — higher road types cost more to maintain
- **Caravan upkeep** — long routes cost disproportionately more
- **Building maintenance** — buildings require stone/gold
- **Population consumes food** — growth without food causes decline
- **Gold sources** — trade income, settlement taxes, resource caches (multiple sources prevent single-point-of-failure)

**Failure is gradual and visible:**
- Struggling settlements shrink visually
- Caravan traffic thins
- Buildings appear dimmed
- Decline is always reversible — player can redirect resources to recover

---

## 7. Interaction Specification

### 7.1 Controls
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

### 7.2 Build Mode UI
- **Trigger:** Press `B`
- **Menu:** Radial or sidebar showing buildable structures
- **Preview:** Ghost image of structure follows cursor
- **Placement:** Left-click to place, right-click to cancel
- **Invalid Placement:** Red tint on tiles where building is not allowed
- **Road Building:** Drag to place roads, snap-to-grid

### 7.3 Settlement Management
- **Select:** Click on settlement center tile
- **Info Panel:** Shows name, population, specialization, resources, buildings
- **Specialization Choice:** Panel appears when Village → Town upgrade available
- **Build Menu:** Options to add structures within settlement

### 7.4 Visual Feedback

**Settlement Visuals:**
- Growing settlements visually evolve
- **Specialization identity** — each specialization has distinct visual style:
  - Logging Camps have timber structures
  - Mining Towns have stone buildings
  - Farming Villages have agricultural buildings
  - Trade Hubs have market/warehouse aesthetics
- Struggling settlements show subtle visual degradation (dimmed, shrinking)

**Road Visuals:**
- Roads show traffic density
- **Congestion through behavior** — caravans visibly queue and slow at bottlenecks

**Trade Route Visuals:**
- Trade routes visibly animate direction
- **Route identity** — color-coded or labeled routes to distinguish at scale
- **Hover highlight** — hover over a route to highlight it, dims all other routes
  - Prevents color overload when many routes exist
  - Shows route-specific info (caravans, throughput, profit)

### 7.5 Information Overlays
- Highlight trade routes
- Show resource flow
- Simple overlays for efficiency
- **Route cost previews** — show estimated caravan upkeep including expected congestion penalty
  - Preview calculates: `baseUpkeep * distance^1.5 * expectedCongestionMultiplier`
  - Helps player make informed routing decisions without changing actual routing
- **Congestion indicators** — highlight bottleneck roads that need upgrading
- **Export backlog display** — shows resources waiting for transport

---

## 8. UI Specification

### 8.1 HUD Elements
- **Top-Left:** Current resources (Wood, Stone, Food, Gold)
- **Top-Right:** Minimap (toggleable)
- **Bottom:** Build toolbar (when in build mode)
- **Resource indicators:** ↑/↓ per resource with reason breakdown:
  - "Food ↑ Low supply" / "Food ↓ High demand" / "Food ↓ Export backlog"
  - Simple icons with tooltip showing exact cause

### 8.2 Settlement Panel
- Appears when settlement selected
- Displays:
  - Settlement name
  - Population count
  - Specialization (if any)
  - Resource production rates with modifiers
  - Building list with upgrade buttons
  - Trade route status
  - Specialization choice (when upgrading to Town)

### 8.3 Tech Panel
- Shows current research progress
- Displays available tech branches based on settlements
- Locked branches show hints ("Requires Mining Town")
- Research queue (one active at a time)

### 8.4 Visual Style
- **Color Palette:** Earth tones with vibrant accent colors
- **Font:** Monospace for UI (pixel aesthetic)
- **Icons:** 16×16 inline sprites for resources

---

## 9. Technical Architecture

### 9.1 Module Structure
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
│   ├── Settlement.java       - Settlement with specialization
│   ├── SettlementType.java   - Village/Town/City/Metropolis
│   ├── Specialization.java   - Logging/Mining/Farming/Trade
│   ├── Road.java             - Road tile with type
│   ├── RoadType.java         - Dirt/Stone/Cobblestone/Roman
│   ├── Caravan.java          - Moving trade unit
│   ├── TradeRoute.java       - Connection between settlements
│   ├── TechTree.java         - Research system
│   ├── Tech.java             - Individual tech definition
│   ├── ResourceCache.java    - Exploration reward
│   ├── Harbor.java           - Coastal building for sea access
│   ├── Ship.java             - Small/Large ship for sea exploration
│   ├── systems/              - Simulation systems (logic layer)
│   │   ├── SimulationSystem.java      - Tick coordinator
│   │   ├── EconomySystem.java          - Resource production/consumption
│   │   ├── PopulationSystem.java      - Growth and starvation
│   │   ├── TradeSystem.java            - Route management, pricing
│   │   ├── CaravanSystem.java          - Movement, spawning, pathfinding
│   │   ├── UpkeepSystem.java           - Road, caravan, building costs
│   │   └── TechSystem.java              - Research progression
│   ├── event/                - Event system for decoupled communication
│   │   ├── EventBus.java              - Central event dispatcher
│   │   ├── GameEvent.java             - Base event interface
│   │   ├── ResourceChangedEvent.java
│   │   ├── CaravanArrivedEvent.java
│   │   ├── SettlementUpgradedEvent.java
│   │   └── ... (other events)
│   ├── render/
│   │   ├── RenderLayer.java  - Layer interface for rendering
│   │   ├── Renderer.java     - Orchestrates render layers
│   │   ├── TerrainRenderLayer.java
│   │   ├── ObjectRenderLayer.java
│   │   ├── FogOfWarRenderLayer.java
│   │   ├── SettlementRenderLayer.java
│   │   ├── RoadRenderLayer.java
│   │   ├── CaravanRenderLayer.java
│   │   └── UIRenderLayer.java
│   └── mode/
│       ├── GameMode.java     - Interface for game modes
│       ├── ExploreMode.java   - Exploration mode implementation
│       ├── BuildMode.java    - Building placement mode
│       └── ResearchMode.java - Tech tree browsing mode
```

### 9.2 System Architecture

Systems contain simulation logic; entities are data containers. Each system:
1. Updates per tick based on game state
2. Reads from entities, writes results back
3. Emits events for UI/rendering when state changes

```
┌─────────────────────────────────────────────────────────────┐
│                     SimulationSystem                        │
│  (Tick coordinator: 1 Hz fixed update, ordered execution)  │
└─────────────────────────────────────────────────────────────┘
          │           │           │           │
          ▼           ▼           ▼           ▼
   ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
   │ Economy  │ │Population│ │  Trade   │ │ Caravan  │
   │ System   │ │ System   │ │ System   │ │ System   │
   └──────────┘ └──────────┘ └──────────┘ └──────────┘
          │           │           │           │
          ▼           ▼           ▼           ▼
   ┌──────────────────────────────────────────────────────┐
   │                      Entities                        │
   │  Settlement[], Caravan[], Road[], TradeRoute[]       │
   └──────────────────────────────────────────────────────┘
```

**System Responsibilities:**

| System | Reads | Writes |
|--------|-------|--------|
| EconomySystem | Settlements, terrain | Settlement.productionRates |
| PopulationSystem | Settlements, food | Settlement.population |
| TradeSystem | Settlements, routes | TradeRoute.priceModifiers |
| CaravanSystem | Settlements, routes, roads | Caravan.position, Caravan.cargo |
| UpkeepSystem | Roads, caravans, settlements | Settlement.gold |
| TechSystem | Settlements, gold | TechTree.researchProgress |

#### Event System

Systems emit events for UI/rendering; subscribers react without tight coupling.

**Core Events:**
| Event | Payload | Use Case |
|-------|---------|----------|
| `ResourceChangedEvent` | settlement, resource, delta | Update HUD, settlement panel |
| `CaravanArrivedEvent` | caravan, settlement, cargo | Trigger deliveries, animations |
| `CaravanSpawnedEvent` | caravan, route | Route visualization |
| `SettlementUpgradedEvent` | settlement, oldLevel, newLevel | UI notification, specialization choice |
| `PopulationChangedEvent` | settlement, oldPop, newPop | Visual updates |
| `PriceChangedEvent` | settlement, resource, oldPrice, newPrice | Price indicators |
| `TradeCompletedEvent` | route, gold | Income notifications |
| `ResearchCompletedEvent` | tech | Unlock notifications |
| `ExportBacklogChangedEvent` | settlement, resource, backlog | Backlog warnings |

**Event Bus:** Singleton `EventBus` instance. Systems register listeners; events dispatched synchronously after tick updates.

### 9.3 Extension Points

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
3. Modes can be switched at runtime (explore, build, research, etc.)

### 9.4 Data Persistence

**Persisted (Essential State):**
- Explored chunks (fog of war state)
- Settlement data (position, population, specialization, buildings, gold)
- Roads (position, type)
- Active caravans (position, cargo, route)
- Tech tree (researched tech, current research progress)
- Resource caches (collected/uncollected state)

**Derived (Recomputed on Load):**
- Trade routes (rebuilt from settlement/road graph)
- Pricing (recalculated from supply/demand)
- Pathfinding routes (recomputed when needed)

**Save Format:** JSON for human readability and future extensibility

**Save Location:** `Gdx.files.local()` directory
- Linux: `~/.local/share/simple-sim-game/data/`
- Windows: `%APPDATA%/simple-sim-game/data/`
- macOS: `~/Library/Application Support/simple-sim-game/data/`

---

## 10. Numerical Definitions

These formulas determine pacing, balance, and player experience. Test and iterate early.

### 10.1 Core Formulas

| Formula | Expression | Description |
|---------|------------|-------------|
| Population Growth | `growth = surplus * 0.01 * levelMult` | 1% of food surplus per tick |
| Population Starvation | `starve = deficit * 0.02` | 2% of food deficit per tick, capped |
| Food Consumption | `consumed = population * 0.15` | Per tick (tuned for slower growth) |
| Caravan Upkeep | `upkeep = baseCost * days^1.5` | Non-linear distance cost |
| Caravan Speed | `speed = baseSpeed * roadMultiplier` | Tiles per tick |
| Traffic Slowdown | `slowdown = clamp(1 - (traffic-cap)/cap * 0.5, 0.2, 1.0)` | Based on traffic ratio |
| Caravan Spawn | `interval = baseInterval / sqrt(sizeA + sizeB)` | Flattened scaling |
| Export Backlog Penalty | `priceMult = max(0.5, 1.0 - backlog * 0.01)` | Soft transport pressure |
| Price Smoothing | `smoothed = lerp(prev, target, 0.1-0.2)` | Prevents price oscillation |
| Trade Hub Bonus | `bonus = baseBonus * sqrt(connections)` | Diminishing returns |

### 10.2 Test Values (Tunable)

| Parameter | Value | Notes |
|-----------|-------|-------|
| Tick Rate | 1 Hz | 1 tick per second |
| Food Production | 0.5/tile | Grass produces food |
| Food Consumption | 0.15/pop/tick | Slower than before |
| Growth Rate | 0.01 | Fraction of surplus per tick |
| Starvation Rate | 0.02 | Fraction of deficit per tick |
| Caravan Base Speed | 1 tile/tick | On dirt road |
| Caravan Base Upkeep | 1 gold | Per day (tick) |
| Price Smoothing | 0.1-0.2 | Lerp factor for production and price |
| Export Backlog Penalty | 0.01 | Price reduction per backlog unit |
| Trade Hub Connections | sqrt(n) | Diminishing returns scaling |

### 10.3 Road Properties

| Type | Speed Mult | Capacity | Upkeep (gold/tick) |
|------|------------|----------|-------------------|
| Dirt | 1.0x | 2 | 0 |
| Stone | 1.5x | 4 | 0.01 |
| Cobblestone | 2.0x | 6 | 0.02 |
| Roman | 3.0x | 10 | 0.05 |

### 10.4 Settlement Level Thresholds

| Level | Population | Growth Mult |
|-------|------------|-------------|
| Village | 1–50 | 1.0x |
| Town | 51–200 | 0.8x |
| City | 201–500 | 0.6x |
| Metropolis | 500+ | 0.4x |

### 10.5 Specialization Production Multipliers

| Type | Wood | Stone | Food | Trade Goods |
|------|------|-------|------|-------------|
| Logging Camp | 2.0x | 0.5x | 0.5x | 1.0x |
| Mining Town | 0.5x | 2.0x | 0.5x | 1.0x |
| Farming Village | 0.5x | 0.5x | 2.0x | 1.0x |
| Trade Hub | 1.0x | 1.0x | 1.0x | 1.5x |
| None (Village) | 1.0x | 1.0x | 1.0x | 1.0x |

---

## 11. Implementation Phases

### Phase 1: Foundation (Complete)
- [x] Tile rendering with texture atlas
- [x] Perlin noise terrain generation
- [x] Chunk-based world loading
- [x] Fog of war system
- [x] Camera pan/zoom controls

### Phase 2: Settlement Building (Complete)
- [x] Settlement placement system
- [x] Settlement selection and info panel
- [x] Building placement mechanics
- [x] Population system
- [x] Auto-placed starter settlement

### Phase 3: Settlement Specialization (Complete)
- [x] Specialization choice at Town upgrade
- [x] Production modifiers per specialization
- [x] Re-specialization mechanic
- [x] Visual identity per specialization type

### Phase 4: Road Network
- [ ] Road tile rendering (multiple types)
- [ ] Road placement tool with drag
- [ ] Road type upgrades (in-place)
- [ ] Path connectivity detection
- [ ] Traffic capacity visualization
- [ ] Road upkeep system

### Phase 5: Trade & Economy
- [ ] Resource gathering from tiles
- [ ] Resource storage per settlement
- [ ] Trade route establishment
- [ ] Dynamic pricing with autonomous demand
- [ ] Caravan movement with non-linear upkeep
- [ ] Trade policies (import/export priorities)
- [ ] Caravan animation
- [ ] Export backlog pressure system

### Phase 6: Exploration Rewards
- [ ] Resource cache spawning
- [ ] Terrain-based bonus tiles
- [ ] Tech-gated reward visibility
- [ ] Reward collection UI

### Phase 7: Tech Tree
- [ ] Research queue system
- [ ] General tech branch
- [ ] Specialization tech branches
- [ ] Discovery-based UI with hints

### Phase 8: Sea Exploration
- [ ] Harbor building
- [ ] Small ship and exploration
- [ ] Large ship and deep sea
- [ ] Sea trade routes

### Phase 9: Polish
- [ ] Visual feedback (congestion, struggling settlements)
- [ ] Route cost previews with congestion
- [ ] Minimap
- [ ] Save/Load system
- [ ] Event system implementation
- [ ] Sound effects
- [ ] Particle effects
- [ ] First trade moment time slowdown
- [ ] Route hover highlight

---

## 12. Game Balance

### 12.1 Starting Conditions
- Auto-placed starter settlement (one hut on suitable terrain)
- Player begins exploring outward to find resource caches and next settlement location
- Real game begins with second settlement and first trade connection

### 12.2 Progression Curve
- **Early:** 2–3 core resources, basic roads
- **Mid:** Dynamic pricing, upkeep systems introduced
- **Late:** All specializations, advanced roads, sea exploration

### 12.3 Win Condition
Sandbox mode — no forced win. Optional player goals:
- Connect multiple biomes
- Build highly efficient trade loops
- Reach population milestones
- Complete procedural contracts (e.g., "Deliver 200 wood to distant settlement")

---

## 13. Glossary

| Term | Definition |
|------|------------|
| Chunk | 16×16 tile grid unit loaded/unloaded as camera moves |
| Fog of War | Visibility state; undiscovered tiles are hidden |
| Settlement | Player-established settlement with population and buildings |
| Specialization | Settlement type (Logging Camp, Mining Town, etc.) determining production focus |
| Road | Improved path tile enabling trade routes |
| Trade Route | Connection between two settlements via roads |
| Caravan | Traveling unit that moves goods between settlements |
| Tech Tree | Research system unlocking new capabilities |
| Relay Settlement | Settlement breaking long trade routes into cheaper short legs |
| Resource Cache | One-time exploration reward pickup |
| Export Backlog | Resources waiting for caravan transport; causes price penalty |
| Lerp | Linear interpolation: `lerp(a, b, t) = a + (b - a) * t` |
| Tick | 1-second simulation step; all systems update once per tick |
| Fixed-point | Integer representation for precise calculations (avoid float drift) |

---

## 14. Minimal Viable Feature Set (MVP)

### Must Have
- Auto-placed starter settlement
- Settlement placement + evolving specialization
- Resource production (wood, stone, food, gold)
- Road building (dirt roads at minimum)
- Caravan movement with non-linear upkeep
- Dynamic pricing (autonomous demand)
- Basic tech tree (general + one specialization branch)

### Nice to Have
- All four road types
- Full specialization tech branches
- Exploration rewards (tiered by tech)
- Re-specialization mechanic
- Trade policies (import/export priorities)

### Defer
- Sea exploration (harbor, ships, sea trade routes)
- NPC traders
- NPC settlements
- Seasons, events
- Advanced UI overlays
