package com.dokanhisab.app.data.remote

import android.util.Log
import com.dokanhisab.app.data.local.ChatMessageEntity
import com.dokanhisab.app.data.preferences.OpenRouterPresetModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val TAG = "OpenRouterApiClient"
private const val OPENROUTER_CHAT_URL = "https://openrouter.ai/api/v1/chat/completions"
private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

class OpenRouterApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun buildChatPayload(
        modelId: String,
        systemInstruction: String,
        history: List<ChatMessageEntity>,
        userMessage: String
    ): JSONObject {
        val messagesArray = JSONArray()
        if (systemInstruction.isNotBlank()) {
            messagesArray.put(
                JSONObject().apply {
                    put("role", "system")
                    put("content", systemInstruction)
                }
            )
        }
        history.takeLast(24).forEach { msg ->
            messagesArray.put(
                JSONObject().apply {
                    put("role", if (msg.role == "model" || msg.role == "assistant") "assistant" else "user")
                    put("content", msg.text)
                }
            )
        }
        messagesArray.put(
            JSONObject().apply {
                put("role", "user")
                put("content", userMessage)
            }
        )

        val resolvedModel = modelId.trim().ifBlank { OpenRouterPresetModel.GEMINI_2_5_FLASH.modelId }
        return JSONObject().apply {
            put("model", resolvedModel)
            put("stream", true)
            put("messages", messagesArray)
        }
    }

    fun streamChatMessage(
        apiKey: String,
        modelId: String,
        systemInstruction: String,
        history: List<ChatMessageEntity>,
        userMessage: String
    ): Flow<String> = flow {
        val trimmedKey = apiKey.trim()
        require(trimmedKey.isNotEmpty()) { "OpenRouter API key is missing." }

        val payload = buildChatPayload(
            modelId = modelId,
            systemInstruction = systemInstruction,
            history = history,
            userMessage = userMessage
        )

        val request = Request.Builder()
            .url(OPENROUTER_CHAT_URL)
            .header("Authorization", "Bearer $trimmedKey")
            .header("HTTP-Referer", "https://dokanhisab.local")
            .header("X-Title", "Dokan Hisab")
            .header("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(JSON_MEDIA))
            .build()

        withTimeout(60_000L) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string().orEmpty()
                    val msg = parseApiError(response.code, errBody)
                    Log.e(TAG, "OpenRouter chat stream failed (${response.code}): $msg")
                    throw IllegalStateException(msg)
                }

                val source = response.body?.source()
                    ?: throw IllegalStateException("Empty response stream from OpenRouter.")

                var emittedAny = false
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    if (line.startsWith("data:")) {
                        val jsonStr = line.removePrefix("data:").trim()
                        if (jsonStr.isEmpty() || jsonStr == "[DONE]") {
                            continue
                        }
                        val streamErr = extractInlineStreamError(jsonStr)
                        if (streamErr != null) {
                            throw IllegalStateException(streamErr)
                        }
                        val chunkText = extractChunkText(jsonStr)
                        if (chunkText.isNotEmpty()) {
                            emittedAny = true
                            emit(chunkText)
                        }
                    }
                }
                if (!emittedAny) {
                    throw IllegalStateException("OpenRouter returned an empty response.")
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    fun extractChunkText(rawJson: String): String {
        return runCatching {
            val root = JSONObject(rawJson)
            val choices = root.optJSONArray("choices") ?: return ""
            val sb = StringBuilder()
            for (i in 0 until choices.length()) {
                val choice = choices.optJSONObject(i) ?: continue
                val delta = choice.optJSONObject("delta")
                if (delta != null && !delta.isNull("content")) {
                    val content = delta.optString("content", "")
                    if (content.isNotEmpty()) {
                        sb.append(content)
                    }
                } else {
                    val message = choice.optJSONObject("message")
                    if (message != null && !message.isNull("content")) {
                        val content = message.optString("content", "")
                        if (content.isNotEmpty()) {
                            sb.append(content)
                        }
                    }
                }
            }
            sb.toString()
        }.getOrDefault("")
    }

    private fun extractInlineStreamError(rawJson: String): String? {
        return runCatching {
            val root = JSONObject(rawJson)
            val errObj = root.optJSONObject("error") ?: return null
            errObj.optString("message").takeIf { it.isNotBlank() } ?: "OpenRouter stream error."
        }.getOrNull()
    }

    fun parseApiError(statusCode: Int, rawBody: String): String {
        val parsedMessage = runCatching {
            val root = JSONObject(rawBody)
            root.optJSONObject("error")?.optString("message")
                ?.takeIf { it.isNotBlank() }
                ?: root.optString("message").orEmpty()
        }.getOrDefault("")

        return when {
            statusCode == 401 || statusCode == 403 ->
                parsedMessage.ifBlank { "Unauthorized (HTTP $statusCode). Verify your OpenRouter API key in Settings." }
            statusCode == 402 ->
                parsedMessage.ifBlank { "Insufficient OpenRouter credits (HTTP 402). Try a ':free' model or top up credits." }
            statusCode == 429 ->
                parsedMessage.ifBlank { "OpenRouter rate limit reached (HTTP 429). Please wait and try again." }
            statusCode == 404 ->
                parsedMessage.ifBlank { "Selected OpenRouter model was not found (HTTP 404)." }
            parsedMessage.isNotBlank() -> parsedMessage
            else -> "OpenRouter API error (HTTP $statusCode)."
        }
    }
}
