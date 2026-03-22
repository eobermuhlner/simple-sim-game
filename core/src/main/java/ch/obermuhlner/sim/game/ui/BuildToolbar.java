package ch.obermuhlner.sim.game.ui;

import ch.obermuhlner.sim.game.BuildingType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

import java.util.HashMap;
import java.util.Map;

public class BuildToolbar {
    private static final int BUTTON_WIDTH = 80;
    private static final int BUTTON_HEIGHT = 80;
    private static final int BUTTON_PADDING = 8;
    private static final int MAX_COLUMNS = 6;
    private static final int ICON_SIZE = 48;
    private static final int FONT_SIZE = 10;
    
    private static final int TOOLBAR_HEIGHT = BUTTON_HEIGHT + BUTTON_PADDING * 3;
    
    private java.util.List<BuildingButton> buttons = new java.util.ArrayList<>();
    private Texture backgroundTexture;
    private BitmapFont font;
    private int selectedIndex = -1;
    private Map<Integer, Texture> buildingTextures = new HashMap<>();
    
    public BuildToolbar() {
        createBackground();
        createFonts();
        loadBuildingTextures();
        createButtons();
    }
    
    private void loadBuildingTextures() {
        for (BuildingType type : BuildingType.values()) {
            try {
                Texture tex = new Texture(type.getTexturePath());
                buildingTextures.put(type.getId(), tex);
            } catch (Exception e) {
            }
        }
    }
    
