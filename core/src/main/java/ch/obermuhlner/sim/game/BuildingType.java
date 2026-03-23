package ch.obermuhlner.sim.game;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

public enum BuildingType {
    HOUSE_SIMPLE(1, "Simple House", "64x64/objects/house-simple.png", 4),
    HOUSE_LARGE(2, "Large House", "64x64/objects/house-large.png", 8),
    HOUSE_STONE(3, "Stone House", "64x64/objects/house-stone.png", 6),
    HOUSE_THATCHED(4, "Thatched House", "64x64/objects/house-thatched.png", 5),
    FARM_SMALL(5, "Small Farm", "64x64/objects/farm-small.png", 3),
    FARM_LARGE(6, "Large Farm", "64x64/objects/farm-large.png", 6),
    WELL_EMPTY(7, "Empty Well", "64x64/objects/well-empty.png", 1),
    WELL_WATER(8, "Water Well", "64x64/objects/well-water.png", 2),
    WELL_DECORATED(9, "Decorated Well", "64x64/objects/well-decorated.png", 2),
    MARKET_SMALL(10, "Small Market", "64x64/objects/market-small.png", 5),
    MARKET_LARGE(11, "Large Market", "64x64/objects/market-large.png", 10),
    BAKERY(12, "Bakery", "64x64/objects/bakery.png", 4),
    BLACKSMITH(13, "Blacksmith", "64x64/objects/blacksmith.png", 4),
    INN_SMALL(14, "Small Inn", "64x64/objects/inn-small.png", 5),
    INN_LARGE(15, "Large Inn", "64x64/objects/inn-large.png", 8),
    WAREHOUSE(16, "Warehouse", "64x64/objects/warehouse-small.png", 3),
    GRANARY(17, "Granary", "64x64/objects/granary.png", 3),
    CHURCH_SMALL(18, "Small Church", "64x64/objects/church-small.png", 6),
    CHURCH_LARGE(19, "Large Church", "64x64/objects/church-large.png", 12),
    TEMPLE_SMALL(20, "Small Temple", "64x64/objects/temple-small.png", 8),
    TEMPLE_LARGE(21, "Large Temple", "64x64/objects/temple-large.png", 15),
    CASTLE_SMALL(22, "Small Castle", "64x64/objects/castle-small.png", 20),
    CASTLE_LARGE(23, "Large Castle", "64x64/objects/castle-large.png", 40),
    PALACE(24, "Palace", "64x64/objects/palace.png", 50),
    TOWER_SMALL(25, "Small Tower", "64x64/objects/tower-small.png", 4),
    TOWER_LARGE(26, "Large Tower", "64x64/objects/tower-large.png", 8),
    WALL_STONE(27, "Stone Wall", "64x64/objects/wall-stone.png", 2),
    WALL_WOOD(28, "Wood Wall", "64x64/objects/wall-wood.png", 1),
    GATE_STONE(29, "Stone Gate", "64x64/objects/gate-stone.png", 3),
    GATE_WOOD(30, "Wood Gate", "64x64/objects/gate-wood.png", 2),
    MILL_WIND(31, "Windmill", "64x64/objects/mill-wind.png", 5),
    MILL_WATER(32, "Watermill", "64x64/objects/mill-water.png", 6),
    TAVERN(33, "Tavern", "64x64/objects/tavern.png", 6),
    SQUARE(34, "Town Square", "64x64/objects/plaza.png", 5),
    MONUMENT(35, "Monument", "64x64/objects/monument.png", 10),
    SHRINE(36, "Shrine", "64x64/objects/shrine.png", 4),
    BARRACKS(37, "Barracks", "64x64/objects/barracks.png", 6),
    TRAINING_YARD(38, "Training Yard", "64x64/objects/training-yard.png", 5),
    ARCHERY_RANGE(39, "Archery Range", "64x64/objects/archery-range.png", 5),
    FORGE_SMALL(40, "Small Forge", "64x64/objects/forge-small.png", 5),
    FORGE_LARGE(41, "Large Forge", "64x64/objects/forge-large.png", 10),
    BUTCHER(42, "Butcher", "64x64/objects/butcher.png", 3),
    TANNER(43, "Tanner", "64x64/objects/tanner.png", 3),
    WEAVER(44, "Weaver", "64x64/objects/weaver.png", 3),
    COOPER(45, "Cooper", "64x64/objects/cooper.png", 3),
    FOUNTAIN(46, "Fountain", "64x64/objects/fountain-small.png", 3),
    GARDEN(47, "Formal Garden", "64x64/objects/garden-formal.png", 5),
    ORCHARD(48, "Orchard", "64x64/objects/orchard.png", 4),
    VINEYARD(49, "Vineyard", "64x64/objects/vineyard.png", 4),
    FIELD_WHEAT(50, "Wheat Field", "64x64/objects/field-wheat.png", 2),
    FIELD_VEGETABLE(51, "Vegetable Field", "64x64/objects/field-vegetable.png", 2),
    PEN_SHEEP(52, "Sheep Pen", "64x64/objects/pen-sheep.png", 3),
    PEN_CATTLE(53, "Cattle Pen", "64x64/objects/pen-cattle.png", 4),
    PIG_STY(54, "Pig Sty", "64x64/objects/pig-sty.png", 3),
    CHICKEN_COOP(55, "Chicken Coop", "64x64/objects/chicken-coop.png", 2),
    HARBOR(56, "Harbor", "64x64/objects/mill-water.png", 5);

    private final int id;
    private final String displayName;
    private final String texturePath;
    private final int populationCapacity;

    BuildingType(int id, String displayName, String texturePath, int populationCapacity) {
        this.id = id;
        this.displayName = displayName;
        this.texturePath = texturePath;
        this.populationCapacity = populationCapacity;
    }

    public int getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTexturePath() {
        return texturePath;
    }

    public int getPopulationCapacity() {
        return populationCapacity;
    }

    public static BuildingType fromId(int id) {
        for (BuildingType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        return null;
    }
}
