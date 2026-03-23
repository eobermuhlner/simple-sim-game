package ch.obermuhlner.sim.game;

import java.util.Random;

public class TerrainGenerator {
    private final long seed;
    private final int[] perm;
    private final GameConfig config;

    public TerrainGenerator(long seed, GameConfig config) {
        this.seed = seed;
        this.config = config;
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
        double noiseScale = config.getNoiseScale();
        int noiseOctaves = config.getNoiseOctaves();
        double persistence = config.getPersistence();

        for (int ly = 0; ly < size; ly++) {
            for (int lx = 0; lx < size; lx++) {
                int tx = chunk.cx * size + lx;
                int ty = chunk.cy * size + ly;

                double n = octaveNoise(tx * noiseScale, ty * noiseScale, noiseOctaves, persistence);
                TerrainType terrain = getTerrainFromNoise(n);

                int objectId = generateNaturalObjects(random, terrain);

                Tile tile = new Tile(terrain, objectId);
                chunk.setTile(lx, ly, tile);
            }
        }
    }

    protected int generateNaturalObjects(Random random, TerrainType terrain) {
        String terrainName = terrain.name();
        for (GameConfig.TerrainObjectConfig obj : config.getTerrainObjects()) {
            Float prob = obj.spawn.get(terrainName);
            if (prob != null && random.nextFloat() < prob) {
                return obj.id;
            }
        }
        return TileObjectRegistry.NONE;
    }

    protected TerrainType getTerrainFromNoise(double n) {
        if (n < config.getTerrainThreshold("water"))  return TerrainType.WATER;
        if (n < config.getTerrainThreshold("grass"))  return TerrainType.GRASS;
        if (n < config.getTerrainThreshold("forest")) return TerrainType.FOREST;
        if (n < config.getTerrainThreshold("stone"))  return TerrainType.STONE;
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
