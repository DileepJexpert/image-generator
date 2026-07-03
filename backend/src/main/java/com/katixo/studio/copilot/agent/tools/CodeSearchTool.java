package com.katixo.studio.copilot.agent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.katixo.studio.code.CodeWorkspace;
import com.katixo.studio.copilot.agent.CopilotTool;
import com.katixo.studio.copilot.agent.ToolResult;
import com.katixo.studio.copilot.agent.ToolSchema;
import org.springframework.stereotype.Component;

/** Read-only code search over the configured workspace. */
@Component
public class CodeSearchTool implements CopilotTool {

    private final CodeWorkspace workspace;
    private final ObjectMapper mapper;

    public CodeSearchTool(CodeWorkspace workspace, ObjectMapper mapper) {
        this.workspace = workspace;
        this.mapper = mapper;
    }

    @Override
    public String name() {
        return "code_search";
    }

    @Override
    public String description() {
        return "Search text in the configured code workspace. Use before editing.";
    }

    @Override
    public JsonNode parameters() {
        return ToolSchema.object(mapper)
                .prop("query", "string", "Text to search for.")
                .prop("glob", "string", "Optional file glob, e.g. backend/src/**/*.java.")
                .prop("maxMatches", "integer", "Maximum matches to return, 1-80.")
                .require("query")
                .build();
    }

    @Override
    public ToolResult execute(JsonNode args) {
        try {
            String result = workspace.search(
                    Args.requireText(args, "query"),
                    Args.text(args, "glob", null),
                    Args.clampedInt(args, "maxMatches", 30, 1, 80));
            return ToolResult.text(result);
        } catch (Exception e) {
            return ToolResult.text("code_search failed: " + e.getMessage());
        }
    }
}
