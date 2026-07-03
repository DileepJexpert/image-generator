package com.katixo.studio.code;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Bounded local repository workspace for the Copilot coding tools.
 *
 * <p>The model never receives an arbitrary filesystem primitive. Every path is
 * resolved under one configured root, generated/cache directories are skipped
 * for search, and remote or destructive actions live in separate approval-gated
 * tools.
 */
@Component
public class CodeWorkspace {

    private static final int DEFAULT_MAX_FILE_BYTES = 250_000;
    private static final int DEFAULT_COMMAND_TIMEOUT_SECONDS = 180;
    private static final int OUTPUT_LIMIT = 16_000;

    private static final Set<String> SEARCH_SKIPPED_DIRS = Set.of(
            ".git", ".gradle", ".dart_tool", ".idea", ".vscode", "build", "target",
            "node_modules", "dist", "out", ".next", ".mvn"
    );

    private static final List<String> COMMAND_PREFIX_ALLOWLIST = List.of(
            "mvn", "mvnw", "mvnw.cmd", "./mvnw", ".\\mvnw.cmd",
            "gradle", "gradlew", "gradlew.bat", "./gradlew", ".\\gradlew.bat",
            "flutter", "dart", "npm", "pnpm", "yarn", "node",
            "pytest", "python -m pytest", "python -m unittest", "python -m py_compile",
            "ruff", "mypy",
            "git status", "git diff", "git log", "git branch", "git rev-parse"
    );

    private static final List<String> COMMAND_DENYLIST = List.of(
            " rm ", "rm -", " del ", "erase ", "rmdir", "remove-item", "format ",
            "shutdown", "reboot", "git reset --hard", "git clean", "curl ", "wget ",
            "invoke-webrequest", "powershell", "cmd /c", "chmod ", "chown "
    );

    private final String configuredRoot;
    private final int maxFileBytes;
    private final Duration commandTimeout;

    public CodeWorkspace(
            @Value("${katixo.code.workspace-dir:${katixo.git.repo-dir:}}") String configuredRoot,
            @Value("${katixo.code.max-file-bytes:" + DEFAULT_MAX_FILE_BYTES + "}") int maxFileBytes,
            @Value("${katixo.code.command-timeout-seconds:" + DEFAULT_COMMAND_TIMEOUT_SECONDS + "}") int timeoutSeconds
    ) {
        this.configuredRoot = configuredRoot == null ? "" : configuredRoot.trim();
        this.maxFileBytes = Math.max(8_000, maxFileBytes);
        this.commandTimeout = Duration.ofSeconds(Math.max(5, timeoutSeconds));
    }

    public boolean isConfigured() {
        return !configuredRoot.isBlank() && Files.isDirectory(root());
    }

    public String search(String query, String glob, int maxMatches) throws IOException {
        requireConfigured();
        String needle = requireQuery(query);
        Pattern globPattern = globPattern(glob);
        List<String> matches = new ArrayList<>();
        int limit = Math.max(1, Math.min(80, maxMatches));

        try (var stream = Files.walk(root())) {
            for (Path file : stream
                    .filter(Files::isRegularFile)
                    .filter(this::isSearchableFile)
                    .toList()) {
                String rel = relative(file);
                if (globPattern != null && !globPattern.matcher(rel).matches()) {
                    continue;
                }
                addMatches(file, rel, needle, matches, limit);
                if (matches.size() >= limit) {
                    break;
                }
            }
        }

        if (matches.isEmpty()) {
            return "No matches for \"" + needle + "\".";
        }
        return capped(String.join("\n", matches));
    }

    public String readFile(String path, int startLine, int maxLines) throws IOException {
        requireConfigured();
        Path file = resolve(path);
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("Not a file: " + path);
        }
        if (Files.size(file) > maxFileBytes) {
            throw new IllegalArgumentException("File is too large to read through the agent: " + relative(file));
        }

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        int start = Math.max(1, startLine);
        int count = Math.max(1, Math.min(400, maxLines));
        int end = Math.min(lines.size(), start + count - 1);

