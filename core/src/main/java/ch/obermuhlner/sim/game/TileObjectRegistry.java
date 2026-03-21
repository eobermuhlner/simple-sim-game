package ch.obermuhlner.sim.game;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class TileObjectRegistry {
    private static final Map<Integer, TileObject> OBJECTS = new HashMap<>();

    public static final int NONE = 0;
    public static final int TREE_LARGE = 1;
    public static final int TREE_SMALL = 2;
    public static final int BOULDER_LARGE = 3;
    public static final int BOULDER_SMALL = 4;
    public static final int BOULDER_SNOW = 5;

    public static void register(int id, TileObject object) {
        OBJECTS.put(id, object);
    }

    public static TileObject get(int id) {
        return OBJECTS.get(id);
    }

    public static void init() {
        register(NONE, new SimpleObject(NONE, "None", TileObjectType.NATURAL, t -> false, true));
        register(TREE_LARGE, new SimpleObject(TREE_LARGE, "Large Tree", TileObjectType.NATURAL, 
            t -> t == TerrainType.GRASS || t == TerrainType.FOREST, false));
        register(TREE_SMALL, new SimpleObject(TREE_SMALL, "Small Tree", TileObjectType.NATURAL, 
            t -> t == TerrainType.GRASS, false));
        register(BOULDER_LARGE, new SimpleObject(BOULDER_LARGE, "Large Boulder", TileObjectType.NATURAL,
            t -> t == TerrainType.GRASS || t == TerrainType.FOREST, false));
        register(BOULDER_SMALL, new SimpleObject(BOULDER_SMALL, "Small Boulder", TileObjectType.NATURAL,
            t -> t == TerrainType.GRASS || t == TerrainType.STONE, true));
        register(BOULDER_SNOW, new SimpleObject(BOULDER_SNOW, "Snow Boulder", TileObjectType.NATURAL,
            t -> t == TerrainType.SNOW, true));
    }

    private static class SimpleObject implements TileObject {
        private final int id;
        private final String name;
        private final TileObjectType type;
        private final Predicate<TerrainType> validTerrain;
        private final boolean walkable;

        SimpleObject(int id, String name, TileObjectType type, Predicate<TerrainType> validTerrain, boolean walkable) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.validTerrain = validTerrain;
            this.walkable = walkable;
        }

        @Override
        public int getId() { return id; }
        @Override
        public String getName() { return name; }
        @Override
        public TileObjectType getType() { return type; }
        @Override
        public boolean canPlaceOn(TerrainType terrain) { return validTerrain.test(terrain); }
        @Override
        public boolean isWalkable() { return walkable; }
    }
}
