package ch.obermuhlner.sim.game;

import com.badlogic.gdx.Gdx;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads game configuration from assets/application.yml.
 * All fields default to current hardcoded values so the game runs without a YAML file.
 */
public class GameConfig {

    // ---- POJO config classes ----

    public static class WorldConfig {
        public long seed = 42L;
        public float tick_interval = 1.0f;
        public int nearby_settlement_radius = 5;
    }

    public static class SimulationConfig {
        public int scan_radius = 3;
        public float food_demand_per_pop = 0.15f;
        public float growth_rate = 0.01f;
        public float starvation_rate = 0.02f;
        public float price_lerp_alpha = 0.2f;
        public float smoothing_alpha = 0.15f;
        public float caravan_base_speed = 0.5f;
        public float cargo_threshold = 0.2f;
        public float cargo_batch = 0.3f;
        public float caravan_upkeep_per_tile = 0.005f;
        public float price_min = 0.5f;
        public float price_max = 2.0f;
        public float goods_demand_multiplier = 0.01f;
        public float arbitrage_threshold = 0.8f;
        public Map<String, Float> level_growth_multipliers = new HashMap<String, Float>() {{
            put("VILLAGE", 1.0f); put("TOWN", 0.8f); put("CITY", 0.6f); put("METROPOLIS", 0.4f);
        }};
        public Map<String, Float> terrain_production = new HashMap<String, Float>() {{
            put("FOREST_WOOD", 2.0f); put("STONE_STONE", 1.0f); put("GRASS_FOOD", 0.5f);
        }};
    }

    public static class SettlementConfig {
        public int starting_population = 10;
        public float starting_gold = 50.0f;
        public float storage_capacity = 500.0f;
        public Map<String, Integer> max_buildings = new HashMap<String, Integer>() {{
            put("VILLAGE", 5); put("TOWN", 15); put("CITY", 30); put("METROPOLIS", 50);
        }};
    }

    public static class TradeConfig {
        public int max_caravans_per_route = 3;
        public float base_spawn_interval = 120.0f;
    }

    public static class PathfindingConfig {
        public int max_manhattan = 300;
        public int bridge_zone_radius = 2;
        public float bridge_zone_cost = 2.0f;
    }

    public static class ThresholdConfig {
        public double water = 0.45;
        public double grass = 0.55;
        public double forest = 0.60;
        public double stone = 0.65;
    }

    public static class TerrainTypeConfig {
        public int tile_index = -1;
    }

    public static class TerrainObjectConfig {
        public String name = "";
        public int id = 0;
        public String image = "";
        public boolean walkable = false;
        public float destroy_cost = 0f;
        public Map<String, Float> spawn = new LinkedHashMap<>();
        public Map<String, Float> harvest = new LinkedHashMap<>();
    }

    public static class DestroyConfig {
        public float building_fraction = 0.5f;
    }

    public static class ExplorationRewardConfig {
        public String name = "";
        public int id = 0;
        public String image = "";
        public boolean walkable = false;
        public String reward_type = "ONE_TIME"; // ONE_TIME or BONUS
        public String required_level = "VILLAGE";
        public Map<String, Float> rewards = new LinkedHashMap<>();          // resource -> one-time amount
        public Map<String, Float> bonus_production = new LinkedHashMap<>();  // resource -> per-tick bonus
        public Map<String, Float> spawn = new LinkedHashMap<>();             // terrain -> spawn probability

        public boolean isOneTime() { return "ONE_TIME".equals(reward_type); }
        public boolean isBonus()   { return "BONUS".equals(reward_type); }
    }

    public static class TerrainConfig {
        public double noise_scale = 0.04;
        public int noise_octaves = 4;
        public double persistence = 0.5;
        public String tileset = "64x64/map.png";
        public ThresholdConfig thresholds = new ThresholdConfig();
        public Map<String, TerrainTypeConfig> types = new LinkedHashMap<>();
    }

