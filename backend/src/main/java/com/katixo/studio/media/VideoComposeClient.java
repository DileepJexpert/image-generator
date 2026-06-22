package com.katixo.studio.media;

import com.katixo.studio.config.KatixoProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Calls the ffmpeg video-assembly sidecar over HTTP (CLAUDE.md §6). Contract:
 * {@code POST {FFMPEG_URL}/compose} with multipart {@code images} + {@code audios} (aligned by
 * order), returns the narrated slideshow as MP4 bytes — each image is shown for the length of its
 * audio clip, and the segments are concatenated.
 *
 * <p>ffmpeg is CPU-only, so this is not wrapped in the GPU guard. It is the assembly step for the
 * lesson feature's narrated-video output.
 */
@Component
public class VideoComposeClient {

    private final KatixoProperties properties;
    private final HttpClient httpClient;

    public VideoComposeClient(KatixoProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Assemble {@code images} + per-image {@code audios} into one MP4. The two lists must be the
     * same length and aligned (audio i narrates image i).
     */
    public byte[] compose(List<byte[]> images, List<byte[]> audios)
            throws IOException, InterruptedException {
        if (images.isEmpty() || images.size() != audios.size()) {
            throw new IllegalArgumentException("images and audios must be non-empty and the same size");
        }

        List<MultipartBody.Part> parts = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            parts.add(new MultipartBody.Part("images", "img" + i + ".png", "image/png", images.get(i)));
        }
        for (int i = 0; i < audios.size(); i++) {
            parts.add(new MultipartBody.Part("audios", "aud" + i + ".wav", "audio/wav", audios.get(i)));
        }
        MultipartBody body = new MultipartBody(parts);

        HttpRequest request = HttpRequest.newBuilder(URI.create(base() + "/compose"))
                .header("Content-Type", body.contentType())
                .timeout(Duration.ofMinutes(10))
                .POST(body.publisher())
                .build();

        HttpResponse<byte[]> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("ffmpeg /compose failed (" + response.statusCode() + ")");
        }
        return response.body();
    }

    private String base() {
        return properties.ffmpegUrl().replaceAll("/+$", "");
    }
}
