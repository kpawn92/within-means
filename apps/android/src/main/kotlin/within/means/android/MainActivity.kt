package within.means.android

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.koin.compose.koinInject
import within.means.android.persistence.DatabaseUnlocker
import within.means.android.persistence.OnboardingState
import within.means.android.ui.analytics.StatsScreen
import within.means.android.ui.categories.CategoriesListScreen
import within.means.android.ui.categories.CategoryEditScreen
import within.means.android.ui.home.HomeScreen
import within.means.android.ui.onboarding.OnboardingScreen
import within.means.android.ui.settings.SettingsScreen
import within.means.android.ui.transactions.TransactionEditScreen
import within.means.android.ui.transactions.TransactionsListScreen
import within.means.android.ui.unlock.UnlockScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WithinMeansTheme {
                WithinMeansApp()
            }
        }
    }
}

private object Routes {
    const val Onboarding = "onboarding"
    const val Unlock = "unlock"
    const val Home = "home"
    const val Categories = "categories"
    const val CategoryNew = "categories/new"
    const val CategoryEdit = "categories/edit/{categoryId}"
    const val Transactions = "transactions"
    const val TransactionNew = "transactions/new"
    const val TransactionEdit = "transactions/edit/{transactionId}"
    const val Stats = "stats"
    const val Settings = "settings"

    fun categoryEdit(categoryId: String): String = "categories/edit/$categoryId"
    fun transactionEdit(transactionId: String): String = "transactions/edit/$transactionId"
}

private enum class BottomTab(
    val route: String,
    val icon: ImageVector,
    val label: String,
) {
    HOME(Routes.Home, Icons.Filled.Home, "Inicio"),
    TRANSACTIONS(Routes.Transactions, Icons.AutoMirrored.Filled.List, "Movimientos"),
    STATS(Routes.Stats, Icons.Filled.BarChart, "Estadísticas"),
    CATEGORIES(Routes.Categories, Icons.Filled.Category, "Categorías"),
    SETTINGS(Routes.Settings, Icons.Filled.Settings, "Ajustes"),
}

private val authRoutes = setOf(Routes.Onboarding, Routes.Unlock)

@Composable
private fun WithinMeansApp() {
    val onboardingState: OnboardingState = koinInject()
    val unlocker: DatabaseUnlocker = koinInject()
    val navController = rememberNavController()

    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        startDestination = when {
            !onboardingState.isCompleted -> Routes.Onboarding
            !unlocker.isUnlocked -> Routes.Unlock
            else -> Routes.Home
        }
    }
    val resolved = startDestination ?: return

    val activity = LocalContext.current as? Activity
    val exitOnBack: @Composable () -> Unit = { BackHandler { activity?.finish() } }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val showBottomBar = currentRoute != null && currentRoute !in authRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) BottomNavBar(navController, currentRoute)
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = resolved,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.Onboarding) {
                exitOnBack()
                OnboardingScreen(
                    onCompleted = {
                        navController.navigate(Routes.Home) {
                            popUpTo(Routes.Onboarding) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.Unlock) {
                exitOnBack()
                UnlockScreen(
                    onUnlocked = {
                        navController.navigate(Routes.Home) {
                            popUpTo(Routes.Unlock) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.Home) {
                exitOnBack()
                HomeScreen(
                    onNewTransaction = { navController.navigate(Routes.TransactionNew) },
                    onOpenAllTransactions = { navController.navigateToTab(Routes.Transactions) },
                    onOpenTransaction = { id -> navController.navigate(Routes.transactionEdit(id)) },
                )
            }
            composable(Routes.Categories) {
                CategoriesListScreen(
                    onCreate = { navController.navigate(Routes.CategoryNew) },
                    onEdit = { id -> navController.navigate(Routes.categoryEdit(id)) },
                )
            }
            composable(Routes.CategoryNew) {
                CategoryEditScreen(
                    categoryId = null,
                    onBack = { navController.popBackStack() },
                    onFinished = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.CategoryEdit,
                arguments = listOf(navArgument("categoryId") { type = NavType.StringType }),
            ) { entry ->
                CategoryEditScreen(
                    categoryId = entry.arguments?.getString("categoryId"),
                    onBack = { navController.popBackStack() },
                    onFinished = { navController.popBackStack() },
                )
            }
            composable(Routes.Transactions) {
                TransactionsListScreen(
                    onCreate = { navController.navigate(Routes.TransactionNew) },
                    onEdit = { id -> navController.navigate(Routes.transactionEdit(id)) },
                )
            }
            composable(Routes.TransactionNew) {
                TransactionEditScreen(
                    transactionId = null,
                    onBack = { navController.popBackStack() },
                    onFinished = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.TransactionEdit,
                arguments = listOf(navArgument("transactionId") { type = NavType.StringType }),
            ) { entry ->
                TransactionEditScreen(
                    transactionId = entry.arguments?.getString("transactionId"),
                    onBack = { navController.popBackStack() },
                    onFinished = { navController.popBackStack() },
                )
            }
            composable(Routes.Stats) { StatsScreen() }
            composable(Routes.Settings) { SettingsScreen() }
        }
    }
}

@Composable
private fun BottomNavBar(navController: NavController, currentRoute: String?) {
    NavigationBar {
        BottomTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = { navController.navigateToTab(tab.route) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
            )
        }
    }
}

/**
 * Standard bottom-nav tab swap: keeps each tab's back stack isolated via
 * saveState/restoreState and avoids piling up duplicates on top of the
 * graph's start destination.
 */
private fun NavController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun WithinMeansTheme(content: @Composable () -> Unit) {
    val light = lightColorScheme(
        primary = Color(0xFF2E7D32),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFB6F0BB),
        onPrimaryContainer = Color(0xFF002106),
        secondary = Color(0xFF526350),
        background = Color(0xFFFCFDF7),
    )
    val dark = darkColorScheme(
        primary = Color(0xFF7BD389),
        onPrimary = Color(0xFF003910),
        primaryContainer = Color(0xFF005319),
        onPrimaryContainer = Color(0xFFB6F0BB),
        background = Color(0xFF101410),
    )
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    MaterialTheme(colorScheme = if (isDark) dark else light, content = content)
}
