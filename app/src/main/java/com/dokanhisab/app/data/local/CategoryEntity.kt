package com.dokanhisab.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val nameEn: String,
    val nameBn: String,
    val type: TransactionType,
    val iconKey: String,
    val isDefault: Boolean = true
)
