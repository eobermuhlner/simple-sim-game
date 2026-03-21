package ch.obermuhlner.sim.game;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.LongMap;

public class World {
    private final int chunkSize;
    private final LongMap<Chunk> chunks = new LongMap<>();
    private final TerrainGenerator terrainGenerator;

    public World(int chunkSize, long seed) {
        this.chunkSize = chunkSize;
        this.terrainGenerator = new TerrainGenerator(seed);
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public Chunk getChunk(int cx, int cy) {
        long key = ((long) cx << 32) | (cy & 0xFFFFFFFFL);
        Chunk chunk = chunks.get(key);
        if (chunk == null) {
            chunk = createChunk(cx, cy);
            chunks.put(key, chunk);
        }
        return chunk;
    }

    public Tile getTile(int tx, int ty) {
        int cx = Math.floorDiv(tx, chunkSize);
        int cy = Math.floorDiv(ty, chunkSize);
        int lx = Math.floorMod(tx, chunkSize);
        int ly = Math.floorMod(ty, chunkSize);
        return getChunk(cx, cy).getTile(lx, ly);
    }

    public TerrainType getTerrain(int tx, int ty) {
        return getTile(tx, ty).terrain;
    }

    public boolean isRevealed(int tx, int ty) {
        int cx = Math.floorDiv(tx, chunkSize);
        int cy = Math.floorDiv(ty, chunkSize);
        int lx = Math.floorMod(tx, chunkSize);
        int ly = Math.floorMod(ty, chunkSize);
        return getChunk(cx, cy).isRevealed(lx, ly);
    }

    public void reveal(int tx, int ty) {
        int cx = Math.floorDiv(tx, chunkSize);
        int cy = Math.floorDiv(ty, chunkSize);
        int lx = Math.floorMod(tx, chunkSize);
        int ly = Math.floorMod(ty, chunkSize);
        getChunk(cx, cy).reveal(lx, ly);
    }

    public void revealArea(int centerX, int centerY, int radius) {
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (dx * dx + dy * dy <= radius * radius) {
                    reveal(centerX + dx, centerY + dy);
                }
            }
        }
    }

    public boolean hasRevealedNeighbor(int tx, int ty) {
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] dir : dirs) {
            if (isRevealed(tx + dir[0], ty + dir[1])) {
                return true;
            }
        }
        return false;
    }

    public int[] getRevealedNeighbors(int tx, int ty) {
        IntArray neighbors = new IntArray();
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] dir : dirs) {
            int nx = tx + dir[0];
            int ny = ty + dir[1];
            if (isRevealed(nx, ny)) {
                neighbors.add(nx);
                neighbors.add(ny);
            }
        }
        return neighbors.toArray();
    }

    private Chunk createChunk(int cx, int cy) {
        Chunk chunk = new Chunk(cx, cy, chunkSize);
        terrainGenerator.generate(chunk);
        FileHandle fogFile = getFogFile(cx, cy);
        chunk.loadFog(fogFile);
        return chunk;
    }

    private FileHandle getFogFile(int cx, int cy) {
        return com.badlogic.gdx.Gdx.files.local("data/chunks/" + cx + "_" + cy + ".fow");
    }

    public void saveDirtyChunks() {
        for (Chunk chunk : chunks.values()) {
            if (chunk.dirty) {
                chunk.saveFog(getFogFile(chunk.cx, chunk.cy));
                chunk.dirty = false;
            }
        }
    }

    public void forEachVisibleChunk(int startTx, int startTy, int endTx, int endTy, ChunkConsumer consumer) {
        int startCx = Math.floorDiv(startTx, chunkSize);
        int endCx = (int) Math.ceil((double) endTx / chunkSize);
        int startCy = Math.floorDiv(startTy, chunkSize);
        int endCy = (int) Math.ceil((double) endTy / chunkSize);

        for (int cy = startCy; cy <= endCy; cy++) {
            for (int cx = startCx; cx <= endCx; cx++) {
                consumer.accept(getChunk(cx, cy));
            }
        }
    }

    @FunctionalInterface
    public interface ChunkConsumer {
        void accept(Chunk chunk);
    }
}
