package com.katixo.studio.copilot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.katixo.ai.commons.gpu.GpuResourceGuard;
import com.katixo.ai.commons.sidecar.SidecarClient;
import com.katixo.ai.commons.sidecar.SidecarConfig;
import com.katixo.ai.commons.sidecar.SidecarHealth;
import com.katixo.studio.config.GpuCalls;
import com.katixo.studio.config.KatixoProperties;
import com.katixo.studio.config.Probes;
import com.katixo.studio.copilot.CopilotDtos.ModelSummary;
import com.katixo.studio.copilot.agent.AssistantTurn;
import com.katixo.studio.copilot.agent.AssistantTurn.ToolCall;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Talks to the local Ollama LLM sidecar (CLAUDE.md §3: a sidecar we only call
 * over HTTP, never author). Wraps Ollama's {@code /api/chat} and
 * {@code /api/tags}, with both a buffered ({@link #chat}) and a token-streaming
 * ({@link #chatStream}) variant.
 *
 * <p>Note on the async-job directive: heavy GPU <em>generation</em> always goes
 * through the job queue. The Copilot is an interactive request/response tool,
 * so it answers inline like the other sidecar clients here.
 *
 * <p>Every {@code /api/chat} call is GPU work, so it runs through the shared
 * {@link GpuResourceGuard} (so a copilot turn and a katixo-docai extraction can't hit the GPU at
 * once). {@link #listModels} is metadata ({@code /api/tags}) and is not guarded. Extends the platform
 * {@link SidecarClient} base.
 */
@Component
public class OllamaClient extends SidecarClient {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final GpuResourceGuard gpuGuard;

    public OllamaClient(KatixoProperties properties, ObjectMapper objectMapper, GpuResourceGuard gpuGuard) {
        super(properties.ollamaUrl(), SidecarConfig.noRetry("ollama-copilot",
                Duration.ofSeconds(10), Duration.ofMinutes(5)));
        this.objectMapper = objectMapper;
        this.gpuGuard = gpuGuard;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /** Runs a chat completion and returns the assistant message content. */
    public String chat(String model, List<ChatMessage> messages)
            throws IOException, InterruptedException {
        return GpuCalls.guarded(gpuGuard, "copilot-chat", () -> doChat(model, messages));
    }

    private String doChat(String model, List<ChatMessage> messages)
            throws IOException, InterruptedException {

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("stream", false);
        ArrayNode msgs = body.putArray("messages");
        for (ChatMessage m : messages) {
            ObjectNode node = msgs.addObject();
            node.put("role", m.role());
            node.put("content", m.content());
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(url("/api/chat")))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMinutes(5))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("Ollama /api/chat failed (" + response.statusCode()
                    + "): " + response.body());
        }
        JsonNode json = objectMapper.readTree(response.body());
        return json.path("message").path("content").asText();
    }

    /** Streaming chat; the GPU guard is held for the whole stream. */
    public void chatStream(String model, List<ChatMessage> messages, Consumer<String> onToken)
            throws IOException, InterruptedException {
        GpuCalls.guarded(gpuGuard, "copilot-chat-stream", () -> {
            doChatStream(model, messages, onToken);
            return null;
        });
    }

    private void doChatStream(String model, List<ChatMessage> messages, Consumer<String> onToken)
            throws IOException, InterruptedException {

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("stream", true);
        ArrayNode msgs = body.putArray("messages");
        for (ChatMessage m : messages) {
            ObjectNode node = msgs.addObject();
            node.put("role", m.role());
            node.put("content", m.content());
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(url("/api/chat")))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMinutes(5))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        HttpResponse<Stream<String>> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofLines());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("Ollama /api/chat (stream) failed (" + response.statusCode() + ")");
        }
        try (Stream<String> lines = response.body()) {
            for (String line : (Iterable<String>) lines::iterator) {
                if (line.isBlank()) {
                    continue;
                }
                JsonNode node = objectMapper.readTree(line);
                String token = node.path("message").path("content").asText("");
                if (!token.isEmpty()) {
                    onToken.accept(token);
                }
                if (node.path("done").asBoolean(false)) {
                    break;
                }
            }
        }
    }

    /** Tool-enabled chat turn (buffered). Guarded as a single GPU call. */
    public AssistantTurn chatWithTools(String model, ArrayNode messages, ArrayNode tools)
            throws IOException, InterruptedException {
        return GpuCalls.guarded(gpuGuard, "copilot-chat-tools", () -> doChatWithTools(model, messages, tools));
    }

    private AssistantTurn doChatWithTools(String model, ArrayNode messages, ArrayNode tools)
            throws IOException, InterruptedException {

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("stream", false);
        body.set("messages", messages);
        body.set("tools", tools);
        body.putObject("options").put("temperature", 0.1);

        HttpRequest request = HttpRequest.newBuilder(URI.create(url("/api/chat")))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMinutes(5))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("Ollama /api/chat (tools) failed (" + response.statusCode()
                    + "): " + response.body());
        }

        JsonNode message = objectMapper.readTree(response.body()).path("message");
        String content = message.path("content").asText("");
        List<ToolCall> calls = new ArrayList<>();
        for (JsonNode call : message.path("tool_calls")) {
            JsonNode fn = call.path("function");
            String name = fn.path("name").asText();
            if (name.isBlank()) {
                continue;
            }
            calls.add(new ToolCall(name, parseArguments(fn.path("arguments"))));
        }
        return new AssistantTurn(message, content, calls);
    }

    private JsonNode parseArguments(JsonNode arguments) {
        if (arguments.isTextual()) {
            try {
                return objectMapper.readTree(arguments.asText());
            } catch (IOException e) {
                return objectMapper.createObjectNode();
            }
        }
        return arguments.isMissingNode() || arguments.isNull()
                ? objectMapper.createObjectNode()
                : arguments;
    }

    /** Lists models pulled into the local Ollama instance. Metadata only — not a GPU call. */
    public List<ModelSummary> listModels() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url("/api/tags")))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("Ollama /api/tags failed (" + response.statusCode() + ")");
        }
        JsonNode json = objectMapper.readTree(response.body());
        List<ModelSummary> models = new ArrayList<>();
        for (JsonNode node : json.path("models")) {
            models.add(new ModelSummary(
                    node.path("name").asText(),
                    node.path("size").asLong(0)));
        }
        return models;
    }

    @Override
    public SidecarHealth probe() {
        return Probes.reachable(httpClient, baseUrl, config.name());
    }
}
