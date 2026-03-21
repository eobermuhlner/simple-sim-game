package ch.obermuhlner.sim;

import ch.obermuhlner.sim.game.GameController;
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
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;

import java.util.Scanner;

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
    private boolean showBuildUI = false;
    
    private Scanner consoleInput;
    private Thread consoleThread;
    private GameDebugger debugger;

    @Override
    public void create() {
        TileObjectRegistry.init();
        
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(TILE_SIZE / 2f, TILE_SIZE / 2f, 0);

        world = new World(CHUNK_SIZE, WORLD_SEED);
        debugger = new GameDebugger(world);
        
        renderer = new Renderer(world, batch, camera);
        renderer.addLayer(new TerrainRenderLayer(world, true));
        renderer.addLayer(new ObjectRenderLayer(world, true));
        renderer.addLayer(new BuildingRenderLayer(world, true));
        renderer.addLayer(new SettlementRenderLayer(world, true));
        renderer.addLayer(new FogOfWarRenderLayer(world));

        buildMode = new BuildMode(this);
        settlementPanel = new SettlementInfoPanel();
        buildToolbar = new BuildToolbar();

        ExploreMode exploreMode = new ExploreMode();
        exploreMode.init(world, camera);
        exploreMode.setMain(this);
        setGameMode(exploreMode);

        Gdx.input.setInputProcessor(inputMultiplexer);
        
        consoleInput = new Scanner(System.in);
        consoleThread = new Thread(this::runConsole);
        consoleThread.setDaemon(true);
        consoleThread.start();
    }
    
    private void runConsole() {
        System.out.println("=== Console ready (type commands or 'quit' to disable) ===");
        
        while (!Thread.currentThread().isInterrupted()) {
            try {
                if (!consoleInput.hasNextLine()) {
                    Thread.sleep(100);
                    continue;
                }
                String line = consoleInput.nextLine();
                if (line == null || line.isEmpty()) continue;
                
                String result = processCommand(line.trim());
                System.out.println(result);
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
                return "Usage: spawn <x> <y> [name]";
            default:
                return "Unknown: " + cmd + " (type 'help')";
        }
    }
    
    private String getHelp() {
        return "Commands: help, state(s), settlements(st), resources(r), tile x y, map [r] [cx] [cy], reveal x y, spawn x y [name], quit";
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
        
        showBuildUI = (mode instanceof BuildMode);
        
        if (mode instanceof BuildMode) {
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
        
        if (showBuildUI && currentMode instanceof BuildMode) {
            ((BuildMode) currentMode).renderUI();
            ((BuildMode) currentMode).renderToolbar(buildToolbar);
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
    }
    
    @Override
    public World getWorld() {
        return world;
    }
}
