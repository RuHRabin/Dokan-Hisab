package com.dokanhisab.app.domain

import com.dokanhisab.app.data.local.CategoryEntity
import com.dokanhisab.app.data.local.TransactionType
import com.dokanhisab.app.data.local.TransactionWithCategory
import java.util.Locale

data class AutoCategorizationMatch(
    val category: CategoryEntity,
    val matchedTerm: String,
    val fromHistory: Boolean
)

/**
 * Offline-first bilingual keyword and history matcher for Dokan Hisab transactions.
 * Analyzes English, Bangla Unicode, and Romanized Bangla (Banglish) particulars as well
 * as past ledger entries to pick the most likely category automatically.
 */
object TransactionAutoCategorizer {

    private val whitespaceRegex = Regex("""\s+""")
    private val stripPunctuationRegex = Regex("""[^\p{L}\p{N}\p{M}\s]""")

    private data class KeywordRule(
        val iconKey: String,
        val type: TransactionType,
        val keywords: List<String>
    )

    private val rules: List<KeywordRule> = listOf(
        // Expense rules, weighted by specificity
        KeywordRule(
            iconKey = "bolt",
            type = TransactionType.EXPENSE,
            keywords = listOf(
                "electricity", "electric", "electricity bill", "power bill", "current bill",
                "desco", "dpdc", "nesco", "bpdb", "palli bidyut", "polli bidyut",
                "bidyut", "biddut", "bidut", "current", "meter",
                "gas bill", "titas", "gas", "water bill", "wasa", "pani bill", "pani",
                "internet bill", "internet", "wifi", "broadband", "net bill",
                "mobile bill", "recharge", "flexiload", "utility", "bill",
                "বিদ্যুৎ", "বিদ্যুৎ বিল", "কারেন্ট", "কারেন্ট বিল", "মিটার",
                "গ্যাস", "গ্যাস বিল", "পানি", "পানির বিল", "ওয়াসা",
                "ইন্টারনেট", "ওয়াইফাই", "রিচার্জ", "বিল"
            )
        ),
        KeywordRule(
            iconKey = "home_work",
            type = TransactionType.EXPENSE,
            keywords = listOf(
                "shop rent", "store rent", "monthly rent", "godown rent", "warehouse rent",
                "rent", "bhara", "vara", "dokan bhara", "dokan vara", "godown",
                "warehouse", "lease", "dokaan bhara",
                "দোকান ভাড়া", "মাসিক ভাড়া", "গোডাউন ভাড়া", "গোডাউন", "ভাড়া"
            )
        ),
        KeywordRule(
            iconKey = "groups",
            type = TransactionType.EXPENSE,
            keywords = listOf(
                "staff salary", "employee salary", "salary", "wages", "payroll",
                "beton", "kormochari", "staff", "worker", "employee", "helper",
                "salesman", "manager", "eid bonus", "bonus", "overtime", "hajira",
                "বেতন", "কর্মচারী বেতন", "কর্মচারী", "মজুরি", "স্টাফ", "বোনাস", "হাজিরা"
            )
        ),
        KeywordRule(
            iconKey = "local_shipping",
            type = TransactionType.EXPENSE,
            keywords = listOf(
                "transport", "delivery", "courier", "parcel", "freight", "shipping",
                "van", "rickshaw", "truck", "pickup", "cng", "auto", "fare",
                "pathao", "steadfast", "redx", "sundarban", "sa paribahan",
                "fuel", "petrol", "diesel", "octane", "mobil", "unloading", "kuli", "labour", "labor",
                "পরিবহন", "ভ্যান", "রিকশা", "রিক্সা", "ট্রাক", "পিকআপ",
                "কুরিয়ার", "ডেলিভারি", "পার্সেল", "সিএনজি", "তেল", "পেট্রোল",
                "ডিজেল", "কুলি", "যাতায়াত", "মাল আনা", "গাড়ি ভাড়া"
            )
        ),
        KeywordRule(
            iconKey = "receipt_long",
            type = TransactionType.EXPENSE,
            keywords = listOf(
                "due given", "credit given", "baki dilam", "baki deya", "udhar",
                "hawlat", "haolat", "loan given", "baki", "due", "credit",
                "বাকি প্রদান", "বাকি দিলাম", "বাকী", "বাকি", "ধার", "হাওলাত"
            )
        ),
        KeywordRule(
            iconKey = "shopping_bag",
            type = TransactionType.EXPENSE,
            keywords = listOf(
                "stock purchase", "wholesale purchase", "inventory", "stock", "purchase",
                "wholesale", "supplier", "mahajan", "mohajon", "maal", "mal", "goods",
                "restock", "buy", "bought", "carton", "bosta", "sack", "dozen",
                "rice", "chal", "dal", "oil", "soyabean", "sugar", "chini", "flour",
                "ata", "moida", "onion", "peyaj", "potato", "alu", "salt", "lobon",
                "tea", "soap", "detergent", "biscuit", "drinks", "chips", "milk",
                "মাল ক্রয়", "পাইকারি ক্রয়", "মাল কেনা", "স্টক", "মহাজন", "মাল",
                "ক্রয়", "কেনা", "চাল", "ডাল", "তেল", "চিনি", "আটা", "ময়দা",
                "পেঁয়াজ", "আলু", "লবণ", "সাবান", "বিস্কুট", "বস্তা", "কার্টন"
            )
        ),
        KeywordRule(
            iconKey = "more_horiz",
            type = TransactionType.EXPENSE,
            keywords = listOf(
                "misc", "miscellaneous", "repair", "maintenance", "cleaning",
                "tea snacks", "nasta", "nashta", "snacks", "stationery", "polythene",
                "shopping bag", "tape", "paper", "pen", "broom",
                "অন্যান্য খরচ", "মেরামত", "নাস্তা", "চা-নাস্তা", "পলিথিন", "কাগজ", "কলম", "সাজসজ্জা"
            )
        ),

        // Income rules
        KeywordRule(
            iconKey = "payments",
            type = TransactionType.INCOME,
            keywords = listOf(
                "due collected", "due collection", "collection", "recovered", "recovery",
                "paid back", "baki aday", "baki adaye", "baki pawa", "pawna aday",
                "settle", "settlement", "aday",
                "বাকি আদায়", "বাকী আদায়", "বাকি টাকা আদায়", "পাওনা আদায়", "আদায়", "ধার ফেরত"
            )
        ),
        KeywordRule(
            iconKey = "inventory",
            type = TransactionType.INCOME,
            keywords = listOf(
                "wholesale", "bulk sale", "bulk", "dealer", "distributor",
                "paikari", "paikari bikri", "lot sale", "carton sale", "bosta bikri",
                "পাইকারি বিক্রয়", "পাইকারি বিক্রি", "পাইকারি", "ডিলার", "বস্তা বিক্রি"
            )
        ),
        KeywordRule(
            iconKey = "handyman",
            type = TransactionType.INCOME,
            keywords = listOf(
                "service charge", "service", "repairing", "repair", "commission",
                "bkash charge", "nagad charge", "rocket charge", "photocopy", "print",
                "scan", "lamination", "installation", "fitting", "fee",
                "সার্ভিস চার্জ", "সার্ভিস", "কমিশন", "মেরামত", "ফটোকপি", "প্রিন্ট", "চার্জ"
            )
        ),
        KeywordRule(
            iconKey = "storefront",
            type = TransactionType.INCOME,
            keywords = listOf(
                "daily sales", "daily sale", "cash sale", "counter sale", "retail sale",
                "sales", "sale", "sold", "sell", "bikri", "bikroy", "pos",
                "customer", "grocery sale",
                "দৈনিক বিক্রয়", "নগদ বিক্রি", "কাউন্টার বিক্রি", "বিক্রি", "বিক্রয়", "বেচা"
            )
        ),
        KeywordRule(
            iconKey = "savings",
            type = TransactionType.INCOME,
            keywords = listOf(
                "other income", "cashback", "interest", "scrap", "empty carton",
                "empty sack", "bonus", "extra income",
                "অন্যান্য আয়", "ক্যাশব্যাক", "ভাঙারি", "বোনাস", "অতিরিক্ত আয়"
            )
        )
    )

