package com.katixo.studio.lesson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.katixo.studio.asset.Asset;
import com.katixo.studio.asset.AssetService;
import com.katixo.studio.config.KatixoProperties;
import com.katixo.studio.copilot.OllamaClient;
import com.katixo.studio.job.Job;
import com.katixo.studio.job.JobHandler;
import com.katixo.studio.job.JobService;
import com.katixo.studio.job.JobType;
import org.springframework.stereotype.Component;

/**
 * Executes {@link JobType#LESSON} (milestone A — the lesson spine): ask the local LLM for a
 * grade-appropriate lesson, validate it into a {@link Lesson}, and store it as a downloadable JSON
 * asset. Illustrations, the PDF worksheet, and the narrated video are layered on in later milestones,
 * all rendered from this same JSON.
 *
 * <p>The Ollama call is GPU work but {@link OllamaClient#chat} already serializes it through the
 * shared GPU guard, so this handler does not guard again.
 */
@Component
public class LessonHandler implements JobHandler {

    private final OllamaClient ollamaClient;
    private final LessonParser lessonParser;
    private final AssetService assetService;
    private final JobService jobService;
    private final ObjectMapper objectMapper;
    private final KatixoProperties properties;

    public LessonHandler(OllamaClient ollamaClient, LessonParser lessonParser,
                         AssetService assetService, JobService jobService,
                         ObjectMapper objectMapper, KatixoProperties properties) {
        this.ollamaClient = ollamaClient;
        this.lessonParser = lessonParser;
        this.assetService = assetService;
        this.jobService = jobService;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public JobType type() {
        return JobType.LESSON;
    }

    @Override
    public void handle(Job job) throws Exception {
        GenerateLessonRequest request =
                objectMapper.readValue(job.getParamsJson(), GenerateLessonRequest.class);

        jobService.updateProgress(job.getId(), 10);
        String raw = ollamaClient.chat(properties.copilotModel(), LessonPrompt.build(request));

        jobService.updateProgress(job.getId(), 80);
        Lesson lesson = lessonParser.parse(raw);

        byte[] json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(lesson);
        Asset asset = assetService.saveText(json, "application/json", job.getId());
        jobService.markDone(job.getId(), asset.getId());
    }
}
