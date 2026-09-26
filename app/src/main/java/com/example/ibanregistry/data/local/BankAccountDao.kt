package com.example.ibanregistry.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BankAccountDao {
    @Query("SELECT * FROM bank_accounts ORDER BY description COLLATE NOCASE, iban")
    fun observeAll(): Flow<List<BankAccountEntity>>

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
}
