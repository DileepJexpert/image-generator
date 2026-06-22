# ffmpeg video-assembly sidecar (CPU; keeps the GPU free for ComfyUI).
FROM python:3.11-slim

RUN apt-get update \
    && apt-get install -y --no-install-recommends ffmpeg \
    && rm -rf /var/lib/apt/lists/*

RUN pip install --no-cache-dir \
    fastapi==0.115.0 \
    "uvicorn[standard]==0.30.6" \
    python-multipart==0.0.9

WORKDIR /app
COPY app.py .

EXPOSE 7004
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "7004"]
