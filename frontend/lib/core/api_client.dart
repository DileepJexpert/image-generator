import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart' show kDebugMode, kIsWeb;
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../editor/model/project.dart';

/// Base path for the internal API. The Flutter app only ever talks to our
/// backend (CLAUDE.md prime directive #3); nginx proxies /api to the monolith.
const String kApiBase = '/api/v1';

/// Explicit backend origin from `--dart-define=API_ORIGIN=http://host:port`.
const String _apiOriginDefine = String.fromEnvironment('API_ORIGIN');

/// Absolute backend origin, or empty for same-origin.
///
/// Resolution order:
/// 1. An explicit `--dart-define=API_ORIGIN` always wins.
/// 2. In debug (`flutter run`) default to the local backend on :8585, so dev
///    "just works" without remembering the define.
/// 3. Otherwise empty = same origin (production: nginx proxies /api + /ws).
String get kApiOrigin {
  if (_apiOriginDefine.isNotEmpty) return _apiOriginDefine;
  if (kDebugMode) return 'http://localhost:8585';
  return '';
}

final apiClientProvider = Provider<ApiClient>((ref) => ApiClient());

/// Thin wrapper over the internal REST API (projects + assets + generation).
class ApiClient {
  ApiClient({Dio? dio})
      : _dio = dio ??
            Dio(BaseOptions(
              baseUrl: '$kApiOrigin$kApiBase',
              connectTimeout: const Duration(seconds: 15),
              receiveTimeout: const Duration(seconds: 60),
            ));

  final Dio _dio;

  // --- Projects ------------------------------------------------------------

