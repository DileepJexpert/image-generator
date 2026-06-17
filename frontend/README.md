# Katixo Studio — Frontend (Flutter Web)

The design editor + AI panels. Talks only to our internal backend API
(`/api/v1` + `/ws`), never to ComfyUI directly (CLAUDE.md prime directive #3).

## Code generation

The scene model uses **freezed + json_serializable**, so generated sources
(`*.freezed.dart`, `*.g.dart`) must be created before running/building. They are
gitignored; regenerate with:

```bash
flutter pub get
dart run build_runner build --delete-conflicting-outputs
```

The Docker build does this automatically.

## Run (dev)

First-time setup (or after pulling model changes) — generate sources **before**
running, otherwise the analyzer/compiler fails with errors like
`'ShapeElement' isn't a type` or `Method not found: '_$ScenePageFromJson'`:

```bash
flutter pub get
dart run build_runner build --delete-conflicting-outputs
flutter run -d chrome
```

While iterating on the scene model, keep code generation running in a separate
terminal so it regenerates on save, and run the app in another:

```bash
# terminal 1 — regenerates *.freezed.dart / *.g.dart on every save
dart run build_runner watch --delete-conflicting-outputs

# terminal 2
flutter run -d chrome
```

By default the app calls the API at the same origin under `/api/v1`. In
production, nginx proxies `/api` and `/ws` to the backend monolith (see
`nginx.conf`); for `docker compose up` everything is wired automatically.

### Troubleshooting

* **Errors about missing union types (`ImageElement`/`ShapeElement`/
  `VideoElement`), a missing `elements` getter, or `_$...FromJson`/`...ToJson`
  not found** mean the generated files are absent or stale. Run the
  `build_runner build --delete-conflicting-outputs` step above. These files are
  gitignored, so a fresh clone always needs this once.

## Build (web release)

```bash
dart run build_runner build --delete-conflicting-outputs
flutter build web --release
```

## Layout

```
lib/
  core/            api client, router, theme, hex colors, web download
  editor/
    model/         Project + DesignElement union (freezed; the save format)
    canvas/        ScenePainter, image cache, geometry, InteractiveViewer host
    tools/         TransformHandles (resize + rotate)
    widgets/       InspectorPanel (selected-element properties)
    export/        PNG capture from the RepaintBoundary
    editor_page.dart
  projects/        project list + create
```
