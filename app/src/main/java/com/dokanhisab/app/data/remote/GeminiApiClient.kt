package com.dokanhisab.app.data.remote

import android.util.Base64
import android.util.Log
import com.dokanhisab.app.data.local.ChatMessageEntity
import com.dokanhisab.app.data.preferences.AppLanguage
import com.dokanhisab.app.data.preferences.ChatRole
import com.dokanhisab.app.data.preferences.GeminiChatModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

private const val TAG = "GeminiApiClient"
private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
private const val MODEL_TRANSCRIBE = "gemini-3.5-transcribe"
private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

class GeminiApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun buildSystemInstruction(
        role: ChatRole,
        customInstruction: String,
        shopName: String,
        language: AppLanguage,
        ledgerSummaryContext: String
    ): String {
        val baseRoleInstruction = when (role) {
            ChatRole.SHOP_ACCOUNTANT ->
                "You are Dokan Hisab's expert South Asian shop accountant and bookkeeper. " +
                    "Help the shopkeeper with daily income/expense tracking, profit & loss calculations, " +
                    "cash-in-hand reconciliation, due/baki follow-ups, and pricing math in Bangladeshi Taka (৳ BDT). " +
                    "Keep answers practical, structured, and easy for everyday traders to act on."
            ChatRole.BUSINESS_ADVISOR ->
                "You are a practical small-business advisor for neighborhood shops and local traders. " +
                    "Analyze sales vs expenses, suggest inventory restocking priorities, cash-flow improvements, " +
                    "and cost-saving strategies tailored to local retail trade in Taka (৳ BDT)."
            ChatRole.MEMO_WRITER ->
                "You are a bilingual Bangla-English shop communication and memo writer. " +
                    "Draft polite customer due reminders, supplier purchase orders, invoice summaries, " +
                    "and clear translations between Bangla and English."
            ChatRole.CUSTOM ->
                customInstruction.ifBlank {
                    "You are a practical shop accountant and business advisor for Dokan Hisab."
                }
        }
        val langHint = if (language == AppLanguage.BANGLA) {
            "Respond primarily in natural, clear Bangla (বাংলা) unless the user writes in English or requests English."
        } else {
            "Respond primarily in clear, concise English unless the user writes in Bangla."
        }
        val shopCtx = if (shopName.isNotBlank()) "Shop Name: $shopName." else ""
        return "$baseRoleInstruction $langHint $shopCtx Current Shop Ledger Snapshot: $ledgerSummaryContext"
    }

    suspend fun transcribeAudio(
        apiKey: String,
        audioFile: File,
        language: AppLanguage
    ): String = withContext(Dispatchers.IO) {
        val trimmedKey = apiKey.trim()
        require(trimmedKey.isNotEmpty()) { "Gemini API key is missing." }
        require(audioFile.exists() && audioFile.length() > 0L) { "Recorded audio file is empty." }

        val audioBytes = audioFile.readBytes()
        require(audioBytes.size <= 19 * 1024 * 1024) { "Audio recording exceeds 19 MB limit." }
        val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)

        val prompt = if (language == AppLanguage.BANGLA) {
            "Transcribe this spoken audio accurately in its original spoken language (Bangla or English). " +
                "Preserve numbers and amounts clearly. Return only the clean transcription text without extra commentary."
        } else {
            "Transcribe this spoken audio accurately in its original spoken language (English or Bangla). " +
                "Preserve numbers and amounts clearly. Return only the clean transcription text without extra commentary."
        }

        val payload = JSONObject().apply {
            put(
                "contents",
                JSONArray().put(
                    JSONObject().apply {
                        put("role", "user")
                        put(
                            "parts",
                            JSONArray()
                                .put(JSONObject().put("text", prompt))
                                .put(
                                    JSONObject().put(
                                        "inlineData",
                                        JSONObject().apply {
                                            put("mimeType", "audio/mp4")
                                            put("data", base64Audio)
                                        }
                                    )
                                )
                        )
                    }
                )
            )
        }

        val request = Request.Builder()
            .url("$BASE_URL/$MODEL_TRANSCRIBE:generateContent")
            .header("x-goog-api-key", trimmedKey)
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        withTimeout(60_000L) {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val msg = parseApiError(response.code, bodyString)
                    Log.e(TAG, "Transcription failed (${response.code}): $msg")
                    throw IllegalStateException(msg)
                }
                val text = extractTextFromResponse(bodyString).trim()
                if (text.isEmpty()) {
                    throw IllegalStateException("Transcription returned empty text.")
                }
                text
            }
        }
    }

    fun streamChatMessage(
        apiKey: String,
        model: GeminiChatModel,
        systemInstruction: String,
        history: List<ChatMessageEntity>,
        userMessage: String
    ): Flow<String> = flow {
        val trimmedKey = apiKey.trim()
        require(trimmedKey.isNotEmpty()) { "Gemini API key is missing." }

        val contentsArray = JSONArray()
        // Include recent conversation history (last 24 turns)
        history.takeLast(24).forEach { msg ->
            contentsArray.put(
                JSONObject().apply {
                    put("role", if (msg.role == "model") "model" else "user")
                    put("parts", JSONArray().put(JSONObject().put("text", msg.text)))
                }
            )
        }
        contentsArray.put(
            JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", userMessage)))
            }
        )

        val payload = JSONObject().apply {
            if (systemInstruction.isNotBlank()) {
                put(
                    "systemInstruction",
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", systemInstruction))
                    )
                )
            }
            put("contents", contentsArray)
        }

        val request = Request.Builder()
            .url("$BASE_URL/${model.modelId}:streamGenerateContent?alt=sse")
            .header("x-goog-api-key", trimmedKey)
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        withTimeout(60_000L) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string().orEmpty()
                    val msg = parseApiError(response.code, errBody)
                    Log.e(TAG, "Chat stream failed (${response.code}): $msg")
                    throw IllegalStateException(msg)
                }

                val source = response.body?.source()
                    ?: throw IllegalStateException("Empty response stream from Gemini.")

                var emittedAny = false
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    if (line.startsWith("data:")) {
                        val jsonStr = line.removePrefix("data:").trim()
                        if (jsonStr.isNotEmpty() && jsonStr != "[DONE]") {
                            val chunkText = extractTextFromResponse(jsonStr)
                            if (chunkText.isNotEmpty()) {
                                emittedAny = true
                                emit(chunkText)
                            }
                        }
                    }
                }
                if (!emittedAny) {
                    throw IllegalStateException("Gemini returned an empty response.")
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun extractTextFromResponse(rawJson: String): String {
        return runCatching {
            val root = JSONObject(rawJson)
            val candidates = root.optJSONArray("candidates") ?: return ""
            val sb = StringBuilder()
            for (i in 0 until candidates.length()) {
                val content = candidates.optJSONObject(i)?.optJSONObject("content") ?: continue
                val parts = content.optJSONArray("parts") ?: continue
                for (j in 0 until parts.length()) {
                    val partText = parts.optJSONObject(j)?.optString("text", "") ?: ""
                    sb.append(partText)
                }
            }
            sb.toString()
        }.getOrDefault("")
    }

    private fun parseApiError(statusCode: Int, rawBody: String): String {
        val parsedMessage = runCatching {
            JSONObject(rawBody).optJSONObject("error")?.optString("message").orEmpty()
        }.getOrDefault("")

        return when {
            statusCode == 400 && parsedMessage.contains("API key", ignoreCase = true) ->
                "Invalid Gemini API key. Please check your key in Settings."
            statusCode == 401 || statusCode == 403 ->
                parsedMessage.ifBlank { "Unauthorized (HTTP $statusCode). Verify your Gemini API key in Settings." }
            statusCode == 429 ->
                parsedMessage.ifBlank { "Rate limit or quota reached (HTTP 429). Please wait and try again." }
            parsedMessage.isNotBlank() -> parsedMessage
            else -> "Gemini API error (HTTP $statusCode)."
        }
    }
}
