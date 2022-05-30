package net.maxsupermanhd.WebChunkAssistant;

import com.mojang.blaze3d.systems.RenderSystem;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebMapScreen extends Screen {
    public static final Logger LOGGER = LogManager.getLogger("WebMapScreen");
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final Identifier TEX_INVALID = new Identifier("webchunkassistant", "textures/gui/invalid.png");
    private final Identifier TEX_LOADING0 = new Identifier("webchunkassistant", "textures/gui/logo-0.png");
    private final Identifier TEX_LOADING1 = new Identifier("webchunkassistant", "textures/gui/logo-1.png");
    private final Identifier TEX_LOADING2 = new Identifier("webchunkassistant", "textures/gui/logo-2.png");
    private final Identifier TEX_LOADING3 = new Identifier("webchunkassistant", "textures/gui/logo-3.png");
    private final Identifier TEX_LOADING4 = new Identifier("webchunkassistant", "textures/gui/logo-4.png");
    private final HashMap<MapTilePos, WebTexture> loadedTextures = new HashMap<>(16);
    public long mapx, mapz;
    public int mapzoom = 4;
    public double mapoffsetx = 0.5, mapoffsety = 0.5;
    public int maptilesize = 256;
    public int zoombreaklow = 192;
    public int zoombreakhigh = 320;
    public int zoomsensitivity = 32;
    private final ExecutorService executorService = Executors.newFixedThreadPool(12);
    protected WebMapScreen(Text title) {
        super(title);
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

    public void renderTile(MatrixStack matrices, MapTilePos pos, int offx, int offy) {
        int ox = (int) (this.width/2 - maptilesize*mapoffsetx) + offx;
        int oy = (int) (this.height/2 - maptilesize*mapoffsety) + offy;
        WebTexture tex = loadedTextures.computeIfAbsent(pos, (MapTilePos p) -> {
            WebTexture t;
            try {
                t = new WebTexture(getURIfromPos(pos), pos.toIdentifier());
            } catch (Exception e) {
                LOGGER.info(e);
                return null;
            }
            executorService.submit(t);
            return t;
        });
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        boolean drawTex = true;
        if(tex == null) {
            RenderSystem.setShaderTexture(0, TEX_INVALID);
        } else if(Objects.equals(tex.status, "init")) {
            RenderSystem.setShaderTexture(0, TEX_LOADING0);
        } else if(Objects.equals(tex.status, "allocating")) {
            RenderSystem.setShaderTexture(0, TEX_LOADING1);
        } else if(Objects.equals(tex.status, "requesting")) {
            RenderSystem.setShaderTexture(0, TEX_LOADING2);
        } else if(Objects.equals(tex.status, "parsing")) {
            RenderSystem.setShaderTexture(0, TEX_LOADING3);
        } else if(Objects.equals(tex.status, "registering")) {
            RenderSystem.setShaderTexture(0, TEX_LOADING4);
            mc.getTextureManager().registerTexture(tex.id, new NativeImageBackedTexture(tex.img));
            tex.status = "done";
        } else if(Objects.equals(tex.status, "done")) {
            RenderSystem.setShaderTexture(0, tex.id);
        } else {
            drawTex = false;
        }
        if(drawTex) {
            drawTexture(matrices, ox, oy, 0.0f, 0.0f, maptilesize, maptilesize, maptilesize, maptilesize);
        }
        drawBox(matrices, ox, oy, (ox+maptilesize), (oy+maptilesize));
    }

    public URL getURIfromPos(MapTilePos pos) throws MalformedURLException {
        ChunkAssistantConfig config = AutoConfig.getConfigHolder(ChunkAssistantConfig.class).getConfig();
        return new URL(String.format("%sworlds/%s/%s/tiles/terrain/%d/%d/%d/png", config.baseurl, pos.world, pos.dimension, pos.zoom, pos.cx, pos.cz));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        Screen.fill(matrices, 0, 0, this.width, this.height, 0xFF000000);
        int fitx = width / maptilesize + 3;
        int fitz = height / maptilesize + 3;
        for(int offz = -(fitz/2+1); offz < fitz/2+1; offz++) {
            for(int offx = -(fitx/2+1); offx < fitx/2+1; offx++) {
                renderTile(matrices, new MapTilePos("phoenixanarchy.com", "overworld", mapx+offx, mapz+offz, mapzoom), offx*maptilesize, offz*maptilesize);
            }
        }
        this.textRenderer.drawWithShadow(matrices, String.format("Map position: %d:%d (x%d z%d)", mapx, mapz, mapx*(16L *(int)Math.pow(2, mapzoom)), mapz*(16L *(int)Math.pow(2, mapzoom))), 10, 10, 0x00FFFFFF);
        this.textRenderer.drawWithShadow(matrices, String.format("Map offset: %3.3f %3.3f", mapoffsetx, mapoffsety), 10, 20, 0x00FFFFFF);
        this.textRenderer.drawWithShadow(matrices, String.format("Map zoom: %d %d", mapzoom, maptilesize), 10, 30, 0x00FFFFFF);
        this.textRenderer.drawWithShadow(matrices, String.format("Tiles loaded: %d", loadedTextures.size()), 10, 40, 0x00FFFFFF);
        this.textRenderer.drawWithShadow(matrices, String.format("Mouse: %d %d %b", mouseX, mouseY, this.isDragging()), 10, 50, 0x00FFFFFF);
        this.drawVerticalLine(matrices, width/2-1, height/2-3, height/2-10, 0xFF000000);
        this.drawVerticalLine(matrices, width/2  , height/2-3, height/2-10, 0xFFFFFFFF);
        this.drawVerticalLine(matrices, width/2+1, height/2-3, height/2-10, 0xFF000000);
        this.drawVerticalLine(matrices, width/2-1, height/2+3, height/2+10, 0xFF000000);
        this.drawVerticalLine(matrices, width/2  , height/2+3, height/2+10, 0xFFFFFFFF);
        this.drawVerticalLine(matrices, width/2+1, height/2+3, height/2+10, 0xFF000000);
        this.drawHorizontalLine(matrices, width/2-3, width/2-10, height/2+1, 0xFF000000);
        this.drawHorizontalLine(matrices, width/2-3, width/2-10, height/2  , 0xFFFFFFFF);
        this.drawHorizontalLine(matrices, width/2-3, width/2-10, height/2-1, 0xFF000000);
        this.drawHorizontalLine(matrices, width/2+3, width/2+10, height/2+1, 0xFF000000);
        this.drawHorizontalLine(matrices, width/2+3, width/2+10, height/2  , 0xFFFFFFFF);
        this.drawHorizontalLine(matrices, width/2+3, width/2+10, height/2-1, 0xFF000000);
        super.render(matrices, mouseX, mouseY, delta);
    }

    public void normalizeOffset() {
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
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        double dx = deltaX / maptilesize;
        double dy = deltaY / maptilesize;
        mapoffsetx -= dx;
        mapoffsety -= dy;
        normalizeOffset();
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    public double coordToTile(int zoom, long coord) {
        return coord/(Math.pow(2, zoom)*16);
    }

    public long tileToCoord(int zoom, double tile) {
        return (long) (Math.pow(2, zoom)*16*tile);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if(amount > 0) {
            maptilesize += zoomsensitivity;
        } else if(amount < 0) {
            maptilesize -= zoomsensitivity;
        }
        int zoomchunks = (int) Math.pow(2, mapzoom);
        long keepx = tileToCoord(mapzoom, mapx+mapoffsetx);
        long keepz = tileToCoord(mapzoom, mapz+mapoffsety);
        if(maptilesize-1 < zoombreaklow) {
            maptilesize += zoombreakhigh - zoombreaklow;
            mapzoom += 1;
        } else if (maptilesize+1 > zoombreakhigh) {
            maptilesize -= zoombreakhigh - zoombreaklow;
//            mapx = (long) ((mapx * zoomchunks) / Math.pow(2, mapzoom-1))+1;
//            mapz = (long) ((mapz * zoomchunks) / Math.pow(2, mapzoom-1))+1;
//            mapoffsetx = mapoffsetx*2-1;
//            mapoffsety = mapoffsety*2-1;
            mapzoom -= 1;
        }
        double newmapx = coordToTile(mapzoom, keepx);
        double newmapz = coordToTile(mapzoom, keepz);
        mapx = (long)newmapx;
        mapz = (long)newmapz;
        mapoffsetx = newmapx - mapx;
        mapoffsety = newmapz - mapz;
        normalizeOffset();
        return super.mouseScrolled(mouseX, mouseY, amount);
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
        if(keyCode == GLFW.GLFW_KEY_R) {
            loadedTextures.clear();
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    public static class MapTilePos {
        public String world, dimension;
        public long cx, cz, zoom;
        MapTilePos(String world, String dimension, long cx, long cz, int zoom) {
            this.world = world;
            this.dimension = dimension;
            this.cx = cx;
            this.cz = cz;
            this.zoom = zoom;
        }
        public Identifier toIdentifier() {
            if(dimension.contains("minecraft:")) {
                dimension = dimension.replaceFirst("minecraft:", "");
            }
            return new Identifier(String.format("%s/%s/%d/%d/%d", world, dimension, zoom, cx, cz));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MapTilePos that = (MapTilePos) o;
            return cx == that.cx && cz == that.cz && zoom == that.zoom && world.equals(that.world) && dimension.equals(that.dimension);
        }

        @Override
        public int hashCode() {
            return Objects.hash(world, dimension, cx, cz, zoom);
        }
    }
}
