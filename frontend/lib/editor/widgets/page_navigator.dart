import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../state/editor_controller.dart';

/// Bottom strip for multi-page documents: switch, add, and delete pages
/// (CLAUDE.md §7 polish: multi-page).
class PageNavigator extends ConsumerWidget {
  const PageNavigator({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final editor = ref.watch(editorControllerProvider);
    final controller = ref.read(editorControllerProvider.notifier);
    final project = editor.project;
    if (project == null) {
      return const SizedBox.shrink();
    }

    return Container(
      height: 56,
      color: Theme.of(context).colorScheme.surfaceContainerHigh,
      padding: const EdgeInsets.symmetric(horizontal: 8),
      child: Row(
        children: [
          Expanded(
            child: ListView.separated(
              scrollDirection: Axis.horizontal,
              itemCount: project.pages.length,
              separatorBuilder: (_, __) => const SizedBox(width: 6),
              itemBuilder: (context, i) {
                final selected = i == editor.currentPageIndex;
                return Center(
                  child: ChoiceChip(
                    label: Text('Page ${i + 1}'),
                    selected: selected,
                    onSelected: (_) => controller.selectPage(i),
                  ),
                );
              },
            ),
          ),
          const SizedBox(width: 8),
          IconButton(
            tooltip: 'Add page',
            onPressed: controller.addPage,
            icon: const Icon(Icons.add_box_outlined),
          ),
          IconButton(
            tooltip: 'Delete page',
            onPressed: project.pages.length > 1 ? controller.deleteCurrentPage : null,
            icon: const Icon(Icons.delete_outline),
          ),
        ],
      ),
    );
  }
}
