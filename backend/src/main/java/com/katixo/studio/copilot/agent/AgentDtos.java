package com.katixo.studio.copilot.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.katixo.studio.copilot.ChatMessage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/**
 * Request/response payloads for the Copilot agent API (DTOs only at the
 * boundary, CLAUDE.md §6). The agent runs a buffered tool-calling loop, so —
 * unlike plain chat — it returns the executed tool steps and any actions
 * awaiting the user's approval alongside the final message.
 */
public final class AgentDtos {

    private AgentDtos() {
    }

    /** A turn submission. The frontend owns history; the backend is stateless. */
    public record AgentRequest(
            @NotEmpty List<ChatMessage> messages,
            String model,
            EditorContext context
    ) {
    }

    /**
     * A snapshot of what the user is looking at in the editor, so the agent can
     * resolve references like "this" or "the selected image" to a real assetId
     * without the user pasting an id (Cursor-style context injection). All
     * fields optional — the panel sends what it has.
     */
    public record EditorContext(
            String projectName,
            Integer canvasWidth,
            Integer canvasHeight,
            ContextElement selected,
            List<ContextElement> elements
    ) {
    }

    /** A canvas element the agent can reference (id + kind + backing asset). */
    public record ContextElement(String id, String type, String assetId) {
    }

    /**
     * The agent's final reply plus a transcript of what it did.
     *
     * @param steps          tools executed this turn (read-only + auto-applied)
     * @param pendingActions approval-gated tools the model proposed but did not
     *                       run; the UI shows a confirm card, then calls
     *                       {@code /copilot/agent/confirm}
     */
    public record AgentResponse(
            String model,
            ChatMessage message,
            List<ToolStep> steps,
            List<PendingAction> pendingActions
    ) {
    }

    /** One executed (or proposed) tool call, surfaced as a row in the panel. */
    public record ToolStep(
            String tool,
            JsonNode args,
            String status,   // done | failed | pending_approval
            String summary,
            UUID jobId
    ) {
    }

    /** An approval-gated action the user must confirm before it runs. */
    public record PendingAction(String tool, JsonNode args, String label) {
    }

    /** Body for {@code POST /copilot/agent/confirm}: execute a proposed action. */
    public record ConfirmRequest(
            @NotBlank String tool,
            JsonNode args
    ) {
    }
}
