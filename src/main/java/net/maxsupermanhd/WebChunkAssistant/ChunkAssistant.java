package net.maxsupermanhd.WebChunkAssistant;

import fi.dy.masa.malilib.registry.Registry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.maxsupermanhd.WebChunkAssistant.misc.Reference;
import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.MutableText;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ChunkAssistant implements ClientModInitializer {
    public static final Logger logger = LogManager.getLogger(Reference.MOD_ID);
    public TerrainReporter reporter = new TerrainReporter();
    public static final WebMapScreen mapScreen = new WebMapScreen(MutableText.of(new LiteralTextContent("Map")));

    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register(reporter::RenderGameOverlayEvent);
        ClientTickEvents.END_CLIENT_TICK.register(reporter::clientTickEvent);
        Registry.INITIALIZATION_DISPATCHER.registerInitializationHandler(new InitHandler());
    }
}
