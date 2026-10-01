package com.example.ibanregistry

import android.content.Context
import androidx.room.Room
import com.example.ibanregistry.data.local.AppDatabase
import com.example.ibanregistry.data.repository.LocalBankAccountRepository
import com.example.ibanregistry.domain.BankAccountRepository

class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "iban-registry.db",
    ).addMigrations(AppDatabase.MIGRATION_1_2).build()

    val bankAccountRepository: BankAccountRepository =
        LocalBankAccountRepository(database.bankAccountDao())
}
