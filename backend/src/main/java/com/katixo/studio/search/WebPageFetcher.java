package com.katixo.studio.search;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

/**
 * Fetches a public web page and returns its readable text, for the Copilot's {@code fetch_page} tool
 * (read a result that {@code web_search} found). Uses jsoup — no API key, no paid service.
 *
 * <p>Includes a basic SSRF guard: it refuses http(s) URLs that resolve to loopback/private/link-local
 * addresses, so the agent can't be steered into reading internal services (the GPU sidecars, the
 * cloud metadata endpoint, the LAN, etc.). Studio-only — never add web fetching to katixo-docai.
 */
@Component
public class WebPageFetcher {

    private static final String USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/124.0 Safari/537.36";
    private static final int TIMEOUT_MS = 10_000;
    private static final int MAX_BODY_BYTES = 2 * 1024 * 1024;
    private static final int MAX_CHARS = 6000;   // keep within a local model's small context window

    /** Fetch {@code url} and return its title + cleaned main text (truncated to a model-friendly size). */
    public Page fetch(String url) throws IOException {
        validate(url);
        Document doc = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .maxBodySize(MAX_BODY_BYTES)
                .followRedirects(true)
                .get();

        doc.select("script, style, noscript, nav, header, footer, aside, svg, iframe, form").remove();
        String title = doc.title();
        Element body = doc.body();
        String text = (body == null ? doc.text() : body.text()).replaceAll("\\s+", " ").trim();
        boolean truncated = text.length() > MAX_CHARS;
        if (truncated) {
            text = text.substring(0, MAX_CHARS);
        }
        return new Page(title, url, text, truncated);
    }

    private void validate(String url) throws IOException {
        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid URL: " + url);
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new IOException("Only http/https URLs are allowed.");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IOException("URL has no host.");
        }
        if (isBlockedHost(host)) {
            throw new IOException("Refusing to fetch a private/loopback address: " + host);
        }
    }

    /** Block obvious internal targets by name and by resolved address. */
    private boolean isBlockedHost(String host) {
        String h = host.toLowerCase();
        if (h.equals("localhost") || h.endsWith(".localhost") || h.endsWith(".internal")
                || h.equals("metadata.google.internal")) {
            return true;
        }
        try {
            InetAddress addr = InetAddress.getByName(host);
            return addr.isLoopbackAddress() || addr.isAnyLocalAddress()
                    || addr.isSiteLocalAddress() || addr.isLinkLocalAddress();
        } catch (UnknownHostException e) {
            return false;   // let the fetch itself fail if the host can't resolve
        }
    }

    public record Page(String title, String url, String text, boolean truncated) {
    }
}
