# ocr — PaddleOCR text-extraction sidecar

A tiny FastAPI wrapper around [PaddleOCR](https://github.com/PaddlePaddle/PaddleOCR)
that turns a document image (an invoice / GRN / bill page, or a lesson scan) into
text blocks with bounding boxes and a confidence score. Runs on CPU, so it works
on any host and leaves the GPU free for ComfyUI and the Ollama LLM.

This is the OCR engine for the **doc-AI extraction pipeline** that was merged into
the Studio monolith (`com.katixo.ai.*`). It is the only Python in that path and it
makes no outbound calls.

## API

```
POST /ocr     multipart `file` (image)  -> JSON
     { "text": "...",
       "blocks": [ { "text": "...", "bbox": [x0,y0,x1,y1], "confidence": 0.99 }, ... ],
       "confidence": 0.97 }
GET  /health                            -> {"status":"ok","engine":"paddleocr","lang":"en"}
```

Called by the backend `PaddleOcrClient` (`com.katixo.ai.ocr`). OCR is intentionally
**not** routed through the shared `GpuResourceGuard`: PaddleOCR is light and
CPU-bound, so serializing it against the GPU generators would only add latency.

## Models

The image **bakes in** the detection/recognition/angle-classifier models at build
time, so the first request pays no download and the sidecar runs fully offline at
runtime. English (`lang=en`) by default; for Hindi/English documents switch to a
multilingual model (see the `DECISION (language)` note in `app.py`).

## Run

Built and run by `backend/docker-compose.yml` as the `ocr` service on port 8000
(`OCR_URL=http://ocr:8000` for the backend). It is part of the always-runnable
(no-GPU) subset.
