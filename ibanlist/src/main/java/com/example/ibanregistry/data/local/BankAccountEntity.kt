package com.example.ibanregistry.data.local

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bank_accounts",
    indices = [Index(value = ["iban"], unique = true)],
)
data class BankAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val iban: String,
    val description: String,
    val folder: String? = null,
    @ColumnInfo(defaultValue = "''") val tags: String = "",
    @ColumnInfo(name = "sort_order", defaultValue = "0") val sortOrder: Long = 0,
)
