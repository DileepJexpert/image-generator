package com.katixo.studio.audio;

import com.katixo.ai.commons.gpu.GpuResourceGuard;
import com.katixo.ai.commons.sidecar.SidecarClient;
import com.katixo.ai.commons.sidecar.SidecarConfig;
import com.katixo.ai.commons.sidecar.SidecarHealth;
import com.katixo.studio.config.GpuCalls;
import com.katixo.studio.config.KatixoProperties;
import com.katixo.studio.config.Probes;
import com.katixo.studio.media.MultipartBody;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Calls the faster-whisper speech-to-text sidecar over HTTP (CLAUDE.md §6).
 * Contract: {@code POST {WHISPER_URL}/transcribe} with a multipart {@code file}
 * field (audio), returns a transcript JSON {@code {text, language, segments}}.
 *
 * <p>faster-whisper runs on the GPU, so the call is wrapped in the shared {@link GpuResourceGuard}.
 * Extends the platform {@link SidecarClient} base.
 */
@Component
public class WhisperClient extends SidecarClient {

    private final HttpClient httpClient;
    private final GpuResourceGuard gpuGuard;

    public WhisperClient(KatixoProperties properties, GpuResourceGuard gpuGuard) {
        super(properties.whisperUrl(), SidecarConfig.noRetry("whisper",
                Duration.ofSeconds(10), Duration.ofMinutes(10)));
        this.gpuGuard = gpuGuard;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /** Transcribe audio bytes; returns the raw transcript JSON from the sidecar. */
    public byte[] transcribe(byte[] audio, String filename) throws IOException, InterruptedException {
        return GpuCalls.guarded(gpuGuard, "transcribe", () -> doTranscribe(audio, filename));
    }

    private byte[] doTranscribe(byte[] audio, String filename) throws IOException, InterruptedException {
        MultipartBody body = new MultipartBody("file", filename, "audio/wav", audio);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url("/transcribe")))
                .header("Content-Type", body.contentType())
                .timeout(Duration.ofMinutes(10))
                .POST(body.publisher())
                .build();

        HttpResponse<byte[]> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("whisper /transcribe failed (" + response.statusCode() + ")");
        }
        return response.body();
    }

    @Override
    public SidecarHealth probe() {
        return Probes.reachable(httpClient, baseUrl, config.name());
    }
}
