package ch.obermuhlner.sim;

import ch.obermuhlner.sim.game.World;
import ch.obermuhlner.sim.game.Settlement;
import ch.obermuhlner.sim.game.debug.GameDebugger;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DebugServer extends ApplicationAdapter {
    private static final int DEBUG_PORT = 5555;
    
    private World world;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private GameDebugger debugger;
    private ExecutorService executor;
    private volatile boolean running = true;
    private volatile String lastCommand = "";
    private volatile String lastOutput = "";
    
    @Override
    public void create() {
        world = new World(16, 42L);
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 600);
        batch = new SpriteBatch();
        debugger = new GameDebugger(world);
        
        world.reveal(0, 0);
        
        executor = Executors.newSingleThreadExecutor();
        executor.submit(this::runDebugServer);
        
        System.out.println("Debug server started on port " + DEBUG_PORT);
        System.out.println("Connect with: telnet localhost " + DEBUG_PORT);
    }
    
    private void runDebugServer() {
        try (ServerSocket serverSocket = new ServerSocket(DEBUG_PORT)) {
            while (running) {
                try {
                    Socket client = serverSocket.accept();
                    handleClient(client);
                } catch (Exception e) {
                    if (running) e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void handleClient(Socket client) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
             PrintWriter out = new PrintWriter(client.getOutputStream(), true)) {
            
            out.println("=== Simple Sim Game Debug Console ===");
            out.println("Type 'help' for commands, 'quit' to disconnect");
            out.println();
            
            String input;
            while ((input = in.readLine()) != null) {
                String output = processCommand(input.trim());
                out.println(output);
                out.println();
                lastCommand = input.trim();
                lastOutput = output;
            }
        } catch (Exception e) {
            e.printStackTrace();
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
            
            case "quit":
            case "exit":
                return "Goodbye!";
            
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
                int radius = parts.length >= 2 ? Integer.parseInt(parts[1]) : 10;
                int cx = parts.length >= 3 ? Integer.parseInt(parts[2]) : 0;
                int cy = parts.length >= 4 ? Integer.parseInt(parts[3]) : 0;
                return debugger.getMapAround(cx, cy, Math.min(radius, 20));
            
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

            case "road":
                if (parts.length >= 3) {
                    try {
                        int tx = Integer.parseInt(parts[1]);
                        int ty = Integer.parseInt(parts[2]);
                        world.reveal(tx, ty);
                        boolean placed = world.placeRoad(tx, ty, ch.obermuhlner.sim.game.RoadType.DIRT);
                        return placed ? "Placed dirt road at (" + tx + ", " + ty + ")"
                                      : "Cannot place road at (" + tx + ", " + ty + ")";
                    } catch (NumberFormatException e) {
                        return "Invalid coordinates";
                    }
                }
                return "Usage: road <x> <y>";

            default:
                return "Unknown command: " + cmd + "\nType 'help' for available commands";
        }
    }
    
    private String getHelp() {
        return "Available commands:\n" +
               "  help              - Show this help\n" +
               "  quit              - Disconnect\n" +
               "  state (s)         - Show full game state\n" +
               "  settlements (st) - Show settlement info\n" +
               "  resources (r)    - Show resources\n" +
               "  controls (c)      - Show controls\n" +
               "  tile <x> <y>      - Show tile info\n" +
               "  map [r] [cx] [cy] - Show map around point\n" +
               "  reveal <x> <y>    - Reveal area\n" +
               "  spawn <x> <y> [n] - Create settlement\n" +
               "  road <x> <y>      - Place dirt road\n";
    }
    
    @Override
    public void render() {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.end();
    }
    
    @Override
    public void dispose() {
        running = false;
        executor.shutdownNow();
        batch.dispose();
    }
    
    public String getLastCommand() {
        return lastCommand;
    }
    
    public String getLastOutput() {
        return lastOutput;
    }
}
