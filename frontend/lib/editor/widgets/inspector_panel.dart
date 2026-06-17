import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../model/design_element.dart';
import '../state/editor_controller.dart';
import 'edit_actions.dart';

/// Right-hand properties panel for the selected element. Edits flow back through
/// [EditorController] (CLAUDE.md §7: mutate via the Riverpod notifier).
class InspectorPanel extends ConsumerWidget {
  const InspectorPanel({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final selected = ref.watch(editorControllerProvider.select((s) => s.selected));
    final controller = ref.read(editorControllerProvider.notifier);

    return Container(
      width: 260,
      color: Theme.of(context).colorScheme.surfaceContainerHighest,
      padding: const EdgeInsets.all(16),
      child: selected == null
          ? const Center(child: Text('Select an element'))
          : ListView(
              children: [
                Text('Properties',
                    style: Theme.of(context).textTheme.titleMedium),
                const SizedBox(height: 12),
                if (selected is TextElement) ...[
                  _TextEditor(element: selected, controller: controller),
                  const Divider(height: 28),
                ],
                if (selected is ImageElement) ...[
                  EditActions(element: selected),
                  const Divider(height: 28),
                ],
                _SliderRow(
                  label: 'Opacity',
                  value: selected.opacity,
                  min: 0,
                  max: 1,
                  onChanged: (v) =>
                      controller.updateElement(selected.id, (e) => e.copyWith(opacity: v)),
                ),
                _SliderRow(
                  label: 'Rotation',
                  value: selected.rotation,
                  min: -3.1416,
                  max: 3.1416,
                  onChanged: (v) =>
                      controller.updateElement(selected.id, (e) => e.copyWith(rotation: v)),
                ),
                const SizedBox(height: 8),
                Row(
                  children: [
                    Expanded(
                      child: OutlinedButton.icon(
                        onPressed: controller.bringSelectedToFront,
                        icon: const Icon(Icons.flip_to_front, size: 18),
                        label: const Text('Front'),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: OutlinedButton.icon(
                        onPressed: controller.deleteSelected,
                        icon: const Icon(Icons.delete_outline, size: 18),
                        label: const Text('Delete'),
                      ),
                    ),
                  ],
                ),
              ],
            ),
    );
  }
}

class _TextEditor extends StatefulWidget {
  const _TextEditor({required this.element, required this.controller});

  final TextElement element;
  final EditorController controller;

  @override
  State<_TextEditor> createState() => _TextEditorState();
}

class _TextEditorState extends State<_TextEditor> {
  late final TextEditingController _text =
      TextEditingController(text: widget.element.text);

  @override
  void didUpdateWidget(_TextEditor old) {
    super.didUpdateWidget(old);
    if (widget.element.text != _text.text) {
      _text.text = widget.element.text;
    }
  }

  @override
  void dispose() {
    _text.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        TextField(
          controller: _text,
          minLines: 1,
          maxLines: 4,
          decoration: const InputDecoration(
            labelText: 'Text',
            border: OutlineInputBorder(),
          ),
          onChanged: (v) => widget.controller
              .updateElement(widget.element.id, (e) => (e as TextElement).copyWith(text: v)),
        ),
        const SizedBox(height: 8),
        _SliderRow(
          label: 'Font size',
          value: widget.element.fontSize,
          min: 8,
          max: 200,
          onChanged: (v) => widget.controller.updateElement(
              widget.element.id, (e) => (e as TextElement).copyWith(fontSize: v)),
        ),
      ],
    );
  }
}

class _SliderRow extends StatelessWidget {
  const _SliderRow({
    required this.label,
    required this.value,
    required this.min,
    required this.max,
    required this.onChanged,
  });

  final String label;
  final double value;
  final double min;
  final double max;
  final ValueChanged<double> onChanged;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('$label: ${value.toStringAsFixed(2)}',
            style: Theme.of(context).textTheme.bodySmall),
        Slider(value: value.clamp(min, max), min: min, max: max, onChanged: onChanged),
      ],
    );
  }
}
