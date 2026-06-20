package com.katixo.studio.copilot.agent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.katixo.studio.copilot.agent.CopilotTool;
import com.katixo.studio.copilot.agent.ToolResult;
import com.katixo.studio.copilot.agent.ToolSchema;
import com.katixo.studio.search.WebSearchClient;
import com.katixo.studio.search.WebSearchClient.Result;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * Lets the Copilot search the public web and get back titles, URLs and snippets — so the agent can
 * research current topics or find pages to read. Read-only retrieval; it is NOT GPU work, so it does
 * not touch the {@code GpuResourceGuard}. The backend is free (SearXNG or DuckDuckGo) — no paid API.
 */
@Component
public class WebSearchTool implements CopilotTool {

    private static final int DEFAULT_RESULTS = 5;
    private static final int MAX_RESULTS = 10;

    private final WebSearchClient searchClient;
    private final ObjectMapper mapper;

    public WebSearchTool(WebSearchClient searchClient, ObjectMapper mapper) {
        this.searchClient = searchClient;
        this.mapper = mapper;
    }

    @Override
    public String name() {
        return "web_search";
    }

    @Override
    public String description() {
        return "Search the public web for current information. Returns a numbered list of titles, "
                + "URLs and snippets. Use it to research a topic or find pages to read.";
    }

    @Override
    public JsonNode parameters() {
        return ToolSchema.object(mapper)
                .prop("query", "string", "The search query.")
                .prop("maxResults", "integer", "How many results to return (1-10, default 5).")
                .require("query")
                .build();
    }

    @Override
    public ToolResult execute(JsonNode args) {
        String query = Args.requireText(args, "query");
        int max = Args.clampedInt(args, "maxResults", DEFAULT_RESULTS, 1, MAX_RESULTS);
        try {
            List<Result> results = searchClient.search(query, max);
            if (results.isEmpty()) {
                return ToolResult.text("No results for \"" + query + "\".");
            }
            StringBuilder sb = new StringBuilder("Top results for \"" + query + "\":\n");
            int i = 1;
            for (Result r : results) {
                sb.append(i++).append(". ").append(r.title()).append('\n')
                        .append("   ").append(r.url()).append('\n');
                if (!r.snippet().isBlank()) {
                    sb.append("   ").append(r.snippet()).append('\n');
                }
            }
            return ToolResult.text(sb.toString().trim());
        } catch (IOException e) {
            return ToolResult.text("Web search failed: " + e.getMessage());
        }
    }
}
