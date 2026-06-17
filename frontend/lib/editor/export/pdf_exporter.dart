import 'dart:typed_data';
import 'dart:ui' as ui;

import 'package:flutter/painting.dart';
import 'package:pdf/pdf.dart';
import 'package:pdf/widgets.dart' as pw;

import '../model/project.dart';
import 'scene_rasterizer.dart';

/// Builds a PDF where each [ScenePage] becomes a page sized to the canvas, with
/// the rasterized design placed full-bleed (CLAUDE.md §7: place the rendered
/// image via the pdf package).
Future<Uint8List> exportProjectPdf({
  required Project project,
  required Map<String, ui.Image> images,
  double pixelRatio = 3,
}) async {
  final doc = pw.Document();
  final canvasSize =
      Size(project.canvasWidth.toDouble(), project.canvasHeight.toDouble());

  for (final page in project.pages) {
    final png = await rasterizePagePng(
      elements: page.elements,
      canvasSize: canvasSize,
      images: images,
      pixelRatio: pixelRatio,
    );
    final image = pw.MemoryImage(png);
    final pageFormat =
        PdfPageFormat(canvasSize.width, canvasSize.height, marginAll: 0);
    doc.addPage(
      pw.Page(
        pageFormat: pageFormat,
        build: (context) => pw.FullPage(
          ignoreMargins: true,
          child: pw.Image(image, fit: pw.BoxFit.fill),
        ),
      ),
    );
  }

  return doc.save();
}
