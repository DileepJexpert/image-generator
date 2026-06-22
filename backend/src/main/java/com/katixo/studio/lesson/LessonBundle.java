package com.katixo.studio.lesson;

import java.util.List;
import java.util.UUID;

/**
 * Manifest describing every artifact a lesson job produced. It is stored as the job's result asset
 * (JSON); a client polls the job, downloads this manifest from {@code GET /api/v1/assets/{id}}, then
 * fetches each referenced asset the same way.
 *
 * @param lessonAssetId structured lesson JSON — the "text + quiz" format
 * @param imageAssetIds per-section illustration PNGs (best-effort; may be fewer than the section
 *                      count if ComfyUI was unavailable for some sections)
 * @param pdfAssetId    the illustrated PDF worksheet
 * @param videoAssetId  the narrated slideshow MP4, or {@code null} if the TTS/ffmpeg step was
 *                      unavailable (the rest of the bundle is still produced)
 */
public record LessonBundle(
        String title,
        int grade,
        UUID lessonAssetId,
        List<UUID> imageAssetIds,
        UUID pdfAssetId,
        UUID videoAssetId
) {
}
