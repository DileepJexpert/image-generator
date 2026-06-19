import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../core/api_client.dart';
import '../core/download/download.dart';
import '../core/ws_client.dart';

/// One scraped lead.
class _Lead {
  _Lead(this.company, this.sourceUrl, this.email, this.phone, this.socials,
      this.outreach);
  final String company;
  final String sourceUrl;
  final String? email;
  final String? phone;
  final List<String> socials;
  final String? outreach;

  factory _Lead.fromJson(Map<String, dynamic> j) => _Lead(
        (j['company'] ?? '') as String,
        (j['sourceUrl'] ?? '') as String,
        j['email'] as String?,
        j['phone'] as String?,
        [for (final s in (j['socialLinks'] as List? ?? [])) s as String],
        j['outreach'] as String?,
      );
}

/// The Leads panel: point it at one or more public sites, and it crawls them
/// (honoring robots.txt), extracts contact leads, and drafts outreach copy via
/// the local Copilot. Results are shown here and exportable as CSV.
class LeadsPanel extends ConsumerStatefulWidget {
  const LeadsPanel({super.key});

  @override
  ConsumerState<LeadsPanel> createState() => _LeadsPanelState();
}

class _LeadsPanelState extends ConsumerState<LeadsPanel> {
  final _targets = TextEditingController();
  final _offering = TextEditingController();

  JobSocket? _socket;
  StreamSubscription<JobProgress>? _sub;

  bool _busy = false;
  int _progress = 0;
  String _status = '';
  String? _error;
  List<_Lead> _leads = [];

  @override
  void dispose() {
    _sub?.cancel();
    _socket?.close();
    _targets.dispose();
    _offering.dispose();
    super.dispose();
  }

  Future<String> _runJob(String jobId) {
    final completer = Completer<String>();
    final socket = JobSocket(jobId);
    _socket = socket;
    _sub = socket.progress.listen(
      (event) {
        if (mounted) {
          setState(() {
            _progress = event.progress;
            _status = event.status;
          });
        }
        if (event.isDone && event.resultAssetId != null) {
          if (!completer.isCompleted) completer.complete(event.resultAssetId);
        } else if (event.isFailed) {
          if (!completer.isCompleted) {
            completer.completeError(event.error ?? 'Job failed');
          }
        }
      },
      onError: (Object e) {
        if (!completer.isCompleted) completer.completeError(e);
      },
    );
    return completer.future.whenComplete(() {
      _sub?.cancel();
      _socket?.close();
      _sub = null;
      _socket = null;
    });
  }

