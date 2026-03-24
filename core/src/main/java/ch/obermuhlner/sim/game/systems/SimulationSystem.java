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
    private final World world;
    private final GameConfig config;
    private final Random random = new Random();

    // Spawn timer per trade route id
    private final java.util.Map<Integer, Float> spawnTimers = new java.util.HashMap<>();

    private long tickCount = 0;

    public SimulationSystem(World world, GameConfig config) {
        this.world = world;
        this.config = config;
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
        tickResearch(settlements);
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
        int scanRadius = config.getScanRadius();
        for (Settlement s : settlements) {
            float rawWood = 0, rawStone = 0, rawFood = 0;

            for (int dy = -scanRadius; dy <= scanRadius; dy++) {
                for (int dx = -scanRadius; dx <= scanRadius; dx++) {
                    if (dx * dx + dy * dy > scanRadius * scanRadius) continue;
                    Tile tile = world.getTile(s.centerX + dx, s.centerY + dy);
                    switch (tile.terrain) {
                        case FOREST: rawWood  += config.getTerrainProduction("FOREST_WOOD"); break;
                        case STONE:  rawStone += config.getTerrainProduction("STONE_STONE"); break;
                        case GRASS:  rawFood  += config.getTerrainProduction("GRASS_FOOD"); break;
                    }
                    // Apply bonus production from BONUS-type exploration rewards
                    GameConfig.ExplorationRewardConfig reward = config.getExplorationReward(tile.objectId);
                    if (reward != null && reward.isBonus()) {
                        SettlementLevel required = config.getLevelById(reward.required_level);
                        if (required == null || s.getLevel().ordinal() >= required.ordinal()) {
                            rawWood  += reward.bonus_production.getOrDefault("WOOD",  0f);
                            rawStone += reward.bonus_production.getOrDefault("STONE", 0f);
                            rawFood  += reward.bonus_production.getOrDefault("FOOD",  0f);
                        }
                    }
                }
            }

            rawWood  *= config.getSpecWoodMultiplier(s.specialization);
            rawStone *= config.getSpecStoneMultiplier(s.specialization);
            rawFood  *= config.getSpecFoodMultiplier(s.specialization);

            // Apply tech tree production bonuses
            rawWood  *= (1f + world.techTree.getEffectTotal("wood_multiplier",  config));
            rawStone *= (1f + world.techTree.getEffectTotal("stone_multiplier", config));
            rawFood  *= (1f + world.techTree.getEffectTotal("food_multiplier",  config));
            float rawGoods = s.population * config.getGoodsDemandMultiplier() * config.getSpecGoodsMultiplier(s.specialization);

            float smoothingAlpha = config.getSmoothingAlpha();
            s.smoothedWoodProd  = lerp(s.smoothedWoodProd,  rawWood,  smoothingAlpha);
            s.smoothedStoneProd = lerp(s.smoothedStoneProd, rawStone, smoothingAlpha);
            s.smoothedFoodProd  = lerp(s.smoothedFoodProd,  rawFood,  smoothingAlpha);
            s.smoothedGoodsProd = lerp(s.smoothedGoodsProd, rawGoods, smoothingAlpha);

            s.wood  = Math.min(s.storageCapacity, s.wood  + rawWood);
            s.stone = Math.min(s.storageCapacity, s.stone + rawStone);
            s.food  = Math.min(s.storageCapacity, s.food  + rawFood);
            s.goods = Math.min(s.storageCapacity, s.goods + rawGoods);
        }
    }

    // ---- Population Growth ----

    private void consumeAndGrow(List<Settlement> settlements) {
        float foodDemandPerPop = config.getFoodDemandPerPop();
        float growthRate = config.getGrowthRate();
        float starvationRate = config.getStarvationRate();

        for (Settlement s : settlements) {
            float foodConsumed = s.population * foodDemandPerPop;
            s.food = Math.max(0, s.food - foodConsumed);

            float prodVsConsume = s.smoothedFoodProd - foodConsumed;

            if (prodVsConsume > 0) {
                float levelMult = config.getLevelGrowthMultiplier(s.getLevel());
                float growth = prodVsConsume * growthRate * levelMult;
                int intGrowth = (int) growth;
                if (random.nextFloat() < (growth - intGrowth)) intGrowth++;
                if (intGrowth > 0) s.addPopulation(intGrowth);

            } else if (s.food <= 0 && prodVsConsume < -0.1f) {
                float deficit = -prodVsConsume;
                float starvation = deficit * starvationRate;
                starvation = Math.min(starvation, s.population * 0.1f);
                int intStarve = Math.max(0, (int) Math.ceil(starvation));
                if (intStarve > 0) s.addPopulation(-intStarve);
            }
        }
    }

    // ---- Dynamic Pricing ----

    private void updatePrices(List<Settlement> settlements) {
        float priceMin = config.getPriceMin();
        float priceMax = config.getPriceMax();
        float priceLerpAlpha = config.getPriceLerpAlpha();

        for (Settlement s : settlements) {
            if (s.population <= 0) continue;
            for (ResourceType type : new ResourceType[]{
                    ResourceType.WOOD, ResourceType.STONE, ResourceType.FOOD, ResourceType.GOODS}) {
                float smoothedProd = s.getSmoothedProd(type);
                float demand = getDemand(type, s.population);
                float ratio = (demand > 0) ? smoothedProd / demand : 2.0f;
                float clampedRatio = Math.max(priceMin, Math.min(priceMax, ratio));
                float prevMult = s.getPriceMult(type);
                float newMult = lerp(prevMult, clampedRatio, priceLerpAlpha);
                s.setPriceMult(type, newMult);
            }
        }
    }

    private float getDemand(ResourceType type, int population) {
        switch (type) {
            case FOOD:  return population * config.getFoodDemandPerPop();
            default:    return population * config.getGoodsDemandMultiplier();
        }
    }

    // ---- Trade Route Discovery ----

    private void rebuildTradeRoutes(List<Settlement> settlements) {
        for (int i = 0; i < settlements.size(); i++) {
            for (int j = i + 1; j < settlements.size(); j++) {
                Settlement a = settlements.get(i);
                Settlement b = settlements.get(j);

                // ---- Land route ----
                TradeRoute landRoute = world.getLandTradeRoute(a.id, b.id);
                List<int[]> roadPath = world.findRoadPath(a.centerX, a.centerY, b.centerX, b.centerY);

                if (roadPath != null && roadPath.size() >= 2) {
                    if (landRoute == null) {
                        world.addTradeRoute(new TradeRoute(a.id, b.id, roadPath));
                    } else {
                        landRoute.path = roadPath;
                        landRoute.pathLength = roadPath.size();
                    }
                    // Land route supersedes any sea route
                    TradeRoute seaRoute = world.getSeaTradeRoute(a.id, b.id);
                    if (seaRoute != null) world.removeTradeRoute(seaRoute.id);
                } else {
                    if (landRoute != null) world.removeTradeRoute(landRoute.id);

                    // ---- Sea route (fallback when no land road exists) ----
                    boolean aHasHarbor = world.settlementHasHarbor(a)
                        && world.techTree.isAllowed("buildings", "HARBOR", config);
                    boolean bHasHarbor = world.settlementHasHarbor(b)
                        && world.techTree.isAllowed("buildings", "HARBOR", config);

                    TradeRoute seaRoute = world.getSeaTradeRoute(a.id, b.id);
                    if (aHasHarbor && bHasHarbor) {
                        List<int[]> seaPath = world.findSeaPath(a, b);
                        if (seaPath != null && seaPath.size() >= 2) {
                            if (seaRoute == null) {
                                TradeRoute newSea = new TradeRoute(a.id, b.id, seaPath);
                                newSea.isSea = true;
                                world.addTradeRoute(newSea);
                            } else {
                                seaRoute.path = seaPath;
                                seaRoute.pathLength = seaPath.size();
                            }
                        } else if (seaRoute != null) {
                            world.removeTradeRoute(seaRoute.id);
                        }
                    } else if (seaRoute != null) {
                        world.removeTradeRoute(seaRoute.id);
                    }
                }
            }
        }
    }

    // ---- Caravan Spawning ----

    private void tickSpawnTimers(float deltaTime, List<Settlement> settlements) {
        int maxCaravans = config.getMaxCaravansPerRoute();
        float baseInterval = config.getBaseSpawnInterval();

        for (TradeRoute route : world.getTradeRoutes()) {
            if (!route.canSpawnCaravan(maxCaravans)) continue;

            Settlement sA = world.getSettlement(route.idA);
            Settlement sB = world.getSettlement(route.idB);
            if (sA == null || sB == null) continue;

            float interval = route.spawnInterval(sA.population, sB.population, baseInterval);
            float timer = spawnTimers.getOrDefault(route.id, interval);
            timer -= deltaTime;

            if (timer <= 0) {
                trySpawnCaravan(route, sA, sB, maxCaravans);
                timer = interval;
            }
            spawnTimers.put(route.id, timer);
        }
    }

    private void trySpawnCaravan(TradeRoute route, Settlement sA, Settlement sB, int maxCaravans) {
        trySpawnDirection(route, sA, sB);
        if (route.canSpawnCaravan(maxCaravans)) {
            trySpawnDirection(route, sB, sA);
        }
    }

    private boolean trySpawnDirection(TradeRoute route, Settlement src, Settlement dst) {
        ResourceType bestType = null;
        float bestExcess = 0;
        float cargoThreshold = config.getCargoThreshold();
        float arbitrageThreshold = config.getArbitrageThreshold();

        for (ResourceType type : new ResourceType[]{
                ResourceType.FOOD, ResourceType.WOOD, ResourceType.STONE, ResourceType.GOODS}) {
            float amount = src.getResource(type);
            float threshold = src.storageCapacity * cargoThreshold;
            float excess = amount - threshold;
            if (excess > bestExcess) {
                if (dst.getCurrentPrice(type) >= src.getCurrentPrice(type) * arbitrageThreshold) {
                    bestExcess = excess;
                    bestType = type;
                }
            }
        }

        if (bestType == null) return false;

        float cargoAmount = Math.min(bestExcess, src.storageCapacity * config.getCargoBatch());
        src.addResource(bestType, -cargoAmount);

        float upkeepPerTile = config.getCaravanUpkeepPerTile();

        int fromId = src.id;
        int toId = dst.id;
        List<int[]> path = route.path;
        if (src.id == route.idB) {
            path = reversePath(route.path);
        }

        Caravan caravan = new Caravan(route.id, fromId, toId, bestType, cargoAmount, upkeepPerTile);
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

            float speed = getRoadSpeed(path, caravan.pathIndex);
            caravan.subTileProgress += speed * deltaTime;

            while (caravan.subTileProgress >= 1.0f && caravan.pathIndex < path.size() - 1) {
                caravan.subTileProgress -= 1.0f;
                caravan.pathIndex++;

                Settlement src = world.getSettlement(caravan.fromSettlementId);
                if (src != null) {
                    src.gold = Math.max(0, src.gold - caravan.upkeepPerTile);
                }
            }

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

            if (src != null && received > 0) {
                float price = dst.getCurrentPrice(caravan.cargoType);
                float payment = received * price * 0.1f;
                dst.gold = Math.max(0, dst.gold - payment);
                src.gold += payment;
                src.tradeRevenue += payment;
            }
        }

        route.activeCaravans = Math.max(0, route.activeCaravans - 1);
    }

    private float getRoadSpeed(List<int[]> path, int pathIndex) {
        float baseSpeed = config.getCaravanBaseSpeed();
        if (pathIndex >= path.size()) return baseSpeed;
        int[] tile = path.get(pathIndex);
        int roadType = world.getTile(tile[0], tile[1]).roadType;
        if (roadType == 0) return baseSpeed;
        RoadType rt = RoadType.fromId(roadType);
        return rt != null ? baseSpeed * config.getRoadSpeedMultiplier(rt) : baseSpeed;
    }

    // ---- Research ----

    private void tickResearch(List<Settlement> settlements) {
        if (!world.techTree.hasActiveResearch()) return;
        if (settlements.isEmpty()) return;
        float ratePerTick = config.getResearchGoldPerTick();
        float sharePerSettlement = ratePerTick / settlements.size();
        float totalPaid = 0;
        for (Settlement s : settlements) {
            float payment = Math.min(s.gold, sharePerSettlement);
            s.gold -= payment;
            totalPaid += payment;
        }
        world.techTree.addProgress(totalPaid, config);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
