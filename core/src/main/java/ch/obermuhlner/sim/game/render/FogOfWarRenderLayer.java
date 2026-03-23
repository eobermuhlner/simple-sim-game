package ch.obermuhlner.sim.game.render;

import ch.obermuhlner.sim.game.Chunk;
import ch.obermuhlner.sim.game.GameConfig;
import ch.obermuhlner.sim.game.World;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.OrthographicCamera;

public class FogOfWarRenderLayer implements RenderLayer {
    private static final int TILE_SIZE = 64;
    private static final int TILESET_COLS = 4;
    private static final int FOG_TILE_INDEX = 15; // last tile in 4x4 tileset

    private final World world;
    private final GameConfig config;
    private Texture tileset;

    public FogOfWarRenderLayer(World world, GameConfig config) {
        this.world = world;
        this.config = config;
    }

    @Override
    public int getOrder() { return 100; }

    @Override
    public void loadAssets() {
        tileset = new Texture(config.getTerrainTileset());
    }

    @Override
    public void render(Chunk chunk, int startTx, int startTy, SpriteBatch batch, OrthographicCamera camera) {
        int size = world.getChunkSize();
        int offsetCx = chunk.cx * size;
        int offsetCy = chunk.cy * size;

        int srcX = FOG_TILE_INDEX % TILESET_COLS * TILE_SIZE;
        int srcY = FOG_TILE_INDEX / TILESET_COLS * TILE_SIZE;

        for (int ly = 0; ly < size; ly++) {
            for (int lx = 0; lx < size; lx++) {
                if (chunk.isRevealed(lx, ly)) continue;

                int tx = offsetCx + lx;
                int ty = offsetCy + ly;

                batch.draw(tileset,
                    tx * TILE_SIZE, ty * TILE_SIZE,
                    TILE_SIZE, TILE_SIZE,
                    srcX, srcY,
                    TILE_SIZE, TILE_SIZE,
                    false, false);
            }
        }
    }

    @Override
    public void dispose() {
        if (tileset != null) {
            tileset.dispose();
        }
    }
}
