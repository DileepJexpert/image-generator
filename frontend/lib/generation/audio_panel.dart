import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../core/api_client.dart';
import '../core/download/download.dart';
import '../core/ws_client.dart';

/// The voiceover panel (VISION.md Phase 2): type text → POST /generate/speech →
/// WS progress → on done, play or download the generated audio asset. Audio
/// isn't a visual canvas element, so the result is offered as a clip rather than
/// placed on the page.
class AudioPanel extends ConsumerStatefulWidget {
  const AudioPanel({super.key});

  @override
  ConsumerState<AudioPanel> createState() => _AudioPanelState();
}

class _AudioPanelState extends ConsumerState<AudioPanel> {
  final _text = TextEditingController();

  JobSocket? _socket;
  StreamSubscription<JobProgress>? _sub;
  bool _running = false;
  int _progress = 0;
  String _status = '';
  String? _error;
  String? _resultAssetId;

  @override
  void dispose() {
    _sub?.cancel();
    _socket?.close();
    _text.dispose();
    super.dispose();
  }

  Future<void> _generate() async {
    if (_text.text.trim().isEmpty || _running) {
      return;
    }
    setState(() {
      _running = true;
      _progress = 0;
      _status = 'Submitting…';
      _error = null;
      _resultAssetId = null;
    });

    try {
      final jobId =
          await ref.read(apiClientProvider).generateSpeech(_text.text.trim());
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
      setState(() => _resultAssetId = event.resultAssetId);
      _cleanup();
      _toast('Voiceover ready');
    } else if (event.isFailed) {
      _fail(event.error ?? 'Speech generation failed');
    }
  }

  Future<void> _download() async {
    final id = _resultAssetId;
    if (id == null) {
      return;
    }
    try {
      final bytes = await ref.read(apiClientProvider).fetchAssetBytes(id);
      downloadBytes(bytes, 'voiceover.wav', 'audio/wav');
    } catch (e) {
      _toast('Download failed: $e');
    }
  }

  void _play() {
    final id = _resultAssetId;
    if (id == null) {
      return;
    }
    openUrl(ref.read(apiClientProvider).assetUrl(id));
  }

  void _fail(String message) {
    if (!mounted) {
      return;
    }
    setState(() => _error = _describe(message));
    _cleanup();
  }

  String _describe(String message) {
    if (message.contains('502') || message.contains('Connection')) {
      return 'Voiceover service is unreachable. Is the tts sidecar running?';
    }
    return message;
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
              const Icon(Icons.record_voice_over_outlined, size: 20),
              const SizedBox(width: 8),
              Text('Voiceover',
                  style: Theme.of(context).textTheme.titleMedium),
            ],
          ),
          const SizedBox(height: 16),
          TextField(
            controller: _text,
            minLines: 3,
            maxLines: 8,
            decoration: const InputDecoration(
              labelText: 'Script',
              border: OutlineInputBorder(),
              hintText: 'Welcome to our product showcase…',
            ),
          ),
          const SizedBox(height: 16),
          FilledButton.icon(
            onPressed: _running ? null : _generate,
            icon: const Icon(Icons.record_voice_over),
            label: Text(_running ? 'Generating…' : 'Generate voiceover'),
          ),
          if (_running) ...[
            const SizedBox(height: 16),
            LinearProgressIndicator(value: _progress > 0 ? _progress / 100 : null),
            const SizedBox(height: 6),
            Text('$_status  ${_progress > 0 ? '$_progress%' : ''}',
                style: Theme.of(context).textTheme.bodySmall),
          ],
          if (_resultAssetId != null) ...[
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: _play,
                    icon: const Icon(Icons.play_arrow),
                    label: const Text('Play'),
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: _download,
                    icon: const Icon(Icons.download),
                    label: const Text('Save'),
                  ),
                ),
              ],
            ),
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
