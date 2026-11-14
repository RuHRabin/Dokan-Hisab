package com.dokanhisab.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.dokanhisab.app.R
import com.dokanhisab.app.data.local.TransactionEntity
import com.dokanhisab.app.data.local.TransactionType
import com.dokanhisab.app.data.preferences.AppLanguage
import com.dokanhisab.app.domain.HisabCalculator
import com.dokanhisab.app.domain.PeriodSummary
import com.dokanhisab.app.domain.TimePeriod
import com.dokanhisab.app.localization.AppStrings
import com.dokanhisab.app.ui.components.CashBalanceHeroCard
import com.dokanhisab.app.ui.components.MonthlySummaryCard
import com.dokanhisab.app.ui.components.QuickActionPair
import com.dokanhisab.app.ui.components.SevenDayBarChartCard
import com.dokanhisab.app.ui.components.SixMonthTrendChartCard
import com.dokanhisab.app.ui.components.TransactionRowCard
import com.dokanhisab.app.ui.components.pressScale
import com.dokanhisab.app.ui.theme.LocalSemanticColors
import com.dokanhisab.app.ui.viewmodel.HisabUiState
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun DashboardScreen(
    state: HisabUiState.Ready,
    strings: AppStrings,
    onSelectPeriod: (TimePeriod) -> Unit,
    onAddTransaction: (TransactionType) -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onOpenLedger: () -> Unit,
    onOpenMonthlyPdf: () -> Unit,
    onToggleLanguage: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs = state.preferences
    val semantic = LocalSemanticColors.current
    val displayShopName = prefs.shopName.ifBlank { strings.defaultShopName }
    val displayAppName = prefs.customAppName.ifBlank { strings.appName }
    val hasProfilePhoto = prefs.profileImagePath.isNotBlank() && File(prefs.profileImagePath).exists()

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val showStickyBalanceBar by remember {
        derivedStateOf { listState.firstVisibleItemIndex >= 2 }
    }
    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex >= 4 }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "header", contentType = "header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(46.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                        ) {
                            if (hasProfilePhoto) {
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
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayShopName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = displayAppName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    val nextLang = if (prefs.language == AppLanguage.BANGLA) {
                        AppLanguage.ENGLISH
                    } else {
                        AppLanguage.BANGLA
                    }
                    Surface(
                        modifier = Modifier
                            .testTag("dashboard_language_toggle")
                            .clip(RoundedCornerShape(50))
                            .clickable { onToggleLanguage(nextLang) },
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Translate,
                                contentDescription = strings.languageSection,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (prefs.language == AppLanguage.BANGLA) "EN" else "বাংলা",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            item(key = "hero_card", contentType = "hero") {
                CashBalanceHeroCard(
                    period = state.dashboardPeriod,
                    summary = state.dashboardSummary,
                    cashInHandSummary = state.cashInHandSummary,
                    preferences = prefs,
                    strings = strings,
                    onPeriodSelected = onSelectPeriod,
                    modifier = Modifier.graphicsLayer {
                        if (listState.firstVisibleItemIndex <= 1) {
                            val offsetPx = if (listState.firstVisibleItemIndex == 1) {
                                listState.firstVisibleItemScrollOffset.toFloat()
                            } else {
                                0f
                            }
                            val progress = (offsetPx / 620f).coerceIn(0f, 1f)
                            translationY = offsetPx * 0.18f
                            val s = 1f - progress * 0.035f
                            scaleX = s
                            scaleY = s
                            alpha = 1f - progress * 0.22f
                        }
                    }
                )
            }

            item(key = "quick_actions", contentType = "actions") {
                QuickActionPair(
                    strings = strings,
                    onAddIncome = { onAddTransaction(TransactionType.INCOME) },
                    onAddExpense = { onAddTransaction(TransactionType.EXPENSE) }
                )
            }

            item(key = "period_strip", contentType = "strip") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PeriodMiniCard(
                        label = strings.periodToday,
                        summary = state.dailySummary,
                        currencySymbol = prefs.currency.symbol,
                        language = prefs.language,
                        useBanglaDigits = prefs.useBanglaDigits,
                        selected = state.dashboardPeriod == TimePeriod.TODAY,
                        onClick = { onSelectPeriod(TimePeriod.TODAY) },
                        modifier = Modifier.weight(1f)
                    )
                    PeriodMiniCard(
                        label = strings.periodMonth,
                        summary = state.monthlySummary,
                        currencySymbol = prefs.currency.symbol,
                        language = prefs.language,
                        useBanglaDigits = prefs.useBanglaDigits,
                        selected = state.dashboardPeriod == TimePeriod.THIS_MONTH,
                        onClick = { onSelectPeriod(TimePeriod.THIS_MONTH) },
                        modifier = Modifier.weight(1f)
                    )
                    PeriodMiniCard(
                        label = strings.periodYear,
                        summary = state.yearlySummary,
                        currencySymbol = prefs.currency.symbol,
                        language = prefs.language,
                        useBanglaDigits = prefs.useBanglaDigits,
                        selected = state.dashboardPeriod == TimePeriod.THIS_YEAR,
                        onClick = { onSelectPeriod(TimePeriod.THIS_YEAR) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item(key = "monthly_summary", contentType = "monthly_summary") {
                MonthlySummaryCard(
                    monthlySummary = state.monthlySummary,
                    preferences = prefs,
                    strings = strings,
                    onClick = { onSelectPeriod(TimePeriod.THIS_MONTH) },
                    onShareMonthlyPdf = onOpenMonthlyPdf
                )
            }

            item(key = "six_month_trend", contentType = "trend_chart") {
                SixMonthTrendChartCard(
                    points = state.last6MonthsTrend,
                    preferences = prefs,
                    strings = strings
                )
            }

            if (state.recentTransactions.isNotEmpty()) {
                item(key = "seven_day_chart", contentType = "bar_chart") {
                    SevenDayBarChartCard(
                        bars = state.last7DaysBars,
                        strings = strings
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.recentEntries,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(
                        onClick = onOpenLedger,
                        modifier = Modifier.testTag("view_full_ledger_button")
                    ) {
                        Text(strings.viewAllLedger)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (state.recentTransactions.isEmpty()) {
                item(key = "empty_state", contentType = "empty") {
                    EmptyLedgerCard(
                        strings = strings,
                        onAddFirst = { onAddTransaction(TransactionType.INCOME) }
                    )
                }
            } else {
                items(
                    items = state.recentTransactions,
                    key = { it.transaction.id },
                    contentType = { "tx_row" }
                ) { item ->
                    TransactionRowCard(
                        item = item,
                        preferences = prefs,
                        strings = strings,
                        onClick = { onEditTransaction(item.transaction) },
                        modifier = Modifier.animateItem()
                    )
                }
            }

            item(key = "bottom_spacer", contentType = "spacer") {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        AnimatedVisibility(
            visible = showStickyBalanceBar,
            enter = slideInVertically(tween(200)) { -it } + fadeIn(tween(180)),
            exit = slideOutVertically(tween(160)) { -it } + fadeOut(tween(140)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 760.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            val isPositive = state.dashboardSummary.netBalance >= 0
            Surface(
                onClick = {
                    scope.launch { listState.animateScrollToItem(0) }
                },
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.96f),
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, semantic.cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_sticky_balance_bar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$displayShopName • ${strings.periodLabel(state.dashboardPeriod)}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = HisabCalculator.formatAmount(
                            amount = state.dashboardSummary.netBalance,
                            currencySymbol = prefs.currency.symbol,
                            language = prefs.language,
                            useBanglaDigits = prefs.useBanglaDigits
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isPositive) semantic.income else semantic.expense
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showScrollToTop,
            enter = scaleIn(tween(180)) + fadeIn(tween(180)),
            exit = scaleOut(tween(150)) + fadeOut(tween(140)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            SmallFloatingActionButton(
                onClick = {
                    scope.launch { listState.animateScrollToItem(0) }
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                modifier = Modifier.testTag("dashboard_scroll_top_fab")
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = displayShopName
                )
            }
        }
    }
}

@Composable
private fun PeriodMiniCard(
    label: String,
    summary: PeriodSummary,
    currencySymbol: String,
    language: AppLanguage,
    useBanglaDigits: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalSemanticColors.current
    val isPositive = summary.netBalance >= 0
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier.pressScale(interactionSource, pressedScale = 0.96f),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLowest
        },
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else semantic.cardBorder
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = HisabCalculator.formatAmount(
                    amount = summary.netBalance,
                    currencySymbol = currencySymbol,
                    language = language,
                    useBanglaDigits = useBanglaDigits
                ),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (summary.netBalance == 0.0) {
                    MaterialTheme.colorScheme.onSurface
                } else if (isPositive) {
                    semantic.income
                } else {
                    semantic.expense
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EmptyLedgerCard(
    strings: AppStrings,
    onAddFirst: () -> Unit
) {
    val semantic = LocalSemanticColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        border = BorderStroke(1.dp, semantic.cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_ledger),
                contentDescription = strings.noEntriesTitle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop
            )
            Text(
                text = strings.noEntriesTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = strings.noEntriesSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onAddFirst,
                modifier = Modifier
                    .height(48.dp)
                    .testTag("empty_state_add_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.addFirstEntry)
            }
        }
    }
}
