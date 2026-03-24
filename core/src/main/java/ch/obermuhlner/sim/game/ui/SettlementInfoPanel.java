package ch.obermuhlner.sim.game.ui;

import ch.obermuhlner.sim.game.BuildingType;
import ch.obermuhlner.sim.game.GameConfig;
import ch.obermuhlner.sim.game.RoadType;
import ch.obermuhlner.sim.game.Settlement;
import ch.obermuhlner.sim.game.Specialization;
import ch.obermuhlner.sim.game.Tile;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.Align;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettlementInfoPanel {
    private static final int PANEL_WIDTH = 260;
    private static final int PADDING = 15;
    private static final int LINE_HEIGHT = 18;
    private static final int IMAGE_SIZE = 64;
    private static final int HEADER_HEIGHT = 28;
    private static final int SECTION_HEADER_HEIGHT = 16;
    private static final int SECTION_GAP = 8;

    private Texture bodyTexture;
    private Texture headerTexture;
    private Texture dividerTexture;
    private BitmapFont font;
    private BitmapFont titleFont;
    private BitmapFont sectionFont;
    private GlyphLayout glyphLayout;

    private final GameConfig config;
    private Texture terrainTileset;
    private final Map<String, Texture> textureCache = new HashMap<>();

    public SettlementInfoPanel(GameConfig config) {
        this.config = config;
        createBackground();
        createFonts();
        glyphLayout = new GlyphLayout();
        terrainTileset = new Texture(Gdx.files.internal(config.getTerrainTileset()));
    }

    private void createBackground() {
        Pixmap body = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        body.setColor(new Color(0.1f, 0.1f, 0.15f, 0.9f));
        body.fill();
        bodyTexture = new Texture(body);
        body.dispose();

        Pixmap header = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        header.setColor(new Color(0.2f, 0.2f, 0.3f, 1f));
        header.fill();
        headerTexture = new Texture(header);
        header.dispose();

        Pixmap divider = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        divider.setColor(new Color(0.3f, 0.3f, 0.4f, 0.8f));
        divider.fill();
        dividerTexture = new Texture(divider);
        divider.dispose();
    }

    private void createFonts() {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/JetBrainsMono-Regular.ttf"));

        FreeTypeFontGenerator.FreeTypeFontParameter fontParams = new FreeTypeFontGenerator.FreeTypeFontParameter();
        fontParams.size = 12;
        fontParams.color = Color.WHITE;
        fontParams.borderColor = new Color(0, 0, 0, 0.5f);
        fontParams.borderWidth = 0.5f;
        font = generator.generateFont(fontParams);

        FreeTypeFontGenerator.FreeTypeFontParameter titleParams = new FreeTypeFontGenerator.FreeTypeFontParameter();
        titleParams.size = 14;
        titleParams.color = new Color(0.9f, 0.9f, 1f, 1f);
        titleParams.borderColor = new Color(0, 0, 0, 0.5f);
        titleParams.borderWidth = 0.5f;
        titleFont = generator.generateFont(titleParams);

        FreeTypeFontGenerator.FreeTypeFontParameter sectionParams = new FreeTypeFontGenerator.FreeTypeFontParameter();
        sectionParams.size = 11;
        sectionParams.color = new Color(0.6f, 0.7f, 0.9f, 1f);
        sectionParams.borderColor = new Color(0, 0, 0, 0.5f);
        sectionParams.borderWidth = 0.5f;
        sectionFont = generator.generateFont(sectionParams);

        generator.dispose();
    }

    private void drawBackground(SpriteBatch batch, float panelX, float panelY, float panelW, float panelH) {
        batch.draw(bodyTexture, panelX, panelY, panelW, panelH);
        batch.draw(headerTexture, panelX, panelY + panelH - HEADER_HEIGHT, panelW, HEADER_HEIGHT);
    }

    public int getWidth() {
        return PANEL_WIDTH;
    }

    // ---- Tile render (settlement may be null for non-center tiles) ----

    public void render(Tile tile, int tileX, int tileY, Settlement settlement, SpriteBatch batch, int screenWidth, int screenHeight) {
        float textWidth = PANEL_WIDTH - 2 * PADDING;
        List<Chapter> chapters = new ArrayList<>();

        // Terrain chapter
        int tileIndex = config.getTerrainTileIndex(tile.terrain);
        int srcX = (tileIndex % 4) * 64;
        int srcY = (tileIndex / 4) * 64;
        TextureRegion terrainRegion = new TextureRegion(terrainTileset, srcX, srcY, 64, 64);
        List<String> terrainStats = new ArrayList<>();
        terrainStats.add("Terrain: " + tile.terrain.name());
        terrainStats.add("Traversable: " + (tile.terrain.isTraversable() ? "Yes" : "No"));
        terrainStats.add("Buildable:   " + (tile.terrain.isBuildable() ? "Yes" : "No"));
        chapters.add(new Chapter("TERRAIN", terrainRegion, null, terrainStats,
            config.getTerrainDescription(tile.terrain), new Color(0.5f, 0.7f, 1f, 1f)));

        // Object chapter
        if (tile.hasObject()) {
            int objId = tile.objectId;
            List<String> objStats = new ArrayList<>();
            String objDesc;
            Texture objTexture = null;
            String sectionLabel;

            GameConfig.ExplorationRewardConfig erc = config.getExplorationReward(objId);
            if (erc != null) {
                sectionLabel = "EXPLORATION REWARD";
                objStats.add("Name: " + tile.getObject().getName());
                objStats.add("Type: " + (erc.isOneTime() ? "One-time reward" : "Ongoing bonus"));
                objStats.add("Requires: " + erc.required_level);
                for (Map.Entry<String, Float> e : erc.rewards.entrySet()) {
                    objStats.add("Reward: +" + e.getValue().intValue() + " " + e.getKey());
                }
                for (Map.Entry<String, Float> e : erc.bonus_production.entrySet()) {
                    objStats.add("Bonus: +" + e.getValue() + " " + e.getKey() + "/tick");
                }
                objDesc = config.getExplorationRewardDescription(objId);
                if (erc.image != null && !erc.image.isEmpty()) {
                    objTexture = loadOrGetTexture(erc.image);
                }
            } else {
                sectionLabel = "OBJECT";
                objStats.add("Name: " + tile.getObject().getName());
                objStats.add("Walkable: " + (tile.getObject().isWalkable() ? "Yes" : "No"));
                float destroyCost = config.getTerrainObjectDestroyCost(objId);
                if (destroyCost > 0) objStats.add("Clear cost: " + (int) destroyCost + " Gold");
                for (Map.Entry<String, Float> e : config.getTerrainObjectHarvest(objId).entrySet()) {
                    objStats.add("Yields: " + e.getValue().intValue() + " " + e.getKey());
                }
                objDesc = config.getTerrainObjectDescription(objId);
                for (GameConfig.TerrainObjectConfig toc : config.getTerrainObjects()) {
                    if (toc.id == objId && toc.image != null && !toc.image.isEmpty()) {
                        objTexture = loadOrGetTexture(toc.image);
                        break;
                    }
                }
            }
            chapters.add(new Chapter(sectionLabel, null, objTexture, objStats, objDesc,
                new Color(0.5f, 0.9f, 0.5f, 1f)));
        }

        // Building chapter
        if (tile.hasBuilding()) {
            BuildingType bt = BuildingType.fromId(tile.buildingId);
            if (bt != null) {
                List<String> bStats = new ArrayList<>();
                bStats.add("Name: " + bt.getDisplayName());
                bStats.add("Cost: " + (int) config.getBuildingCost(bt) + " Gold");
                bStats.add("Population: " + config.getBuildingPopulationCapacity(bt));
                String imgPath = config.getBuildingTexturePath(bt);
                Texture bTex = (imgPath != null && !imgPath.isEmpty()) ? loadOrGetTexture(imgPath) : null;
                chapters.add(new Chapter("BUILDING", null, bTex, bStats,
                    config.getBuildingDescription(bt), new Color(0.9f, 0.7f, 0.5f, 1f)));
            }
        }

        // Road chapter
        if (tile.hasRoad()) {
            RoadType rt = RoadType.fromId(tile.roadType);
            if (rt != null) {
                List<String> rStats = new ArrayList<>();
                rStats.add("Type: " + rt.getDisplayName());
                rStats.add("Cost: " + (int) config.getRoadCost(rt) + " Gold");
                rStats.add("Speed: " + config.getRoadSpeedMultiplier(rt) + "x");
                rStats.add("Capacity: " + config.getRoadCapacity(rt));
                chapters.add(new Chapter("ROAD", null, null, rStats,
                    config.getRoadDescription(rt), new Color(0.8f, 0.8f, 0.6f, 1f)));
            }
        }

        // Settlement chapter
        if (settlement != null) {
            String imgPath = config.getSettlementImage(settlement.getLevel().name());
            Texture sTex = (imgPath != null && !imgPath.isEmpty()) ? loadOrGetTexture(imgPath) : null;
            List<String> sStats = new ArrayList<>();
            sStats.add("Name: " + settlement.name);
            sStats.add("Level: " + settlement.getLevel().getDisplayName());
            sStats.add("Population: " + settlement.population);
            sStats.add("Spec: " + settlement.specialization.displayName);
            if (settlement.specialization != Specialization.NONE) {
                sStats.add("  " + settlement.specialization.getProductionSummary());
            }
            sStats.add("Buildings: " + settlement.buildingIds.size() + "/" + settlement.getMaxBuildings());
            if (settlement.buildingIds.contains(BuildingType.HARBOR.getId())) {
                sStats.add("Harbor: Sea trade enabled");
            }
            sStats.add(String.format("Wood:  %5.0f +%.1f", settlement.wood,  settlement.smoothedWoodProd));
            sStats.add(String.format("Stone: %5.0f +%.1f", settlement.stone, settlement.smoothedStoneProd));
            sStats.add(String.format("Food:  %5.0f +%.1f", settlement.food,  settlement.smoothedFoodProd));
            sStats.add(String.format("Goods: %5.0f +%.1f", settlement.goods, settlement.smoothedGoodsProd));
            sStats.add(String.format("Gold:  %7.1f", settlement.gold));
            String sDesc = "";
            if (settlement.needsSpecializationChoice()) {
                sDesc = "Choose a specialization!";
            } else if (settlement.needsUpgrade()) {
                sDesc = "[Upgrade] available!";
            } else if (settlement.canRespecialize()) {
                sDesc = "[Re-spec] to change specialization";
            } else {
                int toNext = settlement.getMaxPopulation() - settlement.population;
                if (toNext < Integer.MAX_VALUE && toNext > 0) {
                    sDesc = "To next level: " + toNext + " more population";
                }
            }
            chapters.add(new Chapter("SETTLEMENT", null, sTex, sStats, sDesc,
                new Color(1f, 0.85f, 0.3f, 1f)));
        }

        // Calculate total panel height
        float totalH = HEADER_HEIGHT + PADDING + LINE_HEIGHT + 8; // header bar + position line
        for (Chapter ch : chapters) {
            totalH += calcChapterHeight(ch, textWidth);
        }
        totalH += PADDING;

        float panelH = Math.max(totalH, 120);
        float panelX = screenWidth - PANEL_WIDTH - 20;
        float panelY = screenHeight - panelH - 20;

        drawBackground(batch, panelX, panelY, PANEL_WIDTH, panelH);

        float textX = panelX + PADDING;
        float curY = panelY + panelH - PADDING - 5;

        titleFont.draw(batch, "Tile Info", textX, curY);
        curY -= LINE_HEIGHT + 8;

        font.setColor(new Color(0.5f, 0.7f, 1f, 1f));
        font.draw(batch, "Position: (" + tileX + ", " + tileY + ")", textX, curY);
        font.setColor(Color.WHITE);
        curY -= LINE_HEIGHT + 4;

        for (Chapter ch : chapters) {
            curY = drawChapter(batch, panelX, textX, curY, ch, textWidth);
        }
    }

    private float drawChapter(SpriteBatch batch, float panelX, float textX, float curY,
                               Chapter ch, float textWidth) {
        // Divider line
        batch.draw(dividerTexture, panelX + PADDING, curY - 2, PANEL_WIDTH - 2 * PADDING, 1);
        curY -= SECTION_GAP;

        // Section header
        sectionFont.setColor(ch.color);
        sectionFont.draw(batch, ch.title, textX, curY);
        sectionFont.setColor(Color.WHITE);
        curY -= SECTION_HEADER_HEIGHT;

        // Image
        if (ch.region != null) {
            batch.draw(ch.region, textX, curY - IMAGE_SIZE, IMAGE_SIZE, IMAGE_SIZE);
        } else if (ch.texture != null) {
            batch.draw(ch.texture, textX, curY - IMAGE_SIZE, IMAGE_SIZE, IMAGE_SIZE);
        }
        curY -= IMAGE_SIZE + 4;

        // Stats
        font.setColor(Color.WHITE);
        for (String stat : ch.stats) {
            font.draw(batch, stat, textX, curY);
            curY -= LINE_HEIGHT;
        }

        // Description
        if (ch.description != null && !ch.description.isEmpty()) {
            curY -= 2;
            font.setColor(new Color(0.7f, 0.7f, 0.7f, 1f));
            font.draw(batch, ch.description, textX, curY, textWidth, Align.left, true);
            glyphLayout.setText(font, ch.description, Color.WHITE, textWidth, Align.left, true);
            curY -= glyphLayout.height;
            font.setColor(Color.WHITE);
        }

        curY -= 4;
        return curY;
    }

    private float calcChapterHeight(Chapter ch, float textWidth) {
        float h = SECTION_GAP + SECTION_HEADER_HEIGHT + IMAGE_SIZE + 4;
        h += ch.stats.size() * LINE_HEIGHT;
        if (ch.description != null && !ch.description.isEmpty()) {
            glyphLayout.setText(font, ch.description, Color.WHITE, textWidth, Align.left, true);
            h += 2 + glyphLayout.height;
        }
        h += 4;
        return h;
    }

    private Texture loadOrGetTexture(String path) {
        Texture t = textureCache.get(path);
        if (t == null) {
            t = new Texture(Gdx.files.internal(path));
            textureCache.put(path, t);
        }
        return t;
    }

    // ---- Internal data class ----

    private static class Chapter {
        final String title;
        final TextureRegion region;
        final Texture texture;
        final List<String> stats;
        final String description;
        final Color color;

        Chapter(String title, TextureRegion region, Texture texture,
                List<String> stats, String description, Color color) {
            this.title = title;
            this.region = region;
            this.texture = texture;
            this.stats = stats;
            this.description = description;
            this.color = color;
        }
    }

    public void dispose() {
        if (bodyTexture != null) bodyTexture.dispose();
        if (headerTexture != null) headerTexture.dispose();
        if (dividerTexture != null) dividerTexture.dispose();
        if (font != null) font.dispose();
        if (titleFont != null) titleFont.dispose();
        if (sectionFont != null) sectionFont.dispose();
        if (terrainTileset != null) terrainTileset.dispose();
        for (Texture t : textureCache.values()) t.dispose();
        textureCache.clear();
    }
}
