import 'dart:ui' as ui;

import 'package:flutter/material.dart';

import '../../core/hex_color.dart';
import '../model/design_element.dart';

/// Renders the page background and every element through a single CustomPainter,
/// in zIndex order, applying per-element rotation + opacity (CLAUDE.md §7). Used
/// both on-screen and (via SceneRasterizer) for PNG/PDF export of any page.
class ScenePainter extends CustomPainter {
  ScenePainter({
    required this.elements,
    required this.images,
    this.selectedId,
    this.showSelection = true,
  });

  final List<DesignElement> elements;
  final Map<String, ui.Image> images;
  final String? selectedId;
  final bool showSelection;

  List<DesignElement> get _byZ {
    final sorted = [...elements]..sort((a, b) => a.zIndex.compareTo(b.zIndex));
    return sorted;
  }

  @override
  void paint(Canvas canvas, Size size) {
    // Page background.
    canvas.drawRect(
      Offset.zero & size,
      Paint()..color = Colors.white,
    );

    for (final element in _byZ) {
      _paintElement(canvas, element);
    }
  }

  void _paintElement(Canvas canvas, DesignElement e) {
    canvas.save();
    final center = Offset(e.x + e.width / 2, e.y + e.height / 2);
    canvas.translate(center.dx, center.dy);
    canvas.rotate(e.rotation);

    final rect = Rect.fromCenter(
      center: Offset.zero,
      width: e.width,
      height: e.height,
    );

    final needsLayer = e.opacity < 1.0;
    if (needsLayer) {
      canvas.saveLayer(
        rect,
        Paint()..color = Colors.white.withValues(alpha: e.opacity.clamp(0.0, 1.0)),
      );
    }

    e.map(
      text: (t) => _paintText(canvas, t, rect),
      image: (img) => _paintImage(canvas, img, rect),
      shape: (s) => _paintShape(canvas, s, rect),
      video: (v) => _paintVideo(canvas, v, rect),
    );

    if (needsLayer) {
      canvas.restore();
    }

    if (showSelection && e.id == selectedId) {
      canvas.drawRect(
        rect,
        Paint()
          ..style = PaintingStyle.stroke
          ..strokeWidth = 1.5
          ..color = const Color(0xFF6C5CE7),
      );
    }

    canvas.restore();
  }

  void _paintText(Canvas canvas, TextElement t, Rect rect) {
    final painter = TextPainter(
      text: TextSpan(
        text: t.text,
        style: TextStyle(
          fontSize: t.fontSize,
          fontFamily: t.fontFamily,
          color: hexToColor(t.color, fallback: Colors.black),
          fontWeight: FontWeight.values[
              (t.weight ~/ 100 - 1).clamp(0, FontWeight.values.length - 1)],
        ),
      ),
      textAlign: _textAlign(t.align),
      textDirection: TextDirection.ltr,
    );
    painter.layout(maxWidth: rect.width);
    painter.paint(canvas, rect.topLeft);
  }

  void _paintImage(Canvas canvas, ImageElement el, Rect rect) {
    final image = images[el.assetId];
    if (image == null) {
      _paintPlaceholder(canvas, rect, Icons.image_outlined);
      return;
    }
    _drawImage(canvas, image, rect, _boxFit(el.fit));
  }

  void _drawImage(Canvas canvas, ui.Image image, Rect rect, BoxFit fit) {
    final inputSize = Size(image.width.toDouble(), image.height.toDouble());
    final fitted = applyBoxFit(fit, inputSize, rect.size);
    final src = Alignment.center.inscribe(fitted.source, Offset.zero & inputSize);
    final dst = Alignment.center.inscribe(fitted.destination, rect);
    canvas.drawImageRect(image, src, dst, Paint());
  }

  void _paintShape(Canvas canvas, ShapeElement s, Rect rect) {
    final fill = Paint()..color = hexToColor(s.fill, fallback: Colors.grey);
    final stroke = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2
      ..color = hexToColor(s.stroke);

    switch (s.shape) {
      case 'ellipse':
        canvas.drawOval(rect, fill);
        canvas.drawOval(rect, stroke);
        break;
      case 'rrect':
        final rr = RRect.fromRectAndRadius(rect, Radius.circular(s.cornerRadius));
        canvas.drawRRect(rr, fill);
        canvas.drawRRect(rr, stroke);
        break;
      case 'rect':
      default:
        canvas.drawRect(rect, fill);
        canvas.drawRect(rect, stroke);
    }
  }

  void _paintVideo(Canvas canvas, VideoElement v, Rect rect) {
    final image = images[v.assetId];
    if (image != null) {
      _drawImage(canvas, image, rect, BoxFit.contain);
    } else {
      _paintPlaceholder(canvas, rect, Icons.movie_outlined);
    }
    // Play badge overlay.
    final badge = Paint()..color = Colors.black54;
    canvas.drawCircle(rect.center, 18, badge);
    final tri = Path()
      ..moveTo(rect.center.dx - 6, rect.center.dy - 9)
      ..lineTo(rect.center.dx - 6, rect.center.dy + 9)
      ..lineTo(rect.center.dx + 10, rect.center.dy)
      ..close();
    canvas.drawPath(tri, Paint()..color = Colors.white);
  }

  void _paintPlaceholder(Canvas canvas, Rect rect, IconData icon) {
    canvas.drawRect(rect, Paint()..color = const Color(0xFFE9E9F0));
    final tp = TextPainter(
      text: TextSpan(
        text: String.fromCharCode(icon.codePoint),
        style: TextStyle(
          fontSize: rect.shortestSide * 0.4,
          fontFamily: icon.fontFamily,
          package: icon.fontPackage,
          color: const Color(0xFF9A9AB0),
        ),
      ),
      textDirection: TextDirection.ltr,
    )..layout();
    tp.paint(canvas, rect.center - Offset(tp.width / 2, tp.height / 2));
  }

  TextAlign _textAlign(String align) => switch (align) {
        'center' => TextAlign.center,
        'right' => TextAlign.right,
        'justify' => TextAlign.justify,
        _ => TextAlign.left,
      };

  BoxFit _boxFit(String fit) => switch (fit) {
        'cover' => BoxFit.cover,
        'fill' => BoxFit.fill,
        'fitWidth' => BoxFit.fitWidth,
        'fitHeight' => BoxFit.fitHeight,
        _ => BoxFit.contain,
      };

  @override
  bool shouldRepaint(covariant ScenePainter old) =>
      old.elements != elements ||
      old.images != images ||
      old.selectedId != selectedId ||
      old.showSelection != showSelection;
}
