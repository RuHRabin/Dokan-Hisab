package com.dokanhisab.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dokanhisab.app.data.local.TransactionEntity
import com.dokanhisab.app.data.local.TransactionType
import com.dokanhisab.app.data.preferences.AppLanguage
import com.dokanhisab.app.domain.HisabCalculator
import com.dokanhisab.app.domain.LedgerRow
import com.dokanhisab.app.domain.TimePeriod
import com.dokanhisab.app.domain.TypeFilter
import com.dokanhisab.app.localization.AppStrings
import com.dokanhisab.app.ui.theme.LocalSemanticColors
import com.dokanhisab.app.ui.viewmodel.HisabUiState

private val COL_DATE_W = 108.dp
private val COL_PARTICULARS_W = 156.dp
private val COL_CATEGORY_W = 118.dp
private val COL_INCOME_W = 100.dp
private val COL_EXPENSE_W = 100.dp
private val COL_BALANCE_W = 112.dp

@Composable
fun LedgerScreen(
    state: HisabUiState.Ready,
    strings: AppStrings,
    onPeriodSelected: (TimePeriod) -> Unit,
    onTypeFilterSelected: (TypeFilter) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onRowClick: (TransactionEntity) -> Unit,
    onAddClick: () -> Unit,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs = state.preferences
    val semantic = LocalSemanticColors.current
    val horizontalScroll = rememberScrollState()
    val ledgerListState = rememberLazyListState()

    val (totalFilteredIncome, totalFilteredExpense, netFiltered) = remember(state.ledgerRows) {
        var inc = 0.0
        var exp = 0.0
        state.ledgerRows.forEach { row ->
            if (row.item.transaction.type == TransactionType.INCOME) {
                inc += row.item.transaction.amount
            } else {
                exp += row.item.transaction.amount
            }
        }
        Triple(inc, exp, inc - exp)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.ledgerTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = strings.tapRowHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalIconButton(
                    onClick = onExportClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("ledger_export_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FileDownload,
                        contentDescription = strings.exportTitle
                    )
                }
                FilledTonalIconButton(
                    onClick = onAddClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("ledger_add_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = strings.quickAdd
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        text = strings.searchPlaceholder,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = strings.searchPlaceholder,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.testTag("ledger_search_clear")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = strings.clearSearchLabel
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = semantic.cardBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ledger_search_input")
            )

            if (state.searchQuery.isNotBlank()) {
                val matchCountStr = HisabCalculator.formatCount(
                    state.ledgerRows.size,
                    prefs.language,
                    prefs.useBanglaDigits
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$matchCountStr ${strings.searchResultsCount} (${strings.periodLabel(state.ledgerPeriod)})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    if (state.ledgerPeriod != TimePeriod.ALL_TIME) {
                        TextButton(
                            onClick = { onPeriodSelected(TimePeriod.ALL_TIME) },
                            modifier = Modifier.testTag("ledger_search_all_time_button")
                        ) {
                            Text(
                                text = strings.searchAllTimeLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
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
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimePeriod.entries.forEach { period ->
                FilterChip(
                    selected = state.ledgerPeriod == period,
                    onClick = { onPeriodSelected(period) },
                    label = { Text(strings.periodLabel(period)) }
                )
            }
            TypeFilter.entries.forEach { filter ->
                FilterChip(
                    selected = state.ledgerTypeFilter == filter,
                    onClick = { onTypeFilterSelected(filter) },
                    label = { Text(strings.typeFilterLabel(filter)) }
                )
            }
        }

        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 8.dp),
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, semantic.cardBorder),
            color = MaterialTheme.colorScheme.surfaceContainerLowest
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(horizontalScroll)
                        .background(semantic.ledgerHeaderBg)
                        .height(44.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TableHeaderCell(strings.colDate, COL_DATE_W, semantic.ledgerHeaderFg, TextAlign.Start)
                    VerticalDivider(color = semantic.ledgerHeaderFg.copy(alpha = 0.2f))
                    TableHeaderCell(strings.colParticulars, COL_PARTICULARS_W, semantic.ledgerHeaderFg, TextAlign.Start)
                    VerticalDivider(color = semantic.ledgerHeaderFg.copy(alpha = 0.2f))
                    TableHeaderCell(strings.colCategory, COL_CATEGORY_W, semantic.ledgerHeaderFg, TextAlign.Start)
                    VerticalDivider(color = semantic.ledgerHeaderFg.copy(alpha = 0.2f))
                    TableHeaderCell(strings.colIncome, COL_INCOME_W, semantic.ledgerHeaderFg, TextAlign.End)
                    VerticalDivider(color = semantic.ledgerHeaderFg.copy(alpha = 0.2f))
                    TableHeaderCell(strings.colExpense, COL_EXPENSE_W, semantic.ledgerHeaderFg, TextAlign.End)
                    VerticalDivider(color = semantic.ledgerHeaderFg.copy(alpha = 0.2f))
                    TableHeaderCell(strings.colBalance, COL_BALANCE_W, semantic.ledgerHeaderFg, TextAlign.End)
                }

                if (state.ledgerRows.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (state.searchQuery.isNotBlank()) {
                                    strings.noSearchMatches
                                } else {
                                    strings.noEntriesTitle
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            if (state.searchQuery.isNotBlank()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { onSearchQueryChange("") }) {
                                        Text(strings.clearSearchLabel)
                                    }
                                    if (state.ledgerPeriod != TimePeriod.ALL_TIME) {
                                        TextButton(onClick = { onPeriodSelected(TimePeriod.ALL_TIME) }) {
                                            Text(strings.searchAllTimeLabel)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = ledgerListState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        itemsIndexed(
                            items = state.ledgerRows,
                            key = { _, row -> row.item.transaction.id },
                            contentType = { _, _ -> "ledger_row" }
                        ) { index, row ->
                            LedgerTableRow(
                                row = row,
                                isStriped = index % 2 == 1,
                                horizontalScroll = horizontalScroll,
                                currencySymbol = prefs.currency.symbol,
                                language = prefs.language,
                                useBanglaDigits = prefs.useBanglaDigits,
                                onClick = { onRowClick(row.item.transaction) }
                            )
                            HorizontalDivider(
                                thickness = 0.75.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                            )
                        }
                    }
                }

                HorizontalDivider(
                    thickness = 1.5.dp,
                    color = MaterialTheme.colorScheme.outline
                )
                Row(
                    modifier = Modifier
                        .horizontalScroll(horizontalScroll)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TableBodyCell(
                        text = "${strings.totalRowLabel} (${
                            HisabCalculator.formatCount(
                                state.ledgerRows.size,
                                prefs.language,
                                prefs.useBanglaDigits
                            )
                        })",
                        width = COL_DATE_W + COL_PARTICULARS_W + COL_CATEGORY_W + 2.dp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        align = TextAlign.Start
                    )
                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    TableBodyCell(
                        text = HisabCalculator.formatAmount(
                            amount = totalFilteredIncome,
                            currencySymbol = prefs.currency.symbol,
                            language = prefs.language,
                            useBanglaDigits = prefs.useBanglaDigits
                        ),
                        width = COL_INCOME_W,
                        color = semantic.income,
                        fontWeight = FontWeight.Bold,
                        align = TextAlign.End
                    )
                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    TableBodyCell(
                        text = HisabCalculator.formatAmount(
                            amount = totalFilteredExpense,
                            currencySymbol = prefs.currency.symbol,
                            language = prefs.language,
                            useBanglaDigits = prefs.useBanglaDigits
                        ),
                        width = COL_EXPENSE_W,
                        color = semantic.expense,
                        fontWeight = FontWeight.Bold,
                        align = TextAlign.End
                    )
                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    TableBodyCell(
                        text = HisabCalculator.formatAmount(
                            amount = netFiltered,
                            currencySymbol = prefs.currency.symbol,
                            language = prefs.language,
                            useBanglaDigits = prefs.useBanglaDigits
                        ),
                        width = COL_BALANCE_W,
                        color = if (netFiltered >= 0) semantic.income else semantic.expense,
                        fontWeight = FontWeight.Bold,
                        align = TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
private fun LedgerTableRow(
    row: LedgerRow,
    isStriped: Boolean,
    horizontalScroll: androidx.compose.foundation.ScrollState,
    currencySymbol: String,
    language: AppLanguage,
    useBanglaDigits: Boolean,
    onClick: () -> Unit
) {
    val tx = row.item.transaction
    val cat = row.item.category
    val semantic = LocalSemanticColors.current
    val rowBg = if (isStriped) semantic.ledgerStripeBg else Color.Transparent
    val catName = if (language == AppLanguage.BANGLA) cat.nameBn else cat.nameEn
    val dateStr = HisabCalculator.formatDateTime(
        timestamp = tx.timestamp,
        language = language,
        useBanglaDigits = useBanglaDigits,
        pattern = "dd/MM/yy hh:mm a"
    )
    val incomeText = if (tx.type == TransactionType.INCOME) {
        HisabCalculator.formatAmount(tx.amount, currencySymbol, language, useBanglaDigits)
    } else {
        "—"
    }
    val expenseText = if (tx.type == TransactionType.EXPENSE) {
        HisabCalculator.formatAmount(tx.amount, currencySymbol, language, useBanglaDigits)
    } else {
        "—"
    }
    val balanceText = HisabCalculator.formatAmount(
        row.runningBalance,
        currencySymbol,
        language,
        useBanglaDigits
    )

    Row(
        modifier = Modifier
            .horizontalScroll(horizontalScroll)
            .background(rowBg)
            .clickable(onClick = onClick)
            .height(54.dp)
            .testTag("ledger_row_${tx.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TableBodyCell(
            text = dateStr,
            width = COL_DATE_W,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Normal,
            align = TextAlign.Start
        )
        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
        TableBodyCell(
            text = if (tx.partyName.isNotBlank()) "${tx.title} (${tx.partyName})" else tx.title,
            width = COL_PARTICULARS_W,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            align = TextAlign.Start
        )
        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
        TableBodyCell(
            text = catName,
            width = COL_CATEGORY_W,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Normal,
            align = TextAlign.Start
        )
        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
        TableBodyCell(
            text = incomeText,
            width = COL_INCOME_W,
            color = if (tx.type == TransactionType.INCOME) semantic.income else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            fontWeight = if (tx.type == TransactionType.INCOME) FontWeight.SemiBold else FontWeight.Normal,
            align = TextAlign.End
        )
        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
        TableBodyCell(
            text = expenseText,
            width = COL_EXPENSE_W,
            color = if (tx.type == TransactionType.EXPENSE) semantic.expense else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            fontWeight = if (tx.type == TransactionType.EXPENSE) FontWeight.SemiBold else FontWeight.Normal,
            align = TextAlign.End
        )
        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
        TableBodyCell(
            text = balanceText,
            width = COL_BALANCE_W,
            color = if (row.runningBalance >= 0) MaterialTheme.colorScheme.onSurface else semantic.expense,
            fontWeight = FontWeight.Bold,
            align = TextAlign.End
        )
    }
}

@Composable
private fun TableHeaderCell(
    text: String,
    width: Dp,
    color: Color,
    align: TextAlign
) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(horizontal = 10.dp),
        contentAlignment = if (align == TextAlign.End) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = align
        )
    }
}

@Composable
private fun TableBodyCell(
    text: String,
    width: Dp,
    color: Color,
    fontWeight: FontWeight,
    align: TextAlign
) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(horizontal = 10.dp),
        contentAlignment = if (align == TextAlign.End) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = fontWeight,
            color = color,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = align
        )
    }
}
