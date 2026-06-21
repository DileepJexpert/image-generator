package com.katixo.studio.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Free web search for the Copilot's {@code web_search} tool. Uses a self-hosted SearXNG instance if
 * one is configured ({@code katixo.search.searxng-url}); otherwise it falls back to scraping
 * DuckDuckGo's HTML endpoint with jsoup — no API key, no paid service.
 *
 * <p>This reaches the public internet, which is fine for Studio (a creative app). It must never be
 * added to katixo-docai, whose documents stay on-host.
 */
@Component
public class WebSearchClient {

    private static final String USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/124.0 Safari/537.36";
    private static final int TIMEOUT_MS = 10_000;

    private final String searxngUrl;
    private final ObjectMapper mapper;

    public WebSearchClient(@Value("${katixo.search.searxng-url:}") String searxngUrl, ObjectMapper mapper) {
        this.searxngUrl = searxngUrl == null ? "" : searxngUrl.trim();
        this.mapper = mapper;
    }

    /** Runs a search and returns up to {@code maxResults} results. */
    public List<Result> search(String query, int maxResults) throws IOException {
        List<Result> results = searxngUrl.isBlank() ? searchDuckDuckGo(query) : searchSearxng(query);
        return results.size() > maxResults ? results.subList(0, maxResults) : results;
    }

    private List<Result> searchSearxng(String query) throws IOException {
        String url = searxngUrl.replaceAll("/+$", "")
                + "/search?format=json&q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
        String body = Jsoup.connect(url)
                .ignoreContentType(true)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .execute()
                .body();
        JsonNode json = mapper.readTree(body);
        List<Result> out = new ArrayList<>();
        for (JsonNode r : json.path("results")) {
            String link = r.path("url").asText("");
            if (link.isBlank()) {
                continue;
            }
            out.add(new Result(r.path("title").asText(""), link, r.path("content").asText("")));
        }
        return out;
    }

    private List<Result> searchDuckDuckGo(String query) throws IOException {
        Document doc = Jsoup.connect("https://html.duckduckgo.com/html/")
                .data("q", query)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .post();
        List<Result> out = new ArrayList<>();
        Elements rows = doc.select("div.result, div.web-result");
        for (Element row : rows) {
            Element a = row.selectFirst("a.result__a");
            if (a == null) {
                continue;
            }
            String link = decodeDuckDuckGoHref(a.attr("href"));
            if (link.isBlank()) {
                continue;
            }
            Element snippet = row.selectFirst(".result__snippet");
            out.add(new Result(a.text(), link, snippet == null ? "" : snippet.text()));
        }
        return out;
    }

    /** DuckDuckGo wraps result links as {@code //duckduckgo.com/l/?uddg=<encoded-url>}; unwrap them. */
    private static String decodeDuckDuckGoHref(String href) {
        if (href == null || href.isBlank()) {
            return "";
        }
        int idx = href.indexOf("uddg=");
        if (idx < 0) {
            return href.startsWith("//") ? "https:" + href : href;
        }
        String enc = href.substring(idx + 5);
        int amp = enc.indexOf('&');
        if (amp >= 0) {
            enc = enc.substring(0, amp);
        }
        return URLDecoder.decode(enc, StandardCharsets.UTF_8);
    }

    public record Result(String title, String url, String snippet) {
    }
}
