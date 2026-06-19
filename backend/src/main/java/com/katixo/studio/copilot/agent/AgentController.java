package com.katixo.studio.copilot.agent;

import com.katixo.studio.copilot.agent.AgentDtos.AgentRequest;
import com.katixo.studio.copilot.agent.AgentDtos.AgentResponse;
import com.katixo.studio.copilot.agent.AgentDtos.ConfirmRequest;
import com.katixo.studio.copilot.agent.AgentDtos.ToolStep;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Copilot agent API: a tool-calling loop over the local LLM. Unlike
 * {@code /copilot/chat} (plain assistant), the agent can act in the studio by
 * calling a fixed registry of tools. Sidecar failures map to 502 via the global
 * handler.
 */
@RestController
@RequestMapping("/api/v1/copilot/agent")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    /** Runs one agent turn: may execute tools and/or propose approval-gated ones. */
    @PostMapping
    public AgentResponse run(@Valid @RequestBody AgentRequest request)
            throws IOException, InterruptedException {
        return agentService.run(request);
    }

    /** Executes an approval-gated action the user confirmed from the UI. */
    @PostMapping("/confirm")
    public ToolStep confirm(@Valid @RequestBody ConfirmRequest request) {
        return agentService.confirm(request);
    }
}
