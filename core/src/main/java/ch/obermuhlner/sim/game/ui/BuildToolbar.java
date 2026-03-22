package ch.obermuhlner.sim.game.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

import java.util.ArrayList;
import java.util.List;
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

    private List<ToolButton> tools = new ArrayList<>();
    private Map<Integer, ToolButton> toolById = new HashMap<>();
    private Texture backgroundTexture;
    private BitmapFont font;
    private int selectedToolId = -1;
    private boolean visible = true;
    private int hoveredButtonIndex = -1;

    public BuildToolbar() {
        createBackground();
        createFonts();
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

    public void setTools(List<ToolButton> newTools) {
        tools.clear();
        toolById.clear();
        for (int i = 0; i < newTools.size() && i < MAX_COLUMNS; i++) {
            ToolButton tool = newTools.get(i);
            tool.column = i;
            tools.add(tool);
            toolById.put(tool.id, tool);
        }
        if (selectedToolId >= 0 && !toolById.containsKey(selectedToolId)) {
            selectedToolId = -1;
        }
    }

    public void selectTool(int id) {
        if (toolById.containsKey(id)) {
            selectedToolId = id;
        }
    }

    public int getSelectedToolId() {
        return selectedToolId;
    }

    public void deselectTool() {
        selectedToolId = -1;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public int getWidth() {
        return BUTTON_WIDTH * MAX_COLUMNS + BUTTON_PADDING * (MAX_COLUMNS + 1);
    }

    public int getHeight() {
        return TOOLBAR_HEIGHT;
    }

    public void updateHover(int screenX, int screenY, int screenWidth, int screenHeight) {
        if (!visible || tools.isEmpty()) {
            hoveredButtonIndex = -1;
            return;
        }

        int toolbarWidth = getWidth();
        float panelX = (screenWidth - toolbarWidth) / 2f;
        float panelY = screenHeight - TOOLBAR_HEIGHT - 20;

        if (screenX < panelX || screenX > panelX + toolbarWidth) {
            hoveredButtonIndex = -1;
            return;
        }
        if (screenY < panelY || screenY > panelY + TOOLBAR_HEIGHT) {
            hoveredButtonIndex = -1;
            return;
        }

        int col = (int) ((screenX - panelX - BUTTON_PADDING) / (BUTTON_WIDTH + BUTTON_PADDING));
        if (col >= 0 && col < tools.size()) {
            hoveredButtonIndex = col;
        } else {
            hoveredButtonIndex = -1;
        }
    }

    public int getToolIdAt(int screenX, int screenY, int screenWidth, int screenHeight) {
        if (!visible || tools.isEmpty()) {
            return -1;
        }

        int toolbarWidth = getWidth();
        float panelX = (screenWidth - toolbarWidth) / 2f;
        float panelY = screenHeight - TOOLBAR_HEIGHT - 20;

        if (screenX < panelX || screenX > panelX + toolbarWidth) return -1;
        if (screenY < panelY || screenY > panelY + TOOLBAR_HEIGHT) return -1;

        int col = (int) ((screenX - panelX - BUTTON_PADDING) / (BUTTON_WIDTH + BUTTON_PADDING));

        if (col >= 0 && col < tools.size()) {
            return tools.get(col).id;
        }
        return -1;
    }

    public void render(SpriteBatch batch, int screenWidth, int screenHeight) {
        if (!visible || tools.isEmpty()) return;

        int toolbarWidth = getWidth();
        float panelX = (screenWidth - toolbarWidth) / 2f;
        float panelY = screenHeight - TOOLBAR_HEIGHT - 20;

        batch.draw(backgroundTexture, panelX, panelY, toolbarWidth, TOOLBAR_HEIGHT);

        for (int i = 0; i < tools.size(); i++) {
            ToolButton tool = tools.get(i);

            Texture tex;
            if (tool.id == selectedToolId) {
                tex = tool.selectedTexture;
            } else if (i == hoveredButtonIndex) {
                tex = tool.hoverTexture;
            } else {
                tex = tool.normalTexture;
            }

            float bx = panelX + BUTTON_PADDING + i * (BUTTON_WIDTH + BUTTON_PADDING);
            float by = panelY + BUTTON_PADDING;

            batch.draw(tex, bx, by, BUTTON_WIDTH, BUTTON_HEIGHT);

            if (tool.iconTexture != null) {
                float iconX = bx + (BUTTON_WIDTH - ICON_SIZE) / 2f;
                float iconY = by + BUTTON_HEIGHT - ICON_SIZE - 6;
                batch.draw(tool.iconTexture, iconX, iconY, ICON_SIZE, ICON_SIZE);
            }

            font.draw(batch, tool.label, bx + 4, by + 12);
        }
    }

    public void dispose() {
        if (backgroundTexture != null) backgroundTexture.dispose();
        if (font != null) font.dispose();
        for (ToolButton tool : tools) {
            if (tool.normalTexture != null) tool.normalTexture.dispose();
            if (tool.selectedTexture != null) tool.selectedTexture.dispose();
            if (tool.hoverTexture != null) tool.hoverTexture.dispose();
        }
    }

    public static class ToolButton {
        public int id;
        public String label;
        public Texture iconTexture;
        public Texture normalTexture;
        public Texture selectedTexture;
        public Texture hoverTexture;
        public int column;

        public ToolButton(int id, String label, Texture iconTexture) {
            this.id = id;
            this.label = label;
            this.iconTexture = iconTexture;
            createTextures();
        }

        private void createTextures() {
            Pixmap normalPixmap = new Pixmap(BUTTON_WIDTH, BUTTON_HEIGHT, Pixmap.Format.RGBA8888);
            normalPixmap.setColor(new Color(0.2f, 0.2f, 0.3f, 1f));
            normalPixmap.fill();
            normalPixmap.setColor(new Color(0.4f, 0.4f, 0.5f, 1f));
            normalPixmap.drawRectangle(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT);
            normalTexture = new Texture(normalPixmap);
            normalPixmap.dispose();

            Pixmap selectedPixmap = new Pixmap(BUTTON_WIDTH, BUTTON_HEIGHT, Pixmap.Format.RGBA8888);
            selectedPixmap.setColor(new Color(0.4f, 0.6f, 0.8f, 1f));
            selectedPixmap.fill();
            selectedPixmap.setColor(new Color(0.6f, 0.8f, 1f, 1f));
            selectedPixmap.drawRectangle(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT);
            selectedTexture = new Texture(selectedPixmap);
            selectedPixmap.dispose();

            Pixmap hoverPixmap = new Pixmap(BUTTON_WIDTH, BUTTON_HEIGHT, Pixmap.Format.RGBA8888);
            hoverPixmap.setColor(new Color(0.3f, 0.3f, 0.4f, 1f));
            hoverPixmap.fill();
            hoverPixmap.setColor(new Color(0.5f, 0.5f, 0.6f, 1f));
            hoverPixmap.drawRectangle(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT);
            hoverTexture = new Texture(hoverPixmap);
            hoverPixmap.dispose();
        }
    }
}
