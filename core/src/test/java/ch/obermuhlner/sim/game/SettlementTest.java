package ch.obermuhlner.sim.game;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class SettlementTest {

    @Test
    public void testNewSettlementHasDefaultValues() {
        Settlement settlement = new Settlement("Test Town", 5, 10);
        
        assertEquals("Test Town", settlement.name);
        assertEquals(5, settlement.centerX);
        assertEquals(10, settlement.centerY);
        assertEquals(10, settlement.population);
        assertEquals(SettlementLevel.VILLAGE, settlement.getLevel());
        assertTrue(settlement.buildingIds.isEmpty());
    }

    @Test
    public void testSettlementLevels() {
        Settlement settlement = new Settlement("Test", 0, 0);
        
        assertEquals(SettlementLevel.VILLAGE, settlement.getLevel());
        
        settlement.setPopulation(50);
        assertEquals(SettlementLevel.VILLAGE, settlement.getLevel());
        
        settlement.setPopulation(51);
        assertEquals(SettlementLevel.TOWN, settlement.getLevel());
        
        settlement.setPopulation(200);
        assertEquals(SettlementLevel.TOWN, settlement.getLevel());
        
        settlement.setPopulation(201);
        assertEquals(SettlementLevel.CITY, settlement.getLevel());
        
        settlement.setPopulation(500);
        assertEquals(SettlementLevel.CITY, settlement.getLevel());
        
        settlement.setPopulation(501);
        assertEquals(SettlementLevel.METROPOLIS, settlement.getLevel());
        
        settlement.setPopulation(1000);
        assertEquals(SettlementLevel.METROPOLIS, settlement.getLevel());
    }

    @Test
    public void testAddPopulation() {
        Settlement settlement = new Settlement("Test", 0, 0);
        assertEquals(10, settlement.population);
        
        settlement.addPopulation(5);
        assertEquals(15, settlement.population);
        
        settlement.addPopulation(-20);
        assertEquals(1, settlement.population);
    }

    @Test
    public void testSetPopulation() {
        Settlement settlement = new Settlement("Test", 0, 0);
        
        settlement.setPopulation(100);
        assertEquals(100, settlement.population);
        assertEquals(SettlementLevel.TOWN, settlement.getLevel());
        
        settlement.setPopulation(1);
        assertEquals(1, settlement.population);
        assertEquals(SettlementLevel.VILLAGE, settlement.getLevel());
    }

    @Test
    public void testAddBuilding() {
        Settlement settlement = new Settlement("Test", 0, 0);
        
        assertTrue(settlement.addBuilding(1));
        assertEquals(1, settlement.buildingIds.size());
        assertTrue(settlement.buildingIds.contains(1));
        
        assertTrue(settlement.addBuilding(5));
        assertEquals(2, settlement.buildingIds.size());
    }

    @Test
    public void testMaxBuildings() {
        Settlement settlement = new Settlement("Test", 0, 0);
        assertEquals(5, settlement.getMaxBuildings());
        
        settlement.setPopulation(51);
        assertEquals(15, settlement.getMaxBuildings());
        
        settlement.setPopulation(201);
        assertEquals(30, settlement.getMaxBuildings());
        
        settlement.setPopulation(501);
        assertEquals(50, settlement.getMaxBuildings());
    }

    @Test
    public void testCannotAddMoreThanMaxBuildings() {
        Settlement settlement = new Settlement("Test", 0, 0);
        
        for (int i = 0; i < 5; i++) {
            assertTrue(settlement.addBuilding(i));
        }
        
        assertFalse(settlement.addBuilding(99));
        assertEquals(5, settlement.buildingIds.size());
    }

    @Test
    public void testNeedsUpgrade() {
        Settlement settlement = new Settlement("Test", 0, 0);
        
        assertFalse("VILLAGE at pop 10 has room to grow", settlement.needsUpgrade());
        
        settlement.setPopulation(50);
        assertTrue("VILLAGE at max pop - can upgrade", settlement.needsUpgrade());
        
        settlement.upgrade();
        assertEquals(SettlementLevel.TOWN, settlement.getLevel());
        
        settlement.setPopulation(200);
        assertTrue("TOWN at max pop - can upgrade", settlement.needsUpgrade());
        
        settlement.upgrade();
        assertEquals(SettlementLevel.CITY, settlement.getLevel());
        
        settlement.setPopulation(501);
        assertFalse("METROPOLIS cannot upgrade", settlement.needsUpgrade());
    }

    @Test
    public void testUpgrade() {
        Settlement settlement = new Settlement("Test", 0, 0);
        
        assertEquals(SettlementLevel.VILLAGE, settlement.getLevel());
        assertEquals(5, settlement.getMaxBuildings());
        
        settlement.setPopulation(50);
        settlement.upgrade();
        
        assertEquals(SettlementLevel.TOWN, settlement.getLevel());
        assertEquals(15, settlement.getMaxBuildings());
        
        settlement.setPopulation(200);
        settlement.upgrade();
        
        assertEquals(SettlementLevel.CITY, settlement.getLevel());
        assertEquals(30, settlement.getMaxBuildings());
    }

    @Test
    public void testUpgradeSetsPopulationToMin() {
        Settlement settlement = new Settlement("Test", 0, 0);
        
        settlement.setPopulation(50);
        settlement.upgrade();
        
        assertEquals(SettlementLevel.TOWN.getMinPopulation(), settlement.population);
        assertEquals(SettlementLevel.TOWN, settlement.getLevel());
        
        settlement.setPopulation(200);
        settlement.upgrade();
        
        assertEquals(SettlementLevel.CITY.getMinPopulation(), settlement.population);
        assertEquals(SettlementLevel.CITY, settlement.getLevel());
        
        settlement.setPopulation(501);
        assertEquals(SettlementLevel.METROPOLIS, settlement.getLevel());
        assertFalse("METROPOLIS cannot upgrade", settlement.needsUpgrade());
    }

    @Test
    public void testCannotUpgradeMetropolis() {
        Settlement settlement = new Settlement("Test", 0, 0);
        settlement.setPopulation(501);
        assertEquals(SettlementLevel.METROPOLIS, settlement.getLevel());
        
        settlement.upgrade();
        assertEquals(SettlementLevel.METROPOLIS, settlement.getLevel());
        assertEquals(501, settlement.population);
    }

    @Test
    public void testSettlementIdIsUnique() {
        Settlement s1 = new Settlement("Test1", 0, 0);
        Settlement s2 = new Settlement("Test2", 1, 1);
        Settlement s3 = new Settlement("Test3", 2, 2);
        
        assertTrue(s1.id != s2.id);
        assertTrue(s2.id != s3.id);
        assertTrue(s1.id != s3.id);
        assertTrue(s1.id < s2.id);
        assertTrue(s2.id < s3.id);
    }
}
