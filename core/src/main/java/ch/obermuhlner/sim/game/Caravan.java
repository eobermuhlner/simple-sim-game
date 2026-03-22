package ch.obermuhlner.sim.game;

import java.util.List;

/**
 * A moving trade unit carrying goods between two settlements along a road path.
 */
public class Caravan {
    private static int nextId = 1;

    public static final int TILE_SIZE = 64;

    public final int id;
    public final int routeId;
    public final int fromSettlementId;
    public final int toSettlementId;

    // Cargo
    public ResourceType cargoType;
    public float cargoAmount;

    // Movement: effectivePath is the actual path this caravan travels (may be reversed from route.path)
    private List<int[]> effectivePath;
    public int pathIndex;           // current tile index in effectivePath
    public float subTileProgress;   // 0.0–1.0 fractional position to next tile
    public final float upkeepPerTile;  // gold cost per tile crossed

    public Caravan(int routeId, int fromId, int toId,
                   ResourceType cargo, float amount, float upkeepPerTile) {
        this.id = nextId++;
        this.routeId = routeId;
        this.fromSettlementId = fromId;
        this.toSettlementId = toId;
        this.cargoType = cargo;
        this.cargoAmount = amount;
        this.upkeepPerTile = upkeepPerTile;
        this.pathIndex = 0;
        this.subTileProgress = 0f;
    }

    public void setEffectivePath(List<int[]> path) {
        this.effectivePath = path;
    }

    public List<int[]> getEffectivePath() {
        return effectivePath;
    }

    /** World-space pixel X center of this caravan. */
    public float getWorldX() {
        List<int[]> path = effectivePath;
        if (path == null || path.isEmpty()) return 0;
        int idx  = Math.min(pathIndex, path.size() - 1);
        int next = Math.min(pathIndex + 1, path.size() - 1);
        float tx = path.get(idx)[0] + (path.get(next)[0] - path.get(idx)[0]) * subTileProgress;
        return tx * TILE_SIZE + TILE_SIZE / 2f;
    }

    /** World-space pixel Y center of this caravan. */
    public float getWorldY() {
        List<int[]> path = effectivePath;
        if (path == null || path.isEmpty()) return 0;
        int idx  = Math.min(pathIndex, path.size() - 1);
        int next = Math.min(pathIndex + 1, path.size() - 1);
        float ty = path.get(idx)[1] + (path.get(next)[1] - path.get(idx)[1]) * subTileProgress;
        return ty * TILE_SIZE + TILE_SIZE / 2f;
    }

    /** Current tile X (for fog/chunk checks). */
    public int getTileX() {
        if (effectivePath == null || effectivePath.isEmpty()) return 0;
        return effectivePath.get(Math.min(pathIndex, effectivePath.size() - 1))[0];
    }

    /** Current tile Y (for fog/chunk checks). */
    public int getTileY() {
        if (effectivePath == null || effectivePath.isEmpty()) return 0;
        return effectivePath.get(Math.min(pathIndex, effectivePath.size() - 1))[1];
    }

    /**
     * Returns the movement direction ("south", "north", "east", "west")
     * based on the vector from the current tile to the next tile in the path.
     */
    public String getDirection() {
        if (effectivePath == null || effectivePath.size() < 2) return "south";
        int cur  = Math.min(pathIndex, effectivePath.size() - 1);
        int next = Math.min(pathIndex + 1, effectivePath.size() - 1);
        if (cur == next) return "south";
        int dx = effectivePath.get(next)[0] - effectivePath.get(cur)[0];
        int dy = effectivePath.get(next)[1] - effectivePath.get(cur)[1];
        if (Math.abs(dx) >= Math.abs(dy)) {
            return dx >= 0 ? "east" : "west";
        } else {
            return dy >= 0 ? "north" : "south";
        }
    }
}
