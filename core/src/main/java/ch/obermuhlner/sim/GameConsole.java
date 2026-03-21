package ch.obermuhlner.sim;

import ch.obermuhlner.sim.game.*;
import ch.obermuhlner.sim.game.debug.GameDebugger;

import java.util.Scanner;

public class GameConsole {
    private final World world;
    private final GameDebugger debugger;
    private final Scanner scanner;
    
    public GameConsole(World world) {
        this.world = world;
        this.debugger = new GameDebugger(world);
        this.scanner = new Scanner(System.in);
    }
    
    public void run() {
        System.out.println("=== Simple Sim Game Console ===");
        System.out.println("Type 'help' for commands, 'quit' to exit\n");
        
        world.reveal(0, 0);
        
        while (true) {
            System.out.print("> ");
            String line = scanner.nextLine();
            
            if (line == null || line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit")) {
                System.out.println("Goodbye!");
                break;
            }
            
            String result = processCommand(line.trim());
            System.out.println(result);
        }
    }
    
    private String processCommand(String command) {
        if (command.isEmpty()) {
            return debugger.getState();
        }
        
        String[] parts = command.toLowerCase().split("\\s+");
        String cmd = parts[0];
        
        switch (cmd) {
            case "help":
                return getHelp();
            
            case "state":
            case "s":
                return debugger.getState();
            
            case "settlements":
            case "settle":
            case "st":
                return debugger.getSettlementsInfo();
            
            case "resources":
            case "res":
            case "r":
                return debugger.getResourcesInfo();
            
            case "controls":
            case "c":
                return debugger.getControlsInfo();
            
            case "tile":
            case "t":
                if (parts.length >= 3) {
                    try {
                        int tx = Integer.parseInt(parts[1]);
                        int ty = Integer.parseInt(parts[2]);
                        return debugger.getTileInfo(tx, ty);
                    } catch (NumberFormatException e) {
                        return "Invalid coordinates: " + parts[1] + ", " + parts[2];
                    }
                }
                return "Usage: tile <x> <y>\nExample: tile 0 0";
            
            case "map":
            case "m":
                int radius = parts.length >= 2 ? Math.min(Integer.parseInt(parts[1]), 20) : 10;
                int cx = parts.length >= 3 ? Integer.parseInt(parts[2]) : 0;
                int cy = parts.length >= 4 ? Integer.parseInt(parts[3]) : 0;
                return debugger.getMapAround(cx, cy, radius);
            
            case "reveal":
                if (parts.length >= 3) {
                    try {
                        int tx = Integer.parseInt(parts[1]);
                        int ty = Integer.parseInt(parts[2]);
                        world.revealArea(tx, ty, 3);
                        return "Revealed area around (" + tx + ", " + ty + ")";
                    } catch (NumberFormatException e) {
                        return "Invalid coordinates";
                    }
                }
                return "Usage: reveal <x> <y>";
            
            case "spawn":
            case "create":
                if (parts.length >= 3) {
                    try {
                        int tx = Integer.parseInt(parts[1]);
                        int ty = Integer.parseInt(parts[2]);
                        String name = parts.length >= 4 ? parts[3] : "Settlement " + (world.getSettlements().size() + 1);
                        Settlement s = world.createSettlement(name, tx, ty);
                        if (s != null) {
                            return "Created settlement '" + s.name + "' at (" + tx + ", " + ty + ")";
                        } else {
                            return "Failed: Cannot place settlement at (" + tx + ", " + ty + ")";
                        }
                    } catch (NumberFormatException e) {
                        return "Invalid coordinates";
                    }
                }
                return "Usage: spawn <x> <y> [name]";
            
            case "build":
                if (parts.length >= 3) {
                    try {
                        int tx = Integer.parseInt(parts[1]);
                        int ty = Integer.parseInt(parts[2]);
                        Tile tile = world.getTile(tx, ty);
                        if (!tile.isBuildable()) {
                            return "Tile (" + tx + ", " + ty + ") is not buildable";
                        }
                        int buildingId = parts.length >= 4 ? Integer.parseInt(parts[3]) : 1;
                        
                        // Find nearest settlement
                        Settlement nearest = null;
                        double nearestDist = Double.MAX_VALUE;
                        for (Settlement s : world.getSettlements()) {
                            double dist = Math.hypot(tx - s.centerX, ty - s.centerY);
                            if (dist < nearestDist && dist <= 5) {
                                nearestDist = dist;
                                nearest = s;
                            }
                        }
                        
                        tile.buildingId = buildingId;
                        BuildingType type = BuildingType.fromId(buildingId);
                        String result = "Built " + (type != null ? type.getDisplayName() : "#" + buildingId) + " at (" + tx + ", " + ty + ")";
                        
                        if (nearest != null) {
                            nearest.addBuilding(buildingId);
                            if (type != null) {
                                nearest.addPopulation(type.getPopulationCapacity());
                            }
                            result += " (assigned to " + nearest.name + ")";
                        } else {
                            result += " (no settlement nearby)";
                        }
                        return result;
                    } catch (NumberFormatException e) {
                        return "Invalid arguments";
                    }
                }
                return "Usage: build <x> <y> [buildingId]";
            
            default:
                return "Unknown command: " + cmd + "\nType 'help' for available commands";
        }
    }
    
    private String getHelp() {
        return "Available commands:\n" +
               "  help              - Show this help\n" +
               "  quit              - Exit\n" +
               "  state (s)         - Show full game state\n" +
               "  settlements (st)  - Show settlement info\n" +
               "  resources (r)     - Show resources\n" +
               "  controls (c)      - Show game controls\n" +
               "  tile <x> <y>      - Show tile info\n" +
               "  map [r] [cx] [cy] - Show map (default 10x10 around 0,0)\n" +
               "  reveal <x> <y>    - Reveal area around point\n" +
               "  spawn <x> <y> [n] - Create settlement\n" +
               "  build <x> <y> [id] - Place building on tile\n";
    }
    
    public static void main(String[] args) {
        World world = new World(16, 42L, true);
        GameConsole console = new GameConsole(world);
        console.run();
    }
}
