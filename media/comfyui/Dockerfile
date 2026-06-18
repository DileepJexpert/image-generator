# ComfyUI media sidecar (installed, not authored — see CLAUDE.md section 4).
# GPU image; the host needs nvidia-container-toolkit for the runtime.
FROM pytorch/pytorch:2.4.0-cuda12.1-cudnn9-runtime

ARG COMFYUI_REF=master

RUN apt-get update \
    && apt-get install -y --no-install-recommends git ffmpeg \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /opt
RUN git clone https://github.com/comfyanonymous/ComfyUI.git \
    && cd ComfyUI \
    && git checkout ${COMFYUI_REF} \
    && pip install --no-cache-dir -r requirements.txt

# Custom nodes for image-to-video (LTX-Video). VideoHelperSuite provides
# VHS_VideoCombine (writes the MP4, needs the ffmpeg installed above);
# ComfyUI-LTXVideo provides the LTXV nodes. Installed into custom_nodes/.
RUN cd /opt/ComfyUI/custom_nodes \
    && git clone --depth 1 https://github.com/Kosinkadink/ComfyUI-VideoHelperSuite.git \
    && git clone --depth 1 https://github.com/Lightricks/ComfyUI-LTXVideo.git \
    && pip install --no-cache-dir -r ComfyUI-VideoHelperSuite/requirements.txt \
    && (pip install --no-cache-dir -r ComfyUI-LTXVideo/requirements.txt || true)

WORKDIR /opt/ComfyUI
EXPOSE 8188

# Models are bind-mounted at runtime (see docker-compose.yml) so the large
# checkpoints are not baked into the image. COMFY_ARGS injects launch flags
# (e.g. --lowvram for 8GB cards); shell form so the variable is expanded.
CMD ["sh", "-c", "python main.py --listen 0.0.0.0 --port 8188 ${COMFY_ARGS}"]
