package com.katixo.studio.copilot.agent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.katixo.studio.code.CodeWorkspace;
import com.katixo.studio.code.WorkspaceCheckStatus;
import com.katixo.studio.copilot.agent.CopilotTool;
import com.katixo.studio.copilot.agent.ToolResult;
import com.katixo.studio.copilot.agent.ToolSchema;
import org.springframework.stereotype.Component;

/** Approval-gated source-file writer for the configured workspace. */
@Component
public class CodeWriteFileTool implements CopilotTool {

    private final CodeWorkspace workspace;
    private final ObjectMapper mapper;
    private final WorkspaceCheckStatus checkStatus;

    public CodeWriteFileTool(CodeWorkspace workspace, ObjectMapper mapper, WorkspaceCheckStatus checkStatus) {
        this.workspace = workspace;
        this.mapper = mapper;
        this.checkStatus = checkStatus;
    }

    @Override
    public String name() {
        return "code_write_file";
    }

    @Override
    public String description() {
        return "Create or replace one file in the configured code workspace.";
    }

    @Override
    public JsonNode parameters() {
        return ToolSchema.object(mapper)
                .prop("path", "string", "Workspace-relative file path to create or replace.")
                .prop("content", "string", "Complete new file content.")
                .require("path", "content")
                .build();
    }

    @Override
    public boolean requiresApproval() {
        return true;
    }

    @Override
    public String approvalLabel(JsonNode args) {
        return "Write file: " + Args.text(args, "path", "(missing path)");
    }

    @Override
    public ToolResult execute(JsonNode args) {
        try {
            String result = workspace.writeFile(
                    Args.requireText(args, "path"),
                    Args.requireText(args, "content"));
            checkStatus.recordEdit();
            return ToolResult.text(result);
        } catch (Exception e) {
            return ToolResult.text("code_write_file failed: " + e.getMessage());
        }
    }
}
