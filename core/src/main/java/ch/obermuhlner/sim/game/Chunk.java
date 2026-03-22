package ch.obermuhlner.sim.game;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;

public class Chunk {
    public final int cx;
    public final int cy;
    public final Tile[][] tiles;
    public final boolean[][] fog;
    public boolean dirty;

    private final Array<ChunkListener> listeners = new Array<>();

    public interface ChunkListener {
        void onChunkLoaded(Chunk chunk);
        void onChunkDirty(Chunk chunk);
    }

    public Chunk(int cx, int cy, int size) {
        this.cx = cx;
        this.cy = cy;
        this.tiles = new Tile[size][size];
        this.fog = new boolean[size][size];
        this.dirty = false;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                tiles[y][x] = new Tile();
            }
        }
    }

    public Tile getTile(int lx, int ly) {
        return tiles[ly][lx];
    }

    public void setTile(int lx, int ly, Tile tile) {
        tiles[ly][lx] = tile;
        markDirty();
    }

    public boolean isRevealed(int lx, int ly) {
        return fog[ly][lx];
    }

    public void reveal(int lx, int ly) {
        if (!fog[ly][lx]) {
            fog[ly][lx] = true;
            markDirty();
        }
    }

    public void markDirty() {
        if (!dirty) {
            dirty = true;
            for (ChunkListener listener : listeners) {
                listener.onChunkDirty(this);
            }
        }
    }

    public void addListener(ChunkListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ChunkListener listener) {
        listeners.removeValue(listener, true);
    }

    public long getKey() {
        return ((long) cx << 32) | (cy & 0xFFFFFFFFL);
    }

    public void saveFog(FileHandle file) {
        int size = fog.length;
        byte[] data = new byte[size * size / 8];
        for (int i = 0; i < size * size; i++) {
            int y = i / size;
            int x = i % size;
            if (fog[y][x]) {
                data[i / 8] |= (byte) (1 << (i % 8));
            }
        }
        file.writeBytes(data, false);
    }

    public void loadFog(FileHandle file) {
        if (!file.exists()) return;
        byte[] data = file.readBytes();
        int size = fog.length;
        if (data.length < size * size / 8) return;
        for (int i = 0; i < size * size; i++) {
            int y = i / size;
            int x = i % size;
            fog[y][x] = ((data[i / 8] >> (i % 8)) & 1) == 1;
        }
    }

    /**
     * Saves road, building, and object tile data.
     * Format: 4 bytes per tile (roadType, roadConnection, buildingId, objectId), row-major order.
     */
    public void saveTileData(FileHandle file) {
        int size = tiles.length;
        byte[] data = new byte[size * size * 4];
        int idx = 0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                Tile t = tiles[y][x];
                data[idx++] = (byte) t.roadType;
                data[idx++] = (byte) t.roadConnection;
                data[idx++] = (byte) t.buildingId;
                data[idx++] = (byte) t.objectId;
            }
        }
        file.writeBytes(data, false);
    }

    /**
     * Loads road, building, and object tile data saved by {@link #saveTileData}.
     * Supports the legacy 3-byte format (objectId not overwritten).
     */
    public void loadTileData(FileHandle file) {
        if (!file.exists()) return;
        byte[] data = file.readBytes();
        int size = tiles.length;
        if (data.length < size * size * 3) return;
        boolean hasObjectId = data.length >= size * size * 4;
        int idx = 0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                Tile t = tiles[y][x];
                t.roadType       = data[idx++] & 0xFF;
                t.roadConnection = data[idx++] & 0xFF;
                t.buildingId     = data[idx++] & 0xFF;
                if (hasObjectId) {
                    t.objectId = data[idx++] & 0xFF;
                }
            }
        }
    }
}
