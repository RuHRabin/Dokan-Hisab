package com.dokanhisab.app.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.provider.DocumentsContract
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream

data class BackupFileInfo(
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val lastModifiedMillis: Long
)

sealed interface RestoreResult {
    data class Success(
        val restoredTransactions: Int,
        val restoredCategories: Int
    ) : RestoreResult

    data class Error(val message: String) : RestoreResult
}

object DatabaseBackupManager {

    private const val SQLITE_HEADER_MAGIC = "SQLite format 3"

    suspend fun exportDatabaseSnapshotToFile(
        context: Context,
        database: DokanDatabase,
        destinationFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val sourceDbFile = checkpointAndResolveDatabaseFile(context, database)
                ?: return@withContext false
            destinationFile.parentFile?.mkdirs()
            sourceDbFile.inputStream().use { input ->
                destinationFile.outputStream().use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            }
            destinationFile.exists() && destinationFile.length() > 0L
        }.getOrDefault(false)
    }

    suspend fun backupDatabaseToUri(
        context: Context,
        database: DokanDatabase,
        destinationUri: Uri
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val outStream = context.contentResolver.openOutputStream(destinationUri)
                ?: return@withContext false
            outStream.use { output ->
                writeDatabaseSnapshotToStream(context, database, output)
            }
        }.getOrDefault(false)
    }

    suspend fun backupDatabaseToDirectory(
        context: Context,
        database: DokanDatabase,
        treeUri: Uri,
        fileName: String
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId)
            val createdUri = DocumentsContract.createDocument(
                context.contentResolver,
                parentDocUri,
                "application/octet-stream",
                fileName
            ) ?: return@withContext false
            backupDatabaseToUri(context, database, createdUri)
        }.getOrDefault(false)
    }

    suspend fun listBackupsInDirectory(
        context: Context,
        treeUri: Uri
    ): List<BackupFileInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocId)
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            )
            val results = mutableListOf<BackupFileInfo>()
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                val modIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)

                while (cursor.moveToNext()) {
                    val docId = if (idIdx >= 0) cursor.getString(idIdx) else continue
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx) ?: "" else ""
                    val mime = if (mimeIdx >= 0) cursor.getString(mimeIdx) ?: "" else ""
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) continue
                    if (!name.endsWith(".db", ignoreCase = true) && !name.endsWith(".sqlite", ignoreCase = true)) {
                        continue
                    }
                    val size = if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) cursor.getLong(sizeIdx) else 0L
                    val modified = if (modIdx >= 0 && !cursor.isNull(modIdx)) cursor.getLong(modIdx) else 0L
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                    results.add(
                        BackupFileInfo(
                            uri = docUri,
                            displayName = name,
                            sizeBytes = size,
                            lastModifiedMillis = modified
                        )
                    )
                }
            }
            results.sortedByDescending { it.lastModifiedMillis }
        }.getOrDefault(emptyList())
    }

    suspend fun restoreDatabaseFromUri(
        context: Context,
        database: DokanDatabase,
        sourceUri: Uri
    ): RestoreResult = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "restore_stage_${System.currentTimeMillis()}.db")
        try {
            val copied = runCatching {
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                        output.flush()
                    }
                    true
                } ?: false
            }.getOrDefault(false)

            if (!copied || !tempFile.exists() || tempFile.length() == 0L) {
                return@withContext RestoreResult.Error("Unable to read selected backup file")
            }

            restoreDatabaseFromFile(database, tempFile)
        } finally {
            runCatching { tempFile.delete() }
        }
    }

    suspend fun restoreDatabaseFromFile(
        database: DokanDatabase,
        backupFile: File
    ): RestoreResult = withContext(Dispatchers.IO) {
        if (!isValidSqliteHeader(backupFile)) {
            return@withContext RestoreResult.Error("Selected file is not a valid SQLite database")
        }

        var sqliteDb: SQLiteDatabase? = null
        try {
            sqliteDb = SQLiteDatabase.openDatabase(
                backupFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            )

            // Integrity check
            var integrityOk = false
            sqliteDb.rawQuery("PRAGMA quick_check", null).use { c ->
                if (c.moveToFirst()) {
                    integrityOk = c.getString(0).equals("ok", ignoreCase = true)
                }
            }
            if (!integrityOk) {
                return@withContext RestoreResult.Error("Database integrity check failed")
            }

            val existingTables = mutableSetOf<String>()
            sqliteDb.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table'",
                null
            ).use { c ->
                while (c.moveToNext()) {
                    existingTables.add(c.getString(0))
                }
            }
            if (!existingTables.contains("categories") || !existingTables.contains("transactions")) {
                return@withContext RestoreResult.Error("Backup file does not contain Dokan Hisab tables")
            }

            val converters = RoomConverters()
            val categories = readCategories(sqliteDb, converters)
            val transactions = readTransactions(sqliteDb, converters)
            val hasChatTable = existingTables.contains("chat_messages")
            val chatMessages = if (hasChatTable) readChatMessages(sqliteDb) else emptyList()

            database.withTransaction {
                val txDao = database.transactionDao()
                val catDao = database.categoryDao()
                val chatDao = database.chatMessageDao()

                txDao.deleteAllTransactions()
                catDao.deleteAllCategories()
                if (categories.isNotEmpty()) {
                    catDao.insertCategories(categories)
                }
                if (transactions.isNotEmpty()) {
                    txDao.insertTransactions(transactions)
                }
                if (hasChatTable) {
                    chatDao.clearAllMessages()
                    if (chatMessages.isNotEmpty()) {
                        chatDao.insertMessages(chatMessages)
                    }
                }
            }

            RestoreResult.Success(
                restoredTransactions = transactions.size,
                restoredCategories = categories.size
            )
        } catch (t: Throwable) {
            RestoreResult.Error(t.message ?: "Restore failed")
        } finally {
            runCatching { sqliteDb?.close() }
        }
    }

    private fun writeDatabaseSnapshotToStream(
        context: Context,
        database: DokanDatabase,
        outputStream: OutputStream
    ): Boolean {
        val dbFile = checkpointAndResolveDatabaseFile(context, database) ?: return false
        dbFile.inputStream().use { input ->
            input.copyTo(outputStream)
            outputStream.flush()
        }
        return true
    }

    private fun checkpointAndResolveDatabaseFile(
        context: Context,
        database: DokanDatabase
    ): File? {
        val writableDb = database.openHelper.writableDatabase
        writableDb.query("PRAGMA wal_checkpoint(TRUNCATE)").use { cursor ->
            cursor.moveToFirst()
        }
        val path = writableDb.path
        val file = if (!path.isNullOrBlank()) File(path) else context.getDatabasePath("dokan_hisab.db")
        return if (file.exists() && file.length() > 0L) file else null
    }

    private fun isValidSqliteHeader(file: File): Boolean {
        if (!file.exists() || file.length() < 16L) return false
        return runCatching {
            val buffer = ByteArray(SQLITE_HEADER_MAGIC.length)
            file.inputStream().use { input ->
                val read = input.read(buffer)
                read == buffer.size && String(buffer, Charsets.US_ASCII) == SQLITE_HEADER_MAGIC
            }
        }.getOrDefault(false)
    }

    private fun readCategories(db: SQLiteDatabase, converters: RoomConverters): List<CategoryEntity> {
        val list = mutableListOf<CategoryEntity>()
        db.rawQuery(
            "SELECT id, nameEn, nameBn, type, iconKey, isDefault FROM categories ORDER BY id ASC",
            null
        ).use { c ->
            val idIdx = c.getColumnIndexOrThrow("id")
            val enIdx = c.getColumnIndexOrThrow("nameEn")
            val bnIdx = c.getColumnIndexOrThrow("nameBn")
            val typeIdx = c.getColumnIndexOrThrow("type")
            val iconIdx = c.getColumnIndexOrThrow("iconKey")
            val defIdx = c.getColumnIndexOrThrow("isDefault")

            while (c.moveToNext()) {
                list.add(
                    CategoryEntity(
                        id = c.getLong(idIdx),
                        nameEn = c.getString(enIdx) ?: "",
                        nameBn = c.getString(bnIdx) ?: "",
                        type = converters.toTransactionType(c.getString(typeIdx) ?: TransactionType.INCOME.name),
                        iconKey = c.getString(iconIdx) ?: "category",
                        isDefault = c.getInt(defIdx) != 0
                    )
                )
            }
        }
        return list
    }

    private fun readTransactions(db: SQLiteDatabase, converters: RoomConverters): List<TransactionEntity> {
        val list = mutableListOf<TransactionEntity>()
        db.rawQuery(
            "SELECT id, title, amount, type, categoryId, paymentMethod, partyName, note, timestamp, createdAt FROM transactions ORDER BY id ASC",
            null
        ).use { c ->
            val idIdx = c.getColumnIndexOrThrow("id")
            val titleIdx = c.getColumnIndexOrThrow("title")
            val amtIdx = c.getColumnIndexOrThrow("amount")
            val typeIdx = c.getColumnIndexOrThrow("type")
            val catIdx = c.getColumnIndexOrThrow("categoryId")
            val payIdx = c.getColumnIndexOrThrow("paymentMethod")
            val partyIdx = c.getColumnIndexOrThrow("partyName")
            val noteIdx = c.getColumnIndexOrThrow("note")
            val tsIdx = c.getColumnIndexOrThrow("timestamp")
            val createdIdx = c.getColumnIndexOrThrow("createdAt")

            while (c.moveToNext()) {
                list.add(
                    TransactionEntity(
                        id = c.getLong(idIdx),
                        title = c.getString(titleIdx) ?: "",
                        amount = c.getDouble(amtIdx),
                        type = converters.toTransactionType(c.getString(typeIdx) ?: TransactionType.INCOME.name),
                        categoryId = c.getLong(catIdx),
                        paymentMethod = converters.toPaymentMethod(c.getString(payIdx) ?: PaymentMethod.CASH.name),
                        partyName = c.getString(partyIdx) ?: "",
                        note = c.getString(noteIdx) ?: "",
                        timestamp = c.getLong(tsIdx),
                        createdAt = c.getLong(createdIdx)
                    )
                )
            }
        }
        return list
    }

    private fun readChatMessages(db: SQLiteDatabase): List<ChatMessageEntity> {
        val list = mutableListOf<ChatMessageEntity>()
        db.rawQuery(
            "SELECT id, role, text, modelId, timestamp FROM chat_messages ORDER BY id ASC",
            null
        ).use { c ->
            val idIdx = c.getColumnIndexOrThrow("id")
            val roleIdx = c.getColumnIndexOrThrow("role")
            val textIdx = c.getColumnIndexOrThrow("text")
            val modelIdx = c.getColumnIndexOrThrow("modelId")
            val tsIdx = c.getColumnIndexOrThrow("timestamp")

            while (c.moveToNext()) {
                list.add(
                    ChatMessageEntity(
                        id = c.getLong(idIdx),
                        role = c.getString(roleIdx) ?: "user",
                        text = c.getString(textIdx) ?: "",
                        modelId = c.getString(modelIdx) ?: "",
                        timestamp = c.getLong(tsIdx)
                    )
                )
            }
        }
        return list
    }
}
