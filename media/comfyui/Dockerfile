# ComfyUI media sidecar (installed, not authored — see CLAUDE.md section 4).
# GPU image; the host needs nvidia-container-toolkit for the runtime.
FROM pytorch/pytorch:2.4.0-cuda12.1-cudnn9-runtime

ARG COMFYUI_REF=master

RUN apt-get update \
    && apt-get install -y --no-install-recommends git \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /opt
RUN git clone https://github.com/comfyanonymous/ComfyUI.git \
    && cd ComfyUI \
    && git checkout ${COMFYUI_REF} \
    && pip install --no-cache-dir -r requirements.txt

WORKDIR /opt/ComfyUI
EXPOSE 8188

# Models are bind-mounted at runtime (see docker-compose.yml) so the large
# checkpoints are not baked into the image. COMFY_ARGS injects launch flags
# (e.g. --lowvram for 8GB cards); shell form so the variable is expanded.
CMD ["sh", "-c", "python main.py --listen 0.0.0.0 --port 8188 ${COMFY_ARGS}"]
