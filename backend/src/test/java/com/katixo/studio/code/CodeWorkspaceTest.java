package com.katixo.studio.code;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CodeWorkspaceTest {

    @TempDir
    Path temp;

    @Test
    void searchesAndReadsInsideConfiguredWorkspace() throws Exception {
        Path file = temp.resolve("src/main/App.java");
        Files.createDirectories(file.getParent());
        Files.writeString(file, """
                package demo;

                class App {
                    void run() {
                        System.out.println("needle");
                    }
                }
                """);

        CodeWorkspace workspace = new CodeWorkspace(temp.toString(), 20_000, 5);

        assertThat(workspace.search("needle", "src/**/*.java", 10))
                .contains("src/main/App.java:5")
                .contains("needle");
        assertThat(workspace.readFile("src/main/App.java", 4, 2))
                .contains("   4 |     void run()")
                .contains("   5 |         System.out.println(\"needle\");");
    }

    @Test
    void writesFilesButBlocksTraversalAndGitDirectory() throws Exception {
        CodeWorkspace workspace = new CodeWorkspace(temp.toString(), 20_000, 5);

        assertThat(workspace.writeFile("docs/note.md", "# Hello\n"))
                .contains("docs/note.md");
        assertThat(Files.readString(temp.resolve("docs/note.md"))).isEqualTo("# Hello\n");

        assertThatThrownBy(() -> workspace.writeFile("../outside.txt", "nope"))
                .hasMessageContaining("escapes");
        assertThatThrownBy(() -> workspace.readFile(".git/config", 1, 10))
                .hasMessageContaining(".git");
    }

    @Test
    void rejectsUnsafeCommandsBeforeTheyRun() {
        CodeWorkspace workspace = new CodeWorkspace(temp.toString(), 20_000, 5);

        assertThatThrownBy(() -> workspace.runCommand("rm -rf target", null))
                .hasMessageContaining("guardrail");
        assertThatThrownBy(() -> workspace.runCommand("python script.py", null))
                .hasMessageContaining("allowed build/test/git prefix");
    }

    @Test
    void rejectsShellChainingPastAnAllowedPrefix() {
        CodeWorkspace workspace = new CodeWorkspace(temp.toString(), 20_000, 5);

        assertThatThrownBy(() -> workspace.runCommand("mvn -v && whoami", null))
                .hasMessageContaining("shell operators");
        assertThatThrownBy(() -> workspace.runCommand("git status; cat secrets", null))
                .hasMessageContaining("shell operators");
        assertThatThrownBy(() -> workspace.runCommand("mvn -v `whoami`", null))
                .hasMessageContaining("shell operators");
        assertThatThrownBy(() -> workspace.runCommand("git log > /tmp/out", null))
                .hasMessageContaining("shell operators");
    }

    @Test
    void appliesMixedMultiFilePatchInOneCall() throws Exception {
        Files.writeString(temp.resolve("one.txt"), "hello old\n");
        CodeWorkspace workspace = new CodeWorkspace(temp.toString(), 20_000, 5);

        String result = workspace.applyPatch(List.of(
                new CodeWorkspace.PatchOperation("one.txt", null, "old", "new"),
                new CodeWorkspace.PatchOperation("nested/two.txt", "created\n", null, null)
        ));

        assertThat(result).contains("Applied patch to 2 file(s)");
        assertThat(Files.readString(temp.resolve("one.txt"))).isEqualTo("hello new\n");
        assertThat(Files.readString(temp.resolve("nested/two.txt"))).isEqualTo("created\n");
    }

    @Test
    void previewsPatchWithoutWritingFiles() throws Exception {
        Files.writeString(temp.resolve("one.txt"), "alpha\nold\nomega\n");
        CodeWorkspace workspace = new CodeWorkspace(temp.toString(), 20_000, 5);

        String preview = workspace.previewPatch(List.of(
                new CodeWorkspace.PatchOperation("one.txt", null, "old", "new"),
                new CodeWorkspace.PatchOperation("two.txt", "created\n", null, null)
        ));

        assertThat(preview)
                .contains("Patch preview: 2 file(s)")
                .contains("diff -- one.txt")
                .contains("-old")
                .contains("+new")
                .contains("diff -- two.txt")
                .contains("+++ b/two.txt")
                .contains("+created");
        assertThat(Files.readString(temp.resolve("one.txt"))).isEqualTo("alpha\nold\nomega\n");
        assertThat(temp.resolve("two.txt")).doesNotExist();
    }

    @Test
    void rejectsDuplicateOrAmbiguousPatchOperationsBeforeWriting() throws Exception {
        Files.writeString(temp.resolve("one.txt"), "same same\n");
        Files.writeString(temp.resolve("two.txt"), "untouched\n");
        CodeWorkspace workspace = new CodeWorkspace(temp.toString(), 20_000, 5);

        assertThatThrownBy(() -> workspace.applyPatch(List.of(
                new CodeWorkspace.PatchOperation("one.txt", null, "same", "changed"),
                new CodeWorkspace.PatchOperation("one.txt", "new\n", null, null)
        ))).hasMessageContaining("matched 2");

        assertThatThrownBy(() -> workspace.applyPatch(List.of(
                new CodeWorkspace.PatchOperation("one.txt", "first\n", null, null),
                new CodeWorkspace.PatchOperation("one.txt", "second\n", null, null)
        ))).hasMessageContaining("duplicate path");

        assertThat(Files.readString(temp.resolve("one.txt"))).isEqualTo("same same\n");
        assertThat(Files.readString(temp.resolve("two.txt"))).isEqualTo("untouched\n");
    }
}
