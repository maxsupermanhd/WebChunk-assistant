package net.maxsupermanhd.WebChunkAssistant;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class WebTexture implements Runnable, AutoCloseable {
    public Identifier id;
    public String status = "init";
    public URI uri;
    public boolean disableCache = false;
    public String[] headers;
    NativeImage img = null;

    WebTexture(URL addr, Identifier id, String[] headers) throws URISyntaxException {
        this.id = id;
        this.uri = addr.toURI();
        this.headers = headers;
    }

    public void run() {
        status = "allocating";
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(this.uri);
        builder.header("Accept", "image/png");
        builder.headers(headers);
        builder.method("GET", HttpRequest.BodyPublishers.noBody());
        HttpResponse<InputStream> res = null;
        status = "requesting";
        try {
            res = HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
        } catch (Exception e) {
            e.printStackTrace();
            status = e.toString();
            return;
        }
        if(res.statusCode() != 200) {
            if(res.statusCode() == 404) {
                status = "empty";
                return;
            }
            status = String.valueOf(res.statusCode());
            return;
        }
        status = "parsing";
        try {
            img = NativeImage.read(res.body());
        } catch (Exception e) {
            e.printStackTrace();
            status = e.toString();
            return;
        }
        status = "registering";
    }

    @Override
    public void close() {
        img.close();
    }
}
