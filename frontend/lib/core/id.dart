import 'dart:math';

final Random _rng = Random();

/// Compact unique id for scene elements (not a real UUID; ids only need to be
/// unique within a project's element list).
String newId() {
  final ts = DateTime.now().microsecondsSinceEpoch.toRadixString(36);
  // Web-safe bound: on Flutter Web bit-shifts are 32-bit, so `1 << 32` overflows
  // to 0 and `nextInt(0)` throws RangeError. 2^30 of randomness on top of the
  // microsecond timestamp is plenty to stay unique within a project's elements.
  final rnd = _rng.nextInt(0x40000000).toRadixString(36); // 2^30
  return 'el_${ts}_$rnd';
}
