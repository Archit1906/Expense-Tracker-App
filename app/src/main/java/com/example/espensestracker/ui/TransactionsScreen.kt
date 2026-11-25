package com.example.espensestracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
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
import com.example.espensestracker.data.catOf
import com.example.espensestracker.data.spend
import com.example.espensestracker.sms.SmsParser
import com.example.espensestracker.util.dayLabel
import com.example.espensestracker.util.formatMoney
import com.example.espensestracker.util.startOfDay
import com.example.espensestracker.util.startOfMonthMillis

@Composable
fun TransactionsScreen(vm: ExpenseViewModel, onOpen: (Long) -> Unit) {
    val all by vm.transactions.collectAsStateWithLifecycle()
    val pending by vm.pending.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    var showSearch by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var account by remember { mutableStateOf<String?>(null) }
    var category by remember { mutableStateOf<String?>(null) }
    var kind by remember { mutableIntStateOf(0) }      // 0 all, 1 expenses, 2 money received
    var period by remember { mutableIntStateOf(0) }    // 0 all time, 1 this month, 2 last 7 days, 3 last 30 days

    val accounts = all.filter { it.account != null }.groupBy { it.account!! }
        .map { (a, l) -> a to "${l.firstNotNullOfOrNull { it.sender }?.let(SmsParser::bankName) ?: "Card"} ••$a" }
    val cats = all.map { it.category }.distinct()
    val q = query.trim().lowercase()
    val now = System.currentTimeMillis()
    val day = 86_400_000L
    val since = when (period) { 1 -> startOfMonthMillis(); 2 -> now - 7 * day; 3 -> now - 30 * day; else -> 0L }
    val filtered = all.filter { t ->
        t.timestamp >= since &&
            (account == null || t.account == account) &&
            (category == null || t.category == category) &&
            (kind == 0 || (kind == 1 && t.type == TYPE_DEBIT) || (kind == 2 && t.type != TYPE_DEBIT)) &&
            (q.isEmpty() || listOfNotNull(t.merchant, t.note, t.category, "%.2f".format(t.amount), t.amount.toLong().toString())
                .any { it.lowercase().contains(q) })
    }
    val activeFilters = listOf(account != null, category != null, kind != 0, period != 0).count { it }
    val groups = filtered.groupBy { startOfDay(it.timestamp) }.toList()

    Column(Modifier.padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Transactions", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("Executive Ledger • Automated SMS & Manual", fontSize = 12.sp, color = Muted)
            }
            Row {
                IconButton({ showSearch = !showSearch; if (!showSearch) query = "" }) {
                    Icon(Icons.Default.Search, "Search", tint = if (showSearch || q.isNotEmpty()) Gold else LocalContentColor.current)
                }
                IconButton({ showFilters = !showFilters }) {
                    Icon(Icons.Filled.FilterList, "Filters", tint = if (showFilters || activeFilters > 0) Gold else LocalContentColor.current)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Surface(shape = RoundedCornerShape(50), color = PanelBg) {
            Row(Modifier.padding(4.dp)) {
                listOf("All Expenses (${all.size})", "Unrecognized (${pending.size})").forEachIndexed { i, label ->
                    Surface(
                        Modifier.weight(1f).clickable { tab = i }, shape = RoundedCornerShape(50),
                        color = if (tab == i) CardBg else Color.Transparent
                    ) { Text(label, Modifier.padding(12.dp), fontSize = 13.sp, textAlign = TextAlign.Center) }
                }
            }
        }

        if (tab == 0 && showSearch) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                query, { query = it }, Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(50),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                placeholder = { Text("Search merchant, note, category or amount…") }
            )
        }
        if (tab == 0 && showFilters) {
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All time", "This month", "Last 7 days", "Last 30 days").forEachIndexed { i, l ->
                    item { FilterChip(period == i, { period = i }, { Text(l) }, colors = chipColors()) }
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All types", "Expenses", "Money received").forEachIndexed { i, l ->
                    item { FilterChip(kind == i, { kind = i }, { Text(l) }, colors = chipColors()) }
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(account == null, { account = null }, { Text("All Accounts") }, colors = chipColors()) }
                items(accounts) { (a, label) -> FilterChip(account == a, { account = a }, { Text(label) }, colors = chipColors()) }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(category == null, { category = null }, { Text("All Categories") }, colors = chipColors()) }
                items(cats) { c ->
                    FilterChip(category == c, { category = c }, { Text(c) },
                        leadingIcon = { Icon(catOf(c).icon, null, Modifier.size(18.dp)) }, colors = chipColors())
                }
            }
        }
        if (tab == 0 && (activeFilters > 0 || q.isNotEmpty())) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${filtered.size} result(s)", fontSize = 12.sp, color = Muted)
                TextButton({ account = null; category = null; kind = 0; period = 0; query = "" }) { Text("Clear filters", color = Gold) }
            }
        }
        Spacer(Modifier.height(8.dp))

        if (tab == 1) {
            QueueScreen(vm, pending)
        } else if (all.isEmpty() && pending.isEmpty()) {
            Panel { Text("No transactions yet. Tap + to add one, or grant SMS access on Home.", color = Muted) }
        } else if (filtered.isEmpty() && (activeFilters > 0 || q.isNotEmpty())) {
            Panel { Text("Nothing matches these filters.", color = Muted) }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 96.dp)) {
                if (pending.isNotEmpty() && activeFilters == 0 && q.isEmpty()) item {
                    Surface(Modifier.fillMaxWidth().clickable { tab = 1 }, shape = RoundedCornerShape(16.dp), color = CardBg) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("${pending.size} Parser Exceptions", fontWeight = FontWeight.Bold)
                                Text("Bank SMS formats require category tagging", fontSize = 12.sp, color = Muted)
                            }
                            Text("VIEW QUEUE →", fontSize = 11.sp, color = Gold, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                groups.forEach { (dayStart, list) ->
                    item(key = "h$dayStart") {
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(dayLabel(dayStart), fontWeight = FontWeight.SemiBold)
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
