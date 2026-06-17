import 'dart:typed_data';
import 'dart:ui' as ui;

import 'package:flutter/rendering.dart';
import 'package:flutter/widgets.dart';

/// Captures the canvas [RepaintBoundary] to PNG bytes at [pixelRatio] for crisp
/// output (CLAUDE.md §7 export).
Future<Uint8List> capturePng(GlobalKey boundaryKey,
    {double pixelRatio = 3}) async {
  final boundary =
      boundaryKey.currentContext!.findRenderObject() as RenderRepaintBoundary;
  final ui.Image image = await boundary.toImage(pixelRatio: pixelRatio);
  final data = await image.toByteData(format: ui.ImageByteFormat.png);
  if (data == null) {
    throw StateError('Failed to encode canvas to PNG');
  }
  return data.buffer.asUint8List();
}
