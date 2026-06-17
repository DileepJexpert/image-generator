import 'dart:async';
import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../core/api_client.dart';
import '../core/id.dart';
import '../core/ws_client.dart';
import '../editor/model/design_element.dart';
import '../editor/state/editor_controller.dart';

/// One selectable output size.
class _SizeOption {
  const _SizeOption(this.label, this.width, this.height);
  final String label;
  final int width;
  final int height;
}

const _sizes = [
  _SizeOption('Square 1024²', 1024, 1024),
  _SizeOption('Portrait 832×1216', 832, 1216),
  _SizeOption('Landscape 1216×832', 1216, 832),
  _SizeOption('Small 768²', 768, 768),
];

/// label -> ComfyUI checkpoint filename (must exist in ComfyUI's models dir).
const _models = {
  'SDXL Base': 'sd_xl_base_1.0.safetensors',
  'SDXL Lightning (fast)': 'sdxl_lightning_8step.safetensors',
  'DreamShaper XL': 'dreamshaperXL.safetensors',
};

/// The image AI panel (CLAUDE.md §7): prompt + size + model → POST
/// /generate/image → WS progress → on done, add an ImageElement to the canvas.
class ImagePanel extends ConsumerStatefulWidget {
  const ImagePanel({super.key});

  @override
  ConsumerState<ImagePanel> createState() => _ImagePanelState();
}

class _ImagePanelState extends ConsumerState<ImagePanel> {
  final _prompt = TextEditingController();
  final _negative = TextEditingController();
  _SizeOption _size = _sizes.first;
  String _model = _models.keys.first;

  JobSocket? _socket;
  StreamSubscription<JobProgress>? _sub;
  bool _running = false;
  int _progress = 0;
  String _status = '';
  String? _error;

  @override
  void dispose() {
    _sub?.cancel();
    _socket?.close();
    _prompt.dispose();
    _negative.dispose();
    super.dispose();
  }

  Future<void> _generate() async {
    if (_prompt.text.trim().isEmpty || _running) {
      return;
    }
    setState(() {
      _running = true;
      _progress = 0;
      _status = 'Submitting…';
      _error = null;
    });

    try {
      final jobId = await ref.read(apiClientProvider).generateImage(
            prompt: _prompt.text.trim(),
            negativePrompt: _negative.text.trim(),
            width: _size.width,
            height: _size.height,
            model: _models[_model]!,
          );

      final socket = JobSocket(jobId);
      _socket = socket;
      _sub = socket.progress.listen(
        _onEvent,
        onError: (Object e) => _fail('$e'),
      );
      setState(() => _status = 'Queued…');
    } catch (e) {
      _fail('$e');
    }
  }

  void _onEvent(JobProgress event) {
    if (!mounted) {
      return;
    }
    setState(() {
      _progress = event.progress;
      _status = event.status;
    });
    if (event.isDone && event.resultAssetId != null) {
      _placeImage(event.resultAssetId!);
      _cleanup();
    } else if (event.isFailed) {
      _fail(event.error ?? 'Generation failed');
    }
  }

  void _placeImage(String assetId) {
    final project = ref.read(editorControllerProvider).project;
    if (project == null) {
      return;
    }
    // Fit the generated image within ~60% of the canvas, centered.
    final maxW = project.canvasWidth * 0.6;
    final maxH = project.canvasHeight * 0.6;
    final scale = math.min(1.0, math.min(maxW / _size.width, maxH / _size.height));
    final w = _size.width * scale;
    final h = _size.height * scale;

    ref.read(editorControllerProvider.notifier).addElement(
          DesignElement.image(
            id: newId(),
            x: (project.canvasWidth - w) / 2,
            y: (project.canvasHeight - h) / 2,
            width: w,
            height: h,
            assetId: assetId,
            zIndex: project.nextZIndex,
          ),
        );
    _toast('Image added to canvas');
  }

  void _fail(String message) {
    if (!mounted) {
      return;
    }
    setState(() => _error = message);
    _toast('Generation failed: $message');
    _cleanup();
  }

  void _cleanup() {
    _sub?.cancel();
    _socket?.close();
    _sub = null;
    _socket = null;
    if (mounted) {
      setState(() {
        _running = false;
        _status = '';
      });
    }
  }

  void _toast(String msg) {
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg)));
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 300,
      color: Theme.of(context).colorScheme.surfaceContainerHighest,
      padding: const EdgeInsets.all(16),
      child: ListView(
        children: [
          Row(
            children: [
              const Icon(Icons.auto_awesome, size: 20),
              const SizedBox(width: 8),
              Text('Generate image',
                  style: Theme.of(context).textTheme.titleMedium),
            ],
          ),
          const SizedBox(height: 16),
          TextField(
            controller: _prompt,
            minLines: 2,
            maxLines: 5,
            decoration: const InputDecoration(
              labelText: 'Prompt',
              border: OutlineInputBorder(),
              hintText: 'a serene mountain lake at sunrise…',
            ),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _negative,
            minLines: 1,
            maxLines: 3,
            decoration: const InputDecoration(
              labelText: 'Negative prompt (optional)',
              border: OutlineInputBorder(),
            ),
          ),
          const SizedBox(height: 12),
          DropdownButtonFormField<_SizeOption>(
            initialValue: _size,
            decoration: const InputDecoration(
                labelText: 'Size', border: OutlineInputBorder()),
            items: [
              for (final s in _sizes)
                DropdownMenuItem(value: s, child: Text(s.label)),
            ],
            onChanged: _running ? null : (v) => setState(() => _size = v ?? _size),
          ),
          const SizedBox(height: 12),
          DropdownButtonFormField<String>(
            initialValue: _model,
            decoration: const InputDecoration(
                labelText: 'Model', border: OutlineInputBorder()),
            items: [
              for (final m in _models.keys)
                DropdownMenuItem(value: m, child: Text(m)),
            ],
            onChanged: _running ? null : (v) => setState(() => _model = v ?? _model),
          ),
          const SizedBox(height: 16),
          FilledButton.icon(
            onPressed: _running ? null : _generate,
            icon: const Icon(Icons.auto_awesome),
            label: Text(_running ? 'Generating…' : 'Generate'),
          ),
          if (_running) ...[
            const SizedBox(height: 16),
            LinearProgressIndicator(value: _progress > 0 ? _progress / 100 : null),
            const SizedBox(height: 6),
            Text('$_status  ${_progress > 0 ? '$_progress%' : ''}',
                style: Theme.of(context).textTheme.bodySmall),
          ],
          if (_error != null) ...[
            const SizedBox(height: 12),
            Text(_error!, style: const TextStyle(color: Colors.redAccent)),
          ],
        ],
      ),
    );
  }
}
