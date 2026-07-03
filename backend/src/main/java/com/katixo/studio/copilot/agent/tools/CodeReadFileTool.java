package com.katixo.studio.copilot.agent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.katixo.studio.code.CodeWorkspace;
import com.katixo.studio.copilot.agent.CopilotTool;
import com.katixo.studio.copilot.agent.ToolResult;
import com.katixo.studio.copilot.agent.ToolSchema;
import org.springframework.stereotype.Component;

/** Read-only file reader for the configured workspace. */
@Component
public class CodeReadFileTool implements CopilotTool {

    private final CodeWorkspace workspace;
    private final ObjectMapper mapper;

    public CodeReadFileTool(CodeWorkspace workspace, ObjectMapper mapper) {
        this.workspace = workspace;
        this.mapper = mapper;
    }

    @Override
    public String name() {
        return "code_read_file";
    }

    @Override
    public String description() {
        return "Read a source file from the configured workspace with line numbers.";
    }

    @Override
    public JsonNode parameters() {
        return ToolSchema.object(mapper)
                .prop("path", "string", "Workspace-relative file path.")
                .prop("startLine", "integer", "First line to read, default 1.")
                .prop("maxLines", "integer", "Maximum lines to read, 1-400.")
                .require("path")
                .build();
    }

    @Override
    public ToolResult execute(JsonNode args) {
        try {
            return ToolResult.text(workspace.readFile(
                    Args.requireText(args, "path"),
                    Args.clampedInt(args, "startLine", 1, 1, Integer.MAX_VALUE),
                    Args.clampedInt(args, "maxLines", 160, 1, 400)));
        } catch (Exception e) {
            return ToolResult.text("code_read_file failed: " + e.getMessage());
        }
    }
}
