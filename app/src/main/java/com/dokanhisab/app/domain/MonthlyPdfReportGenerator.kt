package com.dokanhisab.app.domain

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.res.ResourcesCompat
import com.dokanhisab.app.R
import com.dokanhisab.app.data.local.TransactionType
import com.dokanhisab.app.data.local.TransactionWithCategory
import com.dokanhisab.app.data.preferences.AppLanguage
import com.dokanhisab.app.data.preferences.UserPreferences
import com.dokanhisab.app.localization.AppStrings
import java.io.OutputStream
import java.util.Calendar
import java.util.Locale

object MonthlyPdfReportGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_H = 36f
    private const val MARGIN_TOP = 36f
    private const val MARGIN_BOTTOM = 44f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN_H * 2f) // 523f

    private const val COLOR_PRIMARY = 0xFF0F5A46.toInt()
    private const val COLOR_PRIMARY_LIGHT = 0xFFE6F4EF.toInt()
    private const val COLOR_INCOME = 0xFF137333.toInt()
    private const val COLOR_INCOME_BG = 0xFFE8F5E9.toInt()
    private const val COLOR_EXPENSE = 0xFFC5221F.toInt()
    private const val COLOR_EXPENSE_BG = 0xFFFFEBEE.toInt()
    private const val COLOR_TEXT_DARK = 0xFF1A1F1D.toInt()
    private const val COLOR_TEXT_MUTED = 0xFF5A6561.toInt()
    private const val COLOR_BORDER = 0xFFD6DFDC.toInt()
    private const val COLOR_STRIPE = 0xFFF5F8F7.toInt()

    data class MonthlyReportData(
        val shopTitle: String,
        val monthTitle: String,
        val generatedAt: String,
        val breakdown: MonthlyBreakdown
    )

    fun prepareReportData(
        year: Int,
        monthZeroBased: Int,
        allTransactions: List<TransactionWithCategory>,
        preferences: UserPreferences,
        strings: AppStrings,
        nowMillis: Long = System.currentTimeMillis()
    ): MonthlyReportData? {
        val breakdown = HisabCalculator.buildMonthlyBreakdown(
            allTransactions = allTransactions,
            year = year,
            monthZeroBased = monthZeroBased
        )
        if (breakdown.chronologicalRows.isEmpty()) {
            return null
        }

        val monthTimestamp = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, 15)
        }.timeInMillis

        val monthTitle = HisabCalculator.formatDateTime(
            timestamp = monthTimestamp,
            language = preferences.language,
            useBanglaDigits = preferences.useBanglaDigits,
            pattern = "MMMM yyyy"
        )
        val generatedAt = HisabCalculator.formatDateTime(
            timestamp = nowMillis,
            language = preferences.language,
            useBanglaDigits = preferences.useBanglaDigits,
            pattern = "dd MMM yyyy, hh:mm a"
        )
        val shopTitle = preferences.shopName
            .ifBlank { preferences.customAppName }
            .ifBlank { strings.appName }

        return MonthlyReportData(
            shopTitle = shopTitle,
            monthTitle = monthTitle,
            generatedAt = generatedAt,
            breakdown = breakdown
        )
    }

    fun generateMonthlyReport(
        context: Context,
        year: Int,
        monthZeroBased: Int,
        allTransactions: List<TransactionWithCategory>,
        preferences: UserPreferences,
        strings: AppStrings,
        outputStream: OutputStream
    ): Boolean {
        val reportData = prepareReportData(
            year = year,
            monthZeroBased = monthZeroBased,
            allTransactions = allTransactions,
            preferences = preferences,
            strings = strings
        ) ?: return false

        val breakdown = reportData.breakdown
        val banglaFace = runCatching {
            ResourcesCompat.getFont(context, R.font.hind_siliguri)
        }.getOrNull() ?: Typeface.DEFAULT
        val englishFace = runCatching {
            ResourcesCompat.getFont(context, R.font.plus_jakarta_sans)
        }.getOrNull() ?: Typeface.DEFAULT
        val baseFace = if (preferences.language == AppLanguage.BANGLA) banglaFace else englishFace

        val pdfDocument = PdfDocument()
        val state = PdfPageState(
            pdfDocument = pdfDocument,
            shopTitle = reportData.shopTitle,
            currencySymbol = preferences.currency.symbol,
            strings = strings,
            baseFace = baseFace,
            banglaFace = banglaFace
        )

        try {
            state.startNewPage()

            // 1. Header Banner
            state.drawHeaderBanner(
                shopTitle = reportData.shopTitle,
                reportTitle = strings.monthlyPdfReportTitle,
                monthTitle = reportData.monthTitle,
                generatedAt = reportData.generatedAt
            )

            // 2. Summary Cards (Total Income | Total Expenses | Net Profit/Loss)
            state.drawSummarySection(
                summary = breakdown.summary,
                preferences = preferences,
                strings = strings
            )

            // 3. Category Breakdowns (Income Categories + Expense Categories)
            if (breakdown.incomeCategories.isNotEmpty()) {
                state.drawCategoryBreakdownSection(
                    title = "${strings.pdfIncomeBreakdown} (${breakdown.incomeCategories.size})",
                    items = breakdown.incomeCategories,
                    accentColor = COLOR_INCOME,
                    bgColor = COLOR_INCOME_BG,
                    preferences = preferences
                )
            }

            if (breakdown.expenseCategories.isNotEmpty()) {
                state.drawCategoryBreakdownSection(
                    title = "${strings.pdfExpenseBreakdown} (${breakdown.expenseCategories.size})",
                    items = breakdown.expenseCategories,
                    accentColor = COLOR_EXPENSE,
                    bgColor = COLOR_EXPENSE_BG,
                    preferences = preferences
                )
            }

            // 4. Transactions Ledger Table
            state.drawTransactionsSection(
                rows = breakdown.chronologicalRows,
                summary = breakdown.summary,
                preferences = preferences,
                strings = strings
            )

            state.finishCurrentPage()
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            return true
        } finally {
            pdfDocument.close()
        }
    }

    private class PdfPageState(
        private val pdfDocument: PdfDocument,
        private val shopTitle: String,
        private val currencySymbol: String,
        private val strings: AppStrings,
        private val baseFace: Typeface,
        private val banglaFace: Typeface
    ) {
        private var currentPage: PdfDocument.Page? = null
        private var canvas: Canvas? = null
        private var pageNumber = 0
        var y = MARGIN_TOP

        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }
        private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)

        private fun pickTypeface(text: String, bold: Boolean): Typeface {
            val hasBengali = text.any { it in '\u0980'..'\u09FF' }
            val family = if (hasBengali) banglaFace else baseFace
            return Typeface.create(family, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }

        private fun drawTextSmart(
            text: String,
            x: Float,
            baselineY: Float,
            sizeSp: Float,
            color: Int,
            bold: Boolean = false,
            align: Paint.Align = Paint.Align.LEFT,
            maxWidth: Float? = null
        ) {
            val c = canvas ?: return
            textPaint.color = color
            textPaint.textSize = sizeSp
            textPaint.textAlign = align
            textPaint.typeface = pickTypeface(text, bold)
            val rendered = if (maxWidth != null && maxWidth > 0f) {
                TextUtils.ellipsize(text, textPaint, maxWidth, TextUtils.TruncateAt.END).toString()
            } else {
                text
            }
            c.drawText(rendered, x, baselineY, textPaint)
        }

        fun startNewPage() {
            finishCurrentPage()
            pageNumber++
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            currentPage = pdfDocument.startPage(pageInfo)
            canvas = currentPage?.canvas
            y = MARGIN_TOP
        }

        fun finishCurrentPage() {
            val page = currentPage ?: return
            drawFooter()
            pdfDocument.finishPage(page)
            currentPage = null
            canvas = null
        }

        private fun ensureSpace(requiredHeight: Float, onNewPageHeader: (() -> Unit)? = null) {
            if (y + requiredHeight > PAGE_HEIGHT - MARGIN_BOTTOM) {
                startNewPage()
                onNewPageHeader?.invoke()
            }
        }

        private fun drawFooter() {
            val c = canvas ?: return
            val footerY = PAGE_HEIGHT - 20f
            strokePaint.color = COLOR_BORDER
            c.drawLine(MARGIN_H, footerY - 12f, PAGE_WIDTH - MARGIN_H, footerY - 12f, strokePaint)
            drawTextSmart(
                text = "$shopTitle • ${strings.currencyFixedValue}",
                x = MARGIN_H,
                baselineY = footerY,
                sizeSp = 8.5f,
                color = COLOR_TEXT_MUTED
            )
            drawTextSmart(
                text = "${strings.pdfPageLabel} $pageNumber",
                x = PAGE_WIDTH - MARGIN_H,
                baselineY = footerY,
                sizeSp = 8.5f,
                color = COLOR_TEXT_MUTED,
                align = Paint.Align.RIGHT
            )
        }

        fun drawHeaderBanner(
            shopTitle: String,
            reportTitle: String,
            monthTitle: String,
            generatedAt: String
        ) {
            val c = canvas ?: return
            val bannerHeight = 68f
            fillPaint.color = COLOR_PRIMARY
            val rect = RectF(MARGIN_H, y, PAGE_WIDTH - MARGIN_H, y + bannerHeight)
            c.drawRoundRect(rect, 10f, 10f, fillPaint)

            drawTextSmart(
                text = shopTitle,
                x = MARGIN_H + 16f,
                baselineY = y + 26f,
                sizeSp = 17f,
                color = Color.WHITE,
                bold = true,
                maxWidth = 310f
            )
            drawTextSmart(
                text = "$reportTitle • $monthTitle",
                x = MARGIN_H + 16f,
                baselineY = y + 48f,
                sizeSp = 10.5f,
                color = 0xFFE0F2ED.toInt(),
                bold = false,
                maxWidth = 340f
            )

            drawTextSmart(
                text = monthTitle,
                x = PAGE_WIDTH - MARGIN_H - 16f,
                baselineY = y + 26f,
                sizeSp = 11f,
                color = Color.WHITE,
                bold = true,
                align = Paint.Align.RIGHT,
                maxWidth = 150f
            )
            drawTextSmart(
                text = generatedAt,
                x = PAGE_WIDTH - MARGIN_H - 16f,
                baselineY = y + 46f,
                sizeSp = 8f,
                color = 0xFFD0EBE4.toInt(),
                align = Paint.Align.RIGHT,
                maxWidth = 150f
            )

            y += bannerHeight + 14f
        }

        fun drawSummarySection(
            summary: PeriodSummary,
            preferences: UserPreferences,
            strings: AppStrings
        ) {
            val c = canvas ?: return
            val gap = 10f
            val cardW = (CONTENT_WIDTH - gap * 2f) / 3f
            val cardH = 62f

            val incomeAmt = HisabCalculator.formatAmount(
                amount = summary.totalIncome,
                currencySymbol = currencySymbol,
                language = preferences.language,
                useBanglaDigits = preferences.useBanglaDigits
            )
            val expenseAmt = HisabCalculator.formatAmount(
                amount = summary.totalExpense,
                currencySymbol = currencySymbol,
                language = preferences.language,
                useBanglaDigits = preferences.useBanglaDigits
            )
            val netAmt = HisabCalculator.formatAmount(
                amount = summary.netBalance,
                currencySymbol = currencySymbol,
                language = preferences.language,
                useBanglaDigits = preferences.useBanglaDigits,
                includeSign = true
            )
            val isProfit = summary.netBalance >= 0.0

            drawMiniSummaryBox(
                c = c,
                x = MARGIN_H,
                top = y,
                w = cardW,
                h = cardH,
                bgColor = COLOR_INCOME_BG,
                accentColor = COLOR_INCOME,
                label = strings.totalIncome,
                value = incomeAmt,
                subLabel = "${
                    HisabCalculator.formatCount(summary.transactionCount, preferences.language, preferences.useBanglaDigits)
                } ${strings.entriesCount}"
            )
            drawMiniSummaryBox(
                c = c,
                x = MARGIN_H + cardW + gap,
                top = y,
                w = cardW,
                h = cardH,
                bgColor = COLOR_EXPENSE_BG,
                accentColor = COLOR_EXPENSE,
                label = strings.totalExpense,
                value = expenseAmt,
                subLabel = monthSummaryRatioText(summary)
            )
            drawMiniSummaryBox(
                c = c,
                x = MARGIN_H + (cardW + gap) * 2f,
                top = y,
                w = cardW,
                h = cardH,
                bgColor = if (isProfit) COLOR_PRIMARY_LIGHT else COLOR_EXPENSE_BG,
                accentColor = if (isProfit) COLOR_INCOME else COLOR_EXPENSE,
                label = if (isProfit) strings.profit else strings.loss,
                value = netAmt,
                subLabel = strings.netBalance
            )

            y += cardH + 16f
        }

        private fun monthSummaryRatioText(summary: PeriodSummary): String {
            if (summary.totalIncome <= 0.0) return "—"
            val ratio = ((summary.totalExpense / summary.totalIncome) * 100.0).toInt()
            return "$ratio% of income"
        }

        private fun drawMiniSummaryBox(
            c: Canvas,
            x: Float,
            top: Float,
            w: Float,
            h: Float,
            bgColor: Int,
            accentColor: Int,
            label: String,
            value: String,
            subLabel: String
        ) {
            fillPaint.color = bgColor
            c.drawRoundRect(RectF(x, top, x + w, top + h), 8f, 8f, fillPaint)
            strokePaint.color = accentColor
            strokePaint.alpha = 70
            c.drawRoundRect(RectF(x, top, x + w, top + h), 8f, 8f, strokePaint)
            strokePaint.alpha = 255

            drawTextSmart(
                text = label,
                x = x + 10f,
                baselineY = top + 18f,
                sizeSp = 8.5f,
                color = COLOR_TEXT_MUTED,
                bold = true,
                maxWidth = w - 20f
            )
            drawTextSmart(
                text = value,
                x = x + 10f,
                baselineY = top + 38f,
                sizeSp = 13.5f,
                color = accentColor,
                bold = true,
                maxWidth = w - 20f
            )
            drawTextSmart(
                text = subLabel,
                x = x + 10f,
                baselineY = top + 53f,
                sizeSp = 7.8f,
                color = COLOR_TEXT_MUTED,
                maxWidth = w - 20f
            )
        }

        fun drawCategoryBreakdownSection(
            title: String,
            items: List<CategoryBreakdownItem>,
            accentColor: Int,
            bgColor: Int,
            preferences: UserPreferences
        ) {
            ensureSpace(46f)
            drawTextSmart(
                text = title,
                x = MARGIN_H,
                baselineY = y + 12f,
                sizeSp = 11f,
                color = accentColor,
                bold = true,
                maxWidth = CONTENT_WIDTH
            )
            y += 18f

            items.forEachIndexed { idx, item ->
                ensureSpace(26f)
                val c = canvas ?: return@forEachIndexed
                val rowH = 22f
                if (idx % 2 == 0) {
                    fillPaint.color = bgColor
                    fillPaint.alpha = 110
                    c.drawRoundRect(
                        RectF(MARGIN_H, y, PAGE_WIDTH - MARGIN_H, y + rowH),
                        4f,
                        4f,
                        fillPaint
                    )
                    fillPaint.alpha = 255
                }

                val catName = if (preferences.language == AppLanguage.BANGLA) {
                    item.category.nameBn
                } else {
                    item.category.nameEn
                }
                val countStr = HisabCalculator.formatCount(
                    item.transactionCount,
                    preferences.language,
                    preferences.useBanglaDigits
                )
                val amtStr = HisabCalculator.formatAmount(
                    amount = item.totalAmount,
                    currencySymbol = currencySymbol,
                    language = preferences.language,
                    useBanglaDigits = preferences.useBanglaDigits
                )
                val pctStr = String.format(Locale.US, "%.0f%%", item.percentage)

                drawTextSmart(
                    text = "$catName ($countStr)",
                    x = MARGIN_H + 8f,
                    baselineY = y + 14.5f,
                    sizeSp = 9.5f,
                    color = COLOR_TEXT_DARK,
                    bold = false,
                    maxWidth = 210f
                )

                // Mini progress bar
                val barX = MARGIN_H + 225f
                val barW = 140f
                val barY = y + 8f
                fillPaint.color = COLOR_BORDER
                c.drawRoundRect(RectF(barX, barY, barX + barW, barY + 6f), 3f, 3f, fillPaint)
                fillPaint.color = accentColor
                val filledW = (barW * (item.percentage / 100f)).coerceIn(2f, barW)
                c.drawRoundRect(RectF(barX, barY, barX + filledW, barY + 6f), 3f, 3f, fillPaint)

                drawTextSmart(
                    text = pctStr,
                    x = MARGIN_H + 412f,
                    baselineY = y + 14.5f,
                    sizeSp = 9f,
                    color = COLOR_TEXT_MUTED,
                    align = Paint.Align.RIGHT,
                    maxWidth = 38f
                )
                drawTextSmart(
                    text = amtStr,
                    x = PAGE_WIDTH - MARGIN_H - 8f,
                    baselineY = y + 14.5f,
                    sizeSp = 9.5f,
                    color = accentColor,
                    bold = true,
                    align = Paint.Align.RIGHT,
                    maxWidth = 100f
                )
                y += rowH + 2f
            }

            y += 10f
        }

        fun drawTransactionsSection(
            rows: List<LedgerRow>,
            summary: PeriodSummary,
            preferences: UserPreferences,
            strings: AppStrings
        ) {
            ensureSpace(58f)
            drawTextSmart(
                text = "${strings.pdfTransactionsTitle} (${
                    HisabCalculator.formatCount(rows.size, preferences.language, preferences.useBanglaDigits)
                })",
                x = MARGIN_H,
                baselineY = y + 12f,
                sizeSp = 11.5f,
                color = COLOR_TEXT_DARK,
                bold = true,
                maxWidth = CONTENT_WIDTH
            )
            y += 18f

            drawTableHeader(strings)

            rows.forEachIndexed { idx, row ->
                val rowH = 22f
                ensureSpace(rowH + 4f) {
                    drawTableHeader(strings)
                }
                drawTransactionRow(row = row, isStriped = idx % 2 == 1, rowH = rowH, preferences = preferences)
            }

            // Totals Row
            ensureSpace(28f) {
                drawTableHeader(strings)
            }
            drawTotalsRow(summary = summary, preferences = preferences, strings = strings)
        }

        private fun drawTableHeader(strings: AppStrings) {
            val c = canvas ?: return
            val headerH = 24f
            fillPaint.color = COLOR_PRIMARY
            c.drawRect(MARGIN_H, y, PAGE_WIDTH - MARGIN_H, y + headerH, fillPaint)

            val baseline = y + 15.5f
            drawTextSmart(strings.colDate, MARGIN_H + 6f, baseline, 8.5f, Color.WHITE, bold = true, maxWidth = 74f)
            drawTextSmart(strings.colParticulars, MARGIN_H + 84f, baseline, 8.5f, Color.WHITE, bold = true, maxWidth = 156f)
            drawTextSmart(strings.colCategory, MARGIN_H + 244f, baseline, 8.5f, Color.WHITE, bold = true, maxWidth = 88f)
            drawTextSmart(strings.colIncome, MARGIN_H + 394f, baseline, 8.5f, Color.WHITE, bold = true, align = Paint.Align.RIGHT, maxWidth = 58f)
            drawTextSmart(strings.colExpense, MARGIN_H + 458f, baseline, 8.5f, Color.WHITE, bold = true, align = Paint.Align.RIGHT, maxWidth = 58f)
            drawTextSmart(strings.colBalance, PAGE_WIDTH - MARGIN_H - 6f, baseline, 8.5f, Color.WHITE, bold = true, align = Paint.Align.RIGHT, maxWidth = 58f)
            y += headerH
        }

        private fun drawTransactionRow(
            row: LedgerRow,
            isStriped: Boolean,
            rowH: Float,
            preferences: UserPreferences
        ) {
            val c = canvas ?: return
            val tx = row.item.transaction
            val cat = row.item.category
            if (isStriped) {
                fillPaint.color = COLOR_STRIPE
                c.drawRect(MARGIN_H, y, PAGE_WIDTH - MARGIN_H, y + rowH, fillPaint)
            }

            val dateStr = HisabCalculator.formatDateTime(
                timestamp = tx.timestamp,
                language = preferences.language,
                useBanglaDigits = preferences.useBanglaDigits,
                pattern = "dd MMM, yy"
            )
            val particulars = if (tx.partyName.isNotBlank()) "${tx.title} (${tx.partyName})" else tx.title
            val catName = if (preferences.language == AppLanguage.BANGLA) cat.nameBn else cat.nameEn
            val incomeStr = if (tx.type == TransactionType.INCOME) {
                HisabCalculator.formatAmount(tx.amount, currencySymbol, preferences.language, preferences.useBanglaDigits)
            } else {
                "—"
            }
            val expenseStr = if (tx.type == TransactionType.EXPENSE) {
                HisabCalculator.formatAmount(tx.amount, currencySymbol, preferences.language, preferences.useBanglaDigits)
            } else {
                "—"
            }
            val balanceStr = HisabCalculator.formatAmount(
                row.runningBalance,
                currencySymbol,
                preferences.language,
                preferences.useBanglaDigits
            )

            val baseline = y + 14.5f
            drawTextSmart(dateStr, MARGIN_H + 6f, baseline, 8f, COLOR_TEXT_MUTED, maxWidth = 74f)
            drawTextSmart(particulars, MARGIN_H + 84f, baseline, 8.5f, COLOR_TEXT_DARK, maxWidth = 156f)
            drawTextSmart(catName, MARGIN_H + 244f, baseline, 8f, COLOR_TEXT_MUTED, maxWidth = 88f)
            drawTextSmart(
                incomeStr,
                MARGIN_H + 394f,
                baseline,
                8.5f,
                if (tx.type == TransactionType.INCOME) COLOR_INCOME else COLOR_TEXT_MUTED,
                bold = tx.type == TransactionType.INCOME,
                align = Paint.Align.RIGHT,
                maxWidth = 58f
            )
            drawTextSmart(
                expenseStr,
                MARGIN_H + 458f,
                baseline,
                8.5f,
                if (tx.type == TransactionType.EXPENSE) COLOR_EXPENSE else COLOR_TEXT_MUTED,
                bold = tx.type == TransactionType.EXPENSE,
                align = Paint.Align.RIGHT,
                maxWidth = 58f
            )
            drawTextSmart(
                balanceStr,
                PAGE_WIDTH - MARGIN_H - 6f,
                baseline,
                8.5f,
                if (row.runningBalance >= 0) COLOR_TEXT_DARK else COLOR_EXPENSE,
                bold = true,
                align = Paint.Align.RIGHT,
                maxWidth = 58f
            )

            strokePaint.color = COLOR_BORDER
            c.drawLine(MARGIN_H, y + rowH, PAGE_WIDTH - MARGIN_H, y + rowH, strokePaint)
            y += rowH
        }

        private fun drawTotalsRow(
            summary: PeriodSummary,
            preferences: UserPreferences,
            strings: AppStrings
        ) {
            val c = canvas ?: return
            val rowH = 26f
            fillPaint.color = COLOR_PRIMARY_LIGHT
            c.drawRect(MARGIN_H, y, PAGE_WIDTH - MARGIN_H, y + rowH, fillPaint)
            strokePaint.color = COLOR_PRIMARY
            c.drawLine(MARGIN_H, y, PAGE_WIDTH - MARGIN_H, y, strokePaint)

            val incomeStr = HisabCalculator.formatAmount(
                summary.totalIncome,
                currencySymbol,
                preferences.language,
                preferences.useBanglaDigits
            )
            val expenseStr = HisabCalculator.formatAmount(
                summary.totalExpense,
                currencySymbol,
                preferences.language,
                preferences.useBanglaDigits
            )
            val netStr = HisabCalculator.formatAmount(
                summary.netBalance,
                currencySymbol,
                preferences.language,
                preferences.useBanglaDigits,
                includeSign = true
            )

            val baseline = y + 16.5f
            drawTextSmart(
                text = strings.totalRowLabel,
                x = MARGIN_H + 6f,
                baselineY = baseline,
                sizeSp = 9.5f,
                color = COLOR_TEXT_DARK,
                bold = true,
                maxWidth = 200f
            )
            drawTextSmart(
                text = incomeStr,
                x = MARGIN_H + 394f,
                baselineY = baseline,
                sizeSp = 9f,
                color = COLOR_INCOME,
                bold = true,
                align = Paint.Align.RIGHT,
                maxWidth = 58f
            )
            drawTextSmart(
                text = expenseStr,
                x = MARGIN_H + 458f,
                baselineY = baseline,
                sizeSp = 9f,
                color = COLOR_EXPENSE,
                bold = true,
                align = Paint.Align.RIGHT,
                maxWidth = 58f
            )
            drawTextSmart(
                text = netStr,
                x = PAGE_WIDTH - MARGIN_H - 6f,
                baselineY = baseline,
                sizeSp = 9.5f,
                color = if (summary.netBalance >= 0) COLOR_INCOME else COLOR_EXPENSE,
                bold = true,
                align = Paint.Align.RIGHT,
                maxWidth = 58f
            )
            y += rowH + 8f
        }
    }
}
