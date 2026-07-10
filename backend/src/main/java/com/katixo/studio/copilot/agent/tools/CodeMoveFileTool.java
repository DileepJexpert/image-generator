package com.katixo.studio.copilot.agent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.katixo.studio.code.CodeWorkspace;
import com.katixo.studio.copilot.agent.CopilotTool;
import com.katixo.studio.copilot.agent.ToolResult;
import com.katixo.studio.copilot.agent.ToolSchema;
import org.springframework.stereotype.Component;

/** Approval-gated file rename/move inside the configured workspace. */
@Component
public class CodeMoveFileTool implements CopilotTool {

    private final CodeWorkspace workspace;
    private final ObjectMapper mapper;

    public CodeMoveFileTool(CodeWorkspace workspace, ObjectMapper mapper) {
        this.workspace = workspace;
        this.mapper = mapper;
    }

    @Override
    public String name() {
        return "code_move_file";
    }

    @Override
    public String description() {
        return "Rename or move one file in the configured code workspace. "
                + "The destination must not already exist.";
    }

    @Override
    public JsonNode parameters() {
        return ToolSchema.object(mapper)
                .prop("from", "string", "Existing workspace-relative file path.")
                .prop("to", "string", "New workspace-relative file path.")
                .require("from", "to")
                .build();
    }

    @Override
    public boolean requiresApproval() {
        return true;
    }

    @Override
    public String approvalLabel(JsonNode args) {
        return "Move file: " + Args.text(args, "from", "(missing source)")
                + " -> " + Args.text(args, "to", "(missing destination)");
    }

    @Override
    public ToolResult execute(JsonNode args) {
        try {
            return ToolResult.text(workspace.moveFile(
                    Args.requireText(args, "from"),
                    Args.requireText(args, "to")));
        } catch (Exception e) {
            return ToolResult.text("code_move_file failed: " + e.getMessage());
        }
    }
}
