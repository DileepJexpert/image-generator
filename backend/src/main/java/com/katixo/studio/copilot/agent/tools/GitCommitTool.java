package com.katixo.studio.copilot.agent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.katixo.studio.copilot.agent.CopilotTool;
import com.katixo.studio.copilot.agent.ToolResult;
import com.katixo.studio.copilot.agent.ToolSchema;
import com.katixo.studio.git.GitClient;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Commits only the named files in the configured local repo — the scoped
 * counterpart to {@code git_push}'s stage-everything behaviour. Does not push.
 * Approval-gated and disabled until {@code katixo.git.repo-dir} is set.
 */
@Component
public class GitCommitTool implements CopilotTool {

    private final GitClient git;
    private final ObjectMapper mapper;

    public GitCommitTool(GitClient git, ObjectMapper mapper) {
        this.git = git;
        this.mapper = mapper;
    }

    @Override
    public String name() {
        return "git_commit";
    }

    @Override
    public String description() {
        return "Stage and commit only the listed files in the local repo (does not push). "
                + "Use this to commit exactly the files you changed instead of everything.";
    }

    @Override
    public JsonNode parameters() {
        return ToolSchema.object(mapper)
                .prop("message", "string", "The commit message.")
                .arrayProp("paths", "Repo-relative paths to stage and commit.")
                .require("message", "paths")
                .build();
    }

    @Override
    public boolean requiresApproval() {
        return true;
    }

    @Override
    public String approvalLabel(JsonNode args) {
        List<String> paths = Args.stringList(args, "paths");
        return "Commit " + paths.size() + " file(s): \"" + Args.text(args, "message", "(no message)") + "\"";
    }

    @Override
    public String approvalPreview(JsonNode args) {
        return String.join("\n", Args.stringList(args, "paths"));
    }

    @Override
    public ToolResult execute(JsonNode args) {
        String message = Args.requireText(args, "message");
        List<String> paths = Args.stringList(args, "paths");
        if (paths.isEmpty()) {
            return ToolResult.text("git_commit needs at least one path. Use git_push to commit all changes.");
        }
        if (!git.isConfigured()) {
            return ToolResult.text("git_commit is not configured. Set katixo.git.repo-dir to a git "
                    + "repository path to enable it.");
        }
        try {
            return ToolResult.text("Committed.\n" + git.commitPaths(message, paths));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ToolResult.text("git_commit interrupted: " + e.getMessage());
        } catch (Exception e) {
            return ToolResult.text("git_commit failed: " + e.getMessage());
        }
    }
}
