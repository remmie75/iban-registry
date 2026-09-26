package com.example.ibanregistry.domain

import kotlinx.coroutines.flow.Flow

interface BankAccountRepository {
    fun observeAccounts(): Flow<List<BankAccount>>
    suspend fun getAccount(id: Long): BankAccount?
    suspend fun saveAccount(account: BankAccount): Long
    suspend fun deleteAccount(id: Long)
}

class DuplicateIbanException : IllegalArgumentException()

class AccountPersistenceException(cause: Throwable) : RuntimeException(cause)
