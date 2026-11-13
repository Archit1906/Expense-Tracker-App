package com.example.espensestracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.espensestracker.data.TYPE_DEBIT
import com.example.espensestracker.data.Transaction
import com.example.espensestracker.data.catOf
import com.example.espensestracker.sms.SmsParser
import com.example.espensestracker.util.*
import java.util.Calendar
import kotlin.math.abs

@Composable
fun InsightsScreen(vm: ExpenseViewModel, onOpen: (Long) -> Unit) {
    val all by vm.transactions.collectAsStateWithLifecycle()
    var offset by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var account by remember { mutableStateOf<String?>(null) }
    var category by remember { mutableStateOf<String?>(null) }

    val accounts = all.filter { it.account != null }.groupBy { it.account!! }
        .map { (a, l) -> a to "${l.firstNotNullOfOrNull { it.sender }?.let(SmsParser::bankName) ?: "Card"} ••$a" }
    val cats = all.map { it.category }.distinct()
    val q = query.trim().lowercase()
    fun keep(t: Transaction) = (account == null || t.account == account) &&
        (category == null || t.category == category) &&
        (q.isEmpty() || listOfNotNull(t.merchant, t.note, t.category, "%.2f".format(t.amount), t.amount.toLong().toString())
            .any { it.lowercase().contains(q) })

    val cur = monthRange(offset)
    val prev = monthRange(offset - 1)
    val inCur = all.filter { it.timestamp in cur.start..cur.end && keep(it) }
    val debits = inCur.filter { it.type == TYPE_DEBIT }
    val spent = debits.sumOf { it.amount }
    val lastSpent = all.filter { it.timestamp in prev.start..prev.end && it.type == TYPE_DEBIT && keep(it) }.sumOf { it.amount }

    val shown = if (offset == 0) Calendar.getInstance().get(Calendar.DAY_OF_MONTH) else cur.days
    val daily = DoubleArray(cur.days).also { arr -> debits.forEach { arr[dayOfMonth(it.timestamp) - 1] += it.amount } }
    val avg = spent / shown
    val peak = (0 until shown).maxByOrNull { daily[it] } ?: 0
    val peakTop = debits.filter { dayOfMonth(it.timestamp) == peak + 1 }.maxByOrNull { it.amount }
    val byCat = debits.groupBy { it.category }.mapValues { e -> e.value.sumOf { it.amount } }
        .toList().sortedByDescending { it.second }
    val places = debits.groupBy { it.merchantKey ?: it.merchant ?: it.category }.values
        .map { l -> Triple(l.firstNotNullOfOrNull { it.merchant } ?: l.first().category, l.sumOf { it.amount }, l) }
        .sortedByDescending { it.second }.take(5)

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp).padding(bottom = 72.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(cur.label.uppercase(), fontSize = 11.sp, color = Muted, letterSpacing = 1.sp)
                Text("Spending Analytics", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton({ offset -= 1 }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous month") }
                IconButton({ offset += 1 }, enabled = offset < 0) { Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next month") }
            }
        }

        OutlinedTextField(
            query, { query = it }, Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(50),
            leadingIcon = { Icon(Icons.Default.Search, null) },
            placeholder = { Text("Search merchant, note, or amount…") }
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { FilterChip(account == null, { account = null }, { Text("All Accounts") }, colors = chipColors()) }
            items(accounts) { (a, label) -> FilterChip(account == a, { account = a }, { Text(label) }, colors = chipColors()) }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { FilterChip(category == null, { category = null }, { Text("All Categories") }, colors = chipColors()) }
            items(cats) { c -> FilterChip(category == c, { category = c }, { Text("${catOf(c).emoji} $c") }, colors = chipColors()) }
        }

        if (q.isNotEmpty()) {
            Text("Matches in ${cur.label}: ${inCur.size}", fontWeight = FontWeight.SemiBold)
            if (inCur.isEmpty()) Text("Nothing matches.", color = Muted)
            inCur.take(20).forEach { t -> TxRow(t) { onOpen(t.id) } }
        }

        Panel(color = PanelBg) {
            Text("Month-over-Month Comparison", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CompareTile(
                    if (offset == 0) "This month (so far)" else cur.label, spent,
                    (spent / AppSettings.budget).toFloat(), Gold, "${"%.1f".format(spent / AppSettings.budget * 100)}% of ceiling", Modifier.weight(1f)
                )
                CompareTile("Last month", lastSpent, (lastSpent / maxOf(spent, lastSpent, 1.0)).toFloat(), Navy, "Baseline total", Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Color.White) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        when {
                            lastSpent <= 0 -> "No spending recorded in ${prev.label} to compare."
                            spent <= lastSpent -> "Spent ${"%.0f".format(abs(spent - lastSpent) / lastSpent * 100)}% less than ${prev.label} (saved ${formatMoney(lastSpent - spent, 0)})"
                            else -> "Spent ${"%.0f".format(abs(spent - lastSpent) / lastSpent * 100)}% more than ${prev.label} (${formatMoney(spent - lastSpent, 0)} extra)"
                        },
                        fontWeight = FontWeight.SemiBold, fontSize = 14.sp
                    )
                    Text(
                        (if (spent <= AppSettings.budget) "Under ${formatMoney(AppSettings.budget, 0)} ceiling"
                        else "Over the ${formatMoney(AppSettings.budget, 0)} ceiling by ${formatMoney(spent - AppSettings.budget, 0)}") +
                            if (offset == 0) " • month in progress" else "",
                        fontSize = 12.sp, color = Muted
                    )
                }
            }
        }

        if (debits.isEmpty()) {
            Panel { Text("No payments match for ${cur.label}.", color = Muted) }
        } else {
            Panel {
                Text("Daily Cash Outflow", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Average: ${formatMoney(avg, 0)}/day • $shown days logged", fontSize = 12.sp, color = Muted)
                Spacer(Modifier.height(10.dp))
                Surface(shape = RoundedCornerShape(10.dp), color = Navy) {
                    Text(
                        "Day ${peak + 1} peak: ${formatMoney(daily[peak], 0)}" + (peakTop?.let { " • ${it.merchant ?: it.category}" } ?: ""),
                        Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = Color.White, fontSize = 12.sp
                    )
                }
                Spacer(Modifier.height(10.dp))
                DailyBars(daily, shown, avg, peak)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Day 1", fontSize = 11.sp, color = Muted)
                    Text("Day $shown", fontSize = 11.sp, color = Muted)
                }
            }

            Panel {
                Text("Spending Distribution", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Share of spend by category", fontSize = 12.sp, color = Muted)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))) {
                    byCat.forEach { (c, a) -> Box(Modifier.weight(a.toFloat()).fillMaxHeight().background(catOf(c).color)) }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    byCat.take(3).forEach { (c, a) ->
                        Surface(Modifier.weight(1f), shape = RoundedCornerShape(14.dp), color = PanelBg) {
                            Column(Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Dot(catOf(c).color); Text(c, fontSize = 11.sp, maxLines = 1)
                                }
                                Text("%.0f%%".format(a / spent * 100), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Text(formatMoney(a, 0), fontSize = 11.sp, color = Muted)
                            }
                        }
                    }
                }
            }

            Panel {
                Text("Top Places You Spent", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Ranked by total outflow in ${cur.label}", fontSize = 12.sp, color = Muted)
                places.forEachIndexed { i, (name, amount, list) ->
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(28.dp).background(if (i == 0) Gold else Navy, CircleShape), contentAlignment = Alignment.Center) {
                            Text("${i + 1}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(name, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text("${list.size} payment${if (list.size == 1) "" else "s"} • ${list.first().category}", fontSize = 12.sp, color = Muted)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(formatMoney(amount, 0), fontWeight = FontWeight.SemiBold)
                            Text("%.1f%% of total".format(amount / spent * 100), fontSize = 11.sp, color = Muted)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(progress = { (amount / spent).toFloat() }, Modifier.fillMaxWidth().height(5.dp), color = if (i == 0) Gold else Navy, trackColor = PanelBg)
                }
            }
        }
    }
}

