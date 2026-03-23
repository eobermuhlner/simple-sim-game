package ch.obermuhlner.sim.game;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class TileTest {

    @Before
    public void setUp() {
        TileObjectRegistry.init();
    }

    @Test
    public void testDefaultTile() {
        Tile tile = new Tile();
        
        assertEquals(TerrainType.GRASS, tile.terrain);
        assertEquals(TileObjectRegistry.NONE, tile.objectId);
        assertEquals(0, tile.buildingId);
        assertEquals(0, tile.roadConnection);
        assertFalse(tile.hasBuilding());
        assertFalse(tile.hasObject());
        assertFalse(tile.hasRoad());
    }

    @Test
    public void testTileWithTerrainAndObject() {
        Tile tile = new Tile(TerrainType.FOREST, TileObjectRegistry.TREE_LARGE);
        
        assertEquals(TerrainType.FOREST, tile.terrain);
        assertEquals(TileObjectRegistry.TREE_LARGE, tile.objectId);
        assertFalse(tile.hasBuilding());
        assertTrue(tile.hasObject());
    }

    @Test
    public void testHasBuilding() {
        Tile tile = new Tile();
        
        assertFalse(tile.hasBuilding());
        
        tile.buildingId = 1;
        assertTrue(tile.hasBuilding());
        
        tile.buildingId = 0;
        assertFalse(tile.hasBuilding());
    }

    @Test
    public void testHasObject() {
        Tile tile = new Tile();
        
        assertFalse(tile.hasObject());
        
        tile.objectId = TileObjectRegistry.TREE_LARGE;
        assertTrue(tile.hasObject());
        
        tile.objectId = TileObjectRegistry.NONE;
        assertFalse(tile.hasObject());
    }

    @Test
    public void testIsBuildable() {
        Tile grass = new Tile(TerrainType.GRASS, TileObjectRegistry.NONE);
        assertTrue(grass.isBuildable());
        
        grass.objectId = TileObjectRegistry.TREE_LARGE;
        assertFalse(grass.isBuildable());
        
        grass.objectId = TileObjectRegistry.NONE;
        grass.buildingId = 1;
        assertFalse(grass.isBuildable());
        
        Tile water = new Tile(TerrainType.DEEP_SEA, TileObjectRegistry.NONE);
        assertFalse(water.isBuildable());
        
        Tile stone = new Tile(TerrainType.STONE, TileObjectRegistry.NONE);
        assertTrue("STONE is buildable", stone.isBuildable());
        
        Tile forest = new Tile(TerrainType.FOREST, TileObjectRegistry.NONE);
        assertTrue(forest.isBuildable());
    }

    @Test
    public void testIsWalkable() {
        Tile grass = new Tile(TerrainType.GRASS, TileObjectRegistry.NONE);
        assertTrue(grass.isWalkable());
        
        grass.objectId = TileObjectRegistry.TREE_LARGE;
        assertFalse(grass.isWalkable());
        
        grass.objectId = TileObjectRegistry.BOULDER_SMALL;
        assertTrue(grass.isWalkable());
        
        Tile water = new Tile(TerrainType.DEEP_SEA, TileObjectRegistry.NONE);
        assertFalse(water.isWalkable());
    }

    @Test
    public void testGetObject() {
        Tile tile = new Tile();
        tile.objectId = TileObjectRegistry.TREE_LARGE;
        
        TileObject obj = tile.getObject();
        assertNotNull(obj);
        assertEquals("Large Tree", obj.getName());
        
        tile.objectId = TileObjectRegistry.NONE;
        TileObject noneObj = tile.getObject();
        assertNotNull("NONE returns registered object", noneObj);
        assertEquals("None", noneObj.getName());
    }
}
