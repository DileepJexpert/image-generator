package com.katixo.studio.generation;

/** Raw output of a ComfyUI video generation: the encoded clip bytes + its mime. */
public record ComfyVideoResult(byte[] bytes, String mime) {
}
