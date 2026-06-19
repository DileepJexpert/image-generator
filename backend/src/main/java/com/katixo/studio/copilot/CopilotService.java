package com.katixo.studio.copilot;

import com.katixo.studio.config.KatixoProperties;
import com.katixo.studio.copilot.CopilotDtos.ChatRequest;
import com.katixo.studio.copilot.CopilotDtos.ChatResponse;
import com.katixo.studio.copilot.CopilotDtos.ModelSummary;
import com.katixo.studio.copilot.CopilotDtos.TokenEvent;
import jakarta.annotation.PreDestroy;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Orchestrates Copilot chat: resolves the model, injects the studio system
 * prompt, and delegates to {@link OllamaClient}. Controllers stay thin
 * (CLAUDE.md §6).
 */
@Service
public class CopilotService {

    /**
     * Frames the assistant as Katixo's in-app creative copilot. Phase 3 keeps
     * this as plain chat/assist; the tool-driving agent loop arrives in a later
     * phase (see VISION.md).
     */
    private static final String SYSTEM_PROMPT = """
            You are Katixo Copilot, the built-in assistant of Katixo Studio — a \
            local, GPU-powered design and media studio. Help the user create: \
            write and refine image/video generation prompts, suggest layouts, \
            captions, and copy, and answer questions about using the studio. \
            Be concise and practical. When asked for an image prompt, return a \
            single vivid prompt the user can paste into the image panel.""";

    private final OllamaClient ollama;
    private final KatixoProperties properties;

    // Streaming holds a worker thread for the life of each SSE response, so it
    // can't share the request thread. A small daemon pool keeps it off the
    // container's request executor; single-user load stays tiny.
    private final ExecutorService streamExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "copilot-stream");
        t.setDaemon(true);
        return t;
    });

    public CopilotService(OllamaClient ollama, KatixoProperties properties) {
        this.ollama = ollama;
        this.properties = properties;
    }

    public ChatResponse chat(ChatRequest request) throws IOException, InterruptedException {
        String model = resolveModel(request);
        List<ChatMessage> messages = withSystemPrompt(request.messages());
        String reply = ollama.chat(model, messages);
        return new ChatResponse(model, ChatMessage.assistant(reply));
    }

    /**
     * Streams the assistant's reply token-by-token over Server-Sent Events. Each
     * chunk is a {@code data:} event carrying {@link TokenEvent} JSON; a final
     * {@code done} event closes the stream.
     */
    public SseEmitter chatStream(ChatRequest request) {
        String model = resolveModel(request);
        List<ChatMessage> messages = withSystemPrompt(request.messages());
        SseEmitter emitter = new SseEmitter(Duration.ofMinutes(5).toMillis());

        streamExecutor.execute(() -> {
            try {
                ollama.chatStream(model, messages, token -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .data(new TokenEvent(token), MediaType.APPLICATION_JSON));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
                emitter.send(SseEmitter.event().name("done").data("{}", MediaType.APPLICATION_JSON));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    private String resolveModel(ChatRequest request) {
        return (request.model() == null || request.model().isBlank())
                ? properties.copilotModel()
                : request.model();
    }

    @PreDestroy
    void shutdown() {
        streamExecutor.shutdownNow();
    }

    public List<ModelSummary> listModels() throws IOException, InterruptedException {
        return ollama.listModels();
    }

    /** Prepend the studio system prompt unless the caller already set one. */
    private List<ChatMessage> withSystemPrompt(List<ChatMessage> incoming) {
        boolean hasSystem = incoming.stream()
                .anyMatch(m -> "system".equalsIgnoreCase(m.role()));
        if (hasSystem) {
            return incoming;
        }
        List<ChatMessage> messages = new ArrayList<>(incoming.size() + 1);
        messages.add(ChatMessage.system(SYSTEM_PROMPT));
        messages.addAll(incoming);
        return messages;
    }
}
