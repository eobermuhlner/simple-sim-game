package ch.obermuhlner.sim;

import ch.obermuhlner.sim.game.*;
import ch.obermuhlner.sim.game.systems.SimulationSystem;

import java.util.*;

public class SimulationRunner {
    private final World world;
    private final GameConfig config;
    private final SimulationSystem simulation;
    
    private final int ticks;
    private final Map<String, BalanceTracker> trackers = new HashMap<>();
    
    public SimulationRunner(GameConfig config, int ticks) {
        this.config = config;
        this.ticks = ticks;
        this.world = new World(16, config, true);
        this.simulation = new SimulationSystem(world, config);
    }
    
    public void setupDefaultScenario() {
        world.createStarterSettlement();
        
        world.reveal(5, 0);
        Settlement s2 = world.createSettlement("Town Alpha", 5, 0);
        if (s2 != null) {
            s2.specialize(Specialization.LOGGING_CAMP);
            s2.population = 20;
        }
        
        world.reveal(-6, 4);
        Settlement s3 = world.createSettlement("Mining Camp", -6, 4);
        if (s3 != null) {
            s3.specialize(Specialization.MINING_TOWN);
            s3.population = 15;
        }
        
        world.reveal(0, -7);
        Settlement s4 = world.createSettlement("Farming Village", 0, -7);
        if (s4 != null) {
            s4.specialize(Specialization.FARMING_VILLAGE);
            s4.population = 25;
        }
        
        for (int x = 0; x <= 2; x++) {
            for (int y = 0; y <= 2; y++) {
                world.placeRoad(x, y, RoadType.DIRT);
            }
        }
        for (int x = -6; x <= -4; x++) {
            for (int y = 3; y <= 4; y++) {
                world.placeRoad(x, y, RoadType.DIRT);
            }
        }
    }
    
    public void run() {
        System.out.println("=== Running Economy Simulation ===");
        System.out.println("Duration: " + ticks + " ticks (" + (ticks / 60.0) + " sim-minutes)");
        System.out.println("Settlements: " + world.getSettlements().size());
        System.out.println();
        
        for (Settlement s : world.getSettlements()) {
            trackers.put(s.name, new BalanceTracker(s.name));
        }
        
        for (int i = 0; i < ticks; i++) {
            simulation.tick(1.0f);
            simulation.updateCaravans(1.0f / 60f);
            
            for (Settlement s : world.getSettlements()) {
                trackers.get(s.name).record(s, config);
            }
            
            if ((i + 1) % 100 == 0) {
                System.out.print(".");
            }
        }
        System.out.println();
        System.out.println();
        
        printAnalysis();
    }
    
    private void printAnalysis() {
        System.out.println("=== BALANCE ANALYSIS ===");
        System.out.println();
        
        boolean hasIssues = false;
        
        for (BalanceTracker tracker : trackers.values()) {
            System.out.println("--- " + tracker.settlementName + " ---");
            
            if (tracker.avgFoodBalance < 0) {
                System.out.println("  [ISSUE] Food deficit: avg " + String.format("%.2f", tracker.avgFoodBalance) + "/tick");
                System.out.println("    -> Food production insufficient for population");
                System.out.println("    -> Consider: increase GRASS_FOOD, adjust growth_rate, or add food bonuses");
                hasIssues = true;
            } else {
                System.out.println("  [OK] Food balance: avg " + String.format("%.2f", tracker.avgFoodBalance) + "/tick");
            }
            
            if (tracker.sampleCount > 0) {
                tracker.populationGrowthRate = (tracker.finalPop - tracker.initialPop) / (double) tracker.sampleCount;
            }
            if (tracker.populationGrowthRate < 0.01 && tracker.finalPop > 10 && tracker.initialPop < tracker.finalPop) {
                System.out.println("  [ISSUE] Slow growth: " + String.format("%.2f", tracker.populationGrowthRate * 100) + "% per tick");
                System.out.println("    -> Population struggling to grow");
                System.out.println("    -> Consider: increase growth_rate or food production");
                hasIssues = true;
            }
            
            if (tracker.starvationTicks > ticks * 0.1) {
                System.out.println("  [ISSUE] Starvation: " + tracker.starvationTicks + " ticks with starvation");
                System.out.println("    -> Population dying frequently");
                System.out.println("    -> Consider: reduce starvation_rate or increase food production");
                hasIssues = true;
            }
            
            if (tracker.avgGoldBalance < 0) {
                System.out.println("  [ISSUE] Gold deficit: avg " + String.format("%.2f", tracker.avgGoldBalance) + "/tick");
                System.out.println("    -> Not enough gold income");
                System.out.println("    -> Consider: adjust trade prices or caravan revenue");
                hasIssues = true;
            }
            
            double woodProd = tracker.avgWoodProd;
            double stoneProd = tracker.avgStoneProd;
            if (woodProd < 1.0 && tracker.finalPop > 30) {
                System.out.println("  [ISSUE] Low wood production: " + String.format("%.1f", woodProd) + "/tick");
                System.out.println("    -> Insufficient for building construction");
                System.out.println("    -> Consider: increase FOREST_WOOD or add specialization bonus");
                hasIssues = true;
            }
            if (stoneProd < 0.5 && tracker.finalPop > 30) {
                System.out.println("  [ISSUE] Low stone production: " + String.format("%.1f", stoneProd) + "/tick");
                System.out.println("    -> Insufficient for upgrades");
                System.out.println("    -> Consider: increase STONE_STONE or add specialization bonus");
                hasIssues = true;
            }
            
            double priceVariation = tracker.maxPrice - tracker.minPrice;
            if (priceVariation > 1.0) {
                System.out.println("  [ISSUE] Price volatility: " + String.format("%.2f", priceVariation) + " range");
                System.out.println("    -> Prices fluctuating too much");
                System.out.println("    -> Consider: adjust price_lerp_alpha or price bounds");
                hasIssues = true;
            }
            
            if (tracker.tradeRevenue > 0) {
                System.out.println("  [OK] Trade revenue: " + String.format("%.1f", tracker.tradeRevenue) + " gold total");
            }
            
            System.out.println();
        }
        
        printConfigRecommendations();
        
        if (!hasIssues) {
            System.out.println("[SUCCESS] No major balance issues detected!");
        }
    }
    
