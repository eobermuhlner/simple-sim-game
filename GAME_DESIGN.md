# Simple Sim Game – Game Design Document

## 1. Game Overview

**Genre:** Top-down 2D settlement-building & logistics simulation
**Engine:** libGDX (LWJGL3 desktop backend)

**Core Loop:**
Explore → Discover strategic locations → Found specialized settlements → Build efficient road networks → Establish trade routes → Optimize economy → Expand network

**Design Pillars:**

* Simplicity over complexity
* Meaningful decisions through constraints
* Visible, living systems
* Relaxed but engaging gameplay

---

## 2. Key Improvements

### 2.1 Meaningful Decisions

#### Settlement Specialization

Each settlement must choose a specialization:

* **Logging Camp** → High wood production
* **Mining Town** → High stone production
* **Farming Village** → High food production
* **Trade Hub** → Boosts trade income and caravan speed

Each specialization:

* Grants bonuses
* Limits other production types

➡️ Encourages interdependent settlements

---

### 2.2 Distance & Efficiency

* Caravan speed decreases with distance
* Long routes reduce efficiency (time cost)
* Optional: small resource loss over long distances

➡️ Promotes smart network layout instead of brute expansion

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

Exploration reveals more than terrain:

* **Fertile Land** → food bonus
* **Rich Deposits** → increased stone yield
* **Trade Crossroads** → caravan speed bonus
* **Ancient Ruins** → one-time rewards

➡️ Exploration becomes strategic, not cosmetic

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

* Population consumes food
* Buildings require maintenance (stone/gold)

➡️ Prevents unchecked growth

---

## 5. Road & Transport System

### 5.1 Road Types

| Type       | Cost   | Speed  |
| ---------- | ------ | ------ |
| Dirt Road  | Low    | Slow   |
| Stone Road | Medium | Faster |
| Highway    | High   | Fast   |

---

### 5.2 Traffic System (Simple)

* Each road tile has limited capacity
* Too many caravans → slowdown

➡️ Encourages better layouts and multiple routes

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

* Resources have local prices
* High supply → low price
* Low supply → high price

➡️ Encourages intelligent routing and specialization

---

## 7. Light Pressure Systems

### 7.1 Maintenance

* Roads require upkeep (stone/gold)
* Large networks become expensive

### 7.2 Seasonal Variation (Optional)

* Winter reduces food production
* Encourages storage and planning

### 7.3 Events (Rare, Non-Punishing)

* Caravan delay
* Resource boom
* Minor road damage

---

## 8. Progression & Unlocks

Progression unlocks new mechanics instead of just scaling:

* Bridges (cross water)
* Road upgrades
* Caravan improvements
* Advanced buildings

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

* Settlement placement + specialization
* Resource production (3–4 resources)
* Road building + connection detection
* Caravan movement
* Basic trade system

**Nice to Have:**

* Dynamic pricing
* Road upgrades
* Exploration rewards

**Defer:**

* Seasons
* Events
* Advanced UI overlays

---

## 12. Design Summary

This improved design focuses on:

* Fewer systems, but stronger interactions
* Strategic choices without complexity
* Visible, satisfying simulation

The result is a game that is:

* Easy to learn
* Hard to optimize
* Relaxing but engaging
