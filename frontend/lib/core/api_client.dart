import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../editor/model/project.dart';

/// Base path for the internal API. The Flutter app only ever talks to our
/// backend (CLAUDE.md prime directive #3); nginx proxies /api to the monolith.
const String kApiBase = '/api/v1';

/// Optional absolute backend origin for local dev (e.g. when running
/// `flutter run` against a backend on another port). Set with
/// `--dart-define=API_ORIGIN=http://localhost:8080`. Empty = same origin.
const String kApiOrigin = String.fromEnvironment('API_ORIGIN');

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
  /// when the project has never been saved.
  Future<Project> getProject(String id) async {
    final res = await _dio.get<Map<String, dynamic>>('/projects/$id');
    final data = res.data!;
    final scene = data['sceneJson'] as Map<String, dynamic>?;
    if (scene != null) {
      return Project.fromJson(scene);
    }
    return Project(
      id: data['id'] as String,
      name: data['name'] as String,
      canvasWidth: (data['canvasWidth'] as num).toInt(),
      canvasHeight: (data['canvasHeight'] as num).toInt(),
    );
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
  String assetUrl(String assetId) => '$kApiBase/assets/$assetId';

  /// Fetches raw asset bytes (for decoding into the canvas image cache).
  Future<Uint8List> fetchAssetBytes(String assetId) async {
    final res = await _dio.get<List<int>>(
      '/assets/$assetId',
      options: Options(responseType: ResponseType.bytes),
    );
    return Uint8List.fromList(res.data!);
  }
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
