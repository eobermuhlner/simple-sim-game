package ch.obermuhlner.sim.game.render;

import ch.obermuhlner.sim.game.Chunk;
import ch.obermuhlner.sim.game.World;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;

import java.util.Comparator;

public class Renderer {
    private static final int TILE_SIZE = 64;

    private final World world;
    private final SpriteBatch batch;
    private final OrthographicCamera camera;
    private final Array<RenderLayer> layers = new Array<>();

    public Renderer(World world, SpriteBatch batch, OrthographicCamera camera) {
        this.world = world;
        this.batch = batch;
        this.camera = camera;
    }

    public void addLayer(RenderLayer layer) {
        layers.add(layer);
        layers.sort(Comparator.comparingInt(RenderLayer::getOrder));
        layer.loadAssets();
    }

    public void removeLayer(RenderLayer layer) {
        layers.removeValue(layer, true);
        layer.dispose();
    }

    public void render() {
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        int startX = (int) Math.floor((camera.position.x - camera.viewportWidth / 2 * camera.zoom) / TILE_SIZE);
        int startY = (int) Math.floor((camera.position.y - camera.viewportHeight / 2 * camera.zoom) / TILE_SIZE);
        int endX = startX + (int) (camera.viewportWidth / TILE_SIZE * camera.zoom) + 2;
        int endY = startY + (int) (camera.viewportHeight / TILE_SIZE * camera.zoom) + 2;

        world.forEachVisibleChunk(startX, startY, endX, endY, chunk -> {
            for (RenderLayer layer : layers) {
                layer.render(chunk, startX, startY, batch, camera);
            }
        });

        for (RenderLayer layer : layers) {
            layer.renderFull(batch, camera);
        }

        batch.end();
    }

    public void dispose() {
        for (RenderLayer layer : layers) {
            layer.dispose();
        }
        layers.clear();
    }
}
