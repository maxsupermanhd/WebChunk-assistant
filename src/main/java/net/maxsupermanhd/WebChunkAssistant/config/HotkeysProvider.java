package net.maxsupermanhd.WebChunkAssistant.config;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.input.Hotkey;
import fi.dy.masa.malilib.input.HotkeyCategory;
import fi.dy.masa.malilib.input.HotkeyProvider;
import net.maxsupermanhd.WebChunkAssistant.misc.Reference;

import java.util.List;

public class HotkeysProvider implements HotkeyProvider {
    public static final ImmutableList<Hotkey> ALL_HOTKEYS = buildFullHotkeyList();
    public static final HotkeysProvider INSTANCE = new HotkeysProvider();
    private static ImmutableList<Hotkey> buildFullHotkeyList() {
        ImmutableList.Builder<Hotkey> builder = ImmutableList.builder();
        builder.addAll(Hotkeys.HOTKEY_LIST);
        return builder.build();
    }
    @Override
    public List<? extends Hotkey> getAllHotkeys() {
        return ALL_HOTKEYS;
    }
    @Override
    public List<HotkeyCategory> getHotkeysByCategories() {
        return ImmutableList.of(new HotkeyCategory(Reference.MOD_INFO, "webchunkassistant.hotkeys", Hotkeys.HOTKEY_LIST));
    }
}
