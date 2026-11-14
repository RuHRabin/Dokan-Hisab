package com.dokanhisab.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dokan_hisab_prefs")

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("app_language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val PALETTE = stringPreferencesKey("app_palette")
        val CUSTOM_APP_NAME = stringPreferencesKey("custom_app_name")
        val SHOP_NAME = stringPreferencesKey("shop_name")
        val USE_BANGLA_DIGITS = booleanPreferencesKey("use_bangla_digits")
        val BANNER_IMAGE_PATH = stringPreferencesKey("banner_image_path")
        val PROFILE_IMAGE_PATH = stringPreferencesKey("profile_image_path")
        val SAVE_DIRECTORY_URI = stringPreferencesKey("save_directory_uri")
        val SAVE_DIRECTORY_LABEL = stringPreferencesKey("save_directory_label")
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val AI_PROVIDER = stringPreferencesKey("ai_provider")
        val OPENROUTER_API_KEY = stringPreferencesKey("openrouter_api_key")
        val OPENROUTER_MODEL = stringPreferencesKey("openrouter_model")
        val CHAT_MODEL = stringPreferencesKey("gemini_chat_model")
        val CHAT_ROLE = stringPreferencesKey("gemini_chat_role")
        val CUSTOM_SYSTEM_INSTRUCTION = stringPreferencesKey("custom_system_instruction")
        val LAST_BACKUP_TIMESTAMP = longPreferencesKey("last_backup_timestamp")
        val BIOMETRIC_LOCK_ENABLED = booleanPreferencesKey("biometric_lock_enabled")
        val LOCK_ON_BACKGROUND = booleanPreferencesKey("lock_on_background")
    }

    val userPreferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            language = prefs[Keys.LANGUAGE]?.let { name ->
                runCatching { AppLanguage.valueOf(name) }.getOrNull()
            } ?: AppLanguage.BANGLA,
            themeMode = prefs[Keys.THEME_MODE]?.let { name ->
                runCatching { ThemeMode.valueOf(name) }.getOrNull()
            } ?: ThemeMode.SYSTEM,
            palette = prefs[Keys.PALETTE]?.let { name ->
                runCatching { AppPalette.valueOf(name) }.getOrNull()
            } ?: AppPalette.EMERALD,
            customAppName = prefs[Keys.CUSTOM_APP_NAME] ?: "",
            shopName = prefs[Keys.SHOP_NAME] ?: "",
            currency = CurrencyOption.BDT,
            useBanglaDigits = prefs[Keys.USE_BANGLA_DIGITS] ?: true,
            bannerImagePath = prefs[Keys.BANNER_IMAGE_PATH] ?: "",
            profileImagePath = prefs[Keys.PROFILE_IMAGE_PATH] ?: "",
            saveDirectoryUri = prefs[Keys.SAVE_DIRECTORY_URI] ?: "",
            saveDirectoryLabel = prefs[Keys.SAVE_DIRECTORY_LABEL] ?: "",
            aiProvider = prefs[Keys.AI_PROVIDER]?.let { name ->
                runCatching { AiProvider.valueOf(name) }.getOrNull()
            } ?: AiProvider.GEMINI,
            geminiApiKey = prefs[Keys.GEMINI_API_KEY] ?: "",
            openRouterApiKey = prefs[Keys.OPENROUTER_API_KEY] ?: "",
            selectedChatModel = prefs[Keys.CHAT_MODEL]?.let { name ->
                runCatching { GeminiChatModel.valueOf(name) }.getOrNull()
            } ?: GeminiChatModel.HYDROGEN,
            selectedOpenRouterModel = prefs[Keys.OPENROUTER_MODEL]?.takeIf { it.isNotBlank() }
                ?: OpenRouterPresetModel.GEMINI_2_5_FLASH.modelId,
            selectedChatRole = prefs[Keys.CHAT_ROLE]?.let { name ->
                runCatching { ChatRole.valueOf(name) }.getOrNull()
            } ?: ChatRole.SHOP_ACCOUNTANT,
            customSystemInstruction = prefs[Keys.CUSTOM_SYSTEM_INSTRUCTION] ?: "",
            lastBackupTimestamp = prefs[Keys.LAST_BACKUP_TIMESTAMP] ?: 0L,
            biometricLockEnabled = prefs[Keys.BIOMETRIC_LOCK_ENABLED] ?: false,
            lockOnBackground = prefs[Keys.LOCK_ON_BACKGROUND] ?: true
        )
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = themeMode.name }
    }

    suspend fun setPalette(palette: AppPalette) {
        context.dataStore.edit { it[Keys.PALETTE] = palette.name }
    }

    suspend fun setCustomAppName(appName: String) {
        context.dataStore.edit { it[Keys.CUSTOM_APP_NAME] = appName.trim() }
    }

    suspend fun setShopName(shopName: String) {
        context.dataStore.edit { it[Keys.SHOP_NAME] = shopName.trim() }
    }

    suspend fun setUseBanglaDigits(enabled: Boolean) {
        context.dataStore.edit { it[Keys.USE_BANGLA_DIGITS] = enabled }
    }

    suspend fun setBannerImagePath(path: String) {
        context.dataStore.edit { it[Keys.BANNER_IMAGE_PATH] = path }
    }

    suspend fun setProfileImagePath(path: String) {
        context.dataStore.edit { it[Keys.PROFILE_IMAGE_PATH] = path }
    }

    suspend fun setSaveDirectory(uri: String, label: String) {
        context.dataStore.edit {
            it[Keys.SAVE_DIRECTORY_URI] = uri
            it[Keys.SAVE_DIRECTORY_LABEL] = label
        }
    }

    suspend fun clearSaveDirectory() {
        context.dataStore.edit {
            it[Keys.SAVE_DIRECTORY_URI] = ""
            it[Keys.SAVE_DIRECTORY_LABEL] = ""
        }
    }

    suspend fun setAiProvider(provider: AiProvider) {
        context.dataStore.edit { it[Keys.AI_PROVIDER] = provider.name }
    }

    suspend fun setGeminiApiKey(apiKey: String) {
        context.dataStore.edit { it[Keys.GEMINI_API_KEY] = apiKey.trim() }
    }

    suspend fun setOpenRouterApiKey(apiKey: String) {
        context.dataStore.edit { it[Keys.OPENROUTER_API_KEY] = apiKey.trim() }
    }

    suspend fun setChatModel(model: GeminiChatModel) {
        context.dataStore.edit { it[Keys.CHAT_MODEL] = model.name }
    }

    suspend fun setOpenRouterModel(modelId: String) {
        val cleaned = modelId.trim().ifBlank { OpenRouterPresetModel.GEMINI_2_5_FLASH.modelId }
        context.dataStore.edit { it[Keys.OPENROUTER_MODEL] = cleaned }
    }

    suspend fun setChatRole(role: ChatRole) {
        context.dataStore.edit { it[Keys.CHAT_ROLE] = role.name }
    }

    suspend fun setCustomSystemInstruction(instruction: String) {
        context.dataStore.edit { it[Keys.CUSTOM_SYSTEM_INSTRUCTION] = instruction.trim() }
    }

    suspend fun setLastBackupTimestamp(timestamp: Long) {
        context.dataStore.edit { it[Keys.LAST_BACKUP_TIMESTAMP] = timestamp }
    }

    suspend fun setBiometricLockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BIOMETRIC_LOCK_ENABLED] = enabled }
    }

    suspend fun setLockOnBackground(enabled: Boolean) {
        context.dataStore.edit { it[Keys.LOCK_ON_BACKGROUND] = enabled }
    }
}
