package io.github.gk0wk.violet.network;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class NetworkRequestBuilder {
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

    private static final Gson gson = new Gson();
    private static final MediaType JSON_TYPE = MediaType.parse("application/json;charset=utf-8");
    private static final Consumer<Exception> defaultFailHandler = Throwable::printStackTrace;

    public final Request.Builder requestBuilder;
    public NetworkRequestBuilder() {
        this.requestBuilder = new Request.Builder();
    }
    public NetworkRequestBuilder(Request.Builder requestBuilder) {
        this.requestBuilder = requestBuilder;
    }
    public Request.Builder getRequestBuilder() {
        return this.requestBuilder;
    }

    private BiConsumer<Call, BiConsumer<Response, Exception>> handlerType;
    @Nonnull public NetworkRequestBuilder sync() {
        this.handlerType = syncHandler;
        return this;
    }
    @Nonnull public NetworkRequestBuilder async() {
        this.handlerType = asyncHandler;
        return this;
    }

    @Nonnull NetworkRequestBuilder url(String url) {
        this.requestBuilder.url(url);
        return this;
    }

    @Nonnull NetworkRequestBuilder url(URL url) {
        this.requestBuilder.url(url);
        return this;
    }

    @Nonnull NetworkRequestBuilder url(HttpUrl url) {
        this.requestBuilder.url(url);
        return this;
    }

    @Nonnull NetworkRequestBuilder get() {
        this.requestBuilder.get();
        return this;
    }

    @Nonnull NetworkRequestBuilder head() {
        this.requestBuilder.head();
        return this;
    }

    @Nonnull public NetworkRequestBuilder postJson(@NotNull JsonObject body) {
        this.requestBuilder.post(RequestBody.create(body.toString(), JSON_TYPE));
        return this;
    }

    @Nonnull public NetworkRequestBuilder postJson(@NotNull JsonArray body) {
        this.requestBuilder.post(RequestBody.create(body.toString(), JSON_TYPE));
        return this;
    }

    public <T> void run(@NotNull Class<T> preprocessType,
                        @NotNull BiConsumer<Response, Object> onSuccess,
                        @Nullable Consumer<Exception> onFail) {
        this.handlerType.accept(client.newCall(this.requestBuilder.build()), (response, exception) -> {
            if (exception != null) {
                if (onFail == null) {
                    defaultFailHandler.accept(exception);
                } else {
                    onFail.accept(exception);
                }
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
                if (onFail == null) {
                    defaultFailHandler.accept(e);
                } else {
                    onFail.accept(e);
                }
            }
        });
    }
}