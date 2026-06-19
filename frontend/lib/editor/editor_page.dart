import 'dart:math' as math;
import 'dart:ui' as ui;

import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../core/api_client.dart';
import '../core/download/download.dart';
import '../core/id.dart';
import '../generation/audio_panel.dart';
import '../generation/copilot_panel.dart';
import '../generation/image_panel.dart';
import '../generation/leads_panel.dart';
import 'canvas/editor_canvas.dart';
import 'canvas/image_cache.dart';
import 'export/pdf_exporter.dart';
import 'export/scene_rasterizer.dart';
import 'model/design_element.dart';
import 'state/editor_controller.dart';
import 'widgets/inspector_panel.dart';
import 'widgets/page_navigator.dart';

/// The design editor screen: toolbar + AI panel + canvas + inspector + page
/// navigator. Loads the project by id, then drives everything through
/// [EditorController].
class EditorPage extends ConsumerStatefulWidget {
  const EditorPage({super.key, required this.projectId});

  final String projectId;

  @override
  ConsumerState<EditorPage> createState() => _EditorPageState();
}

class _EditorPageState extends ConsumerState<EditorPage> {
  bool _loading = true;
  bool _showAiPanel = true;
  bool _showCopilot = false;
  bool _showAudio = false;
  bool _showLeads = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final project = await ref.read(apiClientProvider).getProject(widget.projectId);
      ref.read(editorControllerProvider.notifier).setProject(project);
      setState(() => _loading = false);
    } catch (e) {
      setState(() {
        _loading = false;
        _error = '$e';
      });
    }
  }

  Future<void> _save() async {
    final project = ref.read(editorControllerProvider).project;
    if (project == null) {
      return;
    }
    try {
      await ref.read(apiClientProvider).saveProject(project);
      ref.read(editorControllerProvider.notifier).markSaved();
      _toast('Saved');
    } catch (e) {
      _toast('Save failed: $e');
    }
  }

  Size get _canvasSize {
    final p = ref.read(editorControllerProvider).project!;
    return Size(p.canvasWidth.toDouble(), p.canvasHeight.toDouble());
  }

  Future<Map<String, ui.Image>> _preload(Iterable<DesignElement> elements) {
    final ids =
        elements.map((e) => e.assetIdOrNull).whereType<String>().toList();
    return ref.read(imageCacheProvider.notifier).loadAll(ids);
  }

  Future<void> _exportPng() async {
    final editor = ref.read(editorControllerProvider);
    final page = editor.currentPage;
    if (page == null) {
      return;
    }
    try {
      final images = await _preload(page.elements);
      final bytes = await rasterizePagePng(
        elements: page.elements,
        canvasSize: _canvasSize,
        images: images,
        pixelRatio: 3,
      );
      downloadBytes(bytes, '${editor.project!.name}.png', 'image/png');
    } catch (e) {
      _toast('Export failed: $e');
    }
  }

  Future<void> _exportPdf() async {
    final project = ref.read(editorControllerProvider).project;
    if (project == null) {
      return;
    }
    try {
      final images = await _preload(project.pages.expand((p) => p.elements));
      final bytes = await exportProjectPdf(project: project, images: images);
      downloadBytes(bytes, '${project.name}.pdf', 'application/pdf');
    } catch (e) {
      _toast('Export failed: $e');
    }
  }

  void _addText() {
    final z = _nextZ();
    ref.read(editorControllerProvider.notifier).addElement(
          DesignElement.text(
            id: newId(),
            x: 80,
            y: 80,
            width: 360,
            height: 96,
            text: 'Double-click to edit',
            color: '#FF111111',
            zIndex: z,
          ),
        );
  }

  void _addShape() {
    ref.read(editorControllerProvider.notifier).addElement(
          DesignElement.shape(
            id: newId(),
            x: 120,
            y: 120,
            width: 240,
            height: 160,
            shape: 'rrect',
            cornerRadius: 16,
            fill: '#FF6C5CE7',
            zIndex: _nextZ(),
          ),
        );
  }

  Future<void> _uploadImage() async {
    final result = await FilePicker.platform.pickFiles(
      type: FileType.image,
      withData: true,
    );
    final file = result?.files.single;
    if (file == null || file.bytes == null) {
      return;
    }
    try {
      final api = ref.read(apiClientProvider);
      final assetId = await api.uploadAsset(file.bytes!, file.name);

      final codec = await ui.instantiateImageCodec(file.bytes!);
      final frame = await codec.getNextFrame();
      final img = frame.image;
      final project = ref.read(editorControllerProvider).project!;
      final maxW = project.canvasWidth * 0.6;
      final maxH = project.canvasHeight * 0.6;
      final scale =
          math.min(1.0, math.min(maxW / img.width, maxH / img.height));
      final w = img.width * scale;
      final h = img.height * scale;

      ref.read(editorControllerProvider.notifier).addElement(
            DesignElement.image(
              id: newId(),
              x: (project.canvasWidth - w) / 2,
              y: (project.canvasHeight - h) / 2,
              width: w,
              height: h,
              assetId: assetId,
              zIndex: _nextZ(),
            ),
          );
    } catch (e) {
      _toast('Upload failed: $e');
    }
  }

  int _nextZ() => ref.read(editorControllerProvider).currentPage?.nextZIndex ?? 0;

  void _toast(String msg) {
    if (!mounted) {
      return;
    }
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg)));
  }

  @override
  Widget build(BuildContext context) {
    final editor = ref.watch(editorControllerProvider);
    final dirty = editor.dirty;

    return Scaffold(
      appBar: AppBar(
        title: Text(editor.project?.name ?? 'Editor'),
        actions: [
          IconButton(
            tooltip: 'AI image panel',
            isSelected: _showAiPanel,
            onPressed: () => setState(() => _showAiPanel = !_showAiPanel),
            icon: const Icon(Icons.auto_awesome_outlined),
            selectedIcon: const Icon(Icons.auto_awesome),
          ),
          IconButton(
            tooltip: 'Voiceover',
            isSelected: _showAudio,
            onPressed: () => setState(() => _showAudio = !_showAudio),
            icon: const Icon(Icons.record_voice_over_outlined),
            selectedIcon: const Icon(Icons.record_voice_over),
          ),
          IconButton(
            tooltip: 'Copilot',
            isSelected: _showCopilot,
            onPressed: () => setState(() => _showCopilot = !_showCopilot),
            icon: const Icon(Icons.smart_toy_outlined),
            selectedIcon: const Icon(Icons.smart_toy),
          ),
          IconButton(
            tooltip: 'Leads',
            isSelected: _showLeads,
            onPressed: () => setState(() => _showLeads = !_showLeads),
            icon: const Icon(Icons.travel_explore_outlined),
            selectedIcon: const Icon(Icons.travel_explore),
          ),
          PopupMenuButton<String>(
            tooltip: 'Export',
            icon: const Icon(Icons.download_outlined),
            enabled: editor.hasProject,
            onSelected: (v) => v == 'png' ? _exportPng() : _exportPdf(),
            itemBuilder: (context) => const [
              PopupMenuItem(value: 'png', child: Text('Export PNG (current page)')),
              PopupMenuItem(value: 'pdf', child: Text('Export PDF (all pages)')),
            ],
          ),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 8),
            child: FilledButton.icon(
              onPressed: editor.hasProject ? _save : null,
              icon: Icon(dirty ? Icons.save : Icons.check),
              label: Text(dirty ? 'Save' : 'Saved'),
            ),
          ),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Text('Failed to load project:\n$_error'))
              : Column(
                  children: [
                    Expanded(
                      child: Row(
                        children: [
                          _Toolbar(
                            onAddText: _addText,
                            onAddShape: _addShape,
                            onUploadImage: _uploadImage,
                          ),
                          if (_showAiPanel) const ImagePanel(),
                          if (_showAudio) const AudioPanel(),
                          if (_showCopilot) const CopilotPanel(),
                          if (_showLeads) const LeadsPanel(),
                          const Expanded(child: EditorCanvas()),
                          const InspectorPanel(),
                        ],
                      ),
                    ),
                    const PageNavigator(),
                  ],
                ),
    );
  }
}

class _Toolbar extends StatelessWidget {
  const _Toolbar({
    required this.onAddText,
    required this.onAddShape,
    required this.onUploadImage,
  });

  final VoidCallback onAddText;
  final VoidCallback onAddShape;
  final VoidCallback onUploadImage;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 72,
      color: Theme.of(context).colorScheme.surfaceContainerHighest,
      child: Column(
        children: [
          const SizedBox(height: 12),
          _ToolButton(icon: Icons.text_fields, label: 'Text', onTap: onAddText),
          _ToolButton(icon: Icons.category_outlined, label: 'Shape', onTap: onAddShape),
          _ToolButton(icon: Icons.image_outlined, label: 'Image', onTap: onUploadImage),
        ],
      ),
    );
  }
}

class _ToolButton extends StatelessWidget {
  const _ToolButton({required this.icon, required this.label, required this.onTap});

  final IconData icon;
  final String label;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(8),
        child: Padding(
          padding: const EdgeInsets.all(8),
          child: Column(
            children: [
              Icon(icon, size: 24),
              const SizedBox(height: 4),
              Text(label, style: const TextStyle(fontSize: 11)),
            ],
          ),
        ),
      ),
    );
  }
}
