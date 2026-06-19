package com.katixo.studio.copilot.agent;

import java.util.UUID;

/**
 * Outcome of a {@link CopilotTool} execution.
 *
 * @param summary text fed back to the model as the {@code tool}-role message and
 *                shown on the step row in the UI
 * @param jobId   the async job started by this tool, or {@code null} for
 *                read-only/instant tools. When present, the frontend tracks
 *                progress over {@code /ws/jobs/{jobId}}.
 */
public record ToolResult(String summary, UUID jobId) {

    public static ToolResult text(String summary) {
        return new ToolResult(summary, null);
    }

    public static ToolResult job(String summary, UUID jobId) {
        return new ToolResult(summary, jobId);
    }
}
