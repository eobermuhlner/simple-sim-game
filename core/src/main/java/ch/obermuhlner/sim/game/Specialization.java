package ch.obermuhlner.sim.game;

public enum Specialization {
    NONE("None",           1.0f, 1.0f, 1.0f, 1.0f),
    LOGGING_CAMP("Logging Camp",   2.0f, 0.5f, 0.5f, 1.0f),
    MINING_TOWN("Mining Town",    0.5f, 2.0f, 0.5f, 1.0f),
    FARMING_VILLAGE("Farm Village",  0.5f, 0.5f, 2.0f, 1.0f),
    TRADE_HUB("Trade Hub",     1.0f, 1.0f, 1.0f, 1.5f);

    public final String displayName;
    public final float woodMultiplier;
    public final float stoneMultiplier;
    public final float foodMultiplier;
    public final float goodsMultiplier;

    Specialization(String displayName, float wood, float stone, float food, float goods) {
        this.displayName = displayName;
        this.woodMultiplier = wood;
        this.stoneMultiplier = stone;
        this.foodMultiplier = food;
        this.goodsMultiplier = goods;
    }

    /** Returns a short production modifier string, e.g. "W:2.0 S:0.5 F:0.5" */
    public String getProductionSummary() {
        if (this == NONE) return "";
        return String.format("W:%.1f S:%.1f F:%.1f G:%.1f",
            woodMultiplier, stoneMultiplier, foodMultiplier, goodsMultiplier);
    }
}
