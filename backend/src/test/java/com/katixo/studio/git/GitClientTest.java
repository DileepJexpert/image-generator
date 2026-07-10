package com.katixo.studio.git;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class GitClientTest {

    @TempDir
    Path repo;

    @Test
    void commitPathsStagesOnlyTheNamedFiles() throws Exception {
        assumeTrue(gitAvailable(), "git is not installed");
        initRepo();
        Files.writeString(repo.resolve("keep.txt"), "committed\n");
        Files.writeString(repo.resolve("leave.txt"), "untracked\n");

        GitClient git = new GitClient(repo.toString());
        String log = git.commitPaths("scoped commit", List.of("keep.txt"));
        assertThat(log).contains("git commit");

        // keep.txt is in the new commit; leave.txt is still untracked.
        assertThat(runGit("log", "-1", "--name-only", "--pretty=format:")).contains("keep.txt");
        assertThat(runGit("status", "--short")).contains("?? leave.txt");
    }

    @Test
    void commitPathsRejectsUnsafePaths() {
        GitClient git = new GitClient(repo.toString());
        assertThatThrownBy(() -> git.commitPaths("msg", List.of("../escape.txt")))
                .hasMessageContaining("escapes");
        assertThatThrownBy(() -> git.commitPaths("msg", List.of(".git/config")))
                .hasMessageContaining(".git");
        assertThatThrownBy(() -> git.commitPaths("msg", List.of("--force")))
                .hasMessageContaining("Unsafe path");
        assertThatThrownBy(() -> git.commitPaths("msg", List.of()))
                .hasMessageContaining("Missing paths");
    }

    private void initRepo() throws Exception {
        runGit("init");
        runGit("config", "user.email", "test@example.com");
        runGit("config", "user.name", "Test");
    }

    private boolean gitAvailable() {
        try {
            return new ProcessBuilder("git", "--version").start().waitFor(10, TimeUnit.SECONDS);
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    private String runGit(String... args) throws IOException, InterruptedException {
        List<String> command = new java.util.ArrayList<>(List.of("git"));
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command)
                .directory(repo.toFile())
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
        process.waitFor(30, TimeUnit.SECONDS);
        return out.toString();
    }
}
