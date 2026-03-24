package ch.obermuhlner.sim;

import ch.obermuhlner.sim.game.*;
import ch.obermuhlner.sim.game.systems.SimulationSystem;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SimulationRunner {
    private final GameConfig config;
    private final SimulationOptions options;
    
    public SimulationRunner(GameConfig config, SimulationOptions options) {
        this.config = config;
        this.options = options;
    }
    
    public static void main(String[] args) {
        SimulationOptions opts = parseArguments(args);
        if (opts.help) {
            printHelp();
            return;
        }
        
        runMultipleSimulations(opts);
    }
    
    private static SimulationOptions parseArguments(String[] args) {
        SimulationOptions opts = new SimulationOptions();
        
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--runs":
                case "-n":
                    if (i + 1 < args.length) {
                        opts.runs = Math.max(1, Math.min(100, Integer.parseInt(args[++i])));
                    }
                    break;
                case "--ticks":
                case "-t":
                    if (i + 1 < args.length) {
                        opts.ticks = Math.max(10, Integer.parseInt(args[++i]));
                    }
                    break;
                case "--seed":
                case "-s":
                    if (i + 1 < args.length) {
                        String seedStr = args[++i];
                        if (!seedStr.equalsIgnoreCase("random")) {
                            opts.seed = Long.parseLong(seedStr);
                            opts.seedSet = true;
                        }
                    }
                    break;
                case "--verbose":
                case "-v":
                    opts.verbose = true;
                    break;
                case "--quiet":
                case "-q":
                    opts.quiet = true;
                    break;
                case "--scenario":
                case "--scen":
                    if (i + 1 < args.length) {
                        opts.scenario = args[++i];
                    }
                    break;
                case "--research":
                case "-r":
                    opts.enableResearch = true;
                    break;
                case "--help":
                case "-h":
                    opts.help = true;
                    break;
                default:
                    System.out.println("Unknown argument: " + arg);
                    opts.help = true;
            }
        }
        return opts;
    }
    
    private static void printHelp() {
        System.out.println("Usage: SimulationRunner [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --runs N, -n N       Number of simulations to run (default: 1, max: 100)");
        System.out.println("  --ticks N, -t N     Ticks per simulation (default: 500)");
        System.out.println("  --seed N, -s N      World seed (default: random, or specific number)");
        System.out.println("  --verbose, -v       Show detailed per-settlement output");
        System.out.println("  --quiet, -q         Suppress individual runs, show only summary");
        System.out.println("  --help, -h          Show this help");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  SimulationRunner                     # Run single simulation");
        System.out.println("  SimulationRunner --runs 5            # Run 5 simulations");
        System.out.println("  SimulationRunner --runs 10 --ticks 1000  # 10 long simulations");
        System.out.println("  SimulationRunner -n 5 -v             # Verbose output");
        System.out.println("  SimulationRunner -s 12345            # Reproducible seed");
    }
    
    public static void runSimulation(int ticks) {
        SimulationOptions opts = new SimulationOptions();
        opts.ticks = ticks;
        runMultipleSimulations(opts);
    }
    
    private static void runMultipleSimulations(SimulationOptions opts) {
        GameConfig config = loadConfigFromYaml();
        
        List<SimulationResult> results = new ArrayList<>();
        Random seedRandom = new Random();
        
        for (int run = 1; run <= opts.runs; run++) {
            long seed = opts.seedSet ? opts.seed : seedRandom.nextLong();
            
            if (!opts.quiet || opts.verbose) {
                System.out.println();
                System.out.println("=== Run " + run + "/" + opts.runs + " (seed=" + seed + ") ===");
            }
            
            SimulationRunner runner = new SimulationRunner(config, opts);
            SimulationResult result = runner.runSingleSimulation(seed);
            results.add(result);
            
            if (opts.verbose) {
                result.printDetailedOutput();
            } else if (!opts.quiet) {
                result.printSummary();
            }
        }
        
        printAggregatedResults(results, opts);
    }
    
    private static String rns(int n) {
        return String.valueOf(n);
    }
    
    private SimulationResult runSingleSimulation(long seed) {
        config.setWorldSeed(seed);
        World world = new World(16, config, true);
        SimulationSystem simulation = new SimulationSystem(world, config);
        
        setupScenario(world, options.scenario);
        
        int settlementCount = world.getSettlements().size();
        SimulationResult result = new SimulationResult(seed, settlementCount);
        
        Map<String, BalanceTracker> trackers = new HashMap<>();
        for (Settlement s : world.getSettlements()) {
            trackers.put(s.name, new BalanceTracker(s.name));
        }
        
        for (int i = 0; i < options.ticks; i++) {
            simulation.tick(1.0f);
            simulation.updateCaravans(1.0f / 60f);
            
            if (options.enableResearch) {
                simulateResearch(world, config, i);
            }
            
            for (Settlement s : world.getSettlements()) {
                trackers.get(s.name).record(s, config);
            }
            
            if (!options.quiet && (i + 1) % 100 == 0) {
                System.out.print(".");
            }
        }
        
        if (options.enableResearch) {
            printResearchResults(world, result);
        }

        result.tradeRouteCount = world.getTradeRoutes().size();

        if (!options.quiet) {
            System.out.println();
        }
        
        for (Map.Entry<String, BalanceTracker> entry : trackers.entrySet()) {
            result.addSettlementResult(entry.getKey(), entry.getValue());
        }
        
        return result;
    }
    
    public void setupScenario(World world, String scenario) {
        switch (scenario.toLowerCase()) {
            case "tech-test":
            case "tech":
                setupTechTestScenario(world);
                break;
            case "default":
            default:
                setupDefaultScenario(world);
                break;
        }
    }
    
    public void setupDefaultScenario(World world) {
        world.createStarterSettlement();
        
        world.reveal(8, 0);
        Settlement s2 = world.createSettlement("Town Alpha", 8, 0);
        if (s2 != null) {
            s2.specialize(Specialization.LOGGING_CAMP);
            s2.population = 20;
            s2.gold = 200;
            addLoggingBuildings(s2);
        }
        
        world.reveal(-10, 6);
        Settlement s3 = world.createSettlement("Mining Camp", -10, 6);
        if (s3 != null) {
            s3.specialize(Specialization.MINING_TOWN);
            s3.population = 15;
            s3.gold = 200;
            addMiningBuildings(s3);
        }
        
        world.reveal(3, -8);
        Settlement s4 = world.createSettlement("Farming Village", 3, -8);
        if (s4 != null) {
            s4.specialize(Specialization.FARMING_VILLAGE);
            s4.population = 25;
            s4.gold = 200;
            addFarmingBuildings(s4);
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
    
    public void setupTechTestScenario(World world) {
        world.createStarterSettlement();
        
        Settlement starterSettlement = world.getSettlements().get(0);
        starterSettlement.gold = 200;
        int starterX = starterSettlement.centerX;
        int starterY = starterSettlement.centerY;
        
        Settlement loggingCamp = createSettlementAtDistance(world, "Logging Town", starterX, starterY, 6, 2, Specialization.LOGGING_CAMP);
        Settlement miningCamp = createSettlementAtDistance(world, "Mining Town", starterX, starterY, -6, 5, Specialization.MINING_TOWN);
        Settlement farmingVillage = createSettlementAtDistance(world, "Farming Town", starterX, starterY, 0, -6, Specialization.FARMING_VILLAGE);
        Settlement tradeHub = createSettlementAtDistance(world, "Trade Hub", starterX, starterY, -4, -4, Specialization.TRADE_HUB);

        buildRoadsBetweenSettlements(world, starterSettlement, loggingCamp, miningCamp, farmingVillage, tradeHub);

        if (loggingCamp != null) loggingCamp.gold = 200;
        if (miningCamp != null) miningCamp.gold = 200;
        if (farmingVillage != null) farmingVillage.gold = 200;
        if (tradeHub != null) tradeHub.gold = 200;

        addBasicBuildings(loggingCamp);
        addBasicBuildings(miningCamp);
        addBasicBuildings(farmingVillage);
        addBasicBuildings(tradeHub);
        addBasicBuildings(starterSettlement);
    }
    
    private Settlement createSettlementAtDistance(World world, String name, int fromX, int fromY, int offsetX, int offsetY, Specialization spec) {
        int targetX = fromX + offsetX;
        int targetY = fromY + offsetY;
        
        for (int radius = 0; radius < 10; radius++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    if (Math.abs(dx) != radius && Math.abs(dy) != radius) continue;
                    int tx = targetX + dx;
                    int ty = targetY + dy;
                    world.reveal(tx, ty);
                    Settlement s = world.createSettlement(name, tx, ty);
                    if (s != null) {
                        s.population = 50;
                        s.specialize(spec);
                        return s;
                    }
                }
            }
        }
        return null;
    }
    
    private void buildRoadsBetweenSettlements(World world, Settlement... settlements) {
        for (Settlement from : settlements) {
            if (from == null) continue;
            for (Settlement to : settlements) {
                if (to == null || to == from) continue;
                
                int dx = Integer.signum(to.centerX - from.centerX);
                int dy = Integer.signum(to.centerY - from.centerY);
                
                int x = from.centerX;
                int y = from.centerY;
                while (x != to.centerX || y != to.centerY) {
                    world.placeRoad(x, y, RoadType.DIRT);
                    if (x != to.centerX) x += dx;
                    else if (y != to.centerY) y += dy;
                }
                world.placeRoad(to.centerX, to.centerY, RoadType.DIRT);
            }
        }
    }
    
    private void simulateResearch(World world, GameConfig config, int tick) {
        if (!world.techTree.hasActiveResearch()) {
            if (tick % 50 == 0 && tick > 0) {
                tryAutoResearch(world, world.techTree, world.getSettlements(), config);
            }
            return;
        }
        
        float researchGoldPerTick = config.getTechTreeConfig().research_gold_per_tick;
        
        for (Settlement s : world.getSettlements()) {
            float goldToResearch = Math.min(s.gold, researchGoldPerTick);
            s.gold -= goldToResearch;
            world.techTree.addProgress(goldToResearch, config);
        }
        
        if (tick % 50 == 0 && tick > 0) {
            tryAutoResearch(world, world.techTree, world.getSettlements(), config);
        }
    }
    
    private void tryAutoResearch(World world, TechTree techTree, List<Settlement> settlements, GameConfig config) {
        if (techTree.hasActiveResearch()) {
            return;
        }
        
        SettlementLevel maxLevel = config.getFirstLevel();
        for (Settlement s : settlements) {
            if (s.getLevel().ordinal() > maxLevel.ordinal()) {
                maxLevel = s.getLevel();
            }
        }
        
        for (GameConfig.CrossSpecializationTechConfig csTech : config.getAllCrossSpecializationTechs()) {
            if (techTree.canResearchCrossSpecialization(csTech, settlements) && !techTree.isResearched(csTech.id)) {
                techTree.researchCrossSpecialization(csTech.id);
                return;
            }
        }
        
        for (GameConfig.ConditionalTechConfig condTech : config.getAllConditionalTechs()) {
            if (techTree.canResearchConditional(condTech, settlements, world.getActiveTradeRouteCount()) && !techTree.isResearched(condTech.id)) {
                techTree.researchConditional(condTech.id);
                return;
            }
        }
        
        for (GameConfig.TechConfig tech : config.getAllTechs()) {
            if (techTree.canResearch(tech, settlements, maxLevel, config)) {
                techTree.startResearch(tech.id);
                return;
            }
        }
    }
    
    private void printResearchResults(World world, SimulationResult result) {
        System.out.println();
        System.out.println("=== Research Results ===");
        System.out.println("Techs researched: " + world.techTree.getResearchedTechs());
        System.out.println("Active research: " + world.techTree.getActiveResearchId());
        System.out.println("Progress: " + world.techTree.getResearchProgress());
        
        System.out.println();
        System.out.println("=== Tech Tree Expansion Test ===");
        List<Settlement> settlements = world.getSettlements();
        System.out.println("Settlement specializations: " + settlements.stream()
            .map(s -> s.name + ":" + s.specialization.name()).collect(java.util.stream.Collectors.toList()));
        for (GameConfig.CrossSpecializationTechConfig csTech : config.getAllCrossSpecializationTechs()) {
            boolean researched = world.techTree.isResearched(csTech.id);
            boolean canResearch = world.techTree.canResearchCrossSpecialization(csTech, settlements);
            String lockHint = !researched && !canResearch ? 
                " (requires: " + csTech.requires + ")" : "";
            System.out.printf("  %s: researched=%s, canResearch=%s%s%n", 
                csTech.id, researched, canResearch, lockHint);
        }
        for (GameConfig.ConditionalTechConfig condTech : config.getAllConditionalTechs()) {
            boolean researched = world.techTree.isResearched(condTech.id);
            boolean canResearch = world.techTree.canResearchConditional(condTech, world.getSettlements(), world.getActiveTradeRouteCount());
            System.out.printf("  %s: researched=%s, canResearch=%s (condition: %s)%n", 
                condTech.id, researched, canResearch, condTech.condition);
        }
    }
    
    public void placeBuilding(Settlement s, BuildingType type, int offsetX, int offsetY) {
        if (s != null) {
            s.addBuilding(type.getId());
            s.addPopulation(type.getPopulationCapacity());
        }
    }
    
    public void addFarmingBuildings(Settlement s) {
        placeBuilding(s, BuildingType.FARM_SMALL, 1, 0);
        placeBuilding(s, BuildingType.FARM_SMALL, 0, 1);
        placeBuilding(s, BuildingType.FARM_SMALL, -1, 0);
    }
    
    public void addHousingBuildings(Settlement s) {
        placeBuilding(s, BuildingType.HOUSE_SIMPLE, 1, 0);
        placeBuilding(s, BuildingType.HOUSE_SIMPLE, 0, 1);
        placeBuilding(s, BuildingType.HOUSE_LARGE, -1, 0);
    }
    
    public void addMarketBuildings(Settlement s) {
        placeBuilding(s, BuildingType.MARKET_SMALL, 0, 0);
    }
    
    public void addMiningBuildings(Settlement s) {
        placeBuilding(s, BuildingType.FORGE_SMALL, 0, 0);
    }
    
    public void addLoggingBuildings(Settlement s) {
        placeBuilding(s, BuildingType.WAREHOUSE, 0, 0);
    }
    
    public void addBasicBuildings(Settlement s) {
        placeBuilding(s, BuildingType.HOUSE_SIMPLE, 1, 0);
        placeBuilding(s, BuildingType.WELL_WATER, 0, 1);
        placeBuilding(s, BuildingType.WAREHOUSE, -1, 0);
    }
    
    private static void printAggregatedResults(List<SimulationResult> results, SimulationOptions opts) {
        if (results.isEmpty()) return;
        
        System.out.println();
        System.out.println("=== AGGREGATED RESULTS (" + results.size() + " runs) ===");
        System.out.println();
        
        Set<String> allSettlements = new TreeSet<>();
        for (SimulationResult r : results) {
            allSettlements.addAll(r.settlementResults.keySet());
        }
        
        System.out.println("Duration: " + opts.ticks + " ticks per simulation");
        System.out.println("World Seeds: " + results.stream().map(r -> String.valueOf(r.seed)).reduce((a, b) -> a + ", " + b).orElse("none"));
        System.out.println();
        
        System.out.println("--- Food Balance (avg per tick) ---");
        for (String settlement : allSettlements) {
            List<Double> values = new ArrayList<>();
            for (SimulationResult r : results) {
                SettlementResult sr = r.settlementResults.get(settlement);
                if (sr != null) {
                    values.add(sr.foodBalance);
                }
            }
            if (!values.isEmpty()) {
                System.out.printf("  %-20s: mean=%7.2f, min=%7.2f, max=%7.2f, std=%6.2f%n",
                    settlement, mean(values), min(values), max(values), stdDev(values));
            }
        }
        System.out.println();
        
        System.out.println("--- Price Volatility (range) ---");
        for (String settlement : allSettlements) {
            List<Double> values = new ArrayList<>();
            for (SimulationResult r : results) {
                SettlementResult sr = r.settlementResults.get(settlement);
                if (sr != null) {
                    values.add(sr.priceVolatility);
                }
            }
            if (!values.isEmpty()) {
                System.out.printf("  %-20s: mean=%7.2f, min=%7.2f, max=%7.2f, std=%6.2f%n",
                    settlement, mean(values), min(values), max(values), stdDev(values));
            }
        }
        System.out.println();
        
        System.out.println("--- Starvation Ticks ---");
        for (String settlement : allSettlements) {
            List<Integer> values = new ArrayList<>();
            for (SimulationResult r : results) {
                SettlementResult sr = r.settlementResults.get(settlement);
                if (sr != null) {
                    values.add(sr.starvationTicks);
                }
            }
            if (!values.isEmpty()) {
                System.out.printf("  %-20s: mean=%7.1f, min=%7d, max=%7d%n",
                    settlement, meanInt(values), minInt(values), maxInt(values));
            }
        }
        System.out.println();
        
        System.out.println("--- Trade Revenue (total gold) ---");
        for (String settlement : allSettlements) {
            List<Double> values = new ArrayList<>();
            for (SimulationResult r : results) {
                SettlementResult sr = r.settlementResults.get(settlement);
                if (sr != null) {
                    values.add(sr.tradeRevenue);
                }
            }
            if (!values.isEmpty()) {
                System.out.printf("  %-20s: mean=%7.1f, min=%7.1f, max=%7.1f%n",
                    settlement, mean(values), min(values), max(values));
            }
        }
        System.out.println();
        
        System.out.println("--- Final Population ---");
        for (String settlement : allSettlements) {
            List<Integer> values = new ArrayList<>();
            for (SimulationResult r : results) {
                SettlementResult sr = r.settlementResults.get(settlement);
                if (sr != null) {
                    values.add(sr.finalPopulation);
                }
            }
            if (!values.isEmpty()) {
                System.out.printf("  %-20s: mean=%7.1f, min=%7d, max=%7d%n",
                    settlement, meanInt(values), minInt(values), maxInt(values));
            }
        }
        System.out.println();
        
        printOverallIssues(results, allSettlements);
    }
    
    private static void printOverallIssues(List<SimulationResult> results, Set<String> settlements) {
        System.out.println("--- Overall Issues ---");
        
        for (String settlement : settlements) {
            List<Double> foodBalances = new ArrayList<>();
            List<Integer> starvationTicks = new ArrayList<>();
            List<Double> priceVol = new ArrayList<>();
            
            for (SimulationResult r : results) {
                SettlementResult sr = r.settlementResults.get(settlement);
                if (sr != null) {
                    foodBalances.add(sr.foodBalance);
                    starvationTicks.add(sr.starvationTicks);
                    priceVol.add(sr.priceVolatility);
                }
            }
            
            int deficitCount = 0;
            for (Double fb : foodBalances) {
                if (fb < 0) deficitCount++;
            }
            if (deficitCount > 0) {
                System.out.printf("  %s food deficit: appears in %d/%d runs%n", 
                    settlement, deficitCount, results.size());
            }
            
            int starvationCount = 0;
            for (Integer st : starvationTicks) {
                if (st > 0) starvationCount++;
            }
            if (starvationCount > 0) {
                System.out.printf("  %s starvation: appears in %d/%d runs%n",
                    settlement, starvationCount, results.size());
            }
            
            int priceVolCount = 0;
            for (Double pv : priceVol) {
                if (pv > 1.0) priceVolCount++;
            }
            if (priceVolCount > 0) {
                System.out.printf("  %s price volatility >1.0: appears in %d/%d runs%n",
                    settlement, priceVolCount, results.size());
            }
        }
        System.out.println();
    }
    
    private static double mean(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }
    
    private static double meanInt(List<Integer> values) {
        return values.stream().mapToInt(Integer::intValue).average().orElse(0);
    }
    
    private static double min(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).min().orElse(0);
    }
    
    private static int minInt(List<Integer> values) {
        return values.stream().mapToInt(Integer::intValue).min().orElse(0);
    }
    
    private static double max(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).max().orElse(0);
    }
    
    private static int maxInt(List<Integer> values) {
        return values.stream().mapToInt(Integer::intValue).max().orElse(0);
    }
    
    private static double stdDev(List<Double> values) {
        if (values.size() < 2) return 0;
        double mean = mean(values);
        double variance = values.stream()
            .mapToDouble(v -> (v - mean) * (v - mean))
            .average()
            .orElse(0);
        return Math.sqrt(variance);
    }
    
    private static GameConfig loadConfigFromYaml() {
        String workingDir = System.getProperty("user.dir");
        File projectRoot = new File(workingDir);
        if (!new File(projectRoot, "assets/application.yml").exists()) {
            projectRoot = projectRoot.getParentFile();
        }
        File configFile = new File(projectRoot, "assets/application.yml");
        if (!configFile.exists()) {
            System.out.println("Warning: assets/application.yml not found at " + configFile.getAbsolutePath() + ", using defaults");
            return new GameConfig(new GameConfig.Root());
        }
        
        try (InputStream is = new FileInputStream(configFile)) {
            Yaml yaml = new Yaml();
            Map<String, Object> raw = yaml.load(is);
            if (raw == null) {
                return new GameConfig(new GameConfig.Root());
            }
            
            GameConfig gameConfig = new GameConfig(new GameConfig.Root());
            gameConfig.loadFromMap(raw);
            System.out.println("Loaded configuration from assets/application.yml");
            return gameConfig;
        } catch (Exception e) {
            System.out.println("Warning: Failed to load application.yml: " + e.getMessage());
            return new GameConfig(new GameConfig.Root());
        }
    }
    
    static class SimulationOptions {
        int runs = 1;
        int ticks = 500;
        long seed = 42;
        boolean seedSet = false;
        boolean verbose = false;
        boolean quiet = false;
        boolean help = false;
        String scenario = "default";
        boolean enableResearch = false;
    }
    
    static class SimulationResult {
        long seed;
        int settlementCount;
        int tradeRouteCount;
        Map<String, SettlementResult> settlementResults = new LinkedHashMap<>();
        
        SimulationResult(long seed, int settlementCount) {
            this.seed = seed;
            this.settlementCount = settlementCount;
        }
        
        void addSettlementResult(String name, BalanceTracker tracker) {
            settlementResults.put(name, new SettlementResult(tracker));
        }
        
        void printDetailedOutput() {
            System.out.println("Duration: " + 500 + " ticks (" + (500 / 60.0) + " sim-minutes)");
            System.out.println("Settlements: " + settlementCount);
            System.out.println("Trade Routes: " + tradeRouteCount);
            System.out.println();
            
            for (Map.Entry<String, SettlementResult> entry : settlementResults.entrySet()) {
                System.out.println("--- " + entry.getKey() + " ---");
                entry.getValue().printDetails();
                System.out.println();
            }
        }
        
        void printSummary() {
            System.out.println("Settlements: " + settlementCount);
            for (Map.Entry<String, SettlementResult> entry : settlementResults.entrySet()) {
                String status = entry.getValue().foodBalance >= 0 ? "[OK]" : "[ISSUE]";
                System.out.printf("  %s %s: food=%.2f, priceVol=%.2f, starve=%d%n",
                    status, entry.getKey(), 
                    entry.getValue().foodBalance,
                    entry.getValue().priceVolatility,
                    entry.getValue().starvationTicks);
            }
        }
    }
    
    static class SettlementResult {
        double foodBalance;
        double priceVolatility;
        int starvationTicks;
        double tradeRevenue;
        int finalPopulation;
        double populationGrowth;
        
        SettlementResult(BalanceTracker tracker) {
            this.foodBalance = tracker.avgFoodBalance;
            this.priceVolatility = tracker.maxPrice - tracker.minPrice;
            this.starvationTicks = tracker.starvationTicks;
            this.tradeRevenue = tracker.tradeRevenue;
            this.finalPopulation = tracker.finalPop;
            this.populationGrowth = tracker.finalPop - tracker.initialPop;
        }
        
        void printDetails() {
            if (foodBalance < 0) {
                System.out.println("  [ISSUE] Food deficit: avg " + String.format("%.2f", foodBalance) + "/tick");
            } else {
                System.out.println("  [OK] Food balance: avg " + String.format("%.2f", foodBalance) + "/tick");
            }
            
            if (priceVolatility > 1.0) {
                System.out.println("  [ISSUE] Price volatility: " + String.format("%.2f", priceVolatility) + " range");
            } else {
                System.out.println("  [OK] Price volatility: " + String.format("%.2f", priceVolatility) + " range");
            }
            
            if (starvationTicks > 0) {
                System.out.println("  [ISSUE] Starvation: " + starvationTicks + " ticks");
            }
            
            if (tradeRevenue > 0) {
                System.out.println("  [OK] Trade revenue: " + String.format("%.1f", tradeRevenue) + " gold");
            }
            
            System.out.println("  Population: " + finalPopulation + " (growth: " + (populationGrowth >= 0 ? "+" : "") + (int)populationGrowth + ")");
        }
    }
    
    static class BalanceTracker {
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
        double prevTradeRevenue = 0;
        
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
                prevTradeRevenue = s.tradeRevenue;
            } else {
                tradeRevenue += s.tradeRevenue - prevTradeRevenue;
            }
            prevTradeRevenue = s.tradeRevenue;
            
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
}
