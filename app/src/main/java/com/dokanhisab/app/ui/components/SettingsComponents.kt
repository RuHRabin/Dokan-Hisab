package com.dokanhisab.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dokanhisab.app.data.local.BackupFileInfo
import com.dokanhisab.app.data.preferences.AiProvider
import com.dokanhisab.app.data.preferences.AppLanguage
import com.dokanhisab.app.data.preferences.OpenRouterPresetModel
import com.dokanhisab.app.data.preferences.UserPreferences
import com.dokanhisab.app.domain.HisabCalculator
import com.dokanhisab.app.localization.AppStrings
import com.dokanhisab.app.security.BiometricCapability
import com.dokanhisab.app.ui.theme.LocalSemanticColors

@Composable
fun SettingsSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val semantic = LocalSemanticColors.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        border = BorderStroke(1.dp, semantic.cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
fun LanguageOptionCard(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalSemanticColors.current
    Surface(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else semantic.cardBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AiProviderAndKeysSection(
    prefs: UserPreferences,
    strings: AppStrings,
    onAiProviderChange: (AiProvider) -> Unit,
    onGeminiApiKeySave: (String) -> Unit,
    onOpenRouterApiKeySave: (String) -> Unit,
    onOpenRouterModelSave: (String) -> Unit
) {
    val semantic = LocalSemanticColors.current
    var geminiKeyInput by remember(prefs.geminiApiKey) { mutableStateOf(prefs.geminiApiKey) }
    var geminiKeyVisible by remember { mutableStateOf(false) }

    var openRouterKeyInput by remember(prefs.openRouterApiKey) { mutableStateOf(prefs.openRouterApiKey) }
    var openRouterKeyVisible by remember { mutableStateOf(false) }

    var openRouterModelInput by remember(prefs.selectedOpenRouterModel) {
        mutableStateOf(prefs.selectedOpenRouterModel)
    }

    val hasGeminiKey = prefs.geminiApiKey.isNotBlank()
    val hasOpenRouterKey = prefs.openRouterApiKey.isNotBlank()

    SettingsSectionCard(title = strings.geminiSection) {
        Text(
            text = strings.aiProviderLabel,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AiProvider.entries.forEach { provider ->
                val selected = prefs.aiProvider == provider
                val icon = if (provider == AiProvider.GEMINI) Icons.Rounded.Bolt else Icons.Rounded.Hub
                Surface(
                    onClick = { onAiProviderChange(provider) },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("settings_provider_${provider.name.lowercase()}"),
                    shape = MaterialTheme.shapes.medium,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    border = BorderStroke(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary else semantic.cardBorder
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (selected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.providerLabel(provider),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
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

        HorizontalDivider(color = semantic.cardBorder.copy(alpha = 0.7f))

        ApiKeyEditorBlock(
            label = strings.geminiApiKeyLabel,
            hint = strings.geminiApiKeyHint,
            input = geminiKeyInput,
            visible = geminiKeyVisible,
            hasSavedKey = hasGeminiKey,
            strings = strings,
            inputTestTag = "gemini_api_key_input",
            badgeTestTag = "api_key_status_badge",
            toggleTestTag = "toggle_api_key_visibility",
            saveTestTag = "save_api_key_button",
            clearTestTag = "clear_api_key_button",
            onInputChange = { geminiKeyInput = it },
            onToggleVisibility = { geminiKeyVisible = !geminiKeyVisible },
            onSave = { onGeminiApiKeySave(geminiKeyInput.trim()) },
            onClear = {
                geminiKeyInput = ""
                onGeminiApiKeySave("")
            }
        )

        HorizontalDivider(color = semantic.cardBorder.copy(alpha = 0.7f))

        ApiKeyEditorBlock(
            label = strings.openRouterApiKeyLabel,
            hint = strings.openRouterApiKeyHint,
            input = openRouterKeyInput,
            visible = openRouterKeyVisible,
            hasSavedKey = hasOpenRouterKey,
            strings = strings,
            inputTestTag = "openrouter_api_key_input",
            badgeTestTag = "openrouter_key_status_badge",
            toggleTestTag = "toggle_openrouter_key_visibility",
            saveTestTag = "save_openrouter_key_button",
            clearTestTag = "clear_openrouter_key_button",
            onInputChange = { openRouterKeyInput = it },
            onToggleVisibility = { openRouterKeyVisible = !openRouterKeyVisible },
            onSave = { onOpenRouterApiKeySave(openRouterKeyInput.trim()) },
            onClear = {
                openRouterKeyInput = ""
                onOpenRouterApiKeySave("")
            }
        )

        Text(
            text = strings.openRouterModelLabel,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OpenRouterPresetModel.entries.forEach { preset ->
                val selected = prefs.selectedOpenRouterModel == preset.modelId
                FilterChip(
                    selected = selected,
                    onClick = {
                        openRouterModelInput = preset.modelId
                        onOpenRouterModelSave(preset.modelId)
                    },
                    label = { Text(preset.label) },
                    modifier = Modifier.testTag("settings_or_model_${preset.name.lowercase()}")
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = openRouterModelInput,
                onValueChange = { openRouterModelInput = it },
                label = { Text(strings.customModelIdLabel) },
                placeholder = { Text(strings.customModelIdHint) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("openrouter_custom_model_input")
            )
            Button(
                onClick = { onOpenRouterModelSave(openRouterModelInput) },
                modifier = Modifier
                    .height(54.dp)
                    .testTag("save_openrouter_model_button")
            ) {
                Text(strings.saveShopName)
            }
        }

        Text(
            text = strings.apiKeyHelperNote,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ApiKeyEditorBlock(
    label: String,
    hint: String,
    input: String,
    visible: Boolean,
    hasSavedKey: Boolean,
    strings: AppStrings,
    inputTestTag: String,
    badgeTestTag: String,
    toggleTestTag: String,
    saveTestTag: String,
    clearTestTag: String,
    onInputChange: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit
) {
    val semantic = LocalSemanticColors.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    imageVector = Icons.Rounded.Key,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                color = if (hasSavedKey) semantic.incomeContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.testTag(badgeTestTag)
            ) {
                Text(
                    text = if (hasSavedKey) strings.apiKeyConfiguredBadge else strings.apiKeyMissingBadge,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (hasSavedKey) semantic.onIncomeContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            placeholder = { Text(hint) },
            singleLine = true,
            visualTransformation = if (visible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(
                    onClick = onToggleVisibility,
                    modifier = Modifier.testTag(toggleTestTag)
                ) {
                    Icon(
                        imageVector = if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = null
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(inputTestTag)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onSave,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag(saveTestTag)
            ) {
                Text(strings.saveApiKey)
            }
            if (hasSavedKey || input.isNotBlank()) {
                OutlinedButton(
                    onClick = onClear,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag(clearTestTag)
                ) {
                    Text(strings.clearApiKey)
                }
            }
        }
    }
}

@Composable
fun RestoreDatabaseDialog(
    strings: AppStrings,
    hasSelectedFolder: Boolean,
    folderBackups: List<BackupFileInfo>,
    language: AppLanguage,
    useBanglaDigits: Boolean,
    onDismiss: () -> Unit,
    onSelectBackup: (BackupFileInfo) -> Unit,
    onBrowseFile: () -> Unit
) {
    val semantic = LocalSemanticColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = strings.restoreDialogTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = strings.restoreDialogWarning,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                if (hasSelectedFolder) {
                    Text(
                        text = strings.backupsInFolderTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (folderBackups.isEmpty()) {
                        Text(
                            text = strings.noBackupsInFolder,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        folderBackups.forEach { item ->
                            val kb = (item.sizeBytes / 1024L).coerceAtLeast(1L)
                            val dateStr = if (item.lastModifiedMillis > 0L) {
                                HisabCalculator.formatDateTime(
                                    timestamp = item.lastModifiedMillis,
                                    language = language,
                                    useBanglaDigits = useBanglaDigits
                                )
                            } else {
                                ""
                            }
                            Surface(
                                onClick = { onSelectBackup(item) },
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                border = BorderStroke(1.dp, semantic.cardBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("restore_backup_item_${item.displayName}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Storage,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (dateStr.isNotBlank()) "$dateStr • ${kb} KB" else "${kb} KB",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = onBrowseFile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("browse_restore_file_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.browseBackupFile)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

@Composable
fun SecurityLockSection(
    prefs: UserPreferences,
    strings: AppStrings,
    capability: BiometricCapability,
    onToggleBiometricLock: (Boolean) -> Unit,
    onToggleLockOnBackground: (Boolean) -> Unit,
    onOpenSecuritySettings: () -> Unit,
    onLockAppNow: () -> Unit
) {
    val semantic = LocalSemanticColors.current
    val badgeText = when (capability) {
        BiometricCapability.READY -> strings.biometricReadyBadge
        BiometricCapability.NONE_ENROLLED -> strings.biometricNotEnrolledBadge
        BiometricCapability.NO_HARDWARE,
        BiometricCapability.UNAVAILABLE -> strings.biometricUnavailableBadge
    }
    val badgeBg = when (capability) {
        BiometricCapability.READY -> semantic.incomeContainer
        BiometricCapability.NONE_ENROLLED -> MaterialTheme.colorScheme.secondaryContainer
        BiometricCapability.NO_HARDWARE,
        BiometricCapability.UNAVAILABLE -> MaterialTheme.colorScheme.surfaceVariant
    }
    val badgeFg = when (capability) {
        BiometricCapability.READY -> semantic.onIncomeContainer
        BiometricCapability.NONE_ENROLLED -> MaterialTheme.colorScheme.onSecondaryContainer
        BiometricCapability.NO_HARDWARE,
        BiometricCapability.UNAVAILABLE -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    SettingsSectionCard(title = strings.securitySection) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Fingerprint,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.biometricLockLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = strings.biometricLockSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = prefs.biometricLockEnabled,
                onCheckedChange = onToggleBiometricLock,
                enabled = capability == BiometricCapability.READY || prefs.biometricLockEnabled,
                modifier = Modifier.testTag("biometric_lock_switch")
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = badgeBg,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.testTag("biometric_capability_badge")
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = badgeFg,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            if (capability == BiometricCapability.NONE_ENROLLED) {
                OutlinedButton(
                    onClick = onOpenSecuritySettings,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("enroll_biometric_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Security,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.enrollBiometricButton, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        if (prefs.biometricLockEnabled) {
            HorizontalDivider(color = semantic.cardBorder.copy(alpha = 0.7f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.lockOnBackgroundLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = strings.lockOnBackgroundSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = prefs.lockOnBackground,
                    onCheckedChange = onToggleLockOnBackground,
                    modifier = Modifier.testTag("lock_on_background_switch")
                )
            }

            OutlinedButton(
                onClick = onLockAppNow,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("lock_app_now_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.lockAppNowLabel)
            }
        }
    }
}

