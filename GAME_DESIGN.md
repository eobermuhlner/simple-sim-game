# Simple Sim Game – Game Design Document

## 1. Game Overview

**Genre:** Top-down 2D settlement-building & logistics simulation
**Engine:** libGDX (LWJGL3 desktop backend)

**Core Loop:**
Start with a settlement → Explore outward → Discover resources and strategic locations → Found specialized settlements → Build road networks → Establish trade routes → Research new tech → Expand network

**Early Game:**
The player begins with a single settlement (one hut) auto-placed on a suitable location (grass terrain, large enough area, adjacent to multiple terrain types). The settlement produces resources slowly. The player explores outward to find resource caches (one-time pickups) and promising locations for their next settlement. The real game begins when the second settlement is founded and trade becomes possible.

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
  * **Trade Hub** → Boosts trade income and caravan speed

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
| Water   | Impassable (bridges unlock later) |

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
* Congestion is a signal to upgrade the road type, not to build parallel routes
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

### 6.2 Dynamic Pricing

* Each settlement has local resource prices driven by supply and demand
* **Demand is autonomous** — settlements develop needs based on population size and growth (e.g., a growing town demands more food whether the player planned for it or not)
* **Supply is player-controlled** — production and trade routes are the player's levers
* High supply → low price, low supply → high price

**Optional:** External NPC traders occasionally arrive offering to buy/sell at their own prices, adding market variety.

**Future:** NPC settlements that trade independently, creating competition and a living economy.

➡️ The player controls production but not demand — pricing reflects real internal pressure

---

## 7. Upkeep & Pressure

The game applies gentle downward pressure through existing mechanics:

* **Road upkeep** — higher road types cost more to maintain (see §5.1)
* **Caravan upkeep** — long routes cost disproportionately more (see §2.2)
* **Building maintenance** — buildings require stone/gold to maintain
* **Population consumes food** — growth without food supply causes decline

➡️ Expansion has ongoing costs, preventing unchecked snowballing

---

## 8. Progression & Tech Tree

### 8.1 Research System

* **Global research queue** — one active research at a time
* **Fueled by Gold** — a functioning trade economy is required to advance
* **Gated by specialization** — tech branches only appear once you have the matching settlement type
* **Discovery-based UI** — players see only what they can currently research, not the full tree

### 8.2 General Tech (available to all)

* Bridges (cross water)
* Basic road building
* Exploration improvements

### 8.3 Specialization Tech (requires matching settlement)

* **Logging Camp branch** → forestry upgrades, advanced wood buildings, tree farming
* **Mining Town branch** → deep mining, stone processing, quarry upgrades
* **Farming Village branch** → irrigation, crop variety, food storage
* **Trade Hub branch** → caravan improvements, road upgrades, trade route optimization

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
* Roads show traffic density
* Trade routes visibly animate direction

### 10.3 Information Clarity

* Highlight trade routes
* Show resource flow
* Simple overlays for efficiency

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

**Defer:**

* NPC traders
* NPC settlements
* Seasons, events
* Advanced UI overlays

---

## 12. Design Summary

This design focuses on:

* Fewer systems, but stronger interactions
* Strategic choices without complexity
* Visible, satisfying simulation

The result is a game that is:

* Easy to learn
* Hard to optimize
* Relaxing but engaging
