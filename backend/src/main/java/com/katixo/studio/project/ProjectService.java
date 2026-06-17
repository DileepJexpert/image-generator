package com.katixo.studio.project;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.katixo.studio.project.ProjectDtos.CreateProjectRequest;
import com.katixo.studio.project.ProjectDtos.ProjectDetail;
import com.katixo.studio.project.ProjectDtos.ProjectSummary;
import com.katixo.studio.project.ProjectDtos.UpdateProjectRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProjectService {

    private final ProjectRepository repository;
    private final ObjectMapper objectMapper;

    public ProjectService(ProjectRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<ProjectSummary> list() {
        return repository.findAllByOrderByUpdatedAtDesc().stream()
                .map(ProjectSummary::from)
                .toList();
    }

    @Transactional
    public ProjectDetail create(CreateProjectRequest request) {
        Project project = new Project(
                UUID.randomUUID(),
                request.name(),
                request.canvasWidth(),
                request.canvasHeight(),
                null);
        return toDetail(repository.saveAndFlush(project));
    }

    @Transactional(readOnly = true)
    public Optional<ProjectDetail> get(UUID id) {
        return repository.findById(id).map(this::toDetail);
    }

    @Transactional
    public Optional<ProjectDetail> update(UUID id, UpdateProjectRequest request) {
        return repository.findById(id).map(project -> {
            if (request.name() != null && !request.name().isBlank()) {
                project.setName(request.name());
            }
            if (request.sceneJson() != null) {
                project.setSceneJson(toJson(request.sceneJson()));
            }
            return toDetail(repository.saveAndFlush(project));
        });
    }

    @Transactional
    public boolean delete(UUID id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }

    private ProjectDetail toDetail(Project p) {
        return new ProjectDetail(
                p.getId(),
                p.getName(),
                p.getCanvasWidth(),
                p.getCanvasHeight(),
                parse(p.getSceneJson()),
                p.getCreatedAt(),
                p.getUpdatedAt());
    }

    private JsonNode parse(String json) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Corrupt scene_json for project", e);
        }
    }

    private String toJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid sceneJson", e);
        }
    }
}