    private void createBackground() {
        int width = BUTTON_WIDTH * MAX_COLUMNS + BUTTON_PADDING * (MAX_COLUMNS + 1);
        int height = TOOLBAR_HEIGHT;
        
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0.15f, 0.15f, 0.2f, 0.95f));
        pixmap.fill();
        pixmap.setColor(new Color(0.4f, 0.4f, 0.5f, 1f));
        pixmap.drawRectangle(0, 0, width, height);
        backgroundTexture = new Texture(pixmap);
        pixmap.dispose();
    }
    
    private void createFonts() {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/JetBrainsMono-Regular.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter params = new FreeTypeFontGenerator.FreeTypeFontParameter();
        params.size = FONT_SIZE;
        params.color = Color.WHITE;
        params.borderColor = new Color(0, 0, 0, 0.5f);
        params.borderWidth = 0.5f;
        font = generator.generateFont(params);
        generator.dispose();
    }
    
    private void createButtons() {
        addButton(0, "[S] New Settlement", -1);
        
        int col = 1;
        for (BuildingType type : BuildingType.values()) {
            if (col >= MAX_COLUMNS) break;
            addButton(col, type.getDisplayName(), type.getId());
            col++;
        }
    }
    
    private void addButton(int column, String label, int buildingType) {
        BuildingButton button = new BuildingButton();
        button.column = column;
        button.label = label;
        button.buildingType = buildingType;
        button.iconTexture = buildingTextures.get(buildingType);
        
        Pixmap normalPixmap = new Pixmap(BUTTON_WIDTH, BUTTON_HEIGHT, Pixmap.Format.RGBA8888);
        normalPixmap.setColor(new Color(0.2f, 0.2f, 0.3f, 1f));
        normalPixmap.fill();
        normalPixmap.setColor(new Color(0.4f, 0.4f, 0.5f, 1f));
        normalPixmap.drawRectangle(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT);
        button.normalTexture = new Texture(normalPixmap);
        normalPixmap.dispose();
        
        Pixmap selectedPixmap = new Pixmap(BUTTON_WIDTH, BUTTON_HEIGHT, Pixmap.Format.RGBA8888);
        selectedPixmap.setColor(new Color(0.4f, 0.6f, 0.8f, 1f));
        selectedPixmap.fill();
        selectedPixmap.setColor(new Color(0.6f, 0.8f, 1f, 1f));
        selectedPixmap.drawRectangle(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT);
        button.selectedTexture = new Texture(selectedPixmap);
        selectedPixmap.dispose();
        
        Pixmap hoverPixmap = new Pixmap(BUTTON_WIDTH, BUTTON_HEIGHT, Pixmap.Format.RGBA8888);
        hoverPixmap.setColor(new Color(0.3f, 0.3f, 0.4f, 1f));
        hoverPixmap.fill();
        hoverPixmap.setColor(new Color(0.5f, 0.5f, 0.6f, 1f));
        hoverPixmap.drawRectangle(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT);
        button.hoverTexture = new Texture(hoverPixmap);
        hoverPixmap.dispose();
        
        buttons.add(button);
    }
    
    public void select(int index) {
        selectedIndex = index;
    }
    
    public int getSelectedBuildingType() {
        if (selectedIndex >= 0 && selectedIndex < buttons.size()) {
            return buttons.get(selectedIndex).buildingType;
        }
        return -1;
    }
    
    public int getWidth() {
        return BUTTON_WIDTH * MAX_COLUMNS + BUTTON_PADDING * (MAX_COLUMNS + 1);
    }
    
    public int getHeight() {
        return TOOLBAR_HEIGHT;
    }
    
    public void render(SpriteBatch batch, int screenWidth, int screenHeight) {
        int toolbarWidth = getWidth();
        
        float panelX = (screenWidth - toolbarWidth) / 2f;
        float panelY = screenHeight - TOOLBAR_HEIGHT - 20;
        
        batch.draw(backgroundTexture, panelX, panelY, toolbarWidth, TOOLBAR_HEIGHT);
        
        for (int i = 0; i < buttons.size(); i++) {
            BuildingButton button = buttons.get(i);
            
            Texture tex;
            if (i == selectedIndex) {
                tex = button.selectedTexture;
            } else {
                tex = button.normalTexture;
            }
            
            float bx = panelX + BUTTON_PADDING + button.column * (BUTTON_WIDTH + BUTTON_PADDING);
            float by = panelY + BUTTON_PADDING;
            
            batch.draw(tex, bx, by, BUTTON_WIDTH, BUTTON_HEIGHT);
            
            if (button.iconTexture != null) {
                float iconX = bx + (BUTTON_WIDTH - ICON_SIZE) / 2f;
                float iconY = by + BUTTON_HEIGHT - ICON_SIZE - 6;
                batch.draw(button.iconTexture, iconX, iconY, ICON_SIZE, ICON_SIZE);
            }
            
            font.draw(batch, button.label, bx + 4, by + 12);
        }
    }
    
    public int getButtonAt(int screenX, int screenY, int screenWidth, int screenHeight) {
        int toolbarWidth = getWidth();
        
        float panelX = (screenWidth - toolbarWidth) / 2f;
        float panelY = screenHeight - TOOLBAR_HEIGHT - 20;
        
        if (screenX < panelX || screenX > panelX + toolbarWidth) return -1;
        if (screenY < panelY || screenY > panelY + TOOLBAR_HEIGHT) return -1;
        
        int col = (int) ((screenX - panelX - BUTTON_PADDING) / (BUTTON_WIDTH + BUTTON_PADDING));
        
        for (int i = 0; i < buttons.size(); i++) {
            if (buttons.get(i).column == col) {
                return i;
            }
        }
        
        return -1;
    }
    
    public void dispose() {
        if (backgroundTexture != null) backgroundTexture.dispose();
        if (font != null) font.dispose();
        for (BuildingButton button : buttons) {
            if (button.normalTexture != null) button.normalTexture.dispose();
            if (button.selectedTexture != null) button.selectedTexture.dispose();
            if (button.hoverTexture != null) button.hoverTexture.dispose();
        }
        for (Texture tex : buildingTextures.values()) {
            tex.dispose();
        }
    }
    
    private static class BuildingButton {
        int column;
        String label;
        int buildingType;
        Texture normalTexture;
        Texture selectedTexture;
        Texture hoverTexture;
        Texture iconTexture;
    }
}
