import 'dart:convert';

import 'package:web_socket_channel/web_socket_channel.dart';

import 'api_client.dart';

/// A job progress event mirrored from `/ws/jobs/{jobId}` (CLAUDE.md §6/§9).
class JobProgress {
  JobProgress({
    required this.id,
    required this.type,
    required this.status,
    required this.progress,
    this.resultAssetId,
    this.error,
  });

  final String id;
  final String type;
  final String status;
  final int progress;
  final String? resultAssetId;
  final String? error;

  bool get isDone => status == 'done';
  bool get isFailed => status == 'failed';
  bool get isTerminal => isDone || isFailed;

  factory JobProgress.fromJson(Map<String, dynamic> json) => JobProgress(
        id: json['id'] as String,
        type: json['type'] as String,
        status: json['status'] as String,
        progress: (json['progress'] as num?)?.toInt() ?? 0,
        resultAssetId: json['resultAssetId'] as String?,
        error: json['error'] as String?,
      );
}

/// Subscribes to a job's progress stream over WebSocket.
class JobSocket {
  JobSocket(String jobId)
      : _channel = WebSocketChannel.connect(_jobWsUri(jobId));

  final WebSocketChannel _channel;

  Stream<JobProgress> get progress => _channel.stream.map((event) =>
      JobProgress.fromJson(jsonDecode(event as String) as Map<String, dynamic>));

  Future<void> close() => _channel.sink.close();

  static Uri _jobWsUri(String jobId) {
    // Use the configured dev origin when set, otherwise the page origin.
    final origin = kApiOrigin.isNotEmpty ? Uri.parse(kApiOrigin) : Uri.base;
    final scheme = origin.scheme == 'https' ? 'wss' : 'ws';
    return Uri(
      scheme: scheme,
      host: origin.host,
      port: origin.hasPort ? origin.port : null,
      path: '/ws/jobs/$jobId',
    );
  }
}
