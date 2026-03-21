package ch.obermuhlner.sim.game.render;

import ch.obermuhlner.sim.game.Chunk;
import ch.obermuhlner.sim.game.World;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.OrthographicCamera;

public class FogOfWarRenderLayer implements RenderLayer {
    private static final int TILE_SIZE = 64;
    private static final Color FOG_COLOR = new Color(0.1f, 0.1f, 0.15f, 0.85f);

    private final World world;
    private Texture whitePixel;

    public FogOfWarRenderLayer(World world) {
        this.world = world;
    }

    @Override
    public int getOrder() { return 100; }

    @Override
    public void loadAssets() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        whitePixel = new Texture(pixmap);
        pixmap.dispose();
    }

    @Override
    public void render(Chunk chunk, int startTx, int startTy, SpriteBatch batch, OrthographicCamera camera) {
        int size = world.getChunkSize();
        int offsetCx = chunk.cx * size;
        int offsetCy = chunk.cy * size;

        batch.setColor(FOG_COLOR);

        for (int ly = 0; ly < size; ly++) {
            for (int lx = 0; lx < size; lx++) {
                if (chunk.isRevealed(lx, ly)) continue;

                int tx = offsetCx + lx;
                int ty = offsetCy + ly;

                batch.draw(whitePixel,
                    tx * TILE_SIZE, ty * TILE_SIZE,
                    TILE_SIZE, TILE_SIZE);
            }
        }

        batch.setColor(Color.WHITE);
    }

    @Override
    public void dispose() {
        if (whitePixel != null) {
            whitePixel.dispose();
        }
    }
}
