package com.katixo.studio.copilot.agent;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * One assistant turn from Ollama's tool-enabled {@code /api/chat}. Carries the
 * raw message node (re-sent verbatim on the next loop iteration so {@code
 * tool_calls} round-trip exactly) plus the parsed content and any tool calls.
 */
public record AssistantTurn(JsonNode rawMessage, String content, List<ToolCall> toolCalls) {

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }

    /** A single tool invocation requested by the model. */
    public record ToolCall(String name, JsonNode arguments) {
    }
}
