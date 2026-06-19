package com.katixo.studio.copilot.agent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.katixo.studio.copilot.agent.CopilotTool;
import com.katixo.studio.copilot.agent.ToolResult;
import com.katixo.studio.copilot.agent.ToolSchema;
import com.katixo.studio.edit.EditRequests.RemoveBgRequest;
import com.katixo.studio.edit.EditService;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Removes the background from an existing image asset (async job). */
@Component
public class RemoveBgTool implements CopilotTool {

    private final EditService editService;
    private final ObjectMapper mapper;

    public RemoveBgTool(EditService editService, ObjectMapper mapper) {
        this.editService = editService;
        this.mapper = mapper;
    }

    @Override
    public String name() {
        return "remove_background";
    }

    @Override
    public String description() {
        return "Remove the background from an existing image asset, by its assetId. "
                + "Runs as a background job and returns a jobId.";
    }

    @Override
    public JsonNode parameters() {
        return ToolSchema.object(mapper)
                .prop("assetId", "string", "The UUID of the image asset to process.")
                .require("assetId")
                .build();
    }

    @Override
    public ToolResult execute(JsonNode args) {
        UUID assetId = Args.requireUuid(args, "assetId");
        UUID jobId = editService.submitRemoveBg(new RemoveBgRequest(assetId));
        return ToolResult.job("Started background removal for asset " + assetId + ".", jobId);
    }
}
