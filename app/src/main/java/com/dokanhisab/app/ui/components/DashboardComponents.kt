package com.dokanhisab.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dokanhisab.app.data.local.TransactionType
import com.dokanhisab.app.data.local.TransactionWithCategory
import com.dokanhisab.app.data.preferences.UserPreferences
import com.dokanhisab.app.domain.DailyBarPoint
import com.dokanhisab.app.domain.HisabCalculator
import com.dokanhisab.app.domain.PeriodSummary
import com.dokanhisab.app.domain.TimePeriod
import com.dokanhisab.app.localization.AppStrings
import com.dokanhisab.app.ui.theme.LocalSemanticColors

@Composable
fun CashBalanceHeroCard(
    period: TimePeriod,
    summary: PeriodSummary,
    cashInHandSummary: PeriodSummary,
    preferences: UserPreferences,
    strings: AppStrings,
    onPeriodSelected: (TimePeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalSemanticColors.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
        colors = CardDefaults.cardColors(containerColor = semantic.heroGradientStart)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            semantic.heroGradientStart,
                            semantic.heroGradientMid,
                            semantic.heroGradientEnd
                        )
                    )
                )
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.88f, size.height * 0.12f),
                        radius = size.width * 0.52f
                    ),
                    radius = size.width * 0.52f,
                    center = Offset(size.width * 0.88f, size.height * 0.12f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.10f, size.height * 0.92f),
                        radius = size.width * 0.42f
                    ),
                    radius = size.width * 0.42f,
                    center = Offset(size.width * 0.10f, size.height * 0.92f)
                )
                val lineColor = Color.White.copy(alpha = 0.055f)
                val step = 32.dp.toPx()
                var y = step
                while (y < size.height) {
                    drawLine(
                        color = lineColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 0.8.dp.toPx()
                    )
                    y += step
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        TimePeriod.TODAY,
                        TimePeriod.THIS_MONTH,
                        TimePeriod.THIS_YEAR,
                        TimePeriod.ALL_TIME
                    ).forEach { item ->
                        val selected = item == period
                        val bg by animateColorAsState(
                            targetValue = if (selected) Color.White else Color.White.copy(alpha = 0.14f),
                            label = "period_bg"
                        )
                        val fg by animateColorAsState(
                            targetValue = if (selected) semantic.heroGradientStart else Color.White.copy(alpha = 0.92f),
                            label = "period_fg"
                        )
                        Surface(
                            modifier = Modifier
                                .testTag("period_chip_${item.name.lowercase()}")
                                .clip(RoundedCornerShape(50))
                                .clickable { onPeriodSelected(item) },
                            color = bg,
                            shape = RoundedCornerShape(50),
                            border = if (selected) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                        ) {
                            Text(
                                text = strings.periodLabel(item),
                                color = fg,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${strings.periodLabel(period)} • ${strings.netBalance}",
                            style = MaterialTheme.typography.labelLarge,
                            color = semantic.heroSubtleText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val isPositive = summary.netBalance >= 0
                        val netBalanceColor by animateColorAsState(
                            targetValue = if (isPositive) semantic.heroOnPrimary else Color(0xFFFFB4B8),
                            animationSpec = tween(220),
                            label = "hero_net_color"
                        )
                        Text(
                            text = HisabCalculator.formatAmount(
                                amount = summary.netBalance,
                                currencySymbol = preferences.currency.symbol,
                                language = preferences.language,
                                useBanglaDigits = preferences.useBanglaDigits,
                                includeSign = false
                            ),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = netBalanceColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val statusTag = if (isPositive) strings.profit else strings.loss
                        val countStr = HisabCalculator.formatCount(
                            summary.transactionCount,
                            preferences.language,
                            preferences.useBanglaDigits
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isPositive) {
                                Color(0xFF34D399).copy(alpha = 0.22f)
                            } else {
                                Color(0xFFFF8589).copy(alpha = 0.26f)
                            }
                        ) {
                            Text(
                                text = "$statusTag • $countStr ${strings.entriesCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isPositive) Color(0xFFA7F3D0) else Color(0xFFFFD4D6),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        color = semantic.heroBadgeBg,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AccountBalanceWallet,
                                    contentDescription = strings.cashInHand,
                                    tint = semantic.heroSubtleText,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = strings.cashInHand,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = semantic.heroSubtleText
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = HisabCalculator.formatAmount(
                                    amount = cashInHandSummary.netBalance,
                                    currencySymbol = preferences.currency.symbol,
                                    language = preferences.language,
                                    useBanglaDigits = preferences.useBanglaDigits
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = semantic.heroBadgeFg
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryMetricTile(
                        label = strings.totalIncome,
                        amountText = HisabCalculator.formatAmount(
                            amount = summary.totalIncome,
                            currencySymbol = preferences.currency.symbol,
                            language = preferences.language,
                            useBanglaDigits = preferences.useBanglaDigits
                        ),
                        containerColor = semantic.heroTileBg,
                        contentColor = semantic.heroOnPrimary,
                        labelColor = semantic.heroSubtleText,
                        badgeBg = Color(0xFF34D399).copy(alpha = 0.26f),
                        accentColor = Color(0xFF6EE7B7),
                        icon = Icons.AutoMirrored.Rounded.TrendingUp,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryMetricTile(
                        label = strings.totalExpense,
                        amountText = HisabCalculator.formatAmount(
                            amount = summary.totalExpense,
                            currencySymbol = preferences.currency.symbol,
                            language = preferences.language,
                            useBanglaDigits = preferences.useBanglaDigits
                        ),
                        containerColor = semantic.heroTileBg,
                        contentColor = semantic.heroOnPrimary,
                        labelColor = semantic.heroSubtleText,
                        badgeBg = Color(0xFFFF8589).copy(alpha = 0.26f),
                        accentColor = Color(0xFFFFB4B8),
                        icon = Icons.AutoMirrored.Rounded.TrendingDown,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryMetricTile(
    label: String,
    amountText: String,
    containerColor: Color,
    contentColor: Color,
    labelColor: Color,
    badgeBg: Color,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(badgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = labelColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = amountText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun QuickActionPair(
    strings: AppStrings,
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalSemanticColors.current
    val incomeInteraction = remember { MutableInteractionSource() }
    val expenseInteraction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onAddIncome,
            interactionSource = incomeInteraction,
            modifier = Modifier
                .weight(1f)
                .height(58.dp)
                .pressScale(incomeInteraction)
                .testTag("add_income_button"),
            shape = MaterialTheme.shapes.medium,
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = semantic.income,
                contentColor = semantic.onIncome
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = strings.addIncome,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Button(
            onClick = onAddExpense,
            interactionSource = expenseInteraction,
            modifier = Modifier
                .weight(1f)
                .height(58.dp)
                .pressScale(expenseInteraction)
                .testTag("add_expense_button"),
            shape = MaterialTheme.shapes.medium,
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = semantic.expense,
                contentColor = semantic.onExpense
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Remove,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = strings.addExpense,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun MonthlySummaryCard(
    monthlySummary: PeriodSummary,
    preferences: UserPreferences,
    strings: AppStrings,
    onClick: () -> Unit,
    onShareMonthlyPdf: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalSemanticColors.current
    val isProfit = monthlySummary.netBalance >= 0.0
    val monthLabel = remember(preferences.language, preferences.useBanglaDigits) {
        HisabCalculator.formatDateTime(
            timestamp = System.currentTimeMillis(),
            language = preferences.language,
            useBanglaDigits = preferences.useBanglaDigits,
            pattern = "MMMM yyyy"
        )
    }
    val incomeText = HisabCalculator.formatAmount(
        amount = monthlySummary.totalIncome,
        currencySymbol = preferences.currency.symbol,
        language = preferences.language,
        useBanglaDigits = preferences.useBanglaDigits
    )
    val expenseText = HisabCalculator.formatAmount(
        amount = monthlySummary.totalExpense,
        currencySymbol = preferences.currency.symbol,
        language = preferences.language,
        useBanglaDigits = preferences.useBanglaDigits
    )
    val netText = HisabCalculator.formatAmount(
        amount = monthlySummary.netBalance,
        currencySymbol = preferences.currency.symbol,
        language = preferences.language,
        useBanglaDigits = preferences.useBanglaDigits,
        includeSign = true
    )

    val combinedVolume = monthlySummary.totalIncome + monthlySummary.totalExpense
    val targetIncomeFraction = if (combinedVolume > 0.0) {
        (monthlySummary.totalIncome / combinedVolume).toFloat().coerceIn(0f, 1f)
    } else {
        0.5f
    }
    val incomeFraction by animateFloatAsState(
        targetValue = targetIncomeFraction,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "monthly_income_fraction"
    )

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_summary_card"),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = strings.monthlySummaryTitle,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = strings.monthlySummaryTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = monthLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        onClick = onShareMonthlyPdf,
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        modifier = Modifier.testTag("monthly_pdf_report_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PictureAsPdf,
                                contentDescription = strings.pdfShareButtonLabel,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = strings.pdfShareButtonLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1
                            )
                        }
                    }

                    val badgeBg = if (monthlySummary.netBalance == 0.0) {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    } else if (isProfit) {
                        semantic.incomeContainer
                    } else {
                        semantic.expenseContainer
                    }
                    val badgeFg = if (monthlySummary.netBalance == 0.0) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else if (isProfit) {
                        semantic.income
                    } else {
                        semantic.expense
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = badgeBg
                    ) {
                        Text(
                            text = if (isProfit) strings.profit else strings.loss,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = badgeFg,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    LegendDot(color = semantic.income, label = strings.totalIncome)
                    Text(
                        text = incomeText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = semantic.income,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    LegendDot(color = semantic.expense, label = strings.totalExpense)
                    Text(
                        text = expenseText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = semantic.expense,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            val emptyTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
            ) {
                if (combinedVolume <= 0.0) {
                    drawRoundRect(
                        color = emptyTrackColor,
                        size = size,
                        cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                    )
                } else {
                    val gapPx = if (monthlySummary.totalIncome > 0.0 && monthlySummary.totalExpense > 0.0) {
                        3.dp.toPx()
                    } else {
                        0f
                    }
                    val usableWidth = (size.width - gapPx).coerceAtLeast(0f)
                    val incomeWidth = usableWidth * incomeFraction
                    val expenseWidth = usableWidth - incomeWidth

                    if (incomeWidth > 0f) {
                        drawRoundRect(
                            color = semantic.income,
                            topLeft = Offset.Zero,
                            size = Size(incomeWidth, size.height),
                            cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                        )
                    }
                    if (expenseWidth > 0f) {
                        drawRoundRect(
                            color = semantic.expense,
                            topLeft = Offset(incomeWidth + gapPx, 0f),
                            size = Size(expenseWidth, size.height),
                            cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                        )
                    }
                }
            }

            HorizontalDivider(color = semantic.cardBorder.copy(alpha = 0.7f))

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (monthlySummary.netBalance == 0.0) {
                    MaterialTheme.colorScheme.surfaceContainerLow
                } else if (isProfit) {
                    semantic.incomeContainer.copy(alpha = 0.65f)
                } else {
                    semantic.expenseContainer.copy(alpha = 0.65f)
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = strings.netProfitLossFormulaLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$incomeText − $expenseText",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = netText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (monthlySummary.netBalance == 0.0) {
                            MaterialTheme.colorScheme.onSurface
                        } else if (isProfit) {
                            semantic.income
                        } else {
                            semantic.expense
                        },
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun SevenDayBarChartCard(
    bars: List<DailyBarPoint>,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    val semantic = LocalSemanticColors.current
    val maxVal = remember(bars) {
        bars.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(1.0) ?: 1.0
    }
    val gridColor = semantic.cardBorder.copy(alpha = 0.6f)
    var barsEntered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { barsEntered = true }
    val barProgress by animateFloatAsState(
        targetValue = if (barsEntered) 1f else 0f,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "seven_day_bar_progress"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, semantic.cardBorder),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.last7DaysChart,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LegendDot(color = semantic.income, label = strings.filterIncome)
                    LegendDot(color = semantic.expense, label = strings.filterExpense)
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(108.dp)
            ) {
                val count = bars.size.coerceAtLeast(1)
                val groupWidth = size.width / count
                val barWidth = (groupWidth * 0.28f).coerceAtMost(14.dp.toPx())
                val gap = 4.dp.toPx()

                drawLine(
                    color = gridColor,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = gridColor,
                    start = Offset(0f, size.height * 0.5f),
                    end = Offset(size.width, size.height * 0.5f),
                    strokeWidth = 0.75.dp.toPx()
                )

                bars.forEachIndexed { index, point ->
                    val centerX = groupWidth * index + groupWidth / 2f
                    val rawIncomeH = ((point.income / maxVal).toFloat() * (size.height - 6.dp.toPx()))
                        .coerceAtLeast(if (point.income > 0) 4.dp.toPx() else 2.dp.toPx())
                    val rawExpenseH = ((point.expense / maxVal).toFloat() * (size.height - 6.dp.toPx()))
                        .coerceAtLeast(if (point.expense > 0) 4.dp.toPx() else 2.dp.toPx())
                    val incomeH = (rawIncomeH * barProgress).coerceAtLeast(2.dp.toPx())
                    val expenseH = (rawExpenseH * barProgress).coerceAtLeast(2.dp.toPx())

                    drawRoundRect(
                        color = if (point.income > 0) semantic.income else semantic.income.copy(alpha = 0.16f),
                        topLeft = Offset(centerX - barWidth - gap / 2f, size.height - incomeH),
                        size = Size(barWidth, incomeH),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                    drawRoundRect(
                        color = if (point.expense > 0) semantic.expense else semantic.expense.copy(alpha = 0.16f),
                        topLeft = Offset(centerX + gap / 2f, size.height - expenseH),
                        size = Size(barWidth, expenseH),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                bars.forEach { point ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = point.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TransactionRowCard(
    item: TransactionWithCategory,
    preferences: UserPreferences,
    strings: AppStrings,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tx = item.transaction
    val cat = item.category
    val semantic = LocalSemanticColors.current
    val isIncome = tx.type == TransactionType.INCOME
    val accent = if (isIncome) semantic.income else semantic.expense
    val container = if (isIncome) semantic.incomeContainer else semantic.expenseContainer
    val catName = if (preferences.language == com.dokanhisab.app.data.preferences.AppLanguage.BANGLA) {
        cat.nameBn
    } else {
        cat.nameEn
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("transaction_card_${tx.id}")
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, semantic.cardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(container),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CategoryIconMapper.iconFor(cat.iconKey),
                    contentDescription = catName,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val partySuffix = if (tx.partyName.isNotBlank()) " • ${tx.partyName}" else ""
                Text(
                    text = "$catName • ${strings.paymentMethodLabel(tx.paymentMethod)}$partySuffix",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = HisabCalculator.formatDateTime(
                        timestamp = tx.timestamp,
                        language = preferences.language,
                        useBanglaDigits = preferences.useBanglaDigits
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            Text(
                text = HisabCalculator.formatAmount(
                    amount = if (isIncome) tx.amount else -tx.amount,
                    currencySymbol = preferences.currency.symbol,
                    language = preferences.language,
                    useBanglaDigits = preferences.useBanglaDigits,
                    includeSign = true
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1
            )
        }
    }
}
