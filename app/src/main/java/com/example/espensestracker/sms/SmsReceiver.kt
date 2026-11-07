package com.example.espensestracker.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (msgs.isNullOrEmpty()) return
        val sender = msgs[0].originatingAddress ?: return
        val body = msgs.joinToString("") { it.messageBody ?: "" }

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Use arrival time so it lines up with the inbox import's DATE column.
                SmsProcessor(context).process(sender, body, System.currentTimeMillis())
            } finally {
                pending.finish()
            }
        }
    }
}
