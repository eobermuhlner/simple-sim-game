package ch.obermuhlner.sim.game;

import ch.obermuhlner.sim.game.systems.SimulationSystem;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for Phase 8: Sea Exploration.
 * Covers SHALLOW_SEA terrain, Harbor building, sea exploration helpers,
 * sea pathfinding, and sea trade route lifecycle in SimulationSystem.
 */
public class SeaExplorationTest {

    private static final int CHUNK_SIZE = 16;
    private World world;
    private GameConfig config;

    @Before
    public void setUp() {
        GameConfig.Root root = new GameConfig.Root();
        root.world.seed = 42L;
        // Register HARBOR_CONSTRUCTION tech so we can research it through the public API
        GameConfig.TechConfig harborTech = new GameConfig.TechConfig();
        harborTech.id = "HARBOR_CONSTRUCTION";
        harborTech.name = "Harbor Construction";
        harborTech.cost = 1.0f;
        List<String> allowedBuildings = new ArrayList<>();
        allowedBuildings.add("HARBOR");
        harborTech.allow.put("buildings", allowedBuildings);
        root.tech_tree.techs.put("HARBOR_CONSTRUCTION", harborTech);
        config = new GameConfig(root);
        world = new World(CHUNK_SIZE, config, true);
    }

    /** Simulate completing HARBOR_CONSTRUCTION research via the public TechTree API. */
    private void researchHarborConstruction() {
        world.techTree.startResearch("HARBOR_CONSTRUCTION");
        world.techTree.addProgress(100f, config);  // more than enough to complete (cost=1)
    }

    // ---- TerrainType.SHALLOW_SEA properties ----

    @Test
    public void testShallowSeaIsNotBuildable() {
        assertFalse("SHALLOW_SEA should not be buildable", TerrainType.SHALLOW_SEA.isBuildable());
    }

    @Test
    public void testShallowSeaIsNotTraversable() {
        assertFalse("SHALLOW_SEA should not be traversable by land units",
            TerrainType.SHALLOW_SEA.isTraversable());
    }

    @Test
    public void testShallowSeaIsWater() {
        assertTrue("SHALLOW_SEA.isWater() must return true", TerrainType.SHALLOW_SEA.isWater());
    }

    @Test
    public void testDeepSeaIsWater() {
        assertTrue("DEEP_SEA.isWater() must return true", TerrainType.DEEP_SEA.isWater());
    }

    @Test
    public void testGrassIsNotWater() {
        assertFalse("GRASS.isWater() must return false", TerrainType.GRASS.isWater());
    }

    @Test
    public void testForestIsNotWater() {
        assertFalse("FOREST.isWater() must return false", TerrainType.FOREST.isWater());
    }

    // ---- ThresholdConfig default keeps SHALLOW_SEA disabled ----

    @Test
    public void testShallowSeaDefaultThresholdEqualsWater() {
        GameConfig.ThresholdConfig tc = new GameConfig.ThresholdConfig();
        assertEquals("Default shallow_sea threshold should equal deep_sea (disabled)",
            tc.deep_sea, tc.shallow_sea, 0.0001);
    }

    // ---- HARBOR building type ----

    @Test
    public void testHarborExists() {
        assertNotNull("HARBOR building type must exist", BuildingType.HARBOR);
        assertEquals(56, BuildingType.HARBOR.getId());
    }

    @Test
    public void testHarborFromId() {
        BuildingType found = BuildingType.fromId(BuildingType.HARBOR.getId());
        assertEquals(BuildingType.HARBOR, found);
    }

    @Test
    public void testHarborDisplayName() {
        assertEquals("Harbor", BuildingType.HARBOR.getDisplayName());
    }

    // ---- World.isCoastal() ----

    /** Force a tile's terrain type to the given value. */
    private void setTerrain(int tx, int ty, TerrainType terrain) {
        world.getTile(tx, ty).terrain = terrain;
    }

    /** Force a tile to GRASS with no object so it is buildable. */
    private void makeGrass(int tx, int ty) {
        setTerrain(tx, ty, TerrainType.GRASS);
        world.getTile(tx, ty).objectId = TileObjectRegistry.NONE;
    }

    @Test
    public void testIsCoastalGrassAdjacentToShallowSea() {
        makeGrass(5, 5);
        setTerrain(6, 5, TerrainType.SHALLOW_SEA);  // east neighbor is shallow sea

        assertTrue("Grass tile next to SHALLOW_SEA should be coastal", world.isCoastal(5, 5));
    }

