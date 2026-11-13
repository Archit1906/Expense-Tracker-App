package com.example.espensestracker

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.espensestracker.ui.*
import com.example.espensestracker.util.AppSettings

// FragmentActivity (not plain ComponentActivity) because BiometricPrompt needs it.
class MainActivity : FragmentActivity() {
    private var locked by mutableStateOf(false)
    private var stoppedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppSettings.load(this)
        applySecureFlag()
        locked = AppSettings.lockEnabled
        setContent {
            LedgerTheme { if (locked) LockScreen(::authenticate) else ExpenseApp() }
        }
    }

    override fun onStart() {
        super.onStart()
        applySecureFlag()
        // Re-lock if the app was in the background for more than 30 seconds.
        if (AppSettings.lockEnabled && stoppedAt != 0L && System.currentTimeMillis() - stoppedAt > 30_000) locked = true
    }

    override fun onStop() {
        super.onStop()
        stoppedAt = System.currentTimeMillis()
    }

    private fun applySecureFlag() {
        if (AppSettings.lockEnabled) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    private fun authenticate() {
        if (BiometricManager.from(this).canAuthenticate(LOCK_AUTH) != BiometricManager.BIOMETRIC_SUCCESS) {
            locked = false   // phone has no screen lock any more, so there is nothing to check against
            return
        }
        val prompt = BiometricPrompt(
            this, ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { locked = false }
            }
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Expense Tracker")
                .setAllowedAuthenticators(LOCK_AUTH)
                .build()
        )
    }
}

@Composable
private fun LockScreen(onUnlock: () -> Unit) {
    LaunchedEffect(Unit) { onUnlock() }
    Surface(Modifier.fillMaxSize(), color = Paper) {
        Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
            Text("🔒", style = MaterialTheme.typography.displayMedium)
            Text("Expense Tracker is locked", Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
            Button(onUnlock, colors = ButtonDefaults.buttonColors(containerColor = Gold)) { Text("Unlock") }
        }
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
            composable("settings") { SettingsScreen(vm) }
            composable("form/{id}", listOf(navArgument("id") { type = NavType.LongType })) {
                TransactionFormScreen(vm, it.arguments?.getLong("id") ?: -1L) { nav.popBackStack() }
            }
        }
    }
}
