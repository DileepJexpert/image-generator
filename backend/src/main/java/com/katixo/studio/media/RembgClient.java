package com.katixo.studio.media;

import com.katixo.ai.commons.gpu.GpuResourceGuard;
import com.katixo.ai.commons.sidecar.SidecarClient;
import com.katixo.ai.commons.sidecar.SidecarConfig;
import com.katixo.ai.commons.sidecar.SidecarHealth;
import com.katixo.studio.config.GpuCalls;
import com.katixo.studio.config.KatixoProperties;
import com.katixo.studio.config.Probes;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Calls the rembg background-removal sidecar over HTTP (CLAUDE.md §6). Contract:
 * {@code POST {REMBG_URL}/remove} with a multipart {@code file} field, returns
 * the cutout as PNG bytes.
 *
 * <p>rembg runs on the GPU, so the call is wrapped in the shared {@link GpuResourceGuard}: only one
 * GPU job (here or in katixo-docai) runs at a time. Extends the platform {@link SidecarClient} base.
 */
@Component
public class RembgClient extends SidecarClient {

    private final HttpClient httpClient;
    private final GpuResourceGuard gpuGuard;

    public RembgClient(KatixoProperties properties, GpuResourceGuard gpuGuard) {
        super(properties.rembgUrl(), SidecarConfig.noRetry("rembg",
                Duration.ofSeconds(10), Duration.ofMinutes(5)));
        this.gpuGuard = gpuGuard;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public byte[] removeBackground(byte[] input) throws IOException, InterruptedException {
        return GpuCalls.guarded(gpuGuard, "remove_bg", () -> doRemoveBackground(input));
    }

    private byte[] doRemoveBackground(byte[] input) throws IOException, InterruptedException {
        MultipartBody body = new MultipartBody("file", "input.png", "image/png", input);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url("/remove")))
                .header("Content-Type", body.contentType())
                .timeout(Duration.ofMinutes(5))
                .POST(body.publisher())
                .build();

        HttpResponse<byte[]> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("rembg /remove failed (" + response.statusCode() + ")");
        }
        return response.body();
    }

    @Override
    public SidecarHealth probe() {
        return Probes.reachable(httpClient, baseUrl, config.name());
    }
}
