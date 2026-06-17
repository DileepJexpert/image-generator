import 'dart:math' as math;
import 'dart:ui';

import '../model/design_element.dart';

/// Which resize handle is being dragged; `sx`/`sy` are the local-axis signs of
/// that corner (used to keep the opposite corner anchored during resize).
enum HandleCorner {
  topLeft(-1, -1),
  topRight(1, -1),
  bottomLeft(-1, 1),
  bottomRight(1, 1);

  const HandleCorner(this.sx, this.sy);
  final double sx;
  final double sy;
}

const double kMinElementSize = 16;

Offset elementCenter(DesignElement e) =>
    Offset(e.x + e.width / 2, e.y + e.height / 2);

/// Rotate an offset around the origin by [angle] radians.
Offset rotateOffset(Offset o, double angle) {
  final c = math.cos(angle);
  final s = math.sin(angle);
  return Offset(o.dx * c - o.dy * s, o.dx * s + o.dy * c);
}

/// Whether a canvas-space point falls inside a (possibly rotated) element.
bool pointInElement(DesignElement e, Offset point) {
  final local = rotateOffset(point - elementCenter(e), -e.rotation);
  return local.dx.abs() <= e.width / 2 && local.dy.abs() <= e.height / 2;
}

/// Topmost element (highest zIndex) under [point], or null.
String? hitTest(List<DesignElement> elements, Offset point) {
  final byZTopFirst = [...elements]..sort((a, b) => b.zIndex.compareTo(a.zIndex));
  for (final e in byZTopFirst) {
    if (!e.locked && pointInElement(e, point)) {
      return e.id;
    }
  }
  return null;
}

/// Canvas-space corner positions of an element in painting order: TL, TR, BR, BL.
List<Offset> elementCorners(DesignElement e) {
  final center = elementCenter(e);
  final hw = e.width / 2;
  final hh = e.height / 2;
  return [
    center + rotateOffset(Offset(-hw, -hh), e.rotation),
    center + rotateOffset(Offset(hw, -hh), e.rotation),
    center + rotateOffset(Offset(hw, hh), e.rotation),
    center + rotateOffset(Offset(-hw, hh), e.rotation),
  ];
}

/// Position of the rotation handle (above the top edge) in canvas space.
Offset rotationHandlePosition(DesignElement e, {double offset = 28}) {
  return elementCenter(e) +
      rotateOffset(Offset(0, -e.height / 2 - offset), e.rotation);
}

DesignElement applyMove(DesignElement e, Offset canvasDelta) =>
    e.copyWith(x: e.x + canvasDelta.dx, y: e.y + canvasDelta.dy);

/// Resize by dragging [corner] with a canvas-space [delta], keeping the
/// opposite corner fixed (works under rotation).
DesignElement applyResize(DesignElement e, HandleCorner corner, Offset delta) {
  final local = rotateOffset(delta, -e.rotation);
  final newW = math.max(kMinElementSize, e.width + corner.sx * local.dx);
  final newH = math.max(kMinElementSize, e.height + corner.sy * local.dy);
  final localCenterShift =
      Offset(corner.sx * (newW - e.width) / 2, corner.sy * (newH - e.height) / 2);
  final center = elementCenter(e) + rotateOffset(localCenterShift, e.rotation);
  return e.copyWith(
    width: newW,
    height: newH,
    x: center.dx - newW / 2,
    y: center.dy - newH / 2,
  );
}

/// Incremental rotation from dragging the rotation handle. [handleVector] is the
/// current vector from the element center to the handle; [delta] is the drag.
DesignElement applyRotate(DesignElement e, Offset handleVector, Offset delta) {
  final len2 = handleVector.dx * handleVector.dx + handleVector.dy * handleVector.dy;
  if (len2 == 0) {
    return e;
  }
  final dTheta =
      (handleVector.dx * delta.dy - handleVector.dy * delta.dx) / len2;
  return e.copyWith(rotation: e.rotation + dTheta);
}
