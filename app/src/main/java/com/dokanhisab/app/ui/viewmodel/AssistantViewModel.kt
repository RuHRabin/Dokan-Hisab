package com.dokanhisab.app.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dokanhisab.app.audio.VoiceAudioRecorder
import com.dokanhisab.app.data.local.ChatMessageDao
import com.dokanhisab.app.data.local.ChatMessageEntity
import com.dokanhisab.app.data.preferences.AiProvider
import com.dokanhisab.app.data.preferences.ChatRole
import com.dokanhisab.app.data.preferences.GeminiChatModel
import com.dokanhisab.app.data.preferences.OpenRouterPresetModel
import com.dokanhisab.app.data.preferences.UserPreferences
import com.dokanhisab.app.data.preferences.UserPreferencesRepository
import com.dokanhisab.app.data.remote.GeminiApiClient
import com.dokanhisab.app.data.remote.OpenRouterApiClient
import com.dokanhisab.app.domain.HisabCalculator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "AssistantViewModel"

sealed interface ChatGenerationState {
    data object Idle : ChatGenerationState
    data object Loading : ChatGenerationState
    data class Streaming(val partialText: String, val modelId: String) : ChatGenerationState
    data class Error(val message: String) : ChatGenerationState
}

sealed interface TranscriptionState {
    data object Idle : TranscriptionState
    data class Recording(val startedAtMillis: Long = System.currentTimeMillis()) : TranscriptionState
    data object Transcribing : TranscriptionState
    data class Success(val text: String) : TranscriptionState
    data class Error(val message: String) : TranscriptionState
}

