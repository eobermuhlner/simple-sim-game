package ch.obermuhlner.sim.game;

public enum SettlementLevel {
    VILLAGE(1, 50, "Village", 5),
    TOWN(51, 200, "Town", 15),
    CITY(201, 500, "City", 30),
    METROPOLIS(501, Integer.MAX_VALUE, "Metropolis", 50);

    private final int minPopulation;
    private final int maxPopulation;
    private final String displayName;
    private final int radius;

    SettlementLevel(int minPopulation, int maxPopulation, String displayName, int radius) {
        this.minPopulation = minPopulation;
        this.maxPopulation = maxPopulation;
        this.displayName = displayName;
        this.radius = radius;
    }

    public int getMinPopulation() {
        return minPopulation;
    }

    public int getMaxPopulation() {
        return maxPopulation;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getRadius() {
        return radius;
    }

    public static SettlementLevel fromPopulation(int population) {
        for (SettlementLevel level : values()) {
            if (population >= level.minPopulation && population <= level.maxPopulation) {
                return level;
            }
        }
        return VILLAGE;
    }
}
