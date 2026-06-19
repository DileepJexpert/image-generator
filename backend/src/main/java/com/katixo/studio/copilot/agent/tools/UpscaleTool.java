package com.katixo.studio.copilot.agent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.katixo.studio.copilot.agent.CopilotTool;
import com.katixo.studio.copilot.agent.ToolResult;
import com.katixo.studio.copilot.agent.ToolSchema;
import com.katixo.studio.edit.EditRequests.UpscaleRequest;
import com.katixo.studio.edit.EditService;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Upscales an existing image asset 2x or 4x via Real-ESRGAN (async job). */
@Component
public class UpscaleTool implements CopilotTool {

    private final EditService editService;
    private final ObjectMapper mapper;

    public UpscaleTool(EditService editService, ObjectMapper mapper) {
        this.editService = editService;
        this.mapper = mapper;
    }

    @Override
    public String name() {
        return "upscale_image";
    }

    @Override
    public String description() {
        return "Upscale an existing image asset by 2x or 4x, by its assetId. "
                + "Runs as a background job and returns a jobId.";
    }

    @Override
    public JsonNode parameters() {
        return ToolSchema.object(mapper)
                .prop("assetId", "string", "The UUID of the image asset to upscale.")
                .enumProp("scale", "Upscale factor: 2 or 4 (default 2).", "2", "4")
                .require("assetId")
                .build();
    }

    @Override
    public ToolResult execute(JsonNode args) {
        UUID assetId = Args.requireUuid(args, "assetId");
        int scale = args.path("scale").asInt(2);
        if (scale != 2 && scale != 4) {
            scale = 2;
        }
        UUID jobId = editService.submitUpscale(new UpscaleRequest(assetId, scale));
        return ToolResult.job("Started " + scale + "x upscale for asset " + assetId + ".", jobId);
    }
}
