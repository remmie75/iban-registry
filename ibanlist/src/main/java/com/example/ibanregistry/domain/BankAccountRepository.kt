package com.example.ibanregistry.domain

import kotlinx.coroutines.flow.Flow

interface BankAccountRepository {
    fun observeAccounts(): Flow<List<BankAccount>>
    suspend fun getAccounts(): List<BankAccount>
    suspend fun getAccount(id: Long): BankAccount?
    suspend fun saveAccount(account: BankAccount): Long
    suspend fun deleteAccount(id: Long)
    suspend fun importAccounts(accounts: List<BankAccount>): BackupImportResult
    suspend fun moveAccount(id: Long, offset: Int)
}

class DuplicateIbanException : IllegalArgumentException()

class AccountPersistenceException(cause: Throwable) : RuntimeException(cause)

data class BackupImportResult(
    val added: Int,
    val updated: Int,
    val unchanged: Int,
) {
    val total: Int = added + updated + unchanged
}
