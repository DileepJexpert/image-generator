import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../core/api_client.dart';
import '../core/ws_client.dart';
import 'canvas_placement.dart';

/// One chat turn held in the panel's local history. Assistant turns may also
/// carry the agent's tool [steps] and any [pending] approval-gated actions.
class _Turn {
  _Turn(this.role, this.content,
      {this.steps = const [], List<PendingAction>? pending})
      : pending = pending ?? [];

  final String role; // 'user' | 'assistant'
  final String content;
  final List<AgentStep> steps;
  final List<PendingAction> pending; // mutable; entries drop as confirmed
}

/// The Copilot panel (VISION.md Phase 3). Two modes:
/// * **Chat** — a local-LLM assistant for prompts, copy, and studio help.
/// * **Agent** — a tool-calling loop that can act in the studio (generate,
///   edit, find leads), with approval gates on anything that reaches outside.
/// Talks only to our backend (CLAUDE.md prime directive #3), never the LLM.
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
  bool _agentMode = true;
  String _streaming = ''; // assistant text accumulating during a chat stream
  String? _error;

  // Jobs started by the agent's tools, tracked to completion so their results
  // land on the canvas. Keyed by jobId -> live status line.
  final Map<String, String> _activeJobs = {};
  final List<JobSocket> _jobSockets = [];
  final List<StreamSubscription<JobProgress>> _jobSubs = [];

  @override
  void dispose() {
    for (final sub in _jobSubs) {
      sub.cancel();
    }
    for (final socket in _jobSockets) {
      socket.close();
    }
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
      _streaming = '';
      _error = null;
    });
    _scrollToEnd();

    final history = [
      for (final t in _turns) {'role': t.role, 'content': t.content},
    ];

    try {
      if (_agentMode) {
        await _runAgent(history);
      } else {
        await _runChat(history);
      }
    } catch (e) {
      if (!mounted) return;
      setState(() => _error = _describe(e));
    } finally {
      if (mounted) {
        setState(() {
          _sending = false;
          _streaming = '';
        });
        _scrollToEnd();
      }
    }
  }

  Future<void> _runChat(List<Map<String, String>> history) async {
    final reply = await ref.read(apiClientProvider).copilotChatStream(
      history,
      onToken: (chunk) {
        if (!mounted) return;
        setState(() => _streaming += chunk);
        _scrollToEnd();
      },
    );
    if (!mounted) return;
    setState(() => _turns.add(_Turn('assistant', reply)));
  }

  Future<void> _runAgent(List<Map<String, String>> history) async {
    final reply = await ref.read(apiClientProvider).copilotAgent(history);
    if (!mounted) return;
    setState(() => _turns.add(_Turn(
          'assistant',
          reply.message,
          steps: reply.steps,
          pending: reply.pendingActions,
        )));
    for (final step in reply.steps) {
      if (step.jobId != null) {
        _trackJob(step.jobId!, _jobLabel(step.tool));
      }
    }
  }

  /// Runs a user-confirmed approval-gated action, then swaps its card for a
  /// completed step chip on the owning turn.
  Future<void> _confirm(_Turn turn, PendingAction action) async {
    try {
      final step = await ref
          .read(apiClientProvider)
          .copilotAgentConfirm(action.tool, action.args);
      if (!mounted) return;
      setState(() {
        turn.pending.remove(action);
        turn.steps.add(step);
      });
      if (step.jobId != null) {
        _trackJob(step.jobId!, _jobLabel(step.tool));
      }
    } catch (e) {
      if (!mounted) return;
      setState(() => _error = _describe(e));
    }
  }

  /// Tracks a tool-started job to completion and places its result on the
  /// canvas, surfacing live progress in the panel meanwhile.
  void _trackJob(String jobId, String label) {
    final socket = JobSocket(jobId);
    _jobSockets.add(socket);
    setState(() => _activeJobs[jobId] = '$label…');

    late final StreamSubscription<JobProgress> sub;
    sub = socket.progress.listen(
      (event) async {
        if (!mounted) return;
        setState(() => _activeJobs[jobId] =
            '$label… ${event.progress > 0 ? '${event.progress}%' : ''}');
        if (event.isDone && event.resultAssetId != null) {
          final msg = await placeJobResultOnCanvas(ref,
              assetId: event.resultAssetId!, jobType: event.type);
          if (msg != null) _toast(msg);
          _endJob(jobId, socket, sub);
        } else if (event.isFailed) {
          _toast('$label failed: ${event.error ?? ''}');
          _endJob(jobId, socket, sub);
        }
      },
      onError: (Object e) {
        _toast('$label error: $e');
        _endJob(jobId, socket, sub);
      },
    );
    _jobSubs.add(sub);
  }

  void _endJob(String jobId, JobSocket socket, StreamSubscription<JobProgress> sub) {
    sub.cancel();
    socket.close();
    _jobSubs.remove(sub);
    _jobSockets.remove(socket);
    if (mounted) {
      setState(() => _activeJobs.remove(jobId));
    }
  }

  void _toast(String msg) {
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg)));
    }
  }

  /// Friendly label for a tool's background job.
  String _jobLabel(String tool) => switch (tool) {
        'generate_image' => 'Generating image',
        'remove_background' => 'Removing background',
        'upscale_image' => 'Upscaling',
        'image_to_video' => 'Rendering video',
        'scrape_leads' => 'Finding leads',
        _ => 'Working',
      };

  String _describe(Object e) {
    final s = '$e';
    if (s.contains('502') || s.contains('Connection')) {
      return 'Copilot is unreachable. Is the Ollama service running and a '
          'tool-capable model pulled (e.g. `ollama pull qwen3`)?';
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
            padding: const EdgeInsets.fromLTRB(16, 16, 16, 4),
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
                    onPressed: _sending ? null : () => setState(_turns.clear),
                    icon: const Icon(Icons.delete_outline, size: 18),
                  ),
              ],
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
            child: SegmentedButton<bool>(
              showSelectedIcon: false,
              style: const ButtonStyle(
                visualDensity: VisualDensity.compact,
                tapTargetSize: MaterialTapTargetSize.shrinkWrap,
              ),
              segments: const [
                ButtonSegment(value: true, label: Text('Agent')),
                ButtonSegment(value: false, label: Text('Chat')),
              ],
              selected: {_agentMode},
              onSelectionChanged: _sending
                  ? null
                  : (s) => setState(() => _agentMode = s.first),
            ),
          ),
          Expanded(
            child: _turns.isEmpty
                ? _EmptyState(agentMode: _agentMode)
                : Builder(builder: (context) {
                    final showStreaming = _sending && _streaming.isNotEmpty;
                    return ListView.builder(
                      controller: _scroll,
                      padding: const EdgeInsets.symmetric(horizontal: 12),
                      itemCount: _turns.length + (showStreaming ? 1 : 0),
                      itemBuilder: (context, i) {
                        if (i >= _turns.length) {
                          return _Bubble(turn: _Turn('assistant', _streaming));
                        }
                        final turn = _turns[i];
                        return _Bubble(
                          turn: turn,
                          onConfirm: (a) => _confirm(turn, a),
                        );
                      },
                    );
                  }),
          ),
          if (_activeJobs.isNotEmpty)
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 4, 16, 0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  for (final status in _activeJobs.values)
                    Row(
                      children: [
                        const SizedBox(
                            width: 12,
                            height: 12,
                            child: CircularProgressIndicator(strokeWidth: 2)),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Text(status,
                              style: const TextStyle(fontSize: 12)),
                        ),
                      ],
                    ),
                ],
              ),
            ),
          if (_error != null)
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
              child: Text(_error!,
                  style: const TextStyle(color: Colors.redAccent, fontSize: 12)),
            ),
          if (_sending && _streaming.isEmpty)
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
              child: Row(
                children: [
                  const SizedBox(
                      width: 14,
                      height: 14,
                      child: CircularProgressIndicator(strokeWidth: 2)),
                  const SizedBox(width: 8),
                  Text(_agentMode ? 'Working…' : 'Thinking…',
                      style: const TextStyle(fontSize: 12)),
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
                    decoration: InputDecoration(
                      hintText: _agentMode
                          ? 'Ask me to generate, edit, find leads…'
                          : 'Ask for a prompt, caption, idea…',
                      border: const OutlineInputBorder(),
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
  const _EmptyState({required this.agentMode});
  final bool agentMode;

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
              agentMode
                  ? 'Your local studio agent.\n'
                      'Try: "Generate a 1024×1024 cozy autumn café image."'
                  : 'Your local creative assistant.\n'
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
  const _Bubble({required this.turn, this.onConfirm});
  final _Turn turn;
  final void Function(PendingAction action)? onConfirm;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final isUser = turn.role == 'user';
    return Align(
      alignment: isUser ? Alignment.centerRight : Alignment.centerLeft,
      child: Container(
        margin: const EdgeInsets.symmetric(vertical: 4),
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        constraints: const BoxConstraints(maxWidth: 280),
        decoration: BoxDecoration(
          color: isUser ? scheme.primaryContainer : scheme.surface,
          borderRadius: BorderRadius.circular(12),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            for (final step in turn.steps) _StepRow(step: step),
            if (turn.steps.isNotEmpty && turn.content.isNotEmpty)
              const SizedBox(height: 6),
            if (turn.content.isNotEmpty) SelectableText(turn.content),
            for (final action in turn.pending)
              _ApprovalCard(
                action: action,
                onConfirm:
                    onConfirm == null ? null : () => onConfirm!(action),
              ),
            if (!isUser && turn.content.isNotEmpty)
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

/// A single executed/failed tool step, shown as a compact icon + summary row.
class _StepRow extends StatelessWidget {
  const _StepRow({required this.step});
  final AgentStep step;

  @override
  Widget build(BuildContext context) {
    final (icon, color) = switch (step.status) {
      'failed' => (Icons.error_outline, Colors.redAccent),
      'pending_approval' => (Icons.schedule, Colors.orangeAccent),
      _ => (Icons.check_circle_outline, Colors.green),
    };
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, size: 14, color: color),
          const SizedBox(width: 6),
          Expanded(
            child: Text(
              step.summary.isEmpty ? step.tool : step.summary,
              style: const TextStyle(fontSize: 12),
            ),
          ),
        ],
      ),
    );
  }
}

/// An approval gate for an outbound/irreversible action the agent proposed.
class _ApprovalCard extends StatelessWidget {
  const _ApprovalCard({required this.action, this.onConfirm});
  final PendingAction action;
  final VoidCallback? onConfirm;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Container(
      margin: const EdgeInsets.only(top: 8),
      padding: const EdgeInsets.all(8),
      decoration: BoxDecoration(
        border: Border.all(color: scheme.outlineVariant),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Icons.shield_outlined, size: 14),
              const SizedBox(width: 6),
              Expanded(
                child: Text('Needs your approval',
                    style: Theme.of(context).textTheme.labelMedium),
              ),
            ],
          ),
          const SizedBox(height: 4),
          Text(action.label, style: const TextStyle(fontSize: 12)),
          const SizedBox(height: 8),
          Align(
            alignment: Alignment.centerRight,
            child: FilledButton.tonalIcon(
              onPressed: onConfirm,
              style: FilledButton.styleFrom(
                visualDensity: VisualDensity.compact,
              ),
              icon: const Icon(Icons.play_arrow, size: 16),
              label: const Text('Run'),
            ),
          ),
        ],
      ),
    );
  }
}
