package com.example.ibanregistry.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BankAccountDao {
    @Query("SELECT * FROM bank_accounts ORDER BY sort_order, id")
    fun observeAll(): Flow<List<BankAccountEntity>>

    @Query("SELECT * FROM bank_accounts ORDER BY sort_order, id")
    suspend fun getAll(): List<BankAccountEntity>

    @Query("SELECT * FROM bank_accounts WHERE id = :id")
    suspend fun findById(id: Long): BankAccountEntity?

    @Query("SELECT id FROM bank_accounts WHERE iban = :iban LIMIT 1")
    suspend fun findIdByIban(iban: String): Long?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(account: BankAccountEntity): Long

    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun update(account: BankAccountEntity)

    @Query("DELETE FROM bank_accounts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Transaction
    suspend fun importAll(accounts: List<BankAccountEntity>): ImportCounts {
        var added = 0
        var updated = 0
        var unchanged = 0
        val importedIbans = accounts.mapTo(mutableSetOf(), BankAccountEntity::iban)
        accounts.forEachIndexed { index, account ->
            val existingId = findIdByIban(account.iban)
            if (existingId == null) {
                insert(account.copy(id = 0, sortOrder = index.toLong()))
                added++
            } else {
                val existing = findById(existingId)
                val restored = account.copy(id = existingId, sortOrder = index.toLong())
                if (existing == restored) {
                    unchanged++
                } else {
                    update(restored)
                    updated++
                }
            }
        }
        getAll()
            .filterNot { it.iban in importedIbans }
            .forEachIndexed { index, account ->
                update(account.copy(sortOrder = (accounts.size + index).toLong()))
            }
        return ImportCounts(added = added, updated = updated, unchanged = unchanged)
    }

    @Transaction
    suspend fun move(id: Long, offset: Int) {
        val accounts = getAll()
        val fromIndex = accounts.indexOfFirst { it.id == id }
        if (fromIndex == -1) return
        val toIndex = (fromIndex + offset).coerceIn(accounts.indices)
        if (fromIndex == toIndex) return

        val reordered = accounts.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
        reordered.forEachIndexed { index, account ->
            if (account.sortOrder != index.toLong()) {
                update(account.copy(sortOrder = index.toLong()))
            }
        }
    }
}

data class ImportCounts(
    val added: Int,
    val updated: Int,
    val unchanged: Int,
)
