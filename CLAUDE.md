# CLAUDE.md — Katixo Studio

Self-hosted, GPU-powered design + media studio (a personal "Canva") for a single operator on one workstation. Generates images and short video clips locally via ComfyUI, and composes them on a Flutter design canvas.

Rename `katixo-studio` freely; it is used only as the repo/module name below.

> **Scope note.** This file specifies the **Phase-1 creative core** (image + short
> video design studio). The product is expanding into a broader local AI studio
> (image, video, audio/voice, text/LLM, plus analytic tools and a Copilot agent) —
> see [`VISION.md`](./VISION.md) for that direction and the phased roadmap. The
> architecture backbone here (single monolith orchestrator + installed sidecars +
> async jobs + Flutter) is unchanged by the expansion; new capabilities are added
> as more sidecars, job types, Flyway migrations, and UI panels.

## 1. Prime directives (read before writing any code)

1. **Single Spring Boot monolith.** One backend application, package-by-feature. No microservices, no service mesh, no event-bus-between-services. The Python media tools (ComfyUI, rembg, Real-ESRGAN) are infrastructure sidecars, not "our services" — we only call them over HTTP.
2. **One Flutter codebase** targeting Flutter Web (and trivially desktop later). The design editor is built with Flutter widgets + `CustomPainter`, NOT a game engine and NOT an embedded JS editor.
3. **Flutter never talks to ComfyUI directly.** All generation goes Flutter → Spring Boot orchestrator → ComfyUI. The frontend only knows our internal API.
4. **Generation is always an async job.** No HTTP request thread ever blocks waiting for a model. Submit job → return `jobId` → stream progress over WebSocket → deliver result.
5. **All schema changes go through Flyway.** No `ddl-auto: update`. Ever.
6. **Build in milestone order (section 11).** Deliver a runnable tool at every milestone. Do not scaffold all features at once.

## 2. What we are building

A local studio where a single user can:

* Generate images locally (SDXL / SDXL-Lightning / Flux GGUF via ComfyUI).
* Generate short (≈5s) video clips, primarily image-to-video.
* Remove backgrounds (rembg / BiRefNet) and upscale (Real-ESRGAN).
* Compose all of the above on a multi-element design canvas with text, shapes, and images; then export to PNG / PDF.

Target hardware: one workstation with a 12GB NVIDIA GPU. Everything runs locally via Docker Compose. No cloud dependency required.

## 3. Architecture

```
Flutter Web (design editor + AI panels)
        │  REST + WebSocket (our internal API only)
        ▼
Spring Boot monolith (orchestrator)
   • auth (single-user/local for now)
   • projects + assets CRUD
   • job queue (Redis) + WebSocket progress
   • ComfyUI workflow templating + dispatch
        │                         │
        ▼                         ▼
Python media sidecars        PostgreSQL + asset storage
   • ComfyUI (GPU)              • projects (scene JSON)
   • rembg                      • assets (files on disk / MinIO)
   • Real-ESRGAN               • jobs
```

The only code we author is the Flutter app and the Spring Boot monolith. Everything else is installed and called.

## 4. Tech stack

Use the latest stable of each unless a specific pin is given.

**Backend**
* Java 21 (LTS), Spring Boot 3.x
* Spring Web, Spring Data JPA, Spring WebSocket
* Flyway (schema), PostgreSQL 16 driver
* Redis (Spring Data Redis / Lettuce) for the job queue
* Maven build (Spring Boot starter parent)

**Frontend**
* Flutter (latest stable channel), targeting Web
* State: Riverpod (`flutter_riverpod`)
* Models: freezed + `json_serializable` (immutable, JSON in/out)
* HTTP: dio
* WebSocket: web_socket_channel
* Routing: go_router

**Media sidecars (do not write — install/configure)**
* ComfyUI (HTTP API on :8188)
* rembg (FastAPI wrapper) or a small custom FastAPI service
* Real-ESRGAN (small FastAPI wrapper)

**Infra**
* Docker Compose, with `nvidia-container-toolkit` for GPU passthrough to ComfyUI
* PostgreSQL 16, Redis 7
* Asset storage: local volume now; structure the storage layer behind an interface so MinIO (S3 API) can drop in later without touching call sites.

## 5. Repository layout (monorepo)

