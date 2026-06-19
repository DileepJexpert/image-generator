package com.katixo.studio.audio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.katixo.studio.config.KatixoProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Calls the text-to-speech sidecar (Piper) over HTTP (CLAUDE.md §6). Contract:
 * {@code POST {TTS_URL}/speak} with JSON {@code {text, voice?}}, returns the
 * synthesized clip as WAV bytes.
 */
@Component
public class TtsClient {

    private final KatixoProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public TtsClient(KatixoProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /** Synthesize {@code text} to speech; returns WAV bytes. */
    public byte[] synthesize(String text, String voice) throws IOException, InterruptedException {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("text", text);
        if (voice != null && !voice.isBlank()) {
            body.put("voice", voice);
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(base() + "/speak"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMinutes(5))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        HttpResponse<byte[]> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("tts /speak failed (" + response.statusCode() + ")");
        }
        return response.body();
    }

    private String base() {
        return properties.ttsUrl().replaceAll("/+$", "");
    }
}
