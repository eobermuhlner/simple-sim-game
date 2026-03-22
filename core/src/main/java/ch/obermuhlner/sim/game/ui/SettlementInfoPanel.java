package ch.obermuhlner.sim.game.ui;

import ch.obermuhlner.sim.game.Settlement;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

public class SettlementInfoPanel {
    private static final int PANEL_WIDTH = 260;
    private static final int PANEL_HEIGHT = 180;
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
        
        font.draw(batch, "Buildings: " + settlement.buildingIds.size() + "/" + settlement.getMaxBuildings(), textX, textY);
        textY -= LINE_HEIGHT;
        
        font.draw(batch, "Position: (" + settlement.centerX + ", " + settlement.centerY + ")", textX, textY);
        textY -= LINE_HEIGHT + 5;
        
        if (settlement.needsUpgrade()) {
            font.setColor(new Color(1f, 0.8f, 0.2f, 1f));
            font.draw(batch, "[U] Upgrade Available!", textX, textY);
            font.setColor(Color.WHITE);
        } else {
            int toNext = settlement.getLevel().getMaxPopulation() - settlement.population;
            if (toNext < Integer.MAX_VALUE && toNext > 0) {
                font.draw(batch, "To next level: " + toNext + " pop", textX, textY);
            }
        }
    }
    
    public void dispose() {
        if (backgroundTexture != null) backgroundTexture.dispose();
        if (font != null) font.dispose();
        if (titleFont != null) titleFont.dispose();
    }
}