@Composable
private fun CompareTile(title: String, amount: Double, progress: Float, color: Color, caption: String, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = Color.White) {
        Column(Modifier.padding(14.dp)) {
            Text(title, fontSize = 11.sp, color = Muted)
            Text(formatMoney(amount, 0), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, Modifier.fillMaxWidth().height(6.dp), color = color, trackColor = PanelBg)
            Spacer(Modifier.height(4.dp))
            Text(caption, fontSize = 11.sp, color = Muted)
        }
    }
}

@Composable
private fun DailyBars(values: DoubleArray, shown: Int, avg: Double, peak: Int) {
    val maxV = (values.take(shown).maxOrNull() ?: 0.0).coerceAtLeast(1.0)
    Canvas(Modifier.fillMaxWidth().height(140.dp)) {
        val slot = size.width / shown
        val barW = slot * 0.6f
        for (i in 0 until shown) {
            val h = ((values[i] / maxV).toFloat() * size.height).coerceAtLeast(2f)
            drawRoundRect(
                if (i == peak && values[i] > 0) Gold else Color(0xFFE3DFD6),
                Offset(i * slot + (slot - barW) / 2, size.height - h), Size(barW, h), CornerRadius(4.dp.toPx())
            )
        }
        val y = size.height - (avg / maxV).toFloat() * size.height
        drawLine(Gold, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)))
    }
}
