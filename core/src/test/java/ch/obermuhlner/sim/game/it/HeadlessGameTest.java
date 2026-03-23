package ch.obermuhlner.sim.game.it;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.graphics.OrthographicCamera;
import ch.obermuhlner.sim.game.GameConfig;
import ch.obermuhlner.sim.game.GameController;
import ch.obermuhlner.sim.game.TileObjectRegistry;
import ch.obermuhlner.sim.game.World;
import ch.obermuhlner.sim.game.mode.GameMode;
import org.junit.After;
import org.junit.Before;

public abstract class HeadlessGameTest implements GameController {
    protected World world;
    protected OrthographicCamera camera;
    protected GameMode currentMode;
    protected HeadlessApplication application;

    @Before
    public void setUpGame() {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        config.updatesPerSecond = 60;
        
        application = new HeadlessApplication(new TestApplicationListener(), config);
        Gdx.app = application;
        
        TileObjectRegistry.init();
        
        world = new World(16, new GameConfig(new GameConfig.Root()), true);
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 600);
        camera.position.set(32, 32, 0);
    }

    @After
    public void tearDownGame() {
        if (currentMode != null) {
            currentMode.dispose();
        }
        if (application != null) {
            application.exit();
            application = null;
        }
        Gdx.app = null;
    }

    @Override
    public void setGameMode(GameMode mode) {
        if (currentMode != null) {
            currentMode.dispose();
        }
        currentMode = mode;
        if (mode != null) {
            mode.init(world, camera);
        }
    }

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public void handleClick(int screenX, int screenY) {
        if (currentMode != null) {
            currentMode.touchDown(screenX, screenY, 0, 0);
            currentMode.touchUp(screenX, screenY, 0, 0);
        }
    }

    @Override
    public boolean handleDrag(int screenX, int screenY) {
        return false;
    }

    protected void setInputProcessor(InputProcessor processor) {
        Gdx.input.setInputProcessor(processor);
    }

    protected void simulateTouchDown(int screenX, int screenY, int pointer, int button) {
        if (currentMode != null) {
            currentMode.touchDown(screenX, screenY, pointer, button);
        }
    }

    protected void simulateTouchUp(int screenX, int screenY, int pointer, int button) {
        if (currentMode != null) {
            currentMode.touchUp(screenX, screenY, pointer, button);
        }
    }

    protected void simulateTouchDragged(int screenX, int screenY, int pointer) {
        if (currentMode != null) {
            currentMode.touchDragged(screenX, screenY, pointer);
        }
    }

    protected void simulateKeyDown(int keycode) {
        if (currentMode != null) {
            currentMode.keyDown(keycode);
        }
    }

    protected void simulateKeyUp(int keycode) {
        if (currentMode != null) {
            currentMode.keyUp(keycode);
        }
    }

    protected void simulateScroll(float amountX, float amountY) {
        if (currentMode != null) {
            currentMode.scrolled(amountX, amountY);
        }
    }

    protected int worldToScreenX(int tileX) {
        return (int) (tileX * 64 + 32);
    }

    protected int worldToScreenY(int tileY) {
        return (int) (tileY * 64 + 32);
    }

    private class TestApplicationListener implements ApplicationListener {
        @Override
        public void create() {}

        @Override
        public void resize(int width, int height) {}

        @Override
        public void render() {}

        @Override
        public void pause() {}

        @Override
        public void resume() {}

        @Override
        public void dispose() {}
    }
}
