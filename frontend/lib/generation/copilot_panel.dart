import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../core/api_client.dart';

/// One chat turn held in the panel's local history.
class _Turn {
  _Turn(this.role, this.content);
  final String role; // 'user' | 'assistant'
  final String content;
}

/// The Copilot panel (VISION.md Phase 3): a local-LLM chat assistant for
/// prompt-writing, copy, captions, and studio help. Talks to our backend
/// (`/api/v1/copilot/chat`), never to the LLM directly.
class CopilotPanel extends ConsumerStatefulWidget {
  const CopilotPanel({super.key});

  @override
  ConsumerState<CopilotPanel> createState() => _CopilotPanelState();
}

class _CopilotPanelState extends ConsumerState<CopilotPanel> {
  final _input = TextEditingController();
  final _scroll = ScrollController();
  final List<_Turn> _turns = [];
  bool _sending = false;
  String? _error;

  @override
  void dispose() {
    _input.dispose();
    _scroll.dispose();
    super.dispose();
  }

  Future<void> _send() async {
    final text = _input.text.trim();
    if (text.isEmpty || _sending) {
      return;
    }
    setState(() {
      _turns.add(_Turn('user', text));
      _input.clear();
      _sending = true;
      _error = null;
    });
    _scrollToEnd();

    try {
      final history = [
        for (final t in _turns) {'role': t.role, 'content': t.content},
      ];
      final reply = await ref.read(apiClientProvider).copilotChat(history);
      if (!mounted) return;
      setState(() => _turns.add(_Turn('assistant', reply)));
    } catch (e) {
      if (!mounted) return;
      setState(() => _error = _describe(e));
    } finally {
      if (mounted) {
        setState(() => _sending = false);
        _scrollToEnd();
      }
    }
  }

  String _describe(Object e) {
    final s = '$e';
    if (s.contains('502') || s.contains('Connection')) {
      return 'Copilot is unreachable. Is the Ollama service running and a '
          'model pulled (e.g. `ollama pull llama3.2`)?';
    }
    return s;
  }

  void _scrollToEnd() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_scroll.hasClients) {
        _scroll.animateTo(
          _scroll.position.maxScrollExtent,
          duration: const Duration(milliseconds: 200),
          curve: Curves.easeOut,
        );
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Container(
      width: 320,
      color: scheme.surfaceContainerHighest,
      child: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
            child: Row(
              children: [
                const Icon(Icons.smart_toy_outlined, size: 20),
                const SizedBox(width: 8),
                Text('Copilot',
                    style: Theme.of(context).textTheme.titleMedium),
                const Spacer(),
                if (_turns.isNotEmpty)
                  IconButton(
                    tooltip: 'Clear chat',
                    visualDensity: VisualDensity.compact,
                    onPressed:
                        _sending ? null : () => setState(_turns.clear),
                    icon: const Icon(Icons.delete_outline, size: 18),
                  ),
              ],
            ),
          ),
          Expanded(
            child: _turns.isEmpty
                ? const _EmptyState()
                : ListView.builder(
                    controller: _scroll,
                    padding: const EdgeInsets.symmetric(horizontal: 12),
                    itemCount: _turns.length,
                    itemBuilder: (context, i) => _Bubble(turn: _turns[i]),
                  ),
          ),
          if (_error != null)
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
              child: Text(_error!,
                  style: const TextStyle(color: Colors.redAccent, fontSize: 12)),
            ),
          if (_sending)
            const Padding(
              padding: EdgeInsets.symmetric(horizontal: 16, vertical: 4),
              child: Row(
                children: [
                  SizedBox(
                      width: 14,
                      height: 14,
                      child: CircularProgressIndicator(strokeWidth: 2)),
                  SizedBox(width: 8),
                  Text('Thinking…', style: TextStyle(fontSize: 12)),
                ],
              ),
            ),
          Padding(
            padding: const EdgeInsets.all(12),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Expanded(
                  child: TextField(
                    controller: _input,
                    minLines: 1,
                    maxLines: 4,
                    textInputAction: TextInputAction.send,
                    onSubmitted: (_) => _send(),
                    decoration: const InputDecoration(
                      hintText: 'Ask for a prompt, caption, idea…',
                      border: OutlineInputBorder(),
                      isDense: true,
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                IconButton.filled(
                  onPressed: _sending ? null : _send,
                  icon: const Icon(Icons.send),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _EmptyState extends StatelessWidget {
  const _EmptyState();

  @override
  Widget build(BuildContext context) {
    final style = Theme.of(context).textTheme.bodySmall;
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.smart_toy_outlined, size: 40),
            const SizedBox(height: 12),
            Text(
              'Your local creative assistant.\n'
              'Try: "Write an image prompt for a cozy autumn café."',
              textAlign: TextAlign.center,
              style: style,
            ),
          ],
        ),
      ),
    );
  }
}

class _Bubble extends StatelessWidget {
  const _Bubble({required this.turn});
  final _Turn turn;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final isUser = turn.role == 'user';
    return Align(
      alignment: isUser ? Alignment.centerRight : Alignment.centerLeft,
      child: Container(
        margin: const EdgeInsets.symmetric(vertical: 4),
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        constraints: const BoxConstraints(maxWidth: 260),
        decoration: BoxDecoration(
          color: isUser ? scheme.primaryContainer : scheme.surface,
          borderRadius: BorderRadius.circular(12),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            SelectableText(turn.content),
            if (!isUser)
              Align(
                alignment: Alignment.centerRight,
                child: IconButton(
                  tooltip: 'Copy',
                  visualDensity: VisualDensity.compact,
                  padding: EdgeInsets.zero,
                  constraints: const BoxConstraints(),
                  onPressed: () {
                    Clipboard.setData(ClipboardData(text: turn.content));
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('Copied')),
                    );
                  },
                  icon: const Icon(Icons.copy, size: 14),
                ),
              ),
          ],
        ),
      ),
    );
  }
}