```
katixo-studio/
├── CLAUDE.md
├── backend/                      # Spring Boot monolith
│   ├── pom.xml
│   ├── docker-compose.yml        # whole-stack compose (one-click run from IDE)
│   └── src/main/java/com/katixo/studio/
│       ├── StudioApplication.java
│       ├── config/               # web, ws, redis, cors
│       ├── project/              # Project entity, repo, service, controller, dto
│       ├── asset/                # Asset storage + metadata
│       ├── job/                  # Job entity, queue, status, ws progress
│       ├── generation/           # ComfyUI client + workflow templating
│       │   └── workflows/        # *.json ComfyUI graph templates
│       └── media/                # rembg / esrgan clients
│   └── src/main/resources/db/migration/   # Flyway V1__*.sql ...
├── frontend/                     # Flutter app
│   └── lib/
│       ├── main.dart
│       ├── core/                 # api client, ws client, theme, router
│       ├── editor/               # canvas, scene model, transform handles, export
│       │   ├── model/            # DesignElement + subtypes (freezed)
│       │   ├── canvas/           # CustomPainter + InteractiveViewer host
│       │   └── tools/            # selection, text, shape, resize/rotate gestures
│       ├── generation/           # AI panels (image, video, bg-remove, upscale)
│       └── projects/             # project list, save/load
└── media/                        # Dockerfiles + configs for python sidecars
    ├── comfyui/
    ├── rembg/
    └── esrgan/
```

## 6. Backend conventions

* Package-by-feature (see layout). No `controllers/ services/ repos/` top-level split.
* Constructor injection only. No field `@Autowired`.
* Entities are persistence-only; never serialize entities to the API. Map to DTOs.
* One Flyway migration per change, never edit a shipped migration.
* Controllers are thin; logic lives in services.
* API base path: `/api/v1`.

**Internal API (Flutter → backend)**

```
# Projects
GET    /api/v1/projects
POST   /api/v1/projects                 {name, canvasWidth, canvasHeight}
GET    /api/v1/projects/{id}
PUT    /api/v1/projects/{id}            {name?, sceneJson}     # autosave target
DELETE /api/v1/projects/{id}

# Assets
GET    /api/v1/assets/{id}              # streams the file
POST   /api/v1/assets                   # multipart upload (user images)

# Generation (all return {jobId})
POST   /api/v1/generate/image           {prompt, negativePrompt?, width, height, model, seed?}
POST   /api/v1/generate/image-to-video  {sourceAssetId, prompt?, durationSeconds}
POST   /api/v1/edit/remove-bg           {assetId}
POST   /api/v1/edit/upscale             {assetId, scale}       # 2 or 4

# Jobs
GET    /api/v1/jobs/{jobId}             # {id, type, status, progress, resultAssetId?, error?}
WS     /ws/jobs/{jobId}                 # progress events: {progress, status, resultAssetId?}
```

**ComfyUI integration (generation feature)**
* Store ComfyUI workflow graphs as JSON templates under `generation/workflows/` (e.g. `text2img.json`, `img2video_ltx.json`).
* The `ComfyUiClient` injects params (prompt, seed, width, height, source image) into the template, then:
   1. `POST /prompt` to ComfyUI → receive `prompt_id`.
   2. Track progress via ComfyUI's `/ws` channel, mirror it onto our job's WebSocket.
   3. On completion, fetch the output image/video, persist it as an `Asset`, set `job.resultAssetId`, emit final WS event.
* Never hardcode node params in Java — only the template JSON files know the graph.

## 7. Frontend: the design editor

The editor is the only genuinely hard part. Build it as data + render + interact:

**Scene model (freezed, JSON-serializable — this IS the save format)**

```
Project { id, name, canvasWidth, canvasHeight, List<DesignElement> elements }

DesignElement (sealed/union):
  common: id, x, y, width, height, rotation, opacity, zIndex, locked
  TextElement      { text, fontSize, fontFamily, color, align, weight }
  ImageElement     { assetId, fit }          # uploaded or generated image
  ShapeElement     { shape, fill, stroke, cornerRadius }
  VideoElement     { assetId, autoplay, loop }
```

`Project.toJson()` is what gets `PUT` to `/projects/{id}` (the `sceneJson` column).

**Canvas**
* Host the canvas in an `InteractiveViewer` for pan/zoom.
* Render the page background + all elements via a single `CustomPainter` (`scene_painter.dart`), drawing in `zIndex` order with rotation/opacity applied.
* Images/video are drawn from a decoded-image cache keyed by `assetId`.

