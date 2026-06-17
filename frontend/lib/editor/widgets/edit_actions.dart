import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/api_client.dart';
import '../../core/ws_client.dart';
import '../model/design_element.dart';
import '../state/editor_controller.dart';

/// Remove-bg / upscale actions for the selected image (CLAUDE.md §7). Submits an
/// edit job, tracks WS progress, and on completion swaps the element's assetId
/// to the processed result (same on-canvas size, new bytes).
class EditActions extends ConsumerStatefulWidget {
  const EditActions({super.key, required this.element});

  final ImageElement element;

  @override
  ConsumerState<EditActions> createState() => _EditActionsState();
}

class _EditActionsState extends ConsumerState<EditActions> {
  JobSocket? _socket;
  StreamSubscription<JobProgress>? _sub;
  bool _running = false;
  String _label = '';
  int _progress = 0;
  void Function(String resultAssetId)? _onResult;

  @override
  void dispose() {
    _sub?.cancel();
    _socket?.close();
    super.dispose();
  }

  Future<void> _run(
    String label,
    Future<String> Function() submit,
    void Function(String resultAssetId) onResult,
  ) async {
    if (_running) {
      return;
    }
    setState(() {
      _running = true;
      _label = label;
      _progress = 0;
      _onResult = onResult;
    });
    try {
      final jobId = await submit();
      final socket = JobSocket(jobId);
      _socket = socket;
      _sub = socket.progress.listen(_onEvent, onError: (Object e) => _fail('$e'));
    } catch (e) {
      _fail('$e');
    }
  }

  /// Swap the selected image's asset for the processed result (remove-bg/upscale).
  void _swapAsset(String newAssetId) {
    ref.read(editorControllerProvider.notifier).updateElement(
          widget.element.id,
          (e) => (e as ImageElement).copyWith(assetId: newAssetId),
        );
  }

  /// Add a VideoElement next to the source image (image-to-video).
  void _addVideo(String videoAssetId) {
    final notifier = ref.read(editorControllerProvider.notifier);
    final project = ref.read(editorControllerProvider).project;
    final el = widget.element;
    notifier.addElement(
      DesignElement.video(
        id: '${el.id}_video_${DateTime.now().millisecondsSinceEpoch}',
        x: el.x + 24,
        y: el.y + 24,
        width: el.width,
        height: el.height,
        assetId: videoAssetId,
        zIndex: project?.nextZIndex ?? el.zIndex + 1,
      ),
    );
  }

  void _onEvent(JobProgress event) {
    if (!mounted) {
      return;
    }
    setState(() => _progress = event.progress);
    if (event.isDone && event.resultAssetId != null) {
      _onResult?.call(event.resultAssetId!);
      _toast('$_label done');
      _cleanup();
    } else if (event.isFailed) {
      _fail(event.error ?? 'failed');
    }
  }

  void _fail(String message) {
    _toast('$_label failed: $message');
    _cleanup();
  }

  void _cleanup() {
    _sub?.cancel();
    _socket?.close();
    _sub = null;
    _socket = null;
    if (mounted) {
      setState(() => _running = false);
    }
  }

  void _toast(String msg) {
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg)));
    }
  }

  Future<void> _animate() async {
    final params = await showDialog<({int seconds, String prompt})>(
      context: context,
      builder: (context) => const _AnimateDialog(),
    );
    if (params == null || !mounted) {
      return;
    }
    final api = ref.read(apiClientProvider);
    await _run(
      'Animate',
      () => api.imageToVideo(widget.element.assetId, params.prompt, params.seconds),
      _addVideo,
    );
  }

  @override
  Widget build(BuildContext context) {
    final api = ref.read(apiClientProvider);
    final assetId = widget.element.assetId;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('AI edits', style: Theme.of(context).textTheme.labelLarge),
        const SizedBox(height: 8),
        SizedBox(
          width: double.infinity,
          child: OutlinedButton.icon(
            onPressed: _running
                ? null
                : () => _run('Remove background',
                    () => api.removeBackground(assetId), _swapAsset),
            icon: const Icon(Icons.auto_fix_high, size: 18),
            label: const Text('Remove background'),
          ),
        ),
        const SizedBox(height: 6),
        Row(
          children: [
            Expanded(
              child: OutlinedButton(
                onPressed: _running
                    ? null
                    : () => _run('Upscale 2×', () => api.upscale(assetId, 2), _swapAsset),
                child: const Text('Upscale 2×'),
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: OutlinedButton(
                onPressed: _running
                    ? null
                    : () => _run('Upscale 4×', () => api.upscale(assetId, 4), _swapAsset),
                child: const Text('Upscale 4×'),
              ),
            ),
          ],
        ),
        const SizedBox(height: 6),
        SizedBox(
          width: double.infinity,
          child: OutlinedButton.icon(
            onPressed: _running ? null : _animate,
            icon: const Icon(Icons.movie_creation_outlined, size: 18),
            label: const Text('Animate → video'),
          ),
        ),
        if (_running) ...[
          const SizedBox(height: 10),
          LinearProgressIndicator(value: _progress > 0 ? _progress / 100 : null),
          const SizedBox(height: 4),
          Text('$_label… ${_progress > 0 ? '$_progress%' : ''}',
              style: Theme.of(context).textTheme.bodySmall),
        ],
      ],
    );
  }
}

/// Collects the clip length + optional motion prompt for image-to-video.
class _AnimateDialog extends StatefulWidget {
  const _AnimateDialog();

  @override
  State<_AnimateDialog> createState() => _AnimateDialogState();
}

class _AnimateDialogState extends State<_AnimateDialog> {
  final _prompt = TextEditingController();
  int _seconds = 3;

  @override
  void dispose() {
    _prompt.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('Animate image → video'),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          TextField(
            controller: _prompt,
            decoration: const InputDecoration(
              labelText: 'Motion prompt (optional)',
              hintText: 'gentle camera zoom in…',
            ),
          ),
          const SizedBox(height: 16),
          Text('Duration: $_seconds s'),
          Slider(
            value: _seconds.toDouble(),
            min: 1,
            max: 5,
            divisions: 4,
            label: '$_seconds s',
            onChanged: (v) => setState(() => _seconds = v.round()),
          ),
        ],
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: const Text('Cancel'),
        ),
        FilledButton(
          onPressed: () => Navigator.pop(
              context, (seconds: _seconds, prompt: _prompt.text.trim())),
          child: const Text('Generate'),
        ),
      ],
    );
  }
}
