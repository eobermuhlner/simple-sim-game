package ch.obermuhlner.sim.game.mode;

import ch.obermuhlner.sim.game.*;
import ch.obermuhlner.sim.game.ui.BuildToolbar;
import ch.obermuhlner.sim.game.ui.SettlementInfoPanel;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.graphics.OrthographicCamera;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuildMode implements GameMode {
    public static final int TOOL_NEW_SETTLEMENT = 0;
    public static final int TOOL_BUILD_HOUSE = 1;
    public static final int TOOL_BUILD_FARM = 2;
    public static final int TOOL_BUILD_MARKET = 3;
    public static final int TOOL_BUILD_WAREHOUSE = 4;
    public static final int TOOL_BUILD_WELL = 5;

    private World world;
    private OrthographicCamera camera;
    private GameController controller;
    private SpriteBatch uiBatch;

    private int selectedTileX = 0;
    private int selectedTileY = 0;
    private int selectedToolId = -1;

    private Texture selectionTexture;
    private Texture settlementTexture;

    private boolean mouseDown = false;
    private int lastMouseX, lastMouseY;
    private boolean wasDragging = false;

    private BuildToolbar toolbar;
    private List<BuildToolbar.ToolButton> availableTools = new ArrayList<>();
    private Map<Integer, Texture> buildingTextures = new HashMap<>();

    public BuildMode(GameController controller) {
        this.controller = controller;
    }

    public void setToolbar(BuildToolbar toolbar) {
        this.toolbar = toolbar;
    }

    @Override
    public String getName() {
        return "Build";
    }

    @Override
    public void init(World world, OrthographicCamera camera) {
        this.world = world;
        this.camera = camera;
        this.uiBatch = new SpriteBatch();

        Pixmap selectionMap = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        selectionMap.setColor(new Color(0.3f, 0.7f, 0.3f, 0.5f));
        selectionMap.fill();
        selectionMap.setColor(Color.WHITE);
        selectionMap.drawRectangle(0, 0, 64, 64);
        selectionTexture = new Texture(selectionMap);
        selectionMap.dispose();

        Pixmap settlementMap = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        settlementMap.setColor(new Color(0.3f, 0.7f, 0.3f, 0.5f));
        settlementMap.fillCircle(32, 32, 28);
        settlementTexture = new Texture(settlementMap);
        settlementMap.dispose();

        loadBuildingTextures();
    }

    private void loadBuildingTextures() {
        GameConfig config = controller.getGameConfig();
        for (BuildingType type : BuildingType.values()) {
            try {
                String texturePath = config.getBuildingTexturePath(type);
                Texture tex = new Texture(texturePath);
                buildingTextures.put(type.getId(), tex);
            } catch (Exception e) {
            }
        }
    }

    @Override
    public void update(float delta) {
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.ESCAPE) {
            selectedToolId = -1;
            toolbar.deselectTool();
            updateAvailableTools();
            return true;
        }
        if (keycode == Input.Keys.F) {
            ExploreMode newMode = new ExploreMode();
            newMode.init(controller.getWorld(), camera);
            newMode.setMain(controller);
            controller.setGameMode(newMode);
            return true;
        }
        if (keycode == Input.Keys.HOME) {
            List<Settlement> settlements = world.getSettlements();
            if (!settlements.isEmpty()) {
                Settlement last = settlements.get(settlements.size() - 1);
                camera.position.set(
                    last.centerX * 64 + 32f,
                    last.centerY * 64 + 32f,
                    0
                );
            }
            return true;
        }
        if (keycode >= Input.Keys.NUM_1 && keycode <= Input.Keys.NUM_9) {
            int index = keycode - Input.Keys.NUM_1;
            if (index < availableTools.size()) {
                selectedToolId = availableTools.get(index).id;
                toolbar.selectTool(selectedToolId);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (button == Input.Buttons.LEFT) {
            mouseDown = true;
            wasDragging = false;
            lastMouseX = screenX;
            lastMouseY = screenY;
            return true;
        }
        if (button == Input.Buttons.RIGHT) {
            selectedToolId = -1;
            toolbar.deselectTool();
            updateAvailableTools();
            return true;
        }
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (button == Input.Buttons.LEFT && !wasDragging) {
            mouseDown = false;

            int screenWidth = Gdx.graphics.getWidth();
            int screenHeight = Gdx.graphics.getHeight();

            int toolId = toolbar.getToolIdAt(screenX, screenY, screenWidth, screenHeight);
            if (toolId >= 0) {
                selectedToolId = toolId;
                toolbar.selectTool(toolId);
                executeTool(toolId, selectedTileX, selectedTileY);
                return true;
            }

            Vector3 worldPos = camera.unproject(new Vector3(screenX, screenY, 0));
            int tileX = (int) Math.floor(worldPos.x / 64);
            int tileY = (int) Math.floor(worldPos.y / 64);

            selectTile(tileX, tileY);
            return true;
        }
        mouseDown = false;
        return false;
    }

    private void selectTile(int tileX, int tileY) {
        selectedTileX = tileX;
        selectedTileY = tileY;

        if (!world.isRevealed(tileX, tileY)) {
            if (world.hasRevealedNeighbor(tileX, tileY)) {
                world.reveal(tileX, tileY);
            }
        }

        updateAvailableTools();
    }

    private void updateAvailableTools() {
        availableTools.clear();

        Tile tile = world.getTile(selectedTileX, selectedTileY);
        boolean isBuildable = tile.terrain.isBuildable();
        Settlement nearbySettlement = getNearbySettlement(selectedTileX, selectedTileY);

        availableTools.add(new BuildToolbar.ToolButton(
            TOOL_NEW_SETTLEMENT,
            "New Settlement",
            null
        ));

        if (isBuildable && nearbySettlement != null) {
            BuildingType house = BuildingType.HOUSE_SIMPLE;
            availableTools.add(new BuildToolbar.ToolButton(
                TOOL_BUILD_HOUSE,
                house.getDisplayName(),
                buildingTextures.get(house.getId())
            ));

            BuildingType farm = BuildingType.FARM_SMALL;
            availableTools.add(new BuildToolbar.ToolButton(
                TOOL_BUILD_FARM,
                farm.getDisplayName(),
                buildingTextures.get(farm.getId())
            ));

            BuildingType market = BuildingType.MARKET_SMALL;
            availableTools.add(new BuildToolbar.ToolButton(
                TOOL_BUILD_MARKET,
                market.getDisplayName(),
                buildingTextures.get(market.getId())
            ));

            BuildingType warehouse = BuildingType.WAREHOUSE;
            availableTools.add(new BuildToolbar.ToolButton(
                TOOL_BUILD_WAREHOUSE,
                warehouse.getDisplayName(),
                buildingTextures.get(warehouse.getId())
            ));

            BuildingType well = BuildingType.WELL_WATER;
            availableTools.add(new BuildToolbar.ToolButton(
                TOOL_BUILD_WELL,
                well.getDisplayName(),
                buildingTextures.get(well.getId())
            ));
        }

        if (toolbar != null) {
            toolbar.setTools(availableTools);
            if (selectedToolId >= 0) {
                toolbar.selectTool(selectedToolId);
            }
        }
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

    private void executeTool(int toolId, int tx, int ty) {
        Tile tile = world.getTile(tx, ty);

        switch (toolId) {
            case TOOL_NEW_SETTLEMENT:
                placeSettlement(tx, ty);
                break;
            case TOOL_BUILD_HOUSE:
                placeBuilding(tx, ty, BuildingType.HOUSE_SIMPLE.getId());
                break;
            case TOOL_BUILD_FARM:
                placeBuilding(tx, ty, BuildingType.FARM_SMALL.getId());
                break;
            case TOOL_BUILD_MARKET:
                placeBuilding(tx, ty, BuildingType.MARKET_SMALL.getId());
                break;
            case TOOL_BUILD_WAREHOUSE:
                placeBuilding(tx, ty, BuildingType.WAREHOUSE.getId());
                break;
            case TOOL_BUILD_WELL:
                placeBuilding(tx, ty, BuildingType.WELL_WATER.getId());
                break;
        }
    }

    private void placeSettlement(int tx, int ty) {
        Tile tile = world.getTile(tx, ty);
        if (!tile.terrain.isBuildable()) return;
        if (world.getSettlementAt(tx, ty) != null) return;
        if (!world.hasRevealedNeighbor(tx, ty)) return;

        String name = "Settlement " + (world.getSettlements().size() + 1);
        Settlement settlement = world.createSettlement(name, tx, ty);
        if (settlement != null) {
            updateAvailableTools();
        }
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

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (mouseDown) {
            float dx = screenX - lastMouseX;
            float dy = screenY - lastMouseY;
            if (Math.abs(dx) > 2 || Math.abs(dy) > 2) {
                wasDragging = true;
            }
            camera.translate(-dx * camera.zoom, dy * camera.zoom);
            lastMouseX = screenX;
            lastMouseY = screenY;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        if (toolbar != null) {
            int screenWidth = Gdx.graphics.getWidth();
            int screenHeight = Gdx.graphics.getHeight();
            toolbar.updateHover(screenX, screenY, screenWidth, screenHeight);
        }
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        camera.zoom = Math.max(0.25f, Math.min(4.0f, camera.zoom - amountY * 0.1f));
        return true;
    }

    public void renderUI() {
        if (uiBatch == null || camera == null) return;
        if (!world.isRevealed(selectedTileX, selectedTileY)) return;

        uiBatch.setProjectionMatrix(camera.combined);
        uiBatch.begin();

        Settlement settlement = world.getSettlementAt(selectedTileX, selectedTileY);
        Texture tex = (settlement != null) ? settlementTexture : selectionTexture;

        uiBatch.draw(tex,
            selectedTileX * 64, selectedTileY * 64,
            64, 64);

        uiBatch.end();
    }

    public void renderToolbar() {
        if (uiBatch == null || toolbar == null) return;

        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        uiBatch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, screenWidth, screenHeight));
        uiBatch.begin();
        toolbar.render(uiBatch, screenWidth, screenHeight);
        uiBatch.end();
    }

    public void renderPanel(SettlementInfoPanel panel) {
        if (uiBatch == null) return;

        Settlement settlement = world.getSettlementAt(selectedTileX, selectedTileY);
        if (settlement == null) return;

        if (uiBatch.isDrawing()) {
            uiBatch.end();
        }

        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();
        uiBatch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, screenWidth, screenHeight));
        uiBatch.begin();
        panel.render(settlement, uiBatch, screenWidth, screenHeight);
        uiBatch.end();
    }

    public SpriteBatch getUiBatch() {
        return uiBatch;
    }
    
    public int getSelectedTileX() {
        return selectedTileX;
    }
    
    public int getSelectedTileY() {
        return selectedTileY;
    }
    
    public int getSelectedToolId() {
        return selectedToolId;
    }
    
    public int getAvailableToolCount() {
        return availableTools.size();
    }
    
    public String getToolName(int index) {
        if (index >= 0 && index < availableTools.size()) {
            return availableTools.get(index).label;
        }
        return null;
    }

    @Override
    public void dispose() {
        if (uiBatch != null) uiBatch.dispose();
        if (selectionTexture != null) selectionTexture.dispose();
        if (settlementTexture != null) settlementTexture.dispose();
        for (Texture tex : buildingTextures.values()) {
            tex.dispose();
        }
    }
}
