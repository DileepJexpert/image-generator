import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../model/design_element.dart';
import '../model/project.dart';

/// Editor UI state: the loaded project, the current selection, and whether
/// there are unsaved changes. Held in a Riverpod [Notifier] (CLAUDE.md §7).
class EditorState {
  const EditorState({this.project, this.selectedId, this.dirty = false});

  final Project? project;
  final String? selectedId;
  final bool dirty;

  bool get hasProject => project != null;

  DesignElement? get selected {
    final p = project;
    final id = selectedId;
    if (p == null || id == null) {
      return null;
    }
    for (final e in p.elements) {
      if (e.id == id) {
        return e;
      }
    }
    return null;
  }

  EditorState copyWith({
    Project? project,
    Object? selectedId = _sentinel,
    bool? dirty,
  }) {
    return EditorState(
      project: project ?? this.project,
      selectedId:
          selectedId == _sentinel ? this.selectedId : selectedId as String?,
      dirty: dirty ?? this.dirty,
    );
  }

  static const Object _sentinel = Object();
}

final editorControllerProvider =
    NotifierProvider<EditorController, EditorState>(EditorController.new);

class EditorController extends Notifier<EditorState> {
  @override
  EditorState build() => const EditorState();

  void setProject(Project project) {
    state = EditorState(project: project);
  }

  void markSaved() {
    state = state.copyWith(dirty: false);
  }

  void select(String? id) {
    state = state.copyWith(selectedId: id);
  }

  void setName(String name) {
    final p = state.project;
    if (p == null) {
      return;
    }
    state = state.copyWith(project: p.copyWith(name: name), dirty: true);
  }

  void addElement(DesignElement element) {
    final p = state.project;
    if (p == null) {
      return;
    }
    state = state.copyWith(
      project: p.copyWith(elements: [...p.elements, element]),
      selectedId: element.id,
      dirty: true,
    );
  }

  /// Replace the element with [id] using [transform]. No-op if it is missing.
  void updateElement(String id, DesignElement Function(DesignElement) transform) {
    final p = state.project;
    if (p == null) {
      return;
    }
    final updated = [
      for (final e in p.elements) e.id == id ? transform(e) : e,
    ];
    state = state.copyWith(project: p.copyWith(elements: updated), dirty: true);
  }

  void deleteSelected() {
    final p = state.project;
    final id = state.selectedId;
    if (p == null || id == null) {
      return;
    }
    state = EditorState(
      project: p.copyWith(elements: p.elements.where((e) => e.id != id).toList()),
      selectedId: null,
      dirty: true,
    );
  }

  void bringSelectedToFront() {
    final p = state.project;
    final id = state.selectedId;
    if (p == null || id == null) {
      return;
    }
    final top = p.nextZIndex;
    updateElement(id, (e) => e.copyWith(zIndex: top));
  }
}
