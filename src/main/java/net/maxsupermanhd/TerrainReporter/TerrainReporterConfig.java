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
    String baseurl = "localhost:8261/api/submit/chunk/";
    @ConfigEntry.Gui.PrefixText
    boolean render_overlay = true;
    int overlayX = 10;
    int overlayY = 10;
    int overlayO = 10;
    boolean report_to_chat = true;
}
