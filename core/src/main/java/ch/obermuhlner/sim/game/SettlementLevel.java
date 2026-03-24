package ch.obermuhlner.sim.game;

/**
 * Represents a settlement level (e.g. VILLAGE, TOWN, CITY, METROPOLIS).
 * Instances are created and owned by {@link GameConfig}; the ordered list of
 * known levels lives in the {@code settlement.types} section of application.yml.
 */
public class SettlementLevel {

    private final String id;
    private final String displayName;
    private final int minPopulation;
    private final int maxPopulation;
    private final int maxBuildings;
    private final int radius;
    private final int index;

    public SettlementLevel(String id, String displayName, int minPopulation, int maxPopulation, int maxBuildings, int radius, int index) {
        this.id = id;
        this.displayName = displayName;
        this.minPopulation = minPopulation;
        this.maxPopulation = maxPopulation;
        this.maxBuildings = maxBuildings;
        this.radius = radius;
        this.index = index;
    }

    /** Stable string key (e.g. "VILLAGE"). Replaces enum {@code name()}. */
    public String getId() { return id; }

    /** Alias for {@link #getId()} kept for call-site compatibility. */
    public String name() { return id; }

    public String getDisplayName()  { return displayName; }
    public int getMinPopulation()   { return minPopulation; }
    public int getMaxPopulation()   { return maxPopulation; }
    public int getMaxBuildings()    { return maxBuildings; }
    public int getRadius()          { return radius; }

    /** Position in the ordered level list. Replaces enum {@code ordinal()}. */
    public int ordinal() { return index; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SettlementLevel)) return false;
        return id.equals(((SettlementLevel) o).id);
    }

    @Override
    public int hashCode() { return id.hashCode(); }

    @Override
    public String toString() { return id; }
}
