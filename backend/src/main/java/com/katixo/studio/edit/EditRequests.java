package com.katixo.studio.edit;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Request bodies for the edit endpoints (CLAUDE.md §6). */
public final class EditRequests {

    private EditRequests() {
    }

    /** {@code POST /edit/remove-bg}. */
    public record RemoveBgRequest(@NotNull UUID assetId) {
    }

    /** {@code POST /edit/upscale} — scale must be 2 or 4. */
    public record UpscaleRequest(@NotNull UUID assetId, int scale) {
    }
}
