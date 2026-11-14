package com.dokanhisab.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dokanhisab.app.data.preferences.UserPreferences
import com.dokanhisab.app.domain.HisabCalculator
import com.dokanhisab.app.domain.MonthlyTrendPoint
import com.dokanhisab.app.localization.AppStrings
import com.dokanhisab.app.ui.theme.LocalSemanticColors
import kotlin.math.roundToInt

data class TrendGrowthSummary(
    val totalSixMonthNet: Double,
    val totalSixMonthIncome: Double,
    val totalSixMonthExpense: Double,
    val monthOverMonthIncomeChangePercent: Int?
)

fun computeTrendGrowthSummary(points: List<MonthlyTrendPoint>): TrendGrowthSummary {
    if (points.isEmpty()) {
        return TrendGrowthSummary(0.0, 0.0, 0.0, null)
    }
    var totalInc = 0.0
    var totalExp = 0.0
    points.forEach {
        totalInc += it.income
        totalExp += it.expense
    }
    val growthPct = if (points.size >= 2) {
        val current = points.last().income
        val prev = points[points.size - 2].income
        if (prev > 0.0) {
            (((current - prev) / prev) * 100.0).roundToInt()
        } else {
            null
        }
    } else {
        null
    }
    return TrendGrowthSummary(
        totalSixMonthNet = totalInc - totalExp,
        totalSixMonthIncome = totalInc,
        totalSixMonthExpense = totalExp,
        monthOverMonthIncomeChangePercent = growthPct
    )
}

