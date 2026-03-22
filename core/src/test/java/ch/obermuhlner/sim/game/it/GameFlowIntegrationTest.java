package ch.obermuhlner.sim.game.it;

import ch.obermuhlner.sim.game.Settlement;
import ch.obermuhlner.sim.game.SettlementLevel;
import ch.obermuhlner.sim.game.TerrainType;
import ch.obermuhlner.sim.game.Tile;
import ch.obermuhlner.sim.game.mode.BuildMode;
import ch.obermuhlner.sim.game.mode.ExploreMode;
import com.badlogic.gdx.Input;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class GameFlowIntegrationTest extends HeadlessGameTest {

    @Before
    public void initExploreMode() {
        world.reveal(0, 0);
        world.reveal(1, 0);
        world.reveal(2, 0);
        world.reveal(3, 0);
        world.reveal(4, 0);
        
        setGameMode(new ExploreMode());
        assertNotNull(currentMode);
        assertEquals("Explore", currentMode.getName());
    }

    @Test
    public void testStartInExploreMode() {
        assertEquals(ExploreMode.class, currentMode.getClass());
    }

    @Test
    public void testToggleFogOfWar() {
        int tx = 10;
        int ty = 10;
        
        assertFalse(world.isRevealed(tx, ty));
        
        int screenX = worldToScreenX(tx);
        int screenY = worldToScreenY(ty);
        
        simulateTouchDown(screenX, screenY, 0, Input.Buttons.LEFT);
        simulateTouchUp(screenX, screenY, 0, Input.Buttons.LEFT);
        
        simulateKeyDown(Input.Keys.F);
        simulateKeyUp(Input.Keys.F);
    }

    @Test
    public void testRevealTiles() {
        int tx = 5;
        int ty = 5;
        
        world.reveal(tx, ty);
        assertTrue(world.isRevealed(tx, ty));
        
        assertTrue(world.hasRevealedNeighbor(tx + 1, ty));
        assertTrue(world.hasRevealedNeighbor(tx, ty + 1));
    }

    @Test
    public void testCreateSettlementAndVerifyState() {
        int tx = 0;
        int ty = 0;
        for (int y = -5; y <= 5; y++) {
            for (int x = -5; x <= 5; x++) {
                if (world.getTerrain(x, y) == TerrainType.GRASS) {
                    tx = x;
                    ty = y;
                    world.revealArea(x, y, 5);
                    break;
                }
            }
            if (tx != 0 || ty != 0) break;
        }
        
        int initialCount = world.getSettlements().size();
        
        Settlement settlement = world.createSettlement("New Town", tx, ty);
        
        if (settlement != null) {
            assertEquals(initialCount + 1, world.getSettlements().size());
            assertEquals("New Town", settlement.name);
            assertEquals(tx, settlement.centerX);
            assertEquals(ty, settlement.centerY);
            assertEquals(SettlementLevel.VILLAGE, settlement.getLevel());
            assertEquals(10, settlement.population);
            assertEquals(5, settlement.getMaxBuildings());
            assertTrue(settlement.buildingIds.isEmpty());
        }
    }

    @Test
    public void testSettlementUpgradePath() {
        int tx = 0;
        int ty = 0;
        for (int y = -10; y <= 10 && tx == 0; y++) {
            for (int x = -10; x <= 10; x++) {
                if (world.getTerrain(x, y) == TerrainType.GRASS) {
                    tx = x;
                    ty = y;
                    world.revealArea(x, y, 5);
                    break;
                }
            }
        }
        
        Settlement settlement = world.createSettlement("Growing Town", tx, ty);
        if (settlement == null) {
            return;
        }
        
        assertEquals(SettlementLevel.VILLAGE, settlement.getLevel());
        assertEquals(10, settlement.population);
        
        settlement.setPopulation(50);
        assertTrue(settlement.needsUpgrade());
        
        settlement.upgrade();
        assertEquals(SettlementLevel.TOWN, settlement.getLevel());
        assertEquals(51, settlement.population);
        assertEquals(15, settlement.getMaxBuildings());
        
        settlement.setPopulation(200);
        assertTrue(settlement.needsUpgrade());
        
        settlement.upgrade();
        assertEquals(SettlementLevel.CITY, settlement.getLevel());
        assertEquals(201, settlement.population);
        assertEquals(30, settlement.getMaxBuildings());
        
        settlement.setPopulation(501);
        assertFalse(settlement.needsUpgrade());
        
        settlement.upgrade();
        assertEquals(SettlementLevel.METROPOLIS, settlement.getLevel());
        assertEquals(501, settlement.population);
    }

    @Test
    public void testSettlementBuildingCapacityScales() {
        int tx = 0;
        int ty = 0;
        for (int y = -10; y <= 10 && tx == 0; y++) {
            for (int x = -10; x <= 10; x++) {
                if (world.getTerrain(x, y) == TerrainType.GRASS) {
                    tx = x;
                    ty = y;
                    world.revealArea(x, y, 5);
                    break;
                }
            }
        }
        
        Settlement settlement = world.createSettlement("Cap Town", tx, ty);
        if (settlement == null) {
            return;
        }
        
        assertEquals(5, settlement.getMaxBuildings());
        
        for (int i = 0; i < 5; i++) {
            assertTrue(settlement.addBuilding(i + 1));
        }
        assertFalse(settlement.addBuilding(99));
        assertEquals(5, settlement.buildingIds.size());
        
        settlement.setPopulation(50);
        settlement.upgrade();
        assertEquals(SettlementLevel.TOWN, settlement.getLevel());
        assertEquals(15, settlement.getMaxBuildings());
        
        for (int i = 0; i < 10; i++) {
            assertTrue(settlement.addBuilding(i + 100));
        }
        assertEquals(15, settlement.buildingIds.size());
    }

    @Test
    public void testBuildBuildingOnTile() {
        int tx = 0;
        int ty = 0;
        for (int y = -10; y <= 10 && tx == 0; y++) {
            for (int x = -10; x <= 10; x++) {
                if (world.getTerrain(x, y) == TerrainType.GRASS) {
                    tx = x;
                    ty = y;
                    world.revealArea(x, y, 5);
                    break;
                }
            }
        }
        
        Settlement settlement = world.createSettlement("Build Town", tx, ty);
        if (settlement == null) {
            return;
        }
        
        int buildingX = tx + 1;
        Tile tile = world.getTile(buildingX, ty);
        
        if (tile.terrain.isBuildable() && !tile.hasBuilding()) {
            tile.buildingId = 1;
            settlement.addBuilding(1);
            settlement.addPopulation(4);
            
            assertTrue(tile.hasBuilding());
            assertEquals(1, settlement.buildingIds.size());
            assertEquals(14, settlement.population);
        }
    }

    @Test
    public void testCannotPlaceSettlementOnWater() {
        int waterX = 0;
        int waterY = 0;
        
        for (int y = -20; y <= 20; y++) {
            for (int x = -20; x <= 20; x++) {
                if (world.getTerrain(x, y) == TerrainType.WATER) {
                    waterX = x;
                    waterY = y;
                    world.reveal(x, y);
                    world.reveal(x + 1, y);
                    break;
                }
            }
            if (waterX != 0 || waterY != 0) break;
        }
        
        Settlement result = world.createSettlement("Water Town", waterX, waterY);
        assertNull("Should not create settlement on water", result);
    }

    @Test
    public void testMultipleSettlementsIndependent() {
        int[] towns = {-8, 0, 8};
        
        Settlement first = null;
        for (int x : towns) {
            for (int y = -10; y <= 10; y++) {
                if (world.getTerrain(x, y) == TerrainType.GRASS) {
                    world.revealArea(x, y, 5);
                    Settlement s = world.createSettlement("Town at " + x, x, y);
                    if (s != null && first == null) {
                        first = s;
                        break;
                    }
                }
            }
        }
        
        if (first != null) {
            first.setPopulation(100);
            assertEquals(SettlementLevel.TOWN, first.getLevel());
            
            for (Settlement s : world.getSettlements()) {
                if (s != first) {
                    assertEquals(SettlementLevel.VILLAGE, s.getLevel());
                }
            }
        }
    }

    @Test
    public void testRemoveSettlement() {
        int tx = 0;
        int ty = 0;
        for (int y = -10; y <= 10 && tx == 0; y++) {
            for (int x = -10; x <= 10; x++) {
                if (world.getTerrain(x, y) == TerrainType.GRASS) {
                    tx = x;
                    ty = y;
                    world.revealArea(x, y, 5);
                    break;
                }
            }
        }
        
        Settlement settlement = world.createSettlement("Temp Town", tx, ty);
        if (settlement == null) {
            return;
        }
        
        int settlementId = settlement.id;
        assertNotNull(world.getSettlement(settlementId));
        
        world.removeSettlement(settlementId);
        
        assertNull(world.getSettlement(settlementId));
        assertEquals(0, world.getSettlements().size());
    }

    @Test
    public void testGetSettlementAt() {
        int tx = 0;
        int ty = 0;
        for (int y = -10; y <= 10 && tx == 0; y++) {
            for (int x = -10; x <= 10; x++) {
                if (world.getTerrain(x, y) == TerrainType.GRASS) {
                    tx = x;
                    ty = y;
                    world.revealArea(x, y, 5);
                    break;
                }
            }
        }
        
        Settlement settlement = world.createSettlement("Find Me", tx, ty);
        if (settlement == null) {
            return;
        }
        
        assertEquals(settlement, world.getSettlementAt(tx, ty));
        assertEquals(settlement, world.getSettlementAt(tx + 1, ty));
        assertEquals(settlement, world.getSettlementAt(tx, ty + 1));
        assertEquals(settlement, world.getSettlementAt(tx + 2, ty));
        assertEquals(settlement, world.getSettlementAt(tx - 2, ty));
        assertNull(world.getSettlementAt(tx + 10, ty));
    }

    @Test
    public void testHomeKeyCentersCamera() {
        int homeX = worldToScreenX(0);
        int homeY = worldToScreenY(0);
        
        camera.position.set(homeX, homeY, 0);
        
        simulateKeyDown(Input.Keys.HOME);
        
        assertEquals(32f, camera.position.x, 0.01f);
    }

    @Test
    public void testZoomInOut() {
        float initialZoom = camera.zoom;
        
        simulateScroll(0, -1);
        
        assertTrue(camera.zoom > initialZoom);
        
        simulateScroll(0, 1);
        
        assertEquals(initialZoom, camera.zoom, 0.01f);
    }

    @Test
    public void testDragPansCamera() {
        int startX = 400;
        int startY = 300;
        
        simulateTouchDown(startX, startY, 0, Input.Buttons.LEFT);
        
        simulateTouchDragged(startX + 50, startY - 50, 0);
        
        float newX = camera.position.x;
        float newY = camera.position.y;
        
        simulateTouchUp(startX + 50, startY - 50, 0, Input.Buttons.LEFT);
        
        assertTrue("Camera should have moved", 
            newX != 32f || newY != 32f);
    }

    @Test
    public void testExploreModeName() {
        assertEquals("Explore", currentMode.getName());
    }
}
