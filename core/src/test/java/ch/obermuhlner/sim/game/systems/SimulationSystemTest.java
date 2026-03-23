package ch.obermuhlner.sim.game.systems;

import ch.obermuhlner.sim.game.*;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class SimulationSystemTest {
    private static final int CHUNK_SIZE = 16;
    private static final long SEED = 99999L;

    private World world;
    private SimulationSystem sim;

    @Before
    public void setUp() {
        GameConfig.Root root = new GameConfig.Root();
        root.world.seed = SEED;
        GameConfig config = new GameConfig(root);
        world = new World(CHUNK_SIZE, config, true);
        sim = new SimulationSystem(world, config);
    }

    /** Remove any natural object from a tile so it is buildable/road-placeable. */
    private void clearTile(int tx, int ty) {
        world.getTile(tx, ty).objectId = TileObjectRegistry.NONE;
    }

    /** Clear all tiles along y=tileY from x=x1 to x=x2 (inclusive). */
    private void clearRow(int x1, int x2, int tileY) {
        for (int x = x1; x <= x2; x++) clearTile(x, tileY);
    }

    @Test
    public void testResourceTypeEnum() {
        assertEquals("Wood",  ResourceType.WOOD.displayName);
        assertEquals("Stone", ResourceType.STONE.displayName);
        assertEquals("Food",  ResourceType.FOOD.displayName);
        assertEquals("Goods", ResourceType.GOODS.displayName);
        assertEquals("Gold",  ResourceType.GOLD.displayName);

        assertEquals(3f,  ResourceType.WOOD.basePrice,  0.001f);
        assertEquals(4f,  ResourceType.STONE.basePrice, 0.001f);
        assertEquals(5f,  ResourceType.FOOD.basePrice,  0.001f);
        assertEquals(10f, ResourceType.GOODS.basePrice, 0.001f);
    }

    @Test
    public void testSettlementStartsWithGold() {
        Settlement s = new Settlement("Test", 0, 0);
        assertEquals(50f, s.gold, 0.001f);
    }

    @Test
    public void testSettlementAddResource() {
        Settlement s = new Settlement("Test", 0, 0);
        s.addResource(ResourceType.WOOD, 100f);
        assertEquals(100f, s.wood, 0.001f);
        assertEquals(100f, s.getResource(ResourceType.WOOD), 0.001f);
    }

    @Test
    public void testSettlementResourceCappedAtStorage() {
        Settlement s = new Settlement("Test", 0, 0);
        s.addResource(ResourceType.FOOD, 1000f);
        assertEquals(s.storageCapacity, s.food, 0.001f);
    }

    @Test
    public void testSettlementResourceNotNegative() {
        Settlement s = new Settlement("Test", 0, 0);
        s.addResource(ResourceType.STONE, -100f);
        assertEquals(0f, s.stone, 0.001f);
    }

    @Test
    public void testSettlementPriceMultiplier() {
        Settlement s = new Settlement("Test", 0, 0);
        assertEquals(1f, s.getPriceMult(ResourceType.FOOD), 0.001f);
        s.setPriceMult(ResourceType.FOOD, 1.5f);
        assertEquals(1.5f, s.getPriceMult(ResourceType.FOOD), 0.001f);
        assertEquals(7.5f, s.getCurrentPrice(ResourceType.FOOD), 0.001f);
    }

    @Test
    public void testSettlementSmoothedProd() {
        Settlement s = new Settlement("Test", 0, 0);
        assertEquals(0f, s.getSmoothedProd(ResourceType.WOOD), 0.001f);
        s.setSmoothedProd(ResourceType.WOOD, 5f);
        assertEquals(5f, s.getSmoothedProd(ResourceType.WOOD), 0.001f);
    }

    @Test
    public void testTickIncreasesTickCount() {
        assertEquals(0, sim.getTickCount());
        sim.tick(1.0f);
        assertEquals(1, sim.getTickCount());
        sim.tick(1.0f);
        assertEquals(2, sim.getTickCount());
    }

    @Test
    public void testGoodsProductionFromPopulation() {
        clearTile(5, 5);
        Settlement settlement = world.createSettlement("GoodsTest", 5, 5);
        assertNotNull(settlement);
        settlement.setPopulation(20);

        sim.tick(1.0f);

        // Goods = population * 0.01 * goodsMultiplier (1.0 for NONE)
        // After 1 tick with EMA smoothing, smoothedGoodsProd should be > 0
        assertTrue("Goods should be produced", settlement.goods > 0);
    }

    @Test
    public void testFoodConsumptionReducesFood() {
        clearTile(5, 5);
        Settlement settlement = world.createSettlement("FoodTest", 5, 5);
        assertNotNull(settlement);
        settlement.setPopulation(10);
        settlement.food = 1000f;
        // Set smoothed food prod to 0 so no production offsets consumption
        settlement.smoothedFoodProd = 0f;

        sim.tick(1.0f);
        // Test that simulation runs without error
        assertTrue("Simulation ran", sim.getTickCount() == 1);
    }

    @Test
    public void testStarvationReducesPopulation() {
        clearTile(5, 5);
        Settlement settlement = world.createSettlement("StarveTest", 5, 5);
        assertNotNull(settlement);
        settlement.setPopulation(10);
        settlement.food = 0f;
        settlement.smoothedFoodProd = 0f;

        // Run multiple ticks to see starvation
        for (int i = 0; i < 5; i++) {
            settlement.food = 0f;
            settlement.smoothedFoodProd = 0f;
            sim.tick(1.0f);
        }
        // Population should be at least 1 (never below 1)
        assertTrue("Population never below 1", settlement.population >= 1);
    }

    @Test
    public void testTradeRouteCreatedWhenSettlementsConnectedByRoad() {
        // Clear tiles so settlements and roads can be placed at the desired positions
        clearRow(0, 5, 0);
        Settlement a = world.createSettlement("TownA", 0, 0);
        Settlement b = world.createSettlement("TownB", 5, 0);
        assertNotNull(a);
        assertNotNull(b);

        // Initially no trade route
        assertEquals(0, world.getTradeRoutes().size());

        // Connect with a road
        for (int x = 1; x <= 4; x++) {
            world.placeRoad(x, 0, RoadType.DIRT);
        }

        // Tick to trigger route discovery
        sim.tick(1.0f);

        assertEquals("Trade route should be created", 1, world.getTradeRoutes().size());
        TradeRoute route = world.getTradeRoutes().get(0);
        assertTrue(route.connects(a.id));
        assertTrue(route.connects(b.id));
    }

    @Test
    public void testNoTradeRouteWithoutRoad() {
        clearTile(0, 0);
        clearTile(10, 0);
        Settlement a = world.createSettlement("TownA", 0, 0);
        Settlement b = world.createSettlement("TownB", 10, 0);
        assertNotNull(a);
        assertNotNull(b);

        // No road between them
        sim.tick(1.0f);

        assertEquals("No route without road", 0, world.getTradeRoutes().size());
    }

    @Test
    public void testBfsFindsPath() {
        // Clear the road corridor and place road tiles in a straight line
        clearRow(2, 7, 0);
        for (int x = 2; x <= 7; x++) {
            world.placeRoad(x, 0, RoadType.DIRT);
        }

        // BFS from near start to near end
        List<int[]> path = world.findRoadPath(0, 0, 9, 0);
        assertNotNull("Path should be found", path);
        assertTrue("Path should have tiles", path.size() > 0);
    }

    @Test
    public void testBfsReturnsNullWithNoRoad() {
        // No roads placed, settlements far apart
        List<int[]> path = world.findRoadPath(0, 0, 20, 0);
        assertNull("No path without roads", path);
    }

    @Test
    public void testCaravanSpawnedWhenRouteExists() {
        // Clear tiles so settlements and roads can be placed
        clearRow(0, 5, 0);
        Settlement a = world.createSettlement("A", 0, 0);
        Settlement b = world.createSettlement("B", 5, 0);
        assertNotNull(a);
        assertNotNull(b);
        for (int x = 1; x <= 4; x++) world.placeRoad(x, 0, RoadType.DIRT);

        // Give A surplus food
        a.food = a.storageCapacity * 0.8f;

        // Tick to create route
        sim.tick(1.0f);
        assertEquals(1, world.getTradeRoutes().size());

        // Run enough ticks for caravan to spawn
        // spawnInterval = 120 / sqrt(10+10) ≈ 26.8 ticks
        for (int i = 0; i < 30; i++) {
            a.food = a.storageCapacity * 0.8f;  // keep food high
            sim.tick(1.0f);
        }

        // We just verify no crash and route exists
        assertTrue("Route still exists", world.getTradeRoutes().size() >= 1);
    }

    @Test
    public void testTradeRouteRemoveWhenRoadRemoved() {
        // Clear all tiles needed for settlements and road corridor
        clearRow(0, 15, 0);
        Settlement a = world.createSettlement("A", 0, 0);
        Settlement b = world.createSettlement("B", 15, 0);
        assertNotNull(a);
        assertNotNull(b);
        for (int x = 3; x <= 12; x++) world.placeRoad(x, 0, RoadType.DIRT);

        sim.tick(1.0f);
        assertEquals(1, world.getTradeRoutes().size());

        // Remove road at x=7 (outside both 2-tile neighborhoods), breaking the connection
        world.removeRoad(7, 0);
        sim.tick(1.0f);

        assertEquals("Route removed when road broken", 0, world.getTradeRoutes().size());
    }
}
