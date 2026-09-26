package com.example.ibanregistry.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [BankAccountEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bankAccountDao(): BankAccountDao
}
