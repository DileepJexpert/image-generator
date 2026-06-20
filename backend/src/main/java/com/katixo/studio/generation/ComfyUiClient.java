package com.katixo.studio.generation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.katixo.ai.commons.gpu.GpuResourceGuard;
import com.katixo.ai.commons.sidecar.SidecarClient;
import com.katixo.ai.commons.sidecar.SidecarConfig;
import com.katixo.ai.commons.sidecar.SidecarHealth;
import com.katixo.studio.config.GpuCalls;
import com.katixo.studio.config.KatixoProperties;
import com.katixo.studio.config.Probes;
import com.katixo.studio.media.MultipartBody;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.IntConsumer;

/**
 * Talks to ComfyUI (CLAUDE.md section 6). Injects params into a workflow JSON
 * template, POSTs the graph to {@code /prompt}, mirrors ComfyUI's {@code /ws}
 * progress onto a callback, then fetches the produced image via
 * {@code /history} + {@code /view}. Node graph knowledge stays in the template;
 * Java only knows token names.
 *
 * <p>ComfyUI is the heaviest GPU consumer, so each generation is wrapped in the shared
 * {@link GpuResourceGuard}: the lock is held for the whole render so this app and katixo-docai never
 * use the single GPU at the same time. Extends the platform {@link SidecarClient} base.
 */
@Component
public class ComfyUiClient extends SidecarClient {

    private static final String WORKFLOW_TEXT2IMG =
            "com/katixo/studio/generation/workflows/text2img.json";
    private static final String WORKFLOW_IMG2VIDEO =
            "com/katixo/studio/generation/workflows/img2video_ltx.json";

    private static final Duration COMPLETION_TIMEOUT = Duration.ofMinutes(10);
    private static final Duration POLL_INTERVAL = Duration.ofMillis(750);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final GpuResourceGuard gpuGuard;

