package com.katixo.studio.copilot.agent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.katixo.studio.copilot.agent.CopilotTool;
import com.katixo.studio.copilot.agent.ToolResult;
import com.katixo.studio.copilot.agent.ToolSchema;
import com.katixo.studio.diagram.DiagramService;
import com.katixo.studio.diagram.RenderDiagramRequest;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Renders a system-design diagram from PlantUML source and places it on the canvas (async job, like
 * image generation). Great for microservices/system design (component diagrams), design patterns
 * (class diagrams) and flows (sequence diagrams). The agent's LLM writes the PlantUML; this tool
 * renders it locally for free.
 */
@Component
public class RenderDiagramTool implements CopilotTool {

    private final DiagramService diagramService;
    private final ObjectMapper mapper;

    public RenderDiagramTool(DiagramService diagramService, ObjectMapper mapper) {
        this.diagramService = diagramService;
        this.mapper = mapper;
    }

    @Override
    public String name() {
        return "render_diagram";
    }

    @Override
    public String description() {
        return "Render a software/system-design diagram from PlantUML source and add it to the "
                + "canvas. Use it for system design & microservices (component diagrams), design "
                + "patterns (class diagrams), and flows (sequence diagrams). Put complete PlantUML "
                + "in 'source' (e.g. @startuml ... @enduml). Runs as a job and returns a jobId.";
    }

    @Override
    public JsonNode parameters() {
        return ToolSchema.object(mapper)
                .prop("source", "string", "Complete PlantUML diagram source (@startuml ... @enduml).")
                .require("source")
                .build();
    }

    @Override
    public ToolResult execute(JsonNode args) {
        String source = Args.requireText(args, "source");
        UUID jobId = diagramService.submitDiagramJob(new RenderDiagramRequest(source));
        return ToolResult.job("Rendering diagram and adding it to the canvas.", jobId);
    }
}
