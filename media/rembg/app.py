"""Minimal FastAPI wrapper around rembg (background removal).

Contract used by the backend RembgClient:
  POST /remove   multipart field `file`  -> image/png (cutout, RGBA)
  GET  /health                            -> {"status": "ok"}
"""
from fastapi import FastAPI, File, UploadFile
from fastapi.responses import JSONResponse, Response
from rembg import remove

app = FastAPI(title="katixo-rembg")


@app.get("/health")
def health():
    return JSONResponse({"status": "ok"})


@app.post("/remove")
async def remove_bg(file: UploadFile = File(...)):
    data = await file.read()
    output = remove(data)  # returns PNG bytes with alpha
    return Response(content=output, media_type="image/png")
