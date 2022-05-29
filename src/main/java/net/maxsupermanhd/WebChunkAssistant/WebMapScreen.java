package net.maxsupermanhd.WebChunkAssistant;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public class WebMapScreen extends Screen {
    public static final Logger LOGGER = LogManager.getLogger("WebMapScreen");
    private MinecraftClient mc = MinecraftClient.getInstance();
    private TextureManager textureManager;
    private HashMap<MapTilePos, WebTexture> loadedTextures = new HashMap<MapTilePos, WebTexture>(16);
    public long mapx, mapz;
    public double mapscale = 1;
    public double mapoffsetx = 0.5, mapoffsety = 0.5;
    public double maptilesize = 256;
    protected WebMapScreen(Text title) {
        super(title);
        textureManager = mc.getTextureManager();
    }

    @Override
    public void tick() {
    }
    @Override
    public void init() {
    }

    public void drawBox(MatrixStack matrices, int x0, int y0, int x1, int y1) {
        this.drawHorizontalLine(matrices, x0, x1, y0, 0xFFFFFFFF);
        this.drawHorizontalLine(matrices, x0, x1, y1, 0xFFFFFFFF);
        this.drawVerticalLine(matrices, x0, y0, y1, 0xFFFFFFFF);
        this.drawVerticalLine(matrices, x1, y0, y1, 0xFFFFFFFF);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        Screen.fill(matrices, 0, 0, this.width, this.height, 0xFF000000);
        this.textRenderer.draw(matrices, String.format("Map position: %d %d", mapx, mapz), 10, 10, 0x00FFFFFF);
        this.textRenderer.draw(matrices, String.format("Map offset: %3.3f %3.3f", mapoffsetx, mapoffsety), 10, 20, 0x00FFFFFF);
        this.textRenderer.draw(matrices, String.format("Mouse: %d %d %b", mouseX, mouseY, this.isDragging()), 10, 30, 0x00FFFFFF);
        int ox = (int) (this.width/2 - maptilesize*mapoffsetx);
        int oy = (int) (this.height/2 - maptilesize*mapoffsety);
        drawBox(matrices, ox, oy, (int) (ox+maptilesize), (int) (oy+maptilesize));
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        double dx = deltaX / maptilesize;
        double dy = deltaY / maptilesize;
        mapoffsetx -= dx;
        mapoffsety -= dy;
        if (mapoffsetx > 1) {
            mapx += 1;
            mapoffsetx -= 1;
        } else if (mapoffsetx < 0) {
            mapx -= 1;
            mapoffsetx += 1;
        }
        if (mapoffsety > 1) {
            mapz += 1;
            mapoffsety -= 1;
        } else if (mapoffsety < 0) {
            mapz -= 1;
            mapoffsety += 1;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    public static class MapTilePos {
        public String world, dimension;
        public long cx, cz;
        MapTilePos(String world, String dimension, long cx, long cz) {
            this.world = world;
            this.dimension = dimension;
            this.cx = cx;
            this.cz = cz;
        }
    }
}
