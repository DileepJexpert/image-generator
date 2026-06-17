import 'dart:typed_data';
import 'dart:ui' as ui;

import 'package:flutter/painting.dart';

import '../canvas/scene_painter.dart';
import '../model/design_element.dart';

/// Renders a page's elements to PNG bytes off-screen via a [ui.PictureRecorder]
/// and the shared [ScenePainter]. This makes export deterministic for *any*
/// page (not just the mounted one), which is what multi-page PDF export needs.
Future<Uint8List> rasterizePagePng({
  required List<DesignElement> elements,
  required Size canvasSize,
  required Map<String, ui.Image> images,
  double pixelRatio = 3,
}) async {
  final recorder = ui.PictureRecorder();
  final canvas = Canvas(recorder);
  canvas.scale(pixelRatio);

  ScenePainter(elements: elements, images: images, showSelection: false)
      .paint(canvas, canvasSize);

  final picture = recorder.endRecording();
  final image = await picture.toImage(
    (canvasSize.width * pixelRatio).ceil(),
    (canvasSize.height * pixelRatio).ceil(),
  );
  try {
    final data = await image.toByteData(format: ui.ImageByteFormat.png);
    if (data == null) {
      throw StateError('Failed to encode page to PNG');
    }
    return data.buffer.asUint8List();
  } finally {
    image.dispose();
    picture.dispose();
  }
}
