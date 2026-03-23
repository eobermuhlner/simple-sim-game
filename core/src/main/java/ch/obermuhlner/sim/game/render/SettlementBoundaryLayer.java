package ch.obermuhlner.sim.game.render;

import ch.obermuhlner.sim.game.Chunk;
import ch.obermuhlner.sim.game.Settlement;
import ch.obermuhlner.sim.game.SettlementLevel;
import ch.obermuhlner.sim.game.Specialization;
import ch.obermuhlner.sim.game.World;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.OrthographicCamera;

public class SettlementBoundaryLayer implements RenderLayer {
    private static final int TILE_SIZE = 64;

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

        Color borderColor = getBorderColor(level, settlement.specialization);
        batch.setColor(borderColor);

        for (int angle = 0; angle < 360; angle += 2) {
            double rad = Math.toRadians(angle);
            double outerRad = rad + Math.toRadians(2);
            
            int x1 = cx + (int)Math.round(radius * Math.cos(rad));
            int y1 = cy + (int)Math.round(radius * Math.sin(rad));
            int x2 = cx + (int)Math.round(radius * Math.cos(outerRad));
            int y2 = cy + (int)Math.round(radius * Math.sin(outerRad));

            if (fogOfWar) {
                if (!world.isRevealed(x1, y1) || !world.isRevealed(x2, y2)) continue;
            }

            float screenX1 = x1 * TILE_SIZE;
            float screenY1 = y1 * TILE_SIZE;
            float screenX2 = x2 * TILE_SIZE;
            float screenY2 = y2 * TILE_SIZE;

            drawThickLine(batch, screenX1 + TILE_SIZE/2f, screenY1 + TILE_SIZE/2f,
                          screenX2 + TILE_SIZE/2f, screenY2 + TILE_SIZE/2f, 3f);
        }

        batch.setColor(Color.WHITE);
    }

    private Color getBorderColor(SettlementLevel level, Specialization spec) {
        switch (level) {
            case VILLAGE:    return new Color(0.3f, 0.7f, 0.3f, 0.7f);
            case TOWN:       return new Color(0.3f, 0.5f, 0.9f, 0.7f);
            case CITY:       return new Color(0.8f, 0.6f, 0.2f, 0.7f);
            case METROPOLIS: return new Color(0.9f, 0.3f, 0.9f, 0.7f);
            default:         return new Color(0.3f, 0.7f, 0.3f, 0.7f);
        }
    }

    private void drawThickLine(SpriteBatch batch, float x1, float y1, float x2, float y2, float thickness) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float)Math.sqrt(dx*dx + dy*dy);
        if (len < 0.001f) return;
        
        float nx = -dy / len * thickness / 2f;
        float ny = dx / len * thickness / 2f;

        float x1n = x1 + nx;
        float y1n = y1 + ny;
        float x1s = x1 - nx;
        float y1s = y1 - ny;
        float x2n = x2 + nx;
        float y2n = y2 + ny;
        float x2s = x2 - nx;
        float y2s = y2 - ny;

        float centerX = (x1n + x1s + x2n + x2s) / 4;
        float centerY = (y1n + y1s + y2n + y2s) / 4;
        
        batch.draw(lineTexture, centerX - len/2, centerY - thickness/2, len, thickness);
    }

    @Override
    public void dispose() {
        if (lineTexture != null) {
            lineTexture.dispose();
        }
    }
}
