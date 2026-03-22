package ch.obermuhlner.sim.game;

public class Tile {
    public TerrainType terrain;
    public int objectId;
    public int buildingId;
    // roadConnection bitmask: bit0=North(+Y), bit1=South(-Y), bit2=East(+X), bit3=West(-X)
    public int roadConnection;
    // roadType: 0=none, 1=dirt, 2=stone, 3=cobblestone, 4=roman
    public int roadType;

    public Tile() {
        this.terrain = TerrainType.GRASS;
        this.objectId = TileObjectRegistry.NONE;
        this.buildingId = 0;
        this.roadConnection = 0;
        this.roadType = 0;
    }

    public Tile(TerrainType terrain, int objectId) {
        this.terrain = terrain;
        this.objectId = objectId;
        this.buildingId = 0;
        this.roadConnection = 0;
        this.roadType = 0;
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
