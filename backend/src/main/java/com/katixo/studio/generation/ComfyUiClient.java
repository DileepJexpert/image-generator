package com.katixo.studio.generation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.katixo.studio.config.KatixoProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.function.IntConsumer;

/**
 * Talks to ComfyUI (CLAUDE.md section 6). Injects params into a workflow JSON
 * template, POSTs the graph to {@code /prompt}, mirrors ComfyUI's {@code /ws}
 * progress onto a callback, then fetches the produced image via
 * {@code /history} + {@code /view}. Node graph knowledge stays in the template;
 * Java only knows token names.
 */
@Component
public class ComfyUiClient {

    private static final Logger log = LoggerFactory.getLogger(ComfyUiClient.class);

    private static final String WORKFLOW_TEXT2IMG =
            "com/katixo/studio/generation/workflows/text2img_sdxl.json";

    private static final Duration COMPLETION_TIMEOUT = Duration.ofMinutes(10);
    private static final Duration POLL_INTERVAL = Duration.ofMillis(750);

    private final KatixoProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public ComfyUiClient(KatixoProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Run the text-to-image workflow. {@code onProgress} receives 0..100 as
     * ComfyUI reports sampling progress.
     */
    public ComfyImageResult generateImage(ImageGenerationParams params, IntConsumer onProgress)
            throws IOException, InterruptedException {

        JsonNode graph = buildGraph(WORKFLOW_TEXT2IMG, Map.of(
                "{{PROMPT}}", params.prompt(),
                "{{NEGATIVE_PROMPT}}", params.negativePrompt() == null ? "" : params.negativePrompt(),
                "{{WIDTH}}", params.width(),
                "{{HEIGHT}}", params.height(),
                "{{SEED}}", params.seed(),
                "{{CKPT_NAME}}", params.checkpoint()
        ));

        String clientId = UUID.randomUUID().toString();
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

    // --- template handling ---------------------------------------------------

    private JsonNode buildGraph(String resource, Map<String, Object> tokens) throws IOException {
        JsonNode template;
        try (var in = new ClassPathResource(resource).getInputStream()) {
            template = objectMapper.readTree(in);
        }
        substituteTokens(template, tokens);
        return template;
    }

    /** Replace any string node whose text is a known token with the typed value. */
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

    // --- ComfyUI HTTP / WS ---------------------------------------------------

    private String submitPrompt(JsonNode graph, String clientId) throws IOException, InterruptedException {
        ObjectNode body = objectMapper.createObjectNode();
        body.set("prompt", graph);
        body.put("client_id", clientId);

        HttpRequest request = HttpRequest.newBuilder(URI.create(httpBase() + "/prompt"))
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
            // Progress streaming is best-effort; completion is detected via /history.
            log.warn("Could not open ComfyUI progress socket: {}", e.getMessage());
            return null;
        }
    }

    /** Poll /history until the prompt has recorded outputs (source of truth for completion). */
    private JsonNode awaitOutputs(String promptId) throws IOException, InterruptedException {
        long deadline = System.nanoTime() + COMPLETION_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(httpBase() + "/history/" + promptId))
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
                String url = UriComponentsBuilder.fromHttpUrl(httpBase() + "/view")
                        .queryParam("filename", image.path("filename").asText())
                        .queryParam("subfolder", image.path("subfolder").asText(""))
                        .queryParam("type", image.path("type").asText("output"))
                        .toUriString();
                HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
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

    private String httpBase() {
        return properties.comfyuiUrl().replaceAll("/+$", "");
    }

    private String wsBase() {
        return httpBase().replaceFirst("^http", "ws");
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
                // Ignore non-JSON / unexpected frames; progress is best-effort.
            }
        }
    }
}
