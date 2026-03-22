package ch.obermuhlner.sim.game;

import ch.obermuhlner.sim.game.mode.GameMode;

public interface GameController {
    void setGameMode(GameMode mode);
    World getWorld();
    void handleClick(int screenX, int screenY);
}
