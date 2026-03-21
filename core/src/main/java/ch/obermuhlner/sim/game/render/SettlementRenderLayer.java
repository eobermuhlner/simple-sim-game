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

import java.util.HashMap;
import java.util.Map;

public class SettlementRenderLayer implements RenderLayer {
    private static final int TILE_SIZE = 64;

    private final World world;
    private final boolean fogOfWar;
    private final Map<Integer, Texture> markerTextures = new HashMap<>();

    public SettlementRenderLayer(World world, boolean fogOfWar) {
        this.world = world;
        this.fogOfWar = fogOfWar;
    }

    @Override
    public int getOrder() { return 15; }

    @Override
    public void loadAssets() {
        for (SettlementLevel level : SettlementLevel.values()) {
            Pixmap pixmap = new Pixmap(16, 16, Pixmap.Format.RGBA8888);
            Color color = getLevelColor(level);
            pixmap.setColor(color);
            pixmap.fillCircle(8, 8, 7);
            pixmap.setColor(Color.BLACK);
            pixmap.drawCircle(8, 8, 7);
            Texture texture = new Texture(pixmap);
            pixmap.dispose();
            markerTextures.put(level.ordinal(), texture);
        }
    }

    private Color getLevelColor(SettlementLevel level) {
        switch (level) {
            case VILLAGE: return new Color(0.3f, 0.7f, 0.3f, 1f);
            case TOWN: return new Color(0.3f, 0.5f, 0.9f, 1f);
            case CITY: return new Color(0.8f, 0.6f, 0.2f, 1f);
            case METROPOLIS: return new Color(0.9f, 0.3f, 0.9f, 1f);
            default: return new Color(0.3f, 0.7f, 0.3f, 1f);
        }
    }

    @Override
    public void render(Chunk chunk, int startTx, int startTy, SpriteBatch batch, OrthographicCamera camera) {
        int size = world.getChunkSize();
        int offsetCx = chunk.cx * size;
        int offsetCy = chunk.cy * size;

        for (Settlement settlement : world.getSettlements()) {
            int tx = settlement.centerX;
            int ty = settlement.centerY;

            int cx = Math.floorDiv(tx, size);
            int cy = Math.floorDiv(ty, size);
            if (cx != chunk.cx || cy != chunk.cy) continue;

            if (fogOfWar && !world.isRevealed(tx, ty)) continue;

            Texture marker = markerTextures.get(settlement.getLevel().ordinal());
            if (marker != null) {
                float x = tx * TILE_SIZE + (TILE_SIZE - 16) / 2f;
                float y = ty * TILE_SIZE + (TILE_SIZE - 16) / 2f;
                batch.draw(marker, x, y, 16, 16);
            }
        }
    }

    @Override
    public void dispose() {
        markerTextures.values().forEach(Texture::dispose);
        markerTextures.clear();
    }
}
