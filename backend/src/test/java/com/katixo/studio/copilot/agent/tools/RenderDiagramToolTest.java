package com.katixo.studio.copilot.agent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.katixo.studio.copilot.agent.ToolResult;
import com.katixo.studio.diagram.DiagramService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RenderDiagramToolTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void submitsDiagramJobAndReturnsJobId() {
        DiagramService service = mock(DiagramService.class);
        UUID jobId = UUID.randomUUID();
        when(service.submitDiagramJob(any())).thenReturn(jobId);
        RenderDiagramTool tool = new RenderDiagramTool(service, mapper);

        ToolResult result = tool.execute(
                mapper.createObjectNode().put("source", "@startuml\nA -> B\n@enduml"));

        assertThat(result.jobId()).isEqualTo(jobId);
        assertThat(result.summary()).contains("canvas");
    }

    @Test
    void exposesSourceAsRequiredAndNeedsNoApproval() {
        RenderDiagramTool tool = new RenderDiagramTool(mock(DiagramService.class), mapper);
        assertThat(tool.name()).isEqualTo("render_diagram");
        assertThat(tool.requiresApproval()).isFalse();
        assertThat(tool.parameters().path("required").toString()).contains("source");
    }
}
