package ch.obermuhlner.sim.game.mode;

import ch.obermuhlner.sim.game.World;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.OrthographicCamera;

public interface GameMode extends InputProcessor {
    String getName();
    void init(World world, OrthographicCamera camera);
    void update(float delta);
    void dispose();
}
