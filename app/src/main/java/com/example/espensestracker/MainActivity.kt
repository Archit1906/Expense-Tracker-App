package com.example.espensestracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.ui.unit.dp
import com.example.espensestracker.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { LedgerTheme { ExpenseApp() } }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "Home", Icons.Default.Home),
    Tab("transactions", "Transactions", Icons.AutoMirrored.Filled.List),
    Tab("insights", "Insights", Icons.Default.Star),
    Tab("settings", "Settings", Icons.Default.Settings)
)

@Composable
fun ExpenseApp(vm: ExpenseViewModel = viewModel()) {
    val nav = rememberNavController()
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    val showTabs = route in tabs.map { it.route }

    Scaffold(
        bottomBar = {
            if (showTabs) NavigationBar(containerColor = Color.White) {
                tabs.forEach { t ->
                    NavigationBarItem(
                        selected = route == t.route,
                        onClick = {
                            nav.navigate(t.route) {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true; restoreState = true
                            }
                        },
                        icon = { Icon(t.icon, null) }, label = { Text(t.label) },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = Color(0xFFF4E6C3), selectedIconColor = Ink)
                    )
                }
            }
        },
        floatingActionButton = {
            if (route == "home" || route == "transactions") FloatingActionButton(
                onClick = { nav.navigate("form/-1") }, containerColor = Gold, contentColor = Color.White
            ) { Icon(Icons.Default.Add, "Add transaction") }
        }
    ) { pad ->
        NavHost(nav, "home", Modifier.padding(pad)) {
            composable("home") { HomeScreen(vm, { nav.navigate("form/$it") }, { nav.navigate("transactions") }) }
            composable("transactions") { TransactionsScreen(vm) { nav.navigate("form/$it") } }
            composable("insights") { InsightsScreen(vm) { nav.navigate("form/$it") } }
            composable("settings") { Soon("Settings", "Budget, categories, export and app lock arrive in Phase 5.") }
            composable("form/{id}", listOf(navArgument("id") { type = NavType.LongType })) {
                TransactionFormScreen(vm, it.arguments?.getLong("id") ?: -1L) { nav.popBackStack() }
            }
        }
    }
}

@Composable
private fun Soon(title: String, body: String) = Box(Modifier.fillMaxSize(), Alignment.Center) {
    androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(body, color = Muted, modifier = Modifier.padding(24.dp))
    }
}
