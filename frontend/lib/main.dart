import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'core/router.dart';
import 'core/theme.dart';

void main() {
  runApp(const ProviderScope(child: KatixoStudioApp()));
}

class KatixoStudioApp extends StatelessWidget {
  const KatixoStudioApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp.router(
      title: 'Katixo Studio',
      debugShowCheckedModeBanner: false,
      theme: katixoTheme(),
      routerConfig: appRouter,
    );
  }
}
