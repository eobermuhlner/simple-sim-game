package ch.obermuhlner.sim;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;

public class Main extends ApplicationAdapter {
    private static final int TILE_SIZE = 64;
    private static final int MAP_WIDTH = 200;
    private static final int MAP_HEIGHT = 200;

    private SpriteBatch batch;
    private Texture tileset;
    private OrthographicCamera camera;
    private boolean[][] visible;
    private int[][] map;

    private boolean mouseDown = false;
    private float totalDrag = 0;
    private int lastMouseX, lastMouseY;

    @Override
    public void create() {
        batch = new SpriteBatch();
        tileset = new Texture("64x64/map.png");

        map = new int[MAP_HEIGHT][MAP_WIDTH];
        for (int y = 0; y < MAP_HEIGHT; y++) {
            for (int x = 0; x < MAP_WIDTH; x++) {
                map[y][x] = 1;
            }
        }

        visible = new boolean[MAP_HEIGHT][MAP_WIDTH];
        visible[MAP_HEIGHT / 2][MAP_WIDTH / 2] = true;

        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(MAP_WIDTH * TILE_SIZE / 2f, MAP_HEIGHT * TILE_SIZE / 2f, 0);
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        clampCamera();
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);

        handleInput();

        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        int startX = Math.max(0, (int) ((camera.position.x - camera.viewportWidth / 2) / TILE_SIZE));
        int startY = Math.max(0, (int) ((camera.position.y - camera.viewportHeight / 2) / TILE_SIZE));
        int endX = Math.min(MAP_WIDTH, startX + (int) (camera.viewportWidth / TILE_SIZE) + 2);
        int endY = Math.min(MAP_HEIGHT, startY + (int) (camera.viewportHeight / TILE_SIZE) + 2);

        for (int y = startY; y < endY; y++) {
            for (int x = startX; x < endX; x++) {
                int tile = visible[y][x] ? map[y][x] : 0;
                batch.draw(tileset,
                    x * TILE_SIZE, y * TILE_SIZE,
                    TILE_SIZE, TILE_SIZE,
                    tile * TILE_SIZE, 0,
                    TILE_SIZE, TILE_SIZE,
                    false, false);
            }
        }

        batch.end();
    }

    private void handleInput() {
        int mx = Gdx.input.getX();
        int my = Gdx.input.getY();

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            mouseDown = true;
            totalDrag = 0;
            lastMouseX = mx;
            lastMouseY = my;
        }

        if (mouseDown && Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            float dx = mx - lastMouseX;
            float dy = my - lastMouseY;
            totalDrag += Math.abs(dx) + Math.abs(dy);
            camera.translate(-dx, dy);
            clampCamera();
            lastMouseX = mx;
            lastMouseY = my;
        }

        if (mouseDown && !Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            mouseDown = false;
            if (totalDrag < 5) {
                Vector3 worldPos = camera.unproject(new Vector3(mx, my, 0));
                int tileX = (int) (worldPos.x / TILE_SIZE);
                int tileY = (int) (worldPos.y / TILE_SIZE);
                revealTile(tileX, tileY);
            }
        }
    }

    private void clampCamera() {
        float halfW = camera.viewportWidth / 2;
        float halfH = camera.viewportHeight / 2;
        camera.position.x = MathUtils.clamp(camera.position.x, halfW, MAP_WIDTH * TILE_SIZE - halfW);
        camera.position.y = MathUtils.clamp(camera.position.y, halfH, MAP_HEIGHT * TILE_SIZE - halfH);
    }

    private void revealTile(int x, int y) {
        if (x < 0 || x >= MAP_WIDTH || y < 0 || y >= MAP_HEIGHT) return;
        if (visible[y][x]) return;
        if (hasVisibleNeighbor(x, y)) {
            visible[y][x] = true;
        }
    }

    private boolean hasVisibleNeighbor(int x, int y) {
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] dir : dirs) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            if (nx >= 0 && nx < MAP_WIDTH && ny >= 0 && ny < MAP_HEIGHT && visible[ny][nx]) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void dispose() {
        batch.dispose();
        tileset.dispose();
    }
}