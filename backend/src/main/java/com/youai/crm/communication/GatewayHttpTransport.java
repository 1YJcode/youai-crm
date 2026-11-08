package com.youai.crm.communication;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

/** Bounded HTTP transport shared by SMS and browser telephony adapters. */
final class GatewayHttpTransport {
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER).build();

    GatewayHttpTransport(ObjectMapper json) { this.json = json; }

    JsonNode exchange(HttpRequest request) throws Exception {
        HttpResponse<byte[]> response = http.send(request, info -> new LimitedResponse());
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            throw new IOException("通信网关返回异常状态");
        return json.readTree(response.body());
    }

    private static final class LimitedResponse implements HttpResponse.BodySubscriber<byte[]> {
        private final CompletableFuture<byte[]> result = new CompletableFuture<>();
        private final ByteArrayOutputStream body = new ByteArrayOutputStream();
        private Flow.Subscription subscription;

        @Override
        public CompletionStage<byte[]> getBody() { return result; }

        @Override
        public void onSubscribe(Flow.Subscription value) {
            subscription = value;
            subscription.request(1);
        }

        @Override
        public void onNext(List<ByteBuffer> buffers) {
            for (var buffer : buffers) {
                if (body.size() + buffer.remaining() > 65536) {
                    subscription.cancel();
                    result.completeExceptionally(new IOException("通信网关响应过大"));
                    return;
                }
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                body.writeBytes(bytes);
            }
            subscription.request(1);
        }

        @Override
        public void onError(Throwable error) { result.completeExceptionally(error); }

        @Override
        public void onComplete() { result.complete(body.toByteArray()); }
    }

}
