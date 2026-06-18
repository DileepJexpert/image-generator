# Real-ESRGAN upscale sidecar (GPU).
FROM pytorch/pytorch:2.4.0-cuda12.1-cudnn9-runtime

RUN apt-get update \
    && apt-get install -y --no-install-recommends wget libgl1 libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

RUN pip install --no-cache-dir \
    realesrgan==0.3.0 \
    basicsr==1.4.2 \
    fastapi==0.115.0 \
    "uvicorn[standard]==0.30.6" \
    python-multipart==0.0.9 \
    pillow numpy

# basicsr 1.4.2 imports `torchvision.transforms.functional_tensor`, which was
# removed in torchvision >= 0.17 (this base ships 0.19), so Real-ESRGAN fails to
# import. Patch the import to its current location (rgb_to_grayscale still lives
# in torchvision.transforms.functional).
RUN python -c "import os,basicsr;p=os.path.join(os.path.dirname(basicsr.__file__),'data','degradations.py');s=open(p).read().replace('torchvision.transforms.functional_tensor','torchvision.transforms.functional');open(p,'w').write(s)"

WORKDIR /app
COPY app.py .

# Fetch the x4plus weights at build time.
RUN mkdir -p weights && wget -q -O weights/RealESRGAN_x4plus.pth \
    https://github.com/xinntao/Real-ESRGAN/releases/download/v0.1.0/RealESRGAN_x4plus.pth

EXPOSE 7001
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "7001"]
