package com.katixo.studio.copilot.agent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.katixo.studio.copilot.agent.CopilotTool;
import com.katixo.studio.copilot.agent.ToolResult;
import com.katixo.studio.copilot.agent.ToolSchema;
import com.katixo.studio.project.ProjectDtos.ProjectDetail;
import com.katixo.studio.project.ProjectService;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Read-only: fetches one project's details (canvas + element summary). */
@Component
public class GetProjectTool implements CopilotTool {

    private final ProjectService projectService;
    private final ObjectMapper mapper;

    public GetProjectTool(ProjectService projectService, ObjectMapper mapper) {
        this.projectService = projectService;
        this.mapper = mapper;
    }

    @Override
    public String name() {
        return "get_project";
    }

    @Override
    public String description() {
        return "Get one project's details by its id: name, canvas size, and how "
                + "many elements are on the canvas. Read-only.";
    }

    @Override
    public JsonNode parameters() {
        return ToolSchema.object(mapper)
                .prop("id", "string", "The UUID of the project.")
                .require("id")
                .build();
    }

    @Override
    public ToolResult execute(JsonNode args) {
        UUID id = Args.requireUuid(args, "id");
        ProjectDetail p = projectService.get(id)
                .orElseThrow(() -> new IllegalArgumentException("No project with id " + id));
        int elements = countElements(p.sceneJson());
        return ToolResult.text("Project \"" + p.name() + "\": canvas "
                + p.canvasWidth() + "x" + p.canvasHeight() + ", " + elements + " element(s).");
    }

    private int countElements(JsonNode sceneJson) {
        if (sceneJson == null) {
            return 0;
        }
        JsonNode elements = sceneJson.path("elements");
        return elements.isArray() ? elements.size() : 0;
    }
}
