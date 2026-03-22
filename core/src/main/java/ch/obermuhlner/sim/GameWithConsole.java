package ch.obermuhlner.sim;

import ch.obermuhlner.sim.game.BuildingType;
import ch.obermuhlner.sim.game.GameController;
import ch.obermuhlner.sim.game.Tile;
import ch.obermuhlner.sim.game.TileObjectRegistry;
import ch.obermuhlner.sim.game.World;
import ch.obermuhlner.sim.game.debug.GameDebugger;
import ch.obermuhlner.sim.game.mode.BuildMode;
import ch.obermuhlner.sim.game.mode.ExploreMode;
import ch.obermuhlner.sim.game.mode.GameMode;
import ch.obermuhlner.sim.game.render.*;
import ch.obermuhlner.sim.game.ui.BuildToolbar;
import ch.obermuhlner.sim.game.ui.SettlementInfoPanel;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Scanner;
import java.util.concurrent.Executors;

public class GameWithConsole extends ApplicationAdapter implements GameController {
    private static final int TILE_SIZE = 64;
    private static final int CHUNK_SIZE = 16;
    private static final long WORLD_SEED = 42L;

    private SpriteBatch batch;
    private OrthographicCamera camera;
    private World world;
    private Renderer renderer;
    private InputMultiplexer inputMultiplexer;
    private GameMode currentMode;
    
    private BuildMode buildMode;
    private SettlementInfoPanel settlementPanel;
    private BuildToolbar buildToolbar;
    
    private Scanner consoleInput;
    private Thread consoleThread;
    private GameDebugger debugger;
    private HttpServer httpServer;

