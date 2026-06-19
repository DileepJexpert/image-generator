# Piper text-to-speech sidecar (CPU; keeps the GPU free for ComfyUI).
FROM python:3.11-slim

RUN apt-get update \
    && apt-get install -y --no-install-recommends wget \
    && rm -rf /var/lib/apt/lists/*

RUN pip install --no-cache-dir \
    piper-tts==1.2.0 \
    fastapi==0.115.0 \
    "uvicorn[standard]==0.30.6"

# Bundle one en_US voice (amy, medium quality). Add more by dropping extra
# .onnx + .onnx.json pairs into /voices and selecting them via the `voice` field.
ENV VOICES_DIR=/voices
RUN mkdir -p /voices \
    && wget -qO /voices/en_US-amy-medium.onnx \
        https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/amy/medium/en_US-amy-medium.onnx \
    && wget -qO /voices/en_US-amy-medium.onnx.json \
        https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/amy/medium/en_US-amy-medium.onnx.json

WORKDIR /app
COPY app.py .

EXPOSE 7002
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "7002"]
