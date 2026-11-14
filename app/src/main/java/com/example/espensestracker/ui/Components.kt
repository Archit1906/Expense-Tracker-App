package com.example.espensestracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.espensestracker.data.SOURCE_SMS
import com.example.espensestracker.data.TYPE_DEBIT
import com.example.espensestracker.data.TYPE_REFUND
import com.example.espensestracker.data.Transaction
import com.example.espensestracker.data.catOf
import com.example.espensestracker.util.formatDateTime
import com.example.espensestracker.util.formatMoney

val Gold = Color(0xFFC4A055)
val Navy = Color(0xFF2B3445)
val Ink = Color(0xFF1B1B1B)
val Paper = Color(0xFFF7F5F0)
val PanelBg = Color(0xFFEFECE5)
val Muted = Color(0xFF6E6A62)
val Debit = Color(0xFFB3261E)
val Credit = Color(0xFF2E7D32)

@Composable
fun LedgerTheme(content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = lightColorScheme(
        primary = Gold, onPrimary = Color.White, background = Paper, surface = Color.White,
        onSurface = Ink, onBackground = Ink, secondaryContainer = Color(0xFFF4E6C3)
    ),
    content = content
)

@Composable
fun Panel(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    content: @Composable ColumnScope.() -> Unit
) = Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = color) {
    Column(Modifier.padding(20.dp), content = content)
}

@Composable
fun CatIcon(category: String) {
    val c = catOf(category)
    Box(
        Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(c.color.copy(alpha = .18f)),
        contentAlignment = Alignment.Center
    ) { Text(c.emoji, fontSize = 20.sp) }
}

@Composable
fun TxRow(t: Transaction, onClick: () -> Unit) {
    val debit = t.type == TYPE_DEBIT
    Surface(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(20.dp), color = Color.White) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CatIcon(t.category)
            Column(Modifier.weight(1f)) {
                Text(t.merchant ?: t.category, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text((if (t.type == TYPE_REFUND) "Refund • " else "") + "${t.category} • ${formatDateTime(t.timestamp)}", fontSize = 12.sp, color = Muted, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text((if (debit) "-" else "+") + formatMoney(t.amount), fontWeight = FontWeight.SemiBold, color = if (debit) Debit else Credit)
                Text(t.account?.let { "•• $it" } ?: if (t.source == SOURCE_SMS) "SMS" else "Manual", fontSize = 11.sp, color = Muted)
            }
        }
    }
}

@Composable
fun Donut(slices: List<Pair<Color, Float>>, top: String, bottom: String) {
    Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(14.dp)) {
            val total = slices.sumOf { it.second.toDouble() }.toFloat()
            var angle = -90f
            slices.forEach { (c, v) ->
                val sweep = v / total * 360f
                drawArc(c, angle, (sweep - 2f).coerceAtLeast(1f), false, style = Stroke(28.dp.toPx(), cap = StrokeCap.Butt))
                angle += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(top, fontSize = 12.sp, color = Muted)
            Text(bottom, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun chipColors() = FilterChipDefaults.filterChipColors(selectedContainerColor = Navy, selectedLabelColor = Color.White)

@Composable
fun Dot(color: Color) = Box(Modifier.size(8.dp).background(color, CircleShape))
