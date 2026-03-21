package ch.obermuhlner.sim.game.ui;

import ch.obermuhlner.sim.game.BuildingType;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class BuildToolbar {
    private static final int BUTTON_SIZE = 64;
    private static final int BUTTON_PADDING = 8;
    private static final int MAX_COLUMNS = 6;
    
    private static final int TOOLBAR_HEIGHT = BUTTON_SIZE + BUTTON_PADDING * 3;
    
    private java.util.List<BuildingButton> buttons = new java.util.ArrayList<>();
    private Texture backgroundTexture;
    private BitmapFont font;
    private int selectedIndex = -1;
    
    public BuildToolbar() {
        createBackground();
        createFonts();
        createButtons();
    }
    
    private void createBackground() {
        int width = BUTTON_SIZE * MAX_COLUMNS + BUTTON_PADDING * (MAX_COLUMNS + 1);
        int height = TOOLBAR_HEIGHT;
        
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        // Semi-transparent dark background
        pixmap.setColor(new Color(0.15f, 0.15f, 0.2f, 0.95f));
        pixmap.fill();
        // Border
        pixmap.setColor(new Color(0.4f, 0.4f, 0.5f, 1f));
        pixmap.drawRectangle(0, 0, width, height);
        backgroundTexture = new Texture(pixmap);
        pixmap.dispose();
    }
    
    private void createFonts() {
        font = new BitmapFont();
        font.setColor(Color.WHITE);
        font.getData().setScale(1.5f);
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
        
        Pixmap normalPixmap = new Pixmap(BUTTON_SIZE, BUTTON_SIZE, Pixmap.Format.RGBA8888);
        normalPixmap.setColor(new Color(0.2f, 0.2f, 0.3f, 1f));
        normalPixmap.fill();
        normalPixmap.setColor(new Color(0.4f, 0.4f, 0.5f, 1f));
        normalPixmap.drawRectangle(0, 0, BUTTON_SIZE, BUTTON_SIZE);
        button.normalTexture = new Texture(normalPixmap);
        normalPixmap.dispose();
        
        Pixmap selectedPixmap = new Pixmap(BUTTON_SIZE, BUTTON_SIZE, Pixmap.Format.RGBA8888);
        selectedPixmap.setColor(new Color(0.4f, 0.6f, 0.8f, 1f));
        selectedPixmap.fill();
        selectedPixmap.setColor(new Color(0.6f, 0.8f, 1f, 1f));
        selectedPixmap.drawRectangle(0, 0, BUTTON_SIZE, BUTTON_SIZE);
        button.selectedTexture = new Texture(selectedPixmap);
        selectedPixmap.dispose();
        
        Pixmap hoverPixmap = new Pixmap(BUTTON_SIZE, BUTTON_SIZE, Pixmap.Format.RGBA8888);
        hoverPixmap.setColor(new Color(0.3f, 0.3f, 0.4f, 1f));
        hoverPixmap.fill();
        hoverPixmap.setColor(new Color(0.5f, 0.5f, 0.6f, 1f));
        hoverPixmap.drawRectangle(0, 0, BUTTON_SIZE, BUTTON_SIZE);
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
        return BUTTON_SIZE * MAX_COLUMNS + BUTTON_PADDING * (MAX_COLUMNS + 1);
    }
    
    public int getHeight() {
        return TOOLBAR_HEIGHT;
    }
    
    public void render(SpriteBatch batch, int screenWidth, int screenHeight) {
        int toolbarWidth = getWidth();
        
        // Center horizontally, place at top with margin
        // With Y increasing upward, top of screen is screenHeight
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
            
            float bx = panelX + BUTTON_PADDING + button.column * (BUTTON_SIZE + BUTTON_PADDING);
            float by = panelY + BUTTON_PADDING;
            
            batch.draw(tex, bx, by, BUTTON_SIZE, BUTTON_SIZE);
            
            // Text: position above button center (Y decreases going down)
            float labelX = bx + 4;
            float labelY = by + 15; // Near top of button
            font.draw(batch, button.label, labelX, labelY);
        }
    }
    
    public int getButtonAt(int screenX, int screenY, int screenWidth, int screenHeight) {
        int toolbarWidth = getWidth();
        
        float panelX = (screenWidth - toolbarWidth) / 2f;
        float panelY = 20;
        
        if (screenX < panelX || screenX > panelX + toolbarWidth) return -1;
        if (screenY < panelY || screenY > panelY + TOOLBAR_HEIGHT) return -1;
        
        int col = (int) ((screenX - panelX - BUTTON_PADDING) / (BUTTON_SIZE + BUTTON_PADDING));
        
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
    }
    
    private static class BuildingButton {
        int column;
        String label;
        int buildingType;
        Texture normalTexture;
        Texture selectedTexture;
        Texture hoverTexture;
    }
}