  Future<void> _generate() async {
    final targets = _targets.text
        .split(RegExp(r'[\n,]'))
        .map((s) => s.trim())
        .where((s) => s.isNotEmpty)
        .toList();
    if (targets.isEmpty || _busy) {
      return;
    }
    setState(() {
      _busy = true;
      _progress = 0;
      _status = 'Submitting…';
      _error = null;
      _leads = [];
    });
    try {
      final api = ref.read(apiClientProvider);
      final jobId = await api.scrapeLeads(targets, offering: _offering.text.trim());
      final assetId = await _runJob(jobId);
      final bytes = await api.fetchAssetBytes(assetId);
      final json = jsonDecode(utf8.decode(bytes)) as Map<String, dynamic>;
      if (!mounted) return;
      setState(() {
        _leads = [
          for (final l in (json['leads'] as List? ?? []))
            _Lead.fromJson(l as Map<String, dynamic>),
        ];
      });
      _toast('${_leads.length} lead(s) found');
    } catch (e) {
      _fail('$e');
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  void _downloadCsv() {
    if (_leads.isEmpty) return;
    final rows = <List<String>>[
      ['Company', 'Email', 'Phone', 'Socials', 'Source', 'Outreach'],
      for (final l in _leads)
        [
          l.company,
          l.email ?? '',
          l.phone ?? '',
          l.socials.join(' '),
          l.sourceUrl,
          l.outreach ?? '',
        ],
    ];
    final csv = rows.map((r) => r.map(_csvCell).join(',')).join('\r\n');
    downloadBytes(utf8.encode(csv), 'leads.csv', 'text/csv');
  }

  String _csvCell(String value) {
    final needsQuote = value.contains(RegExp(r'[",\r\n]'));
    final escaped = value.replaceAll('"', '""');
    return needsQuote ? '"$escaped"' : escaped;
  }

  void _fail(String message) {
    if (!mounted) return;
    final friendly = (message.contains('502') || message.contains('Connection'))
        ? 'Scrape failed — the backend could not reach the target site(s).'
        : message;
    setState(() => _error = friendly);
  }

  void _toast(String msg) {
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg)));
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 320,
      color: Theme.of(context).colorScheme.surfaceContainerHighest,
      padding: const EdgeInsets.all(16),
      child: ListView(
        children: [
          Row(
            children: [
              const Icon(Icons.travel_explore, size: 20),
              const SizedBox(width: 8),
              Text('Leads', style: Theme.of(context).textTheme.titleMedium),
            ],
          ),
          const SizedBox(height: 16),
          TextField(
            controller: _targets,
            minLines: 2,
            maxLines: 5,
            decoration: const InputDecoration(
              labelText: 'Target sites',
              border: OutlineInputBorder(),
              hintText: 'acme.com\nexample.org',
              helperText: 'One domain/URL per line (or comma-separated)',
              helperMaxLines: 2,
            ),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _offering,
            minLines: 2,
            maxLines: 4,
            decoration: const InputDecoration(
              labelText: 'What you offer (for outreach)',
              border: OutlineInputBorder(),
              hintText: 'We design fast Shopify storefronts for indie brands.',
            ),
          ),
          const SizedBox(height: 16),
          FilledButton.icon(
            onPressed: _busy ? null : _generate,
            icon: const Icon(Icons.travel_explore),
            label: Text(_busy ? 'Working…' : 'Find leads'),
          ),
          if (_busy) ...[
            const SizedBox(height: 16),
            LinearProgressIndicator(value: _progress > 0 ? _progress / 100 : null),
            const SizedBox(height: 6),
            Text('$_status  ${_progress > 0 ? '$_progress%' : ''}',
                style: Theme.of(context).textTheme.bodySmall),
          ],
          if (_leads.isNotEmpty) ...[
            const Divider(height: 24),
            Row(
              children: [
                Expanded(
                  child: Text('${_leads.length} lead(s)',
                      style: Theme.of(context).textTheme.titleSmall),
                ),
                TextButton.icon(
                  onPressed: _downloadCsv,
                  icon: const Icon(Icons.download, size: 18),
                  label: const Text('CSV'),
                ),
              ],
            ),
            const SizedBox(height: 4),
            for (final lead in _leads) _LeadCard(lead: lead),
          ],
          if (_error != null) ...[
            const SizedBox(height: 12),
            Text(_error!, style: const TextStyle(color: Colors.redAccent)),
          ],
        ],
      ),
    );
  }
}

class _LeadCard extends StatelessWidget {
  const _LeadCard({required this.lead});
  final _Lead lead;

  @override
  Widget build(BuildContext context) {
    final small = Theme.of(context).textTheme.bodySmall;
    return Card(
      margin: const EdgeInsets.symmetric(vertical: 4),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(lead.company,
                style: Theme.of(context).textTheme.titleSmall,
                overflow: TextOverflow.ellipsis),
            if (lead.email != null) _line(Icons.email_outlined, lead.email!, small),
            if (lead.phone != null) _line(Icons.phone_outlined, lead.phone!, small),
            for (final s in lead.socials) _line(Icons.link, s, small),
            if (lead.outreach != null && lead.outreach!.isNotEmpty) ...[
              const SizedBox(height: 8),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: Theme.of(context).colorScheme.surface,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: SelectableText(lead.outreach!, style: small),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _line(IconData icon, String text, TextStyle? style) => Padding(
        padding: const EdgeInsets.only(top: 4),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(icon, size: 14),
            const SizedBox(width: 6),
            Expanded(child: SelectableText(text, style: style)),
          ],
        ),
      );
}
