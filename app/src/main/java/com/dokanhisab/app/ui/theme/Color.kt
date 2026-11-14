package com.dokanhisab.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.dokanhisab.app.data.preferences.AppPalette

@Immutable
data class HisabSemanticColors(
    val income: Color,
    val onIncome: Color,
    val incomeContainer: Color,
    val onIncomeContainer: Color,
    val expense: Color,
    val onExpense: Color,
    val expenseContainer: Color,
    val onExpenseContainer: Color,
    val heroGradientStart: Color,
    val heroGradientMid: Color,
    val heroGradientEnd: Color,
    val heroOnPrimary: Color,
    val heroSubtleText: Color,
    val heroBadgeBg: Color,
    val heroBadgeFg: Color,
    val heroTileBg: Color,
    val ledgerHeaderBg: Color,
    val ledgerHeaderFg: Color,
    val ledgerStripeBg: Color,
    val cardBorder: Color,
    val navBarBg: Color,
    val navIndicator: Color,
    val navBorder: Color,
    val swatchPreview: Color,
    val swatchAccent: Color
)

val LightSemanticColors = HisabSemanticColors(
    income = Color(0xFF0A7B47),
    onIncome = Color(0xFFFFFFFF),
    incomeContainer = Color(0xFFD8F5E5),
    onIncomeContainer = Color(0xFF05381F),
    expense = Color(0xFFC42B30),
    onExpense = Color(0xFFFFFFFF),
    expenseContainer = Color(0xFFFEE4E4),
    onExpenseContainer = Color(0xFF5D1114),
    heroGradientStart = Color(0xFF064E3B),
    heroGradientMid = Color(0xFF047857),
    heroGradientEnd = Color(0xFF0D9488),
    heroOnPrimary = Color(0xFFFFFFFF),
    heroSubtleText = Color(0xFFD1FAE5),
    heroBadgeBg = Color(0x2BFFFFFF),
    heroBadgeFg = Color(0xFFFFFFFF),
    heroTileBg = Color(0x1FFFFFFF),
    ledgerHeaderBg = Color(0xFF0B4F3C),
    ledgerHeaderFg = Color(0xFFFFFFFF),
    ledgerStripeBg = Color(0xFFF3F8F5),
    cardBorder = Color(0xFFD8E2DC),
    navBarBg = Color(0xFFFFFFFF),
    navIndicator = Color(0xFFC7F3DF),
    navBorder = Color(0xFFE0E8E3),
    swatchPreview = Color(0xFF047857),
    swatchAccent = Color(0xFF34D399)
)

val DarkSemanticColors = HisabSemanticColors(
    income = Color(0xFF5CE69D),
    onIncome = Color(0xFF00391B),
    incomeContainer = Color(0xFF103F27),
    onIncomeContainer = Color(0xFFC4F7D8),
    expense = Color(0xFFFF8589),
    onExpense = Color(0xFF5E0D12),
    expenseContainer = Color(0xFF4D191D),
    onExpenseContainer = Color(0xFFFFDADC),
    heroGradientStart = Color(0xFF072E24),
    heroGradientMid = Color(0xFF0B4A3A),
    heroGradientEnd = Color(0xFF0F6358),
    heroOnPrimary = Color(0xFFF2FBF7),
    heroSubtleText = Color(0xFFA7EBD0),
    heroBadgeBg = Color(0x26FFFFFF),
    heroBadgeFg = Color(0xFFFFFFFF),
    heroTileBg = Color(0x1CFFFFFF),
    ledgerHeaderBg = Color(0xFF123027),
    ledgerHeaderFg = Color(0xFFE4F4ED),
    ledgerStripeBg = Color(0xFF151E1B),
    cardBorder = Color(0xFF2B3933),
    navBarBg = Color(0xFF0F1614),
    navIndicator = Color(0xFF0F4C3A),
    navBorder = Color(0xFF23302B),
    swatchPreview = Color(0xFF34D399),
    swatchAccent = Color(0xFF065F46)
)

val LocalSemanticColors = staticCompositionLocalOf { LightSemanticColors }

