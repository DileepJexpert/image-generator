package com.katixo.studio.copilot.agent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.katixo.studio.copilot.agent.CopilotTool;
import com.katixo.studio.copilot.agent.ToolResult;
import com.katixo.studio.copilot.agent.ToolSchema;
import com.katixo.studio.project.ProjectDtos.ProjectSummary;
import com.katixo.studio.project.ProjectService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/** Read-only: lists the user's projects so the agent can reference them. */
@Component
public class ListProjectsTool implements CopilotTool {

    private final ProjectService projectService;
    private final ObjectMapper mapper;

    public ListProjectsTool(ProjectService projectService, ObjectMapper mapper) {
        this.projectService = projectService;
        this.mapper = mapper;
    }

    @Override
    public String name() {
        return "list_projects";
    }

    @Override
    public String description() {
        return "List the user's design projects (id, name, canvas size). "
                + "Read-only; use it to find a project to act on.";
    }

    @Override
    public JsonNode parameters() {
        return ToolSchema.object(mapper).build();
    }

    @Override
    public ToolResult execute(JsonNode args) {
        List<ProjectSummary> projects = projectService.list();
        if (projects.isEmpty()) {
            return ToolResult.text("No projects yet.");
        }
        String list = projects.stream()
                .map(p -> "- " + p.name() + " (" + p.canvasWidth() + "x" + p.canvasHeight()
                        + ", id " + p.id() + ")")
                .collect(Collectors.joining("\n"));
        return ToolResult.text(projects.size() + " project(s):\n" + list);
    }
}
