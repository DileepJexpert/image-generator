import 'package:flutter/material.dart';

/// Parses `#RRGGBB` or `#AARRGGBB` (case-insensitive, `#` optional) into a
/// [Color]. Colors are stored as hex strings in the scene model so the save
/// format stays plain JSON.
Color hexToColor(String hex, {Color fallback = Colors.transparent}) {
  var value = hex.trim();
  if (value.startsWith('#')) {
    value = value.substring(1);
  }
  if (value.length == 6) {
    value = 'FF$value';
  }
  if (value.length != 8) {
    return fallback;
  }
  final parsed = int.tryParse(value, radix: 16);
  return parsed == null ? fallback : Color(parsed);
}

/// Serializes a [Color] back to `#AARRGGBB`.
String colorToHex(Color color) =>
    '#${color.toARGB32().toRadixString(16).padLeft(8, '0').toUpperCase()}';
