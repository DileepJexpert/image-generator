package com.katixo.studio.media;

import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Builds a {@code multipart/form-data} body with one or more file parts for the
 * JDK {@link java.net.http.HttpClient} (which has no native multipart support).
 * Repeating a field name (e.g. {@code images}) sends a list to a FastAPI sidecar.
 */
public final class MultipartBody {

    /** One file part: form field name, filename, content type, and raw bytes. */
    public record Part(String fieldName, String filename, String contentType, byte[] data) {
    }

    final String boundary = "----katixo" + UUID.randomUUID().toString().replace("-", "");

    private final byte[] body;

    public MultipartBody(String fieldName, String filename, String contentType, byte[] data) {
        this(List.of(new Part(fieldName, filename, contentType, data)));
    }

    public MultipartBody(List<Part> parts) {
        List<byte[]> chunks = new ArrayList<>();
        for (Part p : parts) {
            chunks.add(("--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"" + p.fieldName() + "\"; filename=\"" + p.filename() + "\"\r\n"
                    + "Content-Type: " + p.contentType() + "\r\n\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            chunks.add(p.data());
            chunks.add("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        chunks.add(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        int total = chunks.stream().mapToInt(c -> c.length).sum();
        byte[] assembled = new byte[total];
        int offset = 0;
        for (byte[] chunk : chunks) {
            System.arraycopy(chunk, 0, assembled, offset, chunk.length);
            offset += chunk.length;
        }
        this.body = assembled;
    }

    public String contentType() {
        return "multipart/form-data; boundary=" + boundary;
    }

    public HttpRequest.BodyPublisher publisher() {
        return HttpRequest.BodyPublishers.ofByteArray(body);
    }
}
