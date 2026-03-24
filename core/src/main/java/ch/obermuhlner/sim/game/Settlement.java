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

    // Resource stockpiles
    public float wood  = 0f;
    public float stone = 0f;
    public float food  = 0f;
    public float goods = 0f;
    public float gold;

    public float storageCapacity;

    // Smoothed production rates (exponential moving average)
    public float smoothedWoodProd  = 0f;
    public float smoothedStoneProd = 0f;
    public float smoothedFoodProd  = 0f;
    public float smoothedGoodsProd = 0f;

    // Dynamic price multipliers (clamped to [price_min, price_max])
    public float woodPriceMult  = 1f;
    public float stonePriceMult = 1f;
    public float foodPriceMult  = 1f;
    public float goodsPriceMult = 1f;

    private final GameConfig config;

    public Settlement(String name, int centerX, int centerY) {
        this(name, centerX, centerY, new GameConfig(new GameConfig.Root()));
    }

    public Settlement(String name, int centerX, int centerY, GameConfig config) {
        this.id = nextId++;
        this.name = name;
        this.centerX = centerX;
        this.centerY = centerY;
        this.config = config;
        this.population = config.getStartingPopulation();
        this.gold = config.getStartingGold();
        this.storageCapacity = config.getStorageCapacity();
        this.settlementLevelIndex = 0;
    }

    /** Package-private constructor for deserialization — does NOT trigger normal initialization. */
    Settlement(int id, String name, int centerX, int centerY, int population, int levelIndex, Specialization spec, GameConfig config) {
        this.id = id;
        if (id >= nextId) nextId = id + 1;
        this.name = name;
        this.centerX = centerX;
        this.centerY = centerY;
        this.population = population;
        this.settlementLevelIndex = levelIndex;
        this.specialization = spec;
        this.config = config;
        this.storageCapacity = config.getStorageCapacity();
        this.gold = config.getStartingGold();
    }

    public SettlementLevel getLevel() {
        List<SettlementLevel> levels = config.getSettlementTypes();
        return levels.get(Math.min(settlementLevelIndex, levels.size() - 1));
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
        SettlementLevel newLevel = config.getLevelForPopulation(population);
        SettlementLevel firstLevel = config.getFirstLevel();
        // First level → second level requires explicit specialization choice; cap population at first level max
        if (!firstLevel.equals(newLevel) && specialization == Specialization.NONE) {
            settlementLevelIndex = 0;
            population = Math.min(population, firstLevel.getMaxPopulation());
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
        return config.getMaxBuildings(getLevel());
    }

    public int getMaxPopulation() {
        return getLevel().getMaxPopulation();
    }

    /**
     * Returns true when the Village has reached max population and the player must
     * choose a specialization to unlock the Town upgrade.
     */
    public boolean needsSpecializationChoice() {
        SettlementLevel level = getLevel();
        return level.equals(config.getFirstLevel())
            && population >= level.getMaxPopulation()
            && specialization == Specialization.NONE;
    }

    /**
     * Returns true when a non-Village settlement can be upgraded to the next level.
     * Village → Town is handled via {@link #specialize(Specialization)}.
     */
    public boolean needsUpgrade() {
        SettlementLevel level = getLevel();
        return !level.equals(config.getFirstLevel())
            && !level.equals(config.getLastLevel())
            && population >= level.getMaxPopulation();
    }

    /**
     * Applies a specialization and upgrades the Village to Town.
     * Only valid when {@link #needsSpecializationChoice()} is true.
     */
    public void specialize(Specialization spec) {
        if (!needsSpecializationChoice()) return;
        this.specialization = spec;
        int maxIndex = config.getSettlementTypes().size() - 1;
        settlementLevelIndex = Math.min(settlementLevelIndex + 1, maxIndex);
        population = config.getSecondLevel().getMinPopulation();
    }

    /**
     * Returns true when the settlement can drop one level and choose a new specialization.
     */
    public boolean canRespecialize() {
        return !getLevel().equals(config.getFirstLevel())
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
        population = getLevel().getMaxPopulation();
    }

    /** Upgrades to the next level (no specialization required). */
    public void upgrade() {
        if (needsUpgrade()) {
            int maxIndex = config.getSettlementTypes().size() - 1;
            settlementLevelIndex = Math.min(settlementLevelIndex + 1, maxIndex);
            population = getLevel().getMinPopulation();
        }
    }

    public float getResource(ResourceType type) {
        switch (type) {
            case WOOD:  return wood;
            case STONE: return stone;
            case FOOD:  return food;
            case GOODS: return goods;
            case GOLD:  return gold;
            default:    return 0f;
        }
    }

    public void addResource(ResourceType type, float amount) {
        switch (type) {
            case WOOD:  wood  = Math.max(0, Math.min(storageCapacity, wood  + amount)); break;
            case STONE: stone = Math.max(0, Math.min(storageCapacity, stone + amount)); break;
            case FOOD:  food  = Math.max(0, Math.min(storageCapacity, food  + amount)); break;
            case GOODS: goods = Math.max(0, Math.min(storageCapacity, goods + amount)); break;
            case GOLD:  gold  = Math.max(0, gold + amount); break;
        }
    }

    public float getPriceMult(ResourceType type) {
        switch (type) {
            case WOOD:  return woodPriceMult;
            case STONE: return stonePriceMult;
            case FOOD:  return foodPriceMult;
            case GOODS: return goodsPriceMult;
            default:    return 1f;
        }
    }

    public void setPriceMult(ResourceType type, float value) {
        switch (type) {
            case WOOD:  woodPriceMult  = value; break;
            case STONE: stonePriceMult = value; break;
            case FOOD:  foodPriceMult  = value; break;
            case GOODS: goodsPriceMult = value; break;
        }
    }

    public float getSmoothedProd(ResourceType type) {
        switch (type) {
            case WOOD:  return smoothedWoodProd;
            case STONE: return smoothedStoneProd;
            case FOOD:  return smoothedFoodProd;
            case GOODS: return smoothedGoodsProd;
            default:    return 0f;
        }
    }

    public void setSmoothedProd(ResourceType type, float value) {
        switch (type) {
            case WOOD:  smoothedWoodProd  = value; break;
            case STONE: smoothedStoneProd = value; break;
            case FOOD:  smoothedFoodProd  = value; break;
            case GOODS: smoothedGoodsProd = value; break;
        }
    }

    public float getCurrentPrice(ResourceType type) {
        return config.getBasePrice(type) * getPriceMult(type);
    }
}
