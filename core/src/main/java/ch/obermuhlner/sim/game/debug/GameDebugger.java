package ch.obermuhlner.sim.game.debug;

import ch.obermuhlner.sim.game.*;
import ch.obermuhlner.sim.game.RoadType;
import ch.obermuhlner.sim.game.systems.SimulationSystem;

import java.util.StringJoiner;

public class GameDebugger {
    private final World world;
    private final SimulationSystem simulation;

    public GameDebugger(World world) {
        this.world = world;
        this.simulation = null;
    }

    public GameDebugger(World world, SimulationSystem simulation) {
        this.world = world;
        this.simulation = simulation;
    }
    
    public String getState() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("=== GAME STATE ===\n");
        sb.append(getSettlementsInfo());
        sb.append("\n");
        sb.append(getResourcesInfo());
        sb.append("\n");
        sb.append(getControlsInfo());
        
        return sb.toString();
    }
    
    public String getSettlementsInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("--- SETTLEMENTS ---\n");
        
        if (world.getSettlements().isEmpty()) {
            sb.append("No settlements.\n");
        } else {
            for (Settlement s : world.getSettlements()) {
                sb.append(String.format("[%d] %s (%s)\n",
                    s.id, s.name, s.getLevel().getDisplayName()));
                sb.append(String.format("  Population: %d\n", s.population));
                sb.append(String.format("  Specialization: %s\n", s.specialization.displayName));
                if (s.specialization != Specialization.NONE) {
                    sb.append(String.format("  Production: %s\n", s.specialization.getProductionSummary()));
                }
                sb.append(String.format("  Position: (%d, %d)\n", s.centerX, s.centerY));
                sb.append(String.format("  Buildings: %d/%d\n",
                    s.buildingIds.size(), s.getMaxBuildings()));
                sb.append(String.format("  Resources: W:%.0f S:%.0f F:%.0f G:%.0f Gold:%.0f\n",
                    s.wood, s.stone, s.food, s.goods, s.gold));
                sb.append(String.format("  Prod/tick: W:%.1f S:%.1f F:%.1f G:%.2f\n",
                    s.smoothedWoodProd, s.smoothedStoneProd, s.smoothedFoodProd, s.smoothedGoodsProd));
                if (s.needsSpecializationChoice()) {
                    sb.append("  Status: Choose a specialization!\n");
                } else if (s.needsUpgrade()) {
                    sb.append("  Status: Ready to upgrade!\n");
                }
            }
        }
        
        return sb.toString();
    }
    
    public String getResourcesInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("--- RESOURCES ---\n");
        
        int totalPop = 0;
        int totalBuildings = 0;
        int settlements = world.getSettlements().size();
        
        for (Settlement s : world.getSettlements()) {
            totalPop += s.population;
            totalBuildings += s.buildingIds.size();
        }
        
        sb.append(String.format("Settlements: %d\n", settlements));
        sb.append(String.format("Total Population: %d\n", totalPop));
        sb.append(String.format("Total Buildings: %d\n", totalBuildings));
        sb.append(String.format("Trade Routes: %d\n", world.getTradeRoutes().size()));
        sb.append(String.format("Active Caravans: %d\n", world.getCaravans().size()));
        if (simulation != null) {
            sb.append(String.format("Simulation Tick: %d\n", simulation.getTickCount()));
        }

        return sb.toString();
    }
    
    public String getControlsInfo() {
        return "---\nCONTROLS:\n" +
               "Click - Select tile / Reveal fog\n" +
               "Drag - Pan camera\n" +
               "Scroll - Zoom\n" +
               "F - Toggle fog\n" +
               "HOME - Return to start\n" +
               "1-9 - Quick select building\n";
    }
    
    public String getTileInfo(int tx, int ty) {
        Tile tile = world.getTile(tx, ty);
        Settlement settlement = world.getSettlementAt(tx, ty);
        boolean revealed = world.isRevealed(tx, ty);
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Tile (%d, %d):\n", tx, ty));
        sb.append(String.format("  Revealed: %s\n", revealed));
        
        if (!revealed) {
            sb.append("  (hidden)\n");
            return sb.toString();
        }
        
        sb.append(String.format("  Terrain: %s\n", tile.terrain.name()));
        sb.append(String.format("  Buildable: %s\n", tile.isBuildable()));
        
        if (tile.hasObject()) {
            TileObject obj = tile.getObject();
            sb.append(String.format("  Object: %s\n", obj != null ? obj.getName() : "unknown"));
        }
        
        if (tile.hasBuilding()) {
            BuildingType building = BuildingType.fromId(tile.buildingId);
            sb.append(String.format("  Building: %s\n", building != null ? building.getDisplayName() : "unknown"));
        }

        if (tile.hasRoad()) {
            RoadType roadType = RoadType.fromId(tile.roadType);
            sb.append(String.format("  Road: %s (conn=0b%s)\n",
                roadType != null ? roadType.getDisplayName() : "unknown",
                Integer.toBinaryString(tile.roadConnection)));
        }

        if (settlement != null) {
            sb.append(String.format("  Settlement: %s [%s]\n", settlement.name, settlement.getLevel().getDisplayName()));
        }

        return sb.toString();
    }
    
    public String getMapAround(int centerX, int centerY, int radius) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Map around (%d, %d), radius %d:\n", centerX, centerY, radius));
        sb.append("  (fog=#, grass=G, forest=F, stone=S, water=W, snow=X, road=R, settlement=@, building=B)\n");
        
        for (int y = centerY + radius; y >= centerY - radius; y--) {
            StringBuilder row = new StringBuilder();
            row.append(String.format("%4d: ", y));
            for (int x = centerX - radius; x <= centerX + radius; x++) {
                if (!world.isRevealed(x, y)) {
                    row.append('#');
                } else {
                    Tile tile = world.getTile(x, y);
                    Settlement settlement = world.getSettlementAt(x, y);
                    
                    if (settlement != null && settlement.centerX == x && settlement.centerY == y) {
                        row.append('@');
                    } else if (tile.hasBuilding()) {
                        row.append('B');
                    } else if (tile.hasRoad()) {
                        row.append('R');
                    } else {
                        switch (tile.terrain) {
                            case DEEP_SEA:    row.append('~'); break;
                            case SHALLOW_SEA: row.append('s'); break;
                            case GRASS:       row.append('.'); break;
                            case FOREST:      row.append('T'); break;
                            case STONE:       row.append('O'); break;
                            case SNOW:        row.append('*'); break;
                            default:          row.append('?'); break;
                        }
                    }
                }
            }
            sb.append(row.toString()).append("\n");
        }
        
        sb.append("      ");
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            sb.append(Math.abs(x % 10));
        }
        sb.append("\n");
        
        return sb.toString();
    }
}
