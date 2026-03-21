package ch.obermuhlner.sim;

import ch.obermuhlner.sim.game.GameController;
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
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;

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
    private boolean showBuildUI = false;

    @Override
    public void create() {
        TileObjectRegistry.init();
        
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(TILE_SIZE / 2f, TILE_SIZE / 2f, 0);

        world = new World(CHUNK_SIZE, WORLD_SEED);
        
        renderer = new Renderer(world, batch, camera);
        renderer.addLayer(new TerrainRenderLayer(world, true));
        renderer.addLayer(new ObjectRenderLayer(world, true));
        renderer.addLayer(new BuildingRenderLayer(world, true));
        renderer.addLayer(new SettlementRenderLayer(world, true));
        renderer.addLayer(new FogOfWarRenderLayer(world));

        settlementPanel = new SettlementInfoPanel();
        buildToolbar = new BuildToolbar();

        ExploreMode exploreMode = new ExploreMode();
        exploreMode.init(world, camera);
        exploreMode.setMain(this);
        setGameMode(exploreMode);

        Gdx.input.setInputProcessor(inputMultiplexer);
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
        
        if (showBuildUI && currentMode instanceof BuildMode) {
            BuildMode bm = (BuildMode) currentMode;
            if (bm.getUiBatch() != null) {
                bm.renderToolbar(buildToolbar);
                bm.renderUI();
                bm.renderPanel(settlementPanel);
            }
        }
    }

    @Override
    public void dispose() {
        world.saveDirtyChunks();
        renderer.dispose();
        settlementPanel.dispose();
        buildToolbar.dispose();
        batch.dispose();
    }
    
    @Override
    public World getWorld() {
        return world;
    }
}
