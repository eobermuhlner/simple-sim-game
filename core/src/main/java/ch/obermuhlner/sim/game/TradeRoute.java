package ch.obermuhlner.sim.game;

import java.util.List;

/**
 * Represents a connection between two settlements via a road path.
 * idA is always the lower settlement ID to ensure uniqueness.
 */
public class TradeRoute {
    private static int nextId = 1;

    public final int id;
    public final int idA;  // lower id first
    public final int idB;
    public List<int[]> path;  // tile coords [{x,y},...] from A.center to B.center
    public int pathLength;
    public int activeCaravans = 0;
    public boolean isSea = false;  // true for sea trade routes (bypasses roads)

    public TradeRoute(int idA, int idB, List<int[]> path) {
        this.id = nextId++;
        this.idA = Math.min(idA, idB);
        this.idB = Math.max(idA, idB);
        this.path = path;
        this.pathLength = path.size();
    }

    public boolean canSpawnCaravan(int maxCaravans) {
        return activeCaravans < maxCaravans;
    }

    /** Ticks between spawns: baseInterval / sqrt(popA + popB). */
    public float spawnInterval(int popA, int popB, float baseInterval) {
        return baseInterval / (float) Math.sqrt(Math.max(1, popA + popB));
    }

    public boolean connects(int settlementId) {
        return idA == settlementId || idB == settlementId;
    }
}
