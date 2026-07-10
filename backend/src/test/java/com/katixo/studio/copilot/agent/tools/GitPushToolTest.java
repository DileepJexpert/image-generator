package com.katixo.studio.copilot.agent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.katixo.studio.code.WorkspaceCheckStatus;
import com.katixo.studio.copilot.agent.ToolResult;
import com.katixo.studio.git.GitClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GitPushToolTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final WorkspaceCheckStatus checkStatus = new WorkspaceCheckStatus();

    @Test
    void isApprovalGatedAndNamed() {
        GitPushTool tool = new GitPushTool(mock(GitClient.class), mapper, checkStatus);
        assertThat(tool.name()).isEqualTo("git_push");
        assertThat(tool.requiresApproval()).isTrue();
    }

    @Test
    void tellsUserWhenNotConfigured() {
        GitClient git = mock(GitClient.class);
        when(git.isConfigured()).thenReturn(false);
        GitPushTool tool = new GitPushTool(git, mapper, checkStatus);

        ToolResult result = tool.execute(mapper.createObjectNode().put("message", "wip"));

        assertThat(result.summary()).contains("not configured").contains("katixo.git.repo-dir");
    }

    @Test
    void commitsAndPushesWhenConfigured() throws Exception {
        GitClient git = mock(GitClient.class);
        when(git.isConfigured()).thenReturn(true);
        when(git.commitAndPush("add web_search")).thenReturn("main -> main");
        GitPushTool tool = new GitPushTool(git, mapper, checkStatus);

        ToolResult result = tool.execute(mapper.createObjectNode().put("message", "add web_search"));

        assertThat(result.summary()).contains("Pushed").contains("main -> main");
    }

    @Test
    void approvalPreviewWarnsUntilAPassingCheck() {
        GitPushTool tool = new GitPushTool(mock(GitClient.class), mapper, checkStatus);
        var args = mapper.createObjectNode().put("message", "wip");

        assertThat(tool.approvalPreview(args)).contains("No build/test check");

        checkStatus.recordCommandResult("mvnw test", true);
        assertThat(tool.approvalPreview(args)).contains("Checks passed");

        checkStatus.recordEdit();
        assertThat(tool.approvalPreview(args)).contains("changed since the last passing check");
    }
}
