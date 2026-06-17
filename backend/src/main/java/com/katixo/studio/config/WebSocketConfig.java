package com.katixo.studio.config;

import com.katixo.studio.job.JobProgressSocket;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/** Registers the raw WebSocket handler for job progress streaming. */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final JobProgressSocket jobProgressSocket;

    public WebSocketConfig(JobProgressSocket jobProgressSocket) {
        this.jobProgressSocket = jobProgressSocket;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(jobProgressSocket, "/ws/jobs/*")
                .setAllowedOriginPatterns("*");
    }
}
