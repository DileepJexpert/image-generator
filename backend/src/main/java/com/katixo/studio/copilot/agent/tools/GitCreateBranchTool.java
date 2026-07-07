package com.katixo.studio.copilot.agent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.katixo.studio.copilot.agent.CopilotTool;
import com.katixo.studio.copilot.agent.ToolResult;
import com.katixo.studio.copilot.agent.ToolSchema;
import com.katixo.studio.git.GitClient;
import org.springframework.stereotype.Component;

/** Approval-gated branch creation for coding-agent work. */
@Component
public class GitCreateBranchTool implements CopilotTool {

    private final GitClient git;
    private final ObjectMapper mapper;

    public GitCreateBranchTool(GitClient git, ObjectMapper mapper) {
        this.git = git;
        this.mapper = mapper;
    }

    @Override
    public String name() {
        return "git_create_branch";
    }

    @Override
    public String description() {
        return "Create and switch to a new git branch in the configured repository.";
    }

    @Override
    public JsonNode parameters() {
        return ToolSchema.object(mapper)
                .prop("branch", "string", "New branch name, e.g. codex/add-coding-agent-tools.")
                .require("branch")
                .build();
    }

    @Override
    public boolean requiresApproval() {
        return true;
    }

    @Override
    public String approvalLabel(JsonNode args) {
        return "Create branch: " + Args.text(args, "branch", "(missing branch)");
    }

    @Override
    public ToolResult execute(JsonNode args) {
        if (!git.isConfigured()) {
            return ToolResult.text("git_create_branch is not configured. Set katixo.git.repo-dir or GIT_REPO_DIR.");
        }
        try {
            return ToolResult.text(git.createAndCheckoutBranch(Args.requireText(args, "branch")));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ToolResult.text("git_create_branch interrupted: " + e.getMessage());
        } catch (Exception e) {
            return ToolResult.text("git_create_branch failed: " + e.getMessage());
        }
    }
}
