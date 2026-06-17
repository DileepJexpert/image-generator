package com.katixo.studio.asset;

/**
 * Abstraction over where asset bytes live. Local disk now; a MinIO/S3 impl can
 * drop in later without touching call sites (CLAUDE.md section 4). The returned
 * key is what gets persisted in {@code assets.file_path}.
 */
public interface AssetStorage {

    /** Store bytes under a generated key derived from {@code filename}; returns the storage key. */
    String store(byte[] data, String filename);

    /** Read previously stored bytes by their storage key. */
    byte[] read(String key);
}