    @Test
    public void testIsCoastalGrassAdjacentToWater() {
        makeGrass(5, 5);
        setTerrain(5, 6, TerrainType.DEEP_SEA);  // north neighbor is water

        assertTrue("Grass tile next to WATER should be coastal", world.isCoastal(5, 5));
    }

    @Test
    public void testIsCoastalGrassSurroundedByGrass() {
        makeGrass(5, 5);
        makeGrass(4, 5); makeGrass(6, 5); makeGrass(5, 4); makeGrass(5, 6);

        assertFalse("Grass surrounded by grass is not coastal", world.isCoastal(5, 5));
    }

    @Test
    public void testIsCoastalWaterTileReturnsFalse() {
        setTerrain(5, 5, TerrainType.SHALLOW_SEA);

        assertFalse("Sea tile itself is not coastal (not buildable)", world.isCoastal(5, 5));
    }

    // ---- World.settlementHasHarbor() ----

    @Test
    public void testSettlementHasHarborFalseWhenNoBuildings() {
        makeGrass(0, 0);
        Settlement s = world.createSettlement("Test", 0, 0);
        assertNotNull(s);

        assertFalse("Settlement with no buildings should not have a harbor",
            world.settlementHasHarbor(s));
    }

    @Test
    public void testSettlementHasHarborTrueAfterAdding() {
        makeGrass(0, 0);
        Settlement s = world.createSettlement("Test", 0, 0);
        assertNotNull(s);
        s.buildingIds.add(BuildingType.HARBOR.getId());

        assertTrue("Settlement with HARBOR building should return true",
            world.settlementHasHarbor(s));
    }

    @Test
    public void testSettlementHasHarborFalseWithOtherBuildings() {
        makeGrass(0, 0);
        Settlement s = world.createSettlement("Test", 0, 0);
        assertNotNull(s);
        s.buildingIds.add(BuildingType.HOUSE_SIMPLE.getId());
        s.buildingIds.add(BuildingType.FARM_SMALL.getId());

        assertFalse("Settlement with non-harbor buildings should not have a harbor",
            world.settlementHasHarbor(s));
    }

    // ---- World.revealSeaArea() ----

    @Test
    public void testRevealSeaAreaRevealsShallowSea() {
        setTerrain(5, 5, TerrainType.SHALLOW_SEA);
        setTerrain(5, 6, TerrainType.SHALLOW_SEA);
        setTerrain(6, 5, TerrainType.SHALLOW_SEA);

        assertFalse(world.isRevealed(5, 5));
        world.revealSeaArea(4, 5, 2, false);  // center on adjacent land

        assertTrue("SHALLOW_SEA within radius should be revealed", world.isRevealed(5, 5));
        assertTrue(world.isRevealed(5, 6));
        assertTrue(world.isRevealed(6, 5));
    }

    @Test
    public void testRevealSeaAreaDoesNotRevealDeepWaterWhenDisabled() {
        setTerrain(5, 5, TerrainType.DEEP_SEA);

        world.revealSeaArea(4, 5, 2, false);  // includeDeepSea=false

        assertFalse("Deep WATER should not be revealed when includeDeepSea=false",
            world.isRevealed(5, 5));
    }

    @Test
    public void testRevealSeaAreaRevealsDeepWaterWhenEnabled() {
        setTerrain(5, 5, TerrainType.DEEP_SEA);

        world.revealSeaArea(4, 5, 2, true);  // includeDeepSea=true

        assertTrue("Deep WATER should be revealed when includeDeepSea=true",
            world.isRevealed(5, 5));
    }

    @Test
    public void testRevealSeaAreaDoesNotRevealLandTiles() {
        makeGrass(5, 5);

        world.revealSeaArea(4, 5, 2, true);

        assertFalse("Land (GRASS) tiles should not be revealed by revealSeaArea",
            world.isRevealed(5, 5));
    }

    // ---- World.findSeaPath() ----

    /** Build a straight corridor of SHALLOW_SEA tiles from (x1,y) to (x2,y). */
    private void buildSeaCorridor(int x1, int x2, int y) {
        for (int x = x1; x <= x2; x++) {
            setTerrain(x, y, TerrainType.SHALLOW_SEA);
        }
    }

