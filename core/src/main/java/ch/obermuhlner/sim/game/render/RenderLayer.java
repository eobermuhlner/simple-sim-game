package ch.obermuhlner.sim.game.render;

import ch.obermuhlner.sim.game.Chunk;
import ch.obermuhlner.sim.game.World;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.OrthographicCamera;

public interface RenderLayer {
    int getOrder();
    void render(Chunk chunk, int startTx, int startTy, SpriteBatch batch, OrthographicCamera camera);
    void loadAssets();
    void dispose();

    default void renderFull(SpriteBatch batch, OrthographicCamera camera) {
    }
}
