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

  @override
  void dispose() {
    _sub?.cancel();
    _socket?.close();
    super.dispose();
  }

  Future<void> _run(String label, Future<String> Function() submit) async {
    if (_running) {
      return;
    }
    setState(() {
      _running = true;
      _label = label;
      _progress = 0;
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

  void _onEvent(JobProgress event) {
    if (!mounted) {
      return;
    }
    setState(() => _progress = event.progress);
    if (event.isDone && event.resultAssetId != null) {
      final newAssetId = event.resultAssetId!;
      ref.read(editorControllerProvider.notifier).updateElement(
            widget.element.id,
            (e) => (e as ImageElement).copyWith(assetId: newAssetId),
          );
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
            onPressed: _running ? null : () => _run('Remove background',
                () => api.removeBackground(assetId)),
            icon: const Icon(Icons.auto_fix_high, size: 18),
            label: const Text('Remove background'),
          ),
        ),
        const SizedBox(height: 6),
        Row(
          children: [
            Expanded(
              child: OutlinedButton(
                onPressed:
                    _running ? null : () => _run('Upscale 2×', () => api.upscale(assetId, 2)),
                child: const Text('Upscale 2×'),
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: OutlinedButton(
                onPressed:
                    _running ? null : () => _run('Upscale 4×', () => api.upscale(assetId, 4)),
                child: const Text('Upscale 4×'),
              ),
            ),
          ],
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