    public ComfyUiClient(KatixoProperties properties, ObjectMapper objectMapper, GpuResourceGuard gpuGuard) {
        super(properties.comfyuiUrl(), SidecarConfig.noRetry("comfyui",
                Duration.ofSeconds(10), COMPLETION_TIMEOUT));
        this.objectMapper = objectMapper;
        this.gpuGuard = gpuGuard;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /** Text-to-image; holds the GPU guard for the whole render. */
    public ComfyImageResult generateImage(ImageGenerationParams params, IntConsumer onProgress)
            throws IOException, InterruptedException {
        return GpuCalls.guarded(gpuGuard, "image", () -> doGenerateImage(params, onProgress));
    }

    private ComfyImageResult doGenerateImage(ImageGenerationParams params, IntConsumer onProgress)
            throws IOException, InterruptedException {

        JsonNode graph = buildGraph(WORKFLOW_TEXT2IMG, Map.of(
                "{{PROMPT}}", params.prompt(),
                "{{NEGATIVE_PROMPT}}", params.negativePrompt() == null ? "" : params.negativePrompt(),
                "{{WIDTH}}", params.width(),
                "{{HEIGHT}}", params.height(),
                "{{SEED}}", params.seed(),
                "{{CKPT_NAME}}", params.checkpoint()
        ));

        String clientId = newIdempotencyKey();
        WebSocket progressSocket = openProgressSocket(clientId, onProgress);
        try {
            String promptId = submitPrompt(graph, clientId);
            JsonNode outputs = awaitOutputs(promptId);
            byte[] bytes = fetchFirstImage(outputs);
            onProgress.accept(100);
            return new ComfyImageResult(bytes, "image/png");
        } finally {
            if (progressSocket != null) {
                progressSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done");
            }
        }
    }

    /** Image-to-video; holds the GPU guard for the whole render. */
    public ComfyVideoResult generateVideo(VideoGenerationParams params, IntConsumer onProgress)
            throws IOException, InterruptedException {
        return GpuCalls.guarded(gpuGuard, "image_to_video", () -> doGenerateVideo(params, onProgress));
    }

    private ComfyVideoResult doGenerateVideo(VideoGenerationParams params, IntConsumer onProgress)
            throws IOException, InterruptedException {

        String imageName = uploadImage(params.sourceImage(), "katixo_src.png");

        JsonNode graph = buildGraph(WORKFLOW_IMG2VIDEO, Map.of(
                "{{IMAGE_NAME}}", imageName,
                "{{PROMPT}}", params.prompt() == null ? "" : params.prompt(),
                "{{NEGATIVE_PROMPT}}", params.negativePrompt() == null ? "" : params.negativePrompt(),
                "{{WIDTH}}", params.width(),
                "{{HEIGHT}}", params.height(),
                "{{FRAMES}}", params.frames(),
                "{{FPS}}", params.fps(),
                "{{SEED}}", params.seed()
        ));

        String clientId = newIdempotencyKey();
        WebSocket progressSocket = openProgressSocket(clientId, onProgress);
        try {
            String promptId = submitPrompt(graph, clientId);
            JsonNode outputs = awaitOutputs(promptId);
            ComfyVideoResult result = fetchFirstVideo(outputs);
            onProgress.accept(100);
            return result;
        } finally {
            if (progressSocket != null) {
                progressSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done");
            }
        }
    }

    /** Uploads an image to ComfyUI's input dir; returns the stored image name. */
    private String uploadImage(byte[] bytes, String filename) throws IOException, InterruptedException {
        MultipartBody body = new MultipartBody("image", filename, "image/png", bytes);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url("/upload/image")))
                .header("Content-Type", body.contentType())
                .POST(body.publisher())
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("ComfyUI /upload/image failed (" + response.statusCode() + "): " + response.body());
        }
        JsonNode json = objectMapper.readTree(response.body());
        String name = json.path("name").asText();
        String subfolder = json.path("subfolder").asText("");
        return subfolder.isBlank() ? name : subfolder + "/" + name;
    }

    private JsonNode buildGraph(String resource, Map<String, Object> tokens) throws IOException {
        JsonNode template;
        try (var in = new ClassPathResource(resource).getInputStream()) {
            template = objectMapper.readTree(in);
        }
        substituteTokens(template, tokens);
        return template;
    }

    private void substituteTokens(JsonNode node, Map<String, Object> tokens) {
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            Iterator<String> names = obj.fieldNames();
            while (names.hasNext()) {
                String field = names.next();
                JsonNode child = obj.get(field);
                if (child.isTextual() && tokens.containsKey(child.asText())) {
                    obj.set(field, objectMapper.valueToTree(tokens.get(child.asText())));
                } else {
                    substituteTokens(child, tokens);
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                substituteTokens(child, tokens);
            }
        }
    }

    private String submitPrompt(JsonNode graph, String clientId) throws IOException, InterruptedException {
        ObjectNode body = objectMapper.createObjectNode();
        body.set("prompt", graph);
        body.put("client_id", clientId);

        HttpRequest request = HttpRequest.newBuilder(URI.create(url("/prompt")))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("ComfyUI /prompt failed (" + response.statusCode() + "): " + response.body());
        }
        JsonNode json = objectMapper.readTree(response.body());
        JsonNode promptId = json.get("prompt_id");
        if (promptId == null || promptId.asText().isBlank()) {
            throw new IOException("ComfyUI /prompt returned no prompt_id: " + response.body());
        }
        return promptId.asText();
    }

    private WebSocket openProgressSocket(String clientId, IntConsumer onProgress) {
        try {
            URI wsUri = URI.create(wsBase() + "/ws?clientId=" + clientId);
            return httpClient.newWebSocketBuilder()
                    .buildAsync(wsUri, new ProgressListener(onProgress))
                    .join();
        } catch (Exception e) {
            log.warn("Could not open ComfyUI progress socket: {}", e.getMessage());
            return null;
        }
    }

    private JsonNode awaitOutputs(String promptId) throws IOException, InterruptedException {
        long deadline = System.nanoTime() + COMPLETION_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url("/history/" + promptId)))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 == 2) {
                JsonNode history = objectMapper.readTree(response.body());
                JsonNode entry = history.get(promptId);
                if (entry != null && entry.has("outputs")) {
                    return entry.get("outputs");
                }
            }
            Thread.sleep(POLL_INTERVAL.toMillis());
        }
        throw new IOException("Timed out waiting for ComfyUI prompt " + promptId);
    }

    private byte[] fetchFirstImage(JsonNode outputs) throws IOException, InterruptedException {
        for (JsonNode nodeOutput : outputs) {
            JsonNode images = nodeOutput.get("images");
            if (images != null && images.isArray() && !images.isEmpty()) {
                JsonNode image = images.get(0);
                String viewUrl = UriComponentsBuilder.fromHttpUrl(url("/view"))
                        .queryParam("filename", image.path("filename").asText())
                        .queryParam("subfolder", image.path("subfolder").asText(""))
                        .queryParam("type", image.path("type").asText("output"))
                        .toUriString();
                HttpRequest request = HttpRequest.newBuilder(URI.create(viewUrl)).GET().build();
                HttpResponse<byte[]> response =
                        httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() / 100 != 2) {
                    throw new IOException("ComfyUI /view failed (" + response.statusCode() + ")");
                }
                return response.body();
            }
        }
        throw new IOException("ComfyUI produced no images");
    }

    private ComfyVideoResult fetchFirstVideo(JsonNode outputs) throws IOException, InterruptedException {
        for (JsonNode nodeOutput : outputs) {
            JsonNode media = nodeOutput.get("gifs");
            if (media == null || !media.isArray() || media.isEmpty()) {
                media = nodeOutput.get("images");
            }
            if (media != null && media.isArray() && !media.isEmpty()) {
                JsonNode item = media.get(0);
                String viewUrl = UriComponentsBuilder.fromHttpUrl(url("/view"))
                        .queryParam("filename", item.path("filename").asText())
                        .queryParam("subfolder", item.path("subfolder").asText(""))
                        .queryParam("type", item.path("type").asText("output"))
                        .toUriString();
                HttpRequest request = HttpRequest.newBuilder(URI.create(viewUrl)).GET().build();
                HttpResponse<byte[]> response =
                        httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() / 100 != 2) {
                    throw new IOException("ComfyUI /view (video) failed (" + response.statusCode() + ")");
                }
                String format = item.path("format").asText("");
                String mime = format.contains("mp4") || format.isBlank() ? "video/mp4" : format;
                return new ComfyVideoResult(response.body(), mime);
            }
        }
        throw new IOException("ComfyUI produced no video");
    }

    private String wsBase() {
        return baseUrl.replaceFirst("^http", "ws");
    }

    @Override
    public SidecarHealth probe() {
        return Probes.reachable(httpClient, baseUrl, config.name());
    }

    /** Mirrors ComfyUI sampling progress onto the job callback. */
    private final class ProgressListener implements WebSocket.Listener {

        private final IntConsumer onProgress;
        private final StringBuilder buffer = new StringBuilder();

        private ProgressListener(IntConsumer onProgress) {
            this.onProgress = onProgress;
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buffer.append(data);
            webSocket.request(1);
            if (last) {
                String message = buffer.toString();
                buffer.setLength(0);
                handleMessage(message);
            }
            return null;
        }

        private void handleMessage(String message) {
            try {
                JsonNode json = objectMapper.readTree(message);
                if ("progress".equals(json.path("type").asText())) {
                    JsonNode payload = json.path("data");
                    int value = payload.path("value").asInt();
                    int max = payload.path("max").asInt(0);
                    if (max > 0) {
                        onProgress.accept((int) Math.round(value * 100.0 / max));
                    }
                }
            } catch (Exception e) {
                // best-effort progress
            }
        }
    }
}