    @Test
    public void testFindSeaPathNullWhenNoWater() {
        // Both settlements on pure grass, no water nearby
        for (int dx = -8; dx <= 8; dx++) {
            for (int dy = -8; dy <= 8; dy++) {
                makeGrass(dx, dy);
                makeGrass(20 + dx, dy);
            }
        }
        makeGrass(0, 0);
        makeGrass(20, 0);
        Settlement a = world.createSettlement("A", 0, 0);
        Settlement b = world.createSettlement("B", 20, 0);
        assertNotNull(a);
        assertNotNull(b);

        assertNull("No sea path when no water exists", world.findSeaPath(a, b));
    }

    @Test
    public void testFindSeaPathConnectedThroughShallowSea() {
        // Place settlements on grass
        makeGrass(0, 0);
        makeGrass(10, 0);

        // Build a shallow-sea corridor from x=1 to x=9 at y=1
        buildSeaCorridor(1, 9, 1);
        // Make a coast: GRASS at y=0 borders SHALLOW_SEA at y=1
        // Settlement A at (0,0) has SHALLOW_SEA at (0,1) = entry point
        setTerrain(0, 1, TerrainType.SHALLOW_SEA);
        setTerrain(10, 1, TerrainType.SHALLOW_SEA);

        Settlement a = world.createSettlement("A", 0, 0);
        Settlement b = world.createSettlement("B", 10, 0);
        assertNotNull(a);
        assertNotNull(b);

        List<int[]> path = world.findSeaPath(a, b);
        assertNotNull("Sea path should be found through SHALLOW_SEA corridor", path);
        assertTrue("Sea path should contain multiple tiles", path.size() >= 2);
    }

    @Test
    public void testFindSeaPathNullWhenSeaNotConnected() {
        // Control all tiles in the region so no generated water sneaks through
        for (int dy = -5; dy <= 5; dy++)
            for (int dx = -2; dx <= 12; dx++)
                makeGrass(dx, dy);

        // Each settlement has an isolated sea tile next to it, but they're not connected
        setTerrain(0, 1, TerrainType.SHALLOW_SEA);   // only next to A
        setTerrain(10, 1, TerrainType.SHALLOW_SEA);  // only next to B
        // All tiles from x=1..9 at y=1 are GRASS — no sea corridor

        Settlement a = world.createSettlement("A", 0, 0);
        Settlement b = world.createSettlement("B", 10, 0);
        assertNotNull(a);
        assertNotNull(b);

        assertNull("Sea path should be null when sea tiles are not connected", world.findSeaPath(a, b));
    }

    // ---- TradeRoute.isSea flag ----

    @Test
    public void testTradeRouteIsSeaDefaultFalse() {
        makeGrass(0, 0);
        makeGrass(5, 0);
        Settlement a = world.createSettlement("A", 0, 0);
        Settlement b = world.createSettlement("B", 5, 0);
        assertNotNull(a);
        assertNotNull(b);

        // Link with a road and get the land route
        for (int x = 1; x <= 4; x++) {
            makeGrass(x, 0);
            world.placeRoad(x, 0, RoadType.DIRT);
        }

        SimulationSystem sim = new SimulationSystem(world, config);
        sim.tick(1.0f);

        List<TradeRoute> routes = world.getTradeRoutes();
        assertEquals(1, routes.size());
        assertFalse("Land trade route should have isSea=false", routes.get(0).isSea);
    }

    // ---- Sea trade route via SimulationSystem ----

    /** Set up two coastal settlements connected by a SHALLOW_SEA corridor. */
    private Settlement[] buildCoastalSetup() {
        // Settlement A at (0,0), coast at (0,1)/(1,1)...
        makeGrass(0, 0);
        makeGrass(10, 0);
        // Sea corridor at y=1
        for (int x = 0; x <= 10; x++) setTerrain(x, 1, TerrainType.SHALLOW_SEA);
        // No roads between settlements

        Settlement a = world.createSettlement("A", 0, 0);
        Settlement b = world.createSettlement("B", 10, 0);
        assertNotNull(a);
        assertNotNull(b);
        return new Settlement[]{a, b};
    }

    @Test
    public void testNoSeaRouteWithoutHarbors() {
        Settlement[] pair = buildCoastalSetup();
        // Neither settlement has a harbor
        SimulationSystem sim = new SimulationSystem(world, config);
        sim.tick(1.0f);

        assertEquals("No sea route without harbors", 0, world.getTradeRoutes().size());
    }