    private void printConfigRecommendations() {
        System.out.println("=== CONFIGURATION RECOMMENDATIONS ===");
        System.out.println();
        
        System.out.println("To adjust in application.yml:");
        System.out.println();
        
        System.out.println("# Food balance:");
        System.out.println("simulation:");
        System.out.println("  food_demand_per_pop: 0.15   # Higher = harder to feed population");
        System.out.println("  growth_rate: 0.01           # Higher = faster population growth");
        System.out.println("  starvation_rate: 0.02       # Higher = faster death when starving");
        System.out.println("  terrain_production:");
        System.out.println("    GRASS_FOOD: 0.5            # Increase if food deficits");
        System.out.println();
        
        System.out.println("# Production balance:");
        System.out.println("  terrain_production:");
        System.out.println("    FOREST_WOOD: 2.0           # Increase if wood shortages");
        System.out.println("    STONE_STONE: 1.0           # Increase if stone shortages");
        System.out.println();
        
        System.out.println("# Price stability:");
        System.out.println("  price_lerp_alpha: 0.2       # Lower = slower price changes");
        System.out.println("  price_min: 0.5              # Price floor");
        System.out.println("  price_max: 2.0              # Price ceiling");
        System.out.println();
        
        System.out.println("# Trade/caravan:");
        System.out.println("trade:");
        System.out.println("  max_caravans_per_route: 3   # More = more trade");
        System.out.println("  base_spawn_interval: 120.0  # Lower = more frequent caravans");
    }
    
    private static class BalanceTracker {
        final String settlementName;
        
        double totalFoodProd = 0;
        double totalFoodCons = 0;
        double avgFoodBalance = 0;
        
        double totalWoodProd = 0;
        double totalStoneProd = 0;
        double totalGoodsProd = 0;
        double avgWoodProd = 0;
        double avgStoneProd = 0;
        
        double prevGold = 0;
        double totalGoldChange = 0;
        double avgGoldBalance = 0;
        
        int initialPop = 0;
        int finalPop = 0;
        int maxPop = 0;
        double populationGrowthRate = 0;
        
        int starvationTicks = 0;
        
        double tradeRevenue = 0;
        
        double minPrice = Float.MAX_VALUE;
        double maxPrice = 0;
        
        int sampleCount = 0;
        
        BalanceTracker(String name) {
            this.settlementName = name;
        }
        
        void record(Settlement s, GameConfig config) {
            float foodDemand = s.population * config.getFoodDemandPerPop();
            double foodBalance = s.smoothedFoodProd - foodDemand;
            
            totalFoodProd += s.smoothedFoodProd;
            totalFoodCons += foodDemand;
            avgFoodBalance = (avgFoodBalance * sampleCount + foodBalance) / (sampleCount + 1);
            
            totalWoodProd += s.smoothedWoodProd;
            totalStoneProd += s.smoothedStoneProd;
            totalGoodsProd += s.smoothedGoodsProd;
            avgWoodProd = totalWoodProd / (sampleCount + 1);
            avgStoneProd = totalStoneProd / (sampleCount + 1);
            
            double goldChange = s.gold - prevGold;
            prevGold = s.gold;
            totalGoldChange += goldChange;
            avgGoldBalance = totalGoldChange / (sampleCount + 1);
            
            if (s.food <= 0 && foodBalance < -0.1) {
                starvationTicks++;
            }
            
            if (sampleCount == 0) {
                initialPop = s.population;
                prevGold = s.gold;
            }
            finalPop = s.population;
            maxPop = Math.max(maxPop, s.population);
            
            float avgPrice = (s.getCurrentPrice(ResourceType.FOOD) + 
                            s.getCurrentPrice(ResourceType.WOOD) + 
                            s.getCurrentPrice(ResourceType.STONE)) / 3;
            minPrice = Math.min(minPrice, avgPrice);
            maxPrice = Math.max(maxPrice, avgPrice);
            
            sampleCount++;
        }
    }
    
    public static void main(String[] args) {
        runSimulation(500);
    }
    
    public static void runSimulation(int ticks) {
        GameConfig config = new GameConfig(new GameConfig.Root());
        
        SimulationRunner runner = new SimulationRunner(config, ticks);
        runner.setupDefaultScenario();
        runner.run();
    }
}
