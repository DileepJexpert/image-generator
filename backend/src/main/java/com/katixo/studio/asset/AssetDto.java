package com.katixo.studio.asset;

import java.util.UUID;

/** Metadata returned after an upload / for client references. */
public record AssetDto(
        UUID id,
        String type,
        String mime,
        Integer width,
        Integer height
) {
    public static AssetDto from(Asset asset) {
        return new AssetDto(
                asset.getId(),
                asset.getType().value(),
                asset.getMime(),
                asset.getWidth(),
                asset.getHeight());
    }
}
