package ch.obermuhlner.sim.game.render;

import ch.obermuhlner.sim.game.Caravan;
import ch.obermuhlner.sim.game.Chunk;
import ch.obermuhlner.sim.game.World;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Renders caravans as small colored circles on road tiles.
 * Drawn per-chunk so fog of war (order 100) naturally covers hidden caravans.
 */
public class CaravanRenderLayer implements RenderLayer {
    private static final int TILE_SIZE = 64;
    private static final int SPRITE_SIZE = 20;

    private final World world;
    private Texture caravanTexture;

    public CaravanRenderLayer(World world) {
        this.world = world;
    }

    @Override
    public int getOrder() { return 50; }  // after roads (25), before fog (100)

    @Override
    public void loadAssets() {
        Pixmap p = new Pixmap(SPRITE_SIZE, SPRITE_SIZE, Pixmap.Format.RGBA8888);
        p.setColor(0f, 0f, 0f, 0f);
        p.fill();
        // Orange filled circle with dark border
        p.setColor(1f, 0.6f, 0.1f, 1f);
        p.fillCircle(SPRITE_SIZE / 2, SPRITE_SIZE / 2, SPRITE_SIZE / 2 - 1);
        p.setColor(0.4f, 0.2f, 0f, 1f);
        p.drawCircle(SPRITE_SIZE / 2, SPRITE_SIZE / 2, SPRITE_SIZE / 2 - 1);
        caravanTexture = new Texture(p);
        p.dispose();
    }

    @Override
    public void render(Chunk chunk, int startTx, int startTy, SpriteBatch batch, OrthographicCamera camera) {
        int chunkSize = world.getChunkSize();

        for (Caravan caravan : world.getCaravans()) {
            int tx = caravan.getTileX();
            int ty = caravan.getTileY();

            // Only draw if this tile belongs to the current chunk
            int caravanCx = Math.floorDiv(tx, chunkSize);
            int caravanCy = Math.floorDiv(ty, chunkSize);
            if (caravanCx != chunk.cx || caravanCy != chunk.cy) continue;

            // Don't draw in fog
            if (!world.isRevealed(tx, ty)) continue;

            float wx = caravan.getWorldX() - SPRITE_SIZE / 2f;
            float wy = caravan.getWorldY() - SPRITE_SIZE / 2f;
            batch.draw(caravanTexture, wx, wy, SPRITE_SIZE, SPRITE_SIZE);
        }
    }

    @Override
    public void dispose() {
        if (caravanTexture != null) caravanTexture.dispose();
    }
}
