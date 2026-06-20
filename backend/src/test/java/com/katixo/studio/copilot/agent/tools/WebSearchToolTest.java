package com.katixo.studio.copilot.agent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.katixo.studio.copilot.agent.ToolResult;
import com.katixo.studio.search.WebSearchClient;
import com.katixo.studio.search.WebSearchClient.Result;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebSearchToolTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void formatsResultsWithTitlesAndUrls() throws Exception {
        WebSearchClient client = mock(WebSearchClient.class);
        when(client.search(eq("ollama"), anyInt())).thenReturn(List.of(
                new Result("Ollama", "https://ollama.com", "Run LLMs locally"),
                new Result("GitHub", "https://github.com/ollama/ollama", "")));
        WebSearchTool tool = new WebSearchTool(client, mapper);

        ToolResult result = tool.execute(mapper.createObjectNode().put("query", "ollama"));

        assertThat(result.summary())
                .contains("Ollama").contains("https://ollama.com")
                .contains("Run LLMs locally").contains("https://github.com/ollama/ollama");
        assertThat(result.jobId()).isNull();
    }

    @Test
    void exposesQueryAsRequiredAndNeedsNoApproval() {
        WebSearchTool tool = new WebSearchTool(mock(WebSearchClient.class), mapper);
        assertThat(tool.name()).isEqualTo("web_search");
        assertThat(tool.requiresApproval()).isFalse();
        assertThat(tool.parameters().path("required").toString()).contains("query");
    }

    @Test
    void reportsSearchFailureGracefully() throws Exception {
        WebSearchClient client = mock(WebSearchClient.class);
        when(client.search(eq("x"), anyInt())).thenThrow(new IOException("network down"));
        WebSearchTool tool = new WebSearchTool(client, mapper);

        ToolResult result = tool.execute(mapper.createObjectNode().put("query", "x"));

        assertThat(result.summary()).contains("Web search failed").contains("network down");
    }
}
