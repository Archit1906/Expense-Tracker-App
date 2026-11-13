package com.example.espensestracker.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.espensestracker.data.*
import com.example.espensestracker.sms.SmsParser
import com.example.espensestracker.sms.SmsProcessor
import com.example.espensestracker.util.toCsv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ExpenseViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private val dao = db.transactionDao()
    private val processor = SmsProcessor(app)
    private val prefs = app.getSharedPreferences("app", Context.MODE_PRIVATE)

    val transactions: StateFlow<List<Transaction>> = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pending: StateFlow<List<UnparsedSms>> = db.unparsedSmsDao().observePending()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val rules: StateFlow<List<MerchantRule>> = db.merchantRuleDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val ruleCount: StateFlow<Int> = rules.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _importing = MutableStateFlow(false)
    val importing: StateFlow<Boolean> = _importing.asStateFlow()

    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    init {
        if (!prefs.getBoolean("keys_v3", false)) {
            viewModelScope.launch {
                processor.backfillKeys()
                prefs.edit().putBoolean("keys_v3", true).apply()
            }
        }
    }

    suspend fun get(id: Long): Transaction? = dao.getById(id)

    /** [learn] = remember this merchant's category for future (and past) payments. */
    fun save(t: Transaction, learn: Boolean = false) {
        viewModelScope.launch {
            val key = if (t.source == SOURCE_MANUAL) SmsParser.keyOf(t.merchant)
            else t.merchantKey ?: SmsParser.keyOf(t.merchant)
            val tx = t.copy(merchantKey = key)
            if (tx.id == 0L) dao.insertIfNew(tx) else dao.update(tx)
            if (learn && key != null) processor.remember(key, tx.category, tx.merchant)
        }
    }

    fun delete(t: Transaction) {
        viewModelScope.launch { dao.delete(t) }
    }

    /** First time SMS permission is available: import the existing inbox once. */
    fun onSmsGranted() {
        if (prefs.getBoolean("inbox_imported", false)) return
        prefs.edit().putBoolean("inbox_imported", true).apply()
        rescan()
    }

    fun rescan() {
        viewModelScope.launch {
            _importing.value = true
            try {
                val n = processor.importInbox()
                _notice.value = "Inbox scan finished: $n new item(s)."
            } finally { _importing.value = false }
        }
    }

    fun reparse() {
        viewModelScope.launch { _notice.value = "Re-checked: ${processor.reparse()} item(s) updated." }
    }

    fun exportCsv(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val list = dao.getAllOnce()
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { it.write(toCsv(list).toByteArray()) }
                _notice.value = "Exported ${list.size} transaction(s)."
            } catch (e: Exception) {
                _notice.value = "Export failed: ${e.message}"
            }
        }
    }

    fun deleteRule(key: String) {
        viewModelScope.launch { db.merchantRuleDao().delete(key) }
    }

    fun assign(s: UnparsedSms, category: String, merchant: String?) {
        viewModelScope.launch { processor.assign(s, category, merchant) }
    }

    fun dismiss(s: UnparsedSms) {
        viewModelScope.launch { processor.dismiss(s) }
    }

    fun dismissAll() {
        viewModelScope.launch { processor.dismissAll() }
    }
}
