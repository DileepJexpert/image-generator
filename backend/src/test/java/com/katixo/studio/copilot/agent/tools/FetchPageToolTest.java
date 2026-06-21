package com.katixo.studio.copilot.agent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.katixo.studio.copilot.agent.ToolResult;
import com.katixo.studio.search.WebPageFetcher;
import com.katixo.studio.search.WebPageFetcher.Page;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FetchPageToolTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void returnsTitleUrlAndReadableText() throws Exception {
        WebPageFetcher fetcher = mock(WebPageFetcher.class);
        when(fetcher.fetch(eq("https://ollama.com")))
                .thenReturn(new Page("Ollama", "https://ollama.com", "Run large language models locally.", false));
        FetchPageTool tool = new FetchPageTool(fetcher, mapper);

        ToolResult result = tool.execute(mapper.createObjectNode().put("url", "https://ollama.com"));

        assertThat(result.summary())
                .contains("Ollama").contains("https://ollama.com")
                .contains("Run large language models locally.");
        assertThat(result.jobId()).isNull();
    }

    @Test
    void exposesUrlAsRequiredAndNeedsNoApproval() {
        FetchPageTool tool = new FetchPageTool(mock(WebPageFetcher.class), mapper);
        assertThat(tool.name()).isEqualTo("fetch_page");
        assertThat(tool.requiresApproval()).isFalse();
        assertThat(tool.parameters().path("required").toString()).contains("url");
    }

    @Test
    void reportsFetchFailureGracefully() throws Exception {
        WebPageFetcher fetcher = mock(WebPageFetcher.class);
        when(fetcher.fetch(eq("http://10.0.0.1")))
                .thenThrow(new IOException("Refusing to fetch a private/loopback address: 10.0.0.1"));
        FetchPageTool tool = new FetchPageTool(fetcher, mapper);

        ToolResult result = tool.execute(mapper.createObjectNode().put("url", "http://10.0.0.1"));

        assertThat(result.summary()).contains("Could not fetch page").contains("private/loopback");
    }
}
