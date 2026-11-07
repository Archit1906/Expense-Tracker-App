package com.example.espensestracker.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.espensestracker.data.SOURCE_SMS
import com.example.espensestracker.data.Transaction
import com.example.espensestracker.sms.SmsParser

private val SMS_PERMISSIONS = arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)

/** Permission prompt until granted, then a small "live capture" status pill. */
@Composable
fun SmsStatus(vm: ExpenseViewModel, all: List<Transaction>) {
    val ctx = LocalContext.current
    fun granted() = SMS_PERMISSIONS.all { ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED }
    var ok by remember { mutableStateOf(granted()) }
    val importing by vm.importing.collectAsStateWithLifecycle()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        ok = res.values.all { it }
    }
    LaunchedEffect(ok) { if (ok) vm.onSmsGranted() }

    if (!ok) {
        Panel(color = PanelBg) {
            Text("Enable SMS capture", fontWeight = FontWeight.Bold)
            Text(
                "Allow SMS access so bank and UPI payments are logged automatically. Messages are read and processed only on this phone.",
                fontSize = 13.sp, color = Muted
            )
            Spacer(Modifier.height(12.dp))
            Button({ launcher.launch(SMS_PERMISSIONS) }, colors = ButtonDefaults.buttonColors(containerColor = Gold)) { Text("Grant access") }
        }
    } else {
        val banks = all.filter { it.source == SOURCE_SMS }.mapNotNull { it.sender }
            .map(SmsParser::bankName).distinct().take(3).joinToString(" • ")
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = PanelBg) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Dot(Gold)
                Text(if (importing) "Scanning your SMS inbox…" else "Live SMS capture active", fontSize = 12.sp, modifier = Modifier.weight(1f))
                if (banks.isNotEmpty()) Text(banks, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
