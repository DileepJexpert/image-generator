package com.katixo.studio.audio;

import com.katixo.studio.audio.AudioRequests.GenerateSpeechRequest;
import com.katixo.studio.job.JobIdResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Audio generation API (VISION.md Phase 2). Text-to-speech returns a {@code jobId}
 * like the other generators; the result is an {@code audio} asset.
 */
@RestController
@RequestMapping("/api/v1/generate")
public class AudioController {

    private final AudioService audioService;

    public AudioController(AudioService audioService) {
        this.audioService = audioService;
    }

    @PostMapping("/speech")
    public JobIdResponse speech(@Valid @RequestBody GenerateSpeechRequest request) {
        return new JobIdResponse(audioService.submitSpeech(request));
    }
}
