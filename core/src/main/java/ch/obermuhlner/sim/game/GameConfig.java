package ch.obermuhlner.sim.game;

import com.badlogic.gdx.Gdx;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads game configuration from assets/application.yml.
 * Falls back to zero costs if the file is missing or malformed.
 */
public class GameConfig {
    private final Map<String, Float> roadCosts     = new HashMap<>();
    private final Map<String, Float> buildingCosts = new HashMap<>();

    public GameConfig() {
        load();
    }

    @SuppressWarnings("unchecked")
    private void load() {
        try (InputStream is = Gdx.files.internal("application.yml").read()) {
            Yaml yaml = new Yaml();
            Map<String, Object> root = yaml.load(is);
            if (root == null) return;

            Map<String, Object> roads = (Map<String, Object>) root.get("roads");
            if (roads != null) {
                for (Map.Entry<String, Object> e : roads.entrySet()) {
                    roadCosts.put(e.getKey().toUpperCase(), ((Number) e.getValue()).floatValue());
                }
            }

            Map<String, Object> buildings = (Map<String, Object>) root.get("buildings");
            if (buildings != null) {
                for (Map.Entry<String, Object> e : buildings.entrySet()) {
                    buildingCosts.put(e.getKey().toUpperCase(), ((Number) e.getValue()).floatValue());
                }
            }
        } catch (Exception e) {
            Gdx.app.log("GameConfig", "Failed to load application.yml: " + e.getMessage());
        }
    }

    public float getRoadCost(RoadType type) {
        return roadCosts.getOrDefault(type.name(), 0f);
    }

    public float getBuildingCost(BuildingType type) {
        return buildingCosts.getOrDefault(type.name(), 0f);
    }
}
