package com.dokanhisab.app.domain

import com.dokanhisab.app.data.local.CategoryEntity
import com.dokanhisab.app.data.local.TransactionType
import com.dokanhisab.app.data.local.TransactionWithCategory
import com.dokanhisab.app.data.preferences.AppLanguage
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

enum class TimePeriod {
    TODAY,
    THIS_MONTH,
    THIS_YEAR,
    ALL_TIME
}

enum class TypeFilter {
    ALL,
    INCOME,
    EXPENSE
}

data class PeriodSummary(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val transactionCount: Int = 0
) {
    val netBalance: Double
        get() = totalIncome - totalExpense
}

data class LedgerRow(
    val item: TransactionWithCategory,
    val runningBalance: Double
)

data class DailyBarPoint(
    val label: String,
    val income: Double,
    val expense: Double
)

data class MonthlyTrendPoint(
    val monthLabel: String,
    val fullMonthLabel: String,
    val year: Int,
    val monthZeroBased: Int,
    val income: Double,
    val expense: Double
) {
    val netProfit: Double
        get() = income - expense
}

data class CategoryBreakdownItem(
    val category: CategoryEntity,
    val totalAmount: Double,
    val transactionCount: Int,
    val percentage: Float
)

data class MonthlyBreakdown(
    val summary: PeriodSummary,
    val incomeCategories: List<CategoryBreakdownItem>,
    val expenseCategories: List<CategoryBreakdownItem>,
    val chronologicalRows: List<LedgerRow>
)

object HisabCalculator {

    fun calculateSummary(
        transactions: List<TransactionWithCategory>,
        period: TimePeriod,
        nowMillis: Long = System.currentTimeMillis()
    ): PeriodSummary {
        val (startMillis, endMillis) = periodBounds(period, nowMillis)
        val filtered = if (period == TimePeriod.ALL_TIME) {
            transactions
        } else {
            transactions.filter { it.transaction.timestamp in startMillis..endMillis }
        }
        var income = 0.0
        var expense = 0.0
        filtered.forEach { entry ->
            when (entry.transaction.type) {
                TransactionType.INCOME -> income += entry.transaction.amount
                TransactionType.EXPENSE -> expense += entry.transaction.amount
            }
        }
        return PeriodSummary(
            totalIncome = income,
            totalExpense = expense,
            transactionCount = filtered.size
        )
    }

    fun buildLedgerRows(
        allTransactions: List<TransactionWithCategory>,
        period: TimePeriod,
        typeFilter: TypeFilter,
        query: String,
        nowMillis: Long = System.currentTimeMillis()
    ): List<LedgerRow> {
        // Oldest first so the running balance is correct
        val chronological = allTransactions.sortedWith(
            compareBy<TransactionWithCategory> { it.transaction.timestamp }
                .thenBy { it.transaction.id }
        )
        var running = 0.0
        val withBalance = chronological.map { item ->
            if (item.transaction.type == TransactionType.INCOME) {
                running += item.transaction.amount
            } else {
                running -= item.transaction.amount
            }
            LedgerRow(item = item, runningBalance = running)
        }

        val (startMillis, endMillis) = periodBounds(period, nowMillis)
        val normalizedQuery = query.trim().lowercase(Locale.ROOT)

        return withBalance.filter { row ->
            val tx = row.item.transaction
            val cat = row.item.category
            val matchesPeriod = period == TimePeriod.ALL_TIME || tx.timestamp in startMillis..endMillis
            val matchesType = when (typeFilter) {
                TypeFilter.ALL -> true
                TypeFilter.INCOME -> tx.type == TransactionType.INCOME
                TypeFilter.EXPENSE -> tx.type == TransactionType.EXPENSE
            }
            val matchesQuery = normalizedQuery.isEmpty() ||
                tx.title.lowercase(Locale.ROOT).contains(normalizedQuery) ||
                tx.partyName.lowercase(Locale.ROOT).contains(normalizedQuery) ||
                tx.note.lowercase(Locale.ROOT).contains(normalizedQuery) ||
                cat.nameEn.lowercase(Locale.ROOT).contains(normalizedQuery) ||
                cat.nameBn.lowercase(Locale.ROOT).contains(normalizedQuery)

            matchesPeriod && matchesType && matchesQuery
        }.reversed() // Newest first for display; balances were computed oldest-first
    }

