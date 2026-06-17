import 'package:freezed_annotation/freezed_annotation.dart';

import 'design_element.dart';

part 'project.freezed.dart';
part 'project.g.dart';

/// The design document and the on-disk save format. `Project.toJson()` is what
/// gets PUT to `/projects/{id}` as `sceneJson` (CLAUDE.md section 7).
@freezed
class Project with _$Project {
  const Project._();

  const factory Project({
    required String id,
    required String name,
    required int canvasWidth,
    required int canvasHeight,
    @Default(<DesignElement>[]) List<DesignElement> elements,
  }) = _Project;

  factory Project.fromJson(Map<String, dynamic> json) =>
      _$ProjectFromJson(json);

  /// Elements sorted bottom-to-top for painting.
  List<DesignElement> get elementsByZ {
    final sorted = [...elements]..sort((a, b) => a.zIndex.compareTo(b.zIndex));
    return sorted;
  }

  /// The next z-index to place a new element on top of the stack.
  int get nextZIndex =>
      elements.isEmpty ? 0 : elements.map((e) => e.zIndex).reduce((a, b) => a > b ? a : b) + 1;
}
