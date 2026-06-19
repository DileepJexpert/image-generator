import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../core/api_client.dart';
import '../core/download/download.dart';
import '../core/ws_client.dart';

/// One timed transcript line.
class _Segment {
  _Segment(this.start, this.end, this.text);
  final double start;
  final double end;
  final String text;
}

/// A transcript fetched from a `text` asset (the whisper sidecar's JSON).
class _Transcript {
  _Transcript(this.text, this.language, this.segments);
  final String text;
  final String language;
  final List<_Segment> segments;

  factory _Transcript.fromJson(Map<String, dynamic> json) => _Transcript(
        (json['text'] ?? '') as String,
        (json['language'] ?? '') as String,
        [
          for (final s in (json['segments'] as List? ?? []))
            _Segment(
              ((s as Map<String, dynamic>)['start'] as num?)?.toDouble() ?? 0,
              (s['end'] as num?)?.toDouble() ?? 0,
              (s['text'] ?? '') as String,
            ),
        ],
      );
}

/// The voiceover panel (VISION.md Phase 2): type text → POST /generate/speech →
/// play/save the audio → optionally transcribe it back to text + timed captions
/// (Whisper). Audio/text aren't visual canvas elements, so results are offered
/// as clips/files rather than placed on the page.
class AudioPanel extends ConsumerStatefulWidget {
  const AudioPanel({super.key});

  @override
  ConsumerState<AudioPanel> createState() => _AudioPanelState();
}

class _AudioPanelState extends ConsumerState<AudioPanel> {
  final _text = TextEditingController();

  // The one in-flight job's socket/subscription (TTS and STT run sequentially).
  JobSocket? _socket;
  StreamSubscription<JobProgress>? _sub;

  bool _busy = false;
  int _progress = 0;
  String _status = '';
  String? _error;

  String? _audioAssetId;
  _Transcript? _transcript;

  @override
  void dispose() {
    _sub?.cancel();
    _socket?.close();
    _text.dispose();
    super.dispose();
  }

  /// Runs an async job to completion, mirroring WS progress into the UI, and
  /// returns the result asset id.
  Future<String> _runJob(String jobId) {
    final completer = Completer<String>();
    final socket = JobSocket(jobId);
    _socket = socket;
    _sub = socket.progress.listen(
      (event) {
        if (mounted) {
          setState(() {
            _progress = event.progress;
            _status = event.status;
          });
        }
        if (event.isDone && event.resultAssetId != null) {
          if (!completer.isCompleted) completer.complete(event.resultAssetId);
        } else if (event.isFailed) {
          if (!completer.isCompleted) {
            completer.completeError(event.error ?? 'Job failed');
          }
        }
      },
      onError: (Object e) {
        if (!completer.isCompleted) completer.completeError(e);
      },
    );
    return completer.future.whenComplete(() {
      _sub?.cancel();
      _socket?.close();
      _sub = null;
      _socket = null;
    });
  }

