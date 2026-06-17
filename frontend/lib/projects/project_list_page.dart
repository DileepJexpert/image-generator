import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../core/api_client.dart';
import '../editor/model/project.dart';
import '../editor/templates/templates.dart';

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
    final created = await showDialog<_NewProjectSpec>(
      context: context,
      builder: (context) => const _NewProjectDialog(),
    );
    if (created == null || !context.mounted) {
      return;
    }

    final api = ref.read(apiClientProvider);
    final summary = await api.createProject(created.name, created.width, created.height);

    // Seed the scene from the chosen template, then save it.
    final project = Project(
      id: summary.id,
      name: created.name,
      canvasWidth: created.width,
      canvasHeight: created.height,
      pages: created.template.build(created.width, created.height),
    );
    await api.saveProject(project);

    ref.invalidate(projectListProvider);
    if (context.mounted) {
      context.go('/editor/${summary.id}');
    }
  }
}

class _NewProjectSpec {
  _NewProjectSpec(this.name, this.width, this.height, this.template);
  final String name;
  final int width;
  final int height;
  final DesignTemplate template;
}

const _sizePresets = {
  'Square 1080×1080': [1080, 1080],
  'Story 1080×1920': [1080, 1920],
  'Landscape 1920×1080': [1920, 1080],
  'A4 794×1123': [794, 1123],
};

class _NewProjectDialog extends StatefulWidget {
  const _NewProjectDialog();

  @override
  State<_NewProjectDialog> createState() => _NewProjectDialogState();
}

class _NewProjectDialogState extends State<_NewProjectDialog> {
  final _name = TextEditingController(text: 'Untitled');
  String _size = _sizePresets.keys.first;
  DesignTemplate _template = kTemplates.first;

  @override
  void dispose() {
    _name.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('New project'),
      content: SizedBox(
        width: 360,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: _name,
              decoration: const InputDecoration(labelText: 'Name'),
            ),
            const SizedBox(height: 12),
            DropdownButtonFormField<String>(
              initialValue: _size,
              decoration: const InputDecoration(labelText: 'Canvas size'),
              items: [
                for (final key in _sizePresets.keys)
                  DropdownMenuItem(value: key, child: Text(key)),
              ],
              onChanged: (v) => setState(() => _size = v ?? _size),
            ),
            const SizedBox(height: 12),
            DropdownButtonFormField<DesignTemplate>(
              initialValue: _template,
              decoration: const InputDecoration(labelText: 'Template'),
              items: [
                for (final t in kTemplates)
                  DropdownMenuItem(value: t, child: Text(t.name)),
              ],
              onChanged: (v) => setState(() => _template = v ?? _template),
            ),
          ],
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: const Text('Cancel'),
        ),
        FilledButton(
          onPressed: () {
            final dims = _sizePresets[_size]!;
            final name = _name.text.trim().isEmpty ? 'Untitled' : _name.text.trim();
            Navigator.pop(
                context, _NewProjectSpec(name, dims[0], dims[1], _template));
          },
          child: const Text('Create'),
        ),
      ],
    );
  }
}
