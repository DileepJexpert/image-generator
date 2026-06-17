import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../model/design_element.dart';
import '../model/project.dart';
import '../model/scene_page.dart';

/// Editor UI state: the loaded project, the active page, the current selection,
/// and whether there are unsaved changes (CLAUDE.md §7).
class EditorState {
  const EditorState({
    this.project,
    this.currentPageIndex = 0,
    this.selectedId,
    this.dirty = false,
  });

  final Project? project;
  final int currentPageIndex;
  final String? selectedId;
  final bool dirty;

  bool get hasProject => project != null;

  ScenePage? get currentPage {
    final p = project;
    if (p == null || p.pages.isEmpty) {
      return null;
    }
    final i = currentPageIndex.clamp(0, p.pages.length - 1);
    return p.pages[i];
  }

  DesignElement? get selected {
    final page = currentPage;
    final id = selectedId;
    if (page == null || id == null) {
      return null;
    }
    for (final e in page.elements) {
      if (e.id == id) {
        return e;
      }
    }
    return null;
  }

  EditorState copyWith({
    Project? project,
    int? currentPageIndex,
    Object? selectedId = _sentinel,
    bool? dirty,
  }) {
    return EditorState(
      project: project ?? this.project,
      currentPageIndex: currentPageIndex ?? this.currentPageIndex,
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
    state = EditorState(project: project.ensureHasPage());
  }

  void markSaved() => state = state.copyWith(dirty: false);

  void select(String? id) => state = state.copyWith(selectedId: id);

  void setName(String name) {
    final p = state.project;
    if (p == null) {
      return;
    }
    state = state.copyWith(project: p.copyWith(name: name), dirty: true);
  }

  // --- Pages ---------------------------------------------------------------

  void selectPage(int index) {
    final p = state.project;
    if (p == null || index < 0 || index >= p.pages.length) {
      return;
    }
    state = state.copyWith(currentPageIndex: index, selectedId: null);
  }

  void addPage() {
    final p = state.project;
    if (p == null) {
      return;
    }
    final page = ScenePage(id: '${p.id}_p${DateTime.now().millisecondsSinceEpoch}');
    state = state.copyWith(
      project: p.copyWith(pages: [...p.pages, page]),
      currentPageIndex: p.pages.length,
      selectedId: null,
      dirty: true,
    );
  }

  void deleteCurrentPage() {
    final p = state.project;
    if (p == null || p.pages.length <= 1) {
      return;
    }
    final pages = [...p.pages]..removeAt(state.currentPageIndex);
    final newIndex = state.currentPageIndex.clamp(0, pages.length - 1);
    state = state.copyWith(
      project: p.copyWith(pages: pages),
      currentPageIndex: newIndex,
      selectedId: null,
      dirty: true,
    );
  }

  // --- Elements (operate on the current page) ------------------------------

  void _updateCurrentPage(ScenePage Function(ScenePage) transform,
      {String? selectId, bool clearSelection = false}) {
    final p = state.project;
    final page = state.currentPage;
    if (p == null || page == null) {
      return;
    }
    final pages = [
      for (var i = 0; i < p.pages.length; i++)
        i == state.currentPageIndex.clamp(0, p.pages.length - 1)
            ? transform(page)
            : p.pages[i],
    ];
    state = state.copyWith(
      project: p.copyWith(pages: pages),
      selectedId: clearSelection ? null : (selectId ?? state.selectedId),
      dirty: true,
    );
  }

  void addElement(DesignElement element) {
    _updateCurrentPage(
      (page) => page.copyWith(elements: [...page.elements, element]),
      selectId: element.id,
    );
  }

  void updateElement(String id, DesignElement Function(DesignElement) transform) {
    _updateCurrentPage((page) => page.copyWith(
          elements: [
            for (final e in page.elements) e.id == id ? transform(e) : e,
          ],
        ));
  }

  void deleteSelected() {
    final id = state.selectedId;
    if (id == null) {
      return;
    }
    _updateCurrentPage(
      (page) => page.copyWith(
          elements: page.elements.where((e) => e.id != id).toList()),
      clearSelection: true,
    );
  }

  void bringSelectedToFront() {
    final id = state.selectedId;
    final page = state.currentPage;
    if (id == null || page == null) {
      return;
    }
    final top = page.nextZIndex;
    updateElement(id, (e) => e.copyWith(zIndex: top));
  }
}
