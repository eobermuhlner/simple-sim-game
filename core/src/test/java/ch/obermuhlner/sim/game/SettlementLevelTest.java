package ch.obermuhlner.sim.game;

import org.junit.Test;
import static org.junit.Assert.*;

public class SettlementLevelTest {

    private static final GameConfig CONFIG = new GameConfig(new GameConfig.Root());

    @Test
    public void testLevelForPopulation() {
        assertEquals(CONFIG.getLevelById("VILLAGE"),    CONFIG.getLevelForPopulation(1));
        assertEquals(CONFIG.getLevelById("VILLAGE"),    CONFIG.getLevelForPopulation(25));
        assertEquals(CONFIG.getLevelById("VILLAGE"),    CONFIG.getLevelForPopulation(50));

        assertEquals(CONFIG.getLevelById("TOWN"),       CONFIG.getLevelForPopulation(51));
        assertEquals(CONFIG.getLevelById("TOWN"),       CONFIG.getLevelForPopulation(100));
        assertEquals(CONFIG.getLevelById("TOWN"),       CONFIG.getLevelForPopulation(200));

        assertEquals(CONFIG.getLevelById("CITY"),       CONFIG.getLevelForPopulation(201));
        assertEquals(CONFIG.getLevelById("CITY"),       CONFIG.getLevelForPopulation(350));
        assertEquals(CONFIG.getLevelById("CITY"),       CONFIG.getLevelForPopulation(500));

        assertEquals(CONFIG.getLevelById("METROPOLIS"), CONFIG.getLevelForPopulation(501));
        assertEquals(CONFIG.getLevelById("METROPOLIS"), CONFIG.getLevelForPopulation(1000));
        assertEquals(CONFIG.getLevelById("METROPOLIS"), CONFIG.getLevelForPopulation(Integer.MAX_VALUE));
    }

    @Test
    public void testLevelPopulationRanges() {
        assertEquals(1,               CONFIG.getLevelById("VILLAGE").getMinPopulation());
        assertEquals(50,              CONFIG.getLevelById("VILLAGE").getMaxPopulation());

        assertEquals(51,              CONFIG.getLevelById("TOWN").getMinPopulation());
        assertEquals(200,             CONFIG.getLevelById("TOWN").getMaxPopulation());

        assertEquals(201,             CONFIG.getLevelById("CITY").getMinPopulation());
        assertEquals(500,             CONFIG.getLevelById("CITY").getMaxPopulation());

        assertEquals(501,             CONFIG.getLevelById("METROPOLIS").getMinPopulation());
        assertEquals(Integer.MAX_VALUE, CONFIG.getLevelById("METROPOLIS").getMaxPopulation());
    }

    @Test
    public void testLevelDisplayNames() {
        assertEquals("Village",    CONFIG.getLevelById("VILLAGE").getDisplayName());
        assertEquals("Town",       CONFIG.getLevelById("TOWN").getDisplayName());
        assertEquals("City",       CONFIG.getLevelById("CITY").getDisplayName());
        assertEquals("Metropolis", CONFIG.getLevelById("METROPOLIS").getDisplayName());
    }

    @Test
    public void testLevelOrder() {
        assertEquals(0, CONFIG.getLevelById("VILLAGE").ordinal());
        assertEquals(1, CONFIG.getLevelById("TOWN").ordinal());
        assertEquals(2, CONFIG.getLevelById("CITY").ordinal());
        assertEquals(3, CONFIG.getLevelById("METROPOLIS").ordinal());
    }

    @Test
    public void testGetSettlementTypes() {
        assertEquals(4, CONFIG.getSettlementTypes().size());
        assertEquals("VILLAGE",    CONFIG.getFirstLevel().getId());
        assertEquals("METROPOLIS", CONFIG.getLastLevel().getId());
        assertEquals("TOWN",       CONFIG.getSecondLevel().getId());
    }

    @Test
    public void testGetLevelByIdUnknownReturnsNull() {
        assertNull(CONFIG.getLevelById("UNKNOWN_LEVEL"));
    }
}
