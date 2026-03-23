package ch.obermuhlner.sim.game.render;

import ch.obermuhlner.sim.game.Chunk;
import ch.obermuhlner.sim.game.Settlement;
import ch.obermuhlner.sim.game.SettlementLevel;
import ch.obermuhlner.sim.game.World;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.OrthographicCamera;

public class SettlementBoundaryLayer implements RenderLayer {
    private static final int TILE_SIZE = 64;
    private static final Color BORDER_COLOR = new Color(0.6f, 0.2f, 0.9f, 1f);
    private static final float BORDER_THICKNESS = 3f;

    private final World world;
    private final boolean fogOfWar;
    private Texture lineTexture;

    public SettlementBoundaryLayer(World world, boolean fogOfWar) {
        this.world = world;
        this.fogOfWar = fogOfWar;
    }

    @Override
    public int getOrder() { return 20; }

    @Override
    public void loadAssets() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        lineTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    @Override
    public void render(Chunk chunk, int startTx, int startTy, SpriteBatch batch, OrthographicCamera camera) {
    }

    @Override
    public void renderFull(SpriteBatch batch, OrthographicCamera camera) {
        for (Settlement settlement : world.getSettlements()) {
            renderSettlementBoundary(settlement, batch, camera);
        }
    }

    private void renderSettlementBoundary(Settlement settlement, SpriteBatch batch, OrthographicCamera camera) {
        SettlementLevel level = settlement.getLevel();
        int radius = level.getRadius();
        int cx = settlement.centerX;
        int cy = settlement.centerY;

        batch.setColor(BORDER_COLOR);

        for (int angle = 0; angle < 360; angle += 4) {
            double rad = Math.toRadians(angle);
            int tx = cx + (int)Math.round(radius * Math.cos(rad));
            int ty = cy + (int)Math.round(radius * Math.sin(rad));

            if (fogOfWar && !world.isRevealed(tx, ty)) continue;

            float screenX = tx * TILE_SIZE;
            float screenY = ty * TILE_SIZE;

            double dx = Math.cos(rad);
            double dy = Math.sin(rad);

            if (dx > 0.5) {
                batch.draw(lineTexture, screenX + TILE_SIZE - BORDER_THICKNESS/2, screenY, BORDER_THICKNESS, TILE_SIZE);
            } else if (dx < -0.5) {
                batch.draw(lineTexture, screenX - BORDER_THICKNESS/2, screenY, BORDER_THICKNESS, TILE_SIZE);
            } else if (dy > 0.5) {
                batch.draw(lineTexture, screenX, screenY + TILE_SIZE - BORDER_THICKNESS/2, TILE_SIZE, BORDER_THICKNESS);
            } else if (dy < -0.5) {
                batch.draw(lineTexture, screenX, screenY - BORDER_THICKNESS/2, TILE_SIZE, BORDER_THICKNESS);
            }
        }

        batch.setColor(Color.WHITE);
    }

    @Override
    public void dispose() {
        if (lineTexture != null) {
            lineTexture.dispose();
        }
    }
}
