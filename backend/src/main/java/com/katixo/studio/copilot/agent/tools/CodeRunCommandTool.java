package com.katixo.studio.copilot.agent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.katixo.studio.code.CodeWorkspace;
import com.katixo.studio.copilot.agent.CopilotTool;
import com.katixo.studio.copilot.agent.ToolResult;
import com.katixo.studio.copilot.agent.ToolSchema;
import org.springframework.stereotype.Component;

/** Approval-gated build/test command runner inside the configured workspace. */
@Component
public class CodeRunCommandTool implements CopilotTool {

    private final CodeWorkspace workspace;
    private final ObjectMapper mapper;

    public CodeRunCommandTool(CodeWorkspace workspace, ObjectMapper mapper) {
        this.workspace = workspace;
        this.mapper = mapper;
    }

    @Override
    public String name() {
        return "code_run_command";
    }

    @Override
    public String description() {
        return "Run an allowed build, test, analyze, or read-only git command in the workspace.";
    }

    @Override
    public JsonNode parameters() {
        return ToolSchema.object(mapper)
                .prop("command", "string", "Command to run, e.g. mvnw.cmd test or flutter analyze.")
                .prop("directory", "string", "Optional workspace-relative working directory.")
                .require("command")
                .build();
    }

    @Override
    public boolean requiresApproval() {
        return true;
    }

    @Override
    public String approvalLabel(JsonNode args) {
        return "Run command: " + Args.text(args, "command", "(missing command)");
    }

    @Override
    public ToolResult execute(JsonNode args) {
        try {
            return ToolResult.text(workspace.runCommand(
                    Args.requireText(args, "command"),
                    Args.text(args, "directory", null)));
        } catch (Exception e) {
            return ToolResult.text("code_run_command failed: " + e.getMessage());
        }
    }
}
