# ComfyUI sidecar

ComfyUI is **installed, not authored** (CLAUDE.md §4). The backend talks to it
only over HTTP at `http://comfyui:8188`.

## Models

Checkpoints are large and are **not** baked into the image. They are served from
a host **bind mount**: `media/comfyui/models/` on the host maps to
`/opt/ComfyUI/models` in the container (see `backend/docker-compose.yml`).
Generated files land in `media/comfyui/output/`. Both folders are kept in git
via `.gitkeep`, but their contents are gitignored.

For the `text2img_sdxl.json` workflow, download an SDXL checkpoint into
`media/comfyui/models/checkpoints/`. The default the backend asks for is
`sd_xl_base_1.0.safetensors` — get it from Hugging Face
`stabilityai/stable-diffusion-xl-base-1.0` (~6.5 GB) and place it at:

```
media/comfyui/models/checkpoints/sd_xl_base_1.0.safetensors
```

Override the checkpoint per request with the `model` field of
`POST /api/v1/generate/image`.

### Image-to-video (`img2video_ltx.json`)

The video workflow targets LTX-Video and needs these custom nodes installed in
ComfyUI, plus the LTX checkpoint:

- [ComfyUI-LTXVideo](https://github.com/Lightricks/ComfyUI-LTXVideo) (provides
  `LTXVImgToVideo`)
- [ComfyUI-VideoHelperSuite](https://github.com/Kosinkadink/ComfyUI-VideoHelperSuite)
  (provides `VHS_VideoCombine`, which writes the MP4)
- checkpoint `ltx-video-2b-v0.9.5.safetensors` in `models/checkpoints/`

## GPU & VRAM

The service requires the host to have `nvidia-container-toolkit` installed so
the container can reserve the NVIDIA GPU (see the `deploy.resources` block in
`docker-compose.yml`). On a machine without a GPU, start the rest of the stack
without this service:

```
docker compose up postgres redis backend frontend
```

**8 GB cards (e.g. RTX 4060):** the compose service sets `COMFY_ARGS=--lowvram`
so SDXL fits by offloading aggressively (slower but stable). On a 12 GB+ card you
can drop the flag — set `COMFY_ARGS=""` (or `--normalvram`) in
`backend/docker-compose.yml` for more speed.