**Interaction**
* Tap to select → show a `TransformHandles` overlay (4 corner handles for resize, one top handle for rotation, drag body to move).
* Implement handles with `GestureDetector` + `onPanUpdate`, converting drag deltas into element `width/height/rotation/x/y` mutations through Riverpod.
* Keep selection state and the element list in a Riverpod `Notifier`.

**Export**
* Wrap the canvas in a `RepaintBoundary`; export PNG via `boundary.toImage(pixelRatio: N)` at high ratio for crisp output.
* PDF export via the `pdf` package (place the rendered image).
* Video export is a backend concern (stitch clips); frontend just requests it.

**AI panels**
* Image panel: prompt + size + model dropdown → `POST /generate/image` → open WS on the returned `jobId` → show progress bar → on done, add an `ImageElement` referencing the new asset onto the canvas.
* Image-to-video panel: pick an on-canvas image → `POST /generate/image-to-video`.
* Remove-bg / upscale: act on the selected `ImageElement`'s `assetId`, replace on completion.

## 8. Data model (Flyway V1)

```
projects(id uuid pk, name text, canvas_width int, canvas_height int,
         scene_json jsonb, created_at, updated_at)

assets(id uuid pk, type text,            -- image | video
       file_path text, mime text, width int, height int,
       source_job_id uuid null, created_at)

jobs(id uuid pk, type text,              -- image | image_to_video | remove_bg | upscale
     status text,                        -- queued | running | done | failed
     params_json jsonb, progress int default 0,
     result_asset_id uuid null, error text null,
     created_at, updated_at)
```

## 9. Async job model

* On a generation request: create a `jobs` row (`queued`), push the id to a Redis list, return `{jobId}` immediately.
* A worker (a `@Component` consuming the Redis queue) picks it up, sets `running`, drives ComfyUI/media client, updates `progress`, and on finish writes the asset + sets `done`.
* Progress is pushed to `/ws/jobs/{jobId}` subscribers throughout.
* Concurrency = 1 worker by default (single 12GB GPU can't run parallel jobs).

## 10. Local dev & GPU

`backend/docker-compose.yml` services: `postgres`, `redis`, `comfyui` (GPU), `rembg`, `esrgan`, `backend`, `frontend`. (The compose file lives under `backend/` for one-click run from the IDE; its build contexts are relative to that directory.)

* Give ComfyUI the GPU via the `nvidia` runtime / `deploy.resources.reservations.devices`.
* Mount a persistent volume for ComfyUI `models/` (checkpoints are large).
* Backend reads service URLs from env (`COMFYUI_URL`, `REMBG_URL`, `ESRGAN_URL`, `DATABASE_URL`, `REDIS_URL`).
* `docker compose up` must bring the whole studio online.

## 11. Build milestones (do these in order; stop and confirm after each)

1. **Skeleton.** Monorepo + `backend/docker-compose.yml` (postgres, redis, empty backend with health check, Flutter web shell that loads). Flyway V1 creates the three tables. Acceptance: `docker compose up` runs; `/actuator/health` is UP; Flutter shell renders.
2. **Generation spine.** ComfyUI in compose; `text2img.json` template; `POST /generate/image` → job → ComfyUI → asset stored → job `done`; WS emits progress. Acceptance: a curl call produces a saved PNG and a completed job.
3. **Editor core.** Scene model (freezed), CustomPainter render, add text + upload image, move/resize/rotate, save/load project, export PNG. Acceptance: build a 2-element design, reload it, export it.
4. **Generate-into-canvas.** Image panel wired end-to-end; generated image lands on the canvas as an `ImageElement`. Acceptance: type prompt → image appears on canvas.
5. **Edit tools.** Remove-bg + upscale services and buttons on selected images.
6. **Video.** rembg/esrgan done; add `img2video_ltx.json` + image-to-video panel; `VideoElement` renders on canvas.
7. **Polish.** Templates, presets, multi-page, PDF export.

## 12. Do NOT

* Do NOT split the backend into multiple services.
* Do NOT call ComfyUI from Flutter.
* Do NOT block a request thread on a model; always go through the job queue.
* Do NOT use `ddl-auto`; all schema via Flyway.
* Do NOT hardcode ComfyUI node params in Java; keep them in workflow JSON.
* Do NOT build the canvas on a game engine or pull in a JS editor.
* Do NOT add auth providers / OAuth yet — single local user is fine for now.