private val EmeraldLight = lightColorScheme(
    primary = Color(0xFF047857),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC7F3DF),
    onPrimaryContainer = Color(0xFF002116),
    secondary = Color(0xFF366655),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD0EEE0),
    onSecondaryContainer = Color(0xFF0A261B),
    tertiary = Color(0xFFB45309),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFEF0D6),
    onTertiaryContainer = Color(0xFF3B1B00),
    background = Color(0xFFF5F8F6),
    onBackground = Color(0xFF111C18),
    surface = Color(0xFFF5F8F6),
    onSurface = Color(0xFF111C18),
    surfaceVariant = Color(0xFFE2ECE6),
    onSurfaceVariant = Color(0xFF42524B),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEEF4F0),
    surfaceContainer = Color(0xFFE7EFEA),
    surfaceContainerHigh = Color(0xFFDFE8E2),
    surfaceContainerHighest = Color(0xFFD6E2DB),
    outline = Color(0xFF6B7B73),
    outlineVariant = Color(0xFFC9D6CE)
)

private val EmeraldDark = darkColorScheme(
    primary = Color(0xFF4ADEA0),
    onPrimary = Color(0xFF003826),
    primaryContainer = Color(0xFF0A523B),
    onPrimaryContainer = Color(0xFFC7F3DF),
    secondary = Color(0xFF9ED0BC),
    onSecondary = Color(0xFF0E3729),
    secondaryContainer = Color(0xFF234D3F),
    onSecondaryContainer = Color(0xFFD0EEE0),
    tertiary = Color(0xFFFBBF68),
    onTertiary = Color(0xFF492900),
    tertiaryContainer = Color(0xFF683C00),
    onTertiaryContainer = Color(0xFFFEF0D6),
    background = Color(0xFF09100E),
    onBackground = Color(0xFFE3ECE7),
    surface = Color(0xFF09100E),
    onSurface = Color(0xFFE3ECE7),
    surfaceVariant = Color(0xFF222E29),
    onSurfaceVariant = Color(0xFFB4C4BC),
    surfaceContainerLowest = Color(0xFF0E1714),
    surfaceContainerLow = Color(0xFF141E1A),
    surfaceContainer = Color(0xFF192520),
    surfaceContainerHigh = Color(0xFF202E28),
    surfaceContainerHighest = Color(0xFF283831),
    outline = Color(0xFF819189),
    outlineVariant = Color(0xFF2C3B34)
)

private val RoyalBlueLight = lightColorScheme(
    primary = Color(0xFF1D4ED8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDBE6FF),
    onPrimaryContainer = Color(0xFF00184D),
    secondary = Color(0xFF475B82),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDCE4F7),
    onSecondaryContainer = Color(0xFF0F203D),
    tertiary = Color(0xFF0D7C72),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC4F3EC),
    onTertiaryContainer = Color(0xFF002622),
    background = Color(0xFFF5F7FC),
    onBackground = Color(0xFF121826),
    surface = Color(0xFFF5F7FC),
    onSurface = Color(0xFF121826),
    surfaceVariant = Color(0xFFE2E8F5),
    onSurfaceVariant = Color(0xFF424B5E),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEEF2FA),
    surfaceContainer = Color(0xFFE6ECF6),
    surfaceContainerHigh = Color(0xFFDDE5F2),
    surfaceContainerHighest = Color(0xFFD4DDEC),
    outline = Color(0xFF6C768B),
    outlineVariant = Color(0xFFC8D2E3)
)

private val RoyalBlueDark = darkColorScheme(
    primary = Color(0xFF8AB4FF),
    onPrimary = Color(0xFF002D75),
    primaryContainer = Color(0xFF14429E),
    onPrimaryContainer = Color(0xFFDBE6FF),
    secondary = Color(0xFFB2C4E8),
    onSecondary = Color(0xFF1B2E4E),
    secondaryContainer = Color(0xFF314466),
    onSecondaryContainer = Color(0xFFDCE4F7),
    tertiary = Color(0xFF6CD9CD),
    onTertiary = Color(0xFF003731),
    tertiaryContainer = Color(0xFF005048),
    onTertiaryContainer = Color(0xFFC4F3EC),
    background = Color(0xFF0A0F1A),
    onBackground = Color(0xFFE1E6F2),
    surface = Color(0xFF0A0F1A),
    onSurface = Color(0xFFE1E6F2),
    surfaceVariant = Color(0xFF232C3E),
    onSurfaceVariant = Color(0xFFB9C3D8),
    surfaceContainerLowest = Color(0xFF0F1624),
    surfaceContainerLow = Color(0xFF141D2E),
    surfaceContainer = Color(0xFF1A2438),
    surfaceContainerHigh = Color(0xFF212C43),
    surfaceContainerHighest = Color(0xFF293650),
    outline = Color(0xFF828DA4),
    outlineVariant = Color(0xFF2D3950)
)

