package net.maxsupermanhd.WebChunkAssistant.config;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.option.ConfigInfo;
import fi.dy.masa.malilib.config.util.ConfigUtils;
import fi.dy.masa.malilib.gui.config.BaseConfigScreen;
import fi.dy.masa.malilib.gui.config.BaseConfigTab;
import fi.dy.masa.malilib.gui.config.ConfigTab;
import fi.dy.masa.malilib.gui.tab.ScreenTab;
import net.maxsupermanhd.WebChunkAssistant.misc.Reference;
import net.minecraft.client.gui.screen.Screen;

import javax.annotation.Nullable;
import java.util.ArrayList;

public class ConfigScreen {
    public static final BaseConfigTab MAP = new BaseConfigTab(Reference.MOD_INFO, "map", 160, Config.Map.OPTIONS, ConfigScreen::create);
    public static final BaseConfigTab SERVER = new BaseConfigTab(Reference.MOD_INFO, "server", 160, Config.Server.OPTIONS, ConfigScreen::create);
    public static final BaseConfigTab SUBMISSION = new BaseConfigTab(Reference.MOD_INFO, "submission", 160, Config.Submission.OPTIONS, ConfigScreen::create);
    public static final BaseConfigTab OVERLAY = new BaseConfigTab(Reference.MOD_INFO, "overlay", 160, Config.Overlay.OPTIONS, ConfigScreen::create);
    public static final BaseConfigTab HOTKEYS = new BaseConfigTab(Reference.MOD_INFO, "hotkeys", 160, getHotkeys(), ConfigScreen::create);

    public static ImmutableList<ConfigTab> getConfigTabs()
    {
        return CONFIG_TABS;
    }

    public static final ImmutableList<ConfigTab> CONFIG_TABS = ImmutableList.of(
            MAP,
            SERVER,
            SUBMISSION,
            OVERLAY,
            HOTKEYS
    );
    public static final ImmutableList<ScreenTab> ALL_TABS = ImmutableList.of(
            MAP,
            SERVER,
            SUBMISSION,
            OVERLAY,
            HOTKEYS
    );
    public static BaseConfigScreen create() {
        return new BaseConfigScreen(Reference.MOD_INFO, ALL_TABS, MAP, "webchunkassistant.configScreen", Reference.MOD_VERSION);
    }
    public static BaseConfigScreen create(@Nullable Screen currentScreen) {
        return new BaseConfigScreen(Reference.MOD_INFO, ALL_TABS, MAP, "webchunkassistant.configScreen", Reference.MOD_VERSION);
    }
    public static BaseConfigScreen createOnTab(ConfigTab tab) {
        BaseConfigScreen screen = create();
        screen.setCurrentTab(tab);
        return screen;
    }

    private static ImmutableList<ConfigInfo> getHotkeys() {
        ArrayList<ConfigInfo> list = new ArrayList<>(Hotkeys.HOTKEY_LIST);
        ConfigUtils.sortConfigsByDisplayName(list);
        return ImmutableList.copyOf(list);
    }
}
