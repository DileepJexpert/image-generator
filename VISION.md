# VISION.md — Katixo Studio (expanded scope)

Katixo started as a local, GPU-powered **image + short-video design studio**. This
document captures the expanded direction: **one self-hosted AI studio where anyone
can create and analyze across image, video, audio/voice, and text** — all running
locally, all behind our own API.

This is the *aspirational* product vision. The build still proceeds in milestone
order (see `CLAUDE.md` §11 for the shipped Phase-1 milestones, and the **Roadmap**
below for the expansion). The architecture backbone does **not** change.

---

## 1. Product idea

A single workspace where a user drops in an idea and composes **image + video +
audio + text** on a shared canvas/timeline, assisted by an **LLM Copilot that can
drive every tool**. "Make a 15s product reel with a voiceover and captions" becomes
an orchestrated pipeline of local AI jobs.

The studio = **a library of AI tools** + **a canvas/timeline** + **a copilot agent**
that chains them into outcomes.

Audience: broad — content creators, marketers, general users, and pros who also
want analytic features (transcription, document Q&A, image understanding).

---

## 2. Architecture principle (unchanged)

Every new capability is **just one more sidecar + job type + UI panel** behind the
existing pattern:

```
Flutter (canvas + tool panels + copilot)
      │  REST + WebSocket (our internal API only)
      ▼
Spring Boot monolith (orchestrator)
   • job queue (Redis) + WS progress
   • capability/tool registry
   • copilot agent loop (calls our own job APIs)
      │
      ▼  HTTP to local model sidecars
 ComfyUI · TTS · STT(Whisper) · LLM(Ollama) · music · lipsync · rembg · esrgan
      │
      ▼
 PostgreSQL (+ pgvector for RAG) · asset storage
```

Hard rules carried over from `CLAUDE.md`: **single Spring Boot monolith** (no
microservices), **Flutter never talks to a model directly**, **generation is always
an async job**, **all schema via Flyway**, **model params live in workflow/templates,
not Java**. Sidecars are infrastructure we install and call, never author.

What's already reusable for the expansion:
- **ComfyUI sidecar** already covers most image + video features (new workflows only).
- **PostgreSQL** → add the **pgvector** extension for RAG, no new datastore.
- **Job queue + WS progress** pattern handles every long-running model call.
- **`assets`** table is generic (image/video; add `audio`).

---

## 3. Capability landscape (local models)

Model names are indicative as of early 2026 and will be re-verified per feature at
implementation time. VRAM tiers are rough guidance.

### Image (mostly via ComfyUI)
| Feature | Local models | VRAM |
|---|---|---|
| Text-to-image | SD 1.5 ✅, SDXL ✅, FLUX.1 [schnell/dev], SD 3.5 | 8–24GB |
| Instructed edit | FLUX Kontext, Qwen-Image-Edit, InstructPix2Pix | 12–24GB |
| Inpaint / outpaint | SDXL/FLUX inpaint | 8–16GB |
| Guided gen (pose/depth/edges) | ControlNet, IP-Adapter | +small |
| Background removal ✅ | rembg / BiRefNet | CPU–4GB |
| Upscale ✅ / restore / faces | Real-ESRGAN ✅, SUPIR, GFPGAN/CodeFormer | 4–16GB |
| Product relighting | IC-Light | 8GB |

### Video (heaviest; drives GPU sizing)
| Feature | Local models | VRAM |
|---|---|---|
| Image/Text-to-video | LTX-Video ✅, Wan 2.1/2.2, Hunyuan Video, CogVideoX, Mochi, SVD | 8–24GB+ |
| Talking head / lip-sync | LatentSync, SadTalker, Wav2Lip | 8–12GB |
| Video upscale / restore | Real-ESRGAN per-frame, SUPIR | 8–16GB |
| Frame interpolation | RIFE, FILM | 4–8GB |
| Video style / re-render | AnimateDiff, Wan + ControlNet | 12–24GB |

### Audio & voice (new sidecars)
| Feature | Local models | VRAM |
|---|---|---|
| Text-to-speech | Kokoro, Piper (CPU), XTTS-v2, F5-TTS, StyleTTS2 | CPU–6GB |
| Voice cloning (consent-gated) | XTTS-v2, F5-TTS, RVC | 4–8GB |
| Music generation | MusicGen, Stable Audio | 6–12GB |
| Sound effects | AudioGen / Stable Audio | 6GB |
| Transcription + captions (STT) | Whisper / faster-whisper, WhisperX | CPU–6GB |
| Stem separation / denoise | Demucs | 4–8GB |

