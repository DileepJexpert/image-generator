# faster-whisper speech-to-text sidecar (CPU; keeps the GPU free for ComfyUI).
FROM python:3.11-slim

RUN pip install --no-cache-dir \
    faster-whisper==1.0.3 \
    fastapi==0.115.0 \
    "uvicorn[standard]==0.30.6" \
    python-multipart==0.0.9

# Bake the model into the image so the first request doesn't pay a download.
# Override the size at build with --build-arg WHISPER_MODEL=small (etc.).
ARG WHISPER_MODEL=base
ENV WHISPER_MODEL=${WHISPER_MODEL}
RUN python -c "from faster_whisper import WhisperModel; WhisperModel('${WHISPER_MODEL}', device='cpu', compute_type='int8')"

WORKDIR /app
COPY app.py .

EXPOSE 7003
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "7003"]