private val ClassicAmberLight = lightColorScheme(
    primary = Color(0xFFB45309),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFE2BD),
    onPrimaryContainer = Color(0xFF331700),
    secondary = Color(0xFF6E573E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF8E0C5),
    onSecondaryContainer = Color(0xFF271807),
    tertiary = Color(0xFF186F4D),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC6F2DC),
    onTertiaryContainer = Color(0xFF042617),
    background = Color(0xFFFAF7F2),
    onBackground = Color(0xFF1F1A14),
    surface = Color(0xFFFAF7F2),
    onSurface = Color(0xFF1F1A14),
    surfaceVariant = Color(0xFFF0E6D7),
    onSurfaceVariant = Color(0xFF524637),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5EFE5),
    surfaceContainer = Color(0xFFEFE7DA),
    surfaceContainerHigh = Color(0xFFE8DED0),
    surfaceContainerHighest = Color(0xFFE0D4C3),
    outline = Color(0xFF827462),
    outlineVariant = Color(0xFFD8CBB9)
)

private val ClassicAmberDark = darkColorScheme(
    primary = Color(0xFFFFBA66),
    onPrimary = Color(0xFF4E2600),
    primaryContainer = Color(0xFF783D04),
    onPrimaryContainer = Color(0xFFFFE2BD),
    secondary = Color(0xFFDBC2A4),
    onSecondary = Color(0xFF3D2B16),
    secondaryContainer = Color(0xFF55412A),
    onSecondaryContainer = Color(0xFFF8E0C5),
    tertiary = Color(0xFF7AD9AE),
    onTertiary = Color(0xFF063822),
    tertiaryContainer = Color(0xFF135237),
    onTertiaryContainer = Color(0xFFC6F2DC),
    background = Color(0xFF120F0B),
    onBackground = Color(0xFFEDE2D6),
    surface = Color(0xFF120F0B),
    onSurface = Color(0xFFEDE2D6),
    surfaceVariant = Color(0xFF30281F),
    onSurfaceVariant = Color(0xFFD1C3B2),
    surfaceContainerLowest = Color(0xFF18140F),
    surfaceContainerLow = Color(0xFF1F1A13),
    surfaceContainer = Color(0xFF262018),
    surfaceContainerHigh = Color(0xFF2F281E),
    surfaceContainerHighest = Color(0xFF393125),
    outline = Color(0xFF9B8C7A),
    outlineVariant = Color(0xFF42382B)
)

private val SlateGrayLight = lightColorScheme(
    primary = Color(0xFF334155),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE5F0),
    onPrimaryContainer = Color(0xFF0F172A),
    secondary = Color(0xFF4E5D6C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0E7EE),
    onSecondaryContainer = Color(0xFF17212B),
    tertiary = Color(0xFF0F766E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC7F2EC),
    onTertiaryContainer = Color(0xFF002420),
    background = Color(0xFFF6F7F9),
    onBackground = Color(0xFF151920),
    surface = Color(0xFFF6F7F9),
    onSurface = Color(0xFF151920),
    surfaceVariant = Color(0xFFE3E7EE),
    onSurfaceVariant = Color(0xFF434B56),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEEF1F5),
    surfaceContainer = Color(0xFFE7EBEF),
    surfaceContainerHigh = Color(0xFFDFE4EA),
    surfaceContainerHighest = Color(0xFFD7DDE4),
    outline = Color(0xFF6F7885),
    outlineVariant = Color(0xFFC9D0DA)
)

