"""Minimal FastAPI wrapper around Piper (text-to-speech).

Contract used by the backend TtsClient:
  POST /speak   JSON {text, voice?}  -> audio/wav (synthesized clip)
  GET  /health                       -> {"status": "ok"}

Voices are .onnx (+ .onnx.json) files under VOICES_DIR; the image ships one
en_US voice. `voice` selects another installed voice by name; unknown names
fall back to the default.
"""
import os
import subprocess
import tempfile

from fastapi import FastAPI
from fastapi.responses import JSONResponse, Response
from pydantic import BaseModel

VOICES_DIR = os.environ.get("VOICES_DIR", "/voices")
DEFAULT_VOICE = os.environ.get("DEFAULT_VOICE", "en_US-amy-medium")

app = FastAPI(title="katixo-tts")


class SpeakRequest(BaseModel):
    text: str
    voice: str | None = None


def model_path(voice: str | None) -> str:
    # basename() guards against path traversal in the requested voice name.
    name = os.path.basename(voice) if voice else DEFAULT_VOICE
    path = os.path.join(VOICES_DIR, f"{name}.onnx")
    if not os.path.exists(path):
        path = os.path.join(VOICES_DIR, f"{DEFAULT_VOICE}.onnx")
    return path


@app.get("/health")
def health():
    return JSONResponse({"status": "ok"})


@app.post("/speak")
def speak(req: SpeakRequest):
    text = (req.text or "").strip()
    if not text:
        return Response(content="empty text", status_code=400)

    model = model_path(req.voice)
    out = tempfile.NamedTemporaryFile(suffix=".wav", delete=False).name
    try:
        subprocess.run(
            ["piper", "--model", model, "--output_file", out],
            input=text.encode("utf-8"),
            check=True,
            capture_output=True,
        )
        with open(out, "rb") as f:
            data = f.read()
    finally:
        try:
            os.remove(out)
        except OSError:
            pass

    return Response(content=data, media_type="audio/wav")
