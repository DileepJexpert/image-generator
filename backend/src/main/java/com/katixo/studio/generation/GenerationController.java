package com.katixo.studio.generation;

import com.katixo.studio.job.JobIdResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/generate")
public class GenerationController {

    private final GenerationService generationService;

    public GenerationController(GenerationService generationService) {
        this.generationService = generationService;
    }

    @PostMapping("/image")
    public JobIdResponse generateImage(@Valid @RequestBody GenerateImageRequest request) {
        return new JobIdResponse(generationService.submitImageJob(request));
    }

    @PostMapping("/image-to-video")
    public JobIdResponse generateImageToVideo(@Valid @RequestBody ImageToVideoRequest request) {
        return new JobIdResponse(generationService.submitImageToVideoJob(request));
    }
}
