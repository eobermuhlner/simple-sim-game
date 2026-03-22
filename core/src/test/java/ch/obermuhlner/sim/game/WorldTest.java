package ch.obermuhlner.sim.game;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.List;

public class WorldTest {

    private World world;
    private static final int CHUNK_SIZE = 16;
    private static final long SEED = 12345L;

    @Before
    public void setUp() {
        world = new World(CHUNK_SIZE, SEED, true);
    }

    @Test
    public void testGetTile() {
        Tile tile = world.getTile(0, 0);
        assertNotNull(tile);
        assertNotNull(tile.terrain);
    }

    @Test
    public void testGetChunk() {
        Chunk chunk1 = world.getChunk(0, 0);
        Chunk chunk2 = world.getChunk(0, 0);
        assertSame(chunk1, chunk2);
        
        Chunk chunk3 = world.getChunk(1, 0);
        assertNotSame(chunk1, chunk3);
    }

    @Test
    public void testReveal() {
        assertFalse(world.isRevealed(0, 0));
        
        world.reveal(0, 0);
        assertTrue(world.isRevealed(0, 0));
    }

    @Test
    public void testRevealArea() {
        assertFalse(world.isRevealed(5, 5));
        
        world.revealArea(5, 5, 2);
        
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                if (dx * dx + dy * dy <= 4) {
                    assertTrue(world.isRevealed(5 + dx, 5 + dy));
                }
            }
        }
        
        assertFalse(world.isRevealed(8, 5));
    }

    @Test
    public void testHasRevealedNeighbor() {
        assertFalse(world.hasRevealedNeighbor(0, 0));
        
        world.reveal(0, 0);
        assertTrue(world.hasRevealedNeighbor(1, 0));
        assertTrue(world.hasRevealedNeighbor(0, 1));
        assertFalse(world.hasRevealedNeighbor(2, 0));
    }

    @Test
    public void testGetTerrain() {
        TerrainType terrain = world.getTerrain(0, 0);
        assertNotNull(terrain);
    }

    @Test
    public void testEmptySettlements() {
        assertTrue(world.getSettlements().isEmpty());
    }

    @Test
    public void testGetSettlementReturnsNull() {
        assertNull(world.getSettlement(1));
        assertNull(world.getSettlement(999));
    }

    @Test
    public void testGetSettlementAtReturnsNull() {
        assertNull(world.getSettlementAt(0, 0));
    }

    @Test
    public void testRemoveNonExistentSettlement() {
        world.removeSettlement(999);
        assertTrue(world.getSettlements().isEmpty());
    }

    @Test
    public void testForEachSettlementOnEmptyWorld() {
        final int[] count = {0};
        world.forEachSettlement(s -> count[0]++);
        assertEquals(0, count[0]);
    }

    @Test
    public void testChunkSize() {
        assertEquals(CHUNK_SIZE, world.getChunkSize());
    }
}
