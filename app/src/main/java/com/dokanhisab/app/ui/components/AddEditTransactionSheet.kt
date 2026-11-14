package com.dokanhisab.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dokanhisab.app.data.local.CategoryEntity
import com.dokanhisab.app.data.local.PaymentMethod
import com.dokanhisab.app.data.local.TransactionEntity
import com.dokanhisab.app.data.local.TransactionType
import com.dokanhisab.app.data.local.TransactionWithCategory
import com.dokanhisab.app.data.preferences.AppLanguage
import com.dokanhisab.app.data.preferences.UserPreferences
import com.dokanhisab.app.domain.AutoCategorizationMatch
import com.dokanhisab.app.domain.HisabCalculator
import com.dokanhisab.app.domain.TransactionAutoCategorizer
import com.dokanhisab.app.localization.AppStrings
import com.dokanhisab.app.ui.theme.LocalSemanticColors
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditTransactionSheet(
    initialType: TransactionType,
    editingTransaction: TransactionEntity?,
    initialTitle: String = "",
    categories: List<CategoryEntity>,
    preferences: UserPreferences,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        amount: Double,
        type: TransactionType,
        categoryId: Long,
        paymentMethod: PaymentMethod,
        partyName: String,
        note: String,
        timestamp: Long
    ) -> Unit,
    onDelete: ((TransactionEntity) -> Unit)? = null,
    recentTransactions: List<TransactionWithCategory> = emptyList()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val semantic = LocalSemanticColors.current

    var selectedType by remember(editingTransaction, initialType) {
        mutableStateOf(editingTransaction?.type ?: initialType)
    }
    var amountInput by remember(editingTransaction) {
        mutableStateOf(
            editingTransaction?.let {
                if (it.amount % 1.0 == 0.0) it.amount.toLong().toString() else it.amount.toString()
            } ?: ""
        )
    }
    var title by remember(editingTransaction, initialTitle) {
        mutableStateOf(editingTransaction?.title ?: initialTitle)
    }
    var partyName by remember(editingTransaction) {
        mutableStateOf(editingTransaction?.partyName ?: "")
    }
    var note by remember(editingTransaction) {
        mutableStateOf(editingTransaction?.note ?: "")
    }
    var selectedMethod by remember(editingTransaction) {
        mutableStateOf(editingTransaction?.paymentMethod ?: PaymentMethod.CASH)
    }
    var selectedTimestamp by remember(editingTransaction) {
        mutableLongStateOf(editingTransaction?.timestamp ?: System.currentTimeMillis())
    }

    val filteredCategories = remember(categories, selectedType) {
        categories.filter { it.type == selectedType }
    }
    var selectedCategoryId by remember(editingTransaction, filteredCategories) {
        mutableLongStateOf(
            editingTransaction?.categoryId?.takeIf { id -> filteredCategories.any { it.id == id } }
                ?: filteredCategories.firstOrNull()?.id
                ?: 1L
        )
    }
    var userManuallyPickedCategory by remember(editingTransaction) {
        mutableStateOf(editingTransaction != null)
    }
    var autoMatchedResult by remember(editingTransaction) {
        mutableStateOf<AutoCategorizationMatch?>(null)
    }

    LaunchedEffect(selectedType, filteredCategories) {
        if (filteredCategories.none { it.id == selectedCategoryId }) {
            filteredCategories.firstOrNull()?.let { selectedCategoryId = it.id }
        }
    }

    LaunchedEffect(title, selectedType, filteredCategories, recentTransactions, userManuallyPickedCategory) {
        if (editingTransaction == null && !userManuallyPickedCategory) {
            val match = TransactionAutoCategorizer.suggestCategory(
                description = title,
                type = selectedType,
                availableCategories = filteredCategories,
                recentTransactions = recentTransactions
            )
            if (match != null) {
                selectedCategoryId = match.category.id
                autoMatchedResult = match
            } else {
                autoMatchedResult = null
            }
        }
    }

    var amountError by remember { mutableStateOf<String?>(null) }
    var titleError by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val evaluatedAmount = remember(amountInput) {
        HisabCalculator.evaluateMathExpression(amountInput)
    }
    val hasOperator = remember(amountInput) {
        amountInput.any { it in setOf('+', '-', '*', '/', '×', '÷') }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedTimestamp)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { pickedUtc ->
                            val pickedCal = Calendar.getInstance().apply { timeInMillis = pickedUtc }
                            val currentCal = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
                            currentCal.set(
                                pickedCal.get(Calendar.YEAR),
                                pickedCal.get(Calendar.MONTH),
                                pickedCal.get(Calendar.DAY_OF_MONTH)
                            )
                            selectedTimestamp = currentCal.timeInMillis
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(strings.confirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(strings.cancel)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editingTransaction != null) {
                        strings.editEntry
                    } else if (selectedType == TransactionType.INCOME) {
                        strings.addIncome
                    } else {
                        strings.addExpense
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                if (editingTransaction != null && onDelete != null) {
                    TextButton(
                        onClick = {
                            onDelete(editingTransaction)
                            onDismiss()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = semantic.expense),
                        modifier = Modifier.testTag("delete_transaction_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = strings.deleteEntry,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.deleteEntry)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TypePillButton(
                    label = strings.filterIncome,
                    selected = selectedType == TransactionType.INCOME,
                    activeColor = semantic.income,
                    activeBg = semantic.incomeContainer,
                    activeText = semantic.onIncomeContainer,
                    onClick = {
                        selectedType = TransactionType.INCOME
                        if (editingTransaction == null) {
                            userManuallyPickedCategory = false
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sheet_type_income")
                )
                TypePillButton(
                    label = strings.filterExpense,
                    selected = selectedType == TransactionType.EXPENSE,
                    activeColor = semantic.expense,
                    activeBg = semantic.expenseContainer,
                    activeText = semantic.onExpenseContainer,
                    onClick = {
                        selectedType = TransactionType.EXPENSE
                        if (editingTransaction == null) {
                            userManuallyPickedCategory = false
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sheet_type_expense")
                )
            }

            OutlinedTextField(
                value = amountInput,
                onValueChange = {
                    amountInput = it
                    amountError = null
                },
                label = { Text(strings.amountLabel) },
                placeholder = { Text(strings.amountHint) },
                prefix = {
                    Text(
                        text = "${preferences.currency.symbol} ",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Calculate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                isError = amountError != null,
                supportingText = {
                    when {
                        amountError != null -> Text(amountError!!)
                        hasOperator && evaluatedAmount != null -> {
                            val formatted = HisabCalculator.formatAmount(
                                amount = evaluatedAmount,
                                currencySymbol = preferences.currency.symbol,
                                language = preferences.language,
                                useBanglaDigits = preferences.useBanglaDigits
                            )
                            Text(
                                text = "${strings.calculatedResult} $formatted",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        else -> Text(strings.calculatorTip)
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("amount_input")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("+", "-", "*", "+50", "+100", "+500", "+1000").forEach { token ->
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                if (amountInput.isEmpty() && token.startsWith("+") && token.length > 1) {
                                    amountInput = token.drop(1)
                                } else {
                                    amountInput += token
                                }
                                amountError = null
                            },
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (token == "*") "×" else token,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    titleError = null
                },
                label = { Text(strings.titleLabel) },
                placeholder = {
                    Text(
                        if (selectedType == TransactionType.INCOME) {
                            strings.titleHintIncome
                        } else {
                            strings.titleHintExpense
                        }
                    )
                },
                isError = titleError != null,
                supportingText = titleError?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("title_input")
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.categoryLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AnimatedVisibility(
                        visible = autoMatchedResult != null && !userManuallyPickedCategory,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        val isFromHistory = autoMatchedResult?.fromHistory == true
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("auto_category_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFromHistory) Icons.Rounded.History else Icons.Rounded.Label,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = if (isFromHistory) {
                                        strings.autoCategorizedHistoryLabel
                                    } else {
                                        strings.autoCategorizedLabel
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    filteredCategories.forEach { cat ->
                        val catLabel = if (preferences.language == AppLanguage.BANGLA) cat.nameBn else cat.nameEn
                        FilterChip(
                            selected = cat.id == selectedCategoryId,
                            onClick = {
                                selectedCategoryId = cat.id
                                userManuallyPickedCategory = true
                                autoMatchedResult = null
                            },
                            label = { Text(catLabel) },
                            leadingIcon = {
                                Icon(
                                    imageVector = CategoryIconMapper.iconFor(cat.iconKey),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("category_chip_${cat.iconKey}_${cat.id}")
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = strings.paymentMethodLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentMethod.entries.forEach { method ->
                        FilterChip(
                            selected = selectedMethod == method,
                            onClick = { selectedMethod = method },
                            label = { Text(strings.paymentMethodLabel(method)) }
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small
            ) {
                Icon(
                    imageVector = Icons.Rounded.CalendarMonth,
                    contentDescription = strings.dateLabel,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${strings.dateLabel}: ${
                        HisabCalculator.formatDateTime(
                            timestamp = selectedTimestamp,
                            language = preferences.language,
                            useBanglaDigits = preferences.useBanglaDigits,
                            pattern = "dd MMM yyyy, hh:mm a"
                        )
                    }"
                )
            }

            OutlinedTextField(
                value = partyName,
                onValueChange = { partyName = it },
                label = { Text(strings.partyLabel) },
                placeholder = { Text(strings.partyHint) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(strings.noteLabel) },
                placeholder = { Text(strings.noteHint) },
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val computed = HisabCalculator.evaluateMathExpression(amountInput)
                    if (computed == null || computed <= 0.0) {
                        amountError = strings.invalidAmountError
                        return@Button
                    }
                    val trimmedTitle = title.trim()
                    if (trimmedTitle.isEmpty()) {
                        titleError = strings.emptyTitleError
                        return@Button
                    }

                    onSave(
                        trimmedTitle,
                        computed,
                        selectedType,
                        selectedCategoryId,
                        selectedMethod,
                        partyName,
                        note,
                        selectedTimestamp
                    )
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_transaction_button"),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedType == TransactionType.INCOME) semantic.income else semantic.expense,
                    contentColor = if (selectedType == TransactionType.INCOME) semantic.onIncome else semantic.onExpense
                )
            ) {
                Text(
                    text = if (editingTransaction != null) strings.updateEntry else strings.saveEntry,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TypePillButton(
    label: String,
    selected: Boolean,
    activeColor: androidx.compose.ui.graphics.Color,
    activeBg: androidx.compose.ui.graphics.Color,
    activeText: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = if (selected) activeBg else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = if (selected) BorderStroke(1.5.dp, activeColor) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) activeText else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
