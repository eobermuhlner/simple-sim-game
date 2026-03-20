package ch.obermuhlner.sim;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;

import java.util.HashMap;

public class Main extends ApplicationAdapter implements InputProcessor {
    private static final int TILE_SIZE = 64;
    private static final int CHUNK_SIZE = 16;

    private static final float ZOOM_MIN = 0.25f;
    private static final float ZOOM_MAX = 4.0f;
    private static final float ZOOM_SPEED = 0.1f;

    private SpriteBatch batch;
    private Texture tileset;
    private OrthographicCamera camera;
    private HashMap<Long, Chunk> chunks = new HashMap<>();

    private boolean fogOfWar = true;
    private boolean mouseDown = false;
    private float totalDrag = 0;
    private int lastMouseX, lastMouseY;

    private static class Chunk {
        int cx, cy;
        int[][] terrain = new int[CHUNK_SIZE][CHUNK_SIZE];
        boolean[][] fog = new boolean[CHUNK_SIZE][CHUNK_SIZE];
        boolean dirty = false;
    }

    @Override
    public void create() {
        batch = new SpriteBatch();
        tileset = new Texture("64x64/map.png");

        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(TILE_SIZE / 2f, TILE_SIZE / 2f, 0);

        Chunk startChunk = getChunk(0, 0);
        if (!startChunk.fog[0][0]) {
            startChunk.fog[0][0] = true;
            saveFog(startChunk);
        }

        Gdx.input.setInputProcessor(this);
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);

        handleInput();

        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        int startX = (int) Math.floor((camera.position.x - camera.viewportWidth / 2 * camera.zoom) / TILE_SIZE);
        int startY = (int) Math.floor((camera.position.y - camera.viewportHeight / 2 * camera.zoom) / TILE_SIZE);
        int endX = startX + (int) (camera.viewportWidth / TILE_SIZE * camera.zoom) + 2;
        int endY = startY + (int) (camera.viewportHeight / TILE_SIZE * camera.zoom) + 2;

        for (int ty = startY; ty < endY; ty++) {
            for (int tx = startX; tx < endX; tx++) {
                int cx = Math.floorDiv(tx, CHUNK_SIZE);
                int cy = Math.floorDiv(ty, CHUNK_SIZE);
                int lx = Math.floorMod(tx, CHUNK_SIZE);
                int ly = Math.floorMod(ty, CHUNK_SIZE);
                Chunk chunk = getChunk(cx, cy);
                if (fogOfWar && !chunk.fog[ly][lx]) continue;
                int tile = chunk.terrain[ly][lx];
                batch.draw(tileset,
                    tx * TILE_SIZE, ty * TILE_SIZE,
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
            lastMouseX = mx;
            lastMouseY = my;
        }

        if (mouseDown && !Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            mouseDown = false;
            if (totalDrag < 5) {
                Vector3 worldPos = camera.unproject(new Vector3(mx, my, 0));
                int tileX = (int) Math.floor(worldPos.x / TILE_SIZE);
                int tileY = (int) Math.floor(worldPos.y / TILE_SIZE);
                revealTile(tileX, tileY);
            }
        }
    }

    private void revealTile(int tx, int ty) {
        int cx = Math.floorDiv(tx, CHUNK_SIZE);
        int cy = Math.floorDiv(ty, CHUNK_SIZE);
        int lx = Math.floorMod(tx, CHUNK_SIZE);
        int ly = Math.floorMod(ty, CHUNK_SIZE);
        Chunk chunk = getChunk(cx, cy);
        if (chunk.fog[ly][lx]) return;
        if (hasVisibleNeighbor(tx, ty)) {
            chunk.fog[ly][lx] = true;
            chunk.dirty = true;
            saveFog(chunk);
        }
    }

    private boolean hasVisibleNeighbor(int tx, int ty) {
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] dir : dirs) {
            int nx = tx + dir[0];
            int ny = ty + dir[1];
            int cx = Math.floorDiv(nx, CHUNK_SIZE);
            int cy = Math.floorDiv(ny, CHUNK_SIZE);
            int lx = Math.floorMod(nx, CHUNK_SIZE);
            int ly = Math.floorMod(ny, CHUNK_SIZE);
            if (getChunk(cx, cy).fog[ly][lx]) return true;
        }
        return false;
    }

    private Chunk getChunk(int cx, int cy) {
        long key = ((long) cx << 32) | (cy & 0xFFFFFFFFL);
        Chunk chunk = chunks.get(key);
        if (chunk == null) {
            chunk = new Chunk();
            chunk.cx = cx;
            chunk.cy = cy;
            generateTerrain(chunk);
            loadFog(chunk);
            chunks.put(key, chunk);
        }
        return chunk;
    }

    private void generateTerrain(Chunk chunk) {
        for (int ly = 0; ly < CHUNK_SIZE; ly++) {
            for (int lx = 0; lx < CHUNK_SIZE; lx++) {
                int tx = chunk.cx * CHUNK_SIZE + lx;
                int ty = chunk.cy * CHUNK_SIZE + ly;
                double n = octaveNoise(tx * NOISE_SCALE, ty * NOISE_SCALE, NOISE_OCTAVES, 0.5);
                int terrain;
                if      (n < TERRAIN_THRESHOLDS[0]) terrain = 1;
                else if (n < TERRAIN_THRESHOLDS[1]) terrain = 2;
                else if (n < TERRAIN_THRESHOLDS[2]) terrain = 3;
                else if (n < TERRAIN_THRESHOLDS[3]) terrain = 4;
                else                                terrain = 5;
                chunk.terrain[ly][lx] = terrain;
            }
        }
    }

    private void loadFog(Chunk chunk) {
        FileHandle file = Gdx.files.local("chunks/" + chunk.cx + "_" + chunk.cy + ".fow");
        if (!file.exists()) return;
        byte[] data = file.readBytes();
        if (data.length < CHUNK_SIZE * CHUNK_SIZE / 8) return;
        for (int i = 0; i < CHUNK_SIZE * CHUNK_SIZE; i++) {
            chunk.fog[i / CHUNK_SIZE][i % CHUNK_SIZE] = ((data[i / 8] >> (i % 8)) & 1) == 1;
        }
    }

    private void saveFog(Chunk chunk) {
        byte[] data = new byte[CHUNK_SIZE * CHUNK_SIZE / 8];
        for (int i = 0; i < CHUNK_SIZE * CHUNK_SIZE; i++) {
            if (chunk.fog[i / CHUNK_SIZE][i % CHUNK_SIZE]) {
                data[i / 8] |= (byte) (1 << (i % 8));
            }
        }
        Gdx.files.local("chunks/" + chunk.cx + "_" + chunk.cy + ".fow").writeBytes(data, false);
        chunk.dirty = false;
    }

    @Override
    public boolean keyDown(int keycode) { return false; }
    @Override
    public boolean keyUp(int keycode) { return false; }
    @Override
    public boolean keyTyped(char character) { return false; }
    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) { return false; }
    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) { return false; }
    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) { return false; }
    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) { return false; }
    @Override
    public boolean mouseMoved(int screenX, int screenY) { return false; }

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
        for (Chunk chunk : chunks.values()) {
            if (chunk.dirty) saveFog(chunk);
        }
        batch.dispose();
        tileset.dispose();
    }
}
