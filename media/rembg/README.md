# rembg sidecar

Background removal, **installed not authored** (CLAUDE.md §4). The backend calls
it at `http://rembg:7000`.

## Contract

- `POST /remove` — multipart `file` field → `image/png` cutout (RGBA)
- `GET /health` → `{"status":"ok"}`

## Notes

- Runs on CPU to keep the 12GB GPU free for ComfyUI.
- The u2net model (~170MB) downloads on first request; the
  `docker-compose.yml` mounts a volume at `/root/.u2net` to persist it.
