package ch.obermuhlner.sim.game;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

public enum TerrainType {
    DEEP_SEA(0, false, false),
    SHALLOW_SEA(1, false, false),
    GRASS(2, true, true),
    FOREST(3, true, true),
    STONE(4, true, false),
    SNOW(5, true, false);

    public boolean isWater() {
        return this == DEEP_SEA || this == SHALLOW_SEA;
    }

    private final int tileIndex;
    private final boolean buildable;
    private final boolean traversable;

    TerrainType(int tileIndex, boolean buildable, boolean traversable) {
        this.tileIndex = tileIndex;
        this.buildable = buildable;
        this.traversable = traversable;
    }

    public int getTileIndex() {
        return tileIndex;
    }

    public boolean isBuildable() {
        return buildable;
    }

    public boolean isTraversable() {
        return traversable;
    }

    public static TerrainType fromTileIndex(int index) {
        for (TerrainType type : values()) {
            if (type.tileIndex == index) {
                return type;
            }
        }
        return GRASS;
    }
}