/**
 * Scales the element slightly while it is pressed.
 */
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.965f
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "press_scale"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
fun SixMonthTrendChartCard(
    points: List<MonthlyTrendPoint>,
    preferences: UserPreferences,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    val semantic = LocalSemanticColors.current
    val haptic = LocalHapticFeedback.current
    val currency = preferences.currency.symbol
    val lang = preferences.language
    val banglaDigits = preferences.useBanglaDigits

    var showIncome by remember { mutableStateOf(true) }
    var showExpense by remember { mutableStateOf(true) }
    var selectedIndex by remember(points.size) { mutableIntStateOf(points.lastIndex) }

    var chartVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { chartVisible = true }
    val drawProgress by animateFloatAsState(
        targetValue = if (chartVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 680, easing = FastOutSlowInEasing),
        label = "trend_draw_progress"
    )

    val growthSummary = remember(points) { computeTrendGrowthSummary(points) }
    val hasAnyData = growthSummary.totalSixMonthIncome > 0.0 || growthSummary.totalSixMonthExpense > 0.0

    val rawMax = remember(points, showIncome, showExpense) {
        points.maxOfOrNull { pt ->
            val inc = if (showIncome) pt.income else 0.0
            val exp = if (showExpense) pt.expense else 0.0
            maxOf(inc, exp)
        } ?: 0.0
    }
    val chartMax = remember(rawMax) {
        if (rawMax <= 0.0) 1000.0 else rawMax * 1.15
    }

    val safeIndex = selectedIndex.coerceIn(0, points.lastIndex)
    val activePoint = points[safeIndex]

    val incomeLinePath = remember { Path() }
    val incomeAreaPath = remember { Path() }
    val expenseLinePath = remember { Path() }
    val expenseAreaPath = remember { Path() }
    val gridDashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(9f, 8f), 0f) }
    val cursorDashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f) }

    val gridColor = semantic.cardBorder.copy(alpha = 0.7f)
    val cursorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
    val surfaceLowest = MaterialTheme.colorScheme.surfaceContainerLowest

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("six_month_trend_card"),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, semantic.cardBorder),
        colors = CardDefaults.cardColors(containerColor = surfaceLowest)
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
                            imageVector = Icons.AutoMirrored.Rounded.ShowChart,
                            contentDescription = strings.sixMonthTrendTitle,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = strings.sixMonthTrendTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = strings.sixMonthTrendSubtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                val momPct = growthSummary.monthOverMonthIncomeChangePercent
                val isGrowthPositive = if (momPct != null) momPct >= 0 else growthSummary.totalSixMonthNet >= 0.0
                val badgeBg = if (!hasAnyData) {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                } else if (isGrowthPositive) {
                    semantic.incomeContainer
                } else {
                    semantic.expenseContainer
                }
                val badgeFg = if (!hasAnyData) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else if (isGrowthPositive) {
                    semantic.income
                } else {
                    semantic.expense
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = badgeBg,
                    modifier = Modifier.testTag("six_month_growth_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (hasAnyData) {
                            Icon(
                                imageVector = if (isGrowthPositive) {
                                    Icons.AutoMirrored.Rounded.TrendingUp
                                } else {
                                    Icons.AutoMirrored.Rounded.TrendingDown
                                },
                                contentDescription = null,
                                tint = badgeFg,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        val badgeLabel = if (momPct != null && hasAnyData) {
                            val sign = if (momPct > 0) "+" else ""
                            val pctText = HisabCalculator.formatCount(momPct, lang, banglaDigits)
                            "$sign$pctText%"
                        } else {
                            "${strings.sixMonthNetGrowth}: ${
                                HisabCalculator.formatCompactAxisAmount(
                                    amount = growthSummary.totalSixMonthNet,
                                    currencySymbol = currency,
                                    language = lang,
                                    useBanglaDigits = banglaDigits
                                )
                            }"
                        }
                        Text(
                            text = badgeLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = badgeFg,
                            maxLines = 1
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SeriesTogglePill(
                        label = strings.filterIncome,
                        color = semantic.income,
                        selected = showIncome,
                        onClick = {
                            if (!showIncome || showExpense) showIncome = !showIncome
                        },
                        testTag = "trend_toggle_income"
                    )
                    SeriesTogglePill(
                        label = strings.filterExpense,
                        color = semantic.expense,
                        selected = showExpense,
                        onClick = {
                            if (!showExpense || showIncome) showExpense = !showExpense
                        },
                        testTag = "trend_toggle_expense"
                    )
                }

                Text(
                    text = "${strings.sixMonthNetGrowth}: ${
                        HisabCalculator.formatAmount(
                            amount = growthSummary.totalSixMonthNet,
                            currencySymbol = currency,
                            language = lang,
                            useBanglaDigits = banglaDigits,
                            includeSign = true
                        )
                    }",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (growthSummary.totalSixMonthNet >= 0.0) semantic.income else semantic.expense,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, semantic.cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("six_month_tooltip_card")
            ) {
                AnimatedContent(
                    targetState = activePoint,
                    transitionSpec = {
                        fadeIn(tween(140)) togetherWith fadeOut(tween(110))
                    },
                    label = "tooltip_month_transition"
                ) { point ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = point.fullMonthLabel,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${strings.chartNetLabel}: ${
                                    HisabCalculator.formatAmount(
                                        amount = point.netProfit,
                                        currencySymbol = currency,
                                        language = lang,
                                        useBanglaDigits = banglaDigits,
                                        includeSign = true
                                    )
                                }",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (point.netProfit >= 0.0) semantic.income else semantic.expense
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (showIncome) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = strings.filterIncome,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = HisabCalculator.formatAmount(
                                            amount = point.income,
                                            currencySymbol = currency,
                                            language = lang,
                                            useBanglaDigits = banglaDigits
                                        ),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = semantic.income
                                    )
                                }
                            }
                            if (showExpense) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = strings.filterExpense,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = HisabCalculator.formatAmount(
                                            amount = point.expense,
                                            currencySymbol = currency,
                                            language = lang,
                                            useBanglaDigits = banglaDigits
                                        ),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = semantic.expense
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(172.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .width(50.dp)
                        .fillMaxHeight()
                        .padding(top = 6.dp, bottom = 6.dp, end = 6.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = HisabCalculator.formatCompactAxisAmount(chartMax, currency, lang, banglaDigits),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Text(
                        text = HisabCalculator.formatCompactAxisAmount(chartMax * 0.5, currency, lang, banglaDigits),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Text(
                        text = HisabCalculator.formatCompactAxisAmount(0.0, currency, lang, banglaDigits),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("six_month_trend_canvas")
                            .pointerInput(points.size) {
                                detectTapGestures { offset ->
                                    val count = points.size
                                    if (count > 1) {
                                        val padX = 10.dp.toPx()
                                        val usableW = (size.width - padX * 2f).coerceAtLeast(1f)
                                        val fraction = ((offset.x - padX) / usableW).coerceIn(0f, 1f)
                                        val idx = (fraction * (count - 1)).roundToInt().coerceIn(0, count - 1)
                                        if (idx != selectedIndex) {
                                            selectedIndex = idx
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    }
                                }
                            }
                            .pointerInput(points.size) {
                                detectHorizontalDragGestures { change, _ ->
                                    change.consume()
                                    val count = points.size
                                    if (count > 1) {
                                        val padX = 10.dp.toPx()
                                        val usableW = (size.width - padX * 2f).coerceAtLeast(1f)
                                        val fraction = ((change.position.x - padX) / usableW).coerceIn(0f, 1f)
                                        val idx = (fraction * (count - 1)).roundToInt().coerceIn(0, count - 1)
                                        if (idx != selectedIndex) {
                                            selectedIndex = idx
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    }
                                }
                            }
                    ) {
                        val padTop = 10.dp.toPx()
                        val padBottom = 10.dp.toPx()
                        val padX = 10.dp.toPx()
                        val plotW = (size.width - padX * 2f).coerceAtLeast(1f)
                        val plotH = (size.height - padTop - padBottom).coerceAtLeast(1f)
                        val bottomY = padTop + plotH

                        listOf(padTop, padTop + plotH * 0.5f, bottomY).forEachIndexed { idx, y ->
                            drawLine(
                                color = gridColor,
                                start = Offset(padX, y),
                                end = Offset(padX + plotW, y),
                                strokeWidth = if (idx == 2) 1.2.dp.toPx() else 0.9.dp.toPx(),
                                pathEffect = if (idx == 2) null else gridDashEffect
                            )
                        }

                        val count = points.size
                        val stepX = if (count > 1) plotW / (count - 1) else 0f

                        fun pointX(index: Int): Float = padX + stepX * index
                        fun pointY(value: Double): Float {
                            val ratio = (value / chartMax).toFloat().coerceIn(0f, 1f)
                            return bottomY - ratio * plotH
                        }

                        val activeX = pointX(safeIndex)
                        drawLine(
                            color = cursorColor,
                            start = Offset(activeX, padTop),
                            end = Offset(activeX, bottomY),
                            strokeWidth = 1.2.dp.toPx(),
                            pathEffect = cursorDashEffect
                        )

                        fun buildMonotoneSpline(
                            linePath: Path,
                            areaPath: Path,
                            valueSelector: (MonthlyTrendPoint) -> Double
                        ) {
                            linePath.reset()
                            areaPath.reset()
                            if (points.isEmpty()) return

                            val firstX = pointX(0)
                            val firstY = pointY(valueSelector(points[0]))
                            linePath.moveTo(firstX, firstY)
                            areaPath.moveTo(firstX, bottomY)
                            areaPath.lineTo(firstX, firstY)

                            for (i in 0 until points.lastIndex) {
                                val x0 = pointX(i)
                                val y0 = pointY(valueSelector(points[i]))
                                val x1 = pointX(i + 1)
                                val y1 = pointY(valueSelector(points[i + 1]))
                                val c1x = x0 + (x1 - x0) * 0.45f
                                val c2x = x1 - (x1 - x0) * 0.45f
                                linePath.cubicTo(c1x, y0, c2x, y1, x1, y1)
                                areaPath.cubicTo(c1x, y0, c2x, y1, x1, y1)
                            }
                            val lastX = pointX(points.lastIndex)
                            areaPath.lineTo(lastX, bottomY)
                            areaPath.close()
                        }

                        clipRect(
                            left = 0f,
                            top = 0f,
                            right = padX + plotW * drawProgress + 12.dp.toPx(),
                            bottom = size.height
                        ) {
                            if (showExpense) {
                                buildMonotoneSpline(expenseLinePath, expenseAreaPath) { it.expense }
                                drawPath(
                                    path = expenseAreaPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            semantic.expense.copy(alpha = 0.16f),
                                            Color.Transparent
                                        ),
                                        startY = padTop,
                                        endY = bottomY
                                    )
                                )
                                drawPath(
                                    path = expenseLinePath,
                                    color = semantic.expense,
                                    style = Stroke(
                                        width = 2.6.dp.toPx(),
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }

                            if (showIncome) {
                                buildMonotoneSpline(incomeLinePath, incomeAreaPath) { it.income }
                                drawPath(
                                    path = incomeAreaPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            semantic.income.copy(alpha = 0.24f),
                                            Color.Transparent
                                        ),
                                        startY = padTop,
                                        endY = bottomY
                                    )
                                )
                                drawPath(
                                    path = incomeLinePath,
                                    color = semantic.income,
                                    style = Stroke(
                                        width = 3.dp.toPx(),
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }

                            points.forEachIndexed { idx, pt ->
                                val x = pointX(idx)
                                val isActive = idx == safeIndex

                                if (showExpense) {
                                    val y = pointY(pt.expense)
                                    if (isActive) {
                                        drawCircle(
                                            color = semantic.expense.copy(alpha = 0.22f),
                                            radius = 9.dp.toPx(),
                                            center = Offset(x, y)
                                        )
                                        drawCircle(
                                            color = semantic.expense,
                                            radius = 5.5.dp.toPx(),
                                            center = Offset(x, y)
                                        )
                                        drawCircle(
                                            color = surfaceLowest,
                                            radius = 2.6.dp.toPx(),
                                            center = Offset(x, y)
                                        )
                                    } else {
                                        drawCircle(
                                            color = semantic.expense,
                                            radius = 3.4.dp.toPx(),
                                            center = Offset(x, y)
                                        )
                                        drawCircle(
                                            color = surfaceLowest,
                                            radius = 1.6.dp.toPx(),
                                            center = Offset(x, y)
                                        )
                                    }
                                }

                                if (showIncome) {
                                    val y = pointY(pt.income)
                                    if (isActive) {
                                        drawCircle(
                                            color = semantic.income.copy(alpha = 0.24f),
                                            radius = 9.5.dp.toPx(),
                                            center = Offset(x, y)
                                        )
                                        drawCircle(
                                            color = semantic.income,
                                            radius = 6.dp.toPx(),
                                            center = Offset(x, y)
                                        )
                                        drawCircle(
                                            color = surfaceLowest,
                                            radius = 2.8.dp.toPx(),
                                            center = Offset(x, y)
                                        )
                                    } else {
                                        drawCircle(
                                            color = semantic.income,
                                            radius = 3.6.dp.toPx(),
                                            center = Offset(x, y)
                                        )
                                        drawCircle(
                                            color = surfaceLowest,
                                            radius = 1.7.dp.toPx(),
                                            center = Offset(x, y)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (!hasAnyData) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            Text(
                                text = strings.chartNoDataHint,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 50.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                points.forEachIndexed { idx, pt ->
                    val isSelected = idx == safeIndex
                    Text(
                        text = pt.monthLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                selectedIndex = idx
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            .padding(vertical = 4.dp)
                            .testTag("trend_month_label_$idx")
                    )
                }
            }

            Text(
                text = strings.chartTapToInspectHint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SeriesTogglePill(
    label: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val bg by animateColorAsState(
        targetValue = if (selected) color.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerLow,
        label = "series_pill_bg"
    )
    val border by animateColorAsState(
        targetValue = if (selected) color.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant,
        label = "series_pill_border"
    )
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = bg,
        border = BorderStroke(1.dp, border),
        modifier = Modifier.testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (selected) color else color.copy(alpha = 0.35f))
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DashboardShimmerSkeleton(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "shimmer_sweep")
    val translateX by transition.animateFloat(
        initialValue = -350f,
        targetValue = 1100f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1150, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val highlight = MaterialTheme.colorScheme.surfaceContainerLowest
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(translateX, 0f),
        end = Offset(translateX + 380f, 220f)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("dashboard_shimmer_skeleton"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(shimmerBrush)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .width(150.dp)
                                .height(18.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(shimmerBrush)
                        )
                        Box(
                            modifier = Modifier
                                .width(95.dp)
                                .height(13.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(shimmerBrush)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .width(64.dp)
                        .height(34.dp)
                        .clip(RoundedCornerShape(50))
                        .background(shimmerBrush)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(shimmerBrush)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(shimmerBrush)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(shimmerBrush)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(shimmerBrush)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(shimmerBrush)
            )
        }
    }
}
