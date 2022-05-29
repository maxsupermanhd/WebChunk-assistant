package net.maxsupermanhd.WebChunkAssistant;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "terrainreporter")
public class ChunkAssistantConfig implements ConfigData {
    public boolean enabled = false;
    String baseurl = "http://localhost:8261/api/submit/chunk/%1$s";
    @ConfigEntry.Gui.PrefixText
    boolean render_overlay = true;
    int overlayX = 10;
    int overlayY = 10;
    int overlayO = 10;
    boolean report_to_chat = true;

    @ConfigEntry.Gui.CollapsibleObject
    lowElytraWarningConfig elytraWarning = new lowElytraWarningConfig();

    static class lowElytraWarningConfig {
        boolean lowElytraSoundEnabled = true;
        int lowElytraDurabilityLeft = 60;
        float lowElytraSoundVolume = 1.0f;
        float lowElytraSoundPitch = 1.0f;
    }
}
