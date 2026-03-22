package ch.obermuhlner.sim.game.mode;

import ch.obermuhlner.sim.game.GameController;
import ch.obermuhlner.sim.game.Settlement;
import ch.obermuhlner.sim.game.World;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.graphics.OrthographicCamera;

public class ExploreMode implements GameMode {
    private static final float ZOOM_MIN = 0.25f;
    private static final float ZOOM_MAX = 4.0f;
    private static final float ZOOM_SPEED = 0.1f;

    private World world;
    private OrthographicCamera camera;
    private GameController controller;
    private boolean fogOfWar = true;
    private boolean mouseDown = false;
    private float totalDrag = 0;
    private int lastMouseX, lastMouseY;
    private int lastRevealedTileX = 0;
    private int lastRevealedTileY = 0;

    @Override
    public String getName() {
        return "Explore";
    }

    @Override
    public void init(World world, OrthographicCamera camera) {
        this.world = world;
        this.camera = camera;
        world.reveal(0, 0);
    }
    
    public void setMain(GameController controller) {
        this.controller = controller;
    }
    
    public GameController getController() {
        return controller;
    }

    @Override
    public void update(float delta) {
    }

    @Override
    public boolean keyDown(int keycode) {
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        if (keycode == Input.Keys.F) {
            fogOfWar = !fogOfWar;
            return true;
        }
        if (keycode == Input.Keys.HOME) {
            camera.position.set(
                lastRevealedTileX * 64 + 32f,
                lastRevealedTileY * 64 + 32f,
                0
            );
            return true;
        }
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
            totalDrag = 0;
            lastMouseX = screenX;
            lastMouseY = screenY;
            return true;
        }
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (button == Input.Buttons.LEFT) {
            mouseDown = false;
            if (totalDrag < 5) {
                Vector3 worldPos = camera.unproject(new Vector3(screenX, screenY, 0));
                int tileX = (int) Math.floor(worldPos.x / 64);
                int tileY = (int) Math.floor(worldPos.y / 64);
                
                if (controller != null) {
                    controller.handleClick(screenX, screenY);
                } else {
                    revealTile(tileX, tileY);
                }
            }
            return true;
        }
        return false;
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
            totalDrag += Math.abs(dx) + Math.abs(dy);
            camera.translate(-dx * camera.zoom, dy * camera.zoom);
            lastMouseX = screenX;
            lastMouseY = screenY;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        camera.zoom = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, camera.zoom - amountY * ZOOM_SPEED));
        return true;
    }

    private void revealTile(int tx, int ty) {
        if (world.isRevealed(tx, ty)) return;
        if (world.hasRevealedNeighbor(tx, ty)) {
            world.reveal(tx, ty);
            lastRevealedTileX = tx;
            lastRevealedTileY = ty;
        }
    }

    public boolean isFogOfWarEnabled() {
        return fogOfWar;
    }

    @Override
    public void dispose() {
    }
}
