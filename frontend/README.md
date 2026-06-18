# Katixo Studio — Frontend (Flutter Web)

The design editor + AI panels. Talks only to our internal backend API
(`/api/v1` + `/ws`), never to ComfyUI directly (CLAUDE.md prime directive #3).

## Code generation

The scene model uses **freezed + json_serializable**. The generated sources
(`lib/editor/model/*.freezed.dart`, `*.g.dart`) are **committed to the repo**, so
a fresh clone compiles and runs without any code-gen step — they depend only on
the stable `freezed_annotation` runtime. `pubspec.lock` is committed as well to
pin dependency versions (a too-new transitive `analyzer` can silently break the
freezed 2.x generator).

You only need to regenerate after **changing a model** (`design_element.dart`,
`project.dart`, `scene_page.dart`):

```bash
flutter pub get
dart run build_runner build --delete-conflicting-outputs
```

Then commit the regenerated `*.freezed.dart` / `*.g.dart` alongside the model
change. The Docker build also runs this step.

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

### Backend origin

The app resolves the backend origin automatically:

* **`flutter run` (debug)** → defaults to `http://localhost:8585`, so a locally
  run Spring Boot backend just works — no flag needed.
* **Override** with `--dart-define=API_ORIGIN=http://host:port` (e.g. a backend
  on another machine/port).
* **Release build** (`docker compose up`, served by nginx) → same origin; nginx
  proxies `/api` and `/ws` to the monolith. See `nginx.conf`.

So for local dev you only need the backend up on :8585 and `flutter run -d chrome`.

### Troubleshooting

* **Errors about missing union types (`ImageElement`/`ShapeElement`/
  `VideoElement`), a missing `elements` getter, or `_$...FromJson`/`...ToJson`
  not found** mean the generated `*.freezed.dart` / `*.g.dart` files are missing
  or stale. They are committed, so a fresh clone should already have them; if you
  deleted them or `build_runner` left them empty, restore with
  `git checkout -- lib/editor/model` or regenerate via the step above.
* **`build_runner` produces empty files / silently generates nothing** is usually
  a too-new transitive `analyzer` against the freezed 2.x generator. Use the
  committed `pubspec.lock` (`flutter pub get` without upgrading) so versions stay
  pinned; avoid `flutter pub upgrade` unless you also bump freezed.
* **"Failed to load projects" with `type 'String' is not a subtype of type
  List<dynamic>`** means the API call got HTML (an `index.html`) instead of JSON —
  i.e. the request hit the Flutter dev server, not the backend. In debug the app
  now targets `http://localhost:8585` automatically, so this means the **backend
  isn't running** (or isn't on :8585). Start it and check
  `http://localhost:8585/actuator/health` is `UP`.

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
