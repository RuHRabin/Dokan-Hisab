package com.dokanhisab.app.data.preferences

enum class AppLanguage(val code: String) {
    BANGLA("bn"),
    ENGLISH("en")
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class AppPalette(val key: String) {
    EMERALD("emerald"),
    ROYAL_BLUE("royal_blue"),
    CLASSIC_AMBER("classic_amber"),
    SLATE_GRAY("slate_gray"),
    CRIMSON_ROSE("crimson_rose")
}

enum class CurrencyOption(val symbol: String, val code: String) {
    BDT("৳", "BDT")
}

enum class GeminiChatModel(val modelId: String) {
    HYDROGEN("gemini-4-hydrogen"),
    CARBON("gemini-4-carbon"),
    ARGON("gemini-4-argon"),
    FLASH_LITE("gemini-3.1-flash-lite")
}

enum class AiProvider(val displayName: String) {
    GEMINI("Gemini"),
    OPENROUTER("OpenRouter")
}

enum class OpenRouterPresetModel(val modelId: String, val label: String) {
    GEMINI_2_5_FLASH("google/gemini-2.5-flash", "Gemini 2.5 Flash"),
    GPT_4O_MINI("openai/gpt-4o-mini", "GPT-4o Mini"),
    CLAUDE_HAIKU("anthropic/claude-3.5-haiku", "Claude 3.5 Haiku"),
    LLAMA_3_3_70B("meta-llama/llama-3.3-70b-instruct", "Llama 3.3 70B"),
    DEEPSEEK_V3_FREE("deepseek/deepseek-chat-v3-0324:free", "DeepSeek V3 Free")
}

enum class ChatRole {
    SHOP_ACCOUNTANT,
    BUSINESS_ADVISOR,
    MEMO_WRITER,
    CUSTOM
}

data class UserPreferences(
    val language: AppLanguage = AppLanguage.BANGLA,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val palette: AppPalette = AppPalette.EMERALD,
    val customAppName: String = "",
    val shopName: String = "",
    val currency: CurrencyOption = CurrencyOption.BDT,
    val useBanglaDigits: Boolean = true,
    val bannerImagePath: String = "",
    val profileImagePath: String = "",
    val saveDirectoryUri: String = "",
    val saveDirectoryLabel: String = "",
    val aiProvider: AiProvider = AiProvider.GEMINI,
    val geminiApiKey: String = "",
    val openRouterApiKey: String = "",
    val selectedChatModel: GeminiChatModel = GeminiChatModel.HYDROGEN,
    val selectedOpenRouterModel: String = OpenRouterPresetModel.GEMINI_2_5_FLASH.modelId,
    val selectedChatRole: ChatRole = ChatRole.SHOP_ACCOUNTANT,
    val customSystemInstruction: String = "",
    val lastBackupTimestamp: Long = 0L,
    val biometricLockEnabled: Boolean = false,
    val lockOnBackground: Boolean = true
)
