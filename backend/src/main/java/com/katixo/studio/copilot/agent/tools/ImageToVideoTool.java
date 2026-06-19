package com.katixo.studio.copilot.agent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.katixo.studio.copilot.agent.CopilotTool;
import com.katixo.studio.copilot.agent.ToolResult;
import com.katixo.studio.copilot.agent.ToolSchema;
import com.katixo.studio.generation.GenerationService;
import com.katixo.studio.generation.ImageToVideoRequest;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Animates an existing image asset into a short clip (async job). */
@Component
public class ImageToVideoTool implements CopilotTool {

    private static final int DEFAULT_DURATION = 5;

    private final GenerationService generationService;
    private final ObjectMapper mapper;

    public ImageToVideoTool(GenerationService generationService, ObjectMapper mapper) {
        this.generationService = generationService;
        this.mapper = mapper;
    }

    @Override
    public String name() {
        return "image_to_video";
    }

    @Override
    public String description() {
        return "Animate an existing image asset into a short video clip, by its "
                + "assetId. Runs as a background job and returns a jobId.";
    }

    @Override
    public JsonNode parameters() {
        return ToolSchema.object(mapper)
                .prop("sourceAssetId", "string", "The UUID of the source image asset.")
                .prop("prompt", "string", "Optional motion/scene guidance.")
                .prop("durationSeconds", "integer", "Clip length 1-10 seconds (default 5).")
                .require("sourceAssetId")
                .build();
    }

    @Override
    public ToolResult execute(JsonNode args) {
        UUID source = Args.requireUuid(args, "sourceAssetId");
        String prompt = Args.text(args, "prompt", null);
        int duration = Args.clampedInt(args, "durationSeconds", DEFAULT_DURATION, 1, 10);

        UUID jobId = generationService.submitImageToVideoJob(
                new ImageToVideoRequest(source, prompt, duration));
        return ToolResult.job("Started a " + duration + "s video from asset " + source + ".", jobId);
    }
}
