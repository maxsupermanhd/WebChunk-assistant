package net.maxsupermanhd.WebChunkAssistant;

import fi.dy.masa.malilib.config.JsonModConfig;
import fi.dy.masa.malilib.config.util.ConfigUpdateUtils;
import fi.dy.masa.malilib.event.InitializationHandler;
import fi.dy.masa.malilib.input.ActionResult;
import fi.dy.masa.malilib.registry.Registry;
import net.maxsupermanhd.WebChunkAssistant.config.Config;
import net.maxsupermanhd.WebChunkAssistant.config.ConfigScreen;
import net.maxsupermanhd.WebChunkAssistant.config.Hotkeys;
import net.maxsupermanhd.WebChunkAssistant.config.HotkeysProvider;
import net.maxsupermanhd.WebChunkAssistant.misc.Reference;

import java.util.ArrayList;

public class InitHandler implements InitializationHandler {

    @Override
    public void registerModHandlers() {
        java.util.List<org.apache.commons.lang3.tuple.Pair<String, String>> renamed = new ArrayList<>();
        JsonModConfig.ConfigDataUpdater updater = new ConfigUpdateUtils.ConfigCategoryRenamer(renamed, 1, 1);
        Registry.CONFIG_MANAGER.registerConfigHandler(JsonModConfig.createJsonModConfig(Reference.MOD_INFO, 1, Config.CATEGORIES, updater));
        Registry.CONFIG_SCREEN.registerConfigScreenFactory(Reference.MOD_INFO, ConfigScreen::create);
        Registry.CONFIG_TAB.registerConfigTabProvider(Reference.MOD_INFO, ConfigScreen::getConfigTabs);

        Registry.HOTKEY_MANAGER.registerHotkeyProvider(new HotkeysProvider());

        Hotkeys.KEYBIND_SETTINGS.createCallbackForAction(ctx -> {ctx.getClient().setScreen(ConfigScreen.create()); return ActionResult.SUCCESS;});
        Hotkeys.KEYBIND_MAP.createCallbackForAction(ctx -> {ctx.getClient().setScreen(ChunkAssistant.mapScreen); return ActionResult.SUCCESS;});

//        RenderHandler renderer = new RenderHandler();
//        Registry.RENDER_EVENT_DISPATCHER.registerWorldPostRenderer(renderer);
//        Registry.TICK_EVENT_DISPATCHER.registerClientTickHandler(new ClientTickHandler());
    }
}
