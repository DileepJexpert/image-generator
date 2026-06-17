package com.katixo.studio.asset;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/** Persists asset bytes (via {@link AssetStorage}) plus their metadata row. */
@Service
public class AssetService {

    private final AssetRepository repository;
    private final AssetStorage storage;

    public AssetService(AssetRepository repository, AssetStorage storage) {
        this.repository = repository;
        this.storage = storage;
    }

    @Transactional
    public Asset saveImage(byte[] bytes, String mime, UUID sourceJobId, Integer width, Integer height) {
        UUID id = UUID.randomUUID();
        String filename = id + extensionFor(mime);
        String key = storage.store(bytes, filename);
        Asset asset = new Asset(id, AssetType.IMAGE, key, mime, width, height, sourceJobId);
        return repository.save(asset);
    }

    @Transactional(readOnly = true)
    public Optional<Asset> find(UUID id) {
        return repository.findById(id);
    }

    public byte[] readBytes(Asset asset) {
        return storage.read(asset.getFilePath());
    }

    private String extensionFor(String mime) {
        if (mime == null) {
            return "";
        }
        return switch (mime) {
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            case "image/webp" -> ".webp";
            case "video/mp4" -> ".mp4";
            default -> "";
        };
    }
}
