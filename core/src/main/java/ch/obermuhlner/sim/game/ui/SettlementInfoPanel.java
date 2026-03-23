package ch.obermuhlner.sim.game.ui;

import ch.obermuhlner.sim.game.BuildingType;
import ch.obermuhlner.sim.game.ResourceType;
import ch.obermuhlner.sim.game.Settlement;
import ch.obermuhlner.sim.game.Specialization;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

public class SettlementInfoPanel {
    private static final int PANEL_WIDTH = 260;
    private static final int PANEL_HEIGHT = 348;
    private static final int PADDING = 15;
    private static final int LINE_HEIGHT = 18;

    private Texture backgroundTexture;
    private BitmapFont font;
    private BitmapFont titleFont;

    public SettlementInfoPanel() {
        createBackground();
        createFonts();
    }

    private void createBackground() {
        Pixmap pixmap = new Pixmap(PANEL_WIDTH, PANEL_HEIGHT, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0.1f, 0.1f, 0.15f, 0.9f));
        pixmap.fill();
        pixmap.setColor(new Color(0.3f, 0.3f, 0.4f, 1f));
        pixmap.drawRectangle(0, 0, PANEL_WIDTH, PANEL_HEIGHT);
        pixmap.setColor(new Color(0.2f, 0.2f, 0.3f, 1f));
        pixmap.fillRectangle(1, 1, PANEL_WIDTH - 2, 28);
        backgroundTexture = new Texture(pixmap);
        pixmap.dispose();
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

        generator.dispose();
    }

    public int getWidth() {
        return PANEL_WIDTH;
    }

    public int getHeight() {
        return PANEL_HEIGHT;
    }

    public void render(Settlement settlement, SpriteBatch batch, int screenWidth, int screenHeight) {
        float panelX = screenWidth - PANEL_WIDTH - 20;
        float panelY = screenHeight - PANEL_HEIGHT - 20;

        batch.draw(backgroundTexture, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT);

        float textX = panelX + PADDING;
        float textY = panelY + PANEL_HEIGHT - PADDING - 5;

        titleFont.draw(batch, settlement.name, textX, textY);
        textY -= LINE_HEIGHT + 8;

        font.draw(batch, "Level: " + settlement.getLevel().getDisplayName(), textX, textY);
        textY -= LINE_HEIGHT;

        font.draw(batch, "Population: " + settlement.population, textX, textY);
        textY -= LINE_HEIGHT;

        // Specialization
        Color specColor = getSpecializationColor(settlement.specialization);
        font.setColor(specColor);
        font.draw(batch, "Spec: " + settlement.specialization.displayName, textX, textY);
        font.setColor(Color.WHITE);
        textY -= LINE_HEIGHT;

        // Production modifiers (when specialized)
        if (settlement.specialization != Specialization.NONE) {
            font.setColor(new Color(0.7f, 0.9f, 0.7f, 1f));
            font.draw(batch, settlement.specialization.getProductionSummary(), textX, textY);
            font.setColor(Color.WHITE);
            textY -= LINE_HEIGHT;
        } else {
            textY -= LINE_HEIGHT; // keep spacing consistent
        }

        font.draw(batch, "Buildings: " + settlement.buildingIds.size() + "/" + settlement.getMaxBuildings(), textX, textY);
        textY -= LINE_HEIGHT;

        // Harbor indicator
        boolean hasHarbor = settlement.buildingIds.contains(BuildingType.HARBOR.getId());
        if (hasHarbor) {
            font.setColor(new Color(0.4f, 0.7f, 1f, 1f));
            font.draw(batch, "[Harbor] Sea trade enabled", textX, textY);
            font.setColor(Color.WHITE);
        }
        textY -= LINE_HEIGHT;

        font.draw(batch, "Position: (" + settlement.centerX + ", " + settlement.centerY + ")", textX, textY);
        textY -= LINE_HEIGHT + 5;

        // Status messages
        if (settlement.needsSpecializationChoice()) {
            font.setColor(new Color(1f, 0.5f, 0.1f, 1f));
            font.draw(batch, "Choose a specialization!", textX, textY);
            font.setColor(Color.WHITE);
        } else if (settlement.needsUpgrade()) {
            font.setColor(new Color(1f, 0.8f, 0.2f, 1f));
            font.draw(batch, "[Upgrade] available!", textX, textY);
            font.setColor(Color.WHITE);
        } else if (settlement.canRespecialize()) {
            font.setColor(new Color(0.6f, 0.7f, 1f, 1f));
            font.draw(batch, "[Re-spec] to change spec", textX, textY);
            font.setColor(Color.WHITE);
        } else {
            int toNext = settlement.getLevel().getMaxPopulation() - settlement.population;
            if (toNext < Integer.MAX_VALUE && toNext > 0) {
                font.draw(batch, "To next level: " + toNext + " pop", textX, textY);
            }
        }
        textY -= LINE_HEIGHT + 3;

        // Resources section
        font.setColor(new Color(0.5f, 0.5f, 0.7f, 1f));
        font.draw(batch, "--- Resources ---", textX, textY);
        font.setColor(Color.WHITE);
        textY -= LINE_HEIGHT;

        drawResource(batch, font, textX, textY, "Wood",  settlement.wood,  settlement.smoothedWoodProd,  settlement.woodPriceMult);
        textY -= LINE_HEIGHT;
        drawResource(batch, font, textX, textY, "Stone", settlement.stone, settlement.smoothedStoneProd, settlement.stonePriceMult);
        textY -= LINE_HEIGHT;
        drawResource(batch, font, textX, textY, "Food",  settlement.food,  settlement.smoothedFoodProd,  settlement.foodPriceMult);
        textY -= LINE_HEIGHT;
        drawResource(batch, font, textX, textY, "Goods", settlement.goods, settlement.smoothedGoodsProd, settlement.goodsPriceMult);
        textY -= LINE_HEIGHT;
        font.draw(batch, String.format("Gold:  %6.1f", settlement.gold), textX, textY);
    }

    private void drawResource(SpriteBatch batch, BitmapFont font,
                               float x, float y, String name,
                               float amount, float prod, float priceMult) {
        // Color-code by price: green=surplus (low price), red=shortage (high price)
        if (priceMult < 0.8f) {
            font.setColor(new Color(0.4f, 0.9f, 0.4f, 1f));  // surplus
        } else if (priceMult > 1.3f) {
            font.setColor(new Color(1f, 0.4f, 0.4f, 1f));    // shortage
        } else {
            font.setColor(Color.WHITE);
        }
        font.draw(batch, String.format("%-5s %5.0f +%-4.1f", name, amount, prod), x, y);
        font.setColor(Color.WHITE);
    }

    private Color getSpecializationColor(Specialization spec) {
        switch (spec) {
            case LOGGING_CAMP:    return new Color(0.7f, 0.5f, 0.2f, 1f);
            case MINING_TOWN:     return new Color(0.7f, 0.7f, 0.7f, 1f);
            case FARMING_VILLAGE: return new Color(0.3f, 0.9f, 0.3f, 1f);
            case TRADE_HUB:       return new Color(1.0f, 0.85f, 0.2f, 1f);
            default:              return new Color(0.6f, 0.6f, 0.6f, 1f);
        }
    }

    public void dispose() {
        if (backgroundTexture != null) backgroundTexture.dispose();
        if (font != null) font.dispose();
        if (titleFont != null) titleFont.dispose();
    }
}