    public static class Root {
        public WorldConfig world = new WorldConfig();
        public SimulationConfig simulation = new SimulationConfig();
        public SettlementConfig settlement = new SettlementConfig();
        public TradeConfig trade = new TradeConfig();
        public PathfindingConfig pathfinding = new PathfindingConfig();
        public DestroyConfig destroy = new DestroyConfig();
        public TerrainConfig terrain = new TerrainConfig();
        public Map<String, Map<String, Object>> roads = new HashMap<>();
        public Map<String, Map<String, Object>> buildings = new HashMap<>();
        public Map<String, Map<String, Object>> specializations = new HashMap<>();
        public Map<String, TerrainObjectConfig> terrain_objects = new LinkedHashMap<>();
        public Map<String, ExplorationRewardConfig> exploration_rewards = new LinkedHashMap<>();
    }

    private final Root root;

    public GameConfig() {
        this.root = loadRoot();
    }

    /** Constructor for tests and headless contexts — bypasses Gdx.files. */
    public GameConfig(Root root) {
        this.root = root;
    }

    @SuppressWarnings("unchecked")
    private Root loadRoot() {
        Root r = new Root();
        try (InputStream is = Gdx.files.internal("application.yml").read()) {
            Yaml yaml = new Yaml();
            Map<String, Object> raw = yaml.load(is);
            if (raw == null) return r;
            bindWorld(r, raw);
            bindSimulation(r, raw);
            bindSettlement(r, raw);
            bindTrade(r, raw);
            bindPathfinding(r, raw);
            bindDestroy(r, raw);
            bindTerrain(r, raw);
            bindRoads(r, raw);
            bindBuildings(r, raw);
            bindSpecializations(r, raw);
            bindTerrainObjects(r, raw);
            bindExplorationRewards(r, raw);
        } catch (Exception e) {
            Gdx.app.log("GameConfig", "Failed to load application.yml: " + e.getMessage());
        }
        return r;
    }

    @SuppressWarnings("unchecked")
    private void bindWorld(Root r, Map<String, Object> raw) {
        Map<String, Object> m = (Map<String, Object>) raw.get("world");
        if (m == null) return;
        if (m.containsKey("seed")) r.world.seed = ((Number) m.get("seed")).longValue();
        if (m.containsKey("tick_interval")) r.world.tick_interval = ((Number) m.get("tick_interval")).floatValue();
        if (m.containsKey("nearby_settlement_radius")) r.world.nearby_settlement_radius = ((Number) m.get("nearby_settlement_radius")).intValue();
    }

