package com.example.ibanregistry.data.local

import androidx.room.Entity
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
)
