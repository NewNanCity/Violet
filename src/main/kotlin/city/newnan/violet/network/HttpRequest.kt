package city.newnan.violet.network

import city.newnan.violet.config.ConfigManager2
import com.fasterxml.jackson.databind.JsonNode
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request.Builder
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

private val JSON_TYPE: MediaType = "application/json;charset=utf-8".toMediaType()
infix fun Builder.post(body: JsonNode)
    = post(body.toString().toRequestBody(JSON_TYPE))
infix fun Builder.header(pair: Pair<String, String>)
    = header(pair.first, pair.second)

/**
 * DSL 包装后的 OkHttp
 *
 * 用起来非常舒服
 */
class HttpRequest {
    private val requestBuilder: Builder
    infix fun request(block: Builder.() -> Unit) = this.also { block(it.requestBuilder) }

    constructor(block: Builder.() -> Unit) {
        requestBuilder = Builder()
        block(requestBuilder)
    }

    constructor(requestBuilder: Builder, block: Builder.() -> Unit) {
        this.requestBuilder = requestBuilder
        block(requestBuilder)
    }

    val request: Request
        get() = requestBuilder.build()

    infix fun sync(block: HttpRespondHandler.() -> Unit)
        = this.also { HttpRespondHandler(false, block).run(client.newCall(request)) }
    infix fun async(block: HttpRespondHandler.() -> Unit)
        = this.also { HttpRespondHandler(true, block).run(client.newCall(request)) }

    companion object {
        private val client: OkHttpClient = OkHttpClient.Builder()
            .connectionSpecs(
                listOf(
                    ConnectionSpec.MODERN_TLS,
                    ConnectionSpec.COMPATIBLE_TLS,
                    ConnectionSpec.CLEARTEXT
                )
            ).build()
        fun withClient(block: OkHttpClient.() -> Unit) = block(client)
    }
}

enum class PreprocessType {
    RAW,
    JSON_ARRAY,
    JSON_OBJECT
}

class HttpRespondHandler internal constructor(async: Boolean, block: HttpRespondHandler.() -> Unit) {
    private val handlerMode: (Call, (Response?, Exception?) -> Unit) -> Unit
    private lateinit var preprocessType: PreprocessType
    infix fun preprocessType(type: PreprocessType) = this.also { it.preprocessType = preprocessType }
    private lateinit var onSuccess: (Response?, Any?) -> Unit
    infix fun success(handler: (Response?, Any?) -> Unit) = this.also { it.onSuccess = handler }
    private var onFail: (Exception) -> Unit = { it.printStackTrace() }
    infix fun fail(handler: (Exception) -> Unit) = this.also { it.onFail = handler }

    init {
        handlerMode = if (async) asyncHandler else syncHandler
        block(this)
    }

    internal fun run(call: Call) {
        handlerMode(call) { response, exception ->
            if (exception != null) {
                onFail(exception)
            } else {
                try {
                    when(preprocessType) {
                        PreprocessType.RAW -> onSuccess(response, response)
                        PreprocessType.JSON_ARRAY -> onSuccess(
                            response, ConfigManager2.parse<JsonNode>(response?.body?.string() ?: "[]", ConfigManager2.ConfigFileType.Json)
                        )
                        PreprocessType.JSON_OBJECT -> onSuccess(
                            response, ConfigManager2.parse<JsonNode>(response?.body?.string() ?: "{}", ConfigManager2.ConfigFileType.Json)
                        )
                    }
                } catch (e: IOException) {
                    onFail(e)
                }
            }
        }
    }

    companion object {
        private val syncHandler: (Call, (Response?, Exception?) -> Unit) -> Unit = {
                networkCall, responseHandler ->
            try {
                val response = networkCall.execute()
                if (!response.isSuccessful) throw IOException("Call failed! Unexpected code $response")
                responseHandler(response, null)
            } catch (e: IOException) {
                responseHandler(null, e)
            }
        }
        private val asyncHandler: (Call, (Response?, Exception?) -> Unit) -> Unit = {
                networkCall, responseHandler ->
            networkCall.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) = responseHandler(null, e)
                override fun onResponse(call: Call, response: Response) {
                    try {
                        if (!response.isSuccessful) throw IOException("Call failed! Unexpected code $response")
                        responseHandler(response, null)
                    } catch (e: IOException) {
                        responseHandler(null, e)
                    }
                }
            })
        }
    }
}