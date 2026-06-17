package com.katixo.studio.media;

import com.katixo.studio.config.KatixoProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Calls the Real-ESRGAN upscale sidecar over HTTP (CLAUDE.md §6). Contract:
 * {@code POST {ESRGAN_URL}/upscale?scale=2|4} with a multipart {@code file}
 * field, returns the upscaled image as PNG bytes.
 */
@Component
public class EsrganClient {

    private final KatixoProperties properties;
    private final HttpClient httpClient;

    public EsrganClient(KatixoProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public byte[] upscale(byte[] input, int scale) throws IOException, InterruptedException {
        MultipartBody body = new MultipartBody("file", "input.png", "image/png", input);
        HttpRequest request = HttpRequest.newBuilder(URI.create(base() + "/upscale?scale=" + scale))
                .header("Content-Type", body.contentType())
                .timeout(Duration.ofMinutes(5))
                .POST(body.publisher())
                .build();

        HttpResponse<byte[]> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("esrgan /upscale failed (" + response.statusCode() + ")");
        }
        return response.body();
    }

    private String base() {
        return properties.esrganUrl().replaceAll("/+$", "");
    }
}
