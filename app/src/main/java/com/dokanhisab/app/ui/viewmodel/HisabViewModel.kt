package com.dokanhisab.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dokanhisab.app.data.local.CategoryEntity
import com.dokanhisab.app.data.local.PaymentMethod
import com.dokanhisab.app.data.local.TransactionEntity
import com.dokanhisab.app.data.local.TransactionType
import com.dokanhisab.app.data.local.TransactionWithCategory
import com.dokanhisab.app.data.preferences.AiProvider
import com.dokanhisab.app.data.preferences.AppLanguage
import com.dokanhisab.app.data.preferences.AppPalette
import com.dokanhisab.app.data.preferences.ThemeMode
import com.dokanhisab.app.data.preferences.UserPreferences
import com.dokanhisab.app.data.preferences.UserPreferencesRepository
import com.dokanhisab.app.data.repository.HisabRepository
import com.dokanhisab.app.domain.DailyBarPoint
import com.dokanhisab.app.domain.HisabCalculator
import com.dokanhisab.app.domain.LedgerRow
import com.dokanhisab.app.domain.MonthlyTrendPoint
import com.dokanhisab.app.domain.PeriodSummary
import com.dokanhisab.app.domain.TimePeriod
import com.dokanhisab.app.domain.TypeFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ExportFormat(val extension: String, val mimeType: String) {
    CSV("csv", "text/csv"),
    EXCEL_XML("xls", "application/vnd.ms-excel"),
    PDF("pdf", "application/pdf")
}

sealed interface HisabUiState {
    data object Loading : HisabUiState
    data class Ready(
        val preferences: UserPreferences,
        val categories: List<CategoryEntity>,
        val allTransactions: List<TransactionWithCategory>,
        val recentTransactions: List<TransactionWithCategory>,
        val ledgerRows: List<LedgerRow>,
        val dashboardPeriod: TimePeriod,
        val dashboardSummary: PeriodSummary,
        val cashInHandSummary: PeriodSummary,
        val dailySummary: PeriodSummary,
        val monthlySummary: PeriodSummary,
        val yearlySummary: PeriodSummary,
        val last7DaysBars: List<DailyBarPoint>,
        val last6MonthsTrend: List<MonthlyTrendPoint>,
        val ledgerPeriod: TimePeriod,
        val ledgerTypeFilter: TypeFilter,
        val searchQuery: String
    ) : HisabUiState
}

private data class FilterState(
    val dashboardPeriod: TimePeriod = TimePeriod.TODAY,
    val ledgerPeriod: TimePeriod = TimePeriod.THIS_MONTH,
    val ledgerTypeFilter: TypeFilter = TypeFilter.ALL,
    val searchQuery: String = ""
)

