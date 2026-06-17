package com.katixo.studio.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

/**
 * WebSocket endpoint backing {@code /ws/jobs/{jobId}}. Keeps a registry of
 * subscriber sessions per job and broadcasts {@link JobDto} progress events.
 */
@Component
public class JobProgressSocket extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(JobProgressSocket.class);

    private final ObjectMapper objectMapper;
    private final Map<UUID, Set<WebSocketSession>> subscribers = new ConcurrentHashMap<>();

    public JobProgressSocket(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        UUID jobId = jobIdOf(session);
        if (jobId == null) {
            return;
        }
        subscribers.computeIfAbsent(jobId, k -> ConcurrentHashMap.newKeySet()).add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        UUID jobId = jobIdOf(session);
        if (jobId == null) {
            return;
        }
        Set<WebSocketSession> sessions = subscribers.get(jobId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                subscribers.remove(jobId);
            }
        }
    }

    /** Push a job event to all sockets currently subscribed to that job. */
    public void broadcast(JobDto event) {
        Set<WebSocketSession> sessions = subscribers.get(event.id());
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            log.warn("Failed to serialize job event {}", event.id(), e);
            return;
        }
        TextMessage message = new TextMessage(payload);
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                continue;
            }
            try {
                session.sendMessage(message);
            } catch (IOException e) {
                log.debug("Failed to send progress to session {}", session.getId(), e);
            }
        }
    }

    private UUID jobIdOf(WebSocketSession session) {
        if (session.getUri() == null) {
            return null;
        }
        String path = session.getUri().getPath();
        String last = path.substring(path.lastIndexOf('/') + 1);
        try {
            return UUID.fromString(last);
        } catch (IllegalArgumentException e) {
            log.warn("Job WebSocket opened with non-UUID path segment: {}", last);
            return null;
        }
    }
}
