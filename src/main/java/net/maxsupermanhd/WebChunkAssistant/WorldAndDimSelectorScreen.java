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

public class WorldAndDimSelectorScreen extends Screen {
    public static final Logger LOGGER = LogManager.getLogger("DimSelectorScreen");
    private final WebMapScreen parent;
    private boolean dataReady = false;
    private String error = "";
    private Dimension[] dims = null;
    private Lock dimslock = new ReentrantLock();
    private Thread requesterThread = null;
    private int displayoffset = 0;
    protected WorldAndDimSelectorScreen(WebMapScreen parent, Text title) {
        super(title);
        this.parent = parent;
        requesterThread = new Thread(() -> {
            String rsp = null;
            try {
                var url = new URL(AutoConfig.getConfigHolder(ChunkAssistantConfig.class).getConfig().apiGetDimensions);
                var con = (HttpURLConnection)url.openConnection();
                if(con == null)
                    throw new Exception("con is nul");
                con.setRequestMethod("GET");
                con.setRequestProperty("Accept", "application/json");
                int httpstatus = con.getResponseCode();
                if(httpstatus != 200)
                    throw new Exception(String.format("Bad http response code: %s", httpstatus));
                BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
                String responseLine;
                StringBuilder response = new StringBuilder();
                while ((responseLine = br.readLine()) != null)
                    response.append(responseLine.trim());
                rsp = response.toString();
            } catch (Exception e) {
                dimslock.lock();
                error = e.toString();
                dimslock.unlock();
                LOGGER.info(e);
                e.printStackTrace();
                return;
            }
            dimslock.lock();
            try {
                dims = new Gson().fromJson(rsp.toString(), Dimension[].class);
                dataReady = true;
            } catch (Exception e) {
                error = e.toString();
                LOGGER.info(e);
                e.printStackTrace();
            }
            dimslock.unlock();
            initWidgets();
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
        dimslock.lock();
        if(dataReady) {
            int linesfit = (height - 25)/12;
            for(int i = 0; i + displayoffset < dims.length || i == linesfit; i++) {
                String w = dims[i+displayoffset].world;
                String d = dims[i+displayoffset].name;
                String t;
                int x = width/2 - textRenderer.getWidth(w)+2;
                if(parent.dimensionName.equalsIgnoreCase(d) && parent.worldName.equalsIgnoreCase(w)) {
                    t = String.format(">>> %s | %s <<<", w, d);
                    x = width/2 - textRenderer.getWidth(">>> "+w)+2;
                } else {
                    t = String.format("%s | %s", w, d);
                }
                this.addDrawableChild(new PressableTextWidget(
                        x, 2 + i*12,
                        textRenderer.getWidth(t), 10,
                        new LiteralText(t), b -> {returnWith(w, d);}, this.textRenderer));
            }
        }
        dimslock.unlock();
    }

    public void returnWith(String world, String dim) {
        parent.setWorldName(world);
        parent.setDimensionName(dim);
        close();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        fill(matrices, 0, 0, width, height, 0xFF000000);
        dimslock.lock();
        if(!error.isEmpty()) {
            parent.renderError(matrices, error);
        } else if(!dataReady) {
            parent.renderCenteredText(matrices, "Loading...", 0x00FFFFFF);
        }
        super.render(matrices, mouseX, mouseY, delta);
        dimslock.unlock();
    }

    public void close() {
        assert this.client != null;
        this.client.setScreen(this.parent);
    }

    public static class Dimension {
        public String name;
        public String world;
    }

    //    @Override
//    public void init() {
//        CyclingOption.create("options.particles", ParticlesMode.values(), (particlesMode) -> {
//            return new TranslatableText(particlesMode.getTranslationKey());
//        }, (gameOptions) -> {
//            return gameOptions.particles;
//        }, (gameOptions, option, particlesMode) -> {
//            gameOptions.particles = particlesMode;
//        });
//        this.addDrawableChild(new ButtonWidget(this.width / 2 + 4, this.height / 4 + 48 + -16, 98, 20, new TranslatableText("gui.stats"), (button) -> {
//            this.client.setScreen(new StatsScreen(this, this.client.player.getStatHandler()));
//        }));
//    }

}
