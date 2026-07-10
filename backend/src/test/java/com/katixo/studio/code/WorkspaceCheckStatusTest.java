package com.katixo.studio.code;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WorkspaceCheckStatusTest {

    @Test
    void startsWithNoCheck() {
        WorkspaceCheckStatus status = new WorkspaceCheckStatus();
        assertThat(status.isGreen()).isFalse();
        assertThat(status.advisory()).contains("No build/test check");
    }

    @Test
    void passingCheckGoesGreenUntilNextEdit() {
        WorkspaceCheckStatus status = new WorkspaceCheckStatus();

        status.recordCommandResult("mvnw test", true);
        assertThat(status.isGreen()).isTrue();
        assertThat(status.advisory()).contains("Checks passed").contains("mvnw test");

        status.recordEdit();
        assertThat(status.isGreen()).isFalse();
        assertThat(status.advisory()).contains("changed since the last passing check");
    }

    @Test
    void failingCheckIsNotGreen() {
        WorkspaceCheckStatus status = new WorkspaceCheckStatus();
        status.recordCommandResult("flutter test", false);
        assertThat(status.isGreen()).isFalse();
        assertThat(status.advisory()).contains("last check failed");
    }

    @Test
    void readOnlyGitCommandsDoNotCountAsChecks() {
        WorkspaceCheckStatus status = new WorkspaceCheckStatus();
        status.recordCommandResult("git status", true);
        assertThat(status.isGreen()).isFalse();
        assertThat(status.advisory()).contains("No build/test check");
    }
}