  Future<void> _generate() async {
    if (_text.text.trim().isEmpty || _busy) {
      return;
    }
    setState(() {
      _busy = true;
      _progress = 0;
      _status = 'Submitting…';
      _error = null;
      _audioAssetId = null;
      _transcript = null;
    });
    try {
      final api = ref.read(apiClientProvider);
      final jobId = await api.generateSpeech(_text.text.trim());
      final assetId = await _runJob(jobId);
      if (!mounted) return;
      setState(() => _audioAssetId = assetId);
      _toast('Voiceover ready');
    } catch (e) {
      _fail('$e', 'Voiceover');
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _transcribe() async {
    final id = _audioAssetId;
    if (id == null || _busy) {
      return;
    }
    setState(() {
      _busy = true;
      _progress = 0;
      _status = 'Transcribing…';
      _error = null;
      _transcript = null;
    });
    try {
      final api = ref.read(apiClientProvider);
      final jobId = await api.transcribe(id);
      final transcriptAssetId = await _runJob(jobId);
      final bytes = await api.fetchAssetBytes(transcriptAssetId);
      final json = jsonDecode(utf8.decode(bytes)) as Map<String, dynamic>;
      if (!mounted) return;
      setState(() => _transcript = _Transcript.fromJson(json));
      _toast('Transcript ready');
    } catch (e) {
      _fail('$e', 'Transcription');
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  void _play() {
    final id = _audioAssetId;
    if (id != null) {
      openUrl(ref.read(apiClientProvider).assetUrl(id));
    }
  }

  Future<void> _downloadAudio() async {
    final id = _audioAssetId;
    if (id == null) return;
    try {
      final bytes = await ref.read(apiClientProvider).fetchAssetBytes(id);
      downloadBytes(bytes, 'voiceover.wav', 'audio/wav');
    } catch (e) {
      _toast('Download failed: $e');
    }
  }

  void _downloadCaptions() {
    final t = _transcript;
    if (t == null) return;
    downloadBytes(
      utf8.encode(_toVtt(t.segments)),
      'captions.vtt',
      'text/vtt',
    );
  }

  /// Build a WebVTT file from timed segments.
  String _toVtt(List<_Segment> segments) {
    final buf = StringBuffer('WEBVTT\n\n');
    for (final s in segments) {
      buf.writeln('${_ts(s.start)} --> ${_ts(s.end)}');
      buf.writeln(s.text.trim());
      buf.writeln();
    }
    return buf.toString();
  }

  String _ts(double seconds) {
    final ms = (seconds * 1000).round();
    final h = ms ~/ 3600000;
    final m = (ms % 3600000) ~/ 60000;
    final sec = (ms % 60000) ~/ 1000;
    final millis = ms % 1000;
    String two(int n) => n.toString().padLeft(2, '0');
    return '${two(h)}:${two(m)}:${two(sec)}.${millis.toString().padLeft(3, '0')}';
  }

  void _fail(String message, String what) {
    if (!mounted) return;
    final friendly = (message.contains('502') || message.contains('Connection'))
        ? '$what service is unreachable. Is the sidecar running?'
        : message;
    setState(() => _error = friendly);
  }

  void _toast(String msg) {
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg)));
    }
  }

  @override
  Widget build(BuildContext context) {
    final t = _transcript;
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
            onPressed: _busy ? null : _generate,
            icon: const Icon(Icons.record_voice_over),
            label: Text(_busy ? 'Working…' : 'Generate voiceover'),
          ),
          if (_busy) ...[
            const SizedBox(height: 16),
            LinearProgressIndicator(value: _progress > 0 ? _progress / 100 : null),
            const SizedBox(height: 6),
            Text('$_status  ${_progress > 0 ? '$_progress%' : ''}',
                style: Theme.of(context).textTheme.bodySmall),
          ],
          if (_audioAssetId != null) ...[
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: _busy ? null : _play,
                    icon: const Icon(Icons.play_arrow),
                    label: const Text('Play'),
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: _busy ? null : _downloadAudio,
                    icon: const Icon(Icons.download),
                    label: const Text('Save'),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            TextButton.icon(
              onPressed: _busy ? null : _transcribe,
              icon: const Icon(Icons.subtitles_outlined),
              label: const Text('Transcribe to captions'),
            ),
          ],
          if (t != null) ...[
            const Divider(height: 24),
            Row(
              children: [
                Expanded(
                  child: Text('Transcript (${t.language})',
                      style: Theme.of(context).textTheme.titleSmall),
                ),
                IconButton(
                  tooltip: 'Download .vtt',
                  visualDensity: VisualDensity.compact,
                  onPressed: _downloadCaptions,
                  icon: const Icon(Icons.download, size: 18),
                ),
              ],
            ),
            const SizedBox(height: 8),
            SelectableText(t.text.isEmpty ? '(no speech detected)' : t.text),
            if (t.segments.isNotEmpty) ...[
              const SizedBox(height: 12),
              for (final s in t.segments)
                Padding(
                  padding: const EdgeInsets.symmetric(vertical: 2),
                  child: Text('[${_ts(s.start)}] ${s.text.trim()}',
                      style: Theme.of(context).textTheme.bodySmall),
                ),
            ],
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
