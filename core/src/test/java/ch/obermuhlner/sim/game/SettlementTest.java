package ch.obermuhlner.sim.game;

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
        assertEquals(Specialization.NONE, settlement.specialization);
        assertTrue(settlement.buildingIds.isEmpty());
    }

    @Test
    public void testVillageCannotAutoPromoteToTown() {
        Settlement settlement = new Settlement("Test", 0, 0);

        // Without specialization, population is capped at Village max
        settlement.setPopulation(51);
        assertEquals("Level stays VILLAGE without specialization", SettlementLevel.VILLAGE, settlement.getLevel());
        assertEquals("Population capped at Village max", SettlementLevel.VILLAGE.getMaxPopulation(), settlement.population);
    }

    @Test
    public void testSpecializeUpgradesToTown() {
        Settlement settlement = new Settlement("Test", 0, 0);
        settlement.setPopulation(50);

        assertTrue(settlement.needsSpecializationChoice());
        settlement.specialize(Specialization.LOGGING_CAMP);

        assertEquals(SettlementLevel.TOWN, settlement.getLevel());
        assertEquals(Specialization.LOGGING_CAMP, settlement.specialization);
        assertEquals(SettlementLevel.TOWN.getMinPopulation(), settlement.population);
    }

    @Test
    public void testSpecializeRequiresVillageAtMaxPop() {
        Settlement settlement = new Settlement("Test", 0, 0);
        // Population is 10, not at max
        assertFalse(settlement.needsSpecializationChoice());

        // specialize() should be a no-op
        settlement.specialize(Specialization.MINING_TOWN);
        assertEquals(SettlementLevel.VILLAGE, settlement.getLevel());
        assertEquals(Specialization.NONE, settlement.specialization);
    }

    @Test
    public void testSettlementLevelsWithSpecialization() {
        Settlement settlement = new Settlement("Test", 0, 0);

        assertEquals(SettlementLevel.VILLAGE, settlement.getLevel());

        // Reach Village max, specialize to Town
        settlement.setPopulation(50);
        settlement.specialize(Specialization.TRADE_HUB);
        assertEquals(SettlementLevel.TOWN, settlement.getLevel());

        // Town → City
        settlement.setPopulation(200);
        assertEquals(SettlementLevel.TOWN, settlement.getLevel());
        settlement.setPopulation(201);
        assertEquals(SettlementLevel.CITY, settlement.getLevel());

        // City → Metropolis
        settlement.setPopulation(500);
        assertEquals(SettlementLevel.CITY, settlement.getLevel());
        settlement.setPopulation(501);
        assertEquals(SettlementLevel.METROPOLIS, settlement.getLevel());

        settlement.setPopulation(1000);
        assertEquals(SettlementLevel.METROPOLIS, settlement.getLevel());
    }

    @Test
    public void testAddPopulationCappedAtVillageMax() {
        Settlement settlement = new Settlement("Test", 0, 0);
        assertEquals(10, settlement.population);

        settlement.addPopulation(5);
        assertEquals(15, settlement.population);

        // Cannot exceed Village max without specialization
        settlement.addPopulation(100);
        assertEquals(SettlementLevel.VILLAGE.getMaxPopulation(), settlement.population);
        assertEquals(SettlementLevel.VILLAGE, settlement.getLevel());
    }

    @Test
    public void testAddPopulationNeverBelowOne() {
        Settlement settlement = new Settlement("Test", 0, 0);
        settlement.addPopulation(-20);
        assertEquals(1, settlement.population);
    }

    @Test
    public void testSetPopulationCappedAtVillageMaxWithoutSpec() {
        Settlement settlement = new Settlement("Test", 0, 0);

        // Without spec, cannot go above Village max
        settlement.setPopulation(100);
        assertEquals(SettlementLevel.VILLAGE.getMaxPopulation(), settlement.population);
        assertEquals(SettlementLevel.VILLAGE, settlement.getLevel());

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

        // Reach Town via specialization
        settlement.setPopulation(50);
        settlement.specialize(Specialization.LOGGING_CAMP);
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
    public void testNeedsSpecializationChoice() {
        Settlement settlement = new Settlement("Test", 0, 0);

        assertFalse("Pop 10 does not need spec", settlement.needsSpecializationChoice());

        settlement.setPopulation(50);
        assertTrue("Village at max pop needs spec choice", settlement.needsSpecializationChoice());

        settlement.specialize(Specialization.FARMING_VILLAGE);
        assertFalse("After specializing, no longer needs spec choice", settlement.needsSpecializationChoice());
    }

    @Test
    public void testNeedsUpgradeSkipsVillage() {
        Settlement settlement = new Settlement("Test", 0, 0);

        // Village never triggers needsUpgrade (Village → Town goes through specialize())
        settlement.setPopulation(50);
        assertFalse("Village uses needsSpecializationChoice(), not needsUpgrade()", settlement.needsUpgrade());

        // Town → City uses needsUpgrade()
        settlement.specialize(Specialization.MINING_TOWN);
        settlement.setPopulation(200);
        assertTrue("TOWN at max pop - can upgrade", settlement.needsUpgrade());

        settlement.upgrade();
        assertEquals(SettlementLevel.CITY, settlement.getLevel());

        settlement.setPopulation(501);
        assertFalse("METROPOLIS cannot upgrade", settlement.needsUpgrade());
    }

    @Test
    public void testUpgradeFromTownToCity() {
        Settlement settlement = new Settlement("Test", 0, 0);

        settlement.setPopulation(50);
        settlement.specialize(Specialization.TRADE_HUB);
        assertEquals(SettlementLevel.TOWN, settlement.getLevel());

        settlement.setPopulation(200);
        settlement.upgrade();

        assertEquals(SettlementLevel.CITY, settlement.getLevel());
        assertEquals(SettlementLevel.CITY.getMinPopulation(), settlement.population);
    }

    @Test
    public void testUpgradeSetsPopulationToMin() {
        Settlement settlement = new Settlement("Test", 0, 0);

        settlement.setPopulation(50);
        settlement.specialize(Specialization.TRADE_HUB);
        assertEquals(SettlementLevel.TOWN.getMinPopulation(), settlement.population);

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
        settlement.setPopulation(50);
        settlement.specialize(Specialization.TRADE_HUB);
        settlement.setPopulation(501);
        assertEquals(SettlementLevel.METROPOLIS, settlement.getLevel());

        settlement.upgrade();
        assertEquals(SettlementLevel.METROPOLIS, settlement.getLevel());
    }

    @Test
    public void testCanRespecialize() {
        Settlement settlement = new Settlement("Test", 0, 0);

        assertFalse("Village cannot respecialize", settlement.canRespecialize());

        settlement.setPopulation(50);
        settlement.specialize(Specialization.LOGGING_CAMP);
        assertTrue("Town with spec can respecialize", settlement.canRespecialize());
    }

    @Test
    public void testRespecializeDropsOneLevel() {
        Settlement settlement = new Settlement("Test", 0, 0);

        settlement.setPopulation(50);
        settlement.specialize(Specialization.LOGGING_CAMP);
        assertEquals(SettlementLevel.TOWN, settlement.getLevel());

        settlement.respecialize(Specialization.MINING_TOWN);

        assertEquals(SettlementLevel.VILLAGE, settlement.getLevel());
        assertEquals(Specialization.MINING_TOWN, settlement.specialization);
        assertEquals(SettlementLevel.VILLAGE.getMinPopulation(), settlement.population);
    }

    @Test
    public void testRespecializeFromCity() {
        Settlement settlement = new Settlement("Test", 0, 0);
        settlement.setPopulation(50);
        settlement.specialize(Specialization.FARMING_VILLAGE);
        settlement.setPopulation(200);
        settlement.upgrade();
        assertEquals(SettlementLevel.CITY, settlement.getLevel());

        settlement.respecialize(Specialization.TRADE_HUB);

        assertEquals(SettlementLevel.TOWN, settlement.getLevel());
        assertEquals(Specialization.TRADE_HUB, settlement.specialization);
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
