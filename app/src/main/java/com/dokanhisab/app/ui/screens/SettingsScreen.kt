package com.dokanhisab.app.ui.screens

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.dokanhisab.app.R
import com.dokanhisab.app.data.local.BackupFileInfo
import com.dokanhisab.app.data.preferences.AiProvider
import com.dokanhisab.app.data.preferences.AppLanguage
import com.dokanhisab.app.data.preferences.AppPalette
import com.dokanhisab.app.data.preferences.ThemeMode
import com.dokanhisab.app.domain.HisabCalculator
import com.dokanhisab.app.localization.AppStrings
import com.dokanhisab.app.security.BiometricCapability
import com.dokanhisab.app.ui.components.AiProviderAndKeysSection
import com.dokanhisab.app.ui.components.LanguageOptionCard
import com.dokanhisab.app.ui.components.RestoreDatabaseDialog
import com.dokanhisab.app.ui.components.SecurityLockSection
import com.dokanhisab.app.ui.components.SettingsSectionCard
import com.dokanhisab.app.ui.theme.LocalSemanticColors
import com.dokanhisab.app.ui.theme.paletteAccentSwatchColor
import com.dokanhisab.app.ui.theme.paletteSwatchColor
import com.dokanhisab.app.ui.viewmodel.HisabUiState
import java.io.File

