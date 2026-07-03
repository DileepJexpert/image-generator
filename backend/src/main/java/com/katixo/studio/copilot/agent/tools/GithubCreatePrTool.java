package com.katixo.studio.copilot.agent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.katixo.studio.copilot.agent.CopilotTool;
import com.katixo.studio.copilot.agent.ToolResult;
import com.katixo.studio.copilot.agent.ToolSchema;
import com.katixo.studio.git.GitClient;
import com.katixo.studio.git.GithubClient;
import org.springframework.stereotype.Component;

/** Approval-gated GitHub PR creation. Assumes the branch has already been pushed. */
@Component
public class GithubCreatePrTool implements CopilotTool {

    private final GithubClient github;
    private final GitClient git;
    private final ObjectMapper mapper;

    public GithubCreatePrTool(GithubClient github, GitClient git, ObjectMapper mapper) {
        this.github = github;
        this.git = git;
        this.mapper = mapper;
    }

    @Override
    public String name() {
        return "github_create_pr";
    }

    @Override
    public String description() {
        return "Create a GitHub pull request from the current or specified branch after pushing changes.";
    }

    @Override
    public JsonNode parameters() {
        return ToolSchema.object(mapper)
                .prop("title", "string", "Pull request title.")
                .prop("body", "string", "Pull request body.")
                .prop("head", "string", "Head branch. Defaults to current branch when omitted.")
                .prop("base", "string", "Base branch, usually main.")
                .require("title", "base")
                .build();
    }

    @Override
    public boolean requiresApproval() {
        return true;
    }

    @Override
    public String approvalLabel(JsonNode args) {
        return "Create GitHub PR: " + Args.text(args, "title", "(missing title)");
    }

    @Override
    public ToolResult execute(JsonNode args) {
        if (!github.isConfigured()) {
            return ToolResult.text("github_create_pr is not configured. Set katixo.github.repository and GITHUB_TOKEN.");
        }
        try {
            String head = Args.text(args, "head", null);
            if (head == null) {
                head = git.currentBranch();
            }
            String url = github.createPullRequest(
                    Args.requireText(args, "title"),
                    Args.text(args, "body", ""),
                    head,
                    Args.requireText(args, "base"));
            return ToolResult.text("Created PR: " + url);
        } catch (Exception e) {
            return ToolResult.text("github_create_pr failed: " + e.getMessage());
        }
    }
}
