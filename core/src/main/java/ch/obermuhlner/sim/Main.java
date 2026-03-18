package ch.obermuhlner.sim;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;

public class Main extends ApplicationAdapter implements InputProcessor {
    private static final int TILE_SIZE = 64;
    private static final int MAP_WIDTH = 200;
    private static final int MAP_HEIGHT = 200;

    private static final float ZOOM_MIN = 0.25f;
    private static final float ZOOM_MAX = 4.0f;
    private static final float ZOOM_SPEED = 0.1f;

    private SpriteBatch batch;
    private Texture tileset;
    private OrthographicCamera camera;
    private boolean[][] visible;
    private int[][] map;

    private boolean fogOfWar = true;
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
                double n = octaveNoise(x * NOISE_SCALE, y * NOISE_SCALE, NOISE_OCTAVES, 0.5);
                int terrain = 1; // default to water
                if      (n < TERRAIN_THRESHOLDS[0]) terrain = 1; // water
                else if (n < TERRAIN_THRESHOLDS[1]) terrain = 2; // grass
                else if (n < TERRAIN_THRESHOLDS[2]) terrain = 3; // forest
                else if (n < TERRAIN_THRESHOLDS[3]) terrain = 4; // stone
                else                                 terrain = 5; // snow
                if (Double.isNaN(n) || Double.isInfinite(n) || terrain < 1 || terrain > 5) {
                    terrain = 1; // fallback for edge cases
                }
                map[y][x] = terrain;
            }
        }

        visible = new boolean[MAP_HEIGHT][MAP_WIDTH];
        visible[MAP_HEIGHT / 2][MAP_WIDTH / 2] = true;

        printTerrainDistribution(map);

        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(MAP_WIDTH * TILE_SIZE / 2f, MAP_HEIGHT * TILE_SIZE / 2f, 0);

        Gdx.input.setInputProcessor(this);
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

        int startX = Math.max(0, (int) ((camera.position.x - camera.viewportWidth / 2 * camera.zoom) / TILE_SIZE));
        int startY = Math.max(0, (int) ((camera.position.y - camera.viewportHeight / 2 * camera.zoom) / TILE_SIZE));
        int endX = Math.min(MAP_WIDTH, startX + (int) (camera.viewportWidth / TILE_SIZE * camera.zoom) + 2);
        int endY = Math.min(MAP_HEIGHT, startY + (int) (camera.viewportHeight / TILE_SIZE * camera.zoom) + 2);

        for (int y = startY; y < endY; y++) {
            for (int x = startX; x < endX; x++) {
                int tile = (!fogOfWar || visible[y][x]) ? map[y][x] : 0;
                if (tile == 0) continue;
                batch.draw(tileset,
                    x * TILE_SIZE, y * TILE_SIZE,
                    TILE_SIZE, TILE_SIZE,
                    tile % 4 * TILE_SIZE, tile / 4 * TILE_SIZE,
                    TILE_SIZE, TILE_SIZE,
                    false, false);
            }
        }

        batch.end();
    }

    private void handleInput() {
        int mx = Gdx.input.getX();
        int my = Gdx.input.getY();

        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            fogOfWar = !fogOfWar;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.PAGE_UP) || Gdx.input.isKeyPressed(Input.Keys.PAGE_UP)) {
            camera.zoom = Math.max(ZOOM_MIN, camera.zoom - ZOOM_SPEED);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.PAGE_DOWN) || Gdx.input.isKeyPressed(Input.Keys.PAGE_DOWN)) {
            camera.zoom = Math.min(ZOOM_MAX, camera.zoom + ZOOM_SPEED);
        }

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
        float halfW = camera.viewportWidth / 2 * camera.zoom;
        float halfH = camera.viewportHeight / 2 * camera.zoom;
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
    public boolean keyDown(int keycode) {
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
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        camera.zoom = MathUtils.clamp(camera.zoom - amountY * ZOOM_SPEED, ZOOM_MIN, ZOOM_MAX);
        return true;
    }

    // --- Perlin noise ---

    private static final int[] PERM = buildPerm(42);
    private static final double NOISE_SCALE = 0.04;
    private static final int NOISE_OCTAVES = 4;
    private static final double[] TERRAIN_THRESHOLDS = {0.45, 0.55, 0.6, 0.65};

    private static int[] buildPerm(long seed) {
        int[] p = new int[256];
        for (int i = 0; i < 256; i++) p[i] = i;
        java.util.Random rng = new java.util.Random(seed);
        for (int i = 255; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int tmp = p[i]; p[i] = p[j]; p[j] = tmp;
        }
        int[] perm = new int[512];
        for (int i = 0; i < 512; i++) perm[i] = p[i & 255];
        return perm;
    }

    private static double fade(double t) { return t * t * t * (t * (t * 6 - 15) + 10); }

    private static double grad(int hash, double x, double y) {
        int h = hash & 3;
        double u = h < 2 ? x : y, v = h < 2 ? y : x;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }

    private static double perlin(double x, double y) {
        int X = (int) Math.floor(x) & 255, Y = (int) Math.floor(y) & 255;
        x -= Math.floor(x); y -= Math.floor(y);
        double u = fade(x), v = fade(y);
        int a = PERM[X] + Y, b = PERM[X + 1] + Y;
        return (1 + lerp(v,
            lerp(u, grad(PERM[a],     x,     y), grad(PERM[b],     x - 1, y)),
            lerp(u, grad(PERM[a + 1], x,     y - 1), grad(PERM[b + 1], x - 1, y - 1)))) / 2;
    }

    private static void printTerrainDistribution(int[][] map) {
        int[] counts = new int[6];
        for (int y = 0; y < MAP_HEIGHT; y++) {
            for (int x = 0; x < MAP_WIDTH; x++) {
                counts[map[y][x]]++;
            }
        }
        int total = MAP_HEIGHT * MAP_WIDTH;
        System.out.println("Terrain distribution:");
        System.out.println("  0 (unknown): " + String.format("%5.1f%%", counts[0] * 100.0 / total));
        System.out.println("  1 (water):   " + String.format("%5.1f%%", counts[1] * 100.0 / total));
        System.out.println("  2 (grass):   " + String.format("%5.1f%%", counts[2] * 100.0 / total));
        System.out.println("  3 (forest): " + String.format("%5.1f%%", counts[3] * 100.0 / total));
        System.out.println("  4 (stone):  " + String.format("%5.1f%%", counts[4] * 100.0 / total));
        System.out.println("  5 (snow):   " + String.format("%5.1f%%", counts[5] * 100.0 / total));

        double n1 = octaveNoise(50.7, 50.3, 1, 0.5);
        double n2 = octaveNoise(51.2, 50.9, 1, 0.5);
        double n3 = octaveNoise(100.1, 100.5, 1, 0.5);
        double n4 = perlin(50.7, 50.3);
        double nCenter = octaveNoise(MAP_WIDTH / 2 * NOISE_SCALE, MAP_HEIGHT / 2 * NOISE_SCALE, NOISE_OCTAVES, 0.5);
    }

    private static double lerp(double t, double a, double b) { return a + t * (b - a); }

    private static double octaveNoise(double x, double y, int octaves, double persistence) {
        double val = 0, amp = 1, max = 0, freq = 1;
        for (int i = 0; i < octaves; i++) {
            val += perlin(x * freq, y * freq) * amp;
            max += amp;
            amp *= persistence;
            freq *= 2;
        }
        return val / max;
    }

    @Override
    public void dispose() {
        batch.dispose();
        tileset.dispose();
    }
}
