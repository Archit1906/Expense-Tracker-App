package com.example.espensestracker.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.espensestracker.util.AppSettings
import com.example.espensestracker.work.WeeklyScheduler
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val LOCK_AUTH = BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL

@Composable
fun SettingsScreen(vm: ExpenseViewModel) {
    val ctx = LocalContext.current
    val rules by vm.rules.collectAsStateWithLifecycle()
    val notice by vm.notice.collectAsStateWithLifecycle()
    val restoreState by vm.restoreState.collectAsStateWithLifecycle()
    var budgetText by remember { mutableStateOf(AppSettings.budget.toLong().toString()) }
    val canLock = remember { BiometricManager.from(ctx).canAuthenticate(LOCK_AUTH) == BiometricManager.BIOMETRIC_SUCCESS }
    var batteryOk by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        batteryOk = (ctx.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(ctx.packageName)
        onPauseOrDispose { }
    }
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val gold = ButtonDefaults.buttonColors(containerColor = Gold)

    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) vm.exportCsv(uri)
    }

    // notifications permission (Android 13+)
    var pendingAction by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pendingAction?.invoke(granted); pendingAction = null
    }
    fun withNotifPermission(onResult: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) onResult(true)
        else { pendingAction = onResult; notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }
    }

    // backup & restore
    var showBackupDialog by remember { mutableStateOf(false) }
    var backupPassword by remember { mutableStateOf("") }
    var restorePassword by remember { mutableStateOf("") }
    val createBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) vm.backupTo(uri, backupPassword)
        backupPassword = ""
    }
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.prepareRestore(uri)
    }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp).padding(bottom = 72.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        notice?.let { Panel(color = PanelBg) { Text(it, fontSize = 13.sp, color = Gold) } }

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
            Text("Appearance", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("System", "Light", "Dark").forEachIndexed { i, label ->
                    FilterChip(AppSettings.themeMode == i, { AppSettings.saveTheme(ctx, i) }, { Text(label) }, colors = chipColors())
                }
            }
        }

        Panel {
            Text("Alerts & summaries", fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Unusual spend alerts", fontWeight = FontWeight.SemiBold)
                    Text("Notify me when a new payment is far above my usual for that merchant (or overall). Amounts are hidden on the lock screen.", fontSize = 12.sp, color = Muted)
                }
                Switch(
                    AppSettings.alertsEnabled,
                    { on -> if (on) withNotifPermission { ok -> AppSettings.saveAlerts(ctx, ok) } else AppSettings.saveAlerts(ctx, false) },
                    colors = SwitchDefaults.colors(checkedTrackColor = Gold)
                )
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Weekly summary", fontWeight = FontWeight.SemiBold)
                    Text("Sunday evening: last 7 days vs the week before, plus your biggest category.", fontSize = 12.sp, color = Muted)
                }
                Switch(
                    AppSettings.weeklyEnabled,
                    { on ->
                        if (on) withNotifPermission { ok -> AppSettings.saveWeekly(ctx, ok); if (ok) WeeklyScheduler.schedule(ctx) }
                        else { AppSettings.saveWeekly(ctx, false); WeeklyScheduler.cancel(ctx) }
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = Gold)
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton({ withNotifPermission { ok -> if (ok) vm.sendTestSummary() } }, Modifier.fillMaxWidth()) { Text("Send a test summary now") }
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Net all received money", fontWeight = FontWeight.Bold)
                    Text(
                        "On: every credit (salary, friends paying you back, refunds) reduces your spending totals. " +
                            "Off: only expenses minus refunds. Transfers are always ignored.",
                        fontSize = 12.sp, color = Muted
                    )
                }
                Switch(AppSettings.netCredits, { AppSettings.saveNetCredits(ctx, it) }, colors = SwitchDefaults.colors(checkedTrackColor = Gold))
            }
        }

        Panel {
            Text("SMS reliability", fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(if (batteryOk) Icons.Default.CheckCircle else Icons.Default.Warning, null, Modifier.size(18.dp), tint = if (batteryOk) Credit else Gold)
                Text(
                    if (batteryOk) "Battery optimization is off for this app, so SMS capture won't be paused."
                    else "Battery optimization can delay or block SMS capture on some phones (Xiaomi, Oppo, Vivo, Samsung).",
                    fontSize = 12.sp, color = Muted
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton({ ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }, Modifier.fillMaxWidth()) { Text("Open battery settings") }
            Spacer(Modifier.height(8.dp))
            Text("After new merchant rules, re-check older entries. Only 'Other' items and the review queue are touched; categories you set are never overwritten. Re-scan also refreshes account balances.", fontSize = 12.sp, color = Muted)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button({ vm.reparse() }, Modifier.weight(1f), colors = gold) { Text("Re-check SMS") }
                OutlinedButton({ vm.rescan() }, Modifier.weight(1f)) { Text("Re-scan inbox") }
            }
        }

        Panel {
            Text("Backup & restore", fontWeight = FontWeight.Bold)
            Text("Saves everything (transactions, queue, rules, balances, settings) to a file you keep. Restoring replaces all data on this phone.", fontSize = 12.sp, color = Muted)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button({ showBackupDialog = true }, Modifier.weight(1f), colors = gold) { Text("Create backup") }
                OutlinedButton({ openBackup.launch(arrayOf("*/*")) }, Modifier.weight(1f)) { Text("Restore") }
            }
        }

        Panel {
            Text("Export", fontWeight = FontWeight.Bold)
            Text("Every transaction as CSV (raw SMS text is not included).", fontSize = 12.sp, color = Muted)
            Spacer(Modifier.height(8.dp))
            Button({ exportCsv.launch("expenses-$today.csv") }, Modifier.fillMaxWidth(), colors = gold) { Text("Export CSV") }
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

    if (showBackupDialog) AlertDialog(
        onDismissRequest = { showBackupDialog = false },
        title = { Text("Create backup") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("A password encrypts the file (AES-256). Leave it empty for a plain file. If you set a password and forget it, the backup cannot be recovered.", fontSize = 13.sp)
                OutlinedTextField(
                    backupPassword, { backupPassword = it }, label = { Text("Password (optional)") },
                    singleLine = true, visualTransformation = PasswordVisualTransformation()
                )
            }
        },
        confirmButton = { TextButton({ showBackupDialog = false; createBackup.launch("expense-backup-$today.etb") }) { Text("Choose location") } },
        dismissButton = { TextButton({ showBackupDialog = false; backupPassword = "" }) { Text("Cancel") } }
    )

    restoreState?.let { encrypted ->
        AlertDialog(
            onDismissRequest = { vm.cancelRestore(); restorePassword = "" },
            title = { Text("Restore backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This replaces ALL current data on this phone with the contents of the backup.", fontSize = 13.sp)
                    if (encrypted) OutlinedTextField(
                        restorePassword, { restorePassword = it }, label = { Text("Backup password") },
                        singleLine = true, visualTransformation = PasswordVisualTransformation()
                    )
                }
            },
            confirmButton = { TextButton({ vm.confirmRestore(restorePassword); restorePassword = "" }) { Text("Restore", color = Debit) } },
            dismissButton = { TextButton({ vm.cancelRestore(); restorePassword = "" }) { Text("Cancel") } }
        )
    }
}
