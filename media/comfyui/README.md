# ComfyUI sidecar

ComfyUI is **installed, not authored** (CLAUDE.md §4). The backend talks to it
only over HTTP at `http://comfyui:8188`.

## Models

Checkpoints are large and are **not** baked into the image. They are served from
a host **bind mount**: `media/comfyui/models/` on the host maps to
`/opt/ComfyUI/models` in the container (see `backend/docker-compose.yml`).
Generated files land in `media/comfyui/output/`. Both folders are kept in git
via `.gitkeep`, but their contents are gitignored.

Download a checkpoint into `media/comfyui/models/checkpoints/`. The
`text2img.json` workflow is model-agnostic (standard SD nodes), so it works with
either SD 1.5 or SDXL — pick based on your GPU.

**Default — SD 1.5** (light, fast, comfortable on an 8GB card). The backend asks
for `v1-5-pruned-emaonly.safetensors` (~4 GB):

```
media/comfyui/models/checkpoints/v1-5-pruned-emaonly.safetensors
```

Download it from Hugging Face
`stable-diffusion-v1-5/stable-diffusion-v1-5`
(`v1-5-pruned-emaonly.safetensors`). SD 1.5 is native at 512px — use the 512²
presets in the image panel.

**Optional — SDXL** (higher quality, needs ~12GB for comfort). Place
`sd_xl_base_1.0.safetensors` (~6.5 GB, from
`stabilityai/stable-diffusion-xl-base-1.0`) in the same folder and select
"SDXL Base" in the panel (or pass `model` in the request).

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
