package io.github.gk0wk.violet.network;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class NetworkManager {
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectionSpecs(Arrays.asList(
                    ConnectionSpec.MODERN_TLS,
                    ConnectionSpec.COMPATIBLE_TLS,
                    ConnectionSpec.CLEARTEXT
            ))
            .build();

    private static final BiConsumer<Call, BiConsumer<Response, Exception>> syncHandler =
            (networkCall, responseHandler) -> {
        try {
            Response response = networkCall.execute();
            if (!response.isSuccessful())
                throw new IOException("Call failed! Unexpected code " + response);
            responseHandler.accept(response, null);
        } catch (IOException e) {
            responseHandler.accept(null, e);
        }
    };

    private static final BiConsumer<Call, BiConsumer<Response, Exception>> asyncHandler =
            (networkCall, responseHandler) -> networkCall.enqueue(new Callback() {
        @Override public void onFailure(@NotNull Call call, @NotNull IOException e) {
            responseHandler.accept(null, e);
        }

        @Override public void onResponse(@NotNull Call call, @NotNull Response response){
            try {
                if (!response.isSuccessful())
                    throw new IOException("Call failed! Unexpected code " + response);
                responseHandler.accept(response, null);
            } catch (IOException e) {
                responseHandler.accept(null, e);
            }
        }
    });

    public static class HandlerBuilder {
        private static final Gson gson = new Gson();
        private static final MediaType JSON_TYPE = MediaType.parse("application/json;charset=utf-8");

        public final Request.Builder requestBuilder;
        private BiConsumer<Call, BiConsumer<Response, Exception>> handlerType;
        private Consumer<Exception> onFail = Throwable::printStackTrace;

        public HandlerBuilder() {
            this.requestBuilder = new Request.Builder();
        }

        public HandlerBuilder(Request.Builder requestBuilder) {
            this.requestBuilder = requestBuilder;
        }

        public Request.Builder getRequestBuilder() {
            return this.requestBuilder;
        }

        public HandlerBuilder sync() {
            this.handlerType = NetworkManager.syncHandler;
            return this;
        }

        public HandlerBuilder async() {
            this.handlerType = NetworkManager.asyncHandler;
            return this;
        }

        public HandlerBuilder postJson(JsonObject body) {
            this.requestBuilder.post(RequestBody.create(body.toString(), JSON_TYPE));
            return this;
        }

        public HandlerBuilder postJson(JsonArray body) {
            this.requestBuilder.post(RequestBody.create(body.toString(), JSON_TYPE));
            return this;
        }

        public HandlerBuilder handleFail(Consumer<Exception> onFail) {
            this.onFail = onFail;
            return this;
        }

        public <T> void run(Class<T> preprocessType, BiConsumer<Response, Object> onSuccess) {
            this.handlerType.accept(NetworkManager.client.newCall(this.requestBuilder.build()), (response, exception) -> {
                if (exception != null) {
                    onFail.accept(exception);
                }
                try {
                    if (Response.class.equals(preprocessType)) {
                        onSuccess.accept(response, response);
                    } else if (JsonObject.class.equals(preprocessType)) {
                        onSuccess.accept(response,
                                gson.fromJson(Objects.requireNonNull(response.body()).string(), JsonObject.class));
                    } else {
                        onSuccess.accept(response, null);
                    }
                } catch (IOException e) {
                    onFail.accept(e);
                }
            });
        }
    }
}