    @Override
    public void create() {
        TileObjectRegistry.init();
        
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(TILE_SIZE / 2f, TILE_SIZE / 2f, 0);

        world = new World(CHUNK_SIZE, WORLD_SEED);
        world.createStarterSettlement();
        debugger = new GameDebugger(world);
        
        renderer = new Renderer(world, batch, camera);
        renderer.addLayer(new TerrainRenderLayer(world, true));
        renderer.addLayer(new ObjectRenderLayer(world, true));
        renderer.addLayer(new BuildingRenderLayer(world, true));
        renderer.addLayer(new SettlementRenderLayer(world, true));
        renderer.addLayer(new FogOfWarRenderLayer(world));

        settlementPanel = new SettlementInfoPanel();
        buildToolbar = new BuildToolbar();

        buildMode = new BuildMode(this);
        
        ExploreMode exploreMode = new ExploreMode();
        exploreMode.init(world, camera);
        exploreMode.setMain(this);
        setGameMode(exploreMode);

        Gdx.input.setInputProcessor(inputMultiplexer);
        
        consoleInput = new Scanner(System.in);
        consoleThread = new Thread(this::runConsole);
        consoleThread.setDaemon(true);
        consoleThread.start();
        
        try {
            httpServer = HttpServer.create(new InetSocketAddress(8088), 0);
            httpServer.createContext("/cmd", exchange -> {
                if ("POST".equals(exchange.getRequestMethod())) {
                    try (Scanner scanner = new Scanner(new InputStreamReader(exchange.getRequestBody())).useDelimiter("\\A")) {
                        String command = scanner.hasNext() ? scanner.next().trim() : "";
                        String result = processCommand(command);
                        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
                        byte[] bytes = result.getBytes("UTF-8");
                        exchange.sendResponseHeaders(200, bytes.length);
                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(bytes);
                        }
                    }
                } else {
                    exchange.sendResponseHeaders(405, -1);
                }
            });
            httpServer.setExecutor(Executors.newSingleThreadExecutor());
            httpServer.start();
            System.out.println("=== REST API ready on http://localhost:8088/cmd ===");
            System.out.println("Usage: POST /cmd with body 'command' to execute\n");
        } catch (IOException e) {
            System.out.println("Warning: Could not start REST server: " + e.getMessage());
        }
    }
    
    private void runConsole() {
        System.out.println("=== Debug Console Ready ===");
        System.out.println("Type 'help' for available commands.\n");
        
        while (!Thread.currentThread().isInterrupted()) {
            try {
                if (!consoleInput.hasNextLine()) {
                    Thread.sleep(100);
                    continue;
                }
                String line = consoleInput.nextLine();
                if (line == null || line.isEmpty()) continue;
                
                String result = processCommand(line.trim());
                if (result != null && !result.isEmpty()) {
                    System.out.println(result);
                }
            } catch (InterruptedException e) {
                break;
            }
        }
    }
    
    private String processCommand(String command) {
        if (command.equalsIgnoreCase("quit") || command.equalsIgnoreCase("exit")) {
            consoleThread.interrupt();
            return "Console disabled.";
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
            case "st":
                return debugger.getSettlementsInfo();
            case "resources":
            case "r":
                return debugger.getResourcesInfo();
            case "tile":
            case "t":
                if (parts.length >= 3) {
                    try {
                        return debugger.getTileInfo(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                    } catch (NumberFormatException e) {}
                }
                return "Usage: tile <x> <y>";
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
                    } catch (NumberFormatException e) {}
                }
                return "Usage: reveal <x> <y>";
            case "spawn":
                if (parts.length >= 3) {
                    try {
                        int tx = Integer.parseInt(parts[1]);
                        int ty = Integer.parseInt(parts[2]);
                        String name = parts.length >= 4 ? parts[3] : "Settlement " + (world.getSettlements().size() + 1);
                        ch.obermuhlner.sim.game.Settlement s = world.createSettlement(name, tx, ty);
                        return s != null ? "Created settlement '" + s.name + "' at (" + tx + ", " + ty + ")" : "Failed: Cannot place settlement";
                    } catch (NumberFormatException e) {}
                }
                return "Usage: spawn <x> <y] [name]";
            case "build":
                if (parts.length >= 4) {
                    try {
                        int tx = Integer.parseInt(parts[1]);
                        int ty = Integer.parseInt(parts[2]);
                        int buildingId = Integer.parseInt(parts[3]);
                        Tile tile = world.getTile(tx, ty);
                        if (!tile.terrain.isBuildable()) {
                            return "Failed: Tile (" + tx + ", " + ty + ") is not buildable (" + tile.terrain + ")";
                        }
                        ch.obermuhlner.sim.game.Settlement nearest = null;
                        double nearestDist = Double.MAX_VALUE;
                        for (ch.obermuhlner.sim.game.Settlement s : world.getSettlements()) {
                            double dist = Math.hypot(tx - s.centerX, ty - s.centerY);
                            if (dist < nearestDist && dist <= 5) {
                                nearestDist = dist;
                                nearest = s;
                            }
                        }
                        if (nearest == null) {
                            return "Failed: No settlement within 5 tiles of (" + tx + ", " + ty + ")";
                        }
                        if (tile.hasBuilding()) {
                            return "Failed: Tile (" + tx + ", " + ty + ") already has a building";
                        }
                        tile.buildingId = buildingId;
                        nearest.addBuilding(buildingId);
                        BuildingType type = BuildingType.fromId(buildingId);
                        if (type != null) {
                            nearest.addPopulation(type.getPopulationCapacity());
                        }
                        return "Built " + (type != null ? type.getDisplayName() : "#" + buildingId) + " at (" + tx + ", " + ty + ") (assigned to " + nearest.name + ")";
                    } catch (NumberFormatException e) {
                        return "Invalid coordinates or building ID";
                    }
                }
                return "Usage: build <x> <y> <buildingId>";
            case "buildmode":
            case "b":
                Gdx.app.postRunnable(() -> {
                    ExploreMode exploreMode = new ExploreMode();
                    exploreMode.init(world, camera);
                    exploreMode.setMain(this);
                    setGameMode(exploreMode);
                    BuildMode newBuildMode = new BuildMode(this);
                    newBuildMode.setToolbar(buildToolbar);
                    newBuildMode.init(world, camera);
                    setGameMode(newBuildMode);
                });
                return "Switched to BuildMode (async)";
            case "explore":
            case "e":
                Gdx.app.postRunnable(() -> {
                    ExploreMode em = new ExploreMode();
                    em.init(world, camera);
                    em.setMain(this);
                    setGameMode(em);
                });
                return "Switched to ExploreMode (async)";
            case "click":
                return handleClick(parts);
            case "key":
                return handleKey(parts);
            case "select":
                return handleSelect(parts);
            case "status":
                return getBuildModeStatus();
            case "toolbar":
                return getToolbarStatus();
            default:
                return "Unknown: " + cmd + " (type 'help')";
        }
    }
    
    private String handleClick(String[] parts) {
        if (!(currentMode instanceof BuildMode)) {
            return "Not in BuildMode. Use 'buildmode' or 'b' first.";
        }
        
        if (parts.length < 3) {
            return "Usage: click <screenX> <screenY> [button]\n  button: 0=left, 1=right, 2=middle (default: 0)";
        }
        
        try {
            int screenX = Integer.parseInt(parts[1]);
            int screenY = Integer.parseInt(parts[2]);
            int button = parts.length >= 4 ? Integer.parseInt(parts[3]) : 0;
            
            final int sx = screenX;
            final int sy = screenY;
            final int btn = button;
            
            Gdx.app.postRunnable(() -> {
                BuildMode bm = (BuildMode) currentMode;
                if (btn == 0) {
                    bm.touchDown(sx, sy, 0, Input.Buttons.LEFT);
                    bm.touchUp(sx, sy, 0, Input.Buttons.LEFT);
                } else if (btn == 1) {
                    bm.touchDown(sx, sy, 0, Input.Buttons.RIGHT);
                    bm.touchUp(sx, sy, 0, Input.Buttons.RIGHT);
                }
            });
            
            return "Simulated click at (" + screenX + ", " + screenY + ") button=" + button;
        } catch (NumberFormatException e) {
            return "Invalid coordinates";
        }
    }
    
    private String handleKey(String[] parts) {
        if (!(currentMode instanceof BuildMode)) {
            return "Not in BuildMode";
        }
        
        if (parts.length < 2) {
            return "Usage: key <keycode>\n  Keys: b=buildmode, e=explore, esc=27, 1-9=tools, f=fog, home=36";
        }
        
        String keyName = parts[1].toLowerCase();
        int keycode;
        
        switch (keyName) {
            case "b": keycode = Input.Keys.B; break;
            case "e": keycode = Input.Keys.E; break;
            case "esc": case "escape": keycode = Input.Keys.ESCAPE; break;
            case "f": keycode = Input.Keys.F; break;
            case "home": keycode = Input.Keys.HOME; break;
            case "1": keycode = Input.Keys.NUM_1; break;
            case "2": keycode = Input.Keys.NUM_2; break;
            case "3": keycode = Input.Keys.NUM_3; break;
            case "4": keycode = Input.Keys.NUM_4; break;
            case "5": keycode = Input.Keys.NUM_5; break;
            case "6": keycode = Input.Keys.NUM_6; break;
            case "7": keycode = Input.Keys.NUM_7; break;
            case "8": keycode = Input.Keys.NUM_8; break;
            case "9": keycode = Input.Keys.NUM_9; break;
            default:
                return "Unknown key: " + keyName;
        }
        
        final int kc = keycode;
        Gdx.app.postRunnable(() -> {
            currentMode.keyDown(kc);
        });
        
        return "Pressed key: " + keyName + " (code=" + keycode + ")";
    }
    
    private String handleSelect(String[] parts) {
        if (parts.length < 3) {
            return "Usage: select <tileX> <tileY>";
        }
        
        try {
            int tileX = Integer.parseInt(parts[1]);
            int tileY = Integer.parseInt(parts[2]);
            
            int screenX = (int)(tileX * 64 + 32);
            int screenY = (int)(tileY * 64 + 32);
            
            return handleClick(new String[]{"click", String.valueOf(screenX), String.valueOf(screenY)});
        } catch (NumberFormatException e) {
            return "Invalid tile coordinates";
        }
    }
    
    private String getBuildModeStatus() {
        if (!(currentMode instanceof BuildMode)) {
            return "Not in BuildMode. Mode: " + currentMode.getName();
        }
        
        BuildMode bm = (BuildMode) currentMode;
        StringBuilder sb = new StringBuilder();
        sb.append("=== BuildMode Status ===\n");
        sb.append("Mode: Build\n");
        sb.append("Selected Tile: (").append(bm.getSelectedTileX()).append(", ").append(bm.getSelectedTileY()).append(")\n");
        sb.append("Selected Tool: ").append(bm.getSelectedToolId()).append("\n");
        sb.append("Available Tools: ").append(bm.getAvailableToolCount()).append("\n");
        sb.append("Screen: ").append(Gdx.graphics.getWidth()).append("x").append(Gdx.graphics.getHeight()).append("\n");
        return sb.toString();
    }
    
    private String getToolbarStatus() {
        if (!(currentMode instanceof BuildMode)) {
            return "Not in BuildMode";
        }
        
        BuildMode bm = (BuildMode) currentMode;
        StringBuilder sb = new StringBuilder();
        sb.append("=== Toolbar Status ===\n");
        sb.append("Selected: ").append(bm.getSelectedToolId()).append("\n");
        sb.append("Tools: ");
        
        int count = bm.getAvailableToolCount();
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(", ");
            sb.append(i + 1).append("=").append(bm.getToolName(i));
        }
        return sb.toString();
    }
    
    private String getHelp() {
        return "=== Debug Commands ===\n" +
               "INFO:  state(s), settlements(st), resources(r), tile x y, map [r] [cx] [cy]\n" +
               "MODE:  buildmode(b), explore(e)\n" +
               "UI:    click <sx> <sy> [btn], key <key>, select <tx> <ty>, status, toolbar\n" +
               "      Keys: b=buildmode, e=explore, esc, 1-9=tools, f, home\n" +
               "      Tool buttons: 1=New Settlement, 2=House, 3=Farm, 4=Market, 5=Warehouse, 6=Well";
    }

    public void setGameMode(GameMode mode) {
        if (currentMode != null) {
            inputMultiplexer.removeProcessor(currentMode);
            currentMode.dispose();
        }
        currentMode = mode;
        if (inputMultiplexer == null) {
            inputMultiplexer = new InputMultiplexer();
        }
        inputMultiplexer.addProcessor(currentMode);
        if (Gdx.input.getInputProcessor() == null || Gdx.input.getInputProcessor() == inputMultiplexer) {
            Gdx.input.setInputProcessor(inputMultiplexer);
        }
        
        if (mode instanceof BuildMode) {
            ((BuildMode) mode).setToolbar(buildToolbar);
            ((BuildMode) mode).init(world, camera);
        }
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);

        renderer.render();
        
        if (currentMode instanceof BuildMode) {
            ((BuildMode) currentMode).renderUI();
            ((BuildMode) currentMode).renderToolbar();
            ((BuildMode) currentMode).renderPanel(settlementPanel);
        }
    }

    @Override
    public void dispose() {
        world.saveDirtyChunks();
        renderer.dispose();
        settlementPanel.dispose();
        buildToolbar.dispose();
        batch.dispose();
        consoleInput.close();
        if (httpServer != null) {
            httpServer.stop(1);
        }
    }
    
    @Override
    public World getWorld() {
        return world;
    }
    
    @Override
    public void handleClick(int screenX, int screenY) {
        if (currentMode instanceof BuildMode) {
            BuildMode bm = (BuildMode) currentMode;
            bm.touchDown(screenX, screenY, 0, com.badlogic.gdx.Input.Buttons.LEFT);
            bm.touchUp(screenX, screenY, 0, com.badlogic.gdx.Input.Buttons.LEFT);
        }
    }
}