    fun suggestCategory(
        description: String,
        type: TransactionType,
        availableCategories: List<CategoryEntity>,
        recentTransactions: List<TransactionWithCategory> = emptyList()
    ): AutoCategorizationMatch? {
        val candidates = availableCategories.filter { it.type == type }
        if (candidates.isEmpty()) return null

        val normalizedInput = normalize(description)
        if (normalizedInput.length < 2) return null
        val inputTokens = normalizedInput.split(' ').filter { it.isNotBlank() }

        // 1. Check past transactions of the same type for recurring shop descriptions/suppliers
        val historyMatch = matchFromHistory(
            normalizedInput = normalizedInput,
            inputTokens = inputTokens,
            type = type,
            candidates = candidates,
            recentTransactions = recentTransactions
        )
        if (historyMatch != null) return historyMatch

        // 2. Match directly against category names (handles custom categories created by the shopkeeper)
        val categoryNameMatch = matchCategoryNameDirectly(
            normalizedInput = normalizedInput,
            inputTokens = inputTokens,
            candidates = candidates
        )

        // 3. Score against curated English / Bangla / Banglish keyword rules
        var bestCategory: CategoryEntity? = null
        var bestScore = 0
        var bestTerm = ""

        for (rule in rules) {
            if (rule.type != type) continue
            val targetCategory = candidates.firstOrNull { it.iconKey == rule.iconKey } ?: continue

            for (rawKeyword in rule.keywords) {
                val keyword = normalize(rawKeyword)
                if (keyword.isEmpty()) continue
                val score = scoreMatch(normalizedInput, inputTokens, keyword)
                if (score > bestScore) {
                    bestScore = score
                    bestCategory = targetCategory
                    bestTerm = rawKeyword
                }
            }
        }

        if (categoryNameMatch != null && categoryNameMatch.second >= bestScore) {
            return AutoCategorizationMatch(
                category = categoryNameMatch.first,
                matchedTerm = categoryNameMatch.third,
                fromHistory = false
            )
        }

        return if (bestCategory != null && bestScore > 0) {
            AutoCategorizationMatch(
                category = bestCategory,
                matchedTerm = bestTerm,
                fromHistory = false
            )
        } else {
            null
        }
    }

