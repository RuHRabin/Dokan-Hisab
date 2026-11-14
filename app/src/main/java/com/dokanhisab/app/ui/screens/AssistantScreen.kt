package com.dokanhisab.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.dokanhisab.app.data.local.ChatMessageEntity
import com.dokanhisab.app.data.preferences.AiProvider
import com.dokanhisab.app.data.preferences.AppLanguage
import com.dokanhisab.app.data.preferences.ChatRole
import com.dokanhisab.app.data.preferences.GeminiChatModel
import com.dokanhisab.app.data.preferences.OpenRouterPresetModel
import com.dokanhisab.app.localization.AppStrings
import com.dokanhisab.app.ui.theme.LocalSemanticColors
import com.dokanhisab.app.ui.viewmodel.ChatGenerationState
import com.dokanhisab.app.ui.viewmodel.HisabUiState
import com.dokanhisab.app.ui.viewmodel.TranscriptionState
import kotlinx.coroutines.delay

@Composable
fun AssistantScreen(
    hisabState: HisabUiState.Ready,
    messages: List<ChatMessageEntity>,
    chatState: ChatGenerationState,
    transcriptionState: TranscriptionState,
    strings: AppStrings,
    onSelectProvider: (AiProvider) -> Unit,
    onSelectModel: (GeminiChatModel) -> Unit,
    onSelectOpenRouterModel: (String) -> Unit,
    onSelectRole: (ChatRole) -> Unit,
    onSaveCustomInstruction: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    onClearChat: () -> Unit,
    onDismissChatError: () -> Unit,
    onStartRecording: () -> Unit,
    onStopAndTranscribe: () -> Unit,
    onResetTranscription: () -> Unit,
    onAddTranscribedToLedger: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    val prefs = hisabState.preferences
    val isOpenRouter = prefs.aiProvider == AiProvider.OPENROUTER
    val hasGeminiKey = prefs.geminiApiKey.isNotBlank()
    val hasActiveChatKey = if (isOpenRouter) prefs.openRouterApiKey.isNotBlank() else hasGeminiKey
    val semantic = LocalSemanticColors.current

    var inputText by remember { mutableStateOf("") }
    var showCustomRoleDialog by remember { mutableStateOf(false) }
    var customRoleDraft by remember(prefs.customSystemInstruction) {
        mutableStateOf(prefs.customSystemInstruction)
    }
    var showCustomModelDialog by remember { mutableStateOf(false) }
    var customModelDraft by remember(prefs.selectedOpenRouterModel) {
        mutableStateOf(prefs.selectedOpenRouterModel)
    }
    var showDeskOptions by remember { mutableStateOf(true) }

    val listState = rememberLazyListState()

    var elapsedSec by remember { mutableLongStateOf(0L) }
    LaunchedEffect(transcriptionState) {
        if (transcriptionState is TranscriptionState.Recording) {
            elapsedSec = 0L
            while (true) {
                delay(1000L)
                elapsedSec++
            }
        } else {
            elapsedSec = 0L
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (transcriptionState is TranscriptionState.Recording) {
                onResetTranscription()
            }
        }
    }

    LaunchedEffect(messages.size, chatState) {
        val total = messages.size + if (chatState is ChatGenerationState.Streaming || chatState is ChatGenerationState.Loading) 1 else 0
        if (total > 0) {
            listState.animateScrollToItem(total - 1)
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onStartRecording()
        } else {
            onShowMessage(strings.micPermissionRequired)
        }
    }

    fun triggerMicAction() {
        if (!hasGeminiKey) {
            onOpenSettings()
            return
        }
        when (transcriptionState) {
            is TranscriptionState.Recording -> onStopAndTranscribe()
            is TranscriptionState.Transcribing -> Unit
            else -> {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    onStartRecording()
                } else {
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        }
    }

    if (showCustomRoleDialog) {
        AlertDialog(
            onDismissRequest = { showCustomRoleDialog = false },
            title = { Text(strings.customSystemInstructionLabel, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = customRoleDraft,
                    onValueChange = { customRoleDraft = it },
                    placeholder = { Text(strings.customSystemInstructionHint) },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveCustomInstruction(customRoleDraft)
                        showCustomRoleDialog = false
                    }
                ) {
                    Text(strings.confirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomRoleDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    if (showCustomModelDialog) {
        AlertDialog(
            onDismissRequest = { showCustomModelDialog = false },
            title = { Text(strings.customModelIdLabel, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = customModelDraft,
                    onValueChange = { customModelDraft = it },
                    placeholder = { Text(strings.customModelIdHint) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customModelDraft.isNotBlank()) {
                            onSelectOpenRouterModel(customModelDraft.trim())
                        }
                        showCustomModelDialog = false
                    }
                ) {
                    Text(strings.confirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomModelDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Forum,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Text(
                        text = strings.assistantTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showDeskOptions = !showDeskOptions },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("toggle_desk_options_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = strings.chatModelLabel,
                            tint = if (showDeskOptions) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    if (messages.isNotEmpty()) {
                        IconButton(
                            onClick = onClearChat,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("clear_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteSweep,
                                contentDescription = strings.clearChatHistory
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = showDeskOptions) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, semantic.cardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            AiProvider.entries.forEach { provider ->
                                val selected = prefs.aiProvider == provider
                                val icon = if (provider == AiProvider.GEMINI) Icons.Rounded.Bolt else Icons.Rounded.Hub
                                Surface(
                                    onClick = { onSelectProvider(provider) },
                                    shape = RoundedCornerShape(50),
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        androidx.compose.ui.graphics.Color.Transparent
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .testTag("assistant_provider_${provider.name.lowercase()}")
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (selected) {
                                                MaterialTheme.colorScheme.onPrimary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = strings.providerLabel(provider),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.onPrimary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isOpenRouter) {
                            val presetIds = OpenRouterPresetModel.entries.map { it.modelId }
                            val isCustomSelected = prefs.selectedOpenRouterModel !in presetIds

                            OpenRouterPresetModel.entries.forEach { preset ->
                                FilterChip(
                                    selected = prefs.selectedOpenRouterModel == preset.modelId,
                                    onClick = { onSelectOpenRouterModel(preset.modelId) },
                                    label = { Text(preset.label) },
                                    modifier = Modifier.testTag("or_model_chip_${preset.name.lowercase()}")
                                )
                            }
                            FilterChip(
                                selected = isCustomSelected,
                                onClick = { showCustomModelDialog = true },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Tune,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        if (isCustomSelected) prefs.selectedOpenRouterModel else strings.setCustomModel,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag("or_model_custom_chip")
                            )
                        } else {
                            GeminiChatModel.entries.forEach { model ->
                                FilterChip(
                                    selected = prefs.selectedChatModel == model,
                                    onClick = { onSelectModel(model) },
                                    label = { Text(strings.modelLabel(model)) },
                                    modifier = Modifier.testTag("model_chip_${model.name.lowercase()}")
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ChatRole.entries.forEach { role ->
                            FilterChip(
                                selected = prefs.selectedChatRole == role,
                                onClick = {
                                    if (role == ChatRole.CUSTOM && prefs.customSystemInstruction.isBlank()) {
                                        showCustomRoleDialog = true
                                    } else {
                                        onSelectRole(role)
                                    }
                                },
                                label = { Text(strings.roleLabel(role)) },
                                trailingIcon = if (role == ChatRole.CUSTOM) {
                                    {
                                        IconButton(
                                            onClick = { showCustomRoleDialog = true },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Edit,
                                                contentDescription = strings.editSystemInstruction,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                } else null,
                                modifier = Modifier.testTag("role_chip_${role.name.lowercase()}")
                            )
                        }
                    }
                }
            }

            if (!hasActiveChatKey) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Key,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = if (isOpenRouter) strings.openRouterKeyRequiredTitle else strings.apiKeyRequiredTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        Text(
                            text = if (isOpenRouter) strings.openRouterKeyRequiredBody else strings.apiKeyRequiredBody,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Button(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("open_settings_for_api_key")
                        ) {
                            Text(strings.openSettingsForKey)
                        }
                    }
                }
            }

            VoiceTranscribeCard(
                transcriptionState = transcriptionState,
                elapsedSec = elapsedSec,
                showGeminiKeyHint = isOpenRouter && !hasGeminiKey,
                strings = strings,
                onMicAction = ::triggerMicAction,
                onReset = onResetTranscription,
                onCopy = { text ->
                    clipboard.setText(AnnotatedString(text))
                    onShowMessage(strings.copiedToClipboard)
                },
                onSendToChat = { text ->
                    onSendMessage(text)
                    onResetTranscription()
                },
                onAddToLedger = { text ->
                    onAddTranscribedToLedger(text)
                    onResetTranscription()
                }
            )

            AnimatedVisibility(visible = chatState is ChatGenerationState.Error) {
                val errMsg = (chatState as? ChatGenerationState.Error)?.message.orEmpty()
                Surface(
                    color = semantic.expenseContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = errMsg,
                            style = MaterialTheme.typography.bodySmall,
                            color = semantic.onExpenseContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDismissChatError) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = strings.cancel,
                                tint = semantic.onExpenseContainer
                            )
                        }
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("chat_message_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (messages.isEmpty() && chatState !is ChatGenerationState.Loading && chatState !is ChatGenerationState.Streaming) {
                    item {
                        ChatEmptyState(
                            language = prefs.language,
                            strings = strings,
                            enabled = hasActiveChatKey,
                            onPromptClick = { prompt ->
                                if (hasActiveChatKey) onSendMessage(prompt) else onOpenSettings()
                            }
                        )
                    }
                } else {
                    items(
                        items = messages,
                        key = { it.id }
                    ) { msg ->
                        ChatBubble(
                            role = msg.role,
                            text = msg.text,
                            displaySource = formatDisplayModelLabel(msg.modelId, strings),
                            copyContentDescription = strings.copyText,
                            onCopy = {
                                clipboard.setText(AnnotatedString(msg.text))
                                onShowMessage(strings.copiedToClipboard)
                            }
                        )
                    }

                    if (chatState is ChatGenerationState.Loading) {
                        val activeModelLabel = if (isOpenRouter) {
                            formatDisplayModelLabel(prefs.selectedOpenRouterModel, strings)
                        } else {
                            strings.modelLabel(prefs.selectedChatModel)
                        }
                        item {
                            Row(
                                modifier = Modifier.padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "$activeModelLabel...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else if (chatState is ChatGenerationState.Streaming) {
                        item {
                            ChatBubble(
                                role = "model",
                                text = chatState.partialText,
                                displaySource = formatDisplayModelLabel(chatState.modelId, strings),
                                copyContentDescription = strings.copyText,
                                onCopy = null
                            )
                        }
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                tonalElevation = 2.dp,
                border = BorderStroke(1.dp, semantic.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isRecording = transcriptionState is TranscriptionState.Recording
                    Surface(
                        onClick = ::triggerMicAction,
                        shape = CircleShape,
                        color = if (isRecording) semantic.expense else MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("composer_mic_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isRecording) Icons.Rounded.Stop else Icons.Rounded.Mic,
                                contentDescription = strings.voiceTranscribeTitle,
                                tint = if (isRecording) semantic.onExpense else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text(strings.chatInputHint, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        maxLines = 3,
                        shape = RoundedCornerShape(22.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputText.isNotBlank()) {
                                    onSendMessage(inputText)
                                    inputText = ""
                                }
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field")
                    )

                    val canSend = inputText.isNotBlank() &&
                        chatState !is ChatGenerationState.Loading &&
                        chatState !is ChatGenerationState.Streaming
                    Surface(
                        onClick = {
                            if (canSend) {
                                onSendMessage(inputText)
                                inputText = ""
                            }
                        },
                        shape = CircleShape,
                        color = if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("send_chat_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = strings.sendMessage,
                                tint = if (canSend) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceTranscribeCard(
    transcriptionState: TranscriptionState,
    elapsedSec: Long,
    showGeminiKeyHint: Boolean,
    strings: AppStrings,
    onMicAction: () -> Unit,
    onReset: () -> Unit,
    onCopy: (String) -> Unit,
    onSendToChat: (String) -> Unit,
    onAddToLedger: (String) -> Unit
) {
    val semantic = LocalSemanticColors.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        border = BorderStroke(1.dp, semantic.cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = strings.voiceTranscribeTitle,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (showGeminiKeyHint) {
                            Text(
                                text = strings.voiceRequiresGeminiHint,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                when (transcriptionState) {
                    is TranscriptionState.Recording -> {
                        Button(
                            onClick = onMicAction,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = semantic.expense,
                                contentColor = semantic.onExpense
                            ),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("stop_transcribe_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Stop,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("${strings.stopAndTranscribe} (${elapsedSec}s)")
                        }
                    }
                    is TranscriptionState.Transcribing -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = strings.transcribingAudio,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    else -> {
                        OutlinedButton(
                            onClick = onMicAction,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("start_transcribe_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Mic,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.tapMicToRecord, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            if (transcriptionState is TranscriptionState.Error) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = transcriptionState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = semantic.expense,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onReset) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = strings.cancel,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (transcriptionState is TranscriptionState.Success) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.transcribedResultLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = onReset,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = strings.cancel,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = transcriptionState.text,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.testTag("transcribed_text_result")
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onCopy(transcriptionState.text) },
                                modifier = Modifier.height(42.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.copyText)
                            }
                            OutlinedButton(
                                onClick = { onSendToChat(transcriptionState.text) },
                                modifier = Modifier.height(42.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Send,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.sendToChat)
                            }
                            Button(
                                onClick = { onAddToLedger(transcriptionState.text) },
                                modifier = Modifier.height(42.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AddCircleOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.addToLedger)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDisplayModelLabel(rawModelId: String, strings: AppStrings): String {
    val geminiMatch = GeminiChatModel.entries.firstOrNull { it.modelId == rawModelId }
    if (geminiMatch != null) return "${strings.providerGeminiLabel} • ${strings.modelLabel(geminiMatch)}"
    val openRouterMatch = OpenRouterPresetModel.entries.firstOrNull { it.modelId == rawModelId }
    if (openRouterMatch != null) return openRouterMatch.label
    return rawModelId.substringAfterLast('/').ifBlank { strings.assistantTitle }
}

@Composable
private fun ChatBubble(
    role: String,
    text: String,
    displaySource: String,
    copyContentDescription: String,
    onCopy: (() -> Unit)?
) {
    val isUser = role == "user"
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLowest
            },
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            border = if (isUser) null else BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
            ),
            modifier = Modifier.widthIn(max = 560.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!isUser && displaySource.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displaySource,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (onCopy != null) {
                            IconButton(
                                onClick = onCopy,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentCopy,
                                    contentDescription = copyContentDescription,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

@Composable
private fun ChatEmptyState(
    language: AppLanguage,
    strings: AppStrings,
    enabled: Boolean,
    onPromptClick: (String) -> Unit
) {
    val semantic = LocalSemanticColors.current
    val starters = if (language == AppLanguage.BANGLA) {
        listOf(
            "আজকের জমা-খরচ দেখে আমার লাভ-ক্ষতি বুঝিয়ে বলো",
            "একজন ক্রেতাকে বাকি টাকা চাওয়ার জন্য ভদ্রভাবে মেসেজ লিখে দাও",
            "দোকানের দৈনিক খরচ কমানোর ৩টি বাস্তবসম্মত উপায় বলো"
        )
    } else {
        listOf(
            "Analyze my today's income vs expense and cash-in-hand",
            "Draft a polite customer message reminding them of a due balance",
            "Give 3 practical ways to reduce daily shop operating costs"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Insights,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(26.dp)
            )
        }
        Text(
            text = strings.chatEmptyTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = strings.chatEmptySubtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        starters.forEach { prompt ->
            Surface(
                onClick = { onPromptClick(prompt) },
                enabled = enabled,
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = BorderStroke(1.dp, semantic.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
