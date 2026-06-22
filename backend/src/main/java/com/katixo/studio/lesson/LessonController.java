package com.katixo.studio.lesson;

import com.katixo.studio.job.JobIdResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lesson generation endpoint. Returns a {@code {jobId}} immediately; poll {@code GET /api/v1/jobs/{id}}
 * (or the job WebSocket) for progress. When done, {@code resultAssetId} points at a
 * {@link LessonBundle} manifest (JSON): download it from {@code GET /api/v1/assets/{id}}, then fetch
 * the referenced lesson JSON, section images, and PDF worksheet the same way.
 */
@RestController
@RequestMapping("/api/v1/generate")
public class LessonController {

    private final LessonService lessonService;

    public LessonController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @PostMapping("/lesson")
    public JobIdResponse generateLesson(@Valid @RequestBody GenerateLessonRequest request) {
        return new JobIdResponse(lessonService.submitLessonJob(request));
    }
}
