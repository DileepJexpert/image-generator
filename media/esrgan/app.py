"""Minimal FastAPI wrapper around Real-ESRGAN (upscaling).

Contract used by the backend EsrganClient:
  POST /upscale?scale=2|4   multipart field `file`  -> image/png (upscaled)
  GET  /health                                       -> {"status": "ok"}

The RealESRGAN_x4plus model is loaded once at startup. `outscale` controls the
effective factor (2 or 4); the network itself is x4 and is downsampled for x2.
"""
import io
import os

import numpy as np
from basicsr.archs.rrdbnet_arch import RRDBNet
from fastapi import FastAPI, File, Query, UploadFile
from fastapi.responses import JSONResponse, Response
from PIL import Image
from realesrgan import RealESRGANer

MODEL_PATH = os.environ.get("ESRGAN_MODEL", "weights/RealESRGAN_x4plus.pth")

app = FastAPI(title="katixo-esrgan")

_model = RRDBNet(
    num_in_ch=3, num_out_ch=3, num_feat=64, num_block=23, num_grow_ch=32, scale=4
)
_upsampler = RealESRGANer(
    scale=4,
    model_path=MODEL_PATH,
    model=_model,
    tile=256,
    tile_pad=10,
    pre_pad=0,
    half=True,
)


@app.get("/health")
def health():
    return JSONResponse({"status": "ok"})


@app.post("/upscale")
async def upscale(file: UploadFile = File(...), scale: int = Query(4)):
    if scale not in (2, 4):
        return JSONResponse({"error": "scale must be 2 or 4"}, status_code=400)
    img = np.array(Image.open(io.BytesIO(await file.read())).convert("RGB"))
    output, _ = _upsampler.enhance(img, outscale=scale)
    buf = io.BytesIO()
    Image.fromarray(output).save(buf, format="PNG")
    return Response(content=buf.getvalue(), media_type="image/png")
