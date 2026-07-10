package com.katixo.studio.copilot.agent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.katixo.studio.code.CodeWorkspace;
import com.katixo.studio.code.WorkspaceCheckStatus;
import com.katixo.studio.copilot.agent.CopilotTool;
import com.katixo.studio.copilot.agent.ToolResult;
import com.katixo.studio.copilot.agent.ToolSchema;
import org.springframework.stereotype.Component;

/** Approval-gated single-file deletion inside the configured workspace. */
@Component
public class CodeDeleteFileTool implements CopilotTool {

    private final CodeWorkspace workspace;
    private final ObjectMapper mapper;
    private final WorkspaceCheckStatus checkStatus;

    public CodeDeleteFileTool(CodeWorkspace workspace, ObjectMapper mapper, WorkspaceCheckStatus checkStatus) {
        this.workspace = workspace;
        this.mapper = mapper;
        this.checkStatus = checkStatus;
    }

    @Override
    public String name() {
        return "code_delete_file";
    }

    @Override
    public String description() {
        return "Delete one file from the configured code workspace. Refuses directories.";
    }

    @Override
    public JsonNode parameters() {
        return ToolSchema.object(mapper)
                .prop("path", "string", "Workspace-relative file path to delete.")
                .require("path")
                .build();
    }

    @Override
    public boolean requiresApproval() {
        return true;
    }

    @Override
    public String approvalLabel(JsonNode args) {
        return "Delete file: " + Args.text(args, "path", "(missing path)");
    }

    @Override
    public ToolResult execute(JsonNode args) {
        try {
            String result = workspace.deleteFile(Args.requireText(args, "path"));
            checkStatus.recordEdit();
            return ToolResult.text(result);
        } catch (Exception e) {
            return ToolResult.text("code_delete_file failed: " + e.getMessage());
        }
    }
}
