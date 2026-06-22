package com.katixo.studio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized URLs + paths for infrastructure the monolith calls or uses.
 * Bound from the {@code katixo.*} block in application.yml (env-overridable).
 */
@ConfigurationProperties("katixo")
public record KatixoProperties(
        String comfyuiUrl,
        String rembgUrl,
        String esrganUrl,
        String ttsUrl,
        String whisperUrl,
        String ffmpegUrl,
        String ollamaUrl,
        String copilotModel,
        String assetStoragePath
) {
}
