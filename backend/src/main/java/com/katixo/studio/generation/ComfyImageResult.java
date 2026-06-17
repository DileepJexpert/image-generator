package com.katixo.studio.generation;

/** Raw output of a ComfyUI image generation: the PNG bytes + their mime. */
public record ComfyImageResult(byte[] bytes, String mime) {
}
