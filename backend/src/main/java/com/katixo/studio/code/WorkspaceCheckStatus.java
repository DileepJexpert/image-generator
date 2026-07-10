package com.katixo.studio.code;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;

/**
 * In-memory record of whether the workspace has a passing build/test check since
 * its last edit. Two jobs:
 *
 * <ul>
 *   <li>Feeds the approval card for {@code git_push} / {@code git_commit} so a
 *       human confirming a push sees the check state first.</li>
 *   <li>Provides a reliable hard block: if the last check <em>failed</em>, the
 *       push/commit tools refuse. This is enforced in the tools' {@code execute()},
 *       which for approval-gated tools runs at confirm time — a separate call after
 *       a real human delay, by which point any async check job has finished and
 *       this status is trustworthy.</li>
 * </ul>
 *
 * <p>Only a <em>failed</em> check blocks; "no check run" or "stale after an edit"
 * merely warn, so legitimate no-test pushes (e.g. docs) are not obstructed while
 * known-broken code cannot be pushed.
 */
@Component
public class WorkspaceCheckStatus {

    private enum State { NONE, RUNNING, PASSED, FAILED }

    private State state = State.NONE;
    private String lastCheckCommand = "";
    private boolean editedSinceCheck = false;

    /** Marks that a check command has been submitted and is now running. */
    public synchronized void recordCheckStarted(String command) {
        if (!isCheckCommand(command)) {
            return;
        }
        this.state = State.RUNNING;
        this.lastCheckCommand = command.trim();
    }

    /** Records the outcome of a finished command. Read-only git commands don't count as checks. */
    public synchronized void recordCommandResult(String command, boolean passed) {
        if (!isCheckCommand(command)) {
            return;
        }
        this.state = passed ? State.PASSED : State.FAILED;
        this.lastCheckCommand = command == null ? "" : command.trim();
        this.editedSinceCheck = false;
    }

    /** Marks that a file changed, so any earlier passing check is now stale. */
    public synchronized void recordEdit() {
        this.editedSinceCheck = true;
    }

    /** True only if a check passed and nothing has been edited since. */
    public synchronized boolean isGreen() {
        return state == State.PASSED && !editedSinceCheck;
    }

    /**
     * Present when a push should be hard-blocked: the last check failed, so the
     * code is known-broken. The value is a human-readable reason for the refusal.
     */
    public synchronized Optional<String> pushBlockReason() {
        if (state == State.FAILED) {
            return Optional.of("the last check failed (" + lastCheckCommand
                    + "). Fix the failure and re-run the check before pushing");
        }
        return Optional.empty();
    }

    /** One-line status for the approval card. Always safe to show to a human. */
    public synchronized String advisory() {
        return switch (state) {
            case NONE -> "⚠ No build/test check has run this session — this may push untested code.";
            case RUNNING -> "⏳ A check is running (" + lastCheckCommand + ") — wait for it to finish.";
            case FAILED -> "⛔ The last check failed (" + lastCheckCommand
                    + ") — pushing is blocked until it passes.";
            case PASSED -> editedSinceCheck
                    ? "⚠ Files changed since the last passing check (" + lastCheckCommand
                            + ") — re-run it to be sure."
                    : "✓ Checks passed since the last edit (" + lastCheckCommand + ").";
        };
    }

    private boolean isCheckCommand(String command) {
        if (command == null || command.isBlank()) {
            return false;
        }
        // Read-only git inspection (status/diff/log) must not count as a check.
        return !command.trim().toLowerCase(Locale.ROOT).startsWith("git ");
    }
}
