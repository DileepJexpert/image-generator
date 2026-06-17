import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../state/editor_controller.dart';
import '../tools/transform_handles.dart';
import 'element_geometry.dart';
import 'image_cache.dart';
import 'scene_painter.dart';

/// The interactive design surface: a fixed-size canvas (wrapped in a
/// [RepaintBoundary] for PNG export) inside an [InteractiveViewer] for pan/zoom.
/// Tap selects; dragging the body moves; corner/rotation handles transform.
class EditorCanvas extends ConsumerWidget {
  const EditorCanvas({super.key, required this.exportKey});

  final GlobalKey exportKey;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final editor = ref.watch(editorControllerProvider);
    final controller = ref.read(editorControllerProvider.notifier);
    final images = ref.watch(imageCacheProvider);
    final project = editor.project;

    if (project == null) {
      return const Center(child: CircularProgressIndicator());
    }

    // Ensure decoded images exist for every image/video element.
    final cache = ref.read(imageCacheProvider.notifier);
    for (final e in project.elements) {
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
            key: exportKey,
            child: SizedBox(
              width: canvasSize.width,
              height: canvasSize.height,
              child: Stack(
                clipBehavior: Clip.none,
                children: [
                  // 1) Rendered scene.
                  Positioned.fill(
                    child: CustomPaint(
                      painter: ScenePainter(
                        project: project,
                        images: images,
                        selectedId: editor.selectedId,
                      ),
                    ),
                  ),

                  // 2) Selection + move gestures (below the handles).
                  Positioned.fill(
                    child: GestureDetector(
                      behavior: HitTestBehavior.opaque,
                      onTapDown: (d) =>
                          controller.select(hitTest(project.elements, d.localPosition)),
                      onPanUpdate: (d) {
                        final id = editor.selectedId;
                        if (id != null) {
                          controller.updateElement(
                              id, (e) => e.locked ? e : applyMove(e, d.delta));
                        }
                      },
                    ),
                  ),

                  // 3) Transform handles for the selected element (on top).
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
