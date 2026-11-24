package com.example.espensestracker.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Latest "Avail Bal" / "Avl Limit" seen in a bank SMS, one row per bank + last-4 + kind. */
@Entity(tableName = "account_balances")
data class AccountBalance(
    @PrimaryKey val accountKey: String,
    val bank: String,
    val last4: String,
    val kind: String,      // "BALANCE" or "LIMIT"
    val amount: Double,
    val timestamp: Long
)

@Dao
interface AccountBalanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(b: AccountBalance)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<AccountBalance>)

    @Query("SELECT timestamp FROM account_balances WHERE accountKey = :key")
    suspend fun timestampOf(key: String): Long?

    @Query("SELECT * FROM account_balances ORDER BY kind, bank, last4")
    fun observeAll(): Flow<List<AccountBalance>>

    @Query("SELECT * FROM account_balances")
    suspend fun getAllOnce(): List<AccountBalance>

    @Query("DELETE FROM account_balances")
    suspend fun deleteAll()
}
