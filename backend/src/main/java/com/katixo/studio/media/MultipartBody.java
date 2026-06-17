package com.katixo.studio.media;

import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Builds a {@code multipart/form-data} body with a single file part for the
 * JDK {@link java.net.http.HttpClient} (which has no native multipart support).
 */
final class MultipartBody {

    final String boundary = "----katixo" + UUID.randomUUID().toString().replace("-", "");

    private final byte[] body;

    MultipartBody(String fieldName, String filename, String contentType, byte[] data) {
        List<byte[]> parts = new ArrayList<>();
        parts.add(("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: " + contentType + "\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8));
        parts.add(data);
        parts.add(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        int total = parts.stream().mapToInt(p -> p.length).sum();
        byte[] assembled = new byte[total];
        int offset = 0;
        for (byte[] part : parts) {
            System.arraycopy(part, 0, assembled, offset, part.length);
            offset += part.length;
        }
        this.body = assembled;
    }

    String contentType() {
        return "multipart/form-data; boundary=" + boundary;
    }

    HttpRequest.BodyPublisher publisher() {
        return HttpRequest.BodyPublishers.ofByteArray(body);
    }
}
