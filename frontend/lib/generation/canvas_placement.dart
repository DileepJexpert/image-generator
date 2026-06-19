import 'dart:math' as math;

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../core/id.dart';
import '../editor/canvas/image_cache.dart';
import '../editor/model/design_element.dart';
import '../editor/state/editor_controller.dart';

/// Places a finished job's result asset onto the current editor page, so the
/// agent's actions show up on the canvas (the same end state the image/edit
/// panels reach). Returns a short status for the user, or null when the job
/// type has no visual result to place (leads, speech, transcription).
///
/// Sizing mirrors the image panel: fit the asset within ~60% of the canvas,
/// centered. Image dimensions come from decoding the asset (which also seeds
/// the canvas image cache for rendering); video has no decodable frame here, so
/// it falls back to a canvas-relative square.
Future<String?> placeJobResultOnCanvas(
  WidgetRef ref, {
  required String assetId,
  required String jobType,
}) async {
  final editor = ref.read(editorControllerProvider);
  final project = editor.project;
  final page = editor.currentPage;
  if (project == null || page == null) {
    return 'Result is ready, but no project is open — open one to place it.';
  }
  final notifier = ref.read(editorControllerProvider.notifier);

  switch (jobType) {
    case 'image':
    case 'remove_bg':
    case 'upscale':
      final image =
          (await ref.read(imageCacheProvider.notifier).loadAll([assetId]))[assetId];
      final srcW = (image?.width ?? 1024).toDouble();
      final srcH = (image?.height ?? 1024).toDouble();
      final maxW = project.canvasWidth * 0.6;
      final maxH = project.canvasHeight * 0.6;
      final scale = math.min(1.0, math.min(maxW / srcW, maxH / srcH));
      final w = srcW * scale;
      final h = srcH * scale;
      notifier.addElement(DesignElement.image(
        id: newId(),
        x: (project.canvasWidth - w) / 2,
        y: (project.canvasHeight - h) / 2,
        width: w,
        height: h,
        assetId: assetId,
        zIndex: page.nextZIndex,
      ));
      return 'Image added to canvas.';

    case 'image_to_video':
      final side = math.min(project.canvasWidth, project.canvasHeight) * 0.6;
      notifier.addElement(DesignElement.video(
        id: newId(),
        x: (project.canvasWidth - side) / 2,
        y: (project.canvasHeight - side) / 2,
        width: side,
        height: side,
        assetId: assetId,
        zIndex: page.nextZIndex,
      ));
      return 'Video added to canvas.';

    default:
      return null; // lead_scrape / text_to_speech / transcribe: nothing to place
  }
}
