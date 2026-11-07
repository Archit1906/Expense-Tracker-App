package com.example.espensestracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontFamily
import com.example.expensetracker.data.*
import com.example.expensetracker.sms.SmsParser

/** Used for both "new transaction" (id = -1) and "expense detail / edit". */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionFormScreen(vm: ExpenseViewModel, id: Long, onDone: () -> Unit) {
    var existing by remember { mutableStateOf<Transaction?>(null) }
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TYPE_DEBIT) }
    var merchant by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(CATEGORY_LIST.first().name) }
    var note by remember { mutableStateOf("") }
    var tax by remember { mutableStateOf(false) }
    var learn by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(id) {
        if (id > 0) vm.get(id)?.let { t ->
            existing = t; amount = t.amount.toString().removeSuffix(".0"); type = t.type
            merchant = t.merchant ?: ""; category = t.category; note = t.note ?: ""; tax = t.taxDeductible
        }
    }

    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Column {
                Text(if (existing == null) "New Transaction" else "Expense Detail", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                existing?.let { Text("TRANSACTION #TXN-${it.id}", fontSize = 11.sp, color = Muted) }
            }
        }

        Panel {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(type == TYPE_DEBIT, { type = TYPE_DEBIT }, { Text("Expense") }, colors = chipColors())
                FilterChip(type == TYPE_CREDIT, { type = TYPE_CREDIT }, { Text("Income") }, colors = chipColors())
            }
            Spacer(Modifier.height(12.dp))
            Text("TOTAL AMOUNT", fontSize = 11.sp, color = Muted)
            OutlinedTextField(
                amount, { amount = it.filter { c -> c.isDigit() || c == '.' }; error = null },
                Modifier.fillMaxWidth(), prefix = { Text("₹ ", fontSize = 24.sp, color = Gold) },
                textStyle = LocalTextStyle.current.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold),
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = error != null, supportingText = error?.let { m -> { Text(m) } }
            )
        }

        existing?.takeIf { it.source == SOURCE_SMS }?.let { t ->
            Panel(color = PanelBg) {
                val acct = t.account?.let { "${SmsParser.bankName(t.sender.orEmpty())} •• $it" } ?: "Unknown"
                Text("TRANSACTION TELEMETRY", fontSize = 11.sp, color = Muted)
                Spacer(Modifier.height(8.dp))
                Text("Source account: $acct", fontSize = 13.sp)
                t.refNo?.let { Text("UTR / Ref: $it", fontSize = 13.sp) }
                Spacer(Modifier.height(8.dp))
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = Color.White) {
                    Text(t.rawSms.orEmpty(), Modifier.padding(12.dp), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            }
        }

        Panel {
            Text("EXPENSE CATEGORY", fontSize = 11.sp, color = Muted)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CATEGORY_LIST.forEach { c ->
                    FilterChip(category == c.name, { category = c.name }, { Text("${c.emoji} ${c.name}") }, colors = chipColors())
                }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(merchant, { merchant = it }, Modifier.fillMaxWidth(), label = { Text("Merchant / payee name") }, singleLine = true)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                note, { if (it.length <= 200) note = it }, Modifier.fillMaxWidth(), minLines = 3,
                label = { Text("Personal notes & purpose") }, supportingText = { Text("${note.length}/200") }
            )
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Mark as Tax Deductible", fontWeight = FontWeight.SemiBold)
                    Text("Tags transaction for quarterly deduction summaries", fontSize = 12.sp, color = Muted)
                }
                Switch(tax, { tax = it }, colors = SwitchDefaults.colors(checkedTrackColor = Gold))
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Remember for this merchant", fontWeight = FontWeight.SemiBold)
                    Text("Applies this category to future and past payments to the same merchant", fontSize = 12.sp, color = Muted)
                }
                Switch(learn, { learn = it }, colors = SwitchDefaults.colors(checkedTrackColor = Gold))
            }
        }

        Button(
            onClick = {
                val v = amount.toDoubleOrNull()
                if (v == null || v <= 0.0) error = "Enter a valid amount" else {
                    vm.save(
                        (existing ?: Transaction(amount = v)).copy(
                            amount = v, type = type, merchant = merchant.trim().ifBlank { null },
                            category = category, note = note.trim().ifBlank { null }, taxDeductible = tax
                        ),
                        learn && merchant.isNotBlank()
                    )
                    onDone()
                }
            },
            Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Gold)
        ) { Text(if (existing == null) "Save Transaction" else "Save Changes", fontWeight = FontWeight.SemiBold) }

        existing?.let { t ->
            TextButton({ vm.delete(t); onDone() }, Modifier.align(Alignment.CenterHorizontally), colors = ButtonDefaults.textButtonColors(contentColor = Debit)) {
                Icon(Icons.Default.Delete, null); Spacer(Modifier.width(6.dp)); Text("Delete Expense")
            }
        }
    }
}
