# tts — Piper text-to-speech sidecar

A tiny FastAPI wrapper around [Piper](https://github.com/rhasspy/piper) that
turns text into a WAV clip. Runs on CPU, so it works on any host (no GPU
needed) and leaves the GPU free for ComfyUI.

## API

```
POST /speak   JSON {"text": "...", "voice"?: "en_US-amy-medium"}  -> audio/wav
GET  /health                                                       -> {"status":"ok"}
```

Called by the backend `TtsClient`; the orchestrator wraps it in a
`text_to_speech` job and stores the result as an `audio` asset.

## Voices

The image ships one voice: `en_US-amy-medium`. To add more, drop additional
Piper `*.onnx` + `*.onnx.json` pairs into `/voices` (see the
[piper-voices](https://huggingface.co/rhasspy/piper-voices) collection) and pass
the file stem as the `voice` field. Unknown names fall back to the default.

## Run

Built and run by `backend/docker-compose.yml` as the `tts` service on port 7002.
It is part of the always-runnable (no-GPU) subset.
