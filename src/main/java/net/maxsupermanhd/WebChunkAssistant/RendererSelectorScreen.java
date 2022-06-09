package net.maxsupermanhd.WebChunkAssistant;

import com.google.gson.Gson;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PressableTextWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static net.minecraft.util.Formatting.GRAY;
import static net.minecraft.util.Formatting.WHITE;

public class RendererSelectorScreen extends Screen {
    public static final Logger LOGGER = LogManager.getLogger("RendererSelectorScreen");
    private final WebMapScreen parent;
    private boolean dataReady = false;
    private String error = "";
    private Renderer[] renderers = null;
    private List<String> enabledOverlays = new ArrayList<>();
    private Lock rendererslock = new ReentrantLock();
    private Thread requesterThread = null;
    private int displayoffset = 0;
    private boolean reinitWidgets = false;

    protected RendererSelectorScreen(WebMapScreen parent, Text title) {
        super(title);
        if(parent.overlays != null) {
            enabledOverlays.addAll(Arrays.stream(parent.overlays).toList());
        }
        this.parent = parent;
        requesterThread = new Thread(() -> {
            String jsonresp = null;
            try {
                var url = new URL(AutoConfig.getConfigHolder(ChunkAssistantConfig.class).getConfig().apiGetRenderers);
                var con = (HttpURLConnection) url.openConnection();
                if (con == null)
                    throw new Exception("con is nul");
                con.setRequestMethod("GET");
                con.setRequestProperty("Accept", "application/json");
                int httpstatus = con.getResponseCode();
                if (httpstatus != 200)
                    throw new Exception(String.format("Bad http response code: %s", httpstatus));
                BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
                String responseLine;
                StringBuilder response = new StringBuilder();
                while ((responseLine = br.readLine()) != null)
                    response.append(responseLine.trim());
                jsonresp = response.toString();
            } catch (Exception e) {
                rendererslock.lock();
                error = e.toString();
                rendererslock.unlock();
                LOGGER.info(e);
                e.printStackTrace();
                return;
            }
            rendererslock.lock();
            try {
                renderers = new Gson().fromJson(jsonresp, Renderer[].class);
                dataReady = true;
            } catch (Exception e) {
                error = e.toString();
                LOGGER.info(e);
                e.printStackTrace();
            }
            rendererslock.unlock();
            initWidgets();
        });
        requesterThread.start();
    }

    @Override
    protected void init() {
        initWidgets();
        super.init();
    }

    @Override
    public void tick() {
        if (reinitWidgets) {
            initWidgets();
        }
        super.tick();
    }

    public void initWidgets() {
        this.clearChildren();
        this.addDrawableChild(new ButtonWidget(this.width / 2 - 20, this.height - 15, 40, 13, ScreenTexts.CANCEL, (button) -> {
            this.close();
        }));
        rendererslock.lock();
        if (dataReady) {
            int linesfit = (height - 24) / 12;
            int occupied = 0;
            for (int i = 0; i + displayoffset < renderers.length || i == linesfit; i++) {
                Renderer r = renderers[i + displayoffset];
                if(r.IsOverlay) {
                    continue;
                }
                LiteralText t = new LiteralText(r.DisplayName);
                if(parent.format.equalsIgnoreCase(r.Name) || (r.IsDefault && parent.format.length() == 0)) {
                    t.fillStyle(Style.EMPTY.withColor(WHITE));
                } else {
                    t.fillStyle(Style.EMPTY.withColor(GRAY));
                }
                int l = textRenderer.getWidth(t);
                this.addDrawableChild(new PressableTextWidget(width / 2 - l / 2, 2 + occupied * 12, l, 8, t, b -> {
                    parent.format = r.Name;
                    reinitWidgets = true;
                }, this.textRenderer));
                occupied++;
            }
            for (int i = 0; i + displayoffset < renderers.length || i == linesfit; i++) {
                Renderer r = renderers[i + displayoffset];
                if(!r.IsOverlay) {
                    continue;
                }
                LiteralText t = new LiteralText(r.DisplayName);
                if(enabledOverlays.contains(r.Name)) {
                    t.fillStyle(Style.EMPTY.withColor(WHITE));
                } else {
                    t.fillStyle(Style.EMPTY.withColor(GRAY));
                }
                int l = textRenderer.getWidth(t);
                this.addDrawableChild(new PressableTextWidget(width / 2 - l / 2, 2 + occupied * 12+6, l, 8, t, b -> {
                    if(enabledOverlays.contains(r.Name)) {
                        enabledOverlays.remove(r.Name);
                    } else {
                        enabledOverlays.add(r.Name);
                    }
                    reinitWidgets = true;
                }, this.textRenderer));
                occupied++;
            }
        }
        rendererslock.unlock();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        fill(matrices, 0, 0, width, height, 0xFF000000);
        rendererslock.lock();
        if (!error.isEmpty()) {
            parent.renderError(matrices, error);
        } else if (!dataReady) {
            parent.renderCenteredText(matrices, "Loading...", 0x00FFFFFF);
        }
        super.render(matrices, mouseX, mouseY, delta);
        rendererslock.unlock();
    }

    public void close() {
        parent.overlays = enabledOverlays.toArray(new String[0]);
        assert this.client != null;
        this.client.setScreen(this.parent);
    }

    public class Renderer {
        String Name;
        String DisplayName;
        boolean IsOverlay;
        boolean IsDefault;
        boolean selected = false;
    }
}