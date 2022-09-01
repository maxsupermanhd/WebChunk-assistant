package net.maxsupermanhd.WebChunkAssistant.config;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.category.BaseConfigOptionCategory;
import fi.dy.masa.malilib.config.category.ConfigOptionCategory;
import fi.dy.masa.malilib.config.option.BooleanConfig;
import fi.dy.masa.malilib.config.option.ConfigOption;
import fi.dy.masa.malilib.config.option.IntegerConfig;
import fi.dy.masa.malilib.config.option.StringConfig;
import net.maxsupermanhd.WebChunkAssistant.misc.Reference;

import java.util.List;

public class Config {

    public static class Map {
        public static final BooleanConfig MAP_REQUEST_CACHE = new BooleanConfig("requestCache", true);
        public static final IntegerConfig ZOOM_BREAK_LOW = new IntegerConfig("mapBreakLow", 192);
        public static final IntegerConfig ZOOM_BREAK_HIGH = new IntegerConfig("mapBreakHigh", 320);
        public static final IntegerConfig REQUEST_POOL_SIZE = new IntegerConfig("requestPoolSize", 4);
        public static final ImmutableList<ConfigOption<?>> OPTIONS = ImmutableList.of(
                MAP_REQUEST_CACHE,
                ZOOM_BREAK_LOW,
                ZOOM_BREAK_HIGH,
                REQUEST_POOL_SIZE
        );
    }

    public static class Server {
        public static final StringConfig BASE_URL = new StringConfig("serverURL", "http://localhost:8261/");
        public static final StringConfig ENDPOINT_SUBMIT = new StringConfig("endpointSubmit", "api/submit/chunk/%1$s");
        public static final StringConfig ENDPOINT_DIMS = new StringConfig("endpointGetDims", "api/dims");
        public static final StringConfig ENDPOINT_RENDERERS = new StringConfig("endpointGetRenderers", "api/renderers");
        public static final StringConfig ENDPOINT_MAP_DATA = new StringConfig("endpointMapData", "worlds/%s/%s/tiles/%s/%d/%d/%d/png");
        public static final ImmutableList<ConfigOption<?>> OPTIONS = ImmutableList.of(
                BASE_URL,
                ENDPOINT_SUBMIT,
                ENDPOINT_DIMS,
                ENDPOINT_RENDERERS,
                ENDPOINT_MAP_DATA
        );
    }

    public static class Submission {
        public static final BooleanConfig SUBMIT_DATA = new BooleanConfig("submit", true);
        public static final StringConfig OVERRIDE_WORLD = new StringConfig("overrideWorld", "");
        public static final StringConfig OVERRIDE_DIMENSION = new StringConfig("overrideDimension", "");
        public static final ImmutableList<ConfigOption<?>> OPTIONS = ImmutableList.of(
                SUBMIT_DATA,
                OVERRIDE_WORLD,
                OVERRIDE_DIMENSION
        );
    }
    public static class Overlay {
        public static final BooleanConfig ENABLED = new BooleanConfig("overlayEnabled", true);
        public static final IntegerConfig OFFSET_X = new IntegerConfig("overlayOffsetX", 10);
        public static final IntegerConfig OFFSET_Y = new IntegerConfig("overlayOffsetY", 10);
        public static final IntegerConfig LINE_SPACING = new IntegerConfig("overlaySpacing", 10);
        public static final ImmutableList<ConfigOption<?>> OPTIONS = ImmutableList.of(
                ENABLED,
                OFFSET_X,
                OFFSET_Y,
                LINE_SPACING
        );
    }
    public static final List<ConfigOptionCategory> CATEGORIES = ImmutableList.of(
            BaseConfigOptionCategory.normal(Reference.MOD_INFO, "Map", Map.OPTIONS),
            BaseConfigOptionCategory.normal(Reference.MOD_INFO, "Server", Server.OPTIONS),
            BaseConfigOptionCategory.normal(Reference.MOD_INFO, "Submission", Submission.OPTIONS),
            BaseConfigOptionCategory.normal(Reference.MOD_INFO, "Overlay", Overlay.OPTIONS),
            BaseConfigOptionCategory.normal(Reference.MOD_INFO, "Hotkeys", Hotkeys.HOTKEY_LIST)
    );
}
