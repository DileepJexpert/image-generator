"""Minimal FastAPI wrapper around faster-whisper (speech-to-text).

Contract used by the backend WhisperClient:
  POST /transcribe   multipart field `file` (audio)  -> JSON
       { "text": "...", "language": "en",
         "segments": [ { "start": 0.0, "end": 2.3, "text": "..." }, ... ] }
  GET  /health                                        -> {"status": "ok"}

The model loads once at startup. WHISPER_MODEL selects the size (tiny/base/
small/...); base on int8 keeps it fast and CPU-friendly.
"""
import os
import tempfile

from fastapi import FastAPI, File, UploadFile
from fastapi.responses import JSONResponse
from faster_whisper import WhisperModel

MODEL_SIZE = os.environ.get("WHISPER_MODEL", "base")

app = FastAPI(title="katixo-whisper")

# int8 on CPU is the fast/light default; the model weights are baked into the
# image at build time so the first request doesn't pay a download.
_model = WhisperModel(MODEL_SIZE, device="cpu", compute_type="int8")


@app.get("/health")
def health():
    return JSONResponse({"status": "ok"})


@app.post("/transcribe")
async def transcribe(file: UploadFile = File(...)):
    suffix = os.path.splitext(file.filename or "")[1] or ".wav"
    tmp = tempfile.NamedTemporaryFile(suffix=suffix, delete=False).name
    try:
        with open(tmp, "wb") as f:
            f.write(await file.read())
        segments, info = _model.transcribe(tmp, vad_filter=True)
        seg_list = [
            {"start": round(s.start, 3), "end": round(s.end, 3), "text": s.text.strip()}
            for s in segments
        ]
    finally:
        try:
            os.remove(tmp)
        except OSError:
            pass

    full_text = " ".join(s["text"] for s in seg_list).strip()
    return JSONResponse(
        {"text": full_text, "language": info.language, "segments": seg_list}
    )
