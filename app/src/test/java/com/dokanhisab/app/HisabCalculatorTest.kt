package com.dokanhisab.app

import com.dokanhisab.app.data.local.CategoryEntity
import com.dokanhisab.app.data.local.TransactionEntity
import com.dokanhisab.app.data.local.TransactionType
import com.dokanhisab.app.data.local.TransactionWithCategory
import com.dokanhisab.app.data.preferences.AppLanguage
import com.dokanhisab.app.domain.HisabCalculator
import com.dokanhisab.app.domain.TimePeriod
import com.dokanhisab.app.domain.TypeFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class HisabCalculatorTest {

    @Test
    fun `evaluateMathExpression supports English and Bangla arithmetic`() {
        assertEquals(400.0, HisabCalculator.evaluateMathExpression("250+150")!!, 0.001)
        assertEquals(290.0, HisabCalculator.evaluateMathExpression("120*2+50")!!, 0.001)
        assertEquals(550.0, HisabCalculator.evaluateMathExpression("৫০০+৫০")!!, 0.001)
        assertEquals(325.0, HisabCalculator.evaluateMathExpression("400-75")!!, 0.001)
        assertNull(HisabCalculator.evaluateMathExpression("100/0"))
        assertNull(HisabCalculator.evaluateMathExpression("abc"))
    }

    @Test
    fun `calculateSummary accurately separates today and all time`() {
        val now = 1_763_050_000_000L
        val tenDaysAgo = now - (10L * 86_400_000L)
        val incomeCat = CategoryEntity(1L, "Daily Sales", "দৈনিক বিক্রয়", TransactionType.INCOME, "storefront")
        val expenseCat = CategoryEntity(2L, "Stock Purchase", "মাল ক্রয়", TransactionType.EXPENSE, "shopping_bag")

        val list = listOf(
            TransactionWithCategory(
                TransactionEntity(id = 1, title = "Sale 1", amount = 1500.0, type = TransactionType.INCOME, categoryId = 1, timestamp = now),
                incomeCat
            ),
            TransactionWithCategory(
                TransactionEntity(id = 2, title = "Cost 1", amount = 400.0, type = TransactionType.EXPENSE, categoryId = 2, timestamp = now),
                expenseCat
            ),
            TransactionWithCategory(
                TransactionEntity(id = 3, title = "Old Sale", amount = 2000.0, type = TransactionType.INCOME, categoryId = 1, timestamp = tenDaysAgo),
                incomeCat
            )
        )

        val todaySummary = HisabCalculator.calculateSummary(list, TimePeriod.TODAY, now)
        assertEquals(1500.0, todaySummary.totalIncome, 0.01)
        assertEquals(400.0, todaySummary.totalExpense, 0.01)
        assertEquals(1100.0, todaySummary.netBalance, 0.01)

        val allSummary = HisabCalculator.calculateSummary(list, TimePeriod.ALL_TIME, now)
        assertEquals(3500.0, allSummary.totalIncome, 0.01)
        assertEquals(400.0, allSummary.totalExpense, 0.01)
        assertEquals(3100.0, allSummary.netBalance, 0.01)
    }

    @Test
    fun `calculateSummary computes monthly income vs expense and net profit or loss`() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 15, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val midMonth = cal.timeInMillis
        val earlyMonth = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 2, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val prevMonth = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 28, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val incomeCat = CategoryEntity(1L, "Daily Sales", "দৈনিক বিক্রয়", TransactionType.INCOME, "storefront")
        val expenseCat = CategoryEntity(2L, "Utility Bill", "বিদ্যুৎ বিল", TransactionType.EXPENSE, "bolt")

        val list = listOf(
            TransactionWithCategory(
                TransactionEntity(id = 1, title = "Grocery Sales", amount = 5000.0, type = TransactionType.INCOME, categoryId = 1, timestamp = earlyMonth),
                incomeCat
            ),
            TransactionWithCategory(
                TransactionEntity(id = 2, title = "Wholesale Stock", amount = 6500.0, type = TransactionType.EXPENSE, categoryId = 2, timestamp = midMonth),
                expenseCat
            ),
            TransactionWithCategory(
                TransactionEntity(id = 3, title = "Last Month Sale", amount = 9000.0, type = TransactionType.INCOME, categoryId = 1, timestamp = prevMonth),
                incomeCat
            )
        )

        val monthSummary = HisabCalculator.calculateSummary(list, TimePeriod.THIS_MONTH, midMonth)
        assertEquals(5000.0, monthSummary.totalIncome, 0.01)
        assertEquals(6500.0, monthSummary.totalExpense, 0.01)
        assertEquals(-1500.0, monthSummary.netBalance, 0.01)
        assertEquals(2, monthSummary.transactionCount)
    }

    @Test
    fun `buildLedgerRows filters transactions by description or category`() {
        val now = 1_763_050_000_000L
        val salesCat = CategoryEntity(1L, "Daily Sales", "দৈনিক বিক্রয়", TransactionType.INCOME, "storefront")
        val utilityCat = CategoryEntity(2L, "Utility Bill", "বিদ্যুৎ বিল", TransactionType.EXPENSE, "bolt")

        val list = listOf(
            TransactionWithCategory(
                TransactionEntity(id = 1, title = "Miniket Rice 50kg", amount = 3500.0, type = TransactionType.INCOME, categoryId = 1, timestamp = now - 1000),
                salesCat
            ),
            TransactionWithCategory(
                TransactionEntity(id = 2, title = "Shop Meter Recharge", amount = 850.0, type = TransactionType.EXPENSE, categoryId = 2, timestamp = now),
                utilityCat
            )
        )

        // Match by description
        val byDesc = HisabCalculator.buildLedgerRows(list, TimePeriod.ALL_TIME, TypeFilter.ALL, "rice", now)
        assertEquals(1, byDesc.size)
        assertEquals(1L, byDesc.first().item.transaction.id)

        // Match by English category
        val byCatEn = HisabCalculator.buildLedgerRows(list, TimePeriod.ALL_TIME, TypeFilter.ALL, "utility", now)
        assertEquals(1, byCatEn.size)
        assertEquals(2L, byCatEn.first().item.transaction.id)

        // Match by Bangla category
        val byCatBn = HisabCalculator.buildLedgerRows(list, TimePeriod.ALL_TIME, TypeFilter.ALL, "দৈনিক", now)
        assertEquals(1, byCatBn.size)
        assertEquals(1L, byCatBn.first().item.transaction.id)
    }

    @Test
    fun `buildMonthlyBreakdown computes income and expense category breakdown accurately`() {
        val nov5 = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 5, 11, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val nov12 = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 12, 16, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val oct20 = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 20, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val salesCat = CategoryEntity(1L, "Daily Sales", "দৈনিক বিক্রয়", TransactionType.INCOME, "storefront")
        val wholesaleCat = CategoryEntity(2L, "Wholesale", "পাইকারি বিক্রয়", TransactionType.INCOME, "inventory")
        val rentCat = CategoryEntity(3L, "Shop Rent", "দোকান ভাড়া", TransactionType.EXPENSE, "home_work")

        val list = listOf(
            TransactionWithCategory(
                TransactionEntity(id = 1, title = "Counter Sale", amount = 3000.0, type = TransactionType.INCOME, categoryId = 1, timestamp = nov5),
                salesCat
            ),
            TransactionWithCategory(
                TransactionEntity(id = 2, title = "Bulk Order", amount = 7000.0, type = TransactionType.INCOME, categoryId = 2, timestamp = nov12),
                wholesaleCat
            ),
            TransactionWithCategory(
                TransactionEntity(id = 3, title = "Monthly Rent", amount = 4000.0, type = TransactionType.EXPENSE, categoryId = 3, timestamp = nov12),
                rentCat
            ),
            TransactionWithCategory(
                TransactionEntity(id = 4, title = "October Sale", amount = 9999.0, type = TransactionType.INCOME, categoryId = 1, timestamp = oct20),
                salesCat
            )
        )

        val breakdown = HisabCalculator.buildMonthlyBreakdown(list, 2026, Calendar.NOVEMBER)
        assertEquals(10000.0, breakdown.summary.totalIncome, 0.01)
        assertEquals(4000.0, breakdown.summary.totalExpense, 0.01)
        assertEquals(6000.0, breakdown.summary.netBalance, 0.01)
        assertEquals(3, breakdown.chronologicalRows.size)

        assertEquals(2, breakdown.incomeCategories.size)
        assertEquals("Wholesale", breakdown.incomeCategories.first().category.nameEn)
        assertEquals(70.0f, breakdown.incomeCategories.first().percentage, 0.1f)
        assertEquals(1, breakdown.expenseCategories.size)
        assertEquals(100.0f, breakdown.expenseCategories.first().percentage, 0.1f)
    }

    @Test
    fun `formatAmount renders Bangla and English digits properly`() {
        assertEquals("৳১,২৫০", HisabCalculator.formatAmount(1250.0, "৳", AppLanguage.BANGLA, useBanglaDigits = true))
        assertEquals("৳1,250", HisabCalculator.formatAmount(1250.0, "৳", AppLanguage.ENGLISH, useBanglaDigits = false))
    }
}
