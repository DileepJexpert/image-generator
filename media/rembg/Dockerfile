# rembg background-removal sidecar (CPU; keeps the GPU free for ComfyUI).
FROM python:3.11-slim

RUN apt-get update \
    && apt-get install -y --no-install-recommends libgl1 libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

RUN pip install --no-cache-dir \
    "rembg[cpu]==2.0.59" \
    fastapi==0.115.0 \
    "uvicorn[standard]==0.30.6" \
    python-multipart==0.0.9

WORKDIR /app
COPY app.py .

# u2net model downloads on first request; mount /root/.u2net to persist it.
EXPOSE 7000
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "7000"]
