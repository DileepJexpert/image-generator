package com.katixo.studio.copilot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.katixo.studio.config.KatixoProperties;
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
 */
@Component
public class OllamaClient {

    private final KatixoProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OllamaClient(KatixoProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /** Runs a chat completion and returns the assistant message content. */
    public String chat(String model, List<ChatMessage> messages)
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

        HttpRequest request = HttpRequest.newBuilder(URI.create(base() + "/api/chat"))
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

    /**
     * Runs a streaming chat completion, invoking {@code onToken} for each
     * content chunk as Ollama emits it (NDJSON: one JSON object per line). The
     * call blocks on the worker thread until the stream finishes.
     */
    public void chatStream(String model, List<ChatMessage> messages, Consumer<String> onToken)
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

        HttpRequest request = HttpRequest.newBuilder(URI.create(base() + "/api/chat"))
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

    /**
     * Runs one buffered (non-streaming) tool-enabled chat turn. The model may
     * answer with plain content or request one or more tool calls
     * ({@code message.tool_calls}). Tool turns must be buffered — streaming +
     * tool calls together is not yet reliable across Ollama models.
     *
     * @param messages pre-built conversation array (system/user/assistant/tool),
     *                 owned and mutated by the agent loop
     * @param tools    the tool specs the model may call
     */
    public AssistantTurn chatWithTools(String model, ArrayNode messages, ArrayNode tools)
            throws IOException, InterruptedException {

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("stream", false);
        body.set("messages", messages);
        body.set("tools", tools);
        // Tool selection is far more reliable at low temperature on local models.
        body.putObject("options").put("temperature", 0.1);

        HttpRequest request = HttpRequest.newBuilder(URI.create(base() + "/api/chat"))
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

    /**
     * Ollama usually returns tool-call arguments as a JSON object, but some
     * models emit a JSON string; accept both so the agent gets a real node.
     */
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

    /** Lists models pulled into the local Ollama instance. */
    public List<ModelSummary> listModels() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(base() + "/api/tags"))
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

    private String base() {
        return properties.ollamaUrl().replaceAll("/+$", "");
    }
}
