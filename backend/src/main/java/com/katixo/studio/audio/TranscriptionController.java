package com.katixo.studio.audio;

import com.katixo.studio.audio.AudioRequests.TranscribeRequest;
import com.katixo.studio.job.JobIdResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Transcription API (VISION.md Phase 2). Speech-to-text on an existing audio
 * asset; returns a {@code jobId}. The result is a {@code text} asset holding the
 * transcript JSON ({@code text} + timed {@code segments} for captions).
 */
@RestController
@RequestMapping("/api/v1/transcribe")
public class TranscriptionController {

    private final AudioService audioService;

    public TranscriptionController(AudioService audioService) {
        this.audioService = audioService;
    }

    @PostMapping
    public JobIdResponse transcribe(@Valid @RequestBody TranscribeRequest request) {
        return new JobIdResponse(audioService.submitTranscribe(request));
    }
}
