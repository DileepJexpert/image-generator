package com.katixo.studio.copilot.agent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.katixo.studio.copilot.agent.ToolResult;
import com.katixo.studio.git.GitClient;
import com.katixo.studio.git.GithubClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GithubCreatePrToolTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void isApprovalGatedAndNamed() {
        GithubCreatePrTool tool = new GithubCreatePrTool(
                mock(GithubClient.class), mock(GitClient.class), mapper);

        assertThat(tool.name()).isEqualTo("github_create_pr");
        assertThat(tool.requiresApproval()).isTrue();
    }

    @Test
    void tellsUserWhenNotConfigured() {
        GithubClient github = mock(GithubClient.class);
        when(github.isConfigured()).thenReturn(false);
        GithubCreatePrTool tool = new GithubCreatePrTool(github, mock(GitClient.class), mapper);

        ToolResult result = tool.execute(mapper.createObjectNode()
                .put("title", "Add tools")
                .put("base", "main"));

        assertThat(result.summary()).contains("not configured").contains("GITHUB_TOKEN");
    }

    @Test
    void createsPrUsingCurrentBranchWhenHeadOmitted() throws Exception {
        GithubClient github = mock(GithubClient.class);
        GitClient git = mock(GitClient.class);
        when(github.isConfigured()).thenReturn(true);
        when(git.currentBranch()).thenReturn("codex/add-tools");
        when(github.createPullRequest("Add tools", "Body", "codex/add-tools", "main"))
                .thenReturn("https://github.com/DileepJexpert/image-generator/pull/25");
        GithubCreatePrTool tool = new GithubCreatePrTool(github, git, mapper);

        ToolResult result = tool.execute(mapper.createObjectNode()
                .put("title", "Add tools")
                .put("body", "Body")
                .put("base", "main"));

        assertThat(result.summary()).contains("Created PR")
                .contains("https://github.com/DileepJexpert/image-generator/pull/25");
    }
}
