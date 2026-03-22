package ch.obermuhlner.sim.game.render;

import ch.obermuhlner.sim.game.Caravan;
import ch.obermuhlner.sim.game.Chunk;
import ch.obermuhlner.sim.game.World;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.TimeUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Renders caravans as animated trader sprites.
 * 6-frame walking animation with 4 directional variants (south/east/west/north).
 * Drawn per-chunk so fog of war (order 100) naturally covers hidden caravans.
 */
public class CaravanRenderLayer implements RenderLayer {
    private static final int TILE_SIZE = 64;
    private static final int SPRITE_SIZE = 48;
    private static final int FRAME_COUNT = 6;
    private static final long FRAME_DURATION_MS = 120;  // ms per frame → ~8 fps

    private static final String BASE_PATH = "64x64/characters/trader-walking/animations/walking/";
    private static final String[] DIRECTIONS = {"south", "east", "west", "north"};

    private final World world;

    // frames[direction][frameIndex]
    private final Map<String, Texture[]> frames = new HashMap<>();

    public CaravanRenderLayer(World world) {
        this.world = world;
    }

    @Override
    public int getOrder() { return 50; }  // after roads (25), before fog (100)

    @Override
    public void loadAssets() {
        for (String dir : DIRECTIONS) {
            Texture[] dirFrames = new Texture[FRAME_COUNT];
            for (int i = 0; i < FRAME_COUNT; i++) {
                String path = BASE_PATH + dir + "/frame_00" + i + ".png";
                dirFrames[i] = new Texture(Gdx.files.internal(path));
            }
            frames.put(dir, dirFrames);
        }
    }

    @Override
    public void render(Chunk chunk, int startTx, int startTy, SpriteBatch batch, OrthographicCamera camera) {
        int chunkSize = world.getChunkSize();
        int frameIndex = (int)((TimeUtils.millis() / FRAME_DURATION_MS) % FRAME_COUNT);

        for (Caravan caravan : world.getCaravans()) {
            int tx = caravan.getTileX();
            int ty = caravan.getTileY();

            // Only draw if this tile belongs to the current chunk
            if (Math.floorDiv(tx, chunkSize) != chunk.cx) continue;
            if (Math.floorDiv(ty, chunkSize) != chunk.cy) continue;

            // Don't draw in fog
            if (!world.isRevealed(tx, ty)) continue;

            String dir = caravan.getDirection();
            Texture[] dirFrames = frames.get(dir);
            if (dirFrames == null) dirFrames = frames.get("south");

            Texture frame = dirFrames[frameIndex];
            float wx = caravan.getWorldX() - SPRITE_SIZE / 2f;
            float wy = caravan.getWorldY() - SPRITE_SIZE / 2f;
            batch.draw(frame, wx, wy, SPRITE_SIZE, SPRITE_SIZE);
        }
    }

    @Override
    public void dispose() {
        for (Texture[] dirFrames : frames.values()) {
            for (Texture t : dirFrames) {
                if (t != null) t.dispose();
            }
        }
        frames.clear();
    }
}
