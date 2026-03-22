package ch.obermuhlner.sim.game;

public enum RoadType {
    DIRT(1, "Dirt Road", 1.0f, 2, 0f, "64x64/single-tiles/road-dirt-ns.png"),
    STONE(2, "Stone Road", 1.5f, 4, 0.01f, null),
    COBBLESTONE(3, "Cobblestone", 2.0f, 6, 0.02f, null),
    ROMAN(4, "Roman Road", 3.0f, 10, 0.05f, null);

    private final int id;
    private final String displayName;
    private final float speedMultiplier;
    private final int capacity;
    private final float upkeepPerTick;
    private final String texturePath;

    RoadType(int id, String displayName, float speedMultiplier, int capacity, float upkeepPerTick, String texturePath) {
        this.id = id;
        this.displayName = displayName;
        this.speedMultiplier = speedMultiplier;
        this.capacity = capacity;
        this.upkeepPerTick = upkeepPerTick;
        this.texturePath = texturePath;
    }

    public int getId() { return id; }
    public String getDisplayName() { return displayName; }
    public float getSpeedMultiplier() { return speedMultiplier; }
    public int getCapacity() { return capacity; }
    public float getUpkeepPerTick() { return upkeepPerTick; }
    public String getTexturePath() { return texturePath; }

    public static RoadType fromId(int id) {
        for (RoadType type : values()) {
            if (type.id == id) return type;
        }
        return null;
    }
}
