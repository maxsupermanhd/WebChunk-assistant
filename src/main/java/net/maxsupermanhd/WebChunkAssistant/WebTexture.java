package net.maxsupermanhd.WebChunkAssistant;

import net.minecraft.client.render.MapRenderer;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;

import java.net.URL;

public class WebTexture implements AutoCloseable {
    private final NativeImageBackedTexture texture;

    WebTexture(URL addr, int resolution, String prefix, TextureManager manager) {
        this.texture = new NativeImageBackedTexture(resolution, resolution, true);
        Identifier identifier = manager.registerDynamicTexture(prefix, this.texture);
    }

    @Override
    public void close() {
        this.texture.close();
    }
}
