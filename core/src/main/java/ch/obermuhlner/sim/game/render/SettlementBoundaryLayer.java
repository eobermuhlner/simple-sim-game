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
        int radiusSq = radius * radius;

        batch.setColor(BORDER_COLOR);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                int tx = cx + dx;
                int ty = cy + dy;
                int distSq = dx * dx + dy * dy;

                if (distSq > radiusSq) continue;

                boolean isOutside = distSq > (radius - 1) * (radius - 1);
                if (!isOutside) continue;

                if (fogOfWar && !world.isRevealed(tx, ty)) continue;

                float screenX = tx * TILE_SIZE;
                float screenY = ty * TILE_SIZE;

                boolean nInside = isInside(tx, ty + 1, cx, cy, radiusSq);
                boolean sInside = isInside(tx, ty - 1, cx, cy, radiusSq);
                boolean eInside = isInside(tx + 1, ty, cx, cy, radiusSq);
                boolean wInside = isInside(tx - 1, ty, cx, cy, radiusSq);

                if (!nInside) {
                    batch.draw(lineTexture, screenX, screenY + TILE_SIZE - BORDER_THICKNESS/2, TILE_SIZE, BORDER_THICKNESS);
                }
                if (!sInside) {
                    batch.draw(lineTexture, screenX, screenY - BORDER_THICKNESS/2, TILE_SIZE, BORDER_THICKNESS);
                }
                if (!eInside) {
                    batch.draw(lineTexture, screenX + TILE_SIZE - BORDER_THICKNESS/2, screenY, BORDER_THICKNESS, TILE_SIZE);
                }
                if (!wInside) {
                    batch.draw(lineTexture, screenX - BORDER_THICKNESS/2, screenY, BORDER_THICKNESS, TILE_SIZE);
                }
            }
        }

        batch.setColor(Color.WHITE);
    }

    private boolean isInside(int tx, int ty, int cx, int cy, int radiusSq) {
        int dx = tx - cx;
        int dy = ty - cy;
        return dx * dx + dy * dy <= radiusSq;
    }

    @Override
    public void dispose() {
        if (lineTexture != null) {
            lineTexture.dispose();
        }
    }
}
