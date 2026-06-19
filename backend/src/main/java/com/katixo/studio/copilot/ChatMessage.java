package com.katixo.studio.copilot;

/**
 * One turn in a Copilot conversation. {@code role} is {@code system},
 * {@code user}, or {@code assistant} — matching the Ollama chat schema so it
 * passes through to the LLM unchanged.
 */
public record ChatMessage(String role, String content) {

    public static ChatMessage system(String content) {
        return new ChatMessage("system", content);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage("assistant", content);
    }
}
