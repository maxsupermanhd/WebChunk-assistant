package net.maxsupermanhd.TerrainReporter;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import net.minecraft.client.gui.screen.Screen;

import java.util.ArrayList;
import java.util.List;

@Config(name = "terrainreporter")
public class TerrainReporterConfig implements ConfigData {
    public boolean enabled = false;
    String baseurl = "http://localhost:8261/api/submit/chunk/";
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