    fun buildLast7DaysBars(
        transactions: List<TransactionWithCategory>,
        language: AppLanguage,
        nowMillis: Long = System.currentTimeMillis()
    ): List<DailyBarPoint> {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -6)
        }

        val dayFormat = SimpleDateFormat("EEE", if (language == AppLanguage.BANGLA) Locale("bn", "BD") else Locale.US)
        val points = mutableListOf<DailyBarPoint>()

        repeat(7) {
            val dayStart = cal.timeInMillis
            val dayEnd = dayStart + 86_400_000L - 1L
            var inc = 0.0
            var exp = 0.0
            transactions.forEach { item ->
                val ts = item.transaction.timestamp
                if (ts in dayStart..dayEnd) {
                    if (item.transaction.type == TransactionType.INCOME) {
                        inc += item.transaction.amount
                    } else {
                        exp += item.transaction.amount
                    }
                }
            }
            points.add(
                DailyBarPoint(
                    label = dayFormat.format(Date(dayStart)),
                    income = inc,
                    expense = exp
                )
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return points
    }

    fun buildLast6MonthsTrend(
        transactions: List<TransactionWithCategory>,
        language: AppLanguage,
        useBanglaDigits: Boolean,
        nowMillis: Long = System.currentTimeMillis()
    ): List<MonthlyTrendPoint> {
        val locale = if (language == AppLanguage.BANGLA) Locale("bn", "BD") else Locale.US
        val shortMonthFormat = SimpleDateFormat("MMM", locale)
        val fullMonthFormat = SimpleDateFormat("MMM yyyy", locale)

        val monthBuckets = ArrayList<Pair<Int, Int>>(6)
        val bucketMap = HashMap<Long, DoubleArray>(8)
        val bucketDates = ArrayList<Long>(6)

        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, -5)
        }

        repeat(6) {
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH)
            val key = y.toLong() * 12L + m.toLong()
            monthBuckets.add(y to m)
            bucketDates.add(cal.timeInMillis)
            bucketMap[key] = doubleArrayOf(0.0, 0.0)
            cal.add(Calendar.MONTH, 1)
        }

        // Single pass over the transactions, reusing one Calendar
        val txCal = Calendar.getInstance()
        transactions.forEach { item ->
            txCal.timeInMillis = item.transaction.timestamp
            val key = txCal.get(Calendar.YEAR).toLong() * 12L + txCal.get(Calendar.MONTH).toLong()
            val totals = bucketMap[key] ?: return@forEach
            if (item.transaction.type == TransactionType.INCOME) {
                totals[0] += item.transaction.amount
            } else {
                totals[1] += item.transaction.amount
            }
        }

        return monthBuckets.mapIndexed { index, (year, month) ->
            val key = year.toLong() * 12L + month.toLong()
            val totals = bucketMap[key] ?: doubleArrayOf(0.0, 0.0)
            val date = Date(bucketDates[index])
            val shortLabel = shortMonthFormat.format(date)
            val rawFullLabel = fullMonthFormat.format(date)
            val fullLabel = if (language == AppLanguage.BANGLA && useBanglaDigits) {
                toBanglaDigits(rawFullLabel)
            } else {
                rawFullLabel
            }
            MonthlyTrendPoint(
                monthLabel = shortLabel,
                fullMonthLabel = fullLabel,
                year = year,
                monthZeroBased = month,
                income = totals[0],
                expense = totals[1]
            )
        }
    }

    fun formatCompactAxisAmount(
        amount: Double,
        currencySymbol: String,
        language: AppLanguage,
        useBanglaDigits: Boolean
    ): String {
        val absVal = abs(amount)
        val raw = when {
            absVal >= 1_000_000.0 -> {
                val m = absVal / 1_000_000.0
                if (abs(m % 1.0) < 0.05) "${m.toInt()}M" else String.format(Locale.US, "%.1fM", m)
            }
            absVal >= 1_000.0 -> {
                val k = absVal / 1_000.0
                if (abs(k % 1.0) < 0.05) "${k.toInt()}K" else String.format(Locale.US, "%.1fK", k)
            }
            else -> absVal.toInt().toString()
        }
        val localized = if (language == AppLanguage.BANGLA && useBanglaDigits) toBanglaDigits(raw) else raw
        val sign = if (amount < 0) "-" else ""
        return "$sign$currencySymbol$localized"
    }

    fun monthBounds(year: Int, monthZeroBased: Int): Pair<Long, Long> {
        val start = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val end = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return start.timeInMillis to end.timeInMillis
    }

    fun filterTransactionsForMonth(
        transactions: List<TransactionWithCategory>,
        year: Int,
        monthZeroBased: Int
    ): List<TransactionWithCategory> {
        val (startMillis, endMillis) = monthBounds(year, monthZeroBased)
        return transactions
            .filter { it.transaction.timestamp in startMillis..endMillis }
            .sortedWith(
                compareBy<TransactionWithCategory> { it.transaction.timestamp }
                    .thenBy { it.transaction.id }
            )
    }

    fun buildMonthlyBreakdown(
        allTransactions: List<TransactionWithCategory>,
        year: Int,
        monthZeroBased: Int
    ): MonthlyBreakdown {
        val monthItems = filterTransactionsForMonth(allTransactions, year, monthZeroBased)
        var totalIncome = 0.0
        var totalExpense = 0.0
        var running = 0.0

        val chronologicalRows = monthItems.map { item ->
            if (item.transaction.type == TransactionType.INCOME) {
                totalIncome += item.transaction.amount
                running += item.transaction.amount
            } else {
                totalExpense += item.transaction.amount
                running -= item.transaction.amount
            }
            LedgerRow(item = item, runningBalance = running)
        }

        val incomeGrouped = monthItems
            .filter { it.transaction.type == TransactionType.INCOME }
            .groupBy { it.category.id }
            .values
            .map { group ->
                val amount = group.sumOf { it.transaction.amount }
                val pct = if (totalIncome > 0.0) ((amount / totalIncome) * 100.0).toFloat() else 0f
                CategoryBreakdownItem(
                    category = group.first().category,
                    totalAmount = amount,
                    transactionCount = group.size,
                    percentage = pct
                )
            }
            .sortedByDescending { it.totalAmount }

        val expenseGrouped = monthItems
            .filter { it.transaction.type == TransactionType.EXPENSE }
            .groupBy { it.category.id }
            .values
            .map { group ->
                val amount = group.sumOf { it.transaction.amount }
                val pct = if (totalExpense > 0.0) ((amount / totalExpense) * 100.0).toFloat() else 0f
                CategoryBreakdownItem(
                    category = group.first().category,
                    totalAmount = amount,
                    transactionCount = group.size,
                    percentage = pct
                )
            }
            .sortedByDescending { it.totalAmount }

        return MonthlyBreakdown(
            summary = PeriodSummary(
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                transactionCount = monthItems.size
            ),
            incomeCategories = incomeGrouped,
            expenseCategories = expenseGrouped,
            chronologicalRows = chronologicalRows
        )
    }

    fun periodBounds(period: TimePeriod, nowMillis: Long = System.currentTimeMillis()): Pair<Long, Long> {
        if (period == TimePeriod.ALL_TIME) return 0L to Long.MAX_VALUE
        val start = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val end = Calendar.getInstance().apply { timeInMillis = nowMillis }

        when (period) {
            TimePeriod.TODAY -> {
                start.set(Calendar.HOUR_OF_DAY, 0)
                start.set(Calendar.MINUTE, 0)
                start.set(Calendar.SECOND, 0)
                start.set(Calendar.MILLISECOND, 0)

                end.set(Calendar.HOUR_OF_DAY, 23)
                end.set(Calendar.MINUTE, 59)
                end.set(Calendar.SECOND, 59)
                end.set(Calendar.MILLISECOND, 999)
            }
            TimePeriod.THIS_MONTH -> {
                start.set(Calendar.DAY_OF_MONTH, 1)
                start.set(Calendar.HOUR_OF_DAY, 0)
                start.set(Calendar.MINUTE, 0)
                start.set(Calendar.SECOND, 0)
                start.set(Calendar.MILLISECOND, 0)

                end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
                end.set(Calendar.HOUR_OF_DAY, 23)
                end.set(Calendar.MINUTE, 59)
                end.set(Calendar.SECOND, 59)
                end.set(Calendar.MILLISECOND, 999)
            }
            TimePeriod.THIS_YEAR -> {
                start.set(Calendar.DAY_OF_YEAR, 1)
                start.set(Calendar.HOUR_OF_DAY, 0)
                start.set(Calendar.MINUTE, 0)
                start.set(Calendar.SECOND, 0)
                start.set(Calendar.MILLISECOND, 0)

                end.set(Calendar.MONTH, Calendar.DECEMBER)
                end.set(Calendar.DAY_OF_MONTH, 31)
                end.set(Calendar.HOUR_OF_DAY, 23)
                end.set(Calendar.MINUTE, 59)
                end.set(Calendar.SECOND, 59)
                end.set(Calendar.MILLISECOND, 999)
            }
            TimePeriod.ALL_TIME -> Unit
        }
        return start.timeInMillis to end.timeInMillis
    }

    /**
     * Evaluates simple arithmetic expressions like "250+150", "120*3+50", or "500-75"
     * so shopkeepers never need a separate calculator app.
     * Supports both English digits and Bengali digits (০-৯).
     */
    fun evaluateMathExpression(rawInput: String): Double? {
        val normalized = normalizeDigitsToAscii(rawInput)
            .replace(",", "")
            .replace("×", "*")
            .replace("÷", "/")
            .replace(" ", "")
        if (normalized.isEmpty()) return null

        // Only allow digits, decimal point, and +, -, *, /
        if (!normalized.all { it.isDigit() || it in setOf('.', '+', '-', '*', '/') }) {
            return null
        }

        return runCatching {
            val parser = SimpleExpressionParser(normalized)
            val result = parser.parseExpression()
            if (parser.pos == normalized.length && result.isFinite() && result >= 0.0) {
                result
            } else {
                null
            }
        }.getOrNull()
    }

    fun normalizeDigitsToAscii(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '০'..'৯') {
                sb.append(('0'.code + (ch.code - '০'.code)).toChar())
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun toBanglaDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(('০'.code + (ch.code - '0'.code)).toChar())
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun formatAmount(
        amount: Double,
        currencySymbol: String,
        language: AppLanguage,
        useBanglaDigits: Boolean,
        includeSign: Boolean = false
    ): String {
        val symbols = DecimalFormatSymbols(Locale.US)
        val hasFraction = abs(amount % 1.0) > 0.001
        val formatter = DecimalFormat(if (hasFraction) "#,##0.00" else "#,##0", symbols)
        val absFormatted = formatter.format(abs(amount))
        val localizedDigits = if (language == AppLanguage.BANGLA && useBanglaDigits) {
            toBanglaDigits(absFormatted)
        } else {
            absFormatted
        }
        val sign = when {
            amount < 0 -> "-"
            includeSign && amount > 0 -> "+"
            else -> ""
        }
        return "$sign$currencySymbol$localizedDigits"
    }

    fun formatCount(
        count: Int,
        language: AppLanguage,
        useBanglaDigits: Boolean
    ): String {
        val str = count.toString()
        return if (language == AppLanguage.BANGLA && useBanglaDigits) toBanglaDigits(str) else str
    }

    fun formatDateTime(
        timestamp: Long,
        language: AppLanguage,
        useBanglaDigits: Boolean,
        pattern: String = "dd MMM, hh:mm a"
    ): String {
        val locale = if (language == AppLanguage.BANGLA) Locale("bn", "BD") else Locale.US
        val formatted = SimpleDateFormat(pattern, locale).format(Date(timestamp))
        return if (language == AppLanguage.BANGLA && useBanglaDigits) toBanglaDigits(formatted) else formatted
    }

    private class SimpleExpressionParser(private val expr: String) {
        var pos = 0

        fun parseExpression(): Double {
            var x = parseTerm()
            while (pos < expr.length) {
                when (expr[pos]) {
                    '+' -> {
                        pos++
                        x += parseTerm()
                    }
                    '-' -> {
                        pos++
                        x -= parseTerm()
                    }
                    else -> return x
                }
            }
            return x
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (pos < expr.length) {
                when (expr[pos]) {
                    '*' -> {
                        pos++
                        x *= parseFactor()
                    }
                    '/' -> {
                        pos++
                        val divisor = parseFactor()
                        if (divisor == 0.0) throw ArithmeticException("Division by zero")
                        x /= divisor
                    }
                    else -> return x
                }
            }
            return x
        }

        private fun parseFactor(): Double {
            if (pos < expr.length && expr[pos] == '+') {
                pos++
                return parseFactor()
            }
            val start = pos
            while (pos < expr.length && (expr[pos].isDigit() || expr[pos] == '.')) {
                pos++
            }
            if (start == pos) throw IllegalArgumentException("Unexpected character")
            return expr.substring(start, pos).toDouble()
        }
    }
}
