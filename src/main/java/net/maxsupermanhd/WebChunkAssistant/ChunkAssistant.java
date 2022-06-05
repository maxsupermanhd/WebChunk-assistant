package net.maxsupermanhd.WebChunkAssistant;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.LiteralText;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

public class ChunkAssistant implements ModInitializer  {
    public static final Logger LOGGER = LogManager.getLogger("ChunkAssistant");
    public TerrainReporter reporter = new TerrainReporter();
    public static final KeyBinding keybindOpenConfig = KeyBindingHelper.registerKeyBinding(new KeyBinding("Open config", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_O, "WebChunk Assistant"));
    public static final KeyBinding keybindOpenMap = KeyBindingHelper.registerKeyBinding(new KeyBinding("Open map", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_HOME, "WebChunk Assistant"));
    public static final KeyBinding keybindToggle = KeyBindingHelper.registerKeyBinding(new KeyBinding("Toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_P, "WebChunk Assistant"));
    public static final KeyBinding keybindToggleOverlay = KeyBindingHelper.registerKeyBinding(new KeyBinding("Toggle overlay", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, "WebChunk Assistant"));

    public final WebMapScreen mapScreen = new WebMapScreen(new LiteralText("Map"));

    @Override
    public void onInitialize() {
        AutoConfig.register(ChunkAssistantConfig.class, JanksonConfigSerializer::new);
        HudRenderCallback.EVENT.register(reporter::RenderGameOverlayEvent);
        ClientTickEvents.END_CLIENT_TICK.register(reporter::clientTickEvent);
        ClientTickEvents.END_CLIENT_TICK.register(this::clientTickEvent);
    }

    private void clientTickEvent(MinecraftClient mc) {
        this.keyInputEvent(mc);
    }

    private void keyInputEvent(MinecraftClient mc) {
        if(keybindOpenConfig.wasPressed()) {
            mc.setScreen(AutoConfig.getConfigScreen(ChunkAssistantConfig.class, null).get());
        }
        if(keybindToggle.wasPressed()) {
            ChunkAssistantConfig config = AutoConfig.getConfigHolder(ChunkAssistantConfig.class).getConfig();
            config.enabled = !config.enabled;
            AutoConfig.getConfigHolder(ChunkAssistantConfig.class).save();
        }
        if(keybindToggleOverlay.wasPressed()) {
            ChunkAssistantConfig config = AutoConfig.getConfigHolder(ChunkAssistantConfig.class).getConfig();
            config.render_overlay = !config.render_overlay;
            AutoConfig.getConfigHolder(ChunkAssistantConfig.class).save();
        }
        if(keybindToggleOverlay.wasPressed()) {
            ChunkAssistantConfig config = AutoConfig.getConfigHolder(ChunkAssistantConfig.class).getConfig();
            config.render_overlay = !config.render_overlay;
            AutoConfig.getConfigHolder(ChunkAssistantConfig.class).save();
        }
        if(keybindOpenMap.wasPressed()) {
            mc.setScreen(mapScreen);
        }
    }
}
