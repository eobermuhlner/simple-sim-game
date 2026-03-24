package ch.obermuhlner.sim.game;

public enum SettlementLevel {
    VILLAGE(1, 50, "Village"),
    TOWN(51, 200, "Town"),
    CITY(201, 500, "City"),
    METROPOLIS(501, Integer.MAX_VALUE, "Metropolis");

    private final int minPopulation;
    private final int maxPopulation;
    private final String displayName;

    SettlementLevel(int minPopulation, int maxPopulation, String displayName) {
        this.minPopulation = minPopulation;
        this.maxPopulation = maxPopulation;
        this.displayName = displayName;
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

    public static SettlementLevel fromPopulation(int population) {
        for (SettlementLevel level : values()) {
            if (population >= level.minPopulation && population <= level.maxPopulation) {
                return level;
            }
        }
        return VILLAGE;
    }
}
