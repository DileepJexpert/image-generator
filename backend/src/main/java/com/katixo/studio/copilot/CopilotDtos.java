package com.katixo.studio.copilot;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request/response payloads for the Copilot chat API (CLAUDE.md §6 conventions:
 * DTOs only at the boundary). Grouped here since each is small.
 */
public final class CopilotDtos {

    private CopilotDtos() {
    }

    /**
     * A chat turn submission. {@code messages} is the conversation so far
     * (the frontend owns history; the backend is stateless). {@code model} is
     * optional — falls back to {@code katixo.copilot-model} when blank.
     */
    public record ChatRequest(
            @NotEmpty List<ChatMessage> messages,
            String model
    ) {
    }

    /** The assistant's reply plus the model that produced it. */
    public record ChatResponse(String model, ChatMessage message) {
    }

    /** One streamed content chunk, sent as an SSE {@code data:} JSON event. */
    public record TokenEvent(String token) {
    }

    /** A model available in the local Ollama instance. */
    public record ModelSummary(String name, long sizeBytes) {
    }
}
