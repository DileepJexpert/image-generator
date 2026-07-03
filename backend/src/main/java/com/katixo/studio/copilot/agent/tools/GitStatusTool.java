package com.katixo.studio.copilot.agent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.katixo.studio.copilot.agent.CopilotTool;
import com.katixo.studio.copilot.agent.ToolResult;
import com.katixo.studio.copilot.agent.ToolSchema;
import com.katixo.studio.git.GitClient;
import org.springframework.stereotype.Component;

/** Read-only git status for the configured repository. */
@Component
public class GitStatusTool implements CopilotTool {

    private final GitClient git;
    private final ObjectMapper mapper;

    public GitStatusTool(GitClient git, ObjectMapper mapper) {
        this.git = git;
        this.mapper = mapper;
    }

    @Override
    public String name() {
        return "git_status";
    }

    @Override
    public String description() {
        return "Show git status and current branch for the configured repository.";
    }

    @Override
    public JsonNode parameters() {
        return ToolSchema.object(mapper).build();
    }

    @Override
    public ToolResult execute(JsonNode args) {
        if (!git.isConfigured()) {
            return ToolResult.text("git_status is not configured. Set katixo.git.repo-dir or GIT_REPO_DIR.");
        }
        try {
            return ToolResult.text(git.status());
        } catch (Exception e) {
            return ToolResult.text("git_status failed: " + e.getMessage());
        }
    }
}
