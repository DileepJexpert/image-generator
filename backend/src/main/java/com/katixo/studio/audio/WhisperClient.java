package com.katixo.studio.audio;

import com.katixo.studio.config.KatixoProperties;
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
 */
@Component
public class WhisperClient {

    private final KatixoProperties properties;
    private final HttpClient httpClient;

    public WhisperClient(KatixoProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /** Transcribe audio bytes; returns the raw transcript JSON from the sidecar. */
    public byte[] transcribe(byte[] audio, String filename) throws IOException, InterruptedException {
        MultipartBody body = new MultipartBody("file", filename, "audio/wav", audio);
        HttpRequest request = HttpRequest.newBuilder(URI.create(base() + "/transcribe"))
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

    private String base() {
        return properties.whisperUrl().replaceAll("/+$", "");
    }
}
