import 'dart:typed_data';

// Picks the web implementation when compiling for the browser, otherwise a stub.
export 'download_stub.dart' if (dart.library.html) 'download_web.dart';

/// Trigger a client-side download of [bytes] named [filename].
/// (Declaration kept here for documentation; the real impl is conditional.)
typedef DownloadFn = void Function(Uint8List bytes, String filename, String mime);
