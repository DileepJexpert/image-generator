package com.katixo.studio.copilot.agent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.katixo.studio.copilot.agent.CopilotTool;
import com.katixo.studio.copilot.agent.ToolResult;
import com.katixo.studio.copilot.agent.ToolSchema;
import com.katixo.studio.git.GitClient;
import org.springframework.stereotype.Component;

/**
 * Commits all current changes in the configured local repo and pushes the current branch to its
 * GitHub remote. Approval-gated (it writes to a remote), operates ONLY on the single configured repo
 * ({@code katixo.git.repo-dir}) — never an arbitrary path, never {@code --force} — and is disabled
 * until that path is set.
 */
@Component
public class GitPushTool implements CopilotTool {

    private final GitClient git;
    private final ObjectMapper mapper;

    public GitPushTool(GitClient git, ObjectMapper mapper) {
        this.git = git;
        this.mapper = mapper;
    }

    @Override
    public String name() {
        return "git_push";
    }

    @Override
    public String description() {
        return "Commit all current changes in the local repo and push them to GitHub. "
                + "Writes to a remote, so it requires the user's confirmation.";
    }

    @Override
    public JsonNode parameters() {
        return ToolSchema.object(mapper)
                .prop("message", "string", "The commit message.")
                .require("message")
                .build();
    }

    @Override
    public boolean requiresApproval() {
        return true;
    }

    @Override
    public String approvalLabel(JsonNode args) {
        return "Commit & push to GitHub: \"" + Args.text(args, "message", "(no message)") + "\"";
    }

    @Override
    public ToolResult execute(JsonNode args) {
        String message = Args.requireText(args, "message");
        if (!git.isConfigured()) {
            return ToolResult.text("git_push is not configured. Set katixo.git.repo-dir to a git "
                    + "repository path (with a configured remote) to enable it.");
        }
        try {
            return ToolResult.text("Pushed.\n" + git.commitAndPush(message));
        } catch (Exception e) {
            return ToolResult.text("git push failed: " + e.getMessage());
        }
    }
}
