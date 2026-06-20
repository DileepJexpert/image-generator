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
 * Calls the Real-ESRGAN upscale sidecar over HTTP (CLAUDE.md §6). Contract:
 * {@code POST {ESRGAN_URL}/upscale?scale=2|4} with a multipart {@code file}
 * field, returns the upscaled image as PNG bytes.
 *
 * <p>Real-ESRGAN runs on the GPU, so the call is wrapped in the shared {@link GpuResourceGuard}.
 * Extends the platform {@link SidecarClient} base.
 */
@Component
public class EsrganClient extends SidecarClient {

    private final HttpClient httpClient;
    private final GpuResourceGuard gpuGuard;

    public EsrganClient(KatixoProperties properties, GpuResourceGuard gpuGuard) {
        super(properties.esrganUrl(), SidecarConfig.noRetry("esrgan",
                Duration.ofSeconds(10), Duration.ofMinutes(5)));
        this.gpuGuard = gpuGuard;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public byte[] upscale(byte[] input, int scale) throws IOException, InterruptedException {
        return GpuCalls.guarded(gpuGuard, "upscale", () -> doUpscale(input, scale));
    }

    private byte[] doUpscale(byte[] input, int scale) throws IOException, InterruptedException {
        MultipartBody body = new MultipartBody("file", "input.png", "image/png", input);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url("/upscale?scale=" + scale)))
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

    @Override
    public SidecarHealth probe() {
        return Probes.reachable(httpClient, baseUrl, config.name());
    }
}
