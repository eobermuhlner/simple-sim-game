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
    public static final int SCROLL_LEFT  = -2;
    public static final int SCROLL_RIGHT = -3;

    private static final int BUTTON_WIDTH  = 80;
    private static final int BUTTON_HEIGHT = 80;
    private static final int BUTTON_PADDING = 8;
    private static final int MAX_COLUMNS = 6;
    private static final int ICON_SIZE = 48;
    private static final int FONT_SIZE = 10;
    private static final int SCROLL_BUTTON_WIDTH = 32;

    private static final int TOOLBAR_HEIGHT = BUTTON_HEIGHT + BUTTON_PADDING * 3;

    private List<ToolButton> allTools = new ArrayList<>();
    private List<ToolButton> tools    = new ArrayList<>();
    private Map<Integer, ToolButton> toolById = new HashMap<>();
    private int scrollOffset = 0;

    private Texture backgroundTexture;
    private Texture scrollNormalTexture;
    private Texture scrollHoverTexture;
    private BitmapFont font;
    private int selectedToolId = -1;
    private boolean visible = true;
    private int hoveredButtonIndex = -1;
    private int hoveredScroll = 0; // -1 = left arrow, 0 = none, 1 = right arrow

    public BuildToolbar() {
        createBackground();
        createScrollTextures();
        createFonts();
    }

    private void createBackground() {
        int width  = BUTTON_WIDTH * MAX_COLUMNS + BUTTON_PADDING * (MAX_COLUMNS + 1);
        int height = TOOLBAR_HEIGHT;
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0.15f, 0.15f, 0.2f, 0.95f));
        pixmap.fill();
        pixmap.setColor(new Color(0.4f, 0.4f, 0.5f, 1f));
        pixmap.drawRectangle(0, 0, width, height);
        backgroundTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    private void createScrollTextures() {
        scrollNormalTexture = makeScrollTexture(new Color(0.2f, 0.2f, 0.3f, 0.9f), new Color(0.4f, 0.4f, 0.5f, 1f));
        scrollHoverTexture  = makeScrollTexture(new Color(0.3f, 0.3f, 0.4f, 0.9f), new Color(0.5f, 0.5f, 0.6f, 1f));
    }

    private Texture makeScrollTexture(Color fill, Color border) {
        Pixmap pixmap = new Pixmap(SCROLL_BUTTON_WIDTH, TOOLBAR_HEIGHT, Pixmap.Format.RGBA8888);
        pixmap.setColor(fill);
        pixmap.fill();
        pixmap.setColor(border);
        pixmap.drawRectangle(0, 0, SCROLL_BUTTON_WIDTH, TOOLBAR_HEIGHT);
        Texture t = new Texture(pixmap);
        pixmap.dispose();
        return t;
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
        allTools.clear();
        allTools.addAll(newTools);
        scrollOffset = 0;
        rebuildVisible();
    }

    public void scroll(int delta) {
        int maxOffset = Math.max(0, allTools.size() - MAX_COLUMNS);
        scrollOffset = Math.max(0, Math.min(scrollOffset + delta, maxOffset));
        rebuildVisible();
    }

    private void rebuildVisible() {
        tools.clear();
        toolById.clear();
        int end = Math.min(scrollOffset + MAX_COLUMNS, allTools.size());
        for (int i = scrollOffset; i < end; i++) {
            ToolButton tool = allTools.get(i);
            tool.column = i - scrollOffset;
            tools.add(tool);
            toolById.put(tool.id, tool);
        }
        if (selectedToolId >= 0 && !toolById.containsKey(selectedToolId)) {
            selectedToolId = -1;
        }
    }

    private boolean hasScrollLeft()  { return scrollOffset > 0; }
    private boolean hasScrollRight() { return scrollOffset + MAX_COLUMNS < allTools.size(); }

    public void selectTool(int id) {
        if (toolById.containsKey(id)) {
            selectedToolId = id;
        }
    }

    public int getSelectedToolId() { return selectedToolId; }

    public void deselectTool() { selectedToolId = -1; }

    public boolean isVisible() { return visible; }

    public void setVisible(boolean visible) { this.visible = visible; }

    public int getWidth() {
        return BUTTON_WIDTH * MAX_COLUMNS + BUTTON_PADDING * (MAX_COLUMNS + 1);
    }

    public int getHeight() { return TOOLBAR_HEIGHT; }

    public void updateHover(int screenX, int screenY, int screenWidth, int screenHeight) {
        hoveredButtonIndex = -1;
        hoveredScroll = 0;
        if (!visible || allTools.isEmpty()) return;

        int toolbarWidth = getWidth();
        float panelX = (screenWidth - toolbarWidth) / 2f;
        float panelY = screenHeight - TOOLBAR_HEIGHT - 20;

        if (screenY < panelY || screenY > panelY + TOOLBAR_HEIGHT) return;

        if (hasScrollLeft() && screenX >= panelX - SCROLL_BUTTON_WIDTH && screenX < panelX) {
            hoveredScroll = -1;
            return;
        }
        if (hasScrollRight() && screenX >= panelX + toolbarWidth && screenX < panelX + toolbarWidth + SCROLL_BUTTON_WIDTH) {
            hoveredScroll = 1;
            return;
        }

        if (screenX < panelX || screenX > panelX + toolbarWidth) return;

        int col = (int) ((screenX - panelX - BUTTON_PADDING) / (BUTTON_WIDTH + BUTTON_PADDING));
        if (col >= 0 && col < tools.size()) {
            hoveredButtonIndex = col;
        }
    }

    public int getToolIdAt(int screenX, int screenY, int screenWidth, int screenHeight) {
        if (!visible || allTools.isEmpty()) return -1;

        int toolbarWidth = getWidth();
        float panelX = (screenWidth - toolbarWidth) / 2f;
        float panelY = screenHeight - TOOLBAR_HEIGHT - 20;

        if (screenY < panelY || screenY > panelY + TOOLBAR_HEIGHT) return -1;

        if (hasScrollLeft() && screenX >= panelX - SCROLL_BUTTON_WIDTH && screenX < panelX) {
            return SCROLL_LEFT;
        }
        if (hasScrollRight() && screenX >= panelX + toolbarWidth && screenX < panelX + toolbarWidth + SCROLL_BUTTON_WIDTH) {
            return SCROLL_RIGHT;
        }

        if (screenX < panelX || screenX > panelX + toolbarWidth) return -1;

        int col = (int) ((screenX - panelX - BUTTON_PADDING) / (BUTTON_WIDTH + BUTTON_PADDING));
        if (col >= 0 && col < tools.size()) {
            return tools.get(col).id;
        }
        return -1;
    }

    public void render(SpriteBatch batch, int screenWidth, int screenHeight) {
        if (!visible || allTools.isEmpty()) return;

        int toolbarWidth = getWidth();
        float panelX = (screenWidth - toolbarWidth) / 2f;
        float panelY = screenHeight - TOOLBAR_HEIGHT - 20;

        batch.draw(backgroundTexture, panelX, panelY, toolbarWidth, TOOLBAR_HEIGHT);

        if (hasScrollLeft()) {
            Texture t = hoveredScroll == -1 ? scrollHoverTexture : scrollNormalTexture;
            batch.draw(t, panelX - SCROLL_BUTTON_WIDTH, panelY, SCROLL_BUTTON_WIDTH, TOOLBAR_HEIGHT);
            font.draw(batch, "<", panelX - SCROLL_BUTTON_WIDTH + 10, panelY + TOOLBAR_HEIGHT / 2f + 6);
        }
        if (hasScrollRight()) {
            Texture t = hoveredScroll == 1 ? scrollHoverTexture : scrollNormalTexture;
            batch.draw(t, panelX + toolbarWidth, panelY, SCROLL_BUTTON_WIDTH, TOOLBAR_HEIGHT);
            font.draw(batch, ">", panelX + toolbarWidth + 10, panelY + TOOLBAR_HEIGHT / 2f + 6);
        }

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

            if (tool.costLabel != null && !tool.costLabel.isEmpty()) {
                font.draw(batch, tool.label,     bx + 4, by + 24);
                font.draw(batch, tool.costLabel, bx + 4, by + 12);
            } else {
                font.draw(batch, tool.label, bx + 4, by + 12);
            }
        }
    }

    public void dispose() {
        if (backgroundTexture != null)    backgroundTexture.dispose();
        if (scrollNormalTexture != null)  scrollNormalTexture.dispose();
        if (scrollHoverTexture != null)   scrollHoverTexture.dispose();
        if (font != null)                 font.dispose();
        for (ToolButton tool : allTools) {
            if (tool.normalTexture != null)   tool.normalTexture.dispose();
            if (tool.selectedTexture != null) tool.selectedTexture.dispose();
            if (tool.hoverTexture != null)    tool.hoverTexture.dispose();
        }
    }

    public static class ToolButton {
        public int id;
        public String label;
        public String costLabel = "";
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
