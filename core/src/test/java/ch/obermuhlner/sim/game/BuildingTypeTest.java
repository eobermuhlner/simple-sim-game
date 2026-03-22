package ch.obermuhlner.sim.game;

import org.junit.Test;
import static org.junit.Assert.*;

public class BuildingTypeTest {

    @Test
    public void testFromId() {
        assertEquals(BuildingType.HOUSE_SIMPLE, BuildingType.fromId(1));
        assertEquals(BuildingType.HOUSE_LARGE, BuildingType.fromId(2));
        assertEquals(BuildingType.FARM_SMALL, BuildingType.fromId(5));
        assertEquals(BuildingType.WAREHOUSE, BuildingType.fromId(16));
        assertEquals(BuildingType.CHICKEN_COOP, BuildingType.fromId(55));
    }

    @Test
    public void testFromIdReturnsNullForInvalid() {
        assertNull(BuildingType.fromId(0));
        assertNull(BuildingType.fromId(-1));
        assertNull(BuildingType.fromId(100));
    }

    @Test
    public void testAllBuildingTypesHaveValidIds() {
        for (BuildingType type : BuildingType.values()) {
            assertNotNull(type.getId());
            assertTrue(type.getId() > 0);
            assertEquals(type, BuildingType.fromId(type.getId()));
        }
    }

    @Test
    public void testDisplayNames() {
        assertEquals("Simple House", BuildingType.HOUSE_SIMPLE.getDisplayName());
        assertEquals("Large Farm", BuildingType.FARM_LARGE.getDisplayName());
        assertEquals("Warehouse", BuildingType.WAREHOUSE.getDisplayName());
    }

    @Test
    public void testTexturePaths() {
        for (BuildingType type : BuildingType.values()) {
            String path = type.getTexturePath();
            assertNotNull(path);
            assertTrue(path.startsWith("64x64/"));
            assertTrue(path.endsWith(".png"));
        }
    }

    @Test
    public void testPopulationCapacity() {
        assertEquals(4, BuildingType.HOUSE_SIMPLE.getPopulationCapacity());
        assertEquals(8, BuildingType.HOUSE_LARGE.getPopulationCapacity());
        assertEquals(3, BuildingType.FARM_SMALL.getPopulationCapacity());
        assertEquals(10, BuildingType.MARKET_LARGE.getPopulationCapacity());
        assertEquals(50, BuildingType.PALACE.getPopulationCapacity());
        assertEquals(2, BuildingType.WELL_WATER.getPopulationCapacity());
    }

    @Test
    public void testIdsAreUnique() {
        java.util.Set<Integer> ids = new java.util.HashSet<>();
        for (BuildingType type : BuildingType.values()) {
            assertTrue("Duplicate ID found: " + type.getId(), ids.add(type.getId()));
        }
    }
}