    private fun matchFromHistory(
        normalizedInput: String,
        inputTokens: List<String>,
        type: TransactionType,
        candidates: List<CategoryEntity>,
        recentTransactions: List<TransactionWithCategory>
    ): AutoCategorizationMatch? {
        val candidateById = candidates.associateBy { it.id }
        var bestMatch: AutoCategorizationMatch? = null
        var bestScore = 0

        for (item in recentTransactions) {
            val tx = item.transaction
            if (tx.type != type) continue
            val category = candidateById[tx.categoryId] ?: continue
            val pastTitle = normalize(tx.title)
            if (pastTitle.length < 2) continue

            val score = when {
                pastTitle == normalizedInput -> 1000
                normalizedInput.length >= 4 && pastTitle.startsWith(normalizedInput) -> 600 + normalizedInput.length
                pastTitle.length >= 4 && normalizedInput.contains(pastTitle) -> 500 + pastTitle.length
                inputTokens.isNotEmpty() -> {
                    val pastTokens = pastTitle.split(' ').filter { it.length >= 3 }.toSet()
                    val shared = inputTokens.filter { it.length >= 3 && it in pastTokens }
                    if (shared.size >= 2) 350 + shared.sumOf { it.length } else 0
                }
                else -> 0
            }

            if (score > bestScore) {
                bestScore = score
                bestMatch = AutoCategorizationMatch(
                    category = category,
                    matchedTerm = tx.title,
                    fromHistory = true
                )
            }
        }

        return bestMatch
    }

    private fun matchCategoryNameDirectly(
        normalizedInput: String,
        inputTokens: List<String>,
        candidates: List<CategoryEntity>
    ): Triple<CategoryEntity, Int, String>? {
        var best: Triple<CategoryEntity, Int, String>? = null
        for (cat in candidates) {
            val names = listOf(cat.nameEn, cat.nameBn)
            for (rawName in names) {
                val normName = normalize(rawName)
                if (normName.length < 2) continue
                val score = scoreMatch(normalizedInput, inputTokens, normName)
                if (score > (best?.second ?: 0)) {
                    best = Triple(cat, score + 40, rawName)
                }
            }
        }
        return best
    }

    private fun scoreMatch(
        normalizedInput: String,
        inputTokens: List<String>,
        keyword: String
    ): Int {
        if (normalizedInput == keyword) {
            return 300 + keyword.length * 4
        }
        if (keyword.contains(' ')) {
            // Multi-word phrase match (e.g. "electricity bill", "বাকি আদায়")
            if (normalizedInput.contains(keyword)) {
                return 220 + keyword.length * 3
            }
            return 0
        }

        // Single keyword: prefer exact token match first, then prefix/substring for Bangla/inflected forms
        if (inputTokens.any { it == keyword }) {
            return 160 + keyword.length * 3
        }
        if (keyword.length >= 3 && inputTokens.any { token ->
                token.startsWith(keyword) || (token.length >= 3 && keyword.startsWith(token) && token.length * 2 >= keyword.length)
            }
        ) {
            return 90 + keyword.length * 2
        }
        if (keyword.length >= 4 && normalizedInput.contains(keyword)) {
            return 60 + keyword.length
        }
        return 0
    }

    private fun normalize(text: String): String {
        return stripPunctuationRegex
            .replace(text.lowercase(Locale.ROOT), " ")
            .replace(whitespaceRegex, " ")
            .trim()
    }
}