private val SlateGrayDark = darkColorScheme(
    primary = Color(0xFFAEBFD4),
    onPrimary = Color(0xFF172433),
    primaryContainer = Color(0xFF2B3A4D),
    onPrimaryContainer = Color(0xFFDCE5F0),
    secondary = Color(0xFFB9C6D4),
    onSecondary = Color(0xFF222F3B),
    secondaryContainer = Color(0xFF384654),
    onSecondaryContainer = Color(0xFFE0E7EE),
    tertiary = Color(0xFF64D8CB),
    onTertiary = Color(0xFF003732),
    tertiaryContainer = Color(0xFF005049),
    onTertiaryContainer = Color(0xFFC7F2EC),
    background = Color(0xFF0B0E14),
    onBackground = Color(0xFFE2E6EC),
    surface = Color(0xFF0B0E14),
    onSurface = Color(0xFFE2E6EC),
    surfaceVariant = Color(0xFF252B34),
    onSurfaceVariant = Color(0xFFBEC6D0),
    surfaceContainerLowest = Color(0xFF11151D),
    surfaceContainerLow = Color(0xFF161C26),
    surfaceContainer = Color(0xFF1C232F),
    surfaceContainerHigh = Color(0xFF232B39),
    surfaceContainerHighest = Color(0xFF2B3545),
    outline = Color(0xFF88919E),
    outlineVariant = Color(0xFF323B49)
)

private val CrimsonRoseLight = lightColorScheme(
    primary = Color(0xFF9F1239),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD9E1),
    onPrimaryContainer = Color(0xFF3F0013),
    secondary = Color(0xFF6F4D56),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF8DCE3),
    onSecondaryContainer = Color(0xFF2B131A),
    tertiary = Color(0xFF0F7056),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC7F1E2),
    onTertiaryContainer = Color(0xFF00241A),
    background = Color(0xFFFBF6F7),
    onBackground = Color(0xFF20171A),
    surface = Color(0xFFFBF6F7),
    onSurface = Color(0xFF20171A),
    surfaceVariant = Color(0xFFF1E3E7),
    onSurfaceVariant = Color(0xFF534247),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF6ECEF),
    surfaceContainer = Color(0xFFF0E4E8),
    surfaceContainerHigh = Color(0xFFE9DCE0),
    surfaceContainerHighest = Color(0xFFE2D3D8),
    outline = Color(0xFF847176),
    outlineVariant = Color(0xFFD9C5CA)
)

private val CrimsonRoseDark = darkColorScheme(
    primary = Color(0xFFFFAEC0),
    onPrimary = Color(0xFF64001F),
    primaryContainer = Color(0xFF881337),
    onPrimaryContainer = Color(0xFFFFD9E1),
    secondary = Color(0xFFDDBAC3),
    onSecondary = Color(0xFF3F222A),
    secondaryContainer = Color(0xFF583841),
    onSecondaryContainer = Color(0xFFF8DCE3),
    tertiary = Color(0xFF72DAB9),
    onTertiary = Color(0xFF00382A),
    tertiaryContainer = Color(0xFF00513D),
    onTertiaryContainer = Color(0xFFC7F1E2),
    background = Color(0xFF130B0E),
    onBackground = Color(0xFFEEE0E3),
    surface = Color(0xFF130B0E),
    onSurface = Color(0xFFEEE0E3),
    surfaceVariant = Color(0xFF33252A),
    onSurfaceVariant = Color(0xFFD4C1C6),
    surfaceContainerLowest = Color(0xFF191013),
    surfaceContainerLow = Color(0xFF21161A),
    surfaceContainer = Color(0xFF281B20),
    surfaceContainerHigh = Color(0xFF322228),
    surfaceContainerHighest = Color(0xFF3C2A31),
    outline = Color(0xFF9C898E),
    outlineVariant = Color(0xFF47353B)
)

fun paletteSwatchColor(palette: AppPalette): Color = when (palette) {
    AppPalette.EMERALD -> Color(0xFF047857)
    AppPalette.ROYAL_BLUE -> Color(0xFF1D4ED8)
    AppPalette.CLASSIC_AMBER -> Color(0xFFB45309)
    AppPalette.SLATE_GRAY -> Color(0xFF334155)
    AppPalette.CRIMSON_ROSE -> Color(0xFF9F1239)
}

fun paletteAccentSwatchColor(palette: AppPalette): Color = when (palette) {
    AppPalette.EMERALD -> Color(0xFF34D399)
    AppPalette.ROYAL_BLUE -> Color(0xFF60A5FA)
    AppPalette.CLASSIC_AMBER -> Color(0xFFFBBF24)
    AppPalette.SLATE_GRAY -> Color(0xFF94A3B8)
    AppPalette.CRIMSON_ROSE -> Color(0xFFFB7185)
}

