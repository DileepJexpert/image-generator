import 'package:freezed_annotation/freezed_annotation.dart';

import 'design_element.dart';

part 'scene_page.freezed.dart';
part 'scene_page.g.dart';

/// One page of a (multi-page) design. Holds its own element stack.
@freezed
class ScenePage with _$ScenePage {
  const ScenePage._();

  const factory ScenePage({
    required String id,
    @Default(<DesignElement>[]) List<DesignElement> elements,
  }) = _ScenePage;

  factory ScenePage.fromJson(Map<String, dynamic> json) =>
      _$ScenePageFromJson(json);

  /// Elements sorted bottom-to-top for painting.
  List<DesignElement> get elementsByZ {
    final sorted = [...elements]..sort((a, b) => a.zIndex.compareTo(b.zIndex));
    return sorted;
  }

  /// The next z-index to place a new element on top of the stack.
  int get nextZIndex => elements.isEmpty
      ? 0
      : elements.map((e) => e.zIndex).reduce((a, b) => a > b ? a : b) + 1;
}
