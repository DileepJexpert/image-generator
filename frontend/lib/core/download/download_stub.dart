import 'dart:typed_data';

/// Non-web fallback. Desktop/file-system saving can be wired up later.
void downloadBytes(Uint8List bytes, String filename, String mime) {
  throw UnsupportedError('Download is only implemented for Flutter Web');
}

/// Non-web fallback for opening a URL.
void openUrl(String url) {
  throw UnsupportedError('Opening URLs is only implemented for Flutter Web');
}
