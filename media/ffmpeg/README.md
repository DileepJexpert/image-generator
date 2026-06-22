# ffmpeg sidecar

CPU-only FastAPI wrapper around `ffmpeg` that assembles a narrated slideshow MP4.
Used by the backend `VideoComposeClient` for the lesson feature's narrated-video output.

## Contract

```
POST /compose   multipart: repeated `images` + repeated `audios` (aligned by order)
                -> video/mp4
GET  /health    -> {"status": "ok"}
```

Image `i` is shown for the duration of audio `i`; the per-section segments are
concatenated into a single MP4 with the narration as the audio track. The two
lists must be the same length.

Output size/fps are configurable via env: `VIDEO_WIDTH` (1280), `VIDEO_HEIGHT`
(720), `VIDEO_FPS` (25).

## Run

Part of the always-runnable (no-GPU) subset:

```
docker compose up ffmpeg
```
