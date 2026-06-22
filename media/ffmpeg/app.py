"""Minimal FastAPI wrapper around ffmpeg — assembles a narrated slideshow video.

Contract used by the backend VideoComposeClient:
  POST /compose   multipart: repeated `images` + repeated `audios` (aligned by order)
                  -> video/mp4
  GET  /health                                                   -> {"status": "ok"}

Each image is shown for the duration of its paired audio clip; the per-section
segments are concatenated into one MP4 with the narration as the audio track.
CPU-only — keeps the GPU free for ComfyUI.
"""
import os
import shutil
import subprocess
import tempfile

from fastapi import FastAPI, File, UploadFile
from fastapi.responses import JSONResponse, Response

app = FastAPI(title="katixo-ffmpeg")

WIDTH = int(os.environ.get("VIDEO_WIDTH", "1280"))
HEIGHT = int(os.environ.get("VIDEO_HEIGHT", "720"))
FPS = int(os.environ.get("VIDEO_FPS", "25"))


@app.get("/health")
def health():
    return JSONResponse({"status": "ok"})


def _duration(path: str) -> float:
    """Audio length in seconds (floored to 0.5s so a slide is never zero-length)."""
    out = subprocess.run(
        ["ffprobe", "-v", "error", "-show_entries", "format=duration",
         "-of", "default=noprint_wrappers=1:nokey=1", path],
        capture_output=True, text=True, check=True,
    )
    try:
        return max(0.5, float(out.stdout.strip()))
    except ValueError:
        return 3.0


@app.post("/compose")
async def compose(images: list[UploadFile] = File(...), audios: list[UploadFile] = File(...)):
    if not images:
        return Response(content="no images", status_code=400)
    if len(images) != len(audios):
        return Response(content="images and audios count mismatch", status_code=400)

    work = tempfile.mkdtemp(prefix="katixo-vid-")
    # Fit each image into the frame without distortion, pad to size, force even/compatible output.
    vf = (f"scale={WIDTH}:{HEIGHT}:force_original_aspect_ratio=decrease,"
          f"pad={WIDTH}:{HEIGHT}:(ow-iw)/2:(oh-ih)/2,setsar=1,format=yuv420p")
    try:
        segments = []
        for i, (img, aud) in enumerate(zip(images, audios)):
            img_path = os.path.join(work, f"img{i}")
            aud_path = os.path.join(work, f"aud{i}")
            with open(img_path, "wb") as f:
                f.write(await img.read())
            with open(aud_path, "wb") as f:
                f.write(await aud.read())

            seg = os.path.join(work, f"seg{i}.mp4")
            subprocess.run(
                ["ffmpeg", "-y", "-loop", "1", "-i", img_path, "-i", aud_path,
                 "-t", f"{_duration(aud_path):.3f}", "-r", str(FPS), "-vf", vf,
                 "-c:v", "libx264", "-pix_fmt", "yuv420p",
                 "-c:a", "aac", "-b:a", "128k", "-ar", "44100", "-ac", "2",
                 "-shortest", seg],
                check=True, capture_output=True,
            )
            segments.append(seg)

        listfile = os.path.join(work, "list.txt")
        with open(listfile, "w") as f:
            for seg in segments:
                f.write(f"file '{seg}'\n")

        out = os.path.join(work, "out.mp4")
        # Re-encode on concat (not -c copy) so mismatched segment params can't break playback.
        subprocess.run(
            ["ffmpeg", "-y", "-f", "concat", "-safe", "0", "-i", listfile,
             "-c:v", "libx264", "-pix_fmt", "yuv420p", "-c:a", "aac", "-b:a", "128k",
             "-movflags", "+faststart", out],
            check=True, capture_output=True,
        )
        with open(out, "rb") as f:
            data = f.read()
        return Response(content=data, media_type="video/mp4")
    except subprocess.CalledProcessError as e:
        detail = (e.stderr or b"").decode("utf-8", "ignore")[-500:]
        return Response(content=f"ffmpeg failed: {detail}", status_code=500)
    finally:
        shutil.rmtree(work, ignore_errors=True)
