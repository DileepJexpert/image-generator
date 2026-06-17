import 'package:flutter/material.dart';

import '../canvas/element_geometry.dart';
import '../model/design_element.dart';

/// Overlay drawn over the selected element: four corner handles for resize and
/// one handle above for rotation (CLAUDE.md §7). Implemented as [Positioned]
/// widgets in canvas space, so each [GestureDetector]'s `delta` is already in
/// canvas coordinates. Move is handled by the canvas-level gesture detector.
class TransformHandles extends StatelessWidget {
  const TransformHandles({
    super.key,
    required this.element,
    required this.onResize,
    required this.onRotate,
  });

  final DesignElement element;
  final void Function(HandleCorner corner, Offset canvasDelta) onResize;
  final void Function(Offset handleVector, Offset canvasDelta) onRotate;

  static const double _size = 14;

  @override
  Widget build(BuildContext context) {
    if (element.locked) {
      return const SizedBox.shrink();
    }
    final corners = elementCorners(element);
    final rotationPos = rotationHandlePosition(element);
    final center = elementCenter(element);

    return Stack(
      children: [
        _corner(corners[0], HandleCorner.topLeft),
        _corner(corners[1], HandleCorner.topRight),
        _corner(corners[2], HandleCorner.bottomRight),
        _corner(corners[3], HandleCorner.bottomLeft),
        _rotation(rotationPos, center),
      ],
    );
  }

  Widget _corner(Offset pos, HandleCorner corner) {
    return Positioned(
      left: pos.dx - _size / 2,
      top: pos.dy - _size / 2,
      width: _size,
      height: _size,
      child: GestureDetector(
        behavior: HitTestBehavior.opaque,
        onPanUpdate: (d) => onResize(corner, d.delta),
        child: MouseRegion(
          cursor: SystemMouseCursors.resizeUpLeftDownRight,
          child: Container(
            decoration: BoxDecoration(
              color: Colors.white,
              border: Border.all(color: const Color(0xFF6C5CE7), width: 2),
              borderRadius: BorderRadius.circular(2),
            ),
          ),
        ),
      ),
    );
  }

  Widget _rotation(Offset pos, Offset center) {
    return Positioned(
      left: pos.dx - _size / 2,
      top: pos.dy - _size / 2,
      width: _size,
      height: _size,
      child: GestureDetector(
        behavior: HitTestBehavior.opaque,
        onPanUpdate: (d) => onRotate(pos - center, d.delta),
        child: MouseRegion(
          cursor: SystemMouseCursors.grab,
          child: Container(
            decoration: const BoxDecoration(
              color: Color(0xFF6C5CE7),
              shape: BoxShape.circle,
            ),
            child: const Icon(Icons.rotate_right, size: 10, color: Colors.white),
          ),
        ),
      ),
    );
  }
}
