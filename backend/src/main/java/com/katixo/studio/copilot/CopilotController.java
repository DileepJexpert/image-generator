package com.katixo.studio.copilot;

import com.katixo.studio.copilot.CopilotDtos.ChatRequest;
import com.katixo.studio.copilot.CopilotDtos.ChatResponse;
import com.katixo.studio.copilot.CopilotDtos.ModelSummary;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

/**
 * Copilot API (VISION.md Phase 3): a local-LLM assistant for prompts, copy, and
 * studio help. Interactive request/response — not a generation job (see
 * {@link OllamaClient}). Sidecar failures map to 502 via the global handler.
 */
@RestController
@RequestMapping("/api/v1/copilot")
public class CopilotController {

    private final CopilotService copilotService;

    public CopilotController(CopilotService copilotService) {
        this.copilotService = copilotService;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request)
            throws IOException, InterruptedException {
        return copilotService.chat(request);
    }

    /** Streams the reply token-by-token over Server-Sent Events. */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@Valid @RequestBody ChatRequest request) {
        return copilotService.chatStream(request);
    }

    @GetMapping("/models")
    public List<ModelSummary> models() throws IOException, InterruptedException {
        return copilotService.listModels();
    }
}
