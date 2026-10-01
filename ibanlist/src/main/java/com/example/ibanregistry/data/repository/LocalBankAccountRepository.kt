package com.example.ibanregistry.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.example.ibanregistry.data.local.BankAccountDao
import com.example.ibanregistry.data.local.BankAccountEntity
import com.example.ibanregistry.domain.BackupImportResult
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

    override suspend fun getAccounts(): List<BankAccount> =
        try {
            dao.getAll().map(BankAccountEntity::toDomain)
        } catch (exception: android.database.sqlite.SQLiteException) {
            throw AccountPersistenceException(exception)
        }

    override suspend fun getAccount(id: Long): BankAccount? =
        dao.findById(id)?.toDomain()

    override suspend fun saveAccount(account: BankAccount): Long {
        val normalizedIban = IbanValidator.normalize(account.iban)
        val duplicateId = dao.findIdByIban(normalizedIban)
        if ((duplicateId != null) && (duplicateId != account.id)) {
            throw DuplicateIbanException()
        }

        val entity = BankAccountEntity(
            id = account.id,
            iban = normalizedIban,
            description = account.description.trim(),
            folder = account.folder?.trim()?.takeIf(String::isNotEmpty),
            tags = encodeTags(account.tags),
            sortOrder = account.sortOrder,
        )
        return try {
            if (account.id == 0L) {
                val nextPosition = dao.getAll().size.toLong()
                dao.insert(entity.copy(sortOrder = nextPosition))
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

    override suspend fun importAccounts(accounts: List<BankAccount>): BackupImportResult {
        val entities = accounts.map { account ->
            BankAccountEntity(
                iban = IbanValidator.normalize(account.iban),
                description = account.description.trim(),
                folder = account.folder?.trim()?.takeIf(String::isNotEmpty),
                tags = encodeTags(account.tags),
                sortOrder = account.sortOrder,
            )
        }
        return try {
            val result = dao.importAll(entities)
            BackupImportResult(
                added = result.added,
                updated = result.updated,
                unchanged = result.unchanged,
            )
        } catch (exception: android.database.sqlite.SQLiteException) {
            throw AccountPersistenceException(exception)
        }
    }

    override suspend fun moveAccount(id: Long, offset: Int) {
        try {
            dao.move(id, offset)
        } catch (exception: android.database.sqlite.SQLiteException) {
            throw AccountPersistenceException(exception)
        }
    }
}

private fun BankAccountEntity.toDomain() = BankAccount(
    id = id,
    iban = iban,
    description = description,
    folder = folder,
    tags = decodeTags(tags),
    sortOrder = sortOrder,
)

private const val TAG_SEPARATOR = "\u001F"

private fun encodeTags(tags: List<String>): String =
    tags.joinToString(TAG_SEPARATOR) { it.trim() }

private fun decodeTags(value: String): List<String> =
    value.split(TAG_SEPARATOR).filter(String::isNotBlank)
