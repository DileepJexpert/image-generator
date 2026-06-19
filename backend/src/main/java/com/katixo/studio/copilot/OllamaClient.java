package com.katixo.studio.copilot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.katixo.studio.config.KatixoProperties;
import com.katixo.studio.copilot.CopilotDtos.ModelSummary;
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
 * Talks to the local Ollama LLM sidecar (CLAUDE.md §3: a sidecar we only call
 * over HTTP, never author). Wraps Ollama's {@code /api/chat} and
 * {@code /api/tags}. Chat is non-streaming for now — interactive turns finish
 * in seconds on small models; token streaming is a follow-up.
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
