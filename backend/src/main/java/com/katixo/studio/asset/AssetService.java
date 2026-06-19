package com.katixo.studio.asset;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
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

    /** Store a user-uploaded image; dimensions are read from the bytes when possible. */
    @Transactional
    public Asset saveUpload(byte[] bytes, String mime, String originalFilename) {
        String resolvedMime = (mime != null && !mime.isBlank()) ? mime : "application/octet-stream";
        AssetType type = typeForMime(resolvedMime);

        Integer width = null;
        Integer height = null;
        if (type == AssetType.IMAGE) {
            try {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
                if (img != null) {
                    width = img.getWidth();
                    height = img.getHeight();
                }
            } catch (IOException ignored) {
                // Non-decodable image: store without dimensions.
            }
        }

        UUID id = UUID.randomUUID();
        String filename = id + extensionFor(resolvedMime, originalFilename);
        String key = storage.store(bytes, filename);
        Asset asset = new Asset(id, type, key, resolvedMime, width, height, null);
        return repository.save(asset);
    }

    @Transactional
    public Asset saveImage(byte[] bytes, String mime, UUID sourceJobId, Integer width, Integer height) {
        UUID id = UUID.randomUUID();
        String filename = id + extensionFor(mime, null);
        String key = storage.store(bytes, filename);
        Asset asset = new Asset(id, AssetType.IMAGE, key, mime, width, height, sourceJobId);
        return repository.save(asset);
    }

    /** Store the PNG output of a media edit (rembg/upscale), reading its dimensions. */
    @Transactional
    public Asset saveImageResult(byte[] bytes, UUID sourceJobId) {
        Integer width = null;
        Integer height = null;
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img != null) {
                width = img.getWidth();
                height = img.getHeight();
            }
        } catch (IOException ignored) {
            // Store without dimensions if undecodable.
        }
        return saveImage(bytes, "image/png", sourceJobId, width, height);
    }

    /** Store a generated video clip. */
    @Transactional
    public Asset saveVideo(byte[] bytes, String mime, UUID sourceJobId, Integer width, Integer height) {
        UUID id = UUID.randomUUID();
        String filename = id + extensionFor(mime, null);
        String key = storage.store(bytes, filename);
        Asset asset = new Asset(id, AssetType.VIDEO, key, mime, width, height, sourceJobId);
        return repository.save(asset);
    }

    /** Store a generated audio clip (e.g. TTS voiceover). */
    @Transactional
    public Asset saveAudio(byte[] bytes, String mime, UUID sourceJobId) {
        UUID id = UUID.randomUUID();
        String filename = id + extensionFor(mime, null);
        String key = storage.store(bytes, filename);
        Asset asset = new Asset(id, AssetType.AUDIO, key, mime, null, null, sourceJobId);
        return repository.save(asset);
    }

    /** Store a text result (e.g. a transcript JSON) as a {@code text} asset. */
    @Transactional
    public Asset saveText(byte[] bytes, String mime, UUID sourceJobId) {
        UUID id = UUID.randomUUID();
        String filename = id + extensionFor(mime, null);
        String key = storage.store(bytes, filename);
        Asset asset = new Asset(id, AssetType.TEXT, key, mime, null, null, sourceJobId);
        return repository.save(asset);
    }

    @Transactional(readOnly = true)
    public Optional<Asset> find(UUID id) {
        return repository.findById(id);
    }

    public byte[] readBytes(Asset asset) {
        return storage.read(asset.getFilePath());
    }

    /** Pick the asset kind from a MIME type (audio/* and video/* recognised). */
    private AssetType typeForMime(String mime) {
        if (mime.startsWith("video/")) {
            return AssetType.VIDEO;
        }
        if (mime.startsWith("audio/")) {
            return AssetType.AUDIO;
        }
        return AssetType.IMAGE;
    }

    private String extensionFor(String mime, String originalFilename) {
        if (mime != null) {
            switch (mime) {
                case "image/png":
                    return ".png";
                case "image/jpeg":
                    return ".jpg";
                case "image/webp":
                    return ".webp";
                case "video/mp4":
                    return ".mp4";
                case "audio/wav":
                case "audio/x-wav":
                    return ".wav";
                case "audio/mpeg":
                    return ".mp3";
                case "application/json":
                    return ".json";
                default:
                    break;
            }
        }
        if (originalFilename != null) {
            int dot = originalFilename.lastIndexOf('.');
            if (dot >= 0 && dot < originalFilename.length() - 1) {
                return originalFilename.substring(dot).toLowerCase();
            }
        }
        return "";
    }
}
