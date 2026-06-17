import 'dart:ui' as ui;

import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/api_client.dart';

/// Decoded-image cache keyed by assetId (CLAUDE.md §7). The painter reads
/// ready [ui.Image]s from this map; missing ones are fetched + decoded
/// asynchronously, and the map update triggers a repaint.
final imageCacheProvider =
    NotifierProvider<ImageCacheController, Map<String, ui.Image>>(
        ImageCacheController.new);

class ImageCacheController extends Notifier<Map<String, ui.Image>> {
  final Set<String> _loading = {};

  @override
  Map<String, ui.Image> build() => const {};

  /// Ensure the image for [assetId] is (being) loaded. Safe to call repeatedly.
  void ensure(String assetId) {
    if (state.containsKey(assetId) || _loading.contains(assetId)) {
      return;
    }
    _loading.add(assetId);
    _load(assetId);
  }

  Future<void> _load(String assetId) async {
    try {
      final bytes = await ref.read(apiClientProvider).fetchAssetBytes(assetId);
      final codec = await ui.instantiateImageCodec(bytes);
      final frame = await codec.getNextFrame();
      state = {...state, assetId: frame.image};
    } catch (e) {
      debugPrint('Failed to load asset $assetId: $e');
    } finally {
      _loading.remove(assetId);
    }
  }
}
