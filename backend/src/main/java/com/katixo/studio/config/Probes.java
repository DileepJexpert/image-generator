package com.katixo.studio.config;

import com.katixo.ai.commons.sidecar.SidecarHealth;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Shared reachability probe for the JDK-HttpClient sidecar clients. Any HTTP response (even a 404)
 * means the sidecar process is up; only a transport failure (connection refused, timeout) is "down".
 */
public final class Probes {

    private Probes() {
    }

    public static SidecarHealth reachable(HttpClient client, String baseUrl, String name) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            client.send(req, HttpResponse.BodyHandlers.discarding());
            return SidecarHealth.up(name);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return SidecarHealth.down(name, "interrupted");
        } catch (Exception e) {
            return SidecarHealth.down(name, e.getMessage());
        }
    }
}
