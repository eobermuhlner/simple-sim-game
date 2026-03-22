package ch.obermuhlner.sim;

import ch.obermuhlner.sim.game.BuildingType;
import ch.obermuhlner.sim.game.GameController;
import ch.obermuhlner.sim.game.RoadType;
import ch.obermuhlner.sim.game.Settlement;
import ch.obermuhlner.sim.game.Specialization;
import ch.obermuhlner.sim.game.Tile;
import ch.obermuhlner.sim.game.TileObjectRegistry;
import ch.obermuhlner.sim.game.TradeRoute;
import ch.obermuhlner.sim.game.World;
import ch.obermuhlner.sim.game.mode.BuildMode;
import ch.obermuhlner.sim.game.mode.ExploreMode;
import ch.obermuhlner.sim.game.mode.GameMode;
import ch.obermuhlner.sim.game.render.*;
import ch.obermuhlner.sim.game.render.CaravanRenderLayer;
import ch.obermuhlner.sim.game.render.RoadRenderLayer;
import ch.obermuhlner.sim.game.systems.SimulationSystem;
import ch.obermuhlner.sim.game.ui.BuildToolbar;
import ch.obermuhlner.sim.game.ui.SettlementInfoPanel;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.ScreenUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main extends ApplicationAdapter implements GameController {
    private static final int TILE_SIZE = 64;
    private static final int CHUNK_SIZE = 16;
    private static final long WORLD_SEED = 42L;

    // Tool ID ranges:
    //   0       – New Settlement
    //   1–5     – Buildings
    //   10–13   – Specialization choice (Village → Town)
    //   20      – Upgrade (Town → City, City → Metropolis)
    //   21      – Enter re-specialize mode
    //   30–33   – Re-specialization choice (drop one level + new spec)
    //   50      – Build Dirt Road
    private static final int TOOL_NEW_SETTLEMENT = 0;
    private static final int TOOL_HOUSE   = 1;
    private static final int TOOL_FARM    = 2;
    private static final int TOOL_MARKET  = 3;
    private static final int TOOL_WAREHOUSE = 4;
    private static final int TOOL_WELL    = 5;
    private static final int TOOL_SPEC_LOGGING = 10;
    private static final int TOOL_SPEC_MINING  = 11;
    private static final int TOOL_SPEC_FARMING = 12;
    private static final int TOOL_SPEC_TRADE   = 13;
    private static final int TOOL_UPGRADE      = 20;
    private static final int TOOL_RESPEC_MODE  = 21;
    private static final int TOOL_RESPEC_LOGGING = 30;
    private static final int TOOL_RESPEC_MINING  = 31;
    private static final int TOOL_RESPEC_FARMING = 32;
    private static final int TOOL_RESPEC_TRADE   = 33;
    private static final int TOOL_BUILD_ROAD     = 50;
    private static final int TOOL_DESTROY        = 51;

    private static final float TICK_INTERVAL = 1.0f;

    private SpriteBatch batch;
    private OrthographicCamera camera;
    private World world;
    private Renderer renderer;
    private InputMultiplexer inputMultiplexer;
    private GameMode currentMode;
    private SimulationSystem simulation;
    private float tickAccumulator = 0f;

    private SettlementInfoPanel settlementPanel;
    private BuildToolbar buildToolbar;
    private BitmapFont hudFont;

    private int selectedTileX = -1;
    private int selectedTileY = -1;
    private int selectedToolId = -1;
    private List<BuildToolbar.ToolButton> availableTools = new ArrayList<>();
    private Map<Integer, Texture> buildingTextures = new HashMap<>();
    private Map<Specialization, Texture> specializationIcons = new HashMap<>();
    private SpriteBatch uiBatch;
    private Texture selectionTexture;
    private Texture settlementTexture;
    private boolean tileSelected = false;
    private boolean respecMode = false;
    private Texture roadIcon;
    private Texture destroyIcon;

    @Override
    public void create() {
        TileObjectRegistry.init();

        batch = new SpriteBatch();
        uiBatch = new SpriteBatch();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(TILE_SIZE / 2f, TILE_SIZE / 2f, 0);

        world = new World(CHUNK_SIZE, WORLD_SEED);

        boolean settlementsLoaded = world.loadSettlements(Gdx.files.local("data/settlements.dat"));
        if (!settlementsLoaded) {
            world.createStarterSettlement();
        }

        simulation = new SimulationSystem(world);

        renderer = new Renderer(world, batch, camera);
        renderer.addLayer(new TerrainRenderLayer(world, true));
        renderer.addLayer(new ObjectRenderLayer(world, true));
        renderer.addLayer(new RoadRenderLayer(world, true));
        renderer.addLayer(new BuildingRenderLayer(world, true));
        renderer.addLayer(new SettlementRenderLayer(world, true));
        renderer.addLayer(new CaravanRenderLayer(world));
        renderer.addLayer(new FogOfWarRenderLayer(world));

        roadIcon = new Texture(Gdx.files.internal("64x64/single-tiles/road-dirt-ns.png"));
        destroyIcon = createDestroyIcon();

        settlementPanel = new SettlementInfoPanel();
        buildToolbar = new BuildToolbar();
        createHudFont();

        createSelectionTextures();
        createSpecializationIcons();
        initToolbar();

        ExploreMode exploreMode = new ExploreMode();
        exploreMode.init(world, camera);
        exploreMode.setMain(this);
        setGameMode(exploreMode);

        Gdx.input.setInputProcessor(inputMultiplexer);
    }

    private void createHudFont() {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
            Gdx.files.internal("fonts/JetBrainsMono-Regular.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter params = new FreeTypeFontGenerator.FreeTypeFontParameter();
        params.size = 13;
        params.color = Color.WHITE;
        params.borderColor = new Color(0, 0, 0, 0.7f);
        params.borderWidth = 1f;
        hudFont = generator.generateFont(params);
        generator.dispose();
    }

    private void createSelectionTextures() {
        Pixmap selectionMap = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        selectionMap.setColor(0, 0, 0, 0);
        selectionMap.fill();
        selectionMap.setColor(1f, 1f, 0f, 1f);
        selectionMap.drawRectangle(0, 0, 64, 64);
        selectionMap.drawRectangle(1, 1, 62, 62);
        selectionMap.drawRectangle(2, 2, 60, 60);
        selectionTexture = new Texture(selectionMap);
        selectionMap.dispose();

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

    private void createSpecializationIcons() {
        createSpecIcon(Specialization.LOGGING_CAMP,    new Color(0.6f, 0.4f, 0.1f, 1f));
        createSpecIcon(Specialization.MINING_TOWN,     new Color(0.6f, 0.6f, 0.6f, 1f));
        createSpecIcon(Specialization.FARMING_VILLAGE, new Color(0.2f, 0.8f, 0.2f, 1f));
        createSpecIcon(Specialization.TRADE_HUB,       new Color(1.0f, 0.85f, 0.1f, 1f));
    }

    private Texture createDestroyIcon() {
        int size = 48;
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        p.setColor(new Color(0.25f, 0f, 0f, 1f));
        p.fill();
        p.setColor(new Color(0.9f, 0.15f, 0.15f, 1f));
        // Draw thick X (3-pixel wide diagonals)
        for (int i = 4; i < size - 4; i++) {
            for (int w = -1; w <= 1; w++) {
                p.drawPixel(i + w, i);
                p.drawPixel(size - 1 - i + w, i);
            }
        }
        Texture t = new Texture(p);
        p.dispose();
        return t;
    }

    private void destroyTile(int tx, int ty) {
        if (!world.isRevealed(tx, ty)) return;
        Tile tile = world.getTile(tx, ty);

        if (tile.hasBuilding()) {
            int bid = tile.buildingId;
            Settlement s = getNearbySettlement(tx, ty);
            if (s != null) {
                s.buildingIds.remove(Integer.valueOf(bid));
                BuildingType type = BuildingType.fromId(bid);
                if (type != null) {
                    s.addPopulation(-type.getPopulationCapacity());
                }
            }
            world.setBuilding(tx, ty, 0);
        } else if (tile.hasObject()) {
            world.removeObject(tx, ty);
        } else if (tile.roadType != 0) {
            world.removeRoad(tx, ty);
        }

        updateAvailableTools();
    }

    private void createSpecIcon(Specialization spec, Color color) {
        int size = 48;
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(color.r * 0.5f, color.g * 0.5f, color.b * 0.5f, 1f));
        pixmap.fill();
        pixmap.setColor(color);
        pixmap.fillCircle(size / 2, size / 2, size / 2 - 4);
        pixmap.setColor(Color.BLACK);
        pixmap.drawCircle(size / 2, size / 2, size / 2 - 4);
        specializationIcons.put(spec, new Texture(pixmap));
        pixmap.dispose();
    }

    public void selectTile(int tileX, int tileY) {
        selectedTileX = tileX;
        selectedTileY = tileY;
        tileSelected = true;
        respecMode = false;

        if (!world.isRevealed(tileX, tileY)) {
            if (world.hasRevealedNeighbor(tileX, tileY)) {
                world.reveal(tileX, tileY);
            }
        }

        updateAvailableTools();
    }

    public void initToolbar() {
        availableTools.clear();
        availableTools.add(new BuildToolbar.ToolButton(TOOL_NEW_SETTLEMENT, "New Settlement", null));
        buildToolbar.setTools(availableTools);
    }

    private void updateAvailableTools() {
        availableTools.clear();

        Tile tile = world.getTile(selectedTileX, selectedTileY);
        boolean isBuildable = tile.isBuildable();
        Settlement onTile = world.getSettlementAt(selectedTileX, selectedTileY);
        Settlement nearby = getNearbySettlement(selectedTileX, selectedTileY);

        if (onTile != null && onTile.needsSpecializationChoice()) {
            // Focused specialization choice — Village → Town upgrade
            addSpecButtons(TOOL_SPEC_LOGGING, TOOL_SPEC_MINING, TOOL_SPEC_FARMING, TOOL_SPEC_TRADE);

        } else if (respecMode && onTile != null && onTile.canRespecialize()) {
            // Re-specialization mode — choose new specialization (costs one level)
            addSpecButtons(TOOL_RESPEC_LOGGING, TOOL_RESPEC_MINING, TOOL_RESPEC_FARMING, TOOL_RESPEC_TRADE);

        } else {
            availableTools.add(new BuildToolbar.ToolButton(TOOL_NEW_SETTLEMENT, "New Settlement", null));

            if (onTile != null && onTile.needsUpgrade()) {
                availableTools.add(new BuildToolbar.ToolButton(TOOL_UPGRADE, "Upgrade", null));
            }

            if (onTile != null && onTile.canRespecialize()) {
                availableTools.add(new BuildToolbar.ToolButton(TOOL_RESPEC_MODE, "Re-spec", null));
            }

            if (tile.terrain.isTraversable() && !tile.hasObject()) {
                availableTools.add(new BuildToolbar.ToolButton(TOOL_BUILD_ROAD, "Build Road", roadIcon));
            }

            if (tile.hasObject() || tile.hasBuilding() || tile.roadType != 0) {
                availableTools.add(new BuildToolbar.ToolButton(TOOL_DESTROY, "Destroy", destroyIcon));
            }

            if (isBuildable && nearby != null) {
                addBuildingButton(TOOL_HOUSE,     BuildingType.HOUSE_SIMPLE);
                addBuildingButton(TOOL_FARM,      BuildingType.FARM_SMALL);
                addBuildingButton(TOOL_MARKET,    BuildingType.MARKET_SMALL);
                addBuildingButton(TOOL_WAREHOUSE, BuildingType.WAREHOUSE);
                addBuildingButton(TOOL_WELL,      BuildingType.WELL_WATER);
            }
        }

        buildToolbar.setTools(availableTools);
        if (selectedToolId >= 0) {
            buildToolbar.selectTool(selectedToolId);
        } else {
            buildToolbar.deselectTool();
        }
    }

    private void addSpecButtons(int logId, int minId, int farId, int tradeId) {
        availableTools.add(new BuildToolbar.ToolButton(logId,   "Logging Camp",   specializationIcons.get(Specialization.LOGGING_CAMP)));
        availableTools.add(new BuildToolbar.ToolButton(minId,   "Mining Town",    specializationIcons.get(Specialization.MINING_TOWN)));
        availableTools.add(new BuildToolbar.ToolButton(farId,   "Farm Village",   specializationIcons.get(Specialization.FARMING_VILLAGE)));
        availableTools.add(new BuildToolbar.ToolButton(tradeId, "Trade Hub",      specializationIcons.get(Specialization.TRADE_HUB)));
    }

    private void addBuildingButton(int toolId, BuildingType type) {
        if (availableTools.size() < 6) {
            availableTools.add(new BuildToolbar.ToolButton(toolId, type.getDisplayName(), getBuildingTexture(type)));
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
        switch (toolId) {
            case TOOL_NEW_SETTLEMENT:
                placeSettlement(selectedTileX, selectedTileY);
                break;
            case TOOL_HOUSE:
                placeBuilding(selectedTileX, selectedTileY, BuildingType.HOUSE_SIMPLE.getId());
                break;
            case TOOL_FARM:
                placeBuilding(selectedTileX, selectedTileY, BuildingType.FARM_SMALL.getId());
                break;
            case TOOL_MARKET:
                placeBuilding(selectedTileX, selectedTileY, BuildingType.MARKET_SMALL.getId());
                break;
            case TOOL_WAREHOUSE:
                placeBuilding(selectedTileX, selectedTileY, BuildingType.WAREHOUSE.getId());
                break;
            case TOOL_WELL:
                placeBuilding(selectedTileX, selectedTileY, BuildingType.WELL_WATER.getId());
                break;
            // Specialization choice (Village → Town)
            case TOOL_SPEC_LOGGING:
                specializeSettlement(selectedTileX, selectedTileY, Specialization.LOGGING_CAMP);
                break;
            case TOOL_SPEC_MINING:
                specializeSettlement(selectedTileX, selectedTileY, Specialization.MINING_TOWN);
                break;
            case TOOL_SPEC_FARMING:
                specializeSettlement(selectedTileX, selectedTileY, Specialization.FARMING_VILLAGE);
                break;
            case TOOL_SPEC_TRADE:
                specializeSettlement(selectedTileX, selectedTileY, Specialization.TRADE_HUB);
                break;
            // Upgrade (Town → City, etc.)
            case TOOL_UPGRADE:
                upgradeSettlement(selectedTileX, selectedTileY);
                break;
            // Enter re-specialize mode
            case TOOL_RESPEC_MODE:
                respecMode = true;
                break;
            case TOOL_BUILD_ROAD:
                world.placeRoad(selectedTileX, selectedTileY, RoadType.DIRT);
                break;
            case TOOL_DESTROY:
                destroyTile(selectedTileX, selectedTileY);
                break;
            // Re-specialization choice
            case TOOL_RESPEC_LOGGING:
                respecializeSettlement(selectedTileX, selectedTileY, Specialization.LOGGING_CAMP);
                break;
            case TOOL_RESPEC_MINING:
                respecializeSettlement(selectedTileX, selectedTileY, Specialization.MINING_TOWN);
                break;
            case TOOL_RESPEC_FARMING:
                respecializeSettlement(selectedTileX, selectedTileY, Specialization.FARMING_VILLAGE);
                break;
            case TOOL_RESPEC_TRADE:
                respecializeSettlement(selectedTileX, selectedTileY, Specialization.TRADE_HUB);
                break;
        }

        updateAvailableTools();
    }

    private void specializeSettlement(int tx, int ty, Specialization spec) {
        Settlement settlement = world.getSettlementAt(tx, ty);
        if (settlement != null) {
            settlement.specialize(spec);
        }
    }

    private void upgradeSettlement(int tx, int ty) {
        Settlement settlement = world.getSettlementAt(tx, ty);
        if (settlement != null) {
            settlement.upgrade();
        }
    }

    private void respecializeSettlement(int tx, int ty, Specialization newSpec) {
        Settlement settlement = world.getSettlementAt(tx, ty);
        if (settlement != null) {
            settlement.respecialize(newSpec);
        }
        respecMode = false;
    }

    private void placeSettlement(int tx, int ty) {
        Tile tile = world.getTile(tx, ty);
        if (!tile.isBuildable()) return;
        if (world.getSettlementAt(tx, ty) != null) return;
        if (!world.hasRevealedNeighbor(tx, ty)) return;

        String name = "Settlement " + (world.getSettlements().size() + 1);
        world.createSettlement(name, tx, ty);
    }

    private void placeBuilding(int tx, int ty, int buildingId) {
        Settlement settlement = getNearbySettlement(tx, ty);
        if (settlement == null) return;

        Tile tile = world.getTile(tx, ty);
        if (!tile.isBuildable()) return;

        world.setBuilding(tx, ty, buildingId);
        settlement.addBuilding(buildingId);

        BuildingType type = BuildingType.fromId(buildingId);
        if (type != null) {
            settlement.addPopulation(type.getPopulationCapacity());
        }
    }

    public void handleClick(int screenX, int screenY) {
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        int glY = screenHeight - screenY;

        int toolId = buildToolbar.getToolIdAt(screenX, glY, screenWidth, screenHeight);
        if (toolId >= 0 && tileSelected) {
            selectedToolId = toolId;
            buildToolbar.selectTool(toolId);
            executeTool(toolId);
            return;
        }

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

    @Override
    public boolean handleDrag(int screenX, int screenY) {
        return false;
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
        float delta = Gdx.graphics.getDeltaTime();
        tickAccumulator += delta;
        while (tickAccumulator >= TICK_INTERVAL) {
            simulation.tick(TICK_INTERVAL);
            tickAccumulator -= TICK_INTERVAL;
        }
        simulation.updateCaravans(delta);

        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);

        renderer.render();

        renderUI();
    }

    private void renderUI() {
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        if (tileSelected && world.isRevealed(selectedTileX, selectedTileY)) {
            Settlement settlement = world.getSettlementAt(selectedTileX, selectedTileY);
            Texture tex = (settlement != null) ? settlementTexture : selectionTexture;
            batch.setProjectionMatrix(camera.combined);
            batch.begin();
            batch.draw(tex, selectedTileX * 64, selectedTileY * 64, 64, 64);
            batch.end();
        }

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
        renderResourceHud(uiBatch, screenWidth, screenHeight);
        uiBatch.end();
    }

    private void renderResourceHud(SpriteBatch b, int sw, int sh) {
        if (hudFont == null) return;
        // Aggregate resources across all settlements
        float totalWood = 0, totalStone = 0, totalFood = 0, totalGoods = 0, totalGold = 0;
        for (Settlement s : world.getSettlements()) {
            totalWood  += s.wood;
            totalStone += s.stone;
            totalFood  += s.food;
            totalGoods += s.goods;
            totalGold  += s.gold;
        }
        int routes = world.getTradeRoutes().size();
        int caravans = world.getCaravans().size();

        float x = 15;
        float y = sh - 15;
        int lineH = 17;

        hudFont.setColor(new Color(0.85f, 0.85f, 1f, 1f));
        hudFont.draw(b, String.format("Wood:  %5.0f   Stone: %5.0f", totalWood,  totalStone), x, y); y -= lineH;
        hudFont.draw(b, String.format("Food:  %5.0f   Goods: %5.0f", totalFood,  totalGoods), x, y); y -= lineH;
        hudFont.draw(b, String.format("Gold:  %5.0f   Routes: %d  Caravans: %d",
            totalGold, routes, caravans), x, y);
        hudFont.setColor(Color.WHITE);
    }

    @Override
    public void dispose() {
        world.saveSettlements(Gdx.files.local("data/settlements.dat"));
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
        for (Texture tex : specializationIcons.values()) {
            tex.dispose();
        }
        if (roadIcon != null) roadIcon.dispose();
        if (destroyIcon != null) destroyIcon.dispose();
        if (hudFont != null) hudFont.dispose();
    }

    @Override
    public World getWorld() {
        return world;
    }
}
