# Simple Sim Game – Game Design Document

## 1. Game Overview

**Genre:** Top-down 2D settlement-building & logistics simulation
**Engine:** libGDX (LWJGL3 desktop backend)

**Core Loop:**
Start with a settlement → Explore outward → Discover resources and strategic locations → Found specialized settlements → Build road networks → Establish trade routes → Research new tech → Expand network

**Early Game:**
The player begins with a single settlement (one hut) auto-placed on a suitable location (grass terrain, large enough area, adjacent to multiple terrain types). The settlement produces resources slowly. The player explores outward to find resource caches (one-time pickups) and promising locations for their next settlement. The real game begins when the second settlement is founded and trade becomes possible.

**Gradual complexity:** Early gameplay uses only 2–3 core resources. Dynamic pricing, upkeep systems, and specialization depth are introduced progressively as the player founds more settlements.

**Key moment — first trade connection:** When the second settlement connects via road, the first caravan visibly spawns, gold income begins, and the UI highlights trade activation. This is the "aha" moment where the core loop clicks.

**Design Pillars:**

* Simplicity over complexity
* Meaningful decisions through constraints
* Visible, living systems
* Relaxed but engaging gameplay

---

## 2. Core Mechanics

### 2.1 Meaningful Decisions

#### Settlement Specialization

Settlements start generic and specialize as they grow:

* **Villages** are unspecialized — they produce a bit of everything based on terrain
* **At the Village → Town upgrade**, the player must choose a specialization:
  * **Logging Camp** → High wood production
  * **Mining Town** → High stone production
  * **Farming Village** → High food production
  * **Trade Hub** → Boosts trade income and caravan speed; gains bonus per connected settlement (diminishing returns — e.g., square root scaling)

Each specialization:

* Grants production bonuses and unlocks a tech branch
* Limits other production types

**Changing specialization:** A settlement can re-specialize, but it drops back one level (e.g., Town → Village) and must upgrade again to lock in the new specialization. This cost scales naturally — changing a City hurts more than changing a Town.

➡️ Players can experiment before committing, but pivoting later has real cost

---

### 2.2 Distance & Efficiency

* Caravans cost upkeep (gold) while traveling
* **Upkeep is non-linear** — longer trips cost disproportionately more (e.g., `cost = baseCost * days^1.5`). Short routes are cheap, long routes get expensive fast.
* Faster roads reduce travel days, which reduces upkeep — giving road upgrades a second purpose beyond traffic capacity
* Relay settlements break long routes into cheaper short legs
* **Anti-spam:** settlements require a minimum viable population before they can serve as trade relay points, preventing empty waypoint settlements

➡️ Promotes smart network layout and road investment instead of brute expansion

---

### 2.3 Resource Flow System

#### Core Resources

* Wood
* Stone
* Food
* Trade Goods
* Gold (currency)

#### Dependencies

* Food → required for population growth
* Population → produces goods
* Goods → generate gold via trade
* Gold → used for expansion and upgrades

➡️ Creates a simple but deep economic loop

---

## 3. World & Exploration

### 3.1 Terrain Influence

Terrain now affects gameplay:

| Terrain | Effect                            |
| ------- | --------------------------------- |
| Forest  | High wood, harder road building   |
| Stone   | High stone                        |
| Grass   | Balanced, good for food           |
| Snow    | Slow growth, rare bonuses         |
| Shallow Sea | Coastal waters, explorable with harbor + ships |
| Deep Sea    | Open ocean, requires large ships               |

---

### 3.2 Exploration Rewards

Rewards are tiered by tech progression — early exploration reveals basic rewards, advanced rewards only become visible once the relevant tech is researched. This encourages re-exploration of previously revealed areas as the player advances.

**Early game (always visible):**

* **Resource caches** → one-time pickups (wood, stone, food) to bootstrap the economy
* **Terrain variety** → signals where to specialize next

**Mid game (requires tech):**

* **Fertile Land** → food production bonus
* **Rich Deposits** → increased stone yield

**Late game (requires advanced tech):**

* **Trade Crossroads** → caravan speed bonus
* **Ancient Ruins** → one-time large rewards

**Rewards and specialization:** Location rewards give flat bonuses independent of the settlement built there. A Farming Village on Rich Deposits gets food production AND a stone bonus. However, a matching settlement (e.g., Mining Town on Rich Deposits) benefits more naturally since its higher base production makes the flat bonus more impactful. This makes scouting locations before choosing specialization a meaningful part of the game.

➡️ Exploration stays relevant throughout the game, not just at the start

### 3.3 Sea Exploration

Sea exploration has its own progression, turning water from a barrier into a strategic frontier.

**From land:** Only 1 adjacent sea tile can be revealed (coastline visibility).

