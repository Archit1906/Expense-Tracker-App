package com.example.espensestracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** A financial-looking SMS the parser could not fully understand: waits in the review queue. */
@Entity(tableName = "unparsed_sms", indices = [Index(value = ["smsHash"], unique = true)])
data class UnparsedSms(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String,
    val body: String,
    val timestamp: Long,
    val amount: Double,
    val account: String?,
    val refNo: String?,
    val reason: String,
    val smsHash: String,
    val merchantKey: String? = null,
    val dismissed: Boolean = false
)

@Dao
interface UnparsedSmsDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(s: UnparsedSms): Long

    @Query("SELECT * FROM unparsed_sms WHERE dismissed = 0 ORDER BY timestamp DESC")
    fun observePending(): Flow<List<UnparsedSms>>

    @Query("UPDATE unparsed_sms SET dismissed = 1 WHERE id = :id")
    suspend fun dismiss(id: Long)

    @Query("UPDATE unparsed_sms SET dismissed = 1")
    suspend fun dismissAll()

    @Delete
    suspend fun delete(s: UnparsedSms)

    @Query("SELECT * FROM unparsed_sms WHERE merchantKey = :key AND dismissed = 0")
    suspend fun pendingByKey(key: String): List<UnparsedSms>

    @Query("SELECT * FROM unparsed_sms WHERE merchantKey IS NULL")
    suspend fun missingKeys(): List<UnparsedSms>

    @Query("UPDATE unparsed_sms SET merchantKey = :key WHERE id = :id")
    suspend fun setKey(id: Long, key: String)

    @Query("SELECT * FROM unparsed_sms WHERE dismissed = 0")
    suspend fun pendingAll(): List<UnparsedSms>
}