class HisabViewModel(
    private val repository: HisabRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val filterState = MutableStateFlow(FilterState())
    private var lastDeletedTransaction: TransactionEntity? = null

    init {
        viewModelScope.launch {
            repository.ensureDefaultCategoriesSeeded()
        }
    }

    val uiState: StateFlow<HisabUiState> = combine(
        repository.allTransactions,
        repository.allCategories,
        preferencesRepository.userPreferences,
        filterState
    ) { transactions, categories, prefs, filters ->
        val now = System.currentTimeMillis()
        val daily = HisabCalculator.calculateSummary(transactions, TimePeriod.TODAY, now)
        val monthly = HisabCalculator.calculateSummary(transactions, TimePeriod.THIS_MONTH, now)
        val yearly = HisabCalculator.calculateSummary(transactions, TimePeriod.THIS_YEAR, now)
        val allTime = HisabCalculator.calculateSummary(transactions, TimePeriod.ALL_TIME, now)
        val activeDashSummary = when (filters.dashboardPeriod) {
            TimePeriod.TODAY -> daily
            TimePeriod.THIS_MONTH -> monthly
            TimePeriod.THIS_YEAR -> yearly
            TimePeriod.ALL_TIME -> allTime
        }
        val ledgerRows = HisabCalculator.buildLedgerRows(
            allTransactions = transactions,
            period = filters.ledgerPeriod,
            typeFilter = filters.ledgerTypeFilter,
            query = filters.searchQuery,
            nowMillis = now
        )
        val bars = HisabCalculator.buildLast7DaysBars(
            transactions = transactions,
            language = prefs.language,
            nowMillis = now
        )
        val sixMonthsTrend = HisabCalculator.buildLast6MonthsTrend(
            transactions = transactions,
            language = prefs.language,
            useBanglaDigits = prefs.useBanglaDigits,
            nowMillis = now
        )

        HisabUiState.Ready(
            preferences = prefs,
            categories = categories,
            allTransactions = transactions,
            recentTransactions = transactions.take(8),
            ledgerRows = ledgerRows,
            dashboardPeriod = filters.dashboardPeriod,
            dashboardSummary = activeDashSummary,
            cashInHandSummary = allTime,
            dailySummary = daily,
            monthlySummary = monthly,
            yearlySummary = yearly,
            last7DaysBars = bars,
            last6MonthsTrend = sixMonthsTrend,
            ledgerPeriod = filters.ledgerPeriod,
            ledgerTypeFilter = filters.ledgerTypeFilter,
            searchQuery = filters.searchQuery
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = HisabUiState.Loading
    )

    fun setDashboardPeriod(period: TimePeriod) {
        filterState.value = filterState.value.copy(dashboardPeriod = period)
    }

    fun setLedgerPeriod(period: TimePeriod) {
        filterState.value = filterState.value.copy(ledgerPeriod = period)
    }

    fun setLedgerTypeFilter(typeFilter: TypeFilter) {
        filterState.value = filterState.value.copy(ledgerTypeFilter = typeFilter)
    }

    fun setSearchQuery(query: String) {
        filterState.value = filterState.value.copy(searchQuery = query)
    }

    fun saveTransaction(
        editingTransaction: TransactionEntity?,
        title: String,
        amount: Double,
        type: TransactionType,
        categoryId: Long,
        paymentMethod: PaymentMethod,
        partyName: String,
        note: String,
        timestamp: Long
    ) {
        viewModelScope.launch {
            if (editingTransaction == null) {
                repository.addTransaction(
                    TransactionEntity(
                        title = title.trim(),
                        amount = amount,
                        type = type,
                        categoryId = categoryId,
                        paymentMethod = paymentMethod,
                        partyName = partyName.trim(),
                        note = note.trim(),
                        timestamp = timestamp,
                        createdAt = System.currentTimeMillis()
                    )
                )
            } else {
                repository.updateTransaction(
                    editingTransaction.copy(
                        title = title.trim(),
                        amount = amount,
                        type = type,
                        categoryId = categoryId,
                        paymentMethod = paymentMethod,
                        partyName = partyName.trim(),
                        note = note.trim(),
                        timestamp = timestamp
                    )
                )
            }
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        lastDeletedTransaction = transaction
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun undoLastDelete() {
        val toRestore = lastDeletedTransaction ?: return
        lastDeletedTransaction = null
        viewModelScope.launch {
            repository.addTransaction(toRestore.copy(id = 0L))
        }
    }

    fun clearAllTransactions() {
        viewModelScope.launch {
            repository.clearAllTransactions()
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { preferencesRepository.setLanguage(language) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun setPalette(palette: AppPalette) {
        viewModelScope.launch { preferencesRepository.setPalette(palette) }
    }

    fun setCustomAppName(appName: String) {
        viewModelScope.launch { preferencesRepository.setCustomAppName(appName) }
    }

    fun setShopName(name: String) {
        viewModelScope.launch { preferencesRepository.setShopName(name) }
    }

    fun setUseBanglaDigits(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setUseBanglaDigits(enabled) }
    }

    fun setBannerImagePath(path: String) {
        viewModelScope.launch { preferencesRepository.setBannerImagePath(path) }
    }

    fun setProfileImagePath(path: String) {
        viewModelScope.launch { preferencesRepository.setProfileImagePath(path) }
    }

    fun setSaveDirectory(uri: String, label: String) {
        viewModelScope.launch { preferencesRepository.setSaveDirectory(uri, label) }
    }

    fun clearSaveDirectory() {
        viewModelScope.launch { preferencesRepository.clearSaveDirectory() }
    }

    fun setAiProvider(provider: AiProvider) {
        viewModelScope.launch { preferencesRepository.setAiProvider(provider) }
    }

    fun setGeminiApiKey(apiKey: String) {
        viewModelScope.launch { preferencesRepository.setGeminiApiKey(apiKey) }
    }

    fun setOpenRouterApiKey(apiKey: String) {
        viewModelScope.launch { preferencesRepository.setOpenRouterApiKey(apiKey) }
    }

    fun setOpenRouterModel(modelId: String) {
        viewModelScope.launch { preferencesRepository.setOpenRouterModel(modelId) }
    }

    fun setLastBackupTimestamp(timestamp: Long) {
        viewModelScope.launch { preferencesRepository.setLastBackupTimestamp(timestamp) }
    }

    fun setBiometricLockEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setBiometricLockEnabled(enabled) }
    }

    fun setLockOnBackground(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setLockOnBackground(enabled) }
    }

    fun buildExportPayload(format: ExportFormat): String? {
        val state = uiState.value as? HisabUiState.Ready ?: return null
        val rows = state.ledgerRows
        if (rows.isEmpty()) return null
        val sheetTitle = state.preferences.shopName.ifBlank { state.preferences.customAppName }
        return when (format) {
            ExportFormat.CSV -> repository.buildCsvContent(
                rows = rows.reversed(),
                language = state.preferences.language,
                currencySymbol = state.preferences.currency.symbol
            )
            ExportFormat.EXCEL_XML -> repository.buildExcelXmlContent(
                rows = rows.reversed(),
                language = state.preferences.language,
                currencySymbol = state.preferences.currency.symbol,
                shopTitle = sheetTitle
            )
            ExportFormat.PDF -> null
        }
    }

    class Factory(
        private val repository: HisabRepository,
        private val preferencesRepository: UserPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HisabViewModel(repository, preferencesRepository) as T
        }
    }
}
