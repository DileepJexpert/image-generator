import 'dart:math' as math;
import 'dart:ui' as ui;

import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../core/api_client.dart';
import '../core/download/download.dart';
import '../core/id.dart';
import 'canvas/editor_canvas.dart';
import 'export/png_exporter.dart';
import 'model/design_element.dart';
import 'state/editor_controller.dart';
import 'widgets/inspector_panel.dart';

/// The design editor screen: toolbar + canvas + inspector. Loads the project by
/// id, then drives everything through [EditorController].
class EditorPage extends ConsumerStatefulWidget {
  const EditorPage({super.key, required this.projectId});

  final String projectId;

  @override
  ConsumerState<EditorPage> createState() => _EditorPageState();
}

class _EditorPageState extends ConsumerState<EditorPage> {
  final GlobalKey _exportKey = GlobalKey();
  bool _loading = true;
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

  Future<void> _exportPng() async {
    try {
      final bytes = await capturePng(_exportKey, pixelRatio: 3);
      final name = ref.read(editorControllerProvider).project?.name ?? 'design';
      downloadBytes(bytes, '$name.png', 'image/png');
    } catch (e) {
      _toast('Export failed: $e');
    }
  }

  void _addText() {
    ref.read(editorControllerProvider.notifier).addElement(
          DesignElement.text(
            id: newId(),
            x: 80,
            y: 80,
            width: 360,
            height: 96,
            text: 'Double-click to edit',
            color: '#FF111111',
            zIndex: _nextZ(),
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

      // Decode client-side to size the element sensibly within the canvas.
      final codec = await ui.instantiateImageCodec(file.bytes!);
      final frame = await codec.getNextFrame();
      final img = frame.image;
      final project = ref.read(editorControllerProvider).project!;
      final maxW = project.canvasWidth * 0.6;
      final maxH = project.canvasHeight * 0.6;
      // Scale to fit within 60% of the canvas, never upscaling past 1:1.
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

  int _nextZ() => ref.read(editorControllerProvider).project?.nextZIndex ?? 0;

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
            tooltip: 'Export PNG',
            onPressed: editor.hasProject ? _exportPng : null,
            icon: const Icon(Icons.download_outlined),
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
              : Row(
                  children: [
                    _Toolbar(
                      onAddText: _addText,
                      onAddShape: _addShape,
                      onUploadImage: _uploadImage,
                    ),
                    Expanded(child: EditorCanvas(exportKey: _exportKey)),
                    const InspectorPanel(),
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
