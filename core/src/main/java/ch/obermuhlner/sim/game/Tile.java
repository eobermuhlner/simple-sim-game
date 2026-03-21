package ch.obermuhlner.sim.game;

public class Tile {
    public TerrainType terrain;
    public int objectId;
    public int buildingId;
    public int roadConnection;

    public Tile() {
        this.terrain = TerrainType.GRASS;
        this.objectId = TileObjectRegistry.NONE;
        this.buildingId = 0;
        this.roadConnection = 0;
    }

    public Tile(TerrainType terrain, int objectId) {
        this.terrain = terrain;
        this.objectId = objectId;
        this.buildingId = 0;
        this.roadConnection = 0;
    }

    public boolean hasObject() {
        return objectId != TileObjectRegistry.NONE;
    }

    public boolean hasBuilding() {
        return buildingId != 0;
    }

    public boolean hasRoad() {
        return roadConnection != 0;
    }

    public TileObject getObject() {
        return TileObjectRegistry.get(objectId);
    }

    public boolean isBuildable() {
        return terrain.isBuildable() && !hasBuilding() && !hasObject();
    }

    public boolean isWalkable() {
        if (!terrain.isTraversable()) return false;
        TileObject obj = getObject();
        if (obj != null && !obj.isWalkable()) return false;
        return true;
    }
}
