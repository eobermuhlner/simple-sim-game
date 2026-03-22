package ch.obermuhlner.sim.game.systems;

import ch.obermuhlner.sim.game.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Fixed-tick simulation coordinator (1 Hz).
 * Update order: Resources → Population → Pricing → Trade Routes → Caravans
 */
public class SimulationSystem {
    private static final int SCAN_RADIUS = 3;
    private static final float FOOD_DEMAND_PER_POP = 0.15f;
    private static final float GROWTH_RATE = 0.01f;
    private static final float STARVATION_RATE = 0.02f;
    private static final float PRICE_LERP_ALPHA = 0.2f;
    private static final float SMOOTHING_ALPHA = 0.15f;
    private static final float CARAVAN_BASE_SPEED = 0.5f;  // tiles per second on dirt road
    private static final float CARGO_THRESHOLD = 0.2f;     // send cargo when stock > 20% of capacity
    private static final float CARGO_BATCH = 0.3f;         // send up to 30% of capacity per trip

    private final World world;
    private final Random random = new Random();

    // Spawn timer per trade route id
    private final java.util.Map<Integer, Float> spawnTimers = new java.util.HashMap<>();

    private long tickCount = 0;

    public SimulationSystem(World world) {
        this.world = world;
    }

    public long getTickCount() { return tickCount; }

    /**
     * 1 Hz simulation tick: economy, population, pricing, route discovery, caravan spawning.
     * Caravan movement is NOT updated here — call updateCaravans() every render frame instead.
     */
    public void tick(float deltaTime) {
        List<Settlement> settlements = world.getSettlements();
        gatherResources(settlements);
        consumeAndGrow(settlements);
        updatePrices(settlements);
        if (world.routesDirty || tickCount % 10 == 0) {
            rebuildTradeRoutes(settlements);
            world.routesDirty = false;
        }
        tickSpawnTimers(deltaTime, settlements);
        tickCount++;
    }

    /**
     * Called every render frame (60 fps) to smoothly advance caravan positions.
     */
    public void updateCaravans(float deltaTime) {
        tickCaravans(deltaTime);
    }

    // ---- Resource Production ----

