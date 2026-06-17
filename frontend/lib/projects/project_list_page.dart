import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../core/api_client.dart';

/// Loads the project list from the backend.
final projectListProvider = FutureProvider<List<ProjectSummary>>((ref) {
  return ref.watch(apiClientProvider).listProjects();
});

class ProjectListPage extends ConsumerWidget {
  const ProjectListPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final projects = ref.watch(projectListProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('Katixo Studio — Projects')),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => _createDialog(context, ref),
        icon: const Icon(Icons.add),
        label: const Text('New project'),
      ),
      body: projects.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text('Failed to load projects:\n$e')),
        data: (list) {
          if (list.isEmpty) {
            return const Center(child: Text('No projects yet. Create one!'));
          }
          return ListView.separated(
            padding: const EdgeInsets.all(12),
            itemCount: list.length,
            separatorBuilder: (_, __) => const SizedBox(height: 4),
            itemBuilder: (context, i) {
              final p = list[i];
              return Card(
                child: ListTile(
                  leading: const Icon(Icons.dashboard_customize_outlined),
                  title: Text(p.name),
                  subtitle: Text('${p.canvasWidth} × ${p.canvasHeight}'),
                  trailing: IconButton(
                    icon: const Icon(Icons.delete_outline),
                    onPressed: () async {
                      await ref.read(apiClientProvider).deleteProject(p.id);
                      ref.invalidate(projectListProvider);
                    },
                  ),
                  onTap: () => context.go('/editor/${p.id}'),
                ),
              );
            },
          );
        },
      ),
    );
  }

  Future<void> _createDialog(BuildContext context, WidgetRef ref) async {
    final nameCtrl = TextEditingController(text: 'Untitled');
    int width = 1080;
    int height = 1080;

    final created = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('New project'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: nameCtrl,
              decoration: const InputDecoration(labelText: 'Name'),
            ),
            const SizedBox(height: 12),
            DropdownButtonFormField<String>(
              initialValue: '1080x1080',
              decoration: const InputDecoration(labelText: 'Canvas size'),
              items: const [
                DropdownMenuItem(value: '1080x1080', child: Text('Square 1080×1080')),
                DropdownMenuItem(value: '1080x1920', child: Text('Story 1080×1920')),
                DropdownMenuItem(value: '1920x1080', child: Text('Landscape 1920×1080')),
                DropdownMenuItem(value: '794x1123', child: Text('A4 794×1123')),
              ],
              onChanged: (v) {
                final parts = (v ?? '1080x1080').split('x');
                width = int.parse(parts[0]);
                height = int.parse(parts[1]);
              },
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Create'),
          ),
        ],
      ),
    );

    if (created != true || !context.mounted) {
      return;
    }
    final summary = await ref
        .read(apiClientProvider)
        .createProject(nameCtrl.text.trim().isEmpty ? 'Untitled' : nameCtrl.text.trim(),
            width, height);
    ref.invalidate(projectListProvider);
    if (context.mounted) {
      context.go('/editor/${summary.id}');
    }
  }
}
