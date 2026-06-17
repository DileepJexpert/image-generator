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

```bash
flutter run -d chrome
```

By default the app calls the API at the same origin under `/api/v1`. In
production, nginx proxies `/api` and `/ws` to the backend monolith (see
`nginx.conf`); for `docker compose up` everything is wired automatically.

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
