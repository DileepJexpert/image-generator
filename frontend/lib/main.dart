import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'core/theme.dart';

void main() {
  runApp(const ProviderScope(child: KatixoStudioApp()));
}

class KatixoStudioApp extends StatelessWidget {
  const KatixoStudioApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Katixo Studio',
      debugShowCheckedModeBanner: false,
      theme: katixoTheme(),
      home: const StudioShell(),
    );
  }
}

/// Milestone 1 shell: proves the Flutter Web app boots and renders.
/// The real editor + AI panels land in later milestones.
class StudioShell extends StatelessWidget {
  const StudioShell({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Katixo Studio'),
      ),
      body: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              Icons.brush_outlined,
              size: 72,
              color: Theme.of(context).colorScheme.primary,
            ),
            const SizedBox(height: 16),
            Text(
              'Katixo Studio',
              style: Theme.of(context).textTheme.headlineMedium,
            ),
            const SizedBox(height: 8),
            const Text('Local GPU design + media studio — skeleton online.'),
          ],
        ),
      ),
    );
  }
}
