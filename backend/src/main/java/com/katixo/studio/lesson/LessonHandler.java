package com.katixo.studio.lesson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.katixo.studio.asset.Asset;
import com.katixo.studio.asset.AssetService;
import com.katixo.studio.config.KatixoProperties;
import com.katixo.studio.copilot.OllamaClient;
import com.katixo.studio.generation.ComfyImageResult;
import com.katixo.studio.generation.ComfyUiClient;
import com.katixo.studio.generation.ImageGenerationParams;
import com.katixo.studio.job.Job;
import com.katixo.studio.job.JobHandler;
import com.katixo.studio.job.JobService;
import com.katixo.studio.job.JobType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Executes {@link JobType#LESSON}: the full local tutorial pipeline.
 * <ol>
 *   <li>the local LLM writes a grade-appropriate {@link Lesson} (text + quiz);</li>
 *   <li>ComfyUI illustrates each section from its {@code imagePrompt};</li>
 *   <li>the lesson + illustrations are rendered into a printable PDF worksheet.</li>
 * </ol>
 * Each artifact (lesson JSON, per-section PNGs, PDF) is stored as a downloadable asset, and the job's
 * result is a {@link LessonBundle} manifest referencing them all.
 *
 * <p>Illustrations are best-effort: if ComfyUI is unavailable for a section, that picture is skipped
 * and the lesson/PDF are still produced (text-only for that section) rather than failing the job.
 * Both the Ollama and ComfyUI calls serialize through the shared GPU guard inside their clients, so
 * this handler does not guard again.
 */
@Component
public class LessonHandler implements JobHandler {

    private static final Logger log = LoggerFactory.getLogger(LessonHandler.class);

    private static final int ILLUSTRATION_W = 768;
    private static final int ILLUSTRATION_H = 512;
    private static final String ILLUSTRATION_CHECKPOINT = "v1-5-pruned-emaonly.safetensors";
    private static final String ILLUSTRATION_STYLE =
            "child-friendly cartoon clip-art illustration, bright colours, simple, clean, ";
    private static final String ILLUSTRATION_NEGATIVE =
            "scary, violent, gore, blood, weapon, nsfw, nudity, text, watermark, low quality, deformed";

    private final OllamaClient ollamaClient;
    private final ComfyUiClient comfyUiClient;
    private final LessonParser lessonParser;
    private final LessonPdfRenderer pdfRenderer;
    private final AssetService assetService;
    private final JobService jobService;
    private final ObjectMapper objectMapper;
    private final KatixoProperties properties;
    private final SecureRandom random = new SecureRandom();

    public LessonHandler(OllamaClient ollamaClient, ComfyUiClient comfyUiClient,
                         LessonParser lessonParser, LessonPdfRenderer pdfRenderer,
                         AssetService assetService, JobService jobService,
                         ObjectMapper objectMapper, KatixoProperties properties) {
        this.ollamaClient = ollamaClient;
        this.comfyUiClient = comfyUiClient;
        this.lessonParser = lessonParser;
        this.pdfRenderer = pdfRenderer;
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

        // 1. Lesson text + quiz from the local LLM.
        jobService.updateProgress(job.getId(), 5);
        String raw = ollamaClient.chat(properties.copilotModel(), LessonPrompt.build(request));
        Lesson lesson = lessonParser.parse(raw);
        byte[] lessonJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(lesson);
        Asset lessonAsset = assetService.saveText(lessonJson, "application/json", job.getId());
        jobService.updateProgress(job.getId(), 15);

        // 2. One illustration per section (best-effort), aligned to the section order for the PDF.
        List<Lesson.Section> sections = lesson.sections();
        List<byte[]> sectionImages = new ArrayList<>();
        List<UUID> imageAssetIds = new ArrayList<>();
        for (int i = 0; i < sections.size(); i++) {
            byte[] png = illustrate(lesson, sections.get(i));
            sectionImages.add(png);
            if (png != null) {
                Asset img = assetService.saveImage(png, "image/png", job.getId(), ILLUSTRATION_W, ILLUSTRATION_H);
                imageAssetIds.add(img.getId());
            }
            jobService.updateProgress(job.getId(), 15 + (int) Math.round((i + 1) / (double) sections.size() * 60));
        }

        // 3. Compose the printable PDF worksheet.
        jobService.updateProgress(job.getId(), 80);
        byte[] pdf = pdfRenderer.render(lesson, sectionImages);
        Asset pdfAsset = assetService.saveDocument(pdf, "application/pdf", job.getId());

        // 4. Manifest tying the artifacts together becomes the job result.
        LessonBundle bundle = new LessonBundle(lesson.title(), lesson.grade(),
                lessonAsset.getId(), imageAssetIds, pdfAsset.getId());
        byte[] manifest = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(bundle);
        Asset manifestAsset = assetService.saveText(manifest, "application/json", job.getId());

        jobService.markDone(job.getId(), manifestAsset.getId());
    }

    /** Generate one section illustration, or return {@code null} if ComfyUI couldn't produce it. */
    private byte[] illustrate(Lesson lesson, Lesson.Section section) {
        String base = (section.imagePrompt() != null && !section.imagePrompt().isBlank())
                ? section.imagePrompt()
                : lesson.title() + " - " + section.heading();
        ImageGenerationParams params = new ImageGenerationParams(
                ILLUSTRATION_STYLE + base, ILLUSTRATION_NEGATIVE,
                ILLUSTRATION_W, ILLUSTRATION_H, Math.abs(random.nextLong()), ILLUSTRATION_CHECKPOINT);
        try {
            ComfyImageResult result = comfyUiClient.generateImage(params, progress -> { });
            return result.bytes();
        } catch (Exception e) {
            log.warn("Illustration failed for section '{}' (continuing without it): {}",
                    section.heading(), e.toString());
            return null;
        }
    }
}
