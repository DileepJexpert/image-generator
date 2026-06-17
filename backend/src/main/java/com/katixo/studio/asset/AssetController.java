package com.katixo.studio.asset;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assets")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    /** Multipart upload of a user image. Returns the new asset's metadata. */
    @PostMapping
    public AssetDto upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        Asset asset = assetService.saveUpload(
                file.getBytes(),
                file.getContentType(),
                file.getOriginalFilename());
        return AssetDto.from(asset);
    }

    /** Streams the raw asset bytes with the stored content type. */
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> get(@PathVariable UUID id) {
        return assetService.find(id)
                .map(asset -> ResponseEntity.ok()
                        .contentType(asset.getMime() != null
                                ? MediaType.parseMediaType(asset.getMime())
                                : MediaType.APPLICATION_OCTET_STREAM)
                        .body(assetService.readBytes(asset)))
                .orElse(ResponseEntity.notFound().build());
    }
}
