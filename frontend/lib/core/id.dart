import 'dart:math';

final Random _rng = Random();

/// Compact unique id for scene elements (not a real UUID; ids only need to be
/// unique within a project's element list).
String newId() {
  final ts = DateTime.now().microsecondsSinceEpoch.toRadixString(36);
  final rnd = _rng.nextInt(1 << 32).toRadixString(36);
  return 'el_${ts}_$rnd';
}
