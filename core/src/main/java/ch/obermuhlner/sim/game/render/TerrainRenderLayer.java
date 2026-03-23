package ch.obermuhlner.sim.game.render;

import ch.obermuhlner.sim.game.Chunk;
import ch.obermuhlner.sim.game.GameConfig;
import ch.obermuhlner.sim.game.TerrainType;
import ch.obermuhlner.sim.game.Tile;
import ch.obermuhlner.sim.game.World;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.OrthographicCamera;

public class TerrainRenderLayer implements RenderLayer {
    private static final int TILE_SIZE = 64;

    private final World world;
    private final boolean fogOfWar;
    private final GameConfig config;
    private Texture tileset;

    public TerrainRenderLayer(World world, boolean fogOfWar, GameConfig config) {
        this.world = world;
        this.fogOfWar = fogOfWar;
        this.config = config;
    }

    @Override
    public int getOrder() { return 0; }

    @Override
    public void loadAssets() {
        tileset = new Texture(config.getTerrainTileset());
    }

    @Override
    public void render(Chunk chunk, int startTx, int startTy, SpriteBatch batch, OrthographicCamera camera) {
        int size = world.getChunkSize();
        int offsetCx = chunk.cx * size;
        int offsetCy = chunk.cy * size;

        for (int ly = 0; ly < size; ly++) {
            for (int lx = 0; lx < size; lx++) {
                if (fogOfWar && !chunk.isRevealed(lx, ly)) continue;

                Tile tile = chunk.getTile(lx, ly);
                int tx = offsetCx + lx;
                int ty = offsetCy + ly;

                int tileIndex = config.getTerrainTileIndex(tile.terrain);
                int srcX = tileIndex % 4 * TILE_SIZE;
                int srcY = tileIndex / 4 * TILE_SIZE;

                boolean isShallowSea = tile.terrain == TerrainType.SHALLOW_SEA;
                if (isShallowSea) batch.setColor(0.55f, 0.78f, 0.95f, 1f);

                batch.draw(tileset,
                    tx * TILE_SIZE, ty * TILE_SIZE,
                    TILE_SIZE, TILE_SIZE,
                    srcX, srcY,
                    TILE_SIZE, TILE_SIZE,
                    false, false);

                if (isShallowSea) batch.setColor(Color.WHITE);
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
