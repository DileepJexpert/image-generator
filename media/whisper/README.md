# whisper — faster-whisper speech-to-text sidecar

A tiny FastAPI wrapper around [faster-whisper](https://github.com/SYSTRAN/faster-whisper)
that turns an audio clip into a transcript with word-timed segments (for
captions). Runs on CPU, so it works on any host and leaves the GPU free for
ComfyUI.

## API

```
POST /transcribe   multipart `file` (audio)  -> JSON
     { "text": "...", "language": "en",
       "segments": [ { "start": 0.0, "end": 2.3, "text": "..." }, ... ] }
GET  /health                                  -> {"status":"ok"}
```

Called by the backend `WhisperClient`; the orchestrator wraps it in a
`transcribe` job and stores the JSON result as a `text` asset (so it flows
through the same job → asset → WebSocket pipeline as every other result).

## Model

The image bakes in the `base` model (int8). Build a different size with
`--build-arg WHISPER_MODEL=small` (or `tiny`, `medium`, ...). Bigger models are
more accurate but slower on CPU.

## Run

Built and run by `backend/docker-compose.yml` as the `whisper` service on port
7003. It is part of the always-runnable (no-GPU) subset.
