package ch.obermuhlner.sim;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;

public class Main extends ApplicationAdapter {
    private static final int TILE_SIZE = 64;
    private static final int MAP_WIDTH = 10;
    private static final int MAP_HEIGHT = 8;

    private SpriteBatch batch;
    private Texture tileset;
    private OrthographicCamera camera;
    private boolean[][] visible;
    private int[][] map = {
        {2, 2, 2, 2, 1, 1, 2, 2, 2, 2},
        {2, 2, 2, 1, 1, 1, 1, 2, 2, 2},
        {2, 2, 1, 1, 1, 1, 1, 1, 2, 2},
        {2, 1, 1, 1, 1, 1, 1, 1, 1, 2},
        {2, 1, 1, 1, 1, 1, 1, 1, 1, 2},
        {2, 2, 1, 1, 1, 1, 1, 1, 2, 2},
        {2, 2, 2, 1, 1, 1, 1, 2, 2, 2},
        {2, 2, 2, 2, 1, 1, 2, 2, 2, 2}
    };

    @Override
    public void create() {
        batch = new SpriteBatch();
        tileset = new Texture("64x64/map.png");

        visible = new boolean[MAP_HEIGHT][MAP_WIDTH];
        for (int y = 0; y < MAP_HEIGHT; y++) {
            for (int x = 0; x < MAP_WIDTH; x++) {
                visible[y][x] = false;
            }
        }
        visible[MAP_HEIGHT / 2][MAP_WIDTH / 2] = true;

        camera = new OrthographicCamera();
        camera.setToOrtho(false, MAP_WIDTH * TILE_SIZE, MAP_HEIGHT * TILE_SIZE);
        camera.position.set(MAP_WIDTH * TILE_SIZE / 2f, MAP_HEIGHT * TILE_SIZE / 2f, 0);
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            int tileX = (int) (Gdx.input.getX() / TILE_SIZE);
            int tileY = MAP_HEIGHT - 1 - (int) (Gdx.input.getY() / TILE_SIZE);
            revealTile(tileX, tileY);
        }

        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        for (int y = 0; y < MAP_HEIGHT; y++) {
            for (int x = 0; x < MAP_WIDTH; x++) {
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

    private void revealTile(int x, int y) {
        if (x < 0 || x >= MAP_WIDTH || y < 0 || y >= MAP_HEIGHT) {
            return;
        }
        if (visible[y][x]) {
            return;
        }
        if (hasVisibleNeighbor(x, y)) {
            visible[y][x] = true;
        }
    }

    private boolean hasVisibleNeighbor(int x, int y) {
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] dir : dirs) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            if (nx >= 0 && nx < MAP_WIDTH && ny >= 0 && ny < MAP_HEIGHT) {
                if (visible[ny][nx]) {
                    return true;
                }
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
