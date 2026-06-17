import 'package:flutter/material.dart';

/// Central app theme. Dark studio look by default.
ThemeData katixoTheme() {
  return ThemeData(
    useMaterial3: true,
    colorScheme: ColorScheme.fromSeed(
      seedColor: const Color(0xFF6C5CE7),
      brightness: Brightness.dark,
    ),
  );
}
