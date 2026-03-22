package ch.obermuhlner.sim.game;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.LongMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.Consumer;

public class World {
    private final int chunkSize;
    private final LongMap<Chunk> chunks = new LongMap<>();
    private final TerrainGenerator terrainGenerator;
    private final List<Settlement> settlements = new ArrayList<>();
    private int nextSettlementId = 1;

    private final List<TradeRoute> tradeRoutes = new ArrayList<>();
    private final List<Caravan> caravans = new ArrayList<>();
    public boolean routesDirty = true;

    public World(int chunkSize, long seed) {
        this.chunkSize = chunkSize;
        this.terrainGenerator = new TerrainGenerator(seed);
    }
    
    public World(int chunkSize, long seed, boolean headless) {
        this.chunkSize = chunkSize;
        this.terrainGenerator = new TerrainGenerator(seed);
        this.headless = headless;
    }
    
    private boolean headless = false;

    public Settlement createStarterSettlement() {
        for (int radius = 0; radius < 50; radius++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    if (Math.abs(dx) != radius && Math.abs(dy) != radius) continue;
                    int tx = dx;
                    int ty = dy;
                    if (isSuitableForStarter(tx, ty)) {
                        Settlement settlement = createSettlement("Starter Village", tx, ty);
                        if (settlement != null) {
                            return settlement;
                        }
                    }
                }
            }
        }
        return null;
    }

    private boolean isSuitableForStarter(int tx, int ty) {
        TerrainType terrain = getTerrain(tx, ty);
        if (terrain != TerrainType.GRASS) {
            return false;
        }

        int grassCount = 0;
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                if (getTerrain(tx + dx, ty + dy) == TerrainType.GRASS) {
                    grassCount++;
                }
            }
        }
        if (grassCount < 15) {
            return false;
        }

        java.util.Set<TerrainType> neighbors = new java.util.HashSet<>();
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                if (dx == 0 && dy == 0) continue;
                neighbors.add(getTerrain(tx + dx, ty + dy));
            }
        }
        return neighbors.size() >= 2;
    }

    public Settlement createSettlement(String name, int tx, int ty) {
        Tile tile = getTile(tx, ty);
        if (!tile.terrain.isBuildable()) {
            return null;
        }
        Settlement settlement = new Settlement(name, tx, ty);
        settlements.add(settlement);
        revealArea(tx, ty, 3);
        routesDirty = true;
        return settlement;
    }

    public void removeSettlement(int id) {
        settlements.removeIf(s -> s.id == id);
        tradeRoutes.removeIf(r -> r.connects(id));
        caravans.removeIf(c -> c.fromSettlementId == id || c.toSettlementId == id);
        routesDirty = true;
    }

    public Settlement getSettlement(int id) {
        for (Settlement s : settlements) {
            if (s.id == id) return s;
        }
        return null;
    }

    public Settlement getSettlementAt(int tx, int ty) {
        for (Settlement s : settlements) {
            if (s.centerX == tx && s.centerY == ty) return s;
            int dx = Math.abs(tx - s.centerX);
            int dy = Math.abs(ty - s.centerY);
            if (dx <= 2 && dy <= 2) return s;
        }
        return null;
    }

    public List<Settlement> getSettlements() {
        return settlements;
    }

    public void forEachSettlement(Consumer<Settlement> consumer) {
        settlements.forEach(consumer);
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
        if (!headless) {
            FileHandle fogFile = getFogFile(cx, cy);
            chunk.loadFog(fogFile);
        }
        return chunk;
    }

    private FileHandle getFogFile(int cx, int cy) {
        return com.badlogic.gdx.Gdx.files.local("data/chunks/" + cx + "_" + cy + ".fow");
    }

    public void saveDirtyChunks() {
        if (headless) return;
        for (Chunk chunk : chunks.values()) {
            if (chunk.dirty) {
                chunk.saveFog(getFogFile(chunk.cx, chunk.cy));
                chunk.dirty = false;
            }
        }
    }

    // Road connection bitmask constants
    public static final int ROAD_NORTH = 1;
    public static final int ROAD_SOUTH = 2;
    public static final int ROAD_EAST  = 4;
    public static final int ROAD_WEST  = 8;

    public boolean placeRoad(int tx, int ty, RoadType type) {
        Tile tile = getTile(tx, ty);
        if (!tile.terrain.isTraversable()) return false;

        tile.roadType = type.getId();

        // Update connections for this tile and its 4 neighbors
        updateRoadConnections(tx, ty);
        updateRoadConnections(tx, ty + 1);
        updateRoadConnections(tx, ty - 1);
        updateRoadConnections(tx + 1, ty);
        updateRoadConnections(tx - 1, ty);
        routesDirty = true;
        return true;
    }

    public boolean removeRoad(int tx, int ty) {
        Tile tile = getTile(tx, ty);
        if (tile.roadType == 0) return false;

        tile.roadType = 0;
        tile.roadConnection = 0;

        updateRoadConnections(tx, ty + 1);
        updateRoadConnections(tx, ty - 1);
        updateRoadConnections(tx + 1, ty);
        updateRoadConnections(tx - 1, ty);
        routesDirty = true;
        return true;
    }

    private void updateRoadConnections(int tx, int ty) {
        Tile tile = getTile(tx, ty);
        if (tile.roadType == 0) {
            tile.roadConnection = 0;
            return;
        }
        int conn = 0;
        if (getTile(tx, ty + 1).roadType != 0) conn |= ROAD_NORTH;
        if (getTile(tx, ty - 1).roadType != 0) conn |= ROAD_SOUTH;
        if (getTile(tx + 1, ty).roadType != 0) conn |= ROAD_EAST;
        if (getTile(tx - 1, ty).roadType != 0) conn |= ROAD_WEST;
        tile.roadConnection = conn;
    }

    // ---- Trade route & caravan management ----

    public List<TradeRoute> getTradeRoutes() { return tradeRoutes; }
    public List<Caravan> getCaravans() { return caravans; }

    public TradeRoute getTradeRoute(int idA, int idB) {
        int lo = Math.min(idA, idB), hi = Math.max(idA, idB);
        for (TradeRoute r : tradeRoutes) {
            if (r.idA == lo && r.idB == hi) return r;
        }
        return null;
    }

    public void addTradeRoute(TradeRoute route) {
        tradeRoutes.add(route);
    }

    public void removeTradeRoute(int routeId) {
        tradeRoutes.removeIf(r -> r.id == routeId);
        caravans.removeIf(c -> c.routeId == routeId);
    }

    public TradeRoute getTradeRouteById(int routeId) {
        for (TradeRoute r : tradeRoutes) {
            if (r.id == routeId) return r;
        }
        return null;
    }

    /**
     * BFS pathfinding through road tiles.
     * Tiles within 2 of start or end are treated as walkable even without a road
     * to bridge the gap between settlement centers and the road network.
     * Returns null if no path exists. Max search: 300 tiles Manhattan distance.
     */
    public List<int[]> findRoadPath(int x1, int y1, int x2, int y2) {
        int maxManhattan = 300;
        if (Math.abs(x2 - x1) + Math.abs(y2 - y1) > maxManhattan) return null;

        Map<Long, Long> parent = new HashMap<>();
        Queue<int[]> queue = new LinkedList<>();
        long startKey = encodePos(x1, y1);
        parent.put(startKey, -1L);
        queue.add(new int[]{x1, y1});

        int[][] dirs = {{0,1},{0,-1},{1,0},{-1,0}};

        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            int cx = cur[0], cy = cur[1];

            if (Math.abs(cx - x2) <= 2 && Math.abs(cy - y2) <= 2) {
                return reconstructPath(parent, encodePos(cx, cy), x2, y2);
            }

            for (int[] dir : dirs) {
                int nx = cx + dir[0], ny = cy + dir[1];
                if (Math.abs(nx - x1) + Math.abs(ny - y1) > maxManhattan) continue;
                long nKey = encodePos(nx, ny);
                if (parent.containsKey(nKey)) continue;

                boolean nearStart = Math.abs(nx - x1) <= 2 && Math.abs(ny - y1) <= 2;
                boolean nearEnd   = Math.abs(nx - x2) <= 2 && Math.abs(ny - y2) <= 2;
                boolean hasRoad   = getTile(nx, ny).roadType > 0;

                if (hasRoad || nearStart || nearEnd) {
                    parent.put(nKey, encodePos(cx, cy));
                    queue.add(new int[]{nx, ny});
                }
            }
        }
        return null;
    }

    private List<int[]> reconstructPath(Map<Long, Long> parent, long endKey, int x2, int y2) {
        List<int[]> path = new ArrayList<>();
        long cur = endKey;
        while (cur != -1L) {
            int tx = decodeX(cur), ty = decodeY(cur);
            path.add(new int[]{tx, ty});
            Long p = parent.get(cur);
            if (p == null) break;
            cur = p;
        }
        // Add destination endpoint if not already at exact position
        if (!path.isEmpty()) {
            int[] last = path.get(0); // path is reversed at this point
            if (last[0] != x2 || last[1] != y2) {
                path.add(0, new int[]{x2, y2});
            }
        }
        Collections.reverse(path);
        return path;
    }

    private static long encodePos(int tx, int ty) {
        return ((long)(tx + 1000000)) * 3000000L + (ty + 1000000);
    }

    private static int decodeX(long encoded) {
        return (int)(encoded / 3000000L) - 1000000;
    }

    private static int decodeY(long encoded) {
        return (int)(encoded % 3000000L) - 1000000;
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
