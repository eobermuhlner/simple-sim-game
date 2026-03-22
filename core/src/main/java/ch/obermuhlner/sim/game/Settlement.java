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
        settlementLevelIndex = SettlementLevel.fromPopulation(population).ordinal();
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

    public boolean needsUpgrade() {
        return getLevel() != SettlementLevel.METROPOLIS && population >= getLevel().getMaxPopulation();
    }

    public void upgrade() {
        if (needsUpgrade()) {
            SettlementLevel[] levels = SettlementLevel.values();
            settlementLevelIndex = Math.min(settlementLevelIndex + 1, levels.length - 1);
            SettlementLevel newLevel = levels[settlementLevelIndex];
            population = newLevel.getMinPopulation();
        }
    }
}
