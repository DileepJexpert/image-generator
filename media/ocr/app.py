"""
Katixo OCR sidecar — PaddleOCR behind a tiny FastAPI service.

Part of the Katixo Studio monolith's doc-AI extraction path (the pipeline merged in from
katixo-docai). It does ONE job: turn an image into text blocks with bounding boxes and a
confidence score. It runs on localhost, makes no outbound calls, and — once the models are
baked into the image at build time — needs no internet at runtime.

PaddleOCR runs on CPU by default so the GPU stays free for the heavy generators (ComfyUI) and
the Ollama LLM. OCR is deliberately NOT routed through the shared GpuResourceGuard: it is light
and commonly CPU-bound (see the backend PaddleOcrClient).

Contract used by the backend PaddleOcrClient:
  POST /ocr   multipart field `file` (image)  -> {"text": str, "blocks": [...], "confidence": float}
  GET  /health                                -> {"status": "ok", "engine": "paddleocr", "lang": "en"}
"""
import io
import logging

import cv2
import numpy as np
from fastapi import FastAPI, File, HTTPException, UploadFile
from paddleocr import PaddleOCR

logging.basicConfig(level=logging.INFO)
log = logging.getLogger("katixo-ocr")

app = FastAPI(title="Katixo OCR sidecar", version="0.1.0")

# DECISION (language): English by default. For Hindi/English bilingual docs switch lang to a
# multilingual model (PaddleOCR's English model also reads Latin digits/punctuation well).
_ocr = PaddleOCR(use_angle_cls=True, lang="en")


@app.get("/health")
def health():
    return {"status": "ok", "engine": "paddleocr", "lang": "en"}


@app.post("/ocr")
async def run_ocr(file: UploadFile = File(...)):
    data = await file.read()
    if not data:
        raise HTTPException(status_code=400, detail="Empty file")

    img = cv2.imdecode(np.frombuffer(data, np.uint8), cv2.IMREAD_COLOR)
    if img is None:
        raise HTTPException(status_code=400, detail="Could not decode image")

    try:
        raw = _ocr.ocr(img, cls=True)
    except Exception as e:  # noqa: BLE001 - surface a clean 500, never a stacktrace to the caller
        log.exception("OCR failed")
        raise HTTPException(status_code=500, detail=f"OCR engine error: {e}")

    blocks = []
    confidences = []
    # PaddleOCR 2.x returns a list (one entry per image); each entry is a list of [box, (text, conf)].
    page = raw[0] if raw and raw[0] else []
    for line in page:
        box, (text, conf) = line[0], line[1]
        xs = [float(p[0]) for p in box]
        ys = [float(p[1]) for p in box]
        blocks.append({
            "text": text,
            "bbox": [min(xs), min(ys), max(xs), max(ys)],
            "confidence": float(conf),
        })
        confidences.append(float(conf))

    text = "\n".join(b["text"] for b in blocks)
    confidence = float(sum(confidences) / len(confidences)) if confidences else 0.0
    return {"text": text, "blocks": blocks, "confidence": confidence}
