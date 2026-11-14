package com.example.espensestracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.espensestracker.data.TYPE_DEBIT
import com.example.espensestracker.data.spend
import com.example.espensestracker.util.dayLabel
import com.example.espensestracker.util.formatMoney
import com.example.espensestracker.util.startOfDay

@Composable
fun TransactionsScreen(vm: ExpenseViewModel, onOpen: (Long) -> Unit) {
    val all by vm.transactions.collectAsStateWithLifecycle()
    val pending by vm.pending.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    val groups = all.groupBy { startOfDay(it.timestamp) }.toList()

    Column(Modifier.padding(horizontal = 16.dp)) {
        Text("Transactions", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Executive Ledger • Automated SMS & Manual", fontSize = 12.sp, color = Muted)
        Spacer(Modifier.height(12.dp))
        Surface(shape = RoundedCornerShape(50), color = PanelBg) {
            Row(Modifier.padding(4.dp)) {
                listOf("All Expenses (${all.size})", "Unrecognized (${pending.size})").forEachIndexed { i, label ->
                    Surface(
                        Modifier.weight(1f).clickable { tab = i }, shape = RoundedCornerShape(50),
                        color = if (tab == i) Color.White else Color.Transparent
                    ) { Text(label, Modifier.padding(12.dp), fontSize = 13.sp, textAlign = TextAlign.Center) }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (tab == 1) {
            QueueScreen(vm, pending)
        } else if (all.isEmpty() && pending.isEmpty()) {
            Panel { Text("No transactions yet. Tap + to add one, or grant SMS access on Home.", color = Muted) }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 96.dp)) {
                if (pending.isNotEmpty()) item {
                    Surface(Modifier.fillMaxWidth().clickable { tab = 1 }, shape = RoundedCornerShape(16.dp), color = Color.White) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("${pending.size} Parser Exceptions", fontWeight = FontWeight.Bold)
                                Text("Bank SMS formats require category tagging", fontSize = 12.sp, color = Muted)
                            }
                            Text("VIEW QUEUE →", fontSize = 11.sp, color = Gold, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                groups.forEach { (day, list) ->
                    item(key = "h$day") {
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(dayLabel(day), fontWeight = FontWeight.SemiBold)
                            Surface(shape = RoundedCornerShape(8.dp), color = PanelBg) {
                                Text("Total Outflow: " + formatMoney(maxOf(0.0, list.sumOf { it.spend() })), Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 11.sp)
                            }
                        }
                    }
                    items(list, key = { it.id }) { t -> TxRow(t) { onOpen(t.id) } }
                }
            }
        }
    }
}