    @Test
    public void testNoSeaRouteWithOnlyOneHarbor() {
        Settlement[] pair = buildCoastalSetup();
        pair[0].buildingIds.add(BuildingType.HARBOR.getId());
        // Only A has a harbor; B does not

        // Allow HARBOR via tech tree
        researchHarborConstruction();

        SimulationSystem sim = new SimulationSystem(world, config);
        sim.tick(1.0f);

        assertEquals("Sea route requires both settlements to have a harbor",
            0, world.getTradeRoutes().size());
    }

    @Test
    public void testSeaRouteFormedWhenBothHaveHarbors() {
        Settlement[] pair = buildCoastalSetup();
        pair[0].buildingIds.add(BuildingType.HARBOR.getId());
        pair[1].buildingIds.add(BuildingType.HARBOR.getId());

        // Allow HARBOR via tech tree
        researchHarborConstruction();

        SimulationSystem sim = new SimulationSystem(world, config);
        sim.tick(1.0f);

        assertEquals("Sea route should form when both settlements have harbors",
            1, world.getTradeRoutes().size());
        assertTrue("Route should be marked as sea route",
            world.getTradeRoutes().get(0).isSea);
    }

    @Test
    public void testSeaRouteRemovedWhenHarborTechNotResearched() {
        Settlement[] pair = buildCoastalSetup();
        pair[0].buildingIds.add(BuildingType.HARBOR.getId());
        pair[1].buildingIds.add(BuildingType.HARBOR.getId());
        // Tech NOT researched — harbors exist but tech tree doesn't allow them

        SimulationSystem sim = new SimulationSystem(world, config);
        sim.tick(1.0f);

        assertEquals("Sea route requires HARBOR_CONSTRUCTION tech to be researched",
            0, world.getTradeRoutes().size());
    }

    @Test
    public void testLandRouteSuperseedsSeaRoute() {
        Settlement[] pair = buildCoastalSetup();
        pair[0].buildingIds.add(BuildingType.HARBOR.getId());
        pair[1].buildingIds.add(BuildingType.HARBOR.getId());
        researchHarborConstruction();

        SimulationSystem sim = new SimulationSystem(world, config);
        sim.tick(1.0f);

        // Sea route should be present
        assertEquals(1, world.getTradeRoutes().size());
        assertTrue(world.getTradeRoutes().get(0).isSea);

        // Now connect by road — land route supersedes sea
        for (int x = 1; x <= 9; x++) {
            makeGrass(x, 0);
            world.placeRoad(x, 0, RoadType.DIRT);
        }
        sim.tick(1.0f);

        // Should be exactly one route and it should be a land route
        assertEquals("Exactly one route after road built", 1, world.getTradeRoutes().size());
        assertFalse("Land route should supersede sea route",
            world.getTradeRoutes().get(0).isSea);
    }

    @Test
    public void testSeaRouteReturnsAfterRoadRemoved() {
        Settlement[] pair = buildCoastalSetup();
        pair[0].buildingIds.add(BuildingType.HARBOR.getId());
        pair[1].buildingIds.add(BuildingType.HARBOR.getId());
        researchHarborConstruction();

        // Build road first
        for (int x = 1; x <= 9; x++) {
            makeGrass(x, 0);
            world.placeRoad(x, 0, RoadType.DIRT);
        }

        SimulationSystem sim = new SimulationSystem(world, config);
        sim.tick(1.0f);

        assertEquals(1, world.getTradeRoutes().size());
        assertFalse("Land route should be active", world.getTradeRoutes().get(0).isSea);

        // Remove a road tile to break land connection
        world.removeRoad(5, 0);
        sim.tick(1.0f);

        // Sea route should re-form
        List<TradeRoute> routes = world.getTradeRoutes();
        assertEquals("Sea route should form after land route is broken", 1, routes.size());
        assertTrue("Route should be sea after land disconnection", routes.get(0).isSea);
    }

    // ---- getLandTradeRoute / getSeaTradeRoute selectors ----

    @Test
    public void testGetLandTradeRouteReturnsNullForSeaRoute() {
        Settlement[] pair = buildCoastalSetup();
        pair[0].buildingIds.add(BuildingType.HARBOR.getId());
        pair[1].buildingIds.add(BuildingType.HARBOR.getId());
        researchHarborConstruction();

        SimulationSystem sim = new SimulationSystem(world, config);
        sim.tick(1.0f);

        assertNull("getLandTradeRoute should return null when only a sea route exists",
            world.getLandTradeRoute(pair[0].id, pair[1].id));
        assertNotNull("getSeaTradeRoute should return the sea route",
            world.getSeaTradeRoute(pair[0].id, pair[1].id));
    }
}