@Composable
fun SettingsScreen(
    state: HisabUiState.Ready,
    strings: AppStrings,
    folderBackups: List<BackupFileInfo>,
    onLanguageChange: (AppLanguage) -> Unit,
    onBanglaDigitsChange: (Boolean) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onPaletteChange: (AppPalette) -> Unit,
    onCustomAppNameSave: (String) -> Unit,
    onShopNameSave: (String) -> Unit,
    onAiProviderChange: (AiProvider) -> Unit,
    onGeminiApiKeySave: (String) -> Unit,
    onOpenRouterApiKeySave: (String) -> Unit,
    onOpenRouterModelSave: (String) -> Unit,
    onPickBannerPhoto: () -> Unit,
    onRemoveBannerPhoto: () -> Unit,
    onPickProfilePhoto: () -> Unit,
    onRemoveProfilePhoto: () -> Unit,
    onSelectSaveFolder: () -> Unit,
    onClearSaveFolder: () -> Unit,
    onBackupDatabase: () -> Unit,
    onRefreshFolderBackups: () -> Unit,
    onRestoreBackupUri: (Uri) -> Unit,
    onBrowseRestoreFile: () -> Unit,
    onOpenExport: () -> Unit,
    onClearAllData: () -> Unit,
    biometricCapability: BiometricCapability,
    onToggleBiometricLock: (Boolean) -> Unit,
    onToggleLockOnBackground: (Boolean) -> Unit,
    onOpenSecuritySettings: () -> Unit,
    onLockAppNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs = state.preferences
    val semantic = LocalSemanticColors.current
    var shopNameInput by remember(prefs.shopName) { mutableStateOf(prefs.shopName) }
    var appNameInput by remember(prefs.customAppName) { mutableStateOf(prefs.customAppName) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }

    val hasCustomBanner = prefs.bannerImagePath.isNotBlank() && File(prefs.bannerImagePath).exists()
    val hasCustomProfile = prefs.profileImagePath.isNotBlank() && File(prefs.profileImagePath).exists()
    val displayAppTitle = prefs.customAppName.ifBlank { strings.appName }
    val displayShopTitle = prefs.shopName.ifBlank { strings.defaultShopName }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(strings.clearAllConfirmTitle, fontWeight = FontWeight.Bold) },
            text = { Text(strings.clearAllConfirmBody) },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllData()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = semantic.expense,
                        contentColor = semantic.onExpense
                    )
                ) {
                    Text(strings.confirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    if (showRestoreDialog) {
        RestoreDatabaseDialog(
            strings = strings,
            hasSelectedFolder = prefs.saveDirectoryUri.isNotBlank(),
            folderBackups = folderBackups,
            language = prefs.language,
            useBanglaDigits = prefs.useBanglaDigits,
            onDismiss = { showRestoreDialog = false },
            onSelectBackup = { backupInfo ->
                showRestoreDialog = false
                onRestoreBackupUri(backupInfo.uri)
            },
            onBrowseFile = {
                showRestoreDialog = false
                onBrowseRestoreFile()
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = strings.settingsTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    border = BorderStroke(1.dp, semantic.cardBorder)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(136.dp)
                        ) {
                            if (hasCustomBanner) {
                                AsyncImage(
                                    model = File(prefs.bannerImagePath),
                                    contentDescription = strings.bannerPhotoLabel,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.img_shop_banner),
                                    contentDescription = strings.bannerPhotoLabel,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.74f),
                                                Color.Black.copy(alpha = 0.28f)
                                            )
                                        )
                                    )
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape,
                                    border = BorderStroke(2.dp, Color.White.copy(alpha = 0.85f)),
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .clickable(onClick = onPickProfilePhoto)
                                        .testTag("settings_avatar_picker")
                                ) {
                                    if (hasCustomProfile) {
                                        AsyncImage(
                                            model = File(prefs.profileImagePath),
                                            contentDescription = strings.profilePhotoLabel,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Rounded.Storefront,
                                                contentDescription = strings.profilePhotoLabel,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(30.dp)
                                            )
                                        }
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = displayShopTitle,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = displayAppTitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.88f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onPickBannerPhoto,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("pick_banner_button"),
                                contentPadding = PaddingValues(horizontal = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AddPhotoAlternate,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.changeBanner, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }

                            OutlinedButton(
                                onClick = onPickProfilePhoto,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("pick_profile_button"),
                                contentPadding = PaddingValues(horizontal = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.changeProfilePhoto, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        if (hasCustomBanner || hasCustomProfile) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (hasCustomBanner) {
                                    TextButton(
                                        onClick = onRemoveBannerPhoto,
                                        modifier = Modifier.testTag("remove_banner_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("${strings.removePhoto}: ${strings.bannerPhotoLabel}", maxLines = 1)
                                    }
                                }
                                if (hasCustomProfile) {
                                    TextButton(
                                        onClick = onRemoveProfilePhoto,
                                        modifier = Modifier.testTag("remove_profile_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("${strings.removePhoto}: ${strings.profilePhotoLabel}", maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                SettingsSectionCard(title = strings.shopProfileSection) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = appNameInput,
                            onValueChange = { appNameInput = it },
                            label = { Text(strings.customAppNameLabel) },
                            placeholder = { Text(strings.customAppNameHint) },
                            singleLine = true,
                            trailingIcon = {
                                if (appNameInput.isNotBlank()) {
                                    TextButton(
                                        onClick = {
                                            appNameInput = ""
                                            onCustomAppNameSave("")
                                        }
                                    ) {
                                        Text(strings.resetLabel)
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("custom_app_name_input")
                        )
                        Button(
                            onClick = { onCustomAppNameSave(appNameInput) },
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("save_app_name_button")
                        ) {
                            Text(strings.saveShopName)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = shopNameInput,
                            onValueChange = { shopNameInput = it },
                            label = { Text(strings.shopNameLabel) },
                            placeholder = { Text(strings.shopNameHint) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("shop_name_input")
                        )
                        Button(
                            onClick = { onShopNameSave(shopNameInput) },
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("save_shop_name_button")
                        ) {
                            Text(strings.saveShopName)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.currencyLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.testTag("currency_bdt_badge")
                        ) {
                            Text(
                                text = strings.currencyFixedValue,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            item {
                AiProviderAndKeysSection(
                    prefs = prefs,
                    strings = strings,
                    onAiProviderChange = onAiProviderChange,
                    onGeminiApiKeySave = onGeminiApiKeySave,
                    onOpenRouterApiKeySave = onOpenRouterApiKeySave,
                    onOpenRouterModelSave = onOpenRouterModelSave
                )
            }

            item {
                SettingsSectionCard(title = strings.languageSection) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LanguageOptionCard(
                            label = strings.languageBangla,
                            selected = prefs.language == AppLanguage.BANGLA,
                            onClick = { onLanguageChange(AppLanguage.BANGLA) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("language_bangla_button")
                        )
                        LanguageOptionCard(
                            label = strings.languageEnglish,
                            selected = prefs.language == AppLanguage.ENGLISH,
                            onClick = { onLanguageChange(AppLanguage.ENGLISH) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("language_english_button")
                        )
                    }

                    if (prefs.language == AppLanguage.BANGLA) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.banglaDigitsToggle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = strings.banglaDigitsSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = prefs.useBanglaDigits,
                                onCheckedChange = onBanglaDigitsChange,
                                modifier = Modifier.testTag("bangla_digits_switch")
                            )
                        }
                    }
                }
            }

            item {
                SettingsSectionCard(title = strings.appearanceSection) {
                    Text(
                        text = strings.themeModeLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            val selected = prefs.themeMode == mode
                            val icon = when (mode) {
                                ThemeMode.LIGHT -> Icons.Rounded.LightMode
                                ThemeMode.DARK -> Icons.Rounded.DarkMode
                                ThemeMode.SYSTEM -> Icons.Rounded.SettingsBrightness
                            }
                            Surface(
                                onClick = { onThemeModeChange(mode) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("theme_mode_${mode.name.lowercase()}"),
                                shape = MaterialTheme.shapes.small,
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
                                    modifier = Modifier.fillMaxSize(),
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
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = strings.themeModeLabel(mode),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.colorPaletteLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AppPalette.entries.forEach { palette ->
                            val selected = prefs.palette == palette
                            val primarySwatch = paletteSwatchColor(palette)
                            val accentSwatch = paletteAccentSwatchColor(palette)
                            Surface(
                                onClick = { onPaletteChange(palette) },
                                shape = MaterialTheme.shapes.medium,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                },
                                border = BorderStroke(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) primarySwatch else semantic.cardBorder
                                ),
                                modifier = Modifier.testTag("palette_${palette.key}")
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.size(50.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.linearGradient(
                                                        colors = listOf(primarySwatch, accentSwatch)
                                                    )
                                                )
                                                .then(
                                                    if (selected) {
                                                        Modifier.border(
                                                            width = 2.5.dp,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            shape = CircleShape
                                                        )
                                                    } else Modifier
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (selected) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = strings.paletteLabel(palette),
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .offset(x = (-2).dp, y = (-2).dp)
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(accentSwatch)
                                                .border(1.5.dp, MaterialTheme.colorScheme.surfaceContainerLowest, CircleShape)
                                        )
                                    }
                                    Text(
                                        text = strings.paletteLabel(palette),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                SecurityLockSection(
                    prefs = prefs,
                    strings = strings,
                    capability = biometricCapability,
                    onToggleBiometricLock = onToggleBiometricLock,
                    onToggleLockOnBackground = onToggleLockOnBackground,
                    onOpenSecuritySettings = onOpenSecuritySettings,
                    onLockAppNow = onLockAppNow
                )
            }

            item {
                SettingsSectionCard(title = strings.dataSection) {
                    Text(
                        text = strings.saveFolderLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, semantic.cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = prefs.saveDirectoryLabel.ifBlank { strings.saveFolderNotSelected },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onSelectSaveFolder,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("select_save_folder_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (prefs.saveDirectoryUri.isBlank()) {
                                    strings.selectSaveFolder
                                } else {
                                    strings.changeSaveFolder
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (prefs.saveDirectoryUri.isNotBlank()) {
                            TextButton(
                                onClick = onClearSaveFolder,
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("clear_save_folder_button")
                            ) {
                                Text(strings.clearSaveFolder)
                            }
                        }
                    }

                    HorizontalDivider(color = semantic.cardBorder.copy(alpha = 0.7f))

                    Text(
                        text = strings.backupDatabaseTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val lastBackupText = if (prefs.lastBackupTimestamp > 0L) {
                        "${strings.lastBackupLabel} ${
                            HisabCalculator.formatDateTime(
                                timestamp = prefs.lastBackupTimestamp,
                                language = prefs.language,
                                useBanglaDigits = prefs.useBanglaDigits
                            )
                        }"
                    } else {
                        strings.noBackupYet
                    }
                    Text(
                        text = lastBackupText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onBackupDatabase,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("backup_database_button"),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Backup,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.backupDatabaseButton,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                onRefreshFolderBackups()
                                showRestoreDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("restore_database_button"),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Restore,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.restoreDatabaseButton,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    HorizontalDivider(color = semantic.cardBorder.copy(alpha = 0.7f))

                    OutlinedButton(
                        onClick = onOpenExport,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("settings_export_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.exportTitle)
                    }

                    OutlinedButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("settings_clear_data_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = semantic.expense),
                        border = BorderStroke(1.dp, semantic.expense.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteForever,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.clearAllData)
                    }
                    Text(
                        text = strings.clearAllDataSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
