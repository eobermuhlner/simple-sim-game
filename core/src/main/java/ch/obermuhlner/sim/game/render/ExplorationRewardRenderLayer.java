package ch.obermuhlner.sim.game.render;

import ch.obermuhlner.sim.game.Chunk;
import ch.obermuhlner.sim.game.GameConfig;
import ch.obermuhlner.sim.game.Settlement;
import ch.obermuhlner.sim.game.SettlementLevel;
import ch.obermuhlner.sim.game.Tile;
import ch.obermuhlner.sim.game.World;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.util.HashMap;
import java.util.Map;

/**
 * Renders exploration reward tiles (caches and bonus tiles) with procedural textures.
 * Rewards are level-gated: only visible once any settlement reaches the required level.
 * Order 15 — renders after natural objects (10) and before roads (20+).
 */
public class ExplorationRewardRenderLayer implements RenderLayer {
    private static final int TILE_SIZE = 64;

    private final World world;
    private final boolean fogOfWar;
    private final GameConfig config;
    private final Map<Integer, Texture> textures = new HashMap<>();

    public ExplorationRewardRenderLayer(World world, boolean fogOfWar, GameConfig config) {
        this.world = world;
        this.fogOfWar = fogOfWar;
        this.config = config;
    }

    @Override
    public int getOrder() { return 15; }

    @Override
    public void loadAssets() {
        for (GameConfig.ExplorationRewardConfig reward : config.getExplorationRewards()) {
            if (reward.image != null && !reward.image.isEmpty()) {
                textures.put(reward.id, new Texture(Gdx.files.internal(reward.image)));
            } else {
                textures.put(reward.id, createRewardTexture(reward));
            }
        }
    }

    @Override
    public void render(Chunk chunk, int startTx, int startTy, SpriteBatch batch, OrthographicCamera camera) {
        SettlementLevel maxLevel = getMaxSettlementLevel();
        int size = world.getChunkSize();
        int offsetCx = chunk.cx * size;
        int offsetCy = chunk.cy * size;

        for (int ly = 0; ly < size; ly++) {
            for (int lx = 0; lx < size; lx++) {
                if (fogOfWar && !chunk.isRevealed(lx, ly)) continue;

                Tile tile = chunk.getTile(lx, ly);
                GameConfig.ExplorationRewardConfig reward = config.getExplorationReward(tile.objectId);
                if (reward == null) continue;

                if (!meetsLevelRequirement(maxLevel, reward.required_level)) continue;

                Texture texture = textures.get(tile.objectId);
                if (texture == null) continue;

                int tx = offsetCx + lx;
                int ty = offsetCy + ly;
                batch.draw(texture,
                    tx * TILE_SIZE, ty * TILE_SIZE,
                    TILE_SIZE, TILE_SIZE,
                    0, 0, TILE_SIZE, TILE_SIZE,
                    false, false);
            }
        }
    }

    private SettlementLevel getMaxSettlementLevel() {
        SettlementLevel max = SettlementLevel.VILLAGE;
        for (Settlement s : world.getSettlements()) {
            if (s.getLevel().ordinal() > max.ordinal()) {
                max = s.getLevel();
            }
        }
        return max;
    }

    private boolean meetsLevelRequirement(SettlementLevel maxLevel, String requiredLevel) {
        try {
            SettlementLevel required = SettlementLevel.valueOf(requiredLevel);
            return maxLevel.ordinal() >= required.ordinal();
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private Texture createRewardTexture(GameConfig.ExplorationRewardConfig reward) {
        Pixmap pixmap = new Pixmap(TILE_SIZE, TILE_SIZE, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0);
        pixmap.fill();

        Color bg = getBackgroundColor(reward);
        Color fg = getForegroundColor(reward);

        // Filled circle background
        pixmap.setColor(bg);
        pixmap.fillCircle(TILE_SIZE / 2, TILE_SIZE / 2, 24);

        // Border
        pixmap.setColor(fg);
        pixmap.drawCircle(TILE_SIZE / 2, TILE_SIZE / 2, 24);
        pixmap.drawCircle(TILE_SIZE / 2, TILE_SIZE / 2, 23);

        // Inner symbol
        pixmap.setColor(fg);
        int cx = TILE_SIZE / 2;
        int cy = TILE_SIZE / 2;
        if (reward.isOneTime()) {
            // Diamond shape — collectible cache
            for (int i = -8; i <= 8; i++) {
                int w = 8 - Math.abs(i);
                for (int j = -w; j <= w; j++) {
                    pixmap.drawPixel(cx + j, cy + i);
                }
            }
        } else {
            // Plus shape — permanent production bonus
            for (int i = -9; i <= 9; i++) {
                for (int t = -2; t <= 2; t++) {
                    pixmap.drawPixel(cx + t, cy + i);
                    pixmap.drawPixel(cx + i, cy + t);
                }
            }
        }

        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    private Color getBackgroundColor(GameConfig.ExplorationRewardConfig reward) {
        if (reward.isBonus()) {
            if (reward.bonus_production.containsKey("FOOD"))  return new Color(0.0f, 0.35f, 0.0f, 0.85f);
            if (reward.bonus_production.containsKey("STONE")) return new Color(0.3f, 0.3f, 0.3f, 0.85f);
            return new Color(0.2f, 0.2f, 0.2f, 0.85f);
        }
        if (reward.rewards.size() > 1)                        return new Color(0.3f, 0.0f, 0.4f, 0.85f); // Ancient Ruins
        if (reward.rewards.containsKey("WOOD"))               return new Color(0.45f, 0.25f, 0.0f, 0.85f);
        if (reward.rewards.containsKey("STONE"))              return new Color(0.35f, 0.35f, 0.35f, 0.85f);
        if (reward.rewards.containsKey("FOOD"))               return new Color(0.0f, 0.4f, 0.0f, 0.85f);
        return new Color(0.3f, 0.3f, 0.0f, 0.85f);
    }

    private Color getForegroundColor(GameConfig.ExplorationRewardConfig reward) {
        if (reward.isBonus()) {
            if (reward.bonus_production.containsKey("FOOD"))  return new Color(0.3f, 1.0f, 0.3f, 1f);
            if (reward.bonus_production.containsKey("STONE")) return new Color(0.8f, 0.8f, 0.85f, 1f);
            return new Color(0.9f, 0.9f, 0.9f, 1f);
        }
        if (reward.rewards.size() > 1)                        return new Color(0.9f, 0.7f, 1.0f, 1f); // Ancient Ruins
        if (reward.rewards.containsKey("WOOD"))               return new Color(1.0f, 0.75f, 0.2f, 1f);
        if (reward.rewards.containsKey("STONE"))              return new Color(0.85f, 0.85f, 0.9f, 1f);
        if (reward.rewards.containsKey("FOOD"))               return new Color(0.5f, 1.0f, 0.3f, 1f);
        return new Color(1.0f, 1.0f, 0.5f, 1f);
    }

    @Override
    public void dispose() {
        textures.values().forEach(Texture::dispose);
        textures.clear();
    }
}
