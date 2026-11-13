package com.example.espensestracker.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.espensestracker.util.AppSettings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val LOCK_AUTH = BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL

@Composable
fun SettingsScreen(vm: ExpenseViewModel) {
    val ctx = LocalContext.current
    val rules by vm.rules.collectAsStateWithLifecycle()
    val notice by vm.notice.collectAsStateWithLifecycle()
    var budgetText by remember { mutableStateOf(AppSettings.budget.toLong().toString()) }
    val canLock = remember { BiometricManager.from(ctx).canAuthenticate(LOCK_AUTH) == BiometricManager.BIOMETRIC_SUCCESS }
    var batteryOk by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        batteryOk = (ctx.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(ctx.packageName)
        onPauseOrDispose { }
    }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) vm.exportCsv(uri)
    }
    val gold = ButtonDefaults.buttonColors(containerColor = Gold)

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp).padding(bottom = 72.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", fontSize = 28.sp, fontWeight = FontWeight.Bold)

        Panel {
            Text("Monthly budget", fontWeight = FontWeight.Bold)
            Text("Drives the budget bar on Home and the ceiling on Insights", fontSize = 12.sp, color = Muted)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    budgetText, { budgetText = it.filter(Char::isDigit) }, Modifier.weight(1f),
                    prefix = { Text("₹ ") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Button({ budgetText.toLongOrNull()?.takeIf { it > 0 }?.let { AppSettings.saveBudget(ctx, it.toDouble()) } }, colors = gold) { Text("Save") }
            }
        }

        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("App lock", fontWeight = FontWeight.Bold)
                    Text(
                        if (canLock) "Fingerprint, face or PIN when opening the app. Also hides the app in recents and blocks screenshots."
                        else "Set up a screen lock (PIN, pattern or biometrics) on your phone to use this.",
                        fontSize = 12.sp, color = Muted
                    )
                }
                Switch(
                    AppSettings.lockEnabled,
                    { on ->
                        AppSettings.saveLock(ctx, on)
                        (ctx as? Activity)?.window?.let { w ->
                            if (on) w.addFlags(WindowManager.LayoutParams.FLAG_SECURE) else w.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                        }
                    },
                    enabled = canLock, colors = SwitchDefaults.colors(checkedTrackColor = Gold)
                )
            }
        }

        Panel {
            Text("SMS reliability", fontWeight = FontWeight.Bold)
            Text(
                if (batteryOk) "✅ Battery optimization is off for this app, so SMS capture won't be paused."
                else "⚠ Battery optimization can delay or block SMS capture on some phones (Xiaomi, Oppo, Vivo, Samsung).",
                fontSize = 12.sp, color = Muted
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton({ ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }, Modifier.fillMaxWidth()) { Text("Open battery settings") }
            Spacer(Modifier.height(8.dp))
            Text("After new merchant rules, re-check older entries. Only 'Other' items and the review queue are touched; categories you set are never overwritten.", fontSize = 12.sp, color = Muted)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button({ vm.reparse() }, Modifier.weight(1f), colors = gold) { Text("Re-check SMS") }
                OutlinedButton({ vm.rescan() }, Modifier.weight(1f)) { Text("Re-scan inbox") }
            }
        }

        Panel {
            Text("Your data", fontWeight = FontWeight.Bold)
            Text("Export every transaction as CSV (raw SMS text is not included).", fontSize = 12.sp, color = Muted)
            Spacer(Modifier.height(8.dp))
            Button(
                { export.launch("expenses-${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}.csv") },
                Modifier.fillMaxWidth(), colors = gold
            ) { Text("Export CSV") }
            notice?.let { Text(it, Modifier.padding(top = 8.dp), fontSize = 12.sp, color = Gold) }
        }

        Panel {
            Text("Learned merchant rules (${rules.size})", fontWeight = FontWeight.Bold)
            if (rules.isEmpty()) Text("None yet. They appear when you edit a category with 'Remember' on, or assign a queued SMS.", fontSize = 12.sp, color = Muted)
            rules.forEach { r ->
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(r.displayName ?: r.merchantKey, fontWeight = FontWeight.SemiBold)
                        Text("→ ${r.category}", fontSize = 12.sp, color = Muted)
                    }
                    TextButton({ vm.deleteRule(r.merchantKey) }) { Text("Remove", color = Debit) }
                }
            }
        }

        Panel(color = PanelBg) {
            Text("Everything stays on this phone. Accounts are stored as the last 4 digits only.", fontSize = 12.sp, color = Muted)
        }
    }
}
