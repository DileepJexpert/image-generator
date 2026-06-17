# ComfyUI sidecar

ComfyUI is **installed, not authored** (CLAUDE.md §4). The backend talks to it
only over HTTP at `http://comfyui:8188`.

## Models

Checkpoints are large and are **not** baked into the image. Mount them from the
host into the container's `models/` directory (wired in `docker-compose.yml` via
the `comfyui_models` volume / a host bind mount).

For the `text2img_sdxl.json` workflow, drop an SDXL checkpoint into
`models/checkpoints/`. The default the backend asks for is:

```
sd_xl_base_1.0.safetensors
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

## GPU

The service requires the host to have `nvidia-container-toolkit` installed so
the container can reserve the NVIDIA GPU (see the `deploy.resources` block in
`docker-compose.yml`). On a machine without a GPU, start the rest of the stack
without this service:

```
docker compose up postgres redis backend frontend
```
