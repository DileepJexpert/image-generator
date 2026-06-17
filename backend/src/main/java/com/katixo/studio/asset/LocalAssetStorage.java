package com.katixo.studio.asset;

import com.katixo.studio.config.KatixoProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Stores asset bytes as files under a configured base directory. The storage
 * key is the file name (relative to the base dir).
 */
@Component
public class LocalAssetStorage implements AssetStorage {

    private final Path basePath;

    public LocalAssetStorage(KatixoProperties properties) {
        this.basePath = Paths.get(properties.assetStoragePath());
    }

    @PostConstruct
    void ensureDir() {
        try {
            Files.createDirectories(basePath);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create asset storage dir: " + basePath, e);
        }
    }

    @Override
    public String store(byte[] data, String filename) {
        Path target = basePath.resolve(filename).normalize();
        if (!target.startsWith(basePath)) {
            throw new IllegalArgumentException("Invalid asset filename: " + filename);
        }
        try {
            Files.write(target, data);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store asset " + filename, e);
        }
        return filename;
    }

    @Override
    public byte[] read(String key) {
        Path target = basePath.resolve(key).normalize();
        if (!target.startsWith(basePath)) {
            throw new IllegalArgumentException("Invalid asset key: " + key);
        }
        try {
            return Files.readAllBytes(target);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read asset " + key, e);
        }
    }
}
