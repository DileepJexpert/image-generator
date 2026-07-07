package com.katixo.studio.git;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Minimal GitHub REST client for opening pull requests from the Copilot. */
@Component
public class GithubClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final String token;
    private final String repository;
    private final String apiUrl;
    private final ObjectMapper mapper;
    private final HttpClient http;

    public GithubClient(
            @Value("${katixo.github.token:${GITHUB_TOKEN:}}") String token,
            @Value("${katixo.github.repository:${GITHUB_REPOSITORY:}}") String repository,
            @Value("${katixo.github.api-url:https://api.github.com}") String apiUrl,
            ObjectMapper mapper
    ) {
        this.token = token == null ? "" : token.trim();
        this.repository = repository == null ? "" : repository.trim();
        this.apiUrl = apiUrl == null ? "https://api.github.com" : apiUrl.stripTrailing();
        this.mapper = mapper;
        this.http = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
    }

    public boolean isConfigured() {
        return !token.isBlank() && repository.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+");
    }

    public String createPullRequest(String title, String body, String head, String base)
            throws IOException, InterruptedException {
        if (!isConfigured()) {
            throw new IOException("GitHub PR creation is not configured. Set katixo.github.repository and GITHUB_TOKEN.");
        }
        String safeTitle = require(title, "title");
        String safeHead = require(head, "head branch");
        String safeBase = require(base, "base branch");

        var payload = mapper.createObjectNode();
        payload.put("title", safeTitle);
        payload.put("head", safeHead);
        payload.put("base", safeBase);
        payload.put("body", body == null ? "" : body);

        HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl + "/repos/" + repository + "/pulls"))
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "Bearer " + token)
                .header("X-GitHub-Api-Version", "2022-11-28")
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) {
            throw new IOException("GitHub create PR failed (" + response.statusCode() + "): "
                    + compact(response.body()));
        }
        JsonNode json = mapper.readTree(response.body());
        return json.path("html_url").asText();
    }

    private String require(String value, String label) {
        String v = value == null ? "" : value.trim();
        if (v.isBlank()) {
            throw new IllegalArgumentException("Missing " + label);
        }
        if (v.contains("\n") || v.contains("\r")) {
            throw new IllegalArgumentException("Invalid " + label);
        }
        return v;
    }

    private String compact(String body) {
        if (body == null) {
            return "";
        }
        String oneLine = body.replace('\n', ' ').replace('\r', ' ').trim();
        return oneLine.length() <= 500 ? oneLine : oneLine.substring(0, 500) + "...";
    }
}
