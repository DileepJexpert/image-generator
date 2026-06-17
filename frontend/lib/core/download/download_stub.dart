import 'dart:typed_data';

/// Non-web fallback. Desktop/file-system saving can be wired up later.
void downloadBytes(Uint8List bytes, String filename, String mime) {
  throw UnsupportedError('Download is only implemented for Flutter Web');
}