  Future<List<ProjectSummary>> listProjects() async {
    final res = await _dio.get<List<dynamic>>('/projects');
    return (res.data ?? [])
        .map((e) => ProjectSummary.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<ProjectSummary> createProject(
      String name, int canvasWidth, int canvasHeight) async {
    final res = await _dio.post<Map<String, dynamic>>('/projects', data: {
      'name': name,
      'canvasWidth': canvasWidth,
      'canvasHeight': canvasHeight,
    });
    return ProjectSummary.fromJson(res.data!);
  }

  /// Loads a project, returning the scene model. Falls back to an empty scene
  /// when the project has never been saved, and upgrades older single-page
  /// saves (top-level `elements`) into the multi-page shape.
  Future<Project> getProject(String id) async {
    final res = await _dio.get<Map<String, dynamic>>('/projects/$id');
    final data = res.data!;
    final scene = data['sceneJson'] as Map<String, dynamic>?;
    if (scene != null) {
      final normalized = Map<String, dynamic>.of(scene);
      if (normalized['pages'] == null && normalized['elements'] != null) {
        normalized['pages'] = [
          {'id': '${scene['id'] ?? id}_p0', 'elements': normalized.remove('elements')},
        ];
      }
      return Project.fromJson(normalized).ensureHasPage();
    }
    return Project(
      id: data['id'] as String,
      name: data['name'] as String,
      canvasWidth: (data['canvasWidth'] as num).toInt(),
      canvasHeight: (data['canvasHeight'] as num).toInt(),
    ).ensureHasPage();
  }

  /// Autosave target: PUT the full scene.
  Future<void> saveProject(Project project) async {
    await _dio.put('/projects/${project.id}', data: {
      'name': project.name,
      'sceneJson': project.toJson(),
    });
  }

  Future<void> deleteProject(String id) async {
    await _dio.delete('/projects/$id');
  }

  // --- Generation ----------------------------------------------------------

  /// Submits an image generation job; returns the async job id (CLAUDE.md §6).
  Future<String> generateImage({
    required String prompt,
    String? negativePrompt,
    required int width,
    required int height,
    required String model,
    int? seed,
  }) async {
    final res = await _dio.post<Map<String, dynamic>>('/generate/image', data: {
      'prompt': prompt,
      if (negativePrompt != null && negativePrompt.isNotEmpty)
        'negativePrompt': negativePrompt,
      'width': width,
      'height': height,
      'model': model,
      if (seed != null) 'seed': seed,
    });
    return res.data!['jobId'] as String;
  }

  // --- Edits (return a jobId) ----------------------------------------------

  Future<String> removeBackground(String assetId) async {
    final res = await _dio.post<Map<String, dynamic>>('/edit/remove-bg',
        data: {'assetId': assetId});
    return res.data!['jobId'] as String;
  }

  Future<String> upscale(String assetId, int scale) async {
    final res = await _dio.post<Map<String, dynamic>>('/edit/upscale',
        data: {'assetId': assetId, 'scale': scale});
    return res.data!['jobId'] as String;
  }

  /// Image-to-video: animate a source image into a short clip.
  Future<String> imageToVideo(
      String sourceAssetId, String prompt, int durationSeconds) async {
    final res =
        await _dio.post<Map<String, dynamic>>('/generate/image-to-video', data: {
      'sourceAssetId': sourceAssetId,
      if (prompt.isNotEmpty) 'prompt': prompt,
      'durationSeconds': durationSeconds,
    });
    return res.data!['jobId'] as String;
  }

  // --- Audio (text-to-speech voiceover) ------------------------------------

  /// Submits a text-to-speech job; returns the async job id. The result is an
  /// `audio` asset (WAV).
  Future<String> generateSpeech(String text, {String? voice}) async {
    final res = await _dio.post<Map<String, dynamic>>('/generate/speech', data: {
      'text': text,
      if (voice != null && voice.isNotEmpty) 'voice': voice,
    });
    return res.data!['jobId'] as String;
  }

  /// Submits a transcription job for an existing audio asset; returns the job
  /// id. The result is a `text` asset holding the transcript JSON.
  Future<String> transcribe(String assetId) async {
    final res = await _dio.post<Map<String, dynamic>>('/transcribe',
        data: {'assetId': assetId});
    return res.data!['jobId'] as String;
  }

  // --- Copilot (local LLM chat) --------------------------------------------

  /// Sends the conversation so far to the Copilot and returns the assistant's
  /// reply. The backend is stateless, so [messages] carries the full history as
  /// `{role, content}` maps (roles: `user` / `assistant`).
  Future<String> copilotChat(
    List<Map<String, String>> messages, {
    String? model,
  }) async {
    final res = await _dio.post<Map<String, dynamic>>(
      '/copilot/chat',
      data: {
        'messages': messages,
        if (model != null && model.isNotEmpty) 'model': model,
      },
      // Local LLMs on CPU can take a while; override the default receive timeout.
      options: Options(receiveTimeout: const Duration(minutes: 5)),
    );
    final message = res.data!['message'] as Map<String, dynamic>;
    return message['content'] as String;
  }

  /// Streams the Copilot reply, calling [onToken] with each chunk as it
  /// arrives, and returns the full reply when done.
  ///
  /// On native/desktop this consumes the backend's SSE endpoint incrementally.
  /// On web, dio can't expose a response body as a stream (XHR), so we fall
  /// back to the buffered call and deliver the whole reply as one chunk — same
  /// result, just not progressive.
  Future<String> copilotChatStream(
    List<Map<String, String>> messages, {
    String? model,
    required void Function(String chunk) onToken,
  }) async {
    if (kIsWeb) {
      final reply = await copilotChat(messages, model: model);
      onToken(reply);
      return reply;
    }

    final res = await _dio.post<ResponseBody>(
      '/copilot/chat/stream',
      data: {
        'messages': messages,
        if (model != null && model.isNotEmpty) 'model': model,
      },
      options: Options(
        responseType: ResponseType.stream,
        receiveTimeout: const Duration(minutes: 5),
        headers: {'Accept': 'text/event-stream'},
      ),
    );

    final full = StringBuffer();
    final lines = res.data!.stream
        .cast<List<int>>()
        .transform(utf8.decoder)
        .transform(const LineSplitter());
    await for (final line in lines) {
      if (!line.startsWith('data:')) {
        continue; // skip SSE `event:` / comment / blank lines
      }
      final payload = line.substring(5).trim();
      if (payload.isEmpty) {
        continue;
      }
      try {
        final token = (jsonDecode(payload) as Map<String, dynamic>)['token'];
        if (token is String && token.isNotEmpty) {
          full.write(token);
          onToken(token);
        }
      } catch (_) {
        // The terminal `done` event carries `{}` (no token) — ignore.
      }
    }
    return full.toString();
  }

  // --- Copilot agent (tool-calling loop) -----------------------------------

  /// Runs one agent turn. The backend may call tools (generation/edit/leads/
  /// project reads) and returns the final reply plus the tool steps it ran and
  /// any approval-gated actions awaiting the user's confirmation.
  Future<AgentReply> copilotAgent(
    List<Map<String, String>> messages, {
    String? model,
  }) async {
    final res = await _dio.post<Map<String, dynamic>>(
      '/copilot/agent',
      data: {
        'messages': messages,
        if (model != null && model.isNotEmpty) 'model': model,
      },
      options: Options(receiveTimeout: const Duration(minutes: 5)),
    );
    return AgentReply.fromJson(res.data!);
  }

  /// Executes an approval-gated action the user confirmed; returns the step
  /// (including any `jobId` to track over the job WebSocket).
  Future<AgentStep> copilotAgentConfirm(
      String tool, Map<String, dynamic> args) async {
    final res = await _dio.post<Map<String, dynamic>>(
      '/copilot/agent/confirm',
      data: {'tool': tool, 'args': args},
    );
    return AgentStep.fromJson(res.data!);
  }

  /// Lists models available in the local Ollama instance (names only).
  Future<List<String>> copilotModels() async {
    final res = await _dio.get<List<dynamic>>('/copilot/models');
    return (res.data ?? [])
        .map((e) => (e as Map<String, dynamic>)['name'] as String)
        .toList();
  }

  // --- Leads ---------------------------------------------------------------

  /// Starts a lead-generation scrape over the given site domains/URLs and
  /// returns the job id. The result is a `text` asset holding the leads JSON.
  Future<String> scrapeLeads(
    List<String> targets, {
    String? offering,
    int? maxPagesPerSite,
    int? maxLeads,
  }) async {
    final res = await _dio.post<Map<String, dynamic>>(
      '/leads/scrape',
      data: {
        'targets': targets,
        if (offering != null && offering.isNotEmpty) 'offering': offering,
        if (maxPagesPerSite != null) 'maxPagesPerSite': maxPagesPerSite,
        if (maxLeads != null) 'maxLeads': maxLeads,
      },
    );
    return res.data!['jobId'] as String;
  }

  // --- Assets --------------------------------------------------------------

  /// Uploads image bytes and returns the new asset id.
  Future<String> uploadAsset(Uint8List bytes, String filename) async {
    final form = FormData.fromMap({
      'file': MultipartFile.fromBytes(bytes, filename: filename),
    });
    final res = await _dio.post<Map<String, dynamic>>('/assets', data: form);
    return res.data!['id'] as String;
  }

  /// URL that streams an asset's bytes (used by the image cache + <img>).
  String assetUrl(String assetId) => '$kApiOrigin$kApiBase/assets/$assetId';

  /// Fetches raw asset bytes (for decoding into the canvas image cache).
  Future<Uint8List> fetchAssetBytes(String assetId) async {
    final res = await _dio.get<List<int>>(
      '/assets/$assetId',
      options: Options(responseType: ResponseType.bytes),
    );
    return Uint8List.fromList(res.data!);
  }
}

/// One tool the agent ran (or proposed), mirroring the backend `ToolStep`.
class AgentStep {
  AgentStep({
    required this.tool,
    required this.status,
    required this.summary,
    this.jobId,
    this.args = const {},
  });

  final String tool;
  final String status; // done | failed | pending_approval
  final String summary;
  final String? jobId;
  final Map<String, dynamic> args;

  factory AgentStep.fromJson(Map<String, dynamic> json) => AgentStep(
        tool: json['tool'] as String? ?? 'tool',
        status: json['status'] as String? ?? 'done',
        summary: json['summary'] as String? ?? '',
        jobId: json['jobId'] as String?,
        args: (json['args'] as Map<String, dynamic>?) ?? const {},
      );
}

/// An approval-gated action the user must confirm before it runs.
class PendingAction {
  PendingAction({required this.tool, required this.label, required this.args});

  final String tool;
  final String label;
  final Map<String, dynamic> args;

  factory PendingAction.fromJson(Map<String, dynamic> json) => PendingAction(
        tool: json['tool'] as String,
        label: json['label'] as String? ?? 'Run ${json['tool']}',
        args: (json['args'] as Map<String, dynamic>?) ?? const {},
      );
}

/// The result of an agent turn: the reply text, executed steps, and any
/// actions awaiting confirmation.
class AgentReply {
  AgentReply({
    required this.message,
    required this.steps,
    required this.pendingActions,
  });

  final String message;
  final List<AgentStep> steps;
  final List<PendingAction> pendingActions;

  factory AgentReply.fromJson(Map<String, dynamic> json) => AgentReply(
        message:
            (json['message'] as Map<String, dynamic>?)?['content'] as String? ??
                '',
        steps: ((json['steps'] as List<dynamic>?) ?? [])
            .map((e) => AgentStep.fromJson(e as Map<String, dynamic>))
            .toList(),
        pendingActions: ((json['pendingActions'] as List<dynamic>?) ?? [])
            .map((e) => PendingAction.fromJson(e as Map<String, dynamic>))
            .toList(),
      );
}

/// Minimal list/summary view of a project (matches the backend list payload).
class ProjectSummary {
  ProjectSummary({
    required this.id,
    required this.name,
    required this.canvasWidth,
    required this.canvasHeight,
    this.updatedAt,
  });

  final String id;
  final String name;
  final int canvasWidth;
  final int canvasHeight;
  final DateTime? updatedAt;

  factory ProjectSummary.fromJson(Map<String, dynamic> json) => ProjectSummary(
        id: json['id'] as String,
        name: json['name'] as String,
        canvasWidth: (json['canvasWidth'] as num).toInt(),
        canvasHeight: (json['canvasHeight'] as num).toInt(),
        updatedAt: json['updatedAt'] != null
            ? DateTime.tryParse(json['updatedAt'] as String)
            : null,
      );
}
