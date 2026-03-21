package ch.obermuhlner.sim.game;

import java.util.Random;

public class TerrainGenerator {
    private static final double NOISE_SCALE = 0.04;
    private static final int NOISE_OCTAVES = 4;
    private static final double PERSISTENCE = 0.5;
    private static final double[] TERRAIN_THRESHOLDS = {0.45, 0.55, 0.6, 0.65};

    private final long seed;
    private final int[] perm;

    public TerrainGenerator(long seed) {
        this.seed = seed;
        this.perm = buildPermutationTable(seed);
    }

    private int[] buildPermutationTable(long seed) {
        int[] p = new int[256];
        for (int i = 0; i < 256; i++) p[i] = i;
        Random rng = new Random(seed);
        for (int i = 255; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int tmp = p[i]; p[i] = p[j]; p[j] = tmp;
        }
        int[] perm = new int[512];
        for (int i = 0; i < 512; i++) perm[i] = p[i & 255];
        return perm;
    }

    public void generate(Chunk chunk) {
        Random random = new Random(chunk.cx * 31L + chunk.cy * 17L + seed);
        int size = chunk.tiles.length;

        for (int ly = 0; ly < size; ly++) {
            for (int lx = 0; lx < size; lx++) {
                int tx = chunk.cx * size + lx;
                int ty = chunk.cy * size + ly;

                double n = octaveNoise(tx * NOISE_SCALE, ty * NOISE_SCALE, NOISE_OCTAVES, PERSISTENCE);
                TerrainType terrain = getTerrainFromNoise(n);

                int objectId = TileObjectRegistry.NONE;
                objectId = generateNaturalObjects(random, terrain);

                Tile tile = new Tile(terrain, objectId);
                chunk.setTile(lx, ly, tile);
            }
        }
    }

    protected int generateNaturalObjects(Random random, TerrainType terrain) {
        switch (terrain) {
            case GRASS:
                if (random.nextFloat() < 0.5f) {
                    return random.nextFloat() < 0.2f ? 
                        TileObjectRegistry.TREE_SMALL : 
                        TileObjectRegistry.TREE_LARGE;
                } else if (random.nextFloat() < 0.1f) {
                    return TileObjectRegistry.BOULDER_LARGE;
                } else if (random.nextFloat() < 0.1f) {
                    return TileObjectRegistry.BOULDER_SMALL;
                }
                break;
            case FOREST:
                if (random.nextFloat() < 0.2f) {
                    return TileObjectRegistry.BOULDER_LARGE;
                }
                break;
            case STONE:
                if (random.nextFloat() < 0.4f) {
                    return TileObjectRegistry.BOULDER_SMALL;
                } else if (random.nextFloat() < 0.2f) {
                    return TileObjectRegistry.BOULDER_LARGE;
                }
                break;
            case SNOW:
                if (random.nextFloat() < 0.2f) {
                    return TileObjectRegistry.BOULDER_SNOW;
                }
                break;
            default:
                break;
        }
        return TileObjectRegistry.NONE;
    }

    protected TerrainType getTerrainFromNoise(double n) {
        if (n < TERRAIN_THRESHOLDS[0]) return TerrainType.WATER;
        if (n < TERRAIN_THRESHOLDS[1]) return TerrainType.GRASS;
        if (n < TERRAIN_THRESHOLDS[2]) return TerrainType.FOREST;
        if (n < TERRAIN_THRESHOLDS[3]) return TerrainType.STONE;
        return TerrainType.SNOW;
    }

    private double octaveNoise(double x, double y, int octaves, double persistence) {
        double val = 0, amp = 1, max = 0, freq = 1;
        for (int i = 0; i < octaves; i++) {
            val += perlin(x * freq, y * freq) * amp;
            max += amp;
            amp *= persistence;
            freq *= 2;
        }
        return val / max;
    }

    private double perlin(double x, double y) {
        int X = (int) Math.floor(x) & 255;
        int Y = (int) Math.floor(y) & 255;
        x -= Math.floor(x);
        y -= Math.floor(y);
        double u = fade(x);
        double v = fade(y);
        int a = perm[X] + Y;
        int b = perm[X + 1] + Y;
        return (1 + lerp(v,
            lerp(u, grad(perm[a], x, y), grad(perm[b], x - 1, y)),
            lerp(u, grad(perm[a + 1], x, y - 1), grad(perm[b + 1], x - 1, y - 1)))) / 2;
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private static double grad(int hash, double x, double y) {
        int h = hash & 3;
        double u = h < 2 ? x : y;
        double v = h < 2 ? y : x;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }
}
