package com.katixo.studio.project;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.UUID;

/** Request/response DTOs for the project API (CLAUDE.md section 6). */
public final class ProjectDtos {

    private ProjectDtos() {
    }

    /** {@code POST /projects} body. */
    public record CreateProjectRequest(
            @NotBlank String name,
            @Positive @Max(8192) int canvasWidth,
            @Positive @Max(8192) int canvasHeight
    ) {
    }

    /** {@code PUT /projects/{id}} body (autosave target). Both fields optional. */
    public record UpdateProjectRequest(
            String name,
            JsonNode sceneJson
    ) {
    }

    /** List item — no scene payload. */
    public record ProjectSummary(
            UUID id,
            String name,
            int canvasWidth,
            int canvasHeight,
            Instant updatedAt
    ) {
        static ProjectSummary from(Project p) {
            return new ProjectSummary(p.getId(), p.getName(), p.getCanvasWidth(),
                    p.getCanvasHeight(), p.getUpdatedAt());
        }
    }

    /** Full project including the parsed scene. */
    public record ProjectDetail(
            UUID id,
            String name,
            int canvasWidth,
            int canvasHeight,
            JsonNode sceneJson,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
