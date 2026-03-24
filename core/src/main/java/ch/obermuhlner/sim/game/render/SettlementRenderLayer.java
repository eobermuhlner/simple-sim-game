package ch.obermuhlner.sim.game.render;

import ch.obermuhlner.sim.game.Chunk;
import ch.obermuhlner.sim.game.GameConfig;
import ch.obermuhlner.sim.game.Settlement;
import ch.obermuhlner.sim.game.SettlementLevel;
import ch.obermuhlner.sim.game.Specialization;
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
    private final GameConfig config;
    private final Map<Integer, Texture> markerTextures = new HashMap<>();

    public SettlementRenderLayer(World world, boolean fogOfWar, GameConfig config) {
        this.world = world;
        this.fogOfWar = fogOfWar;
        this.config = config;
    }

    @Override
    public int getOrder() { return 15; }

    @Override
    public void loadAssets() {
    }

    private Texture getMarkerTexture(Settlement settlement) {
        int key = settlement.getLevel().ordinal() * 10 + settlement.specialization.ordinal();
        Texture existing = markerTextures.get(key);
        if (existing == null) {
            existing = createMarkerTexture(settlement.getLevel(), settlement.specialization);
            markerTextures.put(key, existing);
        }
        return existing;
    }

    private Texture createMarkerTexture(SettlementLevel level, Specialization specialization) {
        String imagePath = config.getSettlementImage(level.name());
        if (imagePath != null && !imagePath.isEmpty()) {
            try {
                return new Texture(imagePath);
            } catch (Exception e) {
            }
        }
        return createDefaultMarker(level, specialization);
    }

    private Texture createDefaultMarker(SettlementLevel level, Specialization specialization) {
        Pixmap pixmap = new Pixmap(16, 16, Pixmap.Format.RGBA8888);

        pixmap.setColor(getLevelColor(level));
        pixmap.fillCircle(8, 8, 7);

        if (specialization != Specialization.NONE) {
            pixmap.setColor(getSpecializationColor(specialization));
        } else {
            pixmap.setColor(Color.BLACK);
        }
        pixmap.drawCircle(8, 8, 7);

        if (specialization != Specialization.NONE) {
            pixmap.setColor(getSpecializationColor(specialization));
            pixmap.fillCircle(8, 8, 3);
        }

        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    private Color getLevelColor(SettlementLevel level) {
        switch (level.getId()) {
            case "VILLAGE":    return new Color(0.3f, 0.7f, 0.3f, 1f);
            case "TOWN":       return new Color(0.3f, 0.5f, 0.9f, 1f);
            case "CITY":       return new Color(0.8f, 0.6f, 0.2f, 1f);
            case "METROPOLIS": return new Color(0.9f, 0.3f, 0.9f, 1f);
            default:           return new Color(0.3f, 0.7f, 0.3f, 1f);
        }
    }

    private Color getSpecializationColor(Specialization spec) {
        switch (spec) {
            case LOGGING_CAMP:    return new Color(0.6f, 0.4f, 0.1f, 1f);
            case MINING_TOWN:     return new Color(0.7f, 0.7f, 0.7f, 1f);
            case FARMING_VILLAGE: return new Color(0.2f, 0.9f, 0.2f, 1f);
            case TRADE_HUB:       return new Color(1.0f, 0.85f, 0.1f, 1f);
            default:              return Color.BLACK;
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

            Texture marker = getMarkerTexture(settlement);
            float x = tx * TILE_SIZE;
            float y = ty * TILE_SIZE;
            batch.draw(marker, x, y, TILE_SIZE, TILE_SIZE);
        }
    }

    @Override
    public void dispose() {
        markerTextures.values().forEach(Texture::dispose);
        markerTextures.clear();
    }
}