fun resolveColorScheme(palette: AppPalette, darkTheme: Boolean): ColorScheme {
    return when (palette) {
        AppPalette.EMERALD -> if (darkTheme) EmeraldDark else EmeraldLight
        AppPalette.ROYAL_BLUE -> if (darkTheme) RoyalBlueDark else RoyalBlueLight
        AppPalette.CLASSIC_AMBER -> if (darkTheme) ClassicAmberDark else ClassicAmberLight
        AppPalette.SLATE_GRAY -> if (darkTheme) SlateGrayDark else SlateGrayLight
        AppPalette.CRIMSON_ROSE -> if (darkTheme) CrimsonRoseDark else CrimsonRoseLight
    }
}

fun resolveSemanticColors(palette: AppPalette, darkTheme: Boolean): HisabSemanticColors {
    val base = if (darkTheme) DarkSemanticColors else LightSemanticColors
    return when (palette) {
        AppPalette.EMERALD -> if (darkTheme) {
            base.copy(
                heroGradientStart = Color(0xFF062C22),
                heroGradientMid = Color(0xFF094B3A),
                heroGradientEnd = Color(0xFF0E6A5C),
                heroSubtleText = Color(0xFFA7F3D0),
                ledgerHeaderBg = Color(0xFF112F26),
                ledgerStripeBg = Color(0xFF141E1A),
                cardBorder = Color(0xFF283831),
                navBarBg = Color(0xFF0E1714),
                navIndicator = Color(0xFF0D4E3B),
                navBorder = Color(0xFF22302A),
                swatchPreview = paletteSwatchColor(palette),
                swatchAccent = paletteAccentSwatchColor(palette)
            )
        } else {
            base.copy(
                heroGradientStart = Color(0xFF064E3B),
                heroGradientMid = Color(0xFF047857),
                heroGradientEnd = Color(0xFF0D9488),
                heroSubtleText = Color(0xFFD1FAE5),
                ledgerHeaderBg = Color(0xFF09523D),
                ledgerStripeBg = Color(0xFFF3F8F5),
                cardBorder = Color(0xFFD6E3DC),
                navBarBg = Color(0xFFFFFFFF),
                navIndicator = Color(0xFFC7F3DF),
                navBorder = Color(0xFFDEE8E2),
                swatchPreview = paletteSwatchColor(palette),
                swatchAccent = paletteAccentSwatchColor(palette)
            )
        }

        AppPalette.ROYAL_BLUE -> if (darkTheme) {
            base.copy(
                heroGradientStart = Color(0xFF0F1B3D),
                heroGradientMid = Color(0xFF173170),
                heroGradientEnd = Color(0xFF1D4ED8),
                heroSubtleText = Color(0xFFBFDBFE),
                ledgerHeaderBg = Color(0xFF152546),
                ledgerStripeBg = Color(0xFF131B2B),
                cardBorder = Color(0xFF2A364E),
                navBarBg = Color(0xFF0F1624),
                navIndicator = Color(0xFF173D8C),
                navBorder = Color(0xFF232F45),
                swatchPreview = paletteSwatchColor(palette),
                swatchAccent = paletteAccentSwatchColor(palette)
            )
        } else {
            base.copy(
                heroGradientStart = Color(0xFF172554),
                heroGradientMid = Color(0xFF1E40AF),
                heroGradientEnd = Color(0xFF2563EB),
                heroSubtleText = Color(0xFFDBEAFE),
                ledgerHeaderBg = Color(0xFF163D9B),
                ledgerStripeBg = Color(0xFFF2F5FC),
                cardBorder = Color(0xFFD4DDF0),
                navBarBg = Color(0xFFFFFFFF),
                navIndicator = Color(0xFFDBE6FF),
                navBorder = Color(0xFFDEE5F2),
                swatchPreview = paletteSwatchColor(palette),
                swatchAccent = paletteAccentSwatchColor(palette)
            )
        }

        AppPalette.CLASSIC_AMBER -> if (darkTheme) {
            base.copy(
                heroGradientStart = Color(0xFF2E1504),
                heroGradientMid = Color(0xFF5C2B08),
                heroGradientEnd = Color(0xFF92400E),
                heroSubtleText = Color(0xFFFDE68A),
                ledgerHeaderBg = Color(0xFF332111),
                ledgerStripeBg = Color(0xFF1D1812),
                cardBorder = Color(0xFF423628),
                navBarBg = Color(0xFF18140F),
                navIndicator = Color(0xFF6B3806),
                navBorder = Color(0xFF352B20),
                swatchPreview = paletteSwatchColor(palette),
                swatchAccent = paletteAccentSwatchColor(palette)
            )
        } else {
            base.copy(
                heroGradientStart = Color(0xFF451A03),
                heroGradientMid = Color(0xFF78350F),
                heroGradientEnd = Color(0xFFB45309),
                heroSubtleText = Color(0xFFFEF3C7),
                ledgerHeaderBg = Color(0xFF803D06),
                ledgerStripeBg = Color(0xFFFAF4EC),
                cardBorder = Color(0xFFE4D8C7),
                navBarBg = Color(0xFFFFFFFF),
                navIndicator = Color(0xFFFFE2BD),
                navBorder = Color(0xFFEAE0D2),
                swatchPreview = paletteSwatchColor(palette),
                swatchAccent = paletteAccentSwatchColor(palette)
            )
        }

        AppPalette.SLATE_GRAY -> if (darkTheme) {
            base.copy(
                heroGradientStart = Color(0xFF0F172A),
                heroGradientMid = Color(0xFF1E293B),
                heroGradientEnd = Color(0xFF334155),
                heroSubtleText = Color(0xFFCBD5E1),
                ledgerHeaderBg = Color(0xFF1C2430),
                ledgerStripeBg = Color(0xFF141922),
                cardBorder = Color(0xFF2F3846),
                navBarBg = Color(0xFF11151D),
                navIndicator = Color(0xFF2A394D),
                navBorder = Color(0xFF272F3C),
                swatchPreview = paletteSwatchColor(palette),
                swatchAccent = paletteAccentSwatchColor(palette)
            )
        } else {
            base.copy(
                heroGradientStart = Color(0xFF0F172A),
                heroGradientMid = Color(0xFF1E293B),
                heroGradientEnd = Color(0xFF334155),
                heroSubtleText = Color(0xFFE2E8F0),
                ledgerHeaderBg = Color(0xFF263344),
                ledgerStripeBg = Color(0xFFF3F5F8),
                cardBorder = Color(0xFFD5DCE6),
                navBarBg = Color(0xFFFFFFFF),
                navIndicator = Color(0xFFDCE5F0),
                navBorder = Color(0xFFE0E5EC),
                swatchPreview = paletteSwatchColor(palette),
                swatchAccent = paletteAccentSwatchColor(palette)
            )
        }

        AppPalette.CRIMSON_ROSE -> if (darkTheme) {
            base.copy(
                heroGradientStart = Color(0xFF380615),
                heroGradientMid = Color(0xFF6B0D2B),
                heroGradientEnd = Color(0xFF9F1239),
                heroSubtleText = Color(0xFFFECDD3),
                ledgerHeaderBg = Color(0xFF361822),
                ledgerStripeBg = Color(0xFF1E1418),
                cardBorder = Color(0xFF47333A),
                navBarBg = Color(0xFF191013),
                navIndicator = Color(0xFF751231),
                navBorder = Color(0xFF3A292F),
                swatchPreview = paletteSwatchColor(palette),
                swatchAccent = paletteAccentSwatchColor(palette)
            )
        } else {
            base.copy(
                heroGradientStart = Color(0xFF4C0519),
                heroGradientMid = Color(0xFF881337),
                heroGradientEnd = Color(0xFFBE123C),
                heroSubtleText = Color(0xFFFFE4E6),
                ledgerHeaderBg = Color(0xFF881337),
                ledgerStripeBg = Color(0xFFFAF2F4),
                cardBorder = Color(0xFFE6D4D9),
                navBarBg = Color(0xFFFFFFFF),
                navIndicator = Color(0xFFFFD9E1),
                navBorder = Color(0xFFECDCE0),
                swatchPreview = paletteSwatchColor(palette),
                swatchAccent = paletteAccentSwatchColor(palette)
            )
        }
    }
}
