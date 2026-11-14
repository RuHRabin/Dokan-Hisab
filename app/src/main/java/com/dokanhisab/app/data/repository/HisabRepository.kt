package com.dokanhisab.app.data.repository

import com.dokanhisab.app.data.local.CategoryDao
import com.dokanhisab.app.data.local.CategoryEntity
import com.dokanhisab.app.data.local.PaymentMethod
import com.dokanhisab.app.data.local.TransactionDao
import com.dokanhisab.app.data.local.TransactionEntity
import com.dokanhisab.app.data.local.TransactionType
import com.dokanhisab.app.data.local.TransactionWithCategory
import com.dokanhisab.app.data.preferences.AppLanguage
import com.dokanhisab.app.domain.LedgerRow
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HisabRepository(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao
) {
    val allTransactions: Flow<List<TransactionWithCategory>> =
        transactionDao.getAllTransactionsWithCategory()

    val allCategories: Flow<List<CategoryEntity>> =
        categoryDao.getAllCategories()

    suspend fun ensureDefaultCategoriesSeeded() {
        if (categoryDao.getCategoryCount() == 0) {
            categoryDao.insertCategories(defaultCategories())
        }
    }

    suspend fun addTransaction(transaction: TransactionEntity): Long {
        ensureDefaultCategoriesSeeded()
        return transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: Long) {
        transactionDao.deleteTransactionById(id)
    }

    suspend fun clearAllTransactions() {
        transactionDao.deleteAllTransactions()
    }

    suspend fun addCustomCategory(
        nameEn: String,
        nameBn: String,
        type: TransactionType,
        iconKey: String = "category"
    ): Long {
        return categoryDao.insertCategory(
            CategoryEntity(
                nameEn = nameEn.trim(),
                nameBn = nameBn.trim().ifEmpty { nameEn.trim() },
                type = type,
                iconKey = iconKey,
                isDefault = false
            )
        )
    }

    fun buildCsvContent(
        rows: List<LedgerRow>,
        language: AppLanguage,
        currencySymbol: String
    ): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val sb = StringBuilder()
        // UTF-8 BOM so Microsoft Excel & Google Sheets preserve Bengali script cleanly
        sb.append('\uFEFF')
        if (language == AppLanguage.BANGLA) {
            sb.appendLine("তারিখ ও সময়,বিবরণ,ক্যাটাগরি,পার্টি/ক্রেতা,মাধ্যম,জমা ($currencySymbol),খরচ ($currencySymbol),জের ($currencySymbol),নোট")
        } else {
            sb.appendLine("Date & Time,Particulars,Category,Customer/Supplier,Method,Income ($currencySymbol),Expense ($currencySymbol),Balance ($currencySymbol),Note")
        }

        rows.forEach { row ->
            val tx = row.item.transaction
            val cat = row.item.category
            val dateText = dateFormat.format(Date(tx.timestamp))
            val catName = if (language == AppLanguage.BANGLA) cat.nameBn else cat.nameEn
            val methodName = paymentMethodLabel(tx.paymentMethod, language)
            val incomeStr = if (tx.type == TransactionType.INCOME) String.format(Locale.US, "%.2f", tx.amount) else "0.00"
            val expenseStr = if (tx.type == TransactionType.EXPENSE) String.format(Locale.US, "%.2f", tx.amount) else "0.00"
            val balanceStr = String.format(Locale.US, "%.2f", row.runningBalance)

            sb.appendLine(
                listOf(
                    csvEscape(dateText),
                    csvEscape(tx.title),
                    csvEscape(catName),
                    csvEscape(tx.partyName),
                    csvEscape(methodName),
                    incomeStr,
                    expenseStr,
                    balanceStr,
                    csvEscape(tx.note)
                ).joinToString(",")
            )
        }
        return sb.toString()
    }

    fun buildExcelXmlContent(
        rows: List<LedgerRow>,
        language: AppLanguage,
        currencySymbol: String,
        shopTitle: String
    ): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val headers = if (language == AppLanguage.BANGLA) {
            listOf(
                "তারিখ ও সময়",
                "বিবরণ",
                "ক্যাটাগরি",
                "পার্টি/ক্রেতা",
                "মাধ্যম",
                "জমা ($currencySymbol)",
                "খরচ ($currencySymbol)",
                "জের ($currencySymbol)",
                "নোট"
            )
        } else {
            listOf(
                "Date & Time",
                "Particulars",
                "Category",
                "Customer/Supplier",
                "Method",
                "Income ($currencySymbol)",
                "Expense ($currencySymbol)",
                "Balance ($currencySymbol)",
                "Note"
            )
        }

        val totalIncome = rows.filter { it.item.transaction.type == TransactionType.INCOME }
            .sumOf { it.item.transaction.amount }
        val totalExpense = rows.filter { it.item.transaction.type == TransactionType.EXPENSE }
            .sumOf { it.item.transaction.amount }
        val closingBalance = totalIncome - totalExpense

        return buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("""<?mso-application progid="Excel.Sheet"?>""")
            appendLine("""<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet" xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">""")
            appendLine("""  <Styles>""")
            appendLine("""    <Style ss:ID="Header"><Font ss:Bold="1" ss:Color="#FFFFFF"/><Interior ss:Color="#0D5C46" ss:Pattern="Solid"/></Style>""")
            appendLine("""    <Style ss:ID="Total"><Font ss:Bold="1"/><Interior ss:Color="#E6F4EA" ss:Pattern="Solid"/></Style>""")
            appendLine("""  </Styles>""")
            appendLine("""  <Worksheet ss:Name="${xmlEscape(shopTitle.ifBlank { "Dokan Hisab" }.take(28))}">""")
            appendLine("""    <Table>""")
            appendLine("""      <Row>""")
            headers.forEach { h ->
                appendLine("""        <Cell ss:StyleID="Header"><Data ss:Type="String">${xmlEscape(h)}</Data></Cell>""")
            }
            appendLine("""      </Row>""")

            rows.forEach { row ->
                val tx = row.item.transaction
                val cat = row.item.category
                val dateText = dateFormat.format(Date(tx.timestamp))
                val catName = if (language == AppLanguage.BANGLA) cat.nameBn else cat.nameEn
                val methodName = paymentMethodLabel(tx.paymentMethod, language)
                val incomeVal = if (tx.type == TransactionType.INCOME) tx.amount else 0.0
                val expenseVal = if (tx.type == TransactionType.EXPENSE) tx.amount else 0.0

                appendLine("""      <Row>""")
                appendLine("""        <Cell><Data ss:Type="String">${xmlEscape(dateText)}</Data></Cell>""")
                appendLine("""        <Cell><Data ss:Type="String">${xmlEscape(tx.title)}</Data></Cell>""")
                appendLine("""        <Cell><Data ss:Type="String">${xmlEscape(catName)}</Data></Cell>""")
                appendLine("""        <Cell><Data ss:Type="String">${xmlEscape(tx.partyName)}</Data></Cell>""")
                appendLine("""        <Cell><Data ss:Type="String">${xmlEscape(methodName)}</Data></Cell>""")
                appendLine("""        <Cell><Data ss:Type="Number">${String.format(Locale.US, "%.2f", incomeVal)}</Data></Cell>""")
                appendLine("""        <Cell><Data ss:Type="Number">${String.format(Locale.US, "%.2f", expenseVal)}</Data></Cell>""")
                appendLine("""        <Cell><Data ss:Type="Number">${String.format(Locale.US, "%.2f", row.runningBalance)}</Data></Cell>""")
                appendLine("""        <Cell><Data ss:Type="String">${xmlEscape(tx.note)}</Data></Cell>""")
                appendLine("""      </Row>""")
            }

            val totalLabel = if (language == AppLanguage.BANGLA) "মোট" else "Total"
            appendLine("""      <Row>""")
            appendLine("""        <Cell ss:StyleID="Total"><Data ss:Type="String">$totalLabel</Data></Cell>""")
            appendLine("""        <Cell ss:StyleID="Total"><Data ss:Type="String"></Data></Cell>""")
            appendLine("""        <Cell ss:StyleID="Total"><Data ss:Type="String"></Data></Cell>""")
            appendLine("""        <Cell ss:StyleID="Total"><Data ss:Type="String"></Data></Cell>""")
            appendLine("""        <Cell ss:StyleID="Total"><Data ss:Type="String"></Data></Cell>""")
            appendLine("""        <Cell ss:StyleID="Total"><Data ss:Type="Number">${String.format(Locale.US, "%.2f", totalIncome)}</Data></Cell>""")
            appendLine("""        <Cell ss:StyleID="Total"><Data ss:Type="Number">${String.format(Locale.US, "%.2f", totalExpense)}</Data></Cell>""")
            appendLine("""        <Cell ss:StyleID="Total"><Data ss:Type="Number">${String.format(Locale.US, "%.2f", closingBalance)}</Data></Cell>""")
            appendLine("""        <Cell ss:StyleID="Total"><Data ss:Type="String"></Data></Cell>""")
            appendLine("""      </Row>""")
            appendLine("""    </Table>""")
            appendLine("""  </Worksheet>""")
            appendLine("""</Workbook>""")
        }
    }

    private fun paymentMethodLabel(method: PaymentMethod, language: AppLanguage): String {
        return if (language == AppLanguage.BANGLA) {
            when (method) {
                PaymentMethod.CASH -> "নগদ"
                PaymentMethod.MOBILE_BANKING -> "মোবাইল ব্যাংকিং"
                PaymentMethod.BANK -> "ব্যাংক"
                PaymentMethod.DUE_CREDIT -> "বাকি"
            }
        } else {
            when (method) {
                PaymentMethod.CASH -> "Cash"
                PaymentMethod.MOBILE_BANKING -> "Mobile Banking"
                PaymentMethod.BANK -> "Bank"
                PaymentMethod.DUE_CREDIT -> "Due / Credit"
            }
        }
    }

    private fun csvEscape(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    private fun xmlEscape(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    companion object {
        fun defaultCategories(): List<CategoryEntity> = listOf(
            // Income categories
            CategoryEntity(nameEn = "Daily Sales", nameBn = "দৈনিক বিক্রয়", type = TransactionType.INCOME, iconKey = "storefront"),
            CategoryEntity(nameEn = "Wholesale", nameBn = "পাইকারি বিক্রয়", type = TransactionType.INCOME, iconKey = "inventory"),
            CategoryEntity(nameEn = "Due Collected", nameBn = "বাকি আদায়", type = TransactionType.INCOME, iconKey = "payments"),
            CategoryEntity(nameEn = "Service Charge", nameBn = "সার্ভিস চার্জ", type = TransactionType.INCOME, iconKey = "handyman"),
            CategoryEntity(nameEn = "Other Income", nameBn = "অন্যান্য আয়", type = TransactionType.INCOME, iconKey = "savings"),
            // Expense categories
            CategoryEntity(nameEn = "Stock Purchase", nameBn = "মাল ক্রয়", type = TransactionType.EXPENSE, iconKey = "shopping_bag"),
            CategoryEntity(nameEn = "Shop Rent", nameBn = "দোকান ভাড়া", type = TransactionType.EXPENSE, iconKey = "home_work"),
            CategoryEntity(nameEn = "Electricity & Bills", nameBn = "বিদ্যুৎ ও বিল", type = TransactionType.EXPENSE, iconKey = "bolt"),
            CategoryEntity(nameEn = "Staff Salary", nameBn = "কর্মচারী বেতন", type = TransactionType.EXPENSE, iconKey = "groups"),
            CategoryEntity(nameEn = "Transport", nameBn = "পরিবহন ও ভাড়া", type = TransactionType.EXPENSE, iconKey = "local_shipping"),
            CategoryEntity(nameEn = "Due Given", nameBn = "বাকি প্রদান", type = TransactionType.EXPENSE, iconKey = "receipt_long"),
            CategoryEntity(nameEn = "Misc Expense", nameBn = "অন্যান্য খরচ", type = TransactionType.EXPENSE, iconKey = "more_horiz")
        )
    }
}
