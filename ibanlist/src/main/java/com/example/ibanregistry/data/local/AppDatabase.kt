package com.example.ibanregistry.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [BankAccountEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bankAccountDao(): BankAccountDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bank_accounts ADD COLUMN folder TEXT")
                db.execSQL(
                    "ALTER TABLE bank_accounts ADD COLUMN tags TEXT NOT NULL DEFAULT ''",
                )
                db.execSQL(
                    "ALTER TABLE bank_accounts ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL("UPDATE bank_accounts SET sort_order = id")
            }
        }
    }
}
