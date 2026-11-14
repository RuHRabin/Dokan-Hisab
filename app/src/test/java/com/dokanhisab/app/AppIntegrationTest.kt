package com.dokanhisab.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.dokanhisab.app.data.local.DokanDatabase
import com.dokanhisab.app.data.local.PaymentMethod
import com.dokanhisab.app.data.local.TransactionEntity
import com.dokanhisab.app.data.local.TransactionType
import com.dokanhisab.app.data.preferences.AppLanguage
import com.dokanhisab.app.data.repository.HisabRepository
import com.dokanhisab.app.domain.HisabCalculator
import com.dokanhisab.app.domain.TimePeriod
import com.dokanhisab.app.domain.TypeFilter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppIntegrationTest {

    private lateinit var db: DokanDatabase
    private lateinit var repository: HisabRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, DokanDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = HisabRepository(db.transactionDao(), db.categoryDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Dokan Hisab", appName)
    }

    @Test
    fun `seed categories insert transactions and export csv and excel`() = runTest {
        repository.ensureDefaultCategoriesSeeded()
        val categories = repository.allCategories.first()
        assertTrue(categories.size >= 10)

        val incomeCategory = categories.first { it.type == TransactionType.INCOME }
        val expenseCategory = categories.first { it.type == TransactionType.EXPENSE }

        repository.addTransaction(
            TransactionEntity(
                title = "Rice 50kg",
                amount = 3200.0,
                type = TransactionType.INCOME,
                categoryId = incomeCategory.id,
                paymentMethod = PaymentMethod.CASH,
                timestamp = 1_700_000_000_000L
            )
        )
        repository.addTransaction(
            TransactionEntity(
                title = "Electricity Bill",
                amount = 700.0,
                type = TransactionType.EXPENSE,
                categoryId = expenseCategory.id,
                paymentMethod = PaymentMethod.MOBILE_BANKING,
                timestamp = 1_700_000_060_000L
            )
        )

        val allTx = repository.allTransactions.first()
        assertEquals(2, allTx.size)

        val orderedEntities = db.transactionDao().getAllTransactionsOrderedByDate().first()
        assertEquals(2, orderedEntities.size)
        assertEquals("Electricity Bill", orderedEntities.first().title)
        assertEquals(listOf(com.dokanhisab.app.data.preferences.CurrencyOption.BDT), com.dokanhisab.app.data.preferences.CurrencyOption.entries)

        val ledgerRows = HisabCalculator.buildLedgerRows(
            allTransactions = allTx,
            period = TimePeriod.ALL_TIME,
            typeFilter = TypeFilter.ALL,
            query = ""
        )
        assertEquals(2, ledgerRows.size)
        assertEquals(2500.0, ledgerRows.first().runningBalance, 0.01)
        assertEquals(3200.0, ledgerRows.last().runningBalance, 0.01)

        val csv = repository.buildCsvContent(ledgerRows.reversed(), AppLanguage.BANGLA, "৳")
        assertTrue(csv.contains("Rice 50kg"))
        assertTrue(csv.contains("3200.00"))

        val excel = repository.buildExcelXmlContent(ledgerRows.reversed(), AppLanguage.ENGLISH, "৳", "My Shop")
        assertTrue(excel.contains("<Workbook"))
        assertTrue(excel.contains("2500.00"))
    }

    @Test
    fun `chat history persists in Room and Gemini models match required ids`() = runTest {
        val chatDao = db.chatMessageDao()
        chatDao.insertMessage(
            com.dokanhisab.app.data.local.ChatMessageEntity(
                role = "user",
                text = "Today's profit?",
                modelId = com.dokanhisab.app.data.preferences.GeminiChatModel.HYDROGEN.modelId,
                timestamp = 1_000L
            )
        )
        chatDao.insertMessage(
            com.dokanhisab.app.data.local.ChatMessageEntity(
                role = "model",
                text = "Net profit is ৳2,500.",
                modelId = com.dokanhisab.app.data.preferences.GeminiChatModel.CARBON.modelId,
                timestamp = 2_000L
            )
        )

        val history = chatDao.getAllMessages().first()
        assertEquals(2, history.size)
        assertEquals("user", history[0].role)
        assertEquals("model", history[1].role)

        assertEquals("gemini-4-carbon", com.dokanhisab.app.data.preferences.GeminiChatModel.CARBON.modelId)
        assertEquals("gemini-4-argon", com.dokanhisab.app.data.preferences.GeminiChatModel.ARGON.modelId)
        assertEquals("gemini-4-hydrogen", com.dokanhisab.app.data.preferences.GeminiChatModel.HYDROGEN.modelId)
        assertEquals("gemini-3.1-flash-lite", com.dokanhisab.app.data.preferences.GeminiChatModel.FLASH_LITE.modelId)

        val sysPrompt = com.dokanhisab.app.data.remote.GeminiApiClient().buildSystemInstruction(
            role = com.dokanhisab.app.data.preferences.ChatRole.SHOP_ACCOUNTANT,
            customInstruction = "",
            shopName = "Rahman Store",
            language = AppLanguage.ENGLISH,
            ledgerSummaryContext = "Today Income: ৳3,200"
        )
        assertTrue(sysPrompt.contains("Rahman Store"))
        assertTrue(sysPrompt.contains("Today Income: ৳3,200"))
    }

    @Test
    fun `monthly pdf report generator prepares income and expense breakdown and skips empty month`() = runTest {
        repository.ensureDefaultCategoriesSeeded()
        val categories = repository.allCategories.first()
        val incomeCategory = categories.first { it.type == TransactionType.INCOME }
        val expenseCategory = categories.first { it.type == TransactionType.EXPENSE }

        val cal = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.NOVEMBER, 10, 14, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        repository.addTransaction(
            TransactionEntity(
                title = "Sugar 20kg",
                amount = 2600.0,
                type = TransactionType.INCOME,
                categoryId = incomeCategory.id,
                timestamp = cal.timeInMillis
            )
        )
        repository.addTransaction(
            TransactionEntity(
                title = "Shop Electricity",
                amount = 650.0,
                type = TransactionType.EXPENSE,
                categoryId = expenseCategory.id,
                timestamp = cal.timeInMillis + 60_000L
            )
        )

        val allTx = repository.allTransactions.first()
        val reportData = com.dokanhisab.app.domain.MonthlyPdfReportGenerator.prepareReportData(
            year = 2026,
            monthZeroBased = java.util.Calendar.NOVEMBER,
            allTransactions = allTx,
            preferences = com.dokanhisab.app.data.preferences.UserPreferences(
                language = AppLanguage.ENGLISH,
                shopName = "Rahman Store",
                useBanglaDigits = false
            ),
            strings = com.dokanhisab.app.localization.LocalizationProvider.EnglishStrings
        )
        assertTrue(reportData != null)
        assertEquals("Rahman Store", reportData!!.shopTitle)
        assertTrue(reportData.monthTitle.contains("2026"))
        assertEquals(2600.0, reportData.breakdown.summary.totalIncome, 0.01)
        assertEquals(650.0, reportData.breakdown.summary.totalExpense, 0.01)
        assertEquals(1950.0, reportData.breakdown.summary.netBalance, 0.01)
        assertEquals(1, reportData.breakdown.incomeCategories.size)
        assertEquals(1, reportData.breakdown.expenseCategories.size)
        assertEquals(2, reportData.breakdown.chronologicalRows.size)

        // Empty month returns false immediately
        val emptyOut = java.io.ByteArrayOutputStream()
        val emptyOk = com.dokanhisab.app.domain.MonthlyPdfReportGenerator.generateMonthlyReport(
            context = ApplicationProvider.getApplicationContext(),
            year = 2025,
            monthZeroBased = java.util.Calendar.JANUARY,
            allTransactions = allTx,
            preferences = com.dokanhisab.app.data.preferences.UserPreferences(),
            strings = com.dokanhisab.app.localization.LocalizationProvider.EnglishStrings,
            outputStream = emptyOut
        )
        assertTrue(!emptyOk)
    }

    @Test
    fun `sqlite database backup and restore preserves transactions and rejects invalid files`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fileDb = Room.databaseBuilder(context, DokanDatabase::class.java, "backup_test_dokan.db")
            .allowMainThreadQueries()
            .build()
        try {
            val fileRepo = HisabRepository(fileDb.transactionDao(), fileDb.categoryDao())
            fileRepo.ensureDefaultCategoriesSeeded()
            val cats = fileRepo.allCategories.first()
            val incCat = cats.first { it.type == TransactionType.INCOME }
            val expCat = cats.first { it.type == TransactionType.EXPENSE }

            fileRepo.addTransaction(
                TransactionEntity(
                    title = "Backup Test Sale",
                    amount = 4500.0,
                    type = TransactionType.INCOME,
                    categoryId = incCat.id,
                    timestamp = 1_763_000_000_000L
                )
            )
            fileRepo.addTransaction(
                TransactionEntity(
                    title = "Backup Test Expense",
                    amount = 1200.0,
                    type = TransactionType.EXPENSE,
                    categoryId = expCat.id,
                    timestamp = 1_763_000_060_000L
                )
            )

            val backupFile = java.io.File(context.cacheDir, "dokan_backup_test.db")
            val backedUp = com.dokanhisab.app.data.local.DatabaseBackupManager.exportDatabaseSnapshotToFile(
                context = context,
                database = fileDb,
                destinationFile = backupFile
            )
            assertTrue(backedUp)
            assertTrue(backupFile.exists() && backupFile.length() > 0L)

            // Wipe live transactions and verify restore brings them back
            fileRepo.clearAllTransactions()
            assertEquals(0, fileRepo.allTransactions.first().size)

            val result = com.dokanhisab.app.data.local.DatabaseBackupManager.restoreDatabaseFromFile(
                database = fileDb,
                backupFile = backupFile
            )
            assertTrue(result is com.dokanhisab.app.data.local.RestoreResult.Success)
            val restoredSuccess = result as com.dokanhisab.app.data.local.RestoreResult.Success
            assertEquals(2, restoredSuccess.restoredTransactions)

            val restoredTx = fileRepo.allTransactions.first()
            assertEquals(2, restoredTx.size)
            assertTrue(restoredTx.any { it.transaction.title == "Backup Test Sale" && it.transaction.amount == 4500.0 })

            // Invalid file should cleanly fail validation
            val badFile = java.io.File(context.cacheDir, "not_a_db.db").apply {
                writeText("corrupted text content")
            }
            val badResult = com.dokanhisab.app.data.local.DatabaseBackupManager.restoreDatabaseFromFile(
                database = fileDb,
                backupFile = badFile
            )
            assertTrue(badResult is com.dokanhisab.app.data.local.RestoreResult.Error)
            backupFile.delete()
            badFile.delete()
        } finally {
            fileDb.close()
            context.deleteDatabase("backup_test_dokan.db")
        }
    }

    @Test
    fun `openrouter client builds openai compatible payload and extracts sse chunks`() {
        val client = com.dokanhisab.app.data.remote.OpenRouterApiClient()
        val history = listOf(
            com.dokanhisab.app.data.local.ChatMessageEntity(
                role = "user",
                text = "What is today's total sales?",
                modelId = "openai/gpt-4o-mini",
                timestamp = 100L
            ),
            com.dokanhisab.app.data.local.ChatMessageEntity(
                role = "model",
                text = "Today's total sales is ৳3,200.",
                modelId = "openai/gpt-4o-mini",
                timestamp = 200L
            )
        )

        val payload = client.buildChatPayload(
            modelId = "openai/gpt-4o-mini",
            systemInstruction = "Act as a Bangladeshi shop accountant.",
            history = history,
            userMessage = "And my net profit?"
        )

        assertEquals("openai/gpt-4o-mini", payload.getString("model"))
        assertTrue(payload.getBoolean("stream"))
        val messages = payload.getJSONArray("messages")
        assertEquals(4, messages.length())
        assertEquals("system", messages.getJSONObject(0).getString("role"))
        assertEquals("user", messages.getJSONObject(1).getString("role"))
        assertEquals("assistant", messages.getJSONObject(2).getString("role"))
        assertEquals("user", messages.getJSONObject(3).getString("role"))
        assertEquals("And my net profit?", messages.getJSONObject(3).getString("content"))

        // Verify SSE delta extraction ignores JSON null content
        val chunkWithText = """{"choices":[{"delta":{"content":"Net profit is ৳2,500"}}]}"""
        val chunkWithNull = """{"choices":[{"delta":{"role":"assistant","content":null}}]}"""
        assertEquals("Net profit is ৳2,500", client.extractChunkText(chunkWithText))
        assertEquals("", client.extractChunkText(chunkWithNull))

        // Verify error parsing and default UserPreferences
        val errMsg = client.parseApiError(402, """{"error":{"message":"Insufficient credits"}}""")
        assertEquals("Insufficient credits", errMsg)

        val defaultPrefs = com.dokanhisab.app.data.preferences.UserPreferences()
        assertEquals(com.dokanhisab.app.data.preferences.AiProvider.GEMINI, defaultPrefs.aiProvider)
        assertEquals(
            com.dokanhisab.app.data.preferences.OpenRouterPresetModel.GEMINI_2_5_FLASH.modelId,
            defaultPrefs.selectedOpenRouterModel
        )

        // Verify distinct semantic hero colors across palettes
        val emeraldLight = com.dokanhisab.app.ui.theme.resolveSemanticColors(
            com.dokanhisab.app.data.preferences.AppPalette.EMERALD,
            darkTheme = false
        )
        val royalLight = com.dokanhisab.app.ui.theme.resolveSemanticColors(
            com.dokanhisab.app.data.preferences.AppPalette.ROYAL_BLUE,
            darkTheme = false
        )
        assertTrue(emeraldLight.heroGradientStart != royalLight.heroGradientStart)
        assertTrue(emeraldLight.navIndicator != royalLight.navIndicator)
    }

    @Test
    fun `biometric helper resolves api-safe authenticators and default lock preferences`() {
        val api30Auth = com.dokanhisab.app.security.BiometricAuthHelper.resolveAllowedAuthenticators(android.os.Build.VERSION_CODES.R)
        val api29Auth = com.dokanhisab.app.security.BiometricAuthHelper.resolveAllowedAuthenticators(android.os.Build.VERSION_CODES.Q)

        assertEquals(
            androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK or
                androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL,
            api30Auth
        )
        assertEquals(
            androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK or
                androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL,
            api29Auth
        )

        val enrollIntentApi30 = com.dokanhisab.app.security.BiometricAuthHelper.buildEnrollIntent(android.os.Build.VERSION_CODES.R)
        assertEquals(android.provider.Settings.ACTION_BIOMETRIC_ENROLL, enrollIntentApi30.action)
        val enrollIntentApi28 = com.dokanhisab.app.security.BiometricAuthHelper.buildEnrollIntent(android.os.Build.VERSION_CODES.P)
        assertEquals(android.provider.Settings.ACTION_SECURITY_SETTINGS, enrollIntentApi28.action)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val cap = com.dokanhisab.app.security.BiometricAuthHelper.checkCapability(context)
        assertTrue(cap in com.dokanhisab.app.security.BiometricCapability.entries)

        val defaultPrefs = com.dokanhisab.app.data.preferences.UserPreferences()
        assertEquals(false, defaultPrefs.biometricLockEnabled)
        assertEquals(true, defaultPrefs.lockOnBackground)
    }

    @Test
    fun `six month trend aggregates room transactions accurately and computes growth`() = runTest {
        repository.ensureDefaultCategoriesSeeded()
        val categories = repository.allCategories.first()
        val incomeCat = categories.first { it.type == TransactionType.INCOME }
        val expenseCat = categories.first { it.type == TransactionType.EXPENSE }

        // Reference date: Nov 15, 2026
        fun tsForMonthOffset(monthsAgo: Int): Long {
            return java.util.Calendar.getInstance().apply {
                set(2026, java.util.Calendar.NOVEMBER, 15, 12, 0, 0)
                set(java.util.Calendar.MILLISECOND, 0)
                add(java.util.Calendar.MONTH, -monthsAgo)
            }.timeInMillis
        }

        val now = tsForMonthOffset(0)

        // Old transaction outside 6-month window (7 months ago -> ignored)
        repository.addTransaction(
            TransactionEntity(
                title = "Old Sale",
                amount = 9999.0,
                type = TransactionType.INCOME,
                categoryId = incomeCat.id,
                timestamp = tsForMonthOffset(7)
            )
        )
        // Previous month (Oct 2026): Income 4,000, Expense 1,500
        repository.addTransaction(
            TransactionEntity(
                title = "October Wholesale",
                amount = 4000.0,
                type = TransactionType.INCOME,
                categoryId = incomeCat.id,
                timestamp = tsForMonthOffset(1)
            )
        )
        repository.addTransaction(
            TransactionEntity(
                title = "October Rent",
                amount = 1500.0,
                type = TransactionType.EXPENSE,
                categoryId = expenseCat.id,
                timestamp = tsForMonthOffset(1) + 1000L
            )
        )
        // Current month (Nov 2026): Income 5,000 (+25% MoM), Expense 2,000
        repository.addTransaction(
            TransactionEntity(
                title = "November Sales",
                amount = 5000.0,
                type = TransactionType.INCOME,
                categoryId = incomeCat.id,
                timestamp = now
            )
        )
        repository.addTransaction(
            TransactionEntity(
                title = "November Stock",
                amount = 2000.0,
                type = TransactionType.EXPENSE,
                categoryId = expenseCat.id,
                timestamp = now - 1000L
            )
        )

        val allTx = repository.allTransactions.first()
        val points = HisabCalculator.buildLast6MonthsTrend(
            transactions = allTx,
            language = AppLanguage.ENGLISH,
            useBanglaDigits = false,
            nowMillis = now
        )

        assertEquals(6, points.size)
        // Old transaction 7 months ago must not be counted
        assertEquals(0.0, points[0].income, 0.01)
        assertEquals(4000.0, points[4].income, 0.01)
        assertEquals(1500.0, points[4].expense, 0.01)
        assertEquals(2500.0, points[4].netProfit, 0.01)
        assertEquals(5000.0, points[5].income, 0.01)
        assertEquals(2000.0, points[5].expense, 0.01)
        assertEquals(3000.0, points[5].netProfit, 0.01)

        val growth = com.dokanhisab.app.ui.components.computeTrendGrowthSummary(points)
        assertEquals(9000.0, growth.totalSixMonthIncome, 0.01)
        assertEquals(3500.0, growth.totalSixMonthExpense, 0.01)
        assertEquals(5500.0, growth.totalSixMonthNet, 0.01)
        assertEquals(25, growth.monthOverMonthIncomeChangePercent)

        assertEquals(
            "৳5K",
            HisabCalculator.formatCompactAxisAmount(5000.0, "৳", AppLanguage.ENGLISH, false)
        )
        assertEquals(
            "৳১.৫M",
            HisabCalculator.formatCompactAxisAmount(1_500_000.0, "৳", AppLanguage.BANGLA, true)
        )
    }

    @Test
    fun `auto categorizer matches english bangla banglish keywords and past history`() = runTest {
        repository.ensureDefaultCategoriesSeeded()
        val categories = repository.allCategories.first()

        // English & Banglish expense keywords
        val elecMatch = com.dokanhisab.app.domain.TransactionAutoCategorizer.suggestCategory(
            description = "DESCO Electricity Bill",
            type = TransactionType.EXPENSE,
            availableCategories = categories
        )
        assertEquals("bolt", elecMatch?.category?.iconKey)
        assertEquals(false, elecMatch?.fromHistory)

        val rentMatch = com.dokanhisab.app.domain.TransactionAutoCategorizer.suggestCategory(
            description = "Monthly Shop Rent",
            type = TransactionType.EXPENSE,
            availableCategories = categories
        )
        assertEquals("home_work", rentMatch?.category?.iconKey)

        val salaryMatch = com.dokanhisab.app.domain.TransactionAutoCategorizer.suggestCategory(
            description = "Staff Salary Bonus",
            type = TransactionType.EXPENSE,
            availableCategories = categories
        )
        assertEquals("groups", salaryMatch?.category?.iconKey)

        val transportMatch = com.dokanhisab.app.domain.TransactionAutoCategorizer.suggestCategory(
            description = "Pickup Van Delivery",
            type = TransactionType.EXPENSE,
            availableCategories = categories
        )
        assertEquals("local_shipping", transportMatch?.category?.iconKey)

        val stockMatch = com.dokanhisab.app.domain.TransactionAutoCategorizer.suggestCategory(
            description = "Rice 50kg & Soyabean Oil",
            type = TransactionType.EXPENSE,
            availableCategories = categories
        )
        assertEquals("shopping_bag", stockMatch?.category?.iconKey)

        // Bangla Unicode keywords across expense and income
        val bnElecMatch = com.dokanhisab.app.domain.TransactionAutoCategorizer.suggestCategory(
            description = "নভেম্বর মাসের বিদ্যুৎ বিল",
            type = TransactionType.EXPENSE,
            availableCategories = categories
        )
        assertEquals("bolt", bnElecMatch?.category?.iconKey)

        val bnDueCollectedMatch = com.dokanhisab.app.domain.TransactionAutoCategorizer.suggestCategory(
            description = "করিম ভাই বাকি আদায়",
            type = TransactionType.INCOME,
            availableCategories = categories
        )
        assertEquals("payments", bnDueCollectedMatch?.category?.iconKey)

        val bnWholesaleMatch = com.dokanhisab.app.domain.TransactionAutoCategorizer.suggestCategory(
            description = "পাইকারি চিনি বিক্রি",
            type = TransactionType.INCOME,
            availableCategories = categories
        )
        assertEquals("inventory", bnWholesaleMatch?.category?.iconKey)

        // Past transaction history overrides static keyword defaults for recurring shop entries
        val customExpId = repository.addCustomCategory(
            nameEn = "Packaging Supplies",
            nameBn = "প্যাকেজিং সামগ্রী",
            type = TransactionType.EXPENSE,
            iconKey = "category"
        )
        repository.addTransaction(
            TransactionEntity(
                title = "Bismillah Paper & Tape",
                amount = 450.0,
                type = TransactionType.EXPENSE,
                categoryId = customExpId,
                timestamp = 1_763_000_000_000L
            )
        )
        val refreshedCategories = repository.allCategories.first()
        val history = repository.allTransactions.first()
        val recurringMatch = com.dokanhisab.app.domain.TransactionAutoCategorizer.suggestCategory(
            description = "Bismillah Paper & Tape",
            type = TransactionType.EXPENSE,
            availableCategories = refreshedCategories,
            recentTransactions = history
        )
        assertEquals(customExpId, recurringMatch?.category?.id)
        assertEquals(true, recurringMatch?.fromHistory)

        // Unknown / single-char inputs return null cleanly
        assertEquals(
            null,
            com.dokanhisab.app.domain.TransactionAutoCategorizer.suggestCategory(
                description = "a",
                type = TransactionType.EXPENSE,
                availableCategories = refreshedCategories
            )
        )
        assertEquals(
            null,
            com.dokanhisab.app.domain.TransactionAutoCategorizer.suggestCategory(
                description = "xzqwrty",
                type = TransactionType.EXPENSE,
                availableCategories = refreshedCategories
            )
        )
    }
}
