package ch.obermuhlner.sim.game.mode;

import ch.obermuhlner.sim.game.*;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class BuildModeLogicTest {

    @Before
    public void setUp() {
        TileObjectRegistry.init();
    }

    @Test
    public void testBuildingPlacementRequiresBuildableTerrain() {
        Tile grassTile = new Tile(TerrainType.GRASS, TileObjectRegistry.NONE);
        assertTrue("GRASS is buildable", grassTile.isBuildable());
        
        Tile waterTile = new Tile(TerrainType.WATER, TileObjectRegistry.NONE);
        assertFalse("WATER is not buildable", waterTile.isBuildable());
        
        Tile forestTile = new Tile(TerrainType.FOREST, TileObjectRegistry.NONE);
        assertTrue("FOREST is buildable", forestTile.isBuildable());
    }

    @Test
    public void testBuildingPlacementRequiresNoExistingBuilding() {
        Tile tile = new Tile(TerrainType.GRASS, TileObjectRegistry.NONE);
        
        assertTrue(tile.isBuildable());
        
        tile.buildingId = 1;
        assertFalse(tile.isBuildable());
        assertTrue(tile.hasBuilding());
    }

    @Test
    public void testSettlementBuildingCapacityByLevel() {
        Settlement village = new Settlement("Village", 0, 0);
        assertEquals(5, village.getMaxBuildings());

        // Town requires specialization to reach
        Settlement town = new Settlement("Town", 0, 0);
        town.setPopulation(50);
        town.specialize(Specialization.TRADE_HUB);
        town.setPopulation(100);
        assertEquals(15, town.getMaxBuildings());

        Settlement city = new Settlement("City", 0, 0);
        city.setPopulation(50);
        city.specialize(Specialization.MINING_TOWN);
        city.setPopulation(300);
        assertEquals(30, city.getMaxBuildings());

        Settlement metropolis = new Settlement("Metropolis", 0, 0);
        metropolis.setPopulation(50);
        metropolis.specialize(Specialization.LOGGING_CAMP);
        metropolis.setPopulation(600);
        assertEquals(50, metropolis.getMaxBuildings());
    }

    @Test
    public void testBuildingAddsPopulation() {
        Settlement settlement = new Settlement("Test", 0, 0);
        int initialPop = settlement.population;
        int houseCapacity = BuildingType.HOUSE_SIMPLE.getPopulationCapacity();
        
        settlement.addBuilding(BuildingType.HOUSE_SIMPLE.getId());
        settlement.addPopulation(houseCapacity);
        
        assertEquals(initialPop + houseCapacity, settlement.population);
    }

    @Test
    public void testCannotExceedBuildingLimit() {
        Settlement settlement = new Settlement("Test", 0, 0);
        
        for (int i = 0; i < 5; i++) {
            assertTrue(settlement.addBuilding(i));
        }
        
        assertFalse(settlement.addBuilding(99));
        assertEquals(5, settlement.buildingIds.size());
    }

    @Test
    public void testProximityDistanceCalculation() {
        int cx = 10;
        int cy = 10;
        
        double distCenter = Math.hypot(0, 0);
        double distEdge = Math.hypot(3, 4);
        double distCorner = Math.hypot(3, 3);
        
        assertEquals(0.0, distCenter, 0.001);
        assertEquals(5.0, distEdge, 0.001);
        assertTrue(distCorner <= 5.0);
        assertTrue(distEdge <= 5.0);
        
        assertTrue(Math.hypot(4, 3) <= 5.0);
        assertTrue(Math.hypot(5, 0) <= 5.0);
        assertFalse(Math.hypot(6, 0) <= 5.0);
    }
}