**Harbor:** A building for coastal settlements (general tech). Does not auto-reveal sea tiles, but allows the player to explore sea tiles within a radius determined by ship size. Explored sea tiles must connect back to the harbor.

**Ship tiers:**

| Ship        | Sea Access    | Exploration Radius | Tech Required    |
| ----------- | ------------- | ------------------ | ---------------- |
| Small Ship  | Shallow Sea   | Short              | General tech     |
| Large Ship  | Deep Sea      | Long               | Trade Hub branch |

**Sea trade routes:** Two coastal settlements with harbors can trade by sea, bypassing land routes. This makes coastal locations strategically valuable.

➡️ Water becomes a progression system — from barrier to coastline to shallow exploration to deep sea trade

---

## 4. Settlement System

### 4.1 Growth & Requirements

| Level      | Population | Unlocks            |
| ---------- | ---------- | ------------------ |
| Village    | 1–50       | Basic production   |
| Town       | 51–200     | Road building      |
| City       | 201–500    | Trade optimization |
| Metropolis | 500+       | Advanced upgrades  |

### 4.2 Upkeep

See §7 for full upkeep details (food consumption, building maintenance, road and caravan costs).

---

## 5. Road & Transport System

### 5.1 Road Types

Each road type enables a new transport class. Higher road types also support all lower transport tiers.

| Type        | Cost      | Upkeep | Transport Enabled   | Tech Required          |
| ----------- | --------- | ------ | ------------------- | ---------------------- |
| Dirt Road   | Low       | None   | Pedestrians only    | General (from start)   |
| Stone Road  | Medium    | Low    | Simple carts        | Mining Town branch     |
| Cobblestone | High      | Medium | Large carts         | Mining Town (advanced) |
| Roman Road  | Very high | High   | Fastest, max cargo  | Trade Hub branch       |

➡️ Best roads require both Mining Town (materials/knowledge) and Trade Hub (engineering), tying specializations together

---

### 5.2 Traffic & Upgrades

* Each road type has a traffic capacity
* When traffic exceeds capacity, caravans slow down
* **Primary solution:** upgrade the road type
* **Alternative:** build parallel routes (slightly less efficient than upgrading, but allows spatial creativity)
* **In-place upgrade:** click an existing road, pay the cost difference, it becomes the next tier — no tear-down required

➡️ Traffic pressure drives road progression naturally without frustrating the player

---

### 5.3 Caravan System

* Visible moving units
* Carry goods between settlements
* Frequency increases with settlement size

Visual feedback:

* Busy roads = thriving economy
* Idle roads = inefficiency

---

## 6. Trade System

### 6.1 Trade Routes

* Automatically created when settlements connect
* Require supply & demand to function
* **Simple trade policies** per settlement: import/export priorities (e.g., "prefer food imports", "export surplus only") — keeps automation while giving the player agency

### 6.2 Dynamic Pricing

* Each settlement has local resource prices driven by supply and demand
* **Demand is autonomous** — settlements develop needs based on population size and growth (e.g., a growing town demands more food whether the player planned for it or not)
* **Supply is player-controlled** — production and trade routes are the player's levers
* High supply → low price, low supply → high price
* **Demand floor:** Essential resources (food, basic goods) always have a minimum demand level, preventing economic stagnation from demand collapse

**Optional:** External NPC traders occasionally arrive offering to buy/sell at their own prices, adding market variety.

**Future:** NPC settlements that trade independently, creating competition and a living economy.

**Visibility:** Display supply/demand modifiers clearly (e.g., "Food shortage +40% price", "Oversupply -25% price") with simple ↑/↓ indicators per resource.

➡️ The player controls production but not demand — pricing reflects real internal pressure

---

## 7. Upkeep & Pressure

The game applies gentle downward pressure through existing mechanics:

* **Road upkeep** — higher road types cost more to maintain (see §5.1)
* **Caravan upkeep** — long routes cost disproportionately more (see §2.2)
* **Building maintenance** — buildings require stone/gold to maintain
* **Population consumes food** — growth without food supply causes decline
* **Gold sources** — trade income, settlement taxes, resource caches. Multiple sources prevent a single-point-of-failure death spiral.

**Failure is gradual and visible:** Struggling settlements shrink visually, caravan traffic thins, buildings appear dimmed. Decline is always reversible — the player can redirect resources to recover.

➡️ Expansion has ongoing costs, preventing unchecked snowballing

---

## 8. Progression & Tech Tree

### 8.1 Research System

* **Global research queue** — one active research at a time
* **Fueled by Gold** — a functioning trade economy is required to advance
* **Gated by specialization** — tech branches only appear once you have the matching settlement type
* **Discovery-based UI** — players see only what they can currently research, not the full tree. Locked tech shows hints (e.g., "Requires Mining Town") so the player can plan ahead without being overwhelmed.

