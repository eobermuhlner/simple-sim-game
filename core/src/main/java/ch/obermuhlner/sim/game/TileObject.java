package ch.obermuhlner.sim.game;

public interface TileObject {
    int getId();
    String getName();
    TileObjectType getType();
    boolean canPlaceOn(TerrainType terrain);
    boolean isWalkable();
}
