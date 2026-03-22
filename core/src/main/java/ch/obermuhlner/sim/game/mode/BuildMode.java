package ch.obermuhlner.sim.game.mode;

import ch.obermuhlner.sim.game.GameController;
import ch.obermuhlner.sim.game.*;
import ch.obermuhlner.sim.game.render.BuildingRenderLayer;
import ch.obermuhlner.sim.game.ui.BuildToolbar;
import ch.obermuhlner.sim.game.ui.SettlementInfoPanel;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.graphics.OrthographicCamera;

public class BuildMode implements GameMode {
    private World world;
    private OrthographicCamera camera;
    private GameController controller;
    private SpriteBatch uiBatch;
    
    private boolean placingSettlement = false;
    private boolean placingBuilding = false;
    private int selectedBuildingType = 0;
    private Settlement selectedSettlement = null;
    
    private int hoverTileX = -1;
    private int hoverTileY = -1;
    private boolean validPlacement = false;
    
    private Texture validTexture;
    private Texture invalidTexture;
    private Texture settlementTexture;
    
    private boolean mouseDown = false;
    private int lastMouseX, lastMouseY;
    private boolean wasDragging = false;
    
    private BuildToolbar toolbar;

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
        this.uiBatch.setProjectionMatrix(camera.combined);
        
        Pixmap validMap = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        validMap.setColor(0, 1, 0, 0.3f);
        validMap.fill();
        validTexture = new Texture(validMap);
        validMap.dispose();
        
        Pixmap invalidMap = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        invalidMap.setColor(1, 0, 0, 0.3f);
        invalidMap.fill();
        invalidTexture = new Texture(invalidMap);
        invalidMap.dispose();
        
