package com.katixo.studio.copilot.agent;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A capability the Copilot agent can invoke during its reasoning loop. Each tool
 * is a thin wrapper over an existing studio service/endpoint — the agent only
 * ever calls this fixed registry, never arbitrary shell or code (a deliberate
 * divergence from open-ended agents like OpenClaw; see
 * {@code docs/agentic-copilot-research.md}).
 *
 * <p>Implementations are Spring beans; {@link ToolRegistry} collects them all.
 */
public interface CopilotTool {

    /** Stable tool name exposed to the model (snake_case, e.g. {@code generate_image}). */
    String name();

    /** One-line description the model uses to decide when to call the tool. */
    String description();

    /**
     * JSON-Schema object describing the tool's parameters (the {@code parameters}
     * field of an Ollama tool spec). Built once via {@link ToolSchema}.
     */
    JsonNode parameters();

    /**
     * Whether invoking this tool reaches outside the machine or is otherwise
     * irreversible (e.g. lead scraping). Approval-gated tools are <em>proposed</em>
     * to the user rather than executed inline (CLAUDE.md safety; mirrors Cline's
     * approval gate and OpenClaw's guidance to gate external communication).
     */
    default boolean requiresApproval() {
        return false;
    }

    /**
     * Short human-readable summary of what confirming the action will do, shown on
     * the approval card. Only meaningful when {@link #requiresApproval()} is true.
     */
    default String approvalLabel(JsonNode args) {
        return "Run " + name();
    }

    /**
     * Execute the tool. Long-running work is submitted to the job queue and the
     * result carries the {@code jobId} so the UI can track progress over the
     * existing job WebSocket — the agent thread never blocks on the GPU.
     */
    ToolResult execute(JsonNode args);
}
