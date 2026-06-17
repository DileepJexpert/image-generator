import '../../core/id.dart';
import '../model/design_element.dart';
import '../model/scene_page.dart';

/// A starter layout. [build] seeds the initial page(s) for a new project,
/// sized to the chosen canvas (CLAUDE.md §7 polish: templates).
class DesignTemplate {
  const DesignTemplate(this.name, this.build);

  final String name;
  final List<ScenePage> Function(int canvasWidth, int canvasHeight) build;
}

ScenePage _page(List<DesignElement> elements) =>
    ScenePage(id: newId(), elements: elements);

final List<DesignTemplate> kTemplates = [
  DesignTemplate('Blank', (w, h) => [_page(const [])]),

  DesignTemplate('Title card', (w, h) {
    return [
      _page([
        DesignElement.shape(
          id: newId(),
          x: 0,
          y: 0,
          width: w.toDouble(),
          height: h.toDouble(),
          shape: 'rect',
          fill: '#FF1A1730',
          stroke: '#00000000',
          zIndex: 0,
        ),
        DesignElement.text(
          id: newId(),
          x: w * 0.1,
          y: h * 0.36,
          width: w * 0.8,
          height: h * 0.18,
          text: 'Your Title',
          fontSize: h * 0.09,
          weight: 800,
          align: 'center',
          color: '#FFFFFFFF',
          zIndex: 1,
        ),
        DesignElement.text(
          id: newId(),
          x: w * 0.1,
          y: h * 0.56,
          width: w * 0.8,
          height: h * 0.08,
          text: 'A subtitle goes here',
          fontSize: h * 0.035,
          weight: 400,
          align: 'center',
          color: '#FFB9B4D8',
          zIndex: 2,
        ),
      ]),
    ];
  }),

  DesignTemplate('Quote', (w, h) {
    return [
      _page([
        DesignElement.shape(
          id: newId(),
          x: 0,
          y: 0,
          width: w.toDouble(),
          height: h.toDouble(),
          shape: 'rect',
          fill: '#FF6C5CE7',
          stroke: '#00000000',
          zIndex: 0,
        ),
        DesignElement.text(
          id: newId(),
          x: w * 0.12,
          y: h * 0.3,
          width: w * 0.76,
          height: h * 0.4,
          text: '“Design is intelligence made visible.”',
          fontSize: h * 0.06,
          weight: 700,
          align: 'center',
          color: '#FFFFFFFF',
          zIndex: 1,
        ),
      ]),
    ];
  }),

  DesignTemplate('Photo + caption', (w, h) {
    return [
      _page([
        DesignElement.shape(
          id: newId(),
          x: w * 0.08,
          y: h * 0.08,
          width: w * 0.84,
          height: h * 0.6,
          shape: 'rrect',
          cornerRadius: 24,
          fill: '#FFE9E9F0',
          stroke: '#FFCDCDE0',
          zIndex: 0,
        ),
        DesignElement.text(
          id: newId(),
          x: w * 0.08,
          y: h * 0.74,
          width: w * 0.84,
          height: h * 0.12,
          text: 'Add a caption — drop an image into the frame above.',
          fontSize: h * 0.035,
          weight: 500,
          align: 'center',
          color: '#FF222222',
          zIndex: 1,
        ),
      ]),
    ];
  }),
];
