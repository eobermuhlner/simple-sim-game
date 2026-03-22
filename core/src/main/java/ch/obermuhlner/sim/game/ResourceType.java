package ch.obermuhlner.sim.game;

public enum ResourceType {
    WOOD ("Wood",   3f),
    STONE("Stone",  4f),
    FOOD ("Food",   5f),
    GOODS("Goods", 10f),
    GOLD ("Gold",   1f);

    public final String displayName;
    public final float basePrice;

    ResourceType(String displayName, float basePrice) {
        this.displayName = displayName;
        this.basePrice = basePrice;
    }
}
