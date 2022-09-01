package net.maxsupermanhd.WebChunkAssistant.config;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.option.HotkeyConfig;

import java.util.List;

public class Hotkeys {
    public static final HotkeyConfig KEYBIND_SETTINGS = new HotkeyConfig("openSettings", "Shift,M");
    public static final HotkeyConfig KEYBIND_MAP = new HotkeyConfig("openMap", "M");
    public static final HotkeyConfig KEYBIND_TOGGLE = new HotkeyConfig("toggle", "M,T");

    public static final List<HotkeyConfig> HOTKEY_LIST = ImmutableList.of(
            KEYBIND_SETTINGS,
            KEYBIND_MAP,
            KEYBIND_TOGGLE
    );
}
