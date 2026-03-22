package ch.obermuhlner.sim;

import ch.obermuhlner.sim.game.BuildingType;
import ch.obermuhlner.sim.game.GameController;
import ch.obermuhlner.sim.game.Settlement;
import ch.obermuhlner.sim.game.Tile;
import ch.obermuhlner.sim.game.TileObjectRegistry;
import ch.obermuhlner.sim.game.World;
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
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main extends ApplicationAdapter implements GameController {
    private static final int TILE_SIZE = 64;
    private static final int CHUNK_SIZE = 16;
    private static final long WORLD_SEED = 42L;

    private SpriteBatch batch;
    private OrthographicCamera camera;
    private World world;
    private Renderer renderer;
    private InputMultiplexer inputMultiplexer;
    private GameMode currentMode;
    
    private SettlementInfoPanel settlementPanel;
    private BuildToolbar buildToolbar;
    
    private int selectedTileX = -1;
    private int selectedTileY = -1;
    private int selectedToolId = -1;
    private List<BuildToolbar.ToolButton> availableTools = new ArrayList<>();
    private Map<Integer, Texture> buildingTextures = new HashMap<>();
    private SpriteBatch uiBatch;
    private Texture selectionTexture;
    private Texture settlementTexture;
    private boolean tileSelected = false;

    @Override
    public void create() {
        TileObjectRegistry.init();
        
        batch = new SpriteBatch();
        uiBatch = new SpriteBatch();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(TILE_SIZE / 2f, TILE_SIZE / 2f, 0);

        world = new World(CHUNK_SIZE, WORLD_SEED);
        
        world.createStarterSettlement();
        
        renderer = new Renderer(world, batch, camera);
        renderer.addLayer(new TerrainRenderLayer(world, true));
        renderer.addLayer(new ObjectRenderLayer(world, true));
        renderer.addLayer(new BuildingRenderLayer(world, true));
        renderer.addLayer(new SettlementRenderLayer(world, true));
        renderer.addLayer(new FogOfWarRenderLayer(world));

        settlementPanel = new SettlementInfoPanel();
        buildToolbar = new BuildToolbar();

        createSelectionTextures();
        initToolbar();

        ExploreMode exploreMode = new ExploreMode();
        exploreMode.init(world, camera);
        exploreMode.setMain(this);
        setGameMode(exploreMode);

        Gdx.input.setInputProcessor(inputMultiplexer);
    }
    
    private void createSelectionTextures() {
        // Yellow border, transparent fill for selected tile
        Pixmap selectionMap = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        selectionMap.setColor(0, 0, 0, 0);
        selectionMap.fill();
        selectionMap.setColor(1f, 1f, 0f, 1f);
        selectionMap.drawRectangle(0, 0, 64, 64);
        selectionMap.drawRectangle(1, 1, 62, 62);
        selectionMap.drawRectangle(2, 2, 60, 60);
        selectionTexture = new Texture(selectionMap);
        selectionMap.dispose();

        // Green border for settlement tile
        Pixmap settlementMap = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        settlementMap.setColor(0, 0, 0, 0);
        settlementMap.fill();
        settlementMap.setColor(0.3f, 0.9f, 0.3f, 1f);
        settlementMap.drawRectangle(0, 0, 64, 64);
        settlementMap.drawRectangle(1, 1, 62, 62);
        settlementMap.drawRectangle(2, 2, 60, 60);
        settlementTexture = new Texture(settlementMap);
        settlementMap.dispose();
    }
    
    public void selectTile(int tileX, int tileY) {
        selectedTileX = tileX;
        selectedTileY = tileY;
        tileSelected = true;
        
        if (!world.isRevealed(tileX, tileY)) {
            if (world.hasRevealedNeighbor(tileX, tileY)) {
                world.reveal(tileX, tileY);
            }
        }
        
        updateAvailableTools();
    }
    
    public void initToolbar() {
        availableTools.clear();
        availableTools.add(new BuildToolbar.ToolButton(0, "New Settlement", null));
        buildToolbar.setTools(availableTools);
    }
    
    private void updateAvailableTools() {
        availableTools.clear();
        
        Tile tile = world.getTile(selectedTileX, selectedTileY);
        boolean isBuildable = tile.terrain.isBuildable();
        Settlement nearbySettlement = getNearbySettlement(selectedTileX, selectedTileY);

        availableTools.add(new BuildToolbar.ToolButton(
            0, "New Settlement", null
        ));

        if (isBuildable && nearbySettlement != null) {
            BuildingType house = BuildingType.HOUSE_SIMPLE;
            availableTools.add(new BuildToolbar.ToolButton(1, house.getDisplayName(), getBuildingTexture(house)));

            BuildingType farm = BuildingType.FARM_SMALL;
            availableTools.add(new BuildToolbar.ToolButton(2, farm.getDisplayName(), getBuildingTexture(farm)));

            BuildingType market = BuildingType.MARKET_SMALL;
            availableTools.add(new BuildToolbar.ToolButton(3, market.getDisplayName(), getBuildingTexture(market)));

            BuildingType warehouse = BuildingType.WAREHOUSE;
            availableTools.add(new BuildToolbar.ToolButton(4, warehouse.getDisplayName(), getBuildingTexture(warehouse)));

            BuildingType well = BuildingType.WELL_WATER;
            availableTools.add(new BuildToolbar.ToolButton(5, well.getDisplayName(), getBuildingTexture(well)));
        }

        buildToolbar.setTools(availableTools);
        if (selectedToolId >= 0) {
            buildToolbar.selectTool(selectedToolId);
        } else {
            buildToolbar.deselectTool();
        }
    }
    
    private Texture getBuildingTexture(BuildingType type) {
        return buildingTextures.computeIfAbsent(type.getId(),
            id -> new Texture(Gdx.files.internal(type.getTexturePath())));
    }

    private Settlement getNearbySettlement(int tx, int ty) {
        for (Settlement s : world.getSettlements()) {
            double dist = Math.hypot(tx - s.centerX, ty - s.centerY);
            if (dist <= 5) {
                return s;
            }
        }
        return null;
    }
    
    private void executeTool(int toolId) {
        Tile tile = world.getTile(selectedTileX, selectedTileY);

        switch (toolId) {
            case 0:
                placeSettlement(selectedTileX, selectedTileY);
                break;
            case 1:
                placeBuilding(selectedTileX, selectedTileY, BuildingType.HOUSE_SIMPLE.getId());
                break;
            case 2:
                placeBuilding(selectedTileX, selectedTileY, BuildingType.FARM_SMALL.getId());
                break;
            case 3:
                placeBuilding(selectedTileX, selectedTileY, BuildingType.MARKET_SMALL.getId());
                break;
            case 4:
                placeBuilding(selectedTileX, selectedTileY, BuildingType.WAREHOUSE.getId());
                break;
            case 5:
                placeBuilding(selectedTileX, selectedTileY, BuildingType.WELL_WATER.getId());
                break;
        }
        
        updateAvailableTools();
    }
    
    private void placeSettlement(int tx, int ty) {
        Tile tile = world.getTile(tx, ty);
        if (!tile.terrain.isBuildable()) return;
        if (world.getSettlementAt(tx, ty) != null) return;
        if (!world.hasRevealedNeighbor(tx, ty)) return;

        String name = "Settlement " + (world.getSettlements().size() + 1);
        world.createSettlement(name, tx, ty);
    }
    
    private void placeBuilding(int tx, int ty, int buildingId) {
        Settlement settlement = getNearbySettlement(tx, ty);
        if (settlement == null) return;

        Tile tile = world.getTile(tx, ty);
        if (!tile.terrain.isBuildable()) return;
        if (tile.hasBuilding()) return;

        tile.buildingId = buildingId;
        settlement.addBuilding(buildingId);

        BuildingType type = BuildingType.fromId(buildingId);
        if (type != null) {
            settlement.addPopulation(type.getPopulationCapacity());
        }
    }
    
    public void handleClick(int screenX, int screenY) {
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        // Input Y is 0=top; toolbar uses GL Y (0=bottom) — invert before checking
        int glY = screenHeight - screenY;

        int toolId = buildToolbar.getToolIdAt(screenX, glY, screenWidth, screenHeight);
        if (toolId >= 0 && tileSelected) {
            selectedToolId = toolId;
            buildToolbar.selectTool(toolId);
            executeTool(toolId);
            return;
        }

        // If click is in toolbar area (top of screen), don't process as tile click
        float toolbarTop = screenHeight - 20;
        float toolbarBottom = screenHeight - 104 - 20;
        if (glY >= toolbarBottom && glY <= toolbarTop) {
            return;
        }

        com.badlogic.gdx.math.Vector3 worldPos = camera.unproject(new com.badlogic.gdx.math.Vector3(screenX, screenY, 0));
        int tileX = (int) Math.floor(worldPos.x / 64);
        int tileY = (int) Math.floor(worldPos.y / 64);
        selectTile(tileX, tileY);
    }
    
    public int getSelectedTileX() { return selectedTileX; }
    public int getSelectedTileY() { return selectedTileY; }
    public boolean hasTileSelected() { return tileSelected; }

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
            ((BuildMode) mode).init(world, camera);
            ((BuildMode) mode).setToolbar(buildToolbar);
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
        
        renderUI();
    }
    
    private void renderUI() {
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        // Draw tile selection border in world space (camera-projected)
        if (tileSelected && world.isRevealed(selectedTileX, selectedTileY)) {
            Settlement settlement = world.getSettlementAt(selectedTileX, selectedTileY);
            Texture tex = (settlement != null) ? settlementTexture : selectionTexture;
            batch.setProjectionMatrix(camera.combined);
            batch.begin();
            batch.draw(tex, selectedTileX * 64, selectedTileY * 64, 64, 64);
            batch.end();
        }

        // Draw toolbar and settlement panel in screen space
        uiBatch.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4().setToOrtho2D(0, 0, screenWidth, screenHeight));
        uiBatch.begin();
        if (buildToolbar != null) {
            buildToolbar.render(uiBatch, screenWidth, screenHeight);
        }
        if (tileSelected && world.isRevealed(selectedTileX, selectedTileY)) {
            Settlement settlement = world.getSettlementAt(selectedTileX, selectedTileY);
            if (settlement != null) {
                settlementPanel.render(settlement, uiBatch, screenWidth, screenHeight);
            }
        }
        uiBatch.end();
    }

    @Override
    public void dispose() {
        world.saveDirtyChunks();
        renderer.dispose();
        settlementPanel.dispose();
        buildToolbar.dispose();
        batch.dispose();
        if (uiBatch != null) uiBatch.dispose();
        if (selectionTexture != null) selectionTexture.dispose();
        if (settlementTexture != null) settlementTexture.dispose();
        for (Texture tex : buildingTextures.values()) {
            tex.dispose();
        }
    }
    
    @Override
    public World getWorld() {
        return world;
    }
}
