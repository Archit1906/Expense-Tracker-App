package com.example.espensestracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.espensestracker.data.AccountBalance
import com.example.espensestracker.util.formatDateTime
import com.example.espensestracker.util.formatMoney

/** Latest balances / credit limits found in bank SMS. Hidden until at least one is found. */
@Composable
fun AccountsPanel(balances: List<AccountBalance>) {
    if (balances.isEmpty()) return
    val banks = balances.filter { it.kind == "BALANCE" }
    Panel {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Accounts", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (banks.isNotEmpty()) Text(formatMoney(banks.sumOf { it.amount }, 0), fontWeight = FontWeight.SemiBold)
        }
        Text("Latest balance found in your bank SMS", fontSize = 12.sp, color = Muted)
        balances.forEach { b ->
            val limit = b.kind == "LIMIT"
            val name = if (b.last4.isEmpty()) b.bank else "${b.bank} ••${b.last4}"
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                androidx.compose.foundation.layout.Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(PanelBg),
                    contentAlignment = Alignment.Center
                ) { Icon(if (limit) Icons.Filled.CreditCard else Icons.Filled.AccountBalance, null, tint = Gold) }
                Column(Modifier.weight(1f)) {
                    Text(name, fontWeight = FontWeight.SemiBold)
                    Text((if (limit) "Available credit limit" else "Balance") + " • " + formatDateTime(b.timestamp), fontSize = 11.sp, color = Muted)
                }
                Text(formatMoney(b.amount, 0), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
