package com.katixo.studio.copilot.agent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.katixo.studio.code.CodeCommandRequest;
import com.katixo.studio.code.CodeCommandService;
import com.katixo.studio.copilot.agent.CopilotTool;
import com.katixo.studio.copilot.agent.ToolResult;
import com.katixo.studio.copilot.agent.ToolSchema;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Approval-gated build/test command runner inside the configured workspace. */
@Component
public class CodeRunCommandTool implements CopilotTool {

    private final CodeCommandService commandService;
    private final ObjectMapper mapper;

    public CodeRunCommandTool(CodeCommandService commandService, ObjectMapper mapper) {
        this.commandService = commandService;
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
            UUID jobId = commandService.submit(new CodeCommandRequest(
                    Args.requireText(args, "command"),
                    Args.text(args, "directory", null)));
            return ToolResult.job("Started command job: " + Args.requireText(args, "command"), jobId);
        } catch (Exception e) {
            return ToolResult.text("code_run_command failed: " + e.getMessage());
        }
    }
}
