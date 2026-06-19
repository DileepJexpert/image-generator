package com.katixo.studio.copilot.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.katixo.studio.config.KatixoProperties;
import com.katixo.studio.copilot.ChatMessage;
import com.katixo.studio.copilot.OllamaClient;
import com.katixo.studio.copilot.agent.AgentDtos.AgentRequest;
import com.katixo.studio.copilot.agent.AgentDtos.AgentResponse;
import com.katixo.studio.copilot.agent.AgentDtos.ConfirmRequest;
import com.katixo.studio.copilot.agent.AgentDtos.ContextElement;
import com.katixo.studio.copilot.agent.AgentDtos.EditorContext;
import com.katixo.studio.copilot.agent.AgentDtos.PendingAction;
import com.katixo.studio.copilot.agent.AgentDtos.ToolStep;
import com.katixo.studio.copilot.agent.AssistantTurn.ToolCall;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The Copilot agent loop: assemble context → call the LLM with the tool registry
 * → execute returned tool calls → feed results back → repeat until the model
 * answers without a tool call or the step budget is spent. This is the universal
 * agentic pattern (Cursor/Cline/OpenClaw all share it); ours is constrained to a
 * fixed, mostly-local toolset (see {@code docs/agentic-copilot-research.md}).
 *
 * <p>Heavy GPU work is never run inline: tools submit jobs and return a {@code
 * jobId}, so the loop stays responsive and a single turn can't tie up the GPU.
 */
@Service
public class AgentService {

    /** Cap on LLM round-trips per turn — Cursor-style checkpoint against runaway loops. */
    private static final int MAX_STEPS = 6;

    private static final String SYSTEM_PROMPT = """
            You are Katixo Copilot, the built-in agent of Katixo Studio — a local, \
            GPU-powered design and media studio. You can take actions by calling \
            the provided tools, not just chat.

            Guidelines:
            - Prefer doing the work with tools over describing how. If the user \
            asks to generate, edit, upscale, or find leads, call the matching tool.
            - Generation and editing run as background jobs: a tool returns a jobId \
            and the studio tracks progress. After calling such a tool, tell the \
            user it has started — do not claim the result is ready.
            - Read-only tools (list/get projects) are for gathering context before \
            you act; use them when you need details you don't have.
            - Some tools require the user's confirmation (e.g. scraping external \
            sites). When you call one, it is proposed to the user rather than run; \
            tell them it's waiting for their approval.
            - When the user refers to "this", "the selected image", or "it" \
            without giving an id, use the assetId from the studio context below. \
            Never invent ids — only use ids that appear in the context.
            - Pass only parameters the user actually specified; rely on sensible \
            defaults otherwise. Be concise and practical.""";

    private final OllamaClient ollama;
    private final ToolRegistry registry;
    private final KatixoProperties properties;
    private final ObjectMapper mapper;

    public AgentService(OllamaClient ollama, ToolRegistry registry,
                        KatixoProperties properties, ObjectMapper mapper) {
        this.ollama = ollama;
        this.registry = registry;
        this.properties = properties;
        this.mapper = mapper;
    }

    public AgentResponse run(AgentRequest request) throws IOException, InterruptedException {
        String model = resolveModel(request.model());
        ArrayNode messages = buildMessages(request.messages(), request.context());
        ArrayNode tools = registry.specs();

        List<ToolStep> steps = new ArrayList<>();
        List<PendingAction> pending = new ArrayList<>();

        for (int step = 0; step < MAX_STEPS; step++) {
            AssistantTurn turn = ollama.chatWithTools(model, messages, tools);
            if (!turn.hasToolCalls()) {
                return new AgentResponse(model, ChatMessage.assistant(turn.content()), steps, pending);
            }

            // Re-send the assistant message verbatim so tool_calls round-trip.
            messages.add(turn.rawMessage());

            for (ToolCall call : turn.toolCalls()) {
                CopilotTool tool = registry.find(call.name()).orElse(null);
                if (tool == null) {
                    String msg = "Unknown tool '" + call.name() + "'. Ignore it.";
                    steps.add(new ToolStep(call.name(), call.arguments(), "failed", msg, null));
                    appendToolMessage(messages, msg);
                    continue;
                }
                if (tool.requiresApproval()) {
                    String label = tool.approvalLabel(call.arguments());
                    pending.add(new PendingAction(tool.name(), call.arguments(), label));
                    steps.add(new ToolStep(tool.name(), call.arguments(),
                            "pending_approval", label, null));
                    appendToolMessage(messages, "Proposed to the user for confirmation. "
                            + "Await their approval; do not call this tool again.");
                    continue;
                }
                steps.add(execute(tool, call.arguments(), messages));
            }
        }

        // Budget exhausted: return what we have rather than loop forever.
        return new AgentResponse(model,
                ChatMessage.assistant("I've taken the steps above. Let me know how you'd like to proceed."),
                steps, pending);
    }

