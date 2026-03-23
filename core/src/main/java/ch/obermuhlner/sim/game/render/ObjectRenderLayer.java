package ch.obermuhlner.sim.game.render;

import ch.obermuhlner.sim.game.Chunk;
import ch.obermuhlner.sim.game.GameConfig;
import ch.obermuhlner.sim.game.Tile;
import ch.obermuhlner.sim.game.World;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.OrthographicCamera;

import java.util.HashMap;
import java.util.Map;

public class ObjectRenderLayer implements RenderLayer {
    private static final int TILE_SIZE = 64;

    private final World world;
    private final boolean fogOfWar;
    private final GameConfig config;
    private final Map<Integer, Texture> textures = new HashMap<>();

    public ObjectRenderLayer(World world, boolean fogOfWar, GameConfig config) {
        this.world = world;
        this.fogOfWar = fogOfWar;
        this.config = config;
    }

    @Override
    public int getOrder() { return 10; }

    @Override
    public void loadAssets() {
        for (GameConfig.TerrainObjectConfig obj : config.getTerrainObjects()) {
            if (obj.image != null && !obj.image.isEmpty()) {
                textures.put(obj.id, new Texture(obj.image));
            }
        }
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
                int objectId = tile.objectId;
                
                Texture texture = textures.get(objectId);
                if (texture == null) continue;

                int tx = offsetCx + lx;
                int ty = offsetCy + ly;

                batch.draw(texture,
                    tx * TILE_SIZE, ty * TILE_SIZE,
                    TILE_SIZE, TILE_SIZE,
                    0, 0,
                    TILE_SIZE, TILE_SIZE,
                    false, false);
            }
        }
    }

    @Override
    public void dispose() {
        textures.values().forEach(Texture::dispose);
        textures.clear();
    }
}