### 8.2 General Tech (available to all)

* Bridges (cross water)
* Basic road building
* Exploration improvements
* Harbor construction (requires coastal settlement)
* Small ships (shallow sea exploration and trade)

### 8.3 Specialization Tech (requires matching settlement)

* **Logging Camp branch** → forestry upgrades, advanced wood buildings, tree farming
* **Mining Town branch** → deep mining, stone processing, quarry upgrades
* **Farming Village branch** → irrigation, crop variety, food storage
* **Trade Hub branch** → caravan improvements, road upgrades, trade route optimization, large ships (deep sea)

➡️ The player needs multiple specialized settlements to access the full tech tree, directly driving expansion

---

## 9. Player Goals (Optional)

* Connect multiple biomes
* Build highly efficient trade loops
* Reach population milestones
* Complete procedural contracts

Examples:

* Deliver 200 wood to a distant settlement
* Maintain a trade route for 10 minutes

---

## 10. UX & Feel Improvements

### 10.1 Building

* Drag to place roads
* Snap-to-grid connections
* Clear placement feedback

### 10.2 Visual Feedback

* Growing settlements visually evolve
* **Specialization identity** — each specialization has a distinct visual style (e.g., Logging Camps have timber structures, Mining Towns have stone buildings)
* Roads show traffic density
* **Congestion through behavior** — caravans visibly queue and slow at bottlenecks
* Trade routes visibly animate direction
* **Route identity** — color-coded or labeled routes to distinguish them at scale
* Struggling settlements show subtle visual degradation (dimmed, shrinking)

### 10.3 Information Clarity

* Highlight trade routes
* Show resource flow
* Simple overlays for efficiency
* **Route cost previews** — show estimated caravan upkeep before building a route
* **Congestion indicators** — highlight bottleneck roads that need upgrading

---

## 11. Minimal Viable Feature Set (MVP)

To keep scope realistic:

**Must Have:**

* Auto-placed starter settlement
* Settlement placement + evolving specialization
* Resource production (wood, stone, food, gold)
* Road building (dirt roads at minimum)
* Caravan movement with non-linear upkeep
* Dynamic pricing (autonomous demand)
* Basic tech tree (general + one specialization branch)

**Nice to Have:**

* All four road types
* Full specialization tech branches
* Exploration rewards (tiered by tech)
* Re-specialization mechanic
* Trade policies (import/export priorities)

**Defer:**

* Sea exploration (harbor, ships, sea trade routes)
* NPC traders
* NPC settlements
* Seasons, events
* Advanced UI overlays

---

## 11. Technology Tree Expansion

### 11.1 Overview
The tech tree provides meaningful progression with choices that affect gameplay strategy. All tech tree data is defined in application.yml, allowing content to be added without code changes.

### 11.2 Configuration (application.yml)
```yaml
tech_tree:
  research_gold_per_tick: 2.0
  initially_available:
    roads: [DIRT]
    buildings: [HOUSE_SIMPLE, FARM_SMALL, MARKET_SMALL, WAREHOUSE, WELL_WATER]
  techs:
    BASIC_ROADS:
      name: Basic Roads
      branch: GENERAL
      cost: 50.0
      required_tech: ""
      required_level: VILLAGE
      allow:
        roads: [STONE]
```

### 11.3 Expansion Elements
- **Tier 4-5 Tech:** Advanced upgrades per specialization (configurable in application.yml)
- **Cross-Specialization Tech:** Requires multiple settlement types working together
- **Conditional Tech:** Unlocks based on game state (settlements count, resources stored, population)
- **Tech Categories:** GENERAL, LOGGING_CAMP, MINING_TOWN, FARMING_VILLAGE, TRADE_HUB, CROSS_SPECIALIZATION

### 11.4 Cross-Specialization Example
Some powerful techs require cooperation between settlement types:
- **Industrial Logging:** Requires Logging Camp + Trade Hub → Unlocks advanced wood production
- **Mass Mining:** Requires Mining Town + Trade Hub → Unlocks advanced stone production
- **Agricultural Empire:** Requires Farming Village + Trade Hub → Unlocks food export tech

### 11.5 Conditional Tech Example
Techs that unlock based on game achievements:
- **City Planning:** Unlocks when player has 3+ settlements
- **Empire:** Unlocks when total population reaches 500
- **Trade Mastery:** Unlocks when player has 5+ active trade routes

---

## 12. Building Upgrade System

### 12.1 Overview
Buildings can be upgraded for increased benefits, providing progression within settlements. All building data is defined in application.yml.

