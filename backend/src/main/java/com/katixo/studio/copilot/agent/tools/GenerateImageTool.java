package com.katixo.studio.copilot.agent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.katixo.studio.copilot.agent.CopilotTool;
import com.katixo.studio.copilot.agent.ToolResult;
import com.katixo.studio.copilot.agent.ToolSchema;
import com.katixo.studio.generation.GenerateImageRequest;
import com.katixo.studio.generation.GenerationService;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Generates an image via ComfyUI as an async job. */
@Component
public class GenerateImageTool implements CopilotTool {

    /** Default SDXL checkpoint (matches the image panel's "SDXL Base" option). */
    private static final String DEFAULT_MODEL = "sd_xl_base_1.0.safetensors";
    private static final int DEFAULT_SIZE = 1024;

    private final GenerationService generationService;
    private final ObjectMapper mapper;

    public GenerateImageTool(GenerationService generationService, ObjectMapper mapper) {
        this.generationService = generationService;
        this.mapper = mapper;
    }

    @Override
    public String name() {
        return "generate_image";
    }

    @Override
    public String description() {
        return "Generate an image from a text prompt using the local image model. "
                + "Runs as a background job and returns a jobId.";
    }

    @Override
    public JsonNode parameters() {
        return ToolSchema.object(mapper)
                .prop("prompt", "string", "Vivid description of the image to generate.")
                .prop("negativePrompt", "string", "What to avoid in the image (optional).")
                .prop("width", "integer", "Pixel width, 64-2048 (default 1024).")
                .prop("height", "integer", "Pixel height, 64-2048 (default 1024).")
                .require("prompt")
                .build();
    }

    @Override
    public ToolResult execute(JsonNode args) {
        String prompt = Args.requireText(args, "prompt");
        String negative = Args.text(args, "negativePrompt", null);
        int width = Args.clampedInt(args, "width", DEFAULT_SIZE, 64, 2048);
        int height = Args.clampedInt(args, "height", DEFAULT_SIZE, 64, 2048);

        UUID jobId = generationService.submitImageJob(new GenerateImageRequest(
                prompt, negative, width, height, DEFAULT_MODEL, null));
        return ToolResult.job("Started image generation (" + width + "x" + height
                + ") for: \"" + prompt + "\".", jobId);
    }
}
