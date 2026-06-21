package com.katixo.studio.git;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
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

    /** Stage all changes, commit with {@code message}, and push the current branch. */
    public String commitAndPush(String message) throws IOException, InterruptedException {
        StringBuilder log = new StringBuilder();
        log.append(run(List.of("git", "add", "-A")));
        log.append(run(List.of("git", "commit", "-m", message)));
        log.append(run(List.of("git", "push")));
        return log.toString().trim();
    }

    private String run(List<String> command) throws IOException, InterruptedException {
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