    private void gatherResources(List<Settlement> settlements) {
        for (Settlement s : settlements) {
            float rawWood = 0, rawStone = 0, rawFood = 0;

            for (int dy = -SCAN_RADIUS; dy <= SCAN_RADIUS; dy++) {
                for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++) {
                    if (dx * dx + dy * dy > SCAN_RADIUS * SCAN_RADIUS) continue;
                    TerrainType terrain = world.getTerrain(s.centerX + dx, s.centerY + dy);
                    switch (terrain) {
                        case FOREST: rawWood  += 2.0f; break;
                        case STONE:  rawStone += 1.0f; break;
                        case GRASS:  rawFood  += 0.5f; break;
                    }
                }
            }

            rawWood  *= s.specialization.woodMultiplier;
            rawStone *= s.specialization.stoneMultiplier;
            rawFood  *= s.specialization.foodMultiplier;
            float rawGoods = s.population * 0.01f * s.specialization.goodsMultiplier;

            // Update smoothed production (EMA)
            s.smoothedWoodProd  = lerp(s.smoothedWoodProd,  rawWood,  SMOOTHING_ALPHA);
            s.smoothedStoneProd = lerp(s.smoothedStoneProd, rawStone, SMOOTHING_ALPHA);
            s.smoothedFoodProd  = lerp(s.smoothedFoodProd,  rawFood,  SMOOTHING_ALPHA);
            s.smoothedGoodsProd = lerp(s.smoothedGoodsProd, rawGoods, SMOOTHING_ALPHA);

            // Add to stockpiles
            s.wood  = Math.min(s.storageCapacity, s.wood  + rawWood);
            s.stone = Math.min(s.storageCapacity, s.stone + rawStone);
            s.food  = Math.min(s.storageCapacity, s.food  + rawFood);
            s.goods = Math.min(s.storageCapacity, s.goods + rawGoods);
        }
    }

    // ---- Population Growth ----

    private void consumeAndGrow(List<Settlement> settlements) {
        for (Settlement s : settlements) {
            float foodConsumed = s.population * FOOD_DEMAND_PER_POP;
            s.food = Math.max(0, s.food - foodConsumed);

            float prodVsConsume = s.smoothedFoodProd - foodConsumed;

            if (prodVsConsume > 0) {
                float levelMult = getLevelMultiplier(s.getLevel());
                float growth = prodVsConsume * GROWTH_RATE * levelMult;
                int intGrowth = (int) growth;
                if (random.nextFloat() < (growth - intGrowth)) intGrowth++;
                if (intGrowth > 0) s.addPopulation(intGrowth);

            } else if (s.food <= 0 && prodVsConsume < -0.1f) {
                // Starvation only when stockpile empty AND production < consumption
                float deficit = -prodVsConsume;
                float starvation = deficit * STARVATION_RATE;
                starvation = Math.min(starvation, s.population * 0.1f);
                int intStarve = Math.max(0, (int) Math.ceil(starvation));
                if (intStarve > 0) s.addPopulation(-intStarve);
            }
        }
    }

    private float getLevelMultiplier(SettlementLevel level) {
        switch (level) {
            case VILLAGE:    return 1.0f;
            case TOWN:       return 0.8f;
            case CITY:       return 0.6f;
            case METROPOLIS: return 0.4f;
            default:         return 1.0f;
        }
    }

    // ---- Dynamic Pricing ----

    private void updatePrices(List<Settlement> settlements) {
        for (Settlement s : settlements) {
            if (s.population <= 0) continue;
            for (ResourceType type : new ResourceType[]{
                    ResourceType.WOOD, ResourceType.STONE, ResourceType.FOOD, ResourceType.GOODS}) {
                float smoothedProd = s.getSmoothedProd(type);
                float demand = getDemand(type, s.population);
                float ratio = (demand > 0) ? smoothedProd / demand : 2.0f;
                float clampedRatio = Math.max(0.5f, Math.min(2.0f, ratio));
                float prevMult = s.getPriceMult(type);
                float newMult = lerp(prevMult, clampedRatio, PRICE_LERP_ALPHA);
                s.setPriceMult(type, newMult);
            }
        }
    }

    private float getDemand(ResourceType type, int population) {
        switch (type) {
            case FOOD:  return population * FOOD_DEMAND_PER_POP;
            default:    return population * 0.01f;
        }
    }

    // ---- Trade Route Discovery ----

    private void rebuildTradeRoutes(List<Settlement> settlements) {
        // For each pair, check if a road path exists and add/keep route
        List<TradeRoute> existing = new ArrayList<>(world.getTradeRoutes());

        for (int i = 0; i < settlements.size(); i++) {
            for (int j = i + 1; j < settlements.size(); j++) {
                Settlement a = settlements.get(i);
                Settlement b = settlements.get(j);

                TradeRoute route = world.getTradeRoute(a.id, b.id);
                List<int[]> path = world.findRoadPath(a.centerX, a.centerY, b.centerX, b.centerY);

                if (path != null && path.size() >= 2) {
                    if (route == null) {
                        world.addTradeRoute(new TradeRoute(a.id, b.id, path));
                    } else {
                        // Update path so caravans use newly built or upgraded roads
                        route.path = path;
                        route.pathLength = path.size();
                    }
                } else {
                    if (route != null) {
                        world.removeTradeRoute(route.id);
                    }
                }
            }
        }

        // Remove routes for removed settlements
        existing.clear();
    }

    // ---- Caravan Spawning ----

    private void tickSpawnTimers(float deltaTime, List<Settlement> settlements) {
        for (TradeRoute route : world.getTradeRoutes()) {
            if (!route.canSpawnCaravan()) continue;

            Settlement sA = world.getSettlement(route.idA);
            Settlement sB = world.getSettlement(route.idB);
            if (sA == null || sB == null) continue;

            float interval = route.spawnInterval(sA.population, sB.population);
            float timer = spawnTimers.getOrDefault(route.id, interval);
            timer -= deltaTime;

            if (timer <= 0) {
                trySpawnCaravan(route, sA, sB);
                timer = interval;
            }
            spawnTimers.put(route.id, timer);
        }
    }

    private void trySpawnCaravan(TradeRoute route, Settlement sA, Settlement sB) {
        // Try A→B then B→A
        if (!trySpawnDirection(route, sA, sB)) {
            trySpawnDirection(route, sB, sA);
        }
    }

    private boolean trySpawnDirection(TradeRoute route, Settlement src, Settlement dst) {
        ResourceType bestType = null;
        float bestExcess = 0;

        for (ResourceType type : new ResourceType[]{
                ResourceType.FOOD, ResourceType.WOOD, ResourceType.STONE, ResourceType.GOODS}) {
            float amount = src.getResource(type);
            float threshold = src.storageCapacity * CARGO_THRESHOLD;
            float excess = amount - threshold;
            if (excess > bestExcess) {
                // Also check that destination price is higher (arbitrage)
                if (dst.getCurrentPrice(type) >= src.getCurrentPrice(type) * 0.8f) {
                    bestExcess = excess;
                    bestType = type;
                }
            }
        }

        if (bestType == null) return false;

        float cargoAmount = Math.min(bestExcess, src.storageCapacity * CARGO_BATCH);
        src.addResource(bestType, -cargoAmount);

        // Upkeep: 0.005 gold per tile per caravan
        float upkeepPerTile = 0.005f * route.pathLength;

        // Determine direction for path indexing
        int fromId = src.id;
        int toId = dst.id;
        List<int[]> path = route.path;
        // If going B→A, we need to use the path reversed
        // For simplicity, create a caravan that traverses the stored path
        // If src is idB, the path is reversed from the route's stored direction
        if (src.id == route.idB) {
            // Use reversed path: wrap in a helper that reverses
            path = reversePath(route.path);
        }

        Caravan caravan = new Caravan(route.id, fromId, toId, bestType, cargoAmount, upkeepPerTile);
        // Store the effective path on the caravan via a thread-local workaround:
        // We store the reversed path in a small wrapper field we add to Caravan
        // For now, store path reference; Caravan only uses route.path which may be wrong for B→A
        // Simple fix: store path on the caravan itself
        caravan.setEffectivePath(path);
        world.getCaravans().add(caravan);
        route.activeCaravans++;
        return true;
    }

    private List<int[]> reversePath(List<int[]> path) {
        List<int[]> reversed = new ArrayList<>(path);
        java.util.Collections.reverse(reversed);
        return reversed;
    }

    // ---- Caravan Movement ----

    private void tickCaravans(float deltaTime) {
        List<Caravan> toRemove = new ArrayList<>();

        for (Caravan caravan : world.getCaravans()) {
            TradeRoute route = world.getTradeRouteById(caravan.routeId);
            if (route == null) {
                toRemove.add(caravan);
                continue;
            }

            List<int[]> path = caravan.getEffectivePath();
            if (path == null || path.isEmpty()) {
                toRemove.add(caravan);
                continue;
            }

            // Get road speed at current tile
            float speed = getRoadSpeed(path, caravan.pathIndex);
            caravan.subTileProgress += speed * deltaTime;

            // Advance path index for each full tile crossed
            while (caravan.subTileProgress >= 1.0f && caravan.pathIndex < path.size() - 1) {
                caravan.subTileProgress -= 1.0f;
                caravan.pathIndex++;

                // Deduct upkeep per tile
                Settlement src = world.getSettlement(caravan.fromSettlementId);
                if (src != null) {
                    src.gold = Math.max(0, src.gold - caravan.upkeepPerTile);
                }
            }

            // Check arrival at destination
            if (caravan.pathIndex >= path.size() - 1 && caravan.subTileProgress >= 1.0f) {
                deliverCargo(caravan, route);
                toRemove.add(caravan);
            }
        }

        world.getCaravans().removeAll(toRemove);
    }

    private void deliverCargo(Caravan caravan, TradeRoute route) {
        Settlement dst = world.getSettlement(caravan.toSettlementId);
        Settlement src = world.getSettlement(caravan.fromSettlementId);

        if (dst != null && caravan.cargoType != null) {
            float received = Math.min(caravan.cargoAmount,
                    dst.storageCapacity - dst.getResource(caravan.cargoType));
            dst.addResource(caravan.cargoType, received);

            // Pay source settlement: buyer (dst) pays seller (src)
            if (src != null && received > 0) {
                float price = dst.getCurrentPrice(caravan.cargoType);
                float payment = received * price * 0.1f;
                dst.gold = Math.max(0, dst.gold - payment);
                src.gold += payment;
            }
        }

        route.activeCaravans = Math.max(0, route.activeCaravans - 1);
    }

    private float getRoadSpeed(List<int[]> path, int pathIndex) {
        if (pathIndex >= path.size()) return CARAVAN_BASE_SPEED;
        int[] tile = path.get(pathIndex);
        int roadType = world.getTile(tile[0], tile[1]).roadType;
        if (roadType == 0) return CARAVAN_BASE_SPEED;
        RoadType rt = RoadType.fromId(roadType);
        return rt != null ? CARAVAN_BASE_SPEED * rt.getSpeedMultiplier() : CARAVAN_BASE_SPEED;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