### 12.2 Configuration (application.yml)
```yaml
buildings:
  HOUSE_SIMPLE:
    cost: 10
    population_capacity: 4
    image: 64x64/objects/house-simple.png
    description: A modest wooden dwelling.
    tier: 1
    upgrade_from: null
  HOUSE_LARGE:
    cost: 15
    population_capacity: 8
    image: 64x64/objects/house-large.png
    description: A spacious home housing a larger family.
    tier: 2
    upgrade_from: HOUSE_SIMPLE
    upgrade_cost: 8
```

### 12.3 Upgrade Mechanics
- **Tier System:** Buildings have tier 1, 2, 3 (defined in config)
- **Upgrade Path:** Each building specifies upgrade_from for the chain
- **Cost:** Upgrades cost less than building from scratch
- **Requirements:** Some upgrades require settlement level (TOWN, CITY, etc.)
- **Visual:** Sprite changes based on tier (configurable via image path)

### 12.4 Production Bonuses
Upgrade tiers can provide production bonuses:
```yaml
FARM_LARGE:
  tier: 2
  upgrade_from: FARM_SMALL
  upgrade_cost: 15
  production_bonus:
    FOOD: 2.0
FARM_PLANTATION:
  tier: 3
  upgrade_from: FARM_LARGE
  upgrade_cost: 30
  required_level: TOWN
  production_bonus:
    FOOD: 5.0
```

---

## 13. Dynamic Events System

### 13.1 Overview
Random events add unpredictability and memorable moments to gameplay. All events are defined in application.yml.

### 13.2 Configuration (application.yml)
```yaml
events:
  global_settings:
    min_ticks_between_events: 50
    max_concurrent_events: 3
    notification_duration: 5
  
  categories:
    - id: NATURAL
      weight: 1.0
    - id: ECONOMIC
      weight: 0.8
    - id: SOCIAL
      weight: 0.6
    - id: DANGER
      weight: 0.4
  
  event_templates:
    - id: DROUGHT
      name: Drought
      category: NATURAL
      weight: 0.3
      duration: 30
      effects:
        GRASS_FOOD_MULTIPLIER: 0.5
      notification: "A drought affects {settlement}! Food production halved."
      
    - id: BOUNTIFUL_HARVEST
      name: Bountiful Harvest
      category: NATURAL
      weight: 0.4
      duration: 20
      effects:
        GRASS_FOOD_MULTIPLIER: 2.0
      notification: "{settlement} enjoys a bountiful harvest!"
      
    - id: TRADE_BOOM
      name: Trade Boom
      category: ECONOMIC
      weight: 0.3
      duration: 30
      effects:
        TRADE_INCOME_MULTIPLIER: 1.5
      notification: "Trade is booming! Merchant activity increased."
      
    - id: LOCAL_FESTIVAL
      name: Local Festival
      category: SOCIAL
      weight: 0.2
      duration: 15
      choice: true
      options:
        - name: Host Festival
          cost: 50
          effects:
            GROWTH_RATE_MULTIPLIER: 1.5
        - name: Skip
          cost: 0
          effects: {}
      notification: "{settlement} wants to host a festival."
```

### 13.3 Event Properties
- **Category:** NATURAL, ECONOMIC, SOCIAL, DANGER (each with weight for frequency)
- **Weight:** Probability of this event type being selected
- **Duration:** How long effects last (temporary events)
- **Effects:** Modifiers to game values (production multipliers, resource changes)
- **Choice:** Whether player can decide (optional events)
- **Scope:** SINGLE_SETTLEMENT, REGION, GLOBAL

### 13.4 Effect Types
| Effect | Description | Example |
|--------|-------------|---------|
| RESOURCE_MULTIPLIER | Multiply production | GRASS_FOOD_MULTIPLIER: 0.5 |
| RESOURCE_BONUS | Add flat production | WOOD_BONUS: 2.0 |
| GROWTH_MODIFIER | Adjust growth rate | GROWTH_RATE_MULTIPLIER: 1.5 |
| PRICE_MODIFIER | Adjust prices | PRICE_FLOOR: 0.3 |
| POPULATION_BONUS | Instant population change | POPULATION: +10 |

### 13.5 Event System Flow
1. Event timer reaches threshold
2. Select category based on weights
3. Select event from category based on weight
4. Apply to eligible targets (settlement, region, global)
5. Show notification to player
6. Apply effects for duration
7. Remove effects and show recovery notification

---

## 14. Design Summary

This design focuses on:

* Fewer systems, but stronger interactions
* Strategic choices without complexity
* Visible, satisfying simulation
* Configurable content via application.yml

The result is a game that is:

* Easy to learn
* Hard to optimize
* Relaxing but engaging
* Extensible through configuration
