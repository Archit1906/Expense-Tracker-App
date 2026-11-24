package com.example.espensestracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.espensestracker.util.AppSettings
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

@Immutable
class LedgerColors(
    val navy: Color, val ink: Color, val paper: Color, val panel: Color, val card: Color,
    val muted: Color, val debit: Color, val credit: Color,
    val goldSoft: Color, val debitSoft: Color, val barTrack: Color
)

private val LightLedger = LedgerColors(
    navy = Color(0xFF2B3445), ink = Color(0xFF1B1B1B), paper = Color(0xFFF7F5F0), panel = Color(0xFFEFECE5),
    card = Color.White, muted = Color(0xFF6E6A62), debit = Color(0xFFB3261E), credit = Color(0xFF2E7D32),
    goldSoft = Color(0xFFF4E6C3), debitSoft = Color(0xFFFDECEA), barTrack = Color(0xFFE3DFD6)
)
private val DarkLedger = LedgerColors(
    navy = Color(0xFF4A5A7A), ink = Color(0xFFECE9E2), paper = Color(0xFF121214), panel = Color(0xFF232328),
    card = Color(0xFF1C1C20), muted = Color(0xFFA6A39B), debit = Color(0xFFEF9A9A), credit = Color(0xFF81C784),
    goldSoft = Color(0xFF4A3F22), debitSoft = Color(0xFF3B2220), barTrack = Color(0xFF34343A)
)

val LocalLedger = staticCompositionLocalOf { LightLedger }

val Navy: Color @Composable get() = LocalLedger.current.navy
val Ink: Color @Composable get() = LocalLedger.current.ink
val Paper: Color @Composable get() = LocalLedger.current.paper
val PanelBg: Color @Composable get() = LocalLedger.current.panel
val CardBg: Color @Composable get() = LocalLedger.current.card
val Muted: Color @Composable get() = LocalLedger.current.muted
val Debit: Color @Composable get() = LocalLedger.current.debit
val Credit: Color @Composable get() = LocalLedger.current.credit
val GoldSoft: Color @Composable get() = LocalLedger.current.goldSoft
val DebitSoft: Color @Composable get() = LocalLedger.current.debitSoft
val BarTrack: Color @Composable get() = LocalLedger.current.barTrack

@Composable
fun LedgerTheme(content: @Composable () -> Unit) {
    val dark = when (AppSettings.themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val l = if (dark) DarkLedger else LightLedger
    val scheme = if (dark) darkColorScheme(
        primary = Gold, onPrimary = Color.White, background = l.paper, surface = l.card,
        onSurface = l.ink, onBackground = l.ink, secondaryContainer = l.goldSoft
    ) else lightColorScheme(
        primary = Gold, onPrimary = Color.White, background = l.paper, surface = l.card,
        onSurface = l.ink, onBackground = l.ink, secondaryContainer = l.goldSoft
    )
    CompositionLocalProvider(LocalLedger provides l) { MaterialTheme(colorScheme = scheme, content = content) }
}

@Composable
fun Panel(
    modifier: Modifier = Modifier,
    color: Color = CardBg,
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
    ) { Icon(c.icon, null, tint = c.color) }
}

@Composable
fun TxRow(t: Transaction, onClick: () -> Unit) {
    val debit = t.type == TYPE_DEBIT
    Surface(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(20.dp), color = CardBg) {
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
