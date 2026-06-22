package com.katixo.studio.media;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;

import static org.assertj.core.api.Assertions.assertThat;

/** Offline checks that the multipart body is well-formed for single and repeated-field uploads. */
class MultipartBodyTest {

    @Test
    void singlePartHasHeadersBoundaryAndData() {
        MultipartBody body = new MultipartBody("file", "input.png", "image/png",
                "PNGDATA".getBytes(StandardCharsets.UTF_8));
        String text = asText(body);

        assertThat(body.contentType()).startsWith("multipart/form-data; boundary=----katixo");
        assertThat(text).contains("name=\"file\"; filename=\"input.png\"");
        assertThat(text).contains("Content-Type: image/png");
        assertThat(text).contains("PNGDATA");
        assertThat(text).endsWith("--\r\n");
    }

    @Test
    void repeatedFieldNamesProduceAListOfParts() {
        MultipartBody body = new MultipartBody(List.of(
                new MultipartBody.Part("images", "img0.png", "image/png", "A".getBytes()),
                new MultipartBody.Part("images", "img1.png", "image/png", "B".getBytes()),
                new MultipartBody.Part("audios", "aud0.wav", "audio/wav", "C".getBytes())));
        String text = asText(body);

        assertThat(text).contains("name=\"images\"; filename=\"img0.png\"");
        assertThat(text).contains("name=\"images\"; filename=\"img1.png\"");
        assertThat(text).contains("name=\"audios\"; filename=\"aud0.wav\"");
        // Three opening boundaries (one per part) + one closing boundary = four boundary markers.
        assertThat(text.split("----katixo", -1).length - 1).isEqualTo(4);
    }

    /** Drain the body's {@link HttpRequest.BodyPublisher} to the assembled bytes, then to text. */
    private static String asText(MultipartBody body) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CountDownLatch done = new CountDownLatch(1);
        body.publisher().subscribe(new Flow.Subscriber<ByteBuffer>() {
            @Override
            public void onSubscribe(Flow.Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer buf) {
                byte[] chunk = new byte[buf.remaining()];
                buf.get(chunk);
                out.writeBytes(chunk);
            }

            @Override
            public void onError(Throwable t) {
                done.countDown();
            }

            @Override
            public void onComplete() {
                done.countDown();
            }
        });
        try {
            done.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return out.toString(StandardCharsets.UTF_8);
    }
}
