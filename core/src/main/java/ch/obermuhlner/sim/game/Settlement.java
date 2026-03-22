package ch.obermuhlner.sim.game;

import java.util.ArrayList;
import java.util.List;

public class Settlement {
    private static int nextId = 1;

    public final int id;
    public String name;
    public int centerX;
    public int centerY;
    public int population;
    public final List<Integer> buildingIds = new ArrayList<>();
    public int settlementLevelIndex;
    public Specialization specialization = Specialization.NONE;

    public Settlement(String name, int centerX, int centerY) {
        this.id = nextId++;
        this.name = name;
        this.centerX = centerX;
        this.centerY = centerY;
        this.population = 10;
        this.settlementLevelIndex = SettlementLevel.VILLAGE.ordinal();
    }

    public SettlementLevel getLevel() {
        return SettlementLevel.values()[Math.min(settlementLevelIndex, SettlementLevel.values().length - 1)];
    }

    public void addPopulation(int amount) {
        population = Math.max(1, population + amount);
        updateLevel();
    }

    public void setPopulation(int amount) {
        population = Math.max(1, amount);
        updateLevel();
    }

    private void updateLevel() {
        SettlementLevel newLevel = SettlementLevel.fromPopulation(population);
        // Village → Town requires explicit specialization choice; cap population at Village max
        if (newLevel != SettlementLevel.VILLAGE && specialization == Specialization.NONE) {
            settlementLevelIndex = SettlementLevel.VILLAGE.ordinal();
            population = Math.min(population, SettlementLevel.VILLAGE.getMaxPopulation());
            return;
        }
        settlementLevelIndex = newLevel.ordinal();
    }

    public boolean addBuilding(int buildingId) {
        if (buildingIds.size() < getMaxBuildings()) {
            buildingIds.add(buildingId);
            return true;
        }
        return false;
    }

    public int getMaxBuildings() {
        switch (getLevel()) {
            case VILLAGE: return 5;
            case TOWN: return 15;
            case CITY: return 30;
            case METROPOLIS: return 50;
            default: return 5;
        }
    }

    /**
     * Returns true when the Village has reached max population and the player must
     * choose a specialization to unlock the Town upgrade.
     */
    public boolean needsSpecializationChoice() {
        return getLevel() == SettlementLevel.VILLAGE
            && population >= SettlementLevel.VILLAGE.getMaxPopulation()
            && specialization == Specialization.NONE;
    }

    /**
     * Returns true when a non-Village settlement can be upgraded to the next level.
     * Village → Town is handled via {@link #specialize(Specialization)}.
     */
    public boolean needsUpgrade() {
        return getLevel() != SettlementLevel.VILLAGE
            && getLevel() != SettlementLevel.METROPOLIS
            && population >= getLevel().getMaxPopulation();
    }

    /**
     * Applies a specialization and upgrades the Village to Town.
     * Only valid when {@link #needsSpecializationChoice()} is true.
     */
    public void specialize(Specialization spec) {
        if (!needsSpecializationChoice()) return;
        this.specialization = spec;
        SettlementLevel[] levels = SettlementLevel.values();
        settlementLevelIndex = Math.min(settlementLevelIndex + 1, levels.length - 1);
        population = SettlementLevel.TOWN.getMinPopulation();
    }

    /**
     * Returns true when the settlement can drop one level and choose a new specialization.
     */
    public boolean canRespecialize() {
        return getLevel() != SettlementLevel.VILLAGE
            && specialization != Specialization.NONE;
    }

    /**
     * Drops the settlement one level and applies the new specialization.
     * Only valid when {@link #canRespecialize()} is true.
     */
    public void respecialize(Specialization newSpec) {
        if (!canRespecialize()) return;
        this.specialization = newSpec;
        settlementLevelIndex = Math.max(0, settlementLevelIndex - 1);
        population = getLevel().getMinPopulation();
    }

    /** Upgrades a Town/City to the next level (no specialization required). */
    public void upgrade() {
        if (needsUpgrade()) {
            SettlementLevel[] levels = SettlementLevel.values();
            settlementLevelIndex = Math.min(settlementLevelIndex + 1, levels.length - 1);
            SettlementLevel newLevel = levels[settlementLevelIndex];
            population = newLevel.getMinPopulation();
        }
    }
}
