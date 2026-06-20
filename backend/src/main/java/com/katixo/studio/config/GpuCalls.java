package com.katixo.studio.config;

import com.katixo.ai.commons.gpu.GpuResourceGuard;

import java.io.IOException;

/**
 * Small adapter that runs a GPU sidecar call under the shared {@link GpuResourceGuard} while
 * preserving this app's checked-exception convention ({@code IOException} / {@code InterruptedException}).
 * The sidecar clients here use the JDK HttpClient and declare those checked exceptions; the guard
 * declares the broader {@code Exception}, so this re-narrows it.
 */
public final class GpuCalls {

    private GpuCalls() {
    }

    public static <T> T guarded(GpuResourceGuard guard, String label, GpuResourceGuard.GpuTask<T> task)
            throws IOException, InterruptedException {
        try {
            return guard.runExclusively(label, task);
        } catch (IOException | InterruptedException | RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Guarded GPU call '" + label + "' failed", e);
        }
    }
}
