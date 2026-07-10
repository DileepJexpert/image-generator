package com.katixo.studio.code;

import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * In-memory record of whether the workspace has a passing build/test check since
 * its last edit. Feeds the approval card for {@code git_push} / {@code git_commit}
 * so a human confirming a push sees whether they are about to ship untested code.
 *
 * <p>It is advisory, not a hard gate: commands run as async jobs, so a check's
 * result is not known within the agent turn that proposes a push — but by the time
 * the human confirms (a separate call, after real delay) the job has finished and
 * this status is reliable. Blocking silently would override the human's explicit
 * confirmation; surfacing the risk on the card lets them decide with full context.
 */
@Component
public class WorkspaceCheckStatus {

    private enum State { NONE, PASSED, FAILED }

    private State state = State.NONE;
    private String lastCheckCommand = "";
    private boolean editedSinceCheck = false;

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

    /** One-line status for the approval card. Always safe to show to a human. */
    public synchronized String advisory() {
        return switch (state) {
            case NONE -> "⚠ No build/test check has run this session — this may push untested code.";
            case FAILED -> "⚠ The last check failed (" + lastCheckCommand
                    + ") — this may push broken code.";
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
        // Read-only git inspection (status/diff/log) must not count as a passing check.
        return !command.trim().toLowerCase(Locale.ROOT).startsWith("git ");
    }
}
