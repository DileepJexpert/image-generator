package com.katixo.studio.git;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Minimal, deliberately-constrained wrapper around {@code git} for the Copilot's {@code git_push}
 * tool. It operates ONLY on the single repository directory configured via
 * {@code katixo.git.repo-dir} (blank = disabled), never on an arbitrary path the model supplies, and
 * never force-pushes. The tool that uses it is approval-gated, so a confirmation is always required.
 */
@Component
public class GitClient {

    private static final long TIMEOUT_SECONDS = 120;

    private final String repoDir;

    public GitClient(@Value("${katixo.git.repo-dir:}") String repoDir) {
        this.repoDir = repoDir == null ? "" : repoDir.trim();
    }

    /** True only if a real git repo is configured. */
    public boolean isConfigured() {
        if (repoDir.isBlank()) {
            return false;
        }
        File dir = new File(repoDir);
        return dir.isDirectory() && new File(dir, ".git").exists();
    }

    /** Short status for the configured repository. */
    public String status() throws IOException, InterruptedException {
        return run(List.of("git", "status", "--short", "--branch")).trim();
    }

    /** Current branch name. */
    public String currentBranch() throws IOException, InterruptedException {
        return run(List.of("git", "branch", "--show-current")).lines()
                .filter(line -> !line.startsWith("$ "))
                .findFirst()
                .orElse("")
                .trim();
    }

    /** Create and check out a new branch from the current HEAD. */
    public String createAndCheckoutBranch(String branch) throws IOException, InterruptedException {
        String safeBranch = requireBranch(branch);
        return run(List.of("git", "switch", "-c", safeBranch)).trim();
    }

    /** Stage all changes, commit with {@code message}, and push the current branch. */
    public String commitAndPush(String message) throws IOException, InterruptedException {
        StringBuilder log = new StringBuilder();
        log.append(run(List.of("git", "add", "-A")));
        log.append(run(List.of("git", "commit", "-m", message)));
        log.append(pushCurrentBranch());
        return log.toString().trim();
    }

    /**
     * Stage only {@code paths} and commit them with {@code message}. Does not push,
     * and does not touch any other modified file — the scoped counterpart to
     * {@link #commitAndPush(String)}'s {@code git add -A}.
     */
    public String commitPaths(String message, List<String> paths)
            throws IOException, InterruptedException {
        String safeMessage = requireMessage(message);
        List<String> safePaths = requirePaths(paths);
        List<String> add = new ArrayList<>(List.of("git", "add", "--"));
        add.addAll(safePaths);
        List<String> commit = new ArrayList<>(List.of("git", "commit", "-m", safeMessage, "--"));
        commit.addAll(safePaths);

        StringBuilder log = new StringBuilder();
        log.append(run(add));
        log.append(run(commit));
        return log.toString().trim();
    }

    /** Push current branch, setting upstream when needed. */
    public String pushCurrentBranch() throws IOException, InterruptedException {
        String branch = currentBranch();
        if (branch.isBlank()) {
            throw new IOException("Cannot push: detached HEAD or unknown current branch");
        }
        return run(List.of("git", "push", "-u", "origin", branch));
    }

    private String requireMessage(String message) {
        String m = message == null ? "" : message.trim();
        if (m.isBlank()) {
            throw new IllegalArgumentException("Missing commit message");
        }
        return m;
    }

    /** Validates that every path is a safe, repo-relative pathspec (no flags, no traversal). */
    private List<String> requirePaths(List<String> paths) {
        if (paths == null || paths.isEmpty()) {
            throw new IllegalArgumentException("Missing paths to commit");
        }
        List<String> safe = new ArrayList<>();
        for (String raw : paths) {
            String p = raw == null ? "" : raw.trim().replace('\\', '/');
            if (p.isBlank()) {
                throw new IllegalArgumentException("Empty path in commit list");
            }
            if (p.startsWith("-") || p.startsWith("/") || p.contains(":")) {
                throw new IllegalArgumentException("Unsafe path (must be repo-relative): " + raw);
            }
            for (String part : p.split("/")) {
                if (part.equals("..") || part.equals(".git")) {
                    throw new IllegalArgumentException("Path escapes the repository or targets .git: " + raw);
                }
            }
            safe.add(p);
        }
        return safe;
    }

    private String requireBranch(String branch) {
        String b = branch == null ? "" : branch.trim();
        if (b.isBlank()) {
            throw new IllegalArgumentException("Missing branch name");
        }
        String lowered = b.toLowerCase(Locale.ROOT);
        if (b.startsWith("-") || b.contains("..") || b.contains(" ") || lowered.contains("~")
                || lowered.contains("^") || lowered.contains(":") || lowered.endsWith("/")
                || lowered.endsWith(".lock")) {
            throw new IllegalArgumentException("Unsafe branch name: " + branch);
        }
        return b;
    }

    private String run(List<String> command) throws IOException, InterruptedException {
        if (!isConfigured()) {
            throw new IOException("git repo is not configured");
        }
        Process process = new ProcessBuilder(command)
                .directory(new File(repoDir))
                .redirectErrorStream(true)
                .start();
        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line).append('\n');
            }
        }
        if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IOException("git command timed out: " + String.join(" ", command));
        }
        return "$ " + String.join(" ", command) + "\n" + out;
    }
}
