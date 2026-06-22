package com.katixo.studio.copilot.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.katixo.studio.config.KatixoProperties;
import com.katixo.studio.copilot.ChatMessage;
import com.katixo.studio.copilot.OllamaClient;
import com.katixo.studio.copilot.agent.AgentDtos.AgentRequest;
import com.katixo.studio.copilot.agent.AgentDtos.AgentResponse;
import com.katixo.studio.copilot.agent.AssistantTurn.ToolCall;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the agent loop: it should execute an auto tool then return the
 * model's final answer, and it should <em>propose</em> (not run) an
 * approval-gated tool.
 */
class AgentServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private AgentService serviceWith(OllamaClient ollama, CopilotTool tool) {
        ToolRegistry registry = new ToolRegistry(List.of(tool), mapper);
        KatixoProperties props = new KatixoProperties(
                null, null, null, null, null, null, null, "test-model", null);
        return new AgentService(ollama, registry, props, mapper);
    }

    @Test
    void executesAutoToolThenReturnsFinalMessage() throws Exception {
        OllamaClient ollama = mock(OllamaClient.class);
        AtomicReference<JsonNode> captured = new AtomicReference<>();
        UUID jobId = UUID.randomUUID();
        CopilotTool echo = tool("echo", false, args -> {
            captured.set(args);
            return ToolResult.job("ran echo", jobId);
        });

        ObjectNode args = mapper.createObjectNode().put("x", "y");
        when(ollama.chatWithTools(any(), any(), any())).thenReturn(
                new AssistantTurn(mapper.createObjectNode(), "", List.of(new ToolCall("echo", args))),
                new AssistantTurn(mapper.createObjectNode(), "All done.", List.of()));

        AgentResponse resp = serviceWith(ollama, echo)
                .run(new AgentRequest(List.of(new ChatMessage("user", "go")), null, null));

        assertThat(captured.get()).isEqualTo(args);
        assertThat(resp.message().content()).isEqualTo("All done.");
        assertThat(resp.steps()).singleElement()
                .satisfies(s -> {
                    assertThat(s.status()).isEqualTo("done");
                    assertThat(s.jobId()).isEqualTo(jobId);
                });
        assertThat(resp.pendingActions()).isEmpty();
    }

    @Test
    void approvalGatedToolIsProposedNotExecuted() throws Exception {
        OllamaClient ollama = mock(OllamaClient.class);
        AtomicReference<Boolean> ran = new AtomicReference<>(false);
        CopilotTool scrape = tool("scrape", true, args -> {
            ran.set(true);
            return ToolResult.text("should not run");
        });

        ObjectNode args = mapper.createObjectNode();
        when(ollama.chatWithTools(any(), any(), any())).thenReturn(
                new AssistantTurn(mapper.createObjectNode(), "", List.of(new ToolCall("scrape", args))),
                new AssistantTurn(mapper.createObjectNode(), "Waiting on you.", List.of()));

        AgentResponse resp = serviceWith(ollama, scrape)
                .run(new AgentRequest(List.of(new ChatMessage("user", "find leads")), null, null));

        assertThat(ran.get()).isFalse();
        assertThat(resp.pendingActions()).singleElement()
                .satisfies(p -> assertThat(p.tool()).isEqualTo("scrape"));
        assertThat(resp.steps()).singleElement()
                .satisfies(s -> assertThat(s.status()).isEqualTo("pending_approval"));
    }

    /** Minimal inline tool for the loop under test. */
    private CopilotTool tool(String name, boolean approval,
                             java.util.function.Function<JsonNode, ToolResult> body) {
        return new CopilotTool() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public String description() {
                return name;
            }

            @Override
            public JsonNode parameters() {
                return ToolSchema.object(mapper).build();
            }

            @Override
            public boolean requiresApproval() {
                return approval;
            }

            @Override
            public ToolResult execute(JsonNode args) {
                return body.apply(args);
            }
        };
    }
}
