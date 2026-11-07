package com.example.espensestracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.espensestracker.data.CATEGORY_LIST
import com.example.espensestracker.data.UnparsedSms
import com.example.espensestracker.sms.SmsParser
import com.example.espensestracker.util.formatDateTime
import com.example.espensestracker.util.formatMoney

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QueueScreen(vm: ExpenseViewModel, pending: List<UnparsedSms>) {
    var target by remember { mutableStateOf<UnparsedSms?>(null) }
    val rules by vm.ruleCount.collectAsStateWithLifecycle()

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 96.dp)) {
        item {
            Panel {
                Text("SMS Parsing Triage", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    if (pending.isEmpty()) "All clear. Every payment SMS was categorized automatically."
                    else "${pending.size} message(s) could not be categorized automatically. Review the raw text, assign a merchant and category, or dismiss.",
                    fontSize = 13.sp, color = Muted
                )
            }
        }
        items(pending, key = { it.id }) { s ->
            Panel {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${SmsParser.bankName(s.sender)} • ${s.sender}", fontSize = 12.sp, color = Muted)
                    Text(formatDateTime(s.timestamp), fontSize = 12.sp, color = Muted)
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Extracted Outflow", fontSize = 11.sp, color = Muted)
                        Text(formatMoney(s.amount), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                    Surface(shape = RoundedCornerShape(50), color = Color(0xFFFDECEA)) {
                        Text("⚠ ${s.reason}", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 11.sp, color = Debit)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = Paper) {
                    Column(Modifier.padding(12.dp)) {
                        Text("RAW SMS", fontSize = 10.sp, color = Muted)
                        Text(s.body, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button({ target = s }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Gold)) { Text("Assign Category") }
                    OutlinedButton({ vm.dismiss(s) }) { Text("Dismiss") }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton({ vm.rescan() }) { Text("Re-scan inbox", color = Gold) }
                if (pending.isNotEmpty()) TextButton({ vm.dismissAll() }) { Text("Dismiss all ${pending.size}", color = Debit) }
            }
        }
        item { Text("✨ Auto-learning: $rules merchant rule(s) saved. Assigning a category teaches the app.", fontSize = 12.sp, color = Muted) }
        item {
            Panel(color = PanelBg) {
                Text("All SMS processing happens locally on this device. Messages and account numbers are never sent to external servers.", fontSize = 12.sp, color = Muted)
            }
        }
    }

    target?.let { s ->
        var category by remember(s.id) { mutableStateOf(CATEGORY_LIST.first().name) }
        var merchant by remember(s.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { target = null },
            title = { Text("Assign category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(merchant, { merchant = it }, label = { Text("Merchant name (optional)") }, singleLine = true)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        CATEGORY_LIST.forEach { c ->
                            FilterChip(category == c.name, { category = c.name }, { Text("${c.emoji} ${c.name}", fontSize = 12.sp) }, colors = chipColors())
                        }
                    }
                }
            },
            confirmButton = { TextButton({ vm.assign(s, category, merchant); target = null }) { Text("Save") } },
            dismissButton = { TextButton({ target = null }) { Text("Cancel") } }
        )
    }
}
