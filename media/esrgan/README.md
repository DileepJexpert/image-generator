# Real-ESRGAN sidecar

Image upscaling, **installed not authored** (CLAUDE.md §4). The backend calls it
at `http://esrgan:7001`.

## Contract

- `POST /upscale?scale=2|4` — multipart `file` field → `image/png` upscaled
- `GET /health` → `{"status":"ok"}`

## Notes

- GPU service (shares the NVIDIA GPU with ComfyUI). The `RealESRGAN_x4plus.pth`
  weights are fetched at image build time.
- `scale` 2 or 4; the x4 network is downsampled for x2 via `outscale`.
