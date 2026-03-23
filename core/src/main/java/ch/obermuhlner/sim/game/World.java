package ch.obermuhlner.sim.game;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.LongMap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
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

    private final GameConfig config;
    private boolean headless = false;

    public final TechTree techTree = new TechTree();

    public World(int chunkSize, GameConfig config) {
        this.chunkSize = chunkSize;
        this.config = config;
        this.terrainGenerator = new TerrainGenerator(config.getWorldSeed(), config);
    }

    public World(int chunkSize, GameConfig config, boolean headless) {
        this.chunkSize = chunkSize;
        this.config = config;
        this.headless = headless;
        this.terrainGenerator = new TerrainGenerator(config.getWorldSeed(), config);
    }

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
        if (!tile.isBuildable()) {
            return null;
        }
        Settlement settlement = new Settlement(name, tx, ty, config);
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
            chunk.loadFog(getFogFile(cx, cy));
            chunk.loadTileData(getTileDataFile(cx, cy));
        }
        return chunk;
    }

    private FileHandle getFogFile(int cx, int cy) {
        return com.badlogic.gdx.Gdx.files.local("data/chunks/" + cx + "_" + cy + ".fow");
    }

    private FileHandle getTileDataFile(int cx, int cy) {
        return com.badlogic.gdx.Gdx.files.local("data/chunks/" + cx + "_" + cy + ".tile");
    }

    public void saveDirtyChunks() {
        if (headless) return;
        for (Chunk chunk : chunks.values()) {
            if (chunk.dirty) {
                chunk.saveFog(getFogFile(chunk.cx, chunk.cy));
                chunk.saveTileData(getTileDataFile(chunk.cx, chunk.cy));
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
        if (!tile.terrain.isTraversable() || tile.hasObject()) return false;

        tile.roadType = type.getId();

        // Update connections for this tile and its 4 neighbors
        updateRoadConnections(tx, ty);
        updateRoadConnections(tx, ty + 1);
        updateRoadConnections(tx, ty - 1);
        updateRoadConnections(tx + 1, ty);
        updateRoadConnections(tx - 1, ty);
        markTileDirty(tx, ty);
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
        markTileDirty(tx, ty);
        routesDirty = true;
        return true;
    }

    public void setBuilding(int tx, int ty, int buildingId) {
        getTile(tx, ty).buildingId = buildingId;
        markTileDirty(tx, ty);
    }

    public void removeObject(int tx, int ty) {
        getTile(tx, ty).objectId = TileObjectRegistry.NONE;
        markTileDirty(tx, ty);
    }

    private void markTileDirty(int tx, int ty) {
        int cx = Math.floorDiv(tx, chunkSize);
        int cy = Math.floorDiv(ty, chunkSize);
        getChunk(cx, cy).markDirty();
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
     * Dijkstra pathfinding through road tiles, preferring higher-quality roads.
     * Cost per tile = 1 / speedMultiplier (Roman=0.33, Stone=0.67, Dirt=1.0).
     * Tiles within bridge_zone_radius of start or end are walkable without a road
     * to bridge the gap between settlement centers and the road network.
     * Returns null if no path exists.
     */
    public List<int[]> findRoadPath(int x1, int y1, int x2, int y2) {
        int maxManhattan = config.getMaxManhattan();
        if (Math.abs(x2 - x1) + Math.abs(y2 - y1) > maxManhattan) return null;

        int bridgeRadius = config.getBridgeZoneRadius();
        double bridgeCost = config.getBridgeZoneCost();

        Map<Long, Long> parent = new HashMap<>();
        Map<Long, Double> dist = new HashMap<>();
        // [cost, x, y]
        PriorityQueue<double[]> pq = new PriorityQueue<>((a, b) -> Double.compare(a[0], b[0]));

        long startKey = encodePos(x1, y1);
        parent.put(startKey, -1L);
        dist.put(startKey, 0.0);
        pq.add(new double[]{0.0, x1, y1});

        int[][] dirs = {{0,1},{0,-1},{1,0},{-1,0}};

        while (!pq.isEmpty()) {
            double[] cur = pq.poll();
            double curCost = cur[0];
            int cx = (int) cur[1], cy = (int) cur[2];
            long curKey = encodePos(cx, cy);

            if (curCost > dist.getOrDefault(curKey, Double.MAX_VALUE)) continue;

            if (cx == x2 && cy == y2) {
                return reconstructPath(parent, curKey);
            }

            for (int[] dir : dirs) {
                int nx = cx + dir[0], ny = cy + dir[1];
                if (Math.abs(nx - x1) + Math.abs(ny - y1) > maxManhattan) continue;

                boolean nearStart = Math.abs(nx - x1) <= bridgeRadius && Math.abs(ny - y1) <= bridgeRadius;
                boolean nearEnd   = Math.abs(nx - x2) <= bridgeRadius && Math.abs(ny - y2) <= bridgeRadius;
                int roadType = getTile(nx, ny).roadType;
                boolean hasRoad = roadType > 0;

                if (!hasRoad && !nearStart && !nearEnd) continue;

                double stepCost;
                if (hasRoad) {
                    RoadType rt = RoadType.fromId(roadType);
                    stepCost = rt != null ? 1.0 / config.getRoadSpeedMultiplier(rt) : 1.0;
                } else {
                    stepCost = bridgeCost;
                }

                double newDist = curCost + stepCost;
                long nKey = encodePos(nx, ny);
                if (newDist < dist.getOrDefault(nKey, Double.MAX_VALUE)) {
                    dist.put(nKey, newDist);
                    parent.put(nKey, curKey);
                    pq.add(new double[]{newDist, nx, ny});
                }
            }
        }
        return null;
    }

    private List<int[]> reconstructPath(Map<Long, Long> parent, long endKey) {
        List<int[]> path = new ArrayList<>();
        long cur = endKey;
        while (cur != -1L) {
            path.add(new int[]{decodeX(cur), decodeY(cur)});
            Long p = parent.get(cur);
            if (p == null) break;
            cur = p;
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

    public void saveSettlements(FileHandle file) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeInt(settlements.size());
            for (Settlement s : settlements) {
                dos.writeInt(s.id);
                dos.writeUTF(s.name);
                dos.writeInt(s.centerX);
                dos.writeInt(s.centerY);
                dos.writeInt(s.population);
                dos.writeInt(s.settlementLevelIndex);
                dos.writeInt(s.specialization.ordinal());
                dos.writeFloat(s.wood);
                dos.writeFloat(s.stone);
                dos.writeFloat(s.food);
                dos.writeFloat(s.goods);
                dos.writeFloat(s.gold);
                dos.writeFloat(s.storageCapacity);
                dos.writeFloat(s.smoothedWoodProd);
                dos.writeFloat(s.smoothedStoneProd);
                dos.writeFloat(s.smoothedFoodProd);
                dos.writeFloat(s.smoothedGoodsProd);
                dos.writeFloat(s.woodPriceMult);
                dos.writeFloat(s.stonePriceMult);
                dos.writeFloat(s.foodPriceMult);
                dos.writeFloat(s.goodsPriceMult);
                dos.writeInt(s.buildingIds.size());
                for (int bid : s.buildingIds) dos.writeInt(bid);
            }
            dos.flush();
            file.writeBytes(baos.toByteArray(), false);
        } catch (Exception e) {
            // ignore
        }
    }

    /** Returns true if at least one settlement was loaded. */
    public boolean loadSettlements(FileHandle file) {
        if (!file.exists()) return false;
        try {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(file.readBytes()));
            int count = dis.readInt();
            Specialization[] specs = Specialization.values();
            for (int i = 0; i < count; i++) {
                int id        = dis.readInt();
                String name   = dis.readUTF();
                int cx        = dis.readInt();
                int cy        = dis.readInt();
                int pop       = dis.readInt();
                int levelIdx  = dis.readInt();
                int specOrd   = dis.readInt();
                Specialization spec = (specOrd >= 0 && specOrd < specs.length) ? specs[specOrd] : Specialization.NONE;
                Settlement s = new Settlement(id, name, cx, cy, pop, levelIdx, spec, config);
                s.wood            = dis.readFloat();
                s.stone           = dis.readFloat();
                s.food            = dis.readFloat();
                s.goods           = dis.readFloat();
                s.gold            = dis.readFloat();
                s.storageCapacity = dis.readFloat();
                s.smoothedWoodProd  = dis.readFloat();
                s.smoothedStoneProd = dis.readFloat();
                s.smoothedFoodProd  = dis.readFloat();
                s.smoothedGoodsProd = dis.readFloat();
                s.woodPriceMult   = dis.readFloat();
                s.stonePriceMult  = dis.readFloat();
                s.foodPriceMult   = dis.readFloat();
                s.goodsPriceMult  = dis.readFloat();
                int bidCount = dis.readInt();
                for (int j = 0; j < bidCount; j++) s.buildingIds.add(dis.readInt());
                settlements.add(s);
            }
            return count > 0;
        } catch (Exception e) {
            return false;
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
