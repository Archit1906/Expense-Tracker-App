package com.example.espensestracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    /** Returns the new row id, or -1 if it was ignored as a duplicate (same smsHash). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNew(transaction: Transaction): Long

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAll(): Flow<List<Transaction>>

    @Query(
        "SELECT * FROM transactions " +
            "WHERE timestamp BETWEEN :from AND :to ORDER BY timestamp DESC"
    )
    fun getByMonth(from: Long, to: Long): Flow<List<Transaction>>

    @Query("SELECT COUNT(*) FROM transactions WHERE smsHash = :hash")
    suspend fun countHash(hash: String): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE refNo = :ref AND amount = :amount")
    suspend fun countRef(ref: String, amount: Double): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE amount = :amount AND type = :type AND timestamp BETWEEN :from AND :to")
    suspend fun countNear(amount: Double, type: String, from: Long, to: Long): Int

    @Query("UPDATE transactions SET category = :category, merchant = COALESCE(:merchant, merchant) WHERE merchantKey = :key")
    suspend fun applyRule(key: String, category: String, merchant: String?)

    @Query("SELECT * FROM transactions WHERE merchantKey IS NULL")
    suspend fun missingKeys(): List<Transaction>

    @Update
    suspend fun update(transaction: Transaction)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): Transaction?

    @Delete
    suspend fun delete(transaction: Transaction)
}
