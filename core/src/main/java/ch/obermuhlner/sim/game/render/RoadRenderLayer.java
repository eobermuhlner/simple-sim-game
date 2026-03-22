package ch.obermuhlner.sim.game.render;

import ch.obermuhlner.sim.game.Chunk;
import ch.obermuhlner.sim.game.Tile;
import ch.obermuhlner.sim.game.World;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.OrthographicCamera;

/**
 * Renders road tiles using 4 base images + rotation to cover all 16 connection patterns.
 *
 * Connection bitmask: bit0=North(+Y), bit1=South(-Y), bit2=East(+X), bit3=West(-X)
 *
 * Base images and what connections they show:
 *   road-dirt-ns.png   → N+S (bits: 0b0011 = 3)
 *   road-dirt-se.png   → S+E (bits: 0b0110 = 6)
 *   road-dirt-nse.png  → N+S+E (bits: 0b0111 = 7)
 *   road-dirt-nswe.png → N+S+W+E (bits: 0b1111 = 15)
 *
 * Rotation convention (libGDX counterclockwise):
 *   Each 90° CCW rotation maps: N→W, E→N, S→E, W→S
 */
public class RoadRenderLayer implements RenderLayer {
    private static final int TILE_SIZE = 64;

    // Connection bit constants (matching World constants)
    private static final int N = 1;
    private static final int S = 2;
    private static final int E = 4;
    private static final int W = 8;

    private final World world;
    private final boolean fogOfWar;

    private Texture texNS;    // straight north-south
    private Texture texSE;    // corner south-east
    private Texture texNSE;   // T-junction north-south-east
    private Texture texNSWE;  // cross

    // For each of the 16 connection patterns (0–15): [texture index, rotation in degrees]
    // texture index: 0=NS, 1=SE, 2=NSE, 3=NSWE
    // Rotation: 0, 90, 180, 270 (CCW)
    private static final int[][] CONNECTION_MAP = new int[16][2];

    static {
        // 0 = isolated road — show as a dot (use NS straight, no rotation)
        CONNECTION_MAP[0]  = new int[]{0, 0};
        // 1 = N only dead-end — NS rotated? show straight pointing N
        CONNECTION_MAP[N]  = new int[]{0, 0};   // N
        // 2 = S only dead-end
        CONNECTION_MAP[S]  = new int[]{0, 0};   // S
        // 3 = N+S straight
        CONNECTION_MAP[N|S] = new int[]{0, 0};
        // 4 = E only dead-end
        CONNECTION_MAP[E]  = new int[]{0, 90};  // NS rotated 90° = EW
        // 5 = N+E corner
        // SE rotated 90° CCW: S→E, E→N → NE
        CONNECTION_MAP[N|E] = new int[]{1, 90};
        // 6 = S+E corner
        CONNECTION_MAP[S|E] = new int[]{1, 0};
        // 7 = N+S+E T-junction
        CONNECTION_MAP[N|S|E] = new int[]{2, 0};
        // 8 = W only dead-end
        CONNECTION_MAP[W]  = new int[]{0, 90};  // EW
        // 9 = N+W corner
        // SE rotated 180°: S→N, E→W → NW
        CONNECTION_MAP[N|W] = new int[]{1, 180};
        // 10 = S+W corner
        // SE rotated 270° CCW: S→W, E→S → SW
        CONNECTION_MAP[S|W] = new int[]{1, 270};
        // 11 = N+S+W T-junction
        // NSE rotated 180°: N→S, S→N, E→W → NSW
        CONNECTION_MAP[N|S|W] = new int[]{2, 180};
        // 12 = E+W straight
        CONNECTION_MAP[E|W] = new int[]{0, 90};
        // 13 = N+E+W T-junction
        // NSE rotated 90° CCW: N→W, S→E, E→N → NEW
        CONNECTION_MAP[N|E|W] = new int[]{2, 90};
        // 14 = S+E+W T-junction
        // NSE rotated 270° CCW: N→E, S→W, E→S → SEW
        CONNECTION_MAP[S|E|W] = new int[]{2, 270};
        // 15 = all directions cross
        CONNECTION_MAP[N|S|E|W] = new int[]{3, 0};
    }

    public RoadRenderLayer(World world, boolean fogOfWar) {
        this.world = world;
        this.fogOfWar = fogOfWar;
    }

    @Override
    public int getOrder() { return 25; } // after terrain+objects (0-10), before settlements (30)

    @Override
    public void loadAssets() {
        texNS   = new Texture("64x64/single-tiles/road-dirt-ns.png");
        texSE   = new Texture("64x64/single-tiles/road-dirt-se.png");
        texNSE  = new Texture("64x64/single-tiles/road-dirt-nse.png");
        texNSWE = new Texture("64x64/single-tiles/road-dirt-nswe.png");
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
                if (tile.roadType == 0) continue;

                int tx = offsetCx + lx;
                int ty = offsetCy + ly;

                int conn = tile.roadConnection & 0xF;
                int[] mapping = CONNECTION_MAP[conn];
                Texture tex = getTexture(mapping[0]);
                float rotation = mapping[1];

                float worldX = tx * TILE_SIZE;
                float worldY = ty * TILE_SIZE;

                batch.draw(tex,
                    worldX, worldY,          // position
                    TILE_SIZE / 2f, TILE_SIZE / 2f, // origin (center for rotation)
                    TILE_SIZE, TILE_SIZE,    // size
                    1f, 1f,                  // scale
                    rotation,                // rotation (CCW degrees)
                    0, 0,                    // src x, y
                    tex.getWidth(), tex.getHeight(), // src size
                    false, false);           // flip
            }
        }
    }

    private Texture getTexture(int index) {
        switch (index) {
            case 0: return texNS;
            case 1: return texSE;
            case 2: return texNSE;
            case 3: return texNSWE;
            default: return texNS;
        }
    }

    @Override
    public void dispose() {
        if (texNS != null) texNS.dispose();
        if (texSE != null) texSE.dispose();
        if (texNSE != null) texNSE.dispose();
        if (texNSWE != null) texNSWE.dispose();
    }
}
