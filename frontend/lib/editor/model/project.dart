import 'package:freezed_annotation/freezed_annotation.dart';

import 'scene_page.dart';

part 'project.freezed.dart';
part 'project.g.dart';

/// The design document and the on-disk save format. `Project.toJson()` is what
/// gets PUT to `/projects/{id}` as `sceneJson` (CLAUDE.md section 7). A document
/// has one or more [ScenePage]s (multi-page support, milestone 7).
@freezed
class Project with _$Project {
  const Project._();

  const factory Project({
    required String id,
    required String name,
    required int canvasWidth,
    required int canvasHeight,
    @Default(<ScenePage>[]) List<ScenePage> pages,
  }) = _Project;

  factory Project.fromJson(Map<String, dynamic> json) =>
      _$ProjectFromJson(json);

  /// Guarantees at least one page exists (older single-page saves / fresh docs).
  Project ensureHasPage() => pages.isEmpty
      ? copyWith(pages: [ScenePage(id: '${id}_p0')])
      : this;
}
