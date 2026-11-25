package com.example.espensestracker.backup

import com.example.espensestracker.data.AccountBalance
import com.example.espensestracker.data.MerchantRule
import com.example.espensestracker.data.Transaction
import com.example.espensestracker.data.UnparsedSms
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class BackupData(
    val transactions: List<Transaction>,
    val unparsed: List<UnparsedSms>,
    val rules: List<MerchantRule>,
    val balances: List<AccountBalance>,
    val budget: Long,
    val netCredits: Boolean
)

/** JSON backup, optionally encrypted with a password (PBKDF2 -> AES-256-GCM). */
object Backup {
    private val MAGIC = "ETB1".toByteArray()

    fun isEncrypted(bytes: ByteArray): Boolean =
        bytes.size > MAGIC.size + 28 && bytes.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)

    fun encrypt(plain: ByteArray, password: String): ByteArray {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            .apply { init(Cipher.ENCRYPT_MODE, key(password, salt), GCMParameterSpec(128, iv)) }
        return MAGIC + salt + iv + cipher.doFinal(plain)
    }

    /** Throws a GeneralSecurityException (AEADBadTagException) on a wrong password. */
    fun decrypt(data: ByteArray, password: String): ByteArray {
        val salt = data.copyOfRange(4, 20)
        val iv = data.copyOfRange(20, 32)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            .apply { init(Cipher.DECRYPT_MODE, key(password, salt), GCMParameterSpec(128, iv)) }
        return cipher.doFinal(data, 32, data.size - 32)
    }

    private fun key(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, 120_000, 256)
        return SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded, "AES")
    }

    fun toBytes(data: BackupData, password: String): ByteArray {
        val plain = toJson(data).toByteArray(Charsets.UTF_8)
        return if (password.isEmpty()) plain else encrypt(plain, password)
    }

    fun parse(bytes: ByteArray, password: String): BackupData {
        val plain = if (isEncrypted(bytes)) decrypt(bytes, password) else bytes
        return fromJson(String(plain, Charsets.UTF_8))
    }

    private fun toJson(d: BackupData): String = JSONObject()
        .put("format", 1).put("budget", d.budget).put("netCredits", d.netCredits)
        .put("transactions", JSONArray(d.transactions.map { t ->
            JSONObject().put("id", t.id).put("amount", t.amount).put("type", t.type).put("merchant", t.merchant)
                .put("category", t.category).put("account", t.account).put("refNo", t.refNo)
                .put("timestamp", t.timestamp).put("note", t.note).put("source", t.source)
                .put("rawSms", t.rawSms).put("sender", t.sender).put("smsHash", t.smsHash)
                .put("taxDeductible", t.taxDeductible).put("merchantKey", t.merchantKey)
        }))
        .put("unparsed", JSONArray(d.unparsed.map { s ->
            JSONObject().put("id", s.id).put("sender", s.sender).put("body", s.body).put("timestamp", s.timestamp)
                .put("amount", s.amount).put("account", s.account).put("refNo", s.refNo).put("reason", s.reason)
                .put("smsHash", s.smsHash).put("merchantKey", s.merchantKey).put("dismissed", s.dismissed)
        }))
        .put("rules", JSONArray(d.rules.map { r ->
            JSONObject().put("merchantKey", r.merchantKey).put("category", r.category).put("displayName", r.displayName)
        }))
        .put("balances", JSONArray(d.balances.map { b ->
            JSONObject().put("accountKey", b.accountKey).put("bank", b.bank).put("last4", b.last4)
                .put("kind", b.kind).put("amount", b.amount).put("timestamp", b.timestamp)
        }))
        .toString()

    private fun JSONObject.str(k: String): String? = if (has(k) && !isNull(k)) getString(k) else null

    private fun <T> JSONArray?.mapObjects(f: (JSONObject) -> T): List<T> =
        if (this == null) emptyList() else (0 until length()).map { f(getJSONObject(it)) }

    private fun fromJson(s: String): BackupData {
        val o = JSONObject(s)
        return BackupData(
            transactions = o.optJSONArray("transactions").mapObjects { j ->
                Transaction(
                    id = j.getLong("id"), amount = j.getDouble("amount"), type = j.getString("type"),
                    merchant = j.str("merchant"), category = j.getString("category"), account = j.str("account"),
                    refNo = j.str("refNo"), timestamp = j.getLong("timestamp"), note = j.str("note"),
                    source = j.getString("source"), rawSms = j.str("rawSms"), sender = j.str("sender"),
                    smsHash = j.str("smsHash"), taxDeductible = j.optBoolean("taxDeductible"),
                    merchantKey = j.str("merchantKey")
                )
            },
            unparsed = o.optJSONArray("unparsed").mapObjects { j ->
                UnparsedSms(
                    id = j.getLong("id"), sender = j.getString("sender"), body = j.getString("body"),
                    timestamp = j.getLong("timestamp"), amount = j.getDouble("amount"), account = j.str("account"),
                    refNo = j.str("refNo"), reason = j.getString("reason"), smsHash = j.getString("smsHash"),
                    merchantKey = j.str("merchantKey"), dismissed = j.optBoolean("dismissed")
                )
            },
            rules = o.optJSONArray("rules").mapObjects { j ->
                MerchantRule(j.getString("merchantKey"), j.getString("category"), j.str("displayName"))
            },
            balances = o.optJSONArray("balances").mapObjects { j ->
                AccountBalance(
                    j.getString("accountKey"), j.getString("bank"), j.getString("last4"),
                    j.getString("kind"), j.getDouble("amount"), j.getLong("timestamp")
                )
            },
            budget = o.optLong("budget", 25000), netCredits = o.optBoolean("netCredits", true)
        )
    }
}
