package com.example.espensestracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensetracker.data.SOURCE_SMS
import com.example.expensetracker.data.TYPE_DEBIT
import com.example.expensetracker.data.catOf
import com.example.expensetracker.util.*
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(vm: ExpenseViewModel, onOpen: (Long) -> Unit, onSeeAll: () -> Unit) {
    val all by vm.transactions.collectAsStateWithLifecycle()
    val month = all.filter { it.timestamp >= startOfMonthMillis() }
    val debits = month.filter { it.type == TYPE_DEBIT }
    val spent = debits.sumOf { it.amount }
    val cal = Calendar.getInstance()
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val daysLeft = cal.getActualMaximum(Calendar.DAY_OF_MONTH) - day
    val pct = (spent / MONTHLY_BUDGET).coerceIn(0.0, 1.0).toFloat()
    // Same-length comparison: this month so far vs the first `day` days of last month.
    val last = monthRange(-1)
    val lastSame = all.filter { it.type == TYPE_DEBIT && it.timestamp in last.start..minOf(last.end, last.start + day * 86_400_000L - 1) }.sumOf { it.amount }
    val delta = if (lastSame > 0) (spent - lastSame) / lastSame * 100 else null
    val byCat = debits.groupBy { it.category }.mapValues { e -> e.value.sumOf { it.amount } }
        .toList().sortedByDescending { it.second }
    val greeting = when (cal.get(Calendar.HOUR_OF_DAY)) { in 0..11 -> "Good morning"; in 12..16 -> "Good afternoon"; else -> "Good evening" }

    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp).padding(bottom = 72.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(48.dp).background(Navy, CircleShape), contentAlignment = Alignment.Center) {
                    Text(USER_NAME.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("$greeting, $USER_NAME", fontSize = 13.sp, color = Muted)
                    Text("Financial Command", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
            Surface(shape = RoundedCornerShape(50), color = Color.White) {
                Text(monthLabel(), Modifier.padding(horizontal = 14.dp, vertical = 8.dp), fontSize = 13.sp)
            }
        }

        SmsStatus(vm, all)

        Panel(color = PanelBg) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("MONEY SPENT THIS MONTH", fontSize = 11.sp, color = Muted, letterSpacing = 1.sp)
                delta?.let {
                    Surface(shape = RoundedCornerShape(50), color = Color.White) {
                        Text("%+.1f%% vs last month".format(it), Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 11.sp, color = if (it > 0) Debit else Credit)
                    }
                }
            }
            Text(formatMoney(spent, 0), fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Budget limit: ${formatMoney(MONTHLY_BUDGET, 0)}", fontSize = 12.sp)
                Text("${(pct * 100).toInt()}% used", fontSize = 12.sp)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(progress = { pct }, Modifier.fillMaxWidth().height(8.dp), color = Gold, trackColor = Color.White)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Daily Average", formatMoney(spent / day, 0), "$daysLeft days remaining", Modifier.weight(1f))
                StatTile("Total Payments", "${month.size} items", "${month.count { it.source == SOURCE_SMS }} live via SMS", Modifier.weight(1f))
            }
        }

        Panel {
            Text("Spending Wheel", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("${monthLabel()} • this month", fontSize = 12.sp, color = Muted)
            Spacer(Modifier.height(12.dp))
            if (byCat.isEmpty()) {
                Text("Add an expense to see your spending split.", color = Muted)
            } else {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Donut(byCat.map { catOf(it.first).color to it.second.toFloat() }, "Total Spent", formatCompact(spent))
                }
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    byCat.take(6).forEach { (name, amt) ->
                        Surface(shape = RoundedCornerShape(12.dp), color = Paper) {
                            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Dot(catOf(name).color); Text(name, fontSize = 12.sp); Text(formatMoney(amt, 0), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Recent Payments", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("See All (${all.size}) →", color = Gold, fontSize = 13.sp, modifier = Modifier.clickable(onClick = onSeeAll))
        }
        if (all.isEmpty()) Text("Nothing yet. Tap + to log your first payment.", color = Muted)
        all.take(5).forEach { TxRow(it) { onOpen(it.id) } }
    }
}

@Composable
private fun StatTile(label: String, value: String, sub: String, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = Color.White) {
        Column(Modifier.padding(14.dp)) {
            Text(label, fontSize = 11.sp, color = Muted)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(sub, fontSize = 11.sp, color = Muted)
        }
    }
}