        StringBuilder out = new StringBuilder();
        out.append(relative(file)).append(" lines ").append(start).append("-").append(end).append('\n');
        for (int i = start; i <= end; i++) {
            out.append(String.format(Locale.ROOT, "%4d | %s%n", i, lines.get(i - 1)));
        }
        return capped(out.toString());
    }

    public String writeFile(String path, String content) throws IOException {
        requireConfigured();
        if (content == null) {
            throw new IllegalArgumentException("Missing content");
        }
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > maxFileBytes * 2L) {
            throw new IllegalArgumentException("Content is too large for one agent write");
        }
        Path file = resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return "Wrote " + bytes.length + " bytes to " + relative(file) + ".";
    }

    public String applyPatch(List<PatchOperation> operations) throws IOException {
        requireConfigured();
        if (operations == null || operations.isEmpty()) {
            throw new IllegalArgumentException("Patch must contain at least one operation");
        }
        if (operations.size() > 20) {
            throw new IllegalArgumentException("Patch can touch at most 20 files");
        }

        Set<String> seenPaths = new HashSet<>();
        List<PendingWrite> writes = new ArrayList<>();
        for (PatchOperation op : operations) {
            PendingWrite write = preparePatchOperation(op);
            if (!seenPaths.add(relative(write.path()))) {
                throw new IllegalArgumentException("Patch contains duplicate path: " + relative(write.path()));
            }
            writes.add(write);
        }

        List<PendingWrite> applied = new ArrayList<>();
        try {
            for (PendingWrite write : writes) {
                Files.createDirectories(write.path().getParent());
                Files.writeString(write.path(), write.newContent(), StandardCharsets.UTF_8);
                applied.add(write);
            }
        } catch (RuntimeException | IOException e) {
            rollback(applied);
            throw e;
        }

        return "Applied patch to " + writes.size() + " file(s): "
                + writes.stream().map(w -> relative(w.path())).toList();
    }

    public String runCommand(String command, String directory) throws IOException, InterruptedException {
        requireConfigured();
        String cmd = requireSafeCommand(command);
        Path workingDir = directory == null || directory.isBlank() ? root() : resolve(directory);
        if (!Files.isDirectory(workingDir)) {
            throw new IllegalArgumentException("Working directory does not exist: " + directory);
        }

        boolean windows = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
        List<String> shell = windows
                ? List.of("cmd.exe", "/c", cmd)
                : List.of("sh", "-lc", cmd);
        Process process = new ProcessBuilder(shell)
                .directory(workingDir.toFile())
                .redirectErrorStream(true)
                .start();

        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (out.length() < OUTPUT_LIMIT) {
                    out.append(line).append('\n');
                }
            }
        }
        if (!process.waitFor(commandTimeout.toSeconds(), TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IOException("Command timed out after " + commandTimeout.toSeconds() + "s");
        }
        String header = "$ " + cmd + "\nexit=" + process.exitValue() + "\n";
        return capped(header + out);
    }

    private void addMatches(Path file, String rel, String needle, List<String> matches, int limit)
            throws IOException {
        if (Files.size(file) > maxFileBytes || looksBinary(file)) {
            return;
        }
        String loweredNeedle = needle.toLowerCase(Locale.ROOT);
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.toLowerCase(Locale.ROOT).contains(loweredNeedle)) {
                matches.add(rel + ":" + (i + 1) + ": " + trimLine(line));
                if (matches.size() >= limit) {
                    return;
                }
            }
        }
    }

    private PendingWrite preparePatchOperation(PatchOperation op) throws IOException {
        if (op == null) {
            throw new IllegalArgumentException("Patch operation cannot be null");
        }
        Path path = resolve(op.path());
        boolean wholeFile = op.content() != null;
        boolean exactReplace = op.find() != null || op.replace() != null;
        if (wholeFile == exactReplace) {
            throw new IllegalArgumentException(
                    "Each patch operation must provide either content or find+replace, not both");
        }

        String oldContent = Files.isRegularFile(path)
                ? Files.readString(path, StandardCharsets.UTF_8)
                : null;
        String newContent;
        if (wholeFile) {
            newContent = op.content();
        } else {
            if (op.find() == null || op.replace() == null) {
                throw new IllegalArgumentException("find and replace must be provided together");
            }
            if (oldContent == null) {
                throw new IllegalArgumentException("Cannot replace text in a missing file: " + relative(path));
            }
            int matches = countOccurrences(oldContent, op.find());
            if (matches != 1) {
                throw new IllegalArgumentException("find text must match exactly once in "
                        + relative(path) + " (matched " + matches + ")");
            }
            newContent = oldContent.replace(op.find(), op.replace());
        }

        byte[] bytes = newContent.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > maxFileBytes * 2L) {
            throw new IllegalArgumentException("Patched content is too large: " + relative(path));
        }
        return new PendingWrite(path, newContent, oldContent);
    }

    private int countOccurrences(String text, String needle) {
        if (needle == null || needle.isEmpty()) {
            return 0;
        }
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(needle, idx)) >= 0) {
            count++;
            idx += needle.length();
        }
        return count;
    }

    private void rollback(List<PendingWrite> applied) {
        for (int i = applied.size() - 1; i >= 0; i--) {
            PendingWrite write = applied.get(i);
            try {
                if (write.oldContent() == null) {
                    Files.deleteIfExists(write.path());
                } else {
                    Files.writeString(write.path(), write.oldContent(), StandardCharsets.UTF_8);
                }
            } catch (IOException ignored) {
                // Best-effort rollback; the caller still receives the original failure.
            }
        }
    }

    private boolean isSearchableFile(Path file) {
        Path rel = root().relativize(file);
        for (Path part : rel) {
            if (SEARCH_SKIPPED_DIRS.contains(part.toString())) {
                return false;
            }
        }
        return true;
    }

    private Path resolve(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            throw new IllegalArgumentException("Missing path");
        }
        String normalized = rawPath.replace('\\', '/');
        Path candidate = Path.of(normalized);
        if (candidate.isAbsolute() || normalized.contains(":")) {
            throw new IllegalArgumentException("Path must be relative to the configured workspace");
        }
        Path resolved = root().resolve(normalized).normalize();
        if (!resolved.startsWith(root())) {
            throw new IllegalArgumentException("Path escapes the configured workspace");
        }
        for (Path part : root().relativize(resolved)) {
            if (".git".equals(part.toString())) {
                throw new IllegalArgumentException("The .git directory is not accessible to coding tools");
            }
        }
        return resolved;
    }

    private Path root() {
        return Path.of(configuredRoot).toAbsolutePath().normalize();
    }

    private String relative(Path path) {
        return root().relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }

    private void requireConfigured() {
        if (!isConfigured()) {
            throw new IllegalStateException(
                    "Coding tools are not configured. Set katixo.code.workspace-dir or GIT_REPO_DIR.");
        }
    }

    private String requireQuery(String query) {
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) {
            throw new IllegalArgumentException("Missing query");
        }
        return q;
    }

    private String requireSafeCommand(String command) {
        String c = command == null ? "" : command.trim();
        if (c.isEmpty()) {
            throw new IllegalArgumentException("Missing command");
        }
        String lowered = " " + c.toLowerCase(Locale.ROOT) + " ";
        for (String denied : COMMAND_DENYLIST) {
            if (lowered.contains(denied)) {
                throw new IllegalArgumentException("Command is not allowed by the coding-agent guardrail");
            }
        }
        String normalized = c.replace('\\', '/').toLowerCase(Locale.ROOT);
        boolean allowed = COMMAND_PREFIX_ALLOWLIST.stream()
                .map(p -> p.replace('\\', '/').toLowerCase(Locale.ROOT))
                .anyMatch(p -> normalized.equals(p) || normalized.startsWith(p + " "));
        if (!allowed) {
            throw new IllegalArgumentException("Command must start with an allowed build/test/git prefix");
        }
        return c;
    }

    private Pattern globPattern(String glob) {
        if (glob == null || glob.isBlank()) {
            return null;
        }
        StringBuilder regex = new StringBuilder("^");
        String g = glob.trim().replace('\\', '/');
        for (int i = 0; i < g.length(); i++) {
            char ch = g.charAt(i);
            if (ch == '*') {
                boolean doublestar = i + 1 < g.length() && g.charAt(i + 1) == '*';
                regex.append(doublestar ? ".*" : "[^/]*");
                if (doublestar) {
                    i++;
                }
            } else if (ch == '?') {
                regex.append("[^/]");
            } else {
                regex.append(Pattern.quote(String.valueOf(ch)));
            }
        }
        regex.append('$');
        return Pattern.compile(regex.toString());
    }

    private boolean looksBinary(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        int limit = Math.min(bytes.length, 4096);
        for (int i = 0; i < limit; i++) {
            if (bytes[i] == 0) {
                return true;
            }
        }
        return false;
    }

    private String trimLine(String line) {
        String t = line.strip();
        return t.length() <= 220 ? t : t.substring(0, 220) + "...";
    }

    private String capped(String text) {
        if (text.length() <= OUTPUT_LIMIT) {
            return text;
        }
        return text.substring(0, OUTPUT_LIMIT) + "\n... output truncated ...";
    }

    public record PatchOperation(String path, String content, String find, String replace) {
    }

    private record PendingWrite(Path path, String newContent, String oldContent) {
    }
}