class AssistantViewModel(
    private val chatMessageDao: ChatMessageDao,
    private val preferencesRepository: UserPreferencesRepository,
    private val geminiApiClient: GeminiApiClient,
    private val openRouterApiClient: OpenRouterApiClient,
    private val audioRecorder: VoiceAudioRecorder
) : ViewModel() {

    val messages: StateFlow<List<ChatMessageEntity>> = chatMessageDao.getAllMessages()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList()
        )

    private val _chatState = MutableStateFlow<ChatGenerationState>(ChatGenerationState.Idle)
    val chatState: StateFlow<ChatGenerationState> = _chatState.asStateFlow()

    private val _transcriptionState = MutableStateFlow<TranscriptionState>(TranscriptionState.Idle)
    val transcriptionState: StateFlow<TranscriptionState> = _transcriptionState.asStateFlow()

    fun selectProvider(provider: AiProvider) {
        viewModelScope.launch { preferencesRepository.setAiProvider(provider) }
    }

    fun selectModel(model: GeminiChatModel) {
        viewModelScope.launch { preferencesRepository.setChatModel(model) }
    }

    fun selectOpenRouterModel(modelId: String) {
        viewModelScope.launch { preferencesRepository.setOpenRouterModel(modelId) }
    }

    fun selectRole(role: ChatRole) {
        viewModelScope.launch { preferencesRepository.setChatRole(role) }
    }

    fun saveCustomSystemInstruction(instruction: String) {
        viewModelScope.launch {
            preferencesRepository.setCustomSystemInstruction(instruction)
            preferencesRepository.setChatRole(ChatRole.CUSTOM)
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            chatMessageDao.clearAllMessages()
            _chatState.value = ChatGenerationState.Idle
        }
    }

    fun dismissChatError() {
        if (_chatState.value is ChatGenerationState.Error) {
            _chatState.value = ChatGenerationState.Idle
        }
    }

    fun sendChatMessage(
        userText: String,
        preferences: UserPreferences,
        ledgerSummaryContext: String
    ) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty()) return

        val isOpenRouter = preferences.aiProvider == AiProvider.OPENROUTER
        if (isOpenRouter && preferences.openRouterApiKey.isBlank()) {
            _chatState.value = ChatGenerationState.Error("OpenRouter API key is not set. Add it in Settings.")
            return
        }
        if (!isOpenRouter && preferences.geminiApiKey.isBlank()) {
            _chatState.value = ChatGenerationState.Error("Gemini API key is not set. Add it in Settings.")
            return
        }
        if (_chatState.value is ChatGenerationState.Loading || _chatState.value is ChatGenerationState.Streaming) {
            return
        }

        val currentHistory = messages.value
        val activeModelId = if (isOpenRouter) {
            preferences.selectedOpenRouterModel.ifBlank { OpenRouterPresetModel.GEMINI_2_5_FLASH.modelId }
        } else {
            preferences.selectedChatModel.modelId
        }
        val systemInstruction = geminiApiClient.buildSystemInstruction(
            role = preferences.selectedChatRole,
            customInstruction = preferences.customSystemInstruction,
            shopName = preferences.shopName,
            language = preferences.language,
            ledgerSummaryContext = ledgerSummaryContext
        )

        viewModelScope.launch {
            chatMessageDao.insertMessage(
                ChatMessageEntity(
                    role = "user",
                    text = trimmed,
                    modelId = activeModelId,
                    timestamp = System.currentTimeMillis()
                )
            )
            _chatState.value = ChatGenerationState.Loading

            val accumulated = StringBuilder()
            try {
                val streamFlow = if (isOpenRouter) {
                    openRouterApiClient.streamChatMessage(
                        apiKey = preferences.openRouterApiKey,
                        modelId = activeModelId,
                        systemInstruction = systemInstruction,
                        history = currentHistory,
                        userMessage = trimmed
                    )
                } else {
                    geminiApiClient.streamChatMessage(
                        apiKey = preferences.geminiApiKey,
                        model = preferences.selectedChatModel,
                        systemInstruction = systemInstruction,
                        history = currentHistory,
                        userMessage = trimmed
                    )
                }

                streamFlow.collect { chunk ->
                    accumulated.append(chunk)
                    _chatState.value = ChatGenerationState.Streaming(
                        partialText = accumulated.toString(),
                        modelId = activeModelId
                    )
                }

                val finalReply = accumulated.toString().trim()
                if (finalReply.isNotEmpty()) {
                    chatMessageDao.insertMessage(
                        ChatMessageEntity(
                            role = "model",
                            text = finalReply,
                            modelId = activeModelId,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    _chatState.value = ChatGenerationState.Idle
                } else {
                    _chatState.value = ChatGenerationState.Error("${preferences.aiProvider.displayName} returned an empty response.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Chat request timed out", e)
                _chatState.value = ChatGenerationState.Error("Request timed out after 60 seconds. Please try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Chat request failed", e)
                _chatState.value = ChatGenerationState.Error(
                    e.localizedMessage ?: "Failed to get response from ${preferences.aiProvider.displayName}."
                )
            }
        }
    }

    fun startRecording() {
        audioRecorder.startRecording()
            .onSuccess {
                _transcriptionState.value = TranscriptionState.Recording(System.currentTimeMillis())
            }
            .onFailure { err ->
                _transcriptionState.value = TranscriptionState.Error(
                    err.localizedMessage ?: "Unable to start microphone recording."
                )
            }
    }

    fun stopAndTranscribe(
        preferences: UserPreferences,
        onTranscribedCallback: ((String) -> Unit)? = null
    ) {
        val audioFile: File? = audioRecorder.stopRecording()
        if (audioFile == null) {
            _transcriptionState.value = TranscriptionState.Error("No audio was recorded. Please speak clearly and try again.")
            return
        }
        if (preferences.geminiApiKey.isBlank()) {
            runCatching { audioFile.delete() }
            _transcriptionState.value = TranscriptionState.Error("Gemini API key is required in Settings for transcription.")
            return
        }

        viewModelScope.launch {
            _transcriptionState.value = TranscriptionState.Transcribing
            try {
                val transcript = geminiApiClient.transcribeAudio(
                    apiKey = preferences.geminiApiKey,
                    audioFile = audioFile,
                    language = preferences.language
                )
                _transcriptionState.value = TranscriptionState.Success(transcript)
                onTranscribedCallback?.invoke(transcript)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Audio transcription timed out", e)
                _transcriptionState.value = TranscriptionState.Error("Audio transcription timed out. Please try a shorter clip.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Audio transcription failed", e)
                _transcriptionState.value = TranscriptionState.Error(
                    e.localizedMessage ?: "Audio transcription failed."
                )
            } finally {
                runCatching { audioFile.delete() }
            }
        }
    }

    fun cancelRecordingOrResetTranscription() {
        audioRecorder.cancelRecording()
        _transcriptionState.value = TranscriptionState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorder.cancelRecording()
    }

    companion object {
        fun buildLedgerSnapshot(ready: HisabUiState.Ready): String {
            val sym = ready.preferences.currency.symbol
            val lang = ready.preferences.language
            val banglaDigits = ready.preferences.useBanglaDigits
            val todayIn = HisabCalculator.formatAmount(ready.dailySummary.totalIncome, sym, lang, banglaDigits)
            val todayOut = HisabCalculator.formatAmount(ready.dailySummary.totalExpense, sym, lang, banglaDigits)
            val monthNet = HisabCalculator.formatAmount(ready.monthlySummary.netBalance, sym, lang, banglaDigits)
            val cashInHand = HisabCalculator.formatAmount(ready.cashInHandSummary.netBalance, sym, lang, banglaDigits)
            return "Today Income: $todayIn, Today Expense: $todayOut, This Month Net: $monthNet, Cash-in-Hand: $cashInHand."
        }
    }

    class Factory(
        private val context: Context,
        private val chatMessageDao: ChatMessageDao,
        private val preferencesRepository: UserPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AssistantViewModel(
                chatMessageDao = chatMessageDao,
                preferencesRepository = preferencesRepository,
                geminiApiClient = GeminiApiClient(),
                openRouterApiClient = OpenRouterApiClient(),
                audioRecorder = VoiceAudioRecorder(context.applicationContext)
            ) as T
        }
    }
}