    /**
     * Executes a user-confirmed action (an approval-gated tool the model
     * proposed). The explicit confirm <em>is</em> the approval, so this runs the
     * tool regardless of {@link CopilotTool#requiresApproval()}.
     */
    public ToolStep confirm(ConfirmRequest request) {
        CopilotTool tool = registry.find(request.tool())
                .orElseThrow(() -> new IllegalArgumentException("Unknown tool: " + request.tool()));
        var args = request.args() == null ? mapper.createObjectNode() : request.args();
        ToolResult result = tool.execute(args);
        return new ToolStep(tool.name(), args, "done", result.summary(), result.jobId());
    }

    private ToolStep execute(CopilotTool tool, com.fasterxml.jackson.databind.JsonNode args,
                             ArrayNode messages) {
        try {
            ToolResult result = tool.execute(args);
            appendToolMessage(messages, result.summary());
            return new ToolStep(tool.name(), args, "done", result.summary(), result.jobId());
        } catch (RuntimeException e) {
            String msg = "Tool failed: " + e.getMessage();
            appendToolMessage(messages, msg);
            return new ToolStep(tool.name(), args, "failed", msg, null);
        }
    }

    private void appendToolMessage(ArrayNode messages, String content) {
        ObjectNode node = messages.addObject();
        node.put("role", "tool");
        node.put("content", content);
    }

    private ArrayNode buildMessages(List<ChatMessage> incoming, EditorContext context) {
        ArrayNode messages = mapper.createArrayNode();
        boolean hasSystem = incoming.stream().anyMatch(m -> "system".equalsIgnoreCase(m.role()));
        if (!hasSystem) {
            String content = SYSTEM_PROMPT;
            String contextBlock = contextBlock(context);
            if (contextBlock != null) {
                content = content + "\n\n" + contextBlock;
            }
            ObjectNode sys = messages.addObject();
            sys.put("role", "system");
            sys.put("content", content);
        }
        for (ChatMessage m : incoming) {
            ObjectNode node = messages.addObject();
            node.put("role", m.role());
            node.put("content", m.content());
        }
        return messages;
    }

    /** Renders the editor snapshot as a compact prompt block, or null if empty. */
    private String contextBlock(EditorContext c) {
        if (c == null) {
            return null;
        }
        boolean hasElements = c.elements() != null && !c.elements().isEmpty();
        if (c.projectName() == null && c.selected() == null && !hasElements) {
            return null;
        }
        StringBuilder sb = new StringBuilder("Current studio context (only use ids that appear here):");
        if (c.projectName() != null) {
            sb.append("\n- Project: \"").append(c.projectName()).append('"');
            if (c.canvasWidth() != null && c.canvasHeight() != null) {
                sb.append(" (canvas ").append(c.canvasWidth()).append('x')
                        .append(c.canvasHeight()).append(')');
            }
        }
        if (c.selected() != null) {
            sb.append("\n- Selected element: ").append(describe(c.selected()))
                    .append(" — this is what \"this\"/\"the selected\" refers to.");
        }
        if (hasElements) {
            sb.append("\n- Canvas elements:");
            for (ContextElement e : c.elements()) {
                sb.append("\n  • ").append(describe(e));
            }
        }
        return sb.toString();
    }

    private String describe(ContextElement e) {
        StringBuilder sb = new StringBuilder(e.type() == null ? "element" : e.type());
        if (e.assetId() != null) {
            sb.append(" (assetId ").append(e.assetId()).append(')');
        } else if (e.id() != null) {
            sb.append(" (id ").append(e.id()).append(')');
        }
        return sb.toString();
    }

    private String resolveModel(String requested) {
        return (requested == null || requested.isBlank())
                ? properties.copilotModel()
                : requested;
    }
}
