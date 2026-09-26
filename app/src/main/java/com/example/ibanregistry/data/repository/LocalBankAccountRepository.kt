package com.example.ibanregistry.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.example.ibanregistry.data.local.BankAccountDao
import com.example.ibanregistry.data.local.BankAccountEntity
import com.example.ibanregistry.domain.BankAccount
import com.example.ibanregistry.domain.BankAccountRepository
import com.example.ibanregistry.domain.AccountPersistenceException
import com.example.ibanregistry.domain.DuplicateIbanException
import com.example.ibanregistry.domain.IbanValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalBankAccountRepository(
    private val dao: BankAccountDao,
) : BankAccountRepository {
    override fun observeAccounts(): Flow<List<BankAccount>> =
        dao.observeAll().map { accounts -> accounts.map(BankAccountEntity::toDomain) }

    override suspend fun getAccount(id: Long): BankAccount? =
        dao.findById(id)?.toDomain()

    override suspend fun saveAccount(account: BankAccount): Long {
        val normalizedIban = IbanValidator.normalize(account.iban)
        val duplicateId = dao.findIdByIban(normalizedIban)
        if (duplicateId != null && duplicateId != account.id) {
            throw DuplicateIbanException()
        }

        val entity = BankAccountEntity(
            id = account.id,
            iban = normalizedIban,
            description = account.description.trim(),
        )
        return try {
            if (account.id == 0L) {
                dao.insert(entity)
            } else {
                dao.update(entity)
                account.id
            }
        } catch (_: SQLiteConstraintException) {
            throw DuplicateIbanException()
        } catch (exception: android.database.sqlite.SQLiteException) {
            throw AccountPersistenceException(exception)
        }
    }

    override suspend fun deleteAccount(id: Long) {
        try {
            dao.deleteById(id)
        } catch (exception: android.database.sqlite.SQLiteException) {
            throw AccountPersistenceException(exception)
        }
    }
}

private fun BankAccountEntity.toDomain() = BankAccount(
    id = id,
    iban = iban,
    description = description,
)
