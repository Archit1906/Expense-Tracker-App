package com.example.espensestracker.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.espensestracker.util.AppSettings

const val TYPE_DEBIT = "DEBIT"
const val TYPE_CREDIT = "CREDIT"
const val TYPE_REFUND = "REFUND"
const val SOURCE_MANUAL = "MANUAL"
const val SOURCE_SMS = "SMS"

/**
 * One money movement. Fields like rawSms / sender / smsHash stay null for manual
 * entries and get filled in Phase 2 when SMS parsing is added.
 * smsHash is unique (SQLite allows many NULLs), which gives us dedup later.
 */
@Entity(
    tableName = "transactions",
    indices = [Index(value = ["smsHash"], unique = true)]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val type: String = TYPE_DEBIT,
    val merchant: String? = null,
    val category: String = "Other",
    val account: String? = null,
    val refNo: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String? = null,
    val source: String = SOURCE_MANUAL,
    val rawSms: String? = null,
    val sender: String? = null,
    val smsHash: String? = null,
    val taxDeductible: Boolean = false,
    val merchantKey: String? = null
)

/**
 * How much this transaction changes your real spending:
 * expenses add, refunds subtract, transfers (card-bill payments, own-account moves) count as 0, and
 * other money received (income) subtracts too when [netCredits] is on (Settings: "Net all received money").
 */
fun Transaction.spend(netCredits: Boolean = AppSettings.netCredits): Double = when {
    category == TRANSFER_CATEGORY -> 0.0
    type == TYPE_DEBIT -> amount
    type == TYPE_REFUND -> -amount
    type == TYPE_CREDIT && netCredits -> -amount
    else -> 0.0
}
