import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../state/editor_controller.dart';
import '../tools/transform_handles.dart';
import 'element_geometry.dart';
import 'image_cache.dart';
import 'scene_painter.dart';

/// The interactive design surface: a fixed-size canvas inside an
/// [InteractiveViewer] for pan/zoom. Tap selects; dragging the body moves;
/// corner/rotation handles transform. Renders the active page.
class EditorCanvas extends ConsumerWidget {
  const EditorCanvas({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final editor = ref.watch(editorControllerProvider);
    final controller = ref.read(editorControllerProvider.notifier);
    final images = ref.watch(imageCacheProvider);
    final project = editor.project;
    final page = editor.currentPage;

    if (project == null || page == null) {
      return const Center(child: CircularProgressIndicator());
    }

    // Ensure decoded images exist for every image/video element on this page.
    final cache = ref.read(imageCacheProvider.notifier);
    for (final e in page.elements) {
      final assetId = e.assetIdOrNull;
      if (assetId != null) {
        cache.ensure(assetId);
      }
    }

    final canvasSize =
        Size(project.canvasWidth.toDouble(), project.canvasHeight.toDouble());
    final selected = editor.selected;

    return ColoredBox(
      color: const Color(0xFF26252B),
      child: InteractiveViewer(
        constrained: false,
        boundaryMargin: const EdgeInsets.all(600),
        minScale: 0.1,
        maxScale: 6,
        child: Padding(
          padding: const EdgeInsets.all(300),
          child: RepaintBoundary(
            child: SizedBox(
              width: canvasSize.width,
              height: canvasSize.height,
              child: Stack(
                clipBehavior: Clip.none,
                children: [
                  Positioned.fill(
                    child: CustomPaint(
                      painter: ScenePainter(
                        elements: page.elements,
                        images: images,
                        selectedId: editor.selectedId,
                      ),
                    ),
                  ),
                  Positioned.fill(
                    child: GestureDetector(
                      behavior: HitTestBehavior.opaque,
                      onTapDown: (d) =>
                          controller.select(hitTest(page.elements, d.localPosition)),
                      onPanUpdate: (d) {
                        final id = editor.selectedId;
                        if (id != null) {
                          controller.updateElement(
                              id, (e) => e.locked ? e : applyMove(e, d.delta));
                        }
                      },
                    ),
                  ),
                  if (selected != null)
                    Positioned.fill(
                      child: TransformHandles(
                        element: selected,
                        onResize: (corner, delta) => controller.updateElement(
                            selected.id, (e) => applyResize(e, corner, delta)),
                        onRotate: (vector, delta) => controller.updateElement(
                            selected.id, (e) => applyRotate(e, vector, delta)),
                      ),
                    ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