        Pixmap settlementMap = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        settlementMap.setColor(0.3f, 0.7f, 0.3f, 0.5f);
        settlementMap.fillCircle(32, 32, 28);
        settlementTexture = new Texture(settlementMap);
        settlementMap.dispose();
    }

    public void startSettlementPlacement() {
        placingSettlement = true;
        placingBuilding = false;
        selectedSettlement = null;
    }

    public void startBuildingPlacement(int buildingType) {
        placingBuilding = true;
        placingSettlement = false;
        selectedBuildingType = buildingType;
    }

    public void selectSettlement(Settlement settlement) {
        selectedSettlement = settlement;
        placingSettlement = false;
        placingBuilding = false;
    }

    public void clearSelection() {
        selectedSettlement = null;
        placingSettlement = false;
        placingBuilding = false;
    }

    @Override
    public void update(float delta) {
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.ESCAPE) {
            clearSelection();
            ExploreMode newMode = new ExploreMode();
            newMode.init(controller.getWorld(), camera);
            newMode.setMain(controller);
            controller.setGameMode(newMode);
            return true;
        }
        if (keycode == Input.Keys.S) {
            startSettlementPlacement();
            if (toolbar != null) toolbar.select(0);
            return true;
        }
        if (keycode >= Input.Keys.NUM_1 && keycode <= Input.Keys.NUM_9) {
            int index = keycode - Input.Keys.NUM_1;
            if (index < BuildingType.values().length) {
                startBuildingPlacement(BuildingType.values()[index].getId());
                if (toolbar != null) toolbar.select(index + 1);
            }
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
            java.util.List<Settlement> settlements = world.getSettlements();
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
            clearSelection();
            return true;
        }
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (button == Input.Buttons.LEFT && !wasDragging) {
            mouseDown = false;
            
            // Check if click was on toolbar
            if (toolbar != null) {
                int screenWidth = Gdx.graphics.getWidth();
                int screenHeight = Gdx.graphics.getHeight();
                int buttonIndex = toolbar.getButtonAt(screenX, screenY, screenWidth, screenHeight);
                if (buttonIndex >= 0) {
                    handleToolbarClick(buttonIndex);
                    return true;
                }
            }
            
            Vector3 worldPos = camera.unproject(new Vector3(screenX, screenY, 0));
            int tileX = (int) Math.floor(worldPos.x / 64);
            int tileY = (int) Math.floor(worldPos.y / 64);
            
            // Handle fog reveal - if tile not revealed, reveal it
            if (!world.isRevealed(tileX, tileY)) {
                if (world.hasRevealedNeighbor(tileX, tileY)) {
                    world.reveal(tileX, tileY);
                }
                return true;
            }
            
            if (placingSettlement) {
                placeSettlement(tileX, tileY);
            } else if (placingBuilding) {
                placeBuilding(tileX, tileY);
            } else {
                // Select a settlement if clicking on one
                Settlement clicked = world.getSettlementAt(tileX, tileY);
                if (clicked != null) {
                    selectSettlement(clicked);
                }
            }
            return true;
        }
        mouseDown = false;
        return false;
    }
    
    private void handleToolbarClick(int buttonIndex) {
        if (buttonIndex == 0) {
            // Settlement button
            startSettlementPlacement();
            toolbar.select(0);
        } else {
            // Building button - index 1 = first building
            int buildingIndex = buttonIndex - 1;
            if (buildingIndex < BuildingType.values().length) {
                startBuildingPlacement(BuildingType.values()[buildingIndex].getId());
                toolbar.select(buttonIndex);
            }
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
        Vector3 worldPos = camera.unproject(new Vector3(screenX, screenY, 0));
        hoverTileX = (int) Math.floor(worldPos.x / 64);
        hoverTileY = (int) Math.floor(worldPos.y / 64);
        updateValidPlacement();
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        camera.zoom = Math.max(0.25f, Math.min(4.0f, camera.zoom - amountY * 0.1f));
        return true;
    }

    private void updateValidPlacement() {
        if (hoverTileX < 0 || hoverTileY < 0) {
            validPlacement = false;
            return;
        }
        
        if (placingSettlement) {
            Tile tile = world.getTile(hoverTileX, hoverTileY);
            validPlacement = tile.terrain.isBuildable() && 
                              world.getSettlementAt(hoverTileX, hoverTileY) == null &&
                              world.hasRevealedNeighbor(hoverTileX, hoverTileY);
        } else if (placingBuilding && selectedSettlement != null) {
            Tile tile = world.getTile(hoverTileX, hoverTileY);
            int dx = Math.abs(hoverTileX - selectedSettlement.centerX);
            int dy = Math.abs(hoverTileY - selectedSettlement.centerY);
            validPlacement = tile.terrain.isBuildable() && 
                              !tile.hasBuilding() &&
                              dx <= 3 && dy <= 3;
        } else {
            validPlacement = false;
        }
    }

    private void placeSettlement(int tx, int ty) {
        // Check if tile is valid for settlement
        Tile tile = world.getTile(tx, ty);
        if (!tile.terrain.isBuildable()) return;
        if (world.getSettlementAt(tx, ty) != null) return;
        if (!world.hasRevealedNeighbor(tx, ty)) return;
        
        String name = "Settlement " + (world.getSettlements().size() + 1);
        Settlement settlement = world.createSettlement(name, tx, ty);
        if (settlement != null) {
            selectSettlement(settlement);
            placingSettlement = false;
        }
    }

    private void placeBuilding(int tx, int ty) {
        if (selectedSettlement == null) return;
        
        Tile tile = world.getTile(tx, ty);
        if (!tile.terrain.isBuildable()) return;
        if (tile.hasBuilding()) return;
        
        int dx = Math.abs(tx - selectedSettlement.centerX);
        int dy = Math.abs(ty - selectedSettlement.centerY);
        if (dx > 3 || dy > 3) return;
        
        tile.buildingId = selectedBuildingType;
        selectedSettlement.addBuilding(selectedBuildingType);
        
        BuildingType type = BuildingType.fromId(selectedBuildingType);
        if (type != null) {
            selectedSettlement.addPopulation(type.getPopulationCapacity());
        }
    }

    public void renderUI() {
        if (uiBatch == null) return;
        if (camera == null) return;
        if (hoverTileX < 0 || hoverTileY < 0) return;
        if (!world.isRevealed(hoverTileX, hoverTileY)) return;
        
        uiBatch.setProjectionMatrix(camera.combined);
        uiBatch.begin();
        
        Texture texture = validPlacement ? validTexture : invalidTexture;
        if (placingSettlement) {
            texture = validPlacement ? settlementTexture : invalidTexture;
        }
        
        uiBatch.draw(texture,
            hoverTileX * 64, hoverTileY * 64,
            64, 64);
        
        uiBatch.end();
    }

    public void renderPanel(SettlementInfoPanel panel) {
        if (uiBatch == null) return;
        if (selectedSettlement == null) return;
        
        if (uiBatch.isDrawing()) {
            uiBatch.end();
        }
        
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();
        uiBatch.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4().setToOrtho2D(0, 0, screenWidth, screenHeight));
        uiBatch.begin();
        panel.render(selectedSettlement, uiBatch, screenWidth, screenHeight);
        uiBatch.end();
    }

    public void renderToolbar(BuildToolbar toolbar) {
        if (uiBatch == null) return;
        
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();
        
        uiBatch.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4().setToOrtho2D(0, 0, screenWidth, screenHeight));
        uiBatch.begin();
        toolbar.render(uiBatch, screenWidth, screenHeight);
        uiBatch.end();
    }

    public boolean isPlacingSettlement() {
        return placingSettlement;
    }

    public boolean isPlacingBuilding() {
        return placingBuilding;
    }

    public int getSelectedBuildingType() {
        return selectedBuildingType;
    }

    public Settlement getSelectedSettlement() {
        return selectedSettlement;
    }
    
    public SpriteBatch getUiBatch() {
        return uiBatch;
    }
    
    @Override
    public void dispose() {
        if (uiBatch != null) uiBatch.dispose();
        if (validTexture != null) validTexture.dispose();
        if (invalidTexture != null) invalidTexture.dispose();
        if (settlementTexture != null) settlementTexture.dispose();
    }
}