### Text & LLM (the brain + analytics)
| Feature | Local stack | VRAM |
|---|---|---|
| Chat / writing / summarize | Ollama / llama.cpp / vLLM (Llama 3.x, Qwen 2.5/3, Mistral, Gemma, DeepSeek, Phi) | 8–48GB |
| Copilot that drives tools | LLM + function-calling/agent loop | — |
| RAG over docs/brand assets | embeddings (bge/nomic) + pgvector | small + CPU |
| Image understanding / OCR / auto-caption | LLaVA, Qwen-VL, Florence-2 | 8–16GB |
| Prompt enhancement, alt-text, tags | any local LLM | — |

---

## 4. What makes it ONE studio (not a tool pile)
1. **Shared asset library** — the generic `assets` store (add `audio`).
2. **Canvas + timeline** — the editor gains a time dimension for video/audio.
3. **Copilot agent** — the LLM orchestrates job pipelines (prompt → image → animate →
   TTS → captions → compose → export). This is the differentiator.
4. **Pipelines / templates** — one-click "Instagram reel," "product photoshoot,"
   "explainer video."

---

## 5. Hardware targets
- **Sweet spot: 24GB** (RTX 4090 / 5090-class) — runs FLUX, big video models, a
  14–32B LLM, and TTS/STT concurrently.
- **16GB** — very capable (most image/video, mid LLMs).
- **12GB** — solid entry (SDXL/FLUX-schnell, LTX, small LLMs).
- Pair with **64GB system RAM + fast NVMe**; a desktop GPU beats a same-named laptop.
- Concurrency stays **1 heavy GPU job at a time** by default (queue serializes).

---

## 6. Roadmap (phased; deliver a runnable studio at each step)

- **Phase 1 — Creative core (in progress).** Image gen ✅, remove-bg ✅, upscale ✅,
  image→video (LTX) 🧪, design canvas + export.
- **Phase 2 — Audio.** Whisper STT sidecar (transcription + word-timed captions) and
  a TTS sidecar (voiceover). New `audio` asset type; timeline gains an audio track.
  *In:* Piper TTS sidecar + `POST /api/v1/generate/speech`; faster-whisper STT
  sidecar + `POST /api/v1/transcribe` (transcript JSON stored as a `text` asset,
  downloadable as WebVTT captions). The Voiceover panel both generates speech and
  transcribes either that voiceover or an uploaded clip.
  *Next:* an audio track on the canvas/timeline.
- **Phase 3 — LLM Copilot + RAG.** Ollama sidecar; pgvector on Postgres; prompt
  enhancement, captions, chat, and the agent loop that calls our job APIs.
  *Started:* Ollama sidecar + a stateless Copilot chat API (`/api/v1/copilot/*`)
  with token streaming (`POST /copilot/chat/stream`, Server-Sent Events) and an
  in-editor Copilot panel that renders the reply live. *Next:* pgvector RAG and
  the tool-driving agent loop.
- **Phase 4 — Advanced creative.** ControlNet, inpaint/outpaint, FLUX, lip-sync,
  music/SFX, face restore, relighting.
- **Phase 5 — Pipelines & automation.** One-click templates (reel/photoshoot/
  explainer) built as Copilot-driven multi-job pipelines; analytic tools
  (image understanding, document Q&A).

Each phase adds sidecars + Flyway migrations + job types + UI panels behind the same
orchestrator. No phase requires a rewrite.

---

## 7. Cross-cutting concerns to design in
- **Capability registry** — backend advertises which tools/models are installed so
  the UI and Copilot adapt to the host's hardware/models.
- **Consent & safety** — voice cloning and likeness require an explicit consent gate;
  keep a content policy surface even for a local single-user tool.
- **Model management** — a consistent way to declare/download checkpoints per sidecar
  (we already use host bind mounts for ComfyUI models).
- **Storage growth** — video/audio assets are large; keep the storage layer behind
  its interface (MinIO/S3 drop-in later) and add retention/cleanup.
- **Observability** — per-job timing/VRAM so users understand cost on their hardware.
