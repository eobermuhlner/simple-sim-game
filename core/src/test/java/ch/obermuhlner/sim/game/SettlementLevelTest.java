package ch.obermuhlner.sim.game;

import org.junit.Test;
import static org.junit.Assert.*;

public class SettlementLevelTest {

    @Test
    public void testFromPopulation() {
        assertEquals(SettlementLevel.VILLAGE, SettlementLevel.fromPopulation(1));
        assertEquals(SettlementLevel.VILLAGE, SettlementLevel.fromPopulation(25));
        assertEquals(SettlementLevel.VILLAGE, SettlementLevel.fromPopulation(50));
        
        assertEquals(SettlementLevel.TOWN, SettlementLevel.fromPopulation(51));
        assertEquals(SettlementLevel.TOWN, SettlementLevel.fromPopulation(100));
        assertEquals(SettlementLevel.TOWN, SettlementLevel.fromPopulation(200));
        
        assertEquals(SettlementLevel.CITY, SettlementLevel.fromPopulation(201));
        assertEquals(SettlementLevel.CITY, SettlementLevel.fromPopulation(350));
        assertEquals(SettlementLevel.CITY, SettlementLevel.fromPopulation(500));
        
        assertEquals(SettlementLevel.METROPOLIS, SettlementLevel.fromPopulation(501));
        assertEquals(SettlementLevel.METROPOLIS, SettlementLevel.fromPopulation(1000));
        assertEquals(SettlementLevel.METROPOLIS, SettlementLevel.fromPopulation(Integer.MAX_VALUE));
    }

    @Test
    public void testLevelPopulationRanges() {
        assertEquals(1, SettlementLevel.VILLAGE.getMinPopulation());
        assertEquals(50, SettlementLevel.VILLAGE.getMaxPopulation());
        
        assertEquals(51, SettlementLevel.TOWN.getMinPopulation());
        assertEquals(200, SettlementLevel.TOWN.getMaxPopulation());
        
        assertEquals(201, SettlementLevel.CITY.getMinPopulation());
        assertEquals(500, SettlementLevel.CITY.getMaxPopulation());
        
        assertEquals(501, SettlementLevel.METROPOLIS.getMinPopulation());
        assertEquals(Integer.MAX_VALUE, SettlementLevel.METROPOLIS.getMaxPopulation());
    }

    @Test
    public void testLevelDisplayNames() {
        assertEquals("Village", SettlementLevel.VILLAGE.getDisplayName());
        assertEquals("Town", SettlementLevel.TOWN.getDisplayName());
        assertEquals("City", SettlementLevel.CITY.getDisplayName());
        assertEquals("Metropolis", SettlementLevel.METROPOLIS.getDisplayName());
    }

    @Test
    public void testLevelOrder() {
        assertEquals(0, SettlementLevel.VILLAGE.ordinal());
        assertEquals(1, SettlementLevel.TOWN.ordinal());
        assertEquals(2, SettlementLevel.CITY.ordinal());
        assertEquals(3, SettlementLevel.METROPOLIS.ordinal());
    }
}
