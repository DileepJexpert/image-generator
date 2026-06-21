package com.katixo.studio.copilot.agent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.katixo.studio.copilot.agent.CopilotTool;
import com.katixo.studio.copilot.agent.ToolResult;
import com.katixo.studio.copilot.agent.ToolSchema;
import com.katixo.studio.search.WebPageFetcher;
import com.katixo.studio.search.WebPageFetcher.Page;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Lets the Copilot read a web page by URL and get back its readable text — the companion to
 * {@code web_search} that turns "find links" into actual research. Read-only retrieval of a public
 * page; not GPU work, so it does not touch the {@code GpuResourceGuard}.
 */
@Component
public class FetchPageTool implements CopilotTool {

    private final WebPageFetcher fetcher;
    private final ObjectMapper mapper;

    public FetchPageTool(WebPageFetcher fetcher, ObjectMapper mapper) {
        this.fetcher = fetcher;
        this.mapper = mapper;
    }

    @Override
    public String name() {
        return "fetch_page";
    }

    @Override
    public String description() {
        return "Fetch a web page by its http(s) URL and return the readable text (title + main "
                + "content). Use it after web_search to actually read a result.";
    }

    @Override
    public JsonNode parameters() {
        return ToolSchema.object(mapper)
                .prop("url", "string", "The http(s) URL of the page to read.")
                .require("url")
                .build();
    }

    @Override
    public ToolResult execute(JsonNode args) {
        String url = Args.requireText(args, "url");
        try {
            Page page = fetcher.fetch(url);
            String header = page.title().isBlank() ? page.url() : page.title() + " — " + page.url();
            String body = page.text().isBlank() ? "(no readable text found on the page)" : page.text();
            return ToolResult.text(header + "\n\n" + body + (page.truncated() ? "\n…(truncated)" : ""));
        } catch (IOException e) {
            return ToolResult.text("Could not fetch page: " + e.getMessage());
        }
    }
}
