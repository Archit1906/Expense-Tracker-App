package com.example.espensestracker.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.OnConflictStrategy
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** "Payments to this merchant are always this category" - learned from the user's corrections. */
@Entity(tableName = "merchant_rules")
data class MerchantRule(
    @PrimaryKey val merchantKey: String,
    val category: String,
    val displayName: String?
)

@Dao
interface MerchantRuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: MerchantRule)

    @Query("SELECT * FROM merchant_rules WHERE merchantKey = :key")
    suspend fun get(key: String): MerchantRule?

    @Query("SELECT * FROM merchant_rules ORDER BY merchantKey")
    fun observeAll(): Flow<List<MerchantRule>>

    @Query("DELETE FROM merchant_rules WHERE merchantKey = :key")
    suspend fun delete(key: String)
}