    @SuppressWarnings("unchecked")
    private void bindSimulation(Root r, Map<String, Object> raw) {
        Map<String, Object> m = (Map<String, Object>) raw.get("simulation");
        if (m == null) return;
        SimulationConfig s = r.simulation;
        if (m.containsKey("scan_radius")) s.scan_radius = ((Number) m.get("scan_radius")).intValue();
        if (m.containsKey("food_demand_per_pop")) s.food_demand_per_pop = ((Number) m.get("food_demand_per_pop")).floatValue();
        if (m.containsKey("growth_rate")) s.growth_rate = ((Number) m.get("growth_rate")).floatValue();
        if (m.containsKey("starvation_rate")) s.starvation_rate = ((Number) m.get("starvation_rate")).floatValue();
        if (m.containsKey("price_lerp_alpha")) s.price_lerp_alpha = ((Number) m.get("price_lerp_alpha")).floatValue();
        if (m.containsKey("smoothing_alpha")) s.smoothing_alpha = ((Number) m.get("smoothing_alpha")).floatValue();
        if (m.containsKey("caravan_base_speed")) s.caravan_base_speed = ((Number) m.get("caravan_base_speed")).floatValue();
        if (m.containsKey("cargo_threshold")) s.cargo_threshold = ((Number) m.get("cargo_threshold")).floatValue();
        if (m.containsKey("cargo_batch")) s.cargo_batch = ((Number) m.get("cargo_batch")).floatValue();
        if (m.containsKey("caravan_upkeep_per_tile")) s.caravan_upkeep_per_tile = ((Number) m.get("caravan_upkeep_per_tile")).floatValue();
        if (m.containsKey("price_min")) s.price_min = ((Number) m.get("price_min")).floatValue();
        if (m.containsKey("price_max")) s.price_max = ((Number) m.get("price_max")).floatValue();
        if (m.containsKey("goods_demand_multiplier")) s.goods_demand_multiplier = ((Number) m.get("goods_demand_multiplier")).floatValue();
        if (m.containsKey("arbitrage_threshold")) s.arbitrage_threshold = ((Number) m.get("arbitrage_threshold")).floatValue();
        Map<String, Object> lgm = (Map<String, Object>) m.get("level_growth_multipliers");
        if (lgm != null) {
            for (Map.Entry<String, Object> e : lgm.entrySet()) {
                s.level_growth_multipliers.put(e.getKey().toUpperCase(), ((Number) e.getValue()).floatValue());
            }
        }
        Map<String, Object> tp = (Map<String, Object>) m.get("terrain_production");
        if (tp != null) {
            for (Map.Entry<String, Object> e : tp.entrySet()) {
                s.terrain_production.put(e.getKey().toUpperCase(), ((Number) e.getValue()).floatValue());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void bindSettlement(Root r, Map<String, Object> raw) {
        Map<String, Object> m = (Map<String, Object>) raw.get("settlement");
        if (m == null) return;
        SettlementConfig s = r.settlement;
        if (m.containsKey("starting_population")) s.starting_population = ((Number) m.get("starting_population")).intValue();
        if (m.containsKey("starting_gold")) s.starting_gold = ((Number) m.get("starting_gold")).floatValue();
        if (m.containsKey("storage_capacity")) s.storage_capacity = ((Number) m.get("storage_capacity")).floatValue();
        Map<String, Object> mb = (Map<String, Object>) m.get("max_buildings");
        if (mb != null) {
            for (Map.Entry<String, Object> e : mb.entrySet()) {
                s.max_buildings.put(e.getKey().toUpperCase(), ((Number) e.getValue()).intValue());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void bindTrade(Root r, Map<String, Object> raw) {
        Map<String, Object> m = (Map<String, Object>) raw.get("trade");
        if (m == null) return;
        if (m.containsKey("max_caravans_per_route")) r.trade.max_caravans_per_route = ((Number) m.get("max_caravans_per_route")).intValue();
        if (m.containsKey("base_spawn_interval")) r.trade.base_spawn_interval = ((Number) m.get("base_spawn_interval")).floatValue();
    }

    @SuppressWarnings("unchecked")
    private void bindPathfinding(Root r, Map<String, Object> raw) {
        Map<String, Object> m = (Map<String, Object>) raw.get("pathfinding");
        if (m == null) return;
        if (m.containsKey("max_manhattan")) r.pathfinding.max_manhattan = ((Number) m.get("max_manhattan")).intValue();
        if (m.containsKey("bridge_zone_radius")) r.pathfinding.bridge_zone_radius = ((Number) m.get("bridge_zone_radius")).intValue();
        if (m.containsKey("bridge_zone_cost")) r.pathfinding.bridge_zone_cost = ((Number) m.get("bridge_zone_cost")).floatValue();
    }

    @SuppressWarnings("unchecked")
    private void bindDestroy(Root r, Map<String, Object> raw) {
        Map<String, Object> m = (Map<String, Object>) raw.get("destroy");
        if (m == null) return;
        if (m.containsKey("building_fraction")) r.destroy.building_fraction = ((Number) m.get("building_fraction")).floatValue();
    }

    @SuppressWarnings("unchecked")
    private void bindTerrain(Root r, Map<String, Object> raw) {
        Map<String, Object> m = (Map<String, Object>) raw.get("terrain");
        if (m == null) return;
        TerrainConfig t = r.terrain;
        if (m.containsKey("noise_scale")) t.noise_scale = ((Number) m.get("noise_scale")).doubleValue();
        if (m.containsKey("noise_octaves")) t.noise_octaves = ((Number) m.get("noise_octaves")).intValue();
        if (m.containsKey("persistence")) t.persistence = ((Number) m.get("persistence")).doubleValue();
        if (m.containsKey("tileset")) t.tileset = (String) m.get("tileset");
        Map<String, Object> thresh = (Map<String, Object>) m.get("thresholds");
        if (thresh != null) {
            if (thresh.containsKey("water"))  t.thresholds.water  = ((Number) thresh.get("water")).doubleValue();
            if (thresh.containsKey("grass"))  t.thresholds.grass  = ((Number) thresh.get("grass")).doubleValue();
            if (thresh.containsKey("forest")) t.thresholds.forest = ((Number) thresh.get("forest")).doubleValue();
            if (thresh.containsKey("stone"))  t.thresholds.stone  = ((Number) thresh.get("stone")).doubleValue();
        }
        Map<String, Object> typesRaw = (Map<String, Object>) m.get("types");
        if (typesRaw != null) {
            for (Map.Entry<String, Object> e : typesRaw.entrySet()) {
                TerrainTypeConfig tc = new TerrainTypeConfig();
                if (e.getValue() instanceof Map) {
                    Map<String, Object> tData = (Map<String, Object>) e.getValue();
                    if (tData.containsKey("tile_index")) tc.tile_index = ((Number) tData.get("tile_index")).intValue();
                }
                t.types.put(e.getKey().toUpperCase(), tc);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void bindTerrainObjects(Root r, Map<String, Object> raw) {
        Object toRaw = raw.get("terrain_objects");
        if (toRaw == null) return;
        Map<String, Object> toMap = (Map<String, Object>) toRaw;
        for (Map.Entry<String, Object> e : toMap.entrySet()) {
            String name = e.getKey().toUpperCase();
            if (!(e.getValue() instanceof Map)) continue;
            Map<String, Object> data = (Map<String, Object>) e.getValue();
            TerrainObjectConfig toc = new TerrainObjectConfig();
            toc.name = name;
            if (data.containsKey("id")) toc.id = ((Number) data.get("id")).intValue();
            if (data.containsKey("image")) toc.image = (String) data.get("image");
            if (data.containsKey("walkable")) toc.walkable = (Boolean) data.get("walkable");
            if (data.containsKey("destroy_cost")) toc.destroy_cost = ((Number) data.get("destroy_cost")).floatValue();
            Map<String, Object> spawnRaw = (Map<String, Object>) data.get("spawn");
            if (spawnRaw != null) {
                for (Map.Entry<String, Object> se : spawnRaw.entrySet()) {
                    toc.spawn.put(se.getKey().toUpperCase(), ((Number) se.getValue()).floatValue());
                }
            }
            Map<String, Object> harvestRaw = (Map<String, Object>) data.get("harvest");
            if (harvestRaw != null) {
                for (Map.Entry<String, Object> he : harvestRaw.entrySet()) {
                    toc.harvest.put(he.getKey().toUpperCase(), ((Number) he.getValue()).floatValue());
                }
            }
            r.terrain_objects.put(name, toc);
        }
    }

    @SuppressWarnings("unchecked")
    private void bindExplorationRewards(Root r, Map<String, Object> raw) {
        Object erRaw = raw.get("exploration_rewards");
        if (erRaw == null) return;
        Map<String, Object> erMap = (Map<String, Object>) erRaw;
        for (Map.Entry<String, Object> e : erMap.entrySet()) {
            String name = e.getKey().toUpperCase();
            if (!(e.getValue() instanceof Map)) continue;
            Map<String, Object> data = (Map<String, Object>) e.getValue();
            ExplorationRewardConfig erc = new ExplorationRewardConfig();
            erc.name = name;
            if (data.containsKey("id")) erc.id = ((Number) data.get("id")).intValue();
            if (data.containsKey("image")) erc.image = (String) data.get("image");
            if (data.containsKey("walkable")) erc.walkable = (Boolean) data.get("walkable");
            if (data.containsKey("reward_type")) erc.reward_type = ((String) data.get("reward_type")).toUpperCase();
            if (data.containsKey("required_level")) erc.required_level = ((String) data.get("required_level")).toUpperCase();
            Map<String, Object> rewardsRaw = (Map<String, Object>) data.get("rewards");
            if (rewardsRaw != null) {
                for (Map.Entry<String, Object> re : rewardsRaw.entrySet()) {
                    erc.rewards.put(re.getKey().toUpperCase(), ((Number) re.getValue()).floatValue());
                }
            }
            Map<String, Object> bonusRaw = (Map<String, Object>) data.get("bonus_production");
            if (bonusRaw != null) {
                for (Map.Entry<String, Object> be : bonusRaw.entrySet()) {
                    erc.bonus_production.put(be.getKey().toUpperCase(), ((Number) be.getValue()).floatValue());
                }
            }
            Map<String, Object> spawnRaw = (Map<String, Object>) data.get("spawn");
            if (spawnRaw != null) {
                for (Map.Entry<String, Object> se : spawnRaw.entrySet()) {
                    erc.spawn.put(se.getKey().toUpperCase(), ((Number) se.getValue()).floatValue());
                }
            }
            r.exploration_rewards.put(name, erc);
        }
    }

    @SuppressWarnings("unchecked")
    private void bindRoads(Root r, Map<String, Object> raw) {
        Object roadsRaw = raw.get("roads");
        if (roadsRaw == null) return;
        Map<String, Object> roadsMap = (Map<String, Object>) roadsRaw;
        for (Map.Entry<String, Object> e : roadsMap.entrySet()) {
            String key = e.getKey().toUpperCase();
            Object val = e.getValue();
            if (val instanceof Map) {
                r.roads.put(key, (Map<String, Object>) val);
            } else if (val instanceof Number) {
                // Legacy flat format: just cost
                Map<String, Object> roadData = new HashMap<>();
                roadData.put("cost", val);
                r.roads.put(key, roadData);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void bindBuildings(Root r, Map<String, Object> raw) {
        Object buildingsRaw = raw.get("buildings");
        if (buildingsRaw == null) return;
        Map<String, Object> buildingsMap = (Map<String, Object>) buildingsRaw;
        for (Map.Entry<String, Object> e : buildingsMap.entrySet()) {
            String key = e.getKey().toUpperCase();
            Object val = e.getValue();
            if (val instanceof Map) {
                r.buildings.put(key, (Map<String, Object>) val);
            } else if (val instanceof Number) {
                // Legacy flat format: just cost
                Map<String, Object> bData = new HashMap<>();
                bData.put("cost", val);
                r.buildings.put(key, bData);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void bindSpecializations(Root r, Map<String, Object> raw) {
        Object specsRaw = raw.get("specializations");
        if (specsRaw == null) return;
        Map<String, Object> specsMap = (Map<String, Object>) specsRaw;
        for (Map.Entry<String, Object> e : specsMap.entrySet()) {
            String key = e.getKey().toUpperCase();
            Object val = e.getValue();
            if (val instanceof Map) {
                r.specializations.put(key, (Map<String, Object>) val);
            }
        }
    }

    // ---- World accessors ----

    public long getWorldSeed() { return root.world.seed; }
    public float getTickInterval() { return root.world.tick_interval; }
    public int getNearbySettlementRadius() { return root.world.nearby_settlement_radius; }

    // ---- Simulation accessors ----

    public int getScanRadius() { return root.simulation.scan_radius; }
    public float getFoodDemandPerPop() { return root.simulation.food_demand_per_pop; }
    public float getGrowthRate() { return root.simulation.growth_rate; }
    public float getStarvationRate() { return root.simulation.starvation_rate; }
    public float getPriceLerpAlpha() { return root.simulation.price_lerp_alpha; }
    public float getSmoothingAlpha() { return root.simulation.smoothing_alpha; }
    public float getCaravanBaseSpeed() { return root.simulation.caravan_base_speed; }
    public float getCargoThreshold() { return root.simulation.cargo_threshold; }
    public float getCargoBatch() { return root.simulation.cargo_batch; }
    public float getCaravanUpkeepPerTile() { return root.simulation.caravan_upkeep_per_tile; }
    public float getPriceMin() { return root.simulation.price_min; }
    public float getPriceMax() { return root.simulation.price_max; }
    public float getGoodsDemandMultiplier() { return root.simulation.goods_demand_multiplier; }
    public float getArbitrageThreshold() { return root.simulation.arbitrage_threshold; }

    public float getLevelGrowthMultiplier(SettlementLevel level) {
        return root.simulation.level_growth_multipliers.getOrDefault(level.name(), 1.0f);
    }

    public float getTerrainProduction(String key) {
        return root.simulation.terrain_production.getOrDefault(key, 0f);
    }

    // ---- Settlement accessors ----

    public int getStartingPopulation() { return root.settlement.starting_population; }
    public float getStartingGold() { return root.settlement.starting_gold; }
    public float getStorageCapacity() { return root.settlement.storage_capacity; }

    public int getMaxBuildings(SettlementLevel level) {
        return root.settlement.max_buildings.getOrDefault(level.name(), 5);
    }

    // ---- Trade accessors ----

    public int getMaxCaravansPerRoute() { return root.trade.max_caravans_per_route; }
    public float getBaseSpawnInterval() { return root.trade.base_spawn_interval; }

    // ---- Pathfinding accessors ----

    public int getMaxManhattan() { return root.pathfinding.max_manhattan; }
    public int getBridgeZoneRadius() { return root.pathfinding.bridge_zone_radius; }
    public float getBridgeZoneCost() { return root.pathfinding.bridge_zone_cost; }

    // ---- Terrain accessors ----

    public double getNoiseScale() { return root.terrain.noise_scale; }
    public int getNoiseOctaves() { return root.terrain.noise_octaves; }
    public double getPersistence() { return root.terrain.persistence; }
    public String getTerrainTileset() { return root.terrain.tileset; }

    public int getTerrainTileIndex(TerrainType type) {
        TerrainTypeConfig tc = root.terrain.types.get(type.name());
        if (tc != null && tc.tile_index > 0) return tc.tile_index;
        return type.getTileIndex();
    }

    public double getTerrainThreshold(String terrainName) {
        switch (terrainName.toLowerCase()) {
            case "water":  return root.terrain.thresholds.water;
            case "grass":  return root.terrain.thresholds.grass;
            case "forest": return root.terrain.thresholds.forest;
            case "stone":  return root.terrain.thresholds.stone;
            default:       return 1.0;
        }
    }

    public List<TerrainObjectConfig> getTerrainObjects() {
        return new ArrayList<>(root.terrain_objects.values());
    }

    public Map<String, Float> getTerrainObjectHarvest(int objectId) {
        for (TerrainObjectConfig toc : root.terrain_objects.values()) {
            if (toc.id == objectId) return toc.harvest;
        }
        return java.util.Collections.emptyMap();
    }

    public float getTerrainObjectDestroyCost(int objectId) {
        for (TerrainObjectConfig toc : root.terrain_objects.values()) {
            if (toc.id == objectId) return toc.destroy_cost;
        }
        return 0f;
    }

    public float getDestroyBuildingFraction() {
        return root.destroy.building_fraction;
    }

    // ---- Exploration reward accessors ----

    public List<ExplorationRewardConfig> getExplorationRewards() {
        return new ArrayList<>(root.exploration_rewards.values());
    }

    private Map<Integer, ExplorationRewardConfig> rewardById = null;

    public ExplorationRewardConfig getExplorationReward(int objectId) {
        if (rewardById == null) {
            rewardById = new HashMap<>();
            for (ExplorationRewardConfig erc : root.exploration_rewards.values()) {
                rewardById.put(erc.id, erc);
            }
        }
        return rewardById.get(objectId);
    }

    // ---- Road accessors ----

    public float getRoadCost(RoadType type) {
        Map<String, Object> data = root.roads.get(type.name());
        if (data != null && data.containsKey("cost")) {
            return ((Number) data.get("cost")).floatValue();
        }
        return 0f;
    }

    public float getRoadSpeedMultiplier(RoadType type) {
        Map<String, Object> data = root.roads.get(type.name());
        if (data != null && data.containsKey("speed_multiplier")) {
            return ((Number) data.get("speed_multiplier")).floatValue();
        }
        return type.getSpeedMultiplier();
    }

    public int getRoadCapacity(RoadType type) {
        Map<String, Object> data = root.roads.get(type.name());
        if (data != null && data.containsKey("capacity")) {
            return ((Number) data.get("capacity")).intValue();
        }
        return type.getCapacity();
    }

    public float getRoadUpkeepPerTick(RoadType type) {
        Map<String, Object> data = root.roads.get(type.name());
        if (data != null && data.containsKey("upkeep_per_tick")) {
            return ((Number) data.get("upkeep_per_tick")).floatValue();
        }
        return type.getUpkeepPerTick();
    }

    public float getRoadDestroyCost(RoadType type) {
        Map<String, Object> data = root.roads.get(type.name());
        if (data != null && data.containsKey("destroy_cost")) {
            return ((Number) data.get("destroy_cost")).floatValue();
        }
        return 0f;
    }

    // ---- Building accessors ----

    public float getBuildingCost(BuildingType type) {
        Map<String, Object> data = root.buildings.get(type.name());
        if (data != null && data.containsKey("cost")) {
            return ((Number) data.get("cost")).floatValue();
        }
        return 0f;
    }

    public int getBuildingPopulationCapacity(BuildingType type) {
        Map<String, Object> data = root.buildings.get(type.name());
        if (data != null && data.containsKey("population_capacity")) {
            return ((Number) data.get("population_capacity")).intValue();
        }
        return type.getPopulationCapacity();
    }

    // ---- Specialization accessors ----

    public float getSpecWoodMultiplier(Specialization spec) {
        Map<String, Object> data = root.specializations.get(spec.name());
        if (data != null && data.containsKey("wood")) {
            return ((Number) data.get("wood")).floatValue();
        }
        return spec.woodMultiplier;
    }

    public float getSpecStoneMultiplier(Specialization spec) {
        Map<String, Object> data = root.specializations.get(spec.name());
        if (data != null && data.containsKey("stone")) {
            return ((Number) data.get("stone")).floatValue();
        }
        return spec.stoneMultiplier;
    }

    public float getSpecFoodMultiplier(Specialization spec) {
        Map<String, Object> data = root.specializations.get(spec.name());
        if (data != null && data.containsKey("food")) {
            return ((Number) data.get("food")).floatValue();
        }
        return spec.foodMultiplier;
    }

    public float getSpecGoodsMultiplier(Specialization spec) {
        Map<String, Object> data = root.specializations.get(spec.name());
        if (data != null && data.containsKey("goods")) {
            return ((Number) data.get("goods")).floatValue();
        }
        return spec.goodsMultiplier;
    }
}
