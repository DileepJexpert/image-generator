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
    void failingCheckBlocksPushUntilItPasses() {
        WorkspaceCheckStatus status = new WorkspaceCheckStatus();
        status.recordCommandResult("flutter test", false);

        assertThat(status.isGreen()).isFalse();
        assertThat(status.pushBlockReason()).isPresent();
        assertThat(status.pushBlockReason().get()).contains("flutter test");
        assertThat(status.advisory()).contains("blocked");

        status.recordCommandResult("flutter test", true);
        assertThat(status.pushBlockReason()).isEmpty();
    }

    @Test
    void noCheckOrPassingCheckDoesNotBlockPush() {
        WorkspaceCheckStatus status = new WorkspaceCheckStatus();
        assertThat(status.pushBlockReason()).isEmpty(); // NONE: warn, don't block

        status.recordCommandResult("mvnw test", true);
        assertThat(status.pushBlockReason()).isEmpty();
    }

    @Test
    void runningCheckIsReportedAndDoesNotBlock() {
        WorkspaceCheckStatus status = new WorkspaceCheckStatus();
        status.recordCheckStarted("mvnw verify");
        assertThat(status.advisory()).contains("check is running").contains("mvnw verify");
        assertThat(status.pushBlockReason()).isEmpty();
    }

    @Test
    void readOnlyGitCommandsDoNotCountAsChecks() {
        WorkspaceCheckStatus status = new WorkspaceCheckStatus();
        status.recordCommandResult("git status", true);
        assertThat(status.isGreen()).isFalse();
        assertThat(status.advisory()).contains("No build/test check");
    }
}
