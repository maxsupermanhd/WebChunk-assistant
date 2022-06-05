package net.maxsupermanhd.WebChunkAssistant;

import com.google.gson.Gson;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PressableTextWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RendererSelectorScreen extends Screen {
    public static final Logger LOGGER = LogManager.getLogger("RendererSelectorScreen");
    private final WebMapScreen parent;
    private boolean dataReady = false;
    private String error = "";
    private String[] renderers = null;
    private Lock rendererslock = new ReentrantLock();
    private Thread requesterThread = null;
    private int displayoffset = 0;

    protected RendererSelectorScreen(WebMapScreen parent, Text title) {
        super(title);
        this.parent = parent;
        requesterThread = new Thread(() -> {
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
                rendererslock.lock();
                renderers = new Gson().fromJson(response.toString(), String[].class);
                dataReady = true;
                rendererslock.unlock();
                initWidgets();
            } catch (Exception e) {
                rendererslock.lock();
                error = e.toString();
                rendererslock.unlock();
                LOGGER.info(e);
                e.printStackTrace();
            }
        });
        requesterThread.start();
    }

    @Override
    protected void init() {
        initWidgets();
        super.init();
    }

    public void initWidgets() {
        this.clearChildren();
        this.addDrawableChild(new ButtonWidget(this.width / 2 - 20, this.height - 15, 40, 13, ScreenTexts.CANCEL, (button) -> {
            this.close();
        }));
        rendererslock.lock();
        if (dataReady) {
            int linesfit = (height - 25) / 10;
            for (int i = 0; i + displayoffset < renderers.length || i == linesfit; i++) {
                String w = renderers[i + displayoffset];
                String t = String.format("<[%s]>", w);
                int l = textRenderer.getWidth(t);
                this.addDrawableChild(new PressableTextWidget(width / 2 - l / 2, 2 + i * 8, l, 8, new LiteralText(t), b -> {
                    returnWith(w);
                }, this.textRenderer));
            }
        }
        rendererslock.unlock();
    }

    public void returnWith(String renderer) {
        parent.setFormat(renderer);
        close();
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
        assert this.client != null;
        this.client.setScreen(this.parent);
    }
}