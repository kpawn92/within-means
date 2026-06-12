package within.means.android

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import within.means.android.persistence.ThemeMode
import within.means.android.persistence.ThemePreference
import within.means.android.ui.theme.WithinMeansTheme
import within.means.android.ui.analytics.StatsScreen
import within.means.android.ui.categories.CategoriesListScreen
import within.means.android.ui.categories.CategoryEditScreen
import within.means.android.ui.help.HelpAction
import within.means.android.ui.help.HelpScreen
import within.means.android.ui.home.HomeScreen
import within.means.android.ui.onboarding.IntroCarousel
import within.means.android.ui.onboarding.OnboardingScreen
import within.means.android.ui.quickadd.QuickAddSheet
import within.means.android.ui.recurring.RecurringEditScreen
import within.means.android.ui.recurring.RecurringRulesScreen
import within.means.android.ui.settings.ChangePinScreen
import within.means.android.ui.settings.SettingsScreen
import within.means.android.ui.transactions.TransactionEditScreen
import within.means.android.ui.transactions.TransactionsListScreen
import within.means.android.ui.unlock.UnlockScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Draw behind the system bars so the status/navigation bars take the app's
        // own background colour instead of the system's grey scrim. The bar icon
        // contrast is then driven by the in-app theme (see WithinMeansTheme).
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val themePreference: ThemePreference = koinInject()
            val themeMode by themePreference.mode.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            WithinMeansTheme(darkTheme = darkTheme) {
                WithinMeansApp()
            }
        }
    }
}

/**
 * Restarts the whole process and relaunches into the lock screen. Used when the
 * encrypted databases are closed in-session ("Bloquear ahora") or re-keyed
 * ("Cambiar PIN"): the persistence singletons cache the now-stale database
 * handles, so the only safe way back is a cold start with a fresh object graph.
 * Killing the process also wipes the decrypted state from memory, which is the
 * point of locking.
 */
private fun Context.relaunchToLock() {
    val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
    startActivity(intent)
    Runtime.getRuntime().exit(0)
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
    const val Recurring = "recurring"
    const val RecurringEdit = "recurring/edit/{ruleId}"
    const val Intro = "intro"
    const val ChangePin = "settings/change-pin"
    const val Help = "help"

    fun recurringEdit(ruleId: String): String = "recurring/edit/$ruleId"

    fun categoryEdit(categoryId: String): String = "categories/edit/$categoryId"
    fun transactionEdit(transactionId: String): String = "transactions/edit/$transactionId"
}

/** The four bottom-nav destinations (the center [+] is rendered separately). */
private enum class BottomTab(
    val route: String,
    val icon: ImageVector,
    val label: String,
) {
    HOME(Routes.Home, Icons.Filled.Home, "Inicio"),
    TRANSACTIONS(Routes.Transactions, Icons.AutoMirrored.Filled.List, "Movimientos"),
    STATS(Routes.Stats, Icons.Filled.BarChart, "Análisis"),
    CATEGORIES(Routes.Categories, Icons.Filled.Category, "Categorías"),
}

private val authRoutes = setOf(Routes.Onboarding, Routes.Unlock)
// Routes where the bottom nav + chrome are shown.
private val chromeRoutes = BottomTab.entries.map { it.route }.toSet()

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
    val showChrome = currentRoute in chromeRoutes

    var showQuickAdd by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        // Let auth/editor screens draw edge-to-edge; only main tabs reserve the bar.
        contentWindowInsets = if (showChrome) ScaffoldDefaults.contentWindowInsets
            else androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showChrome) {
                WMBottomBar(
                    currentRoute = currentRoute,
                    onTab = { navController.navigateToTab(it) },
                    onAdd = { showQuickAdd = true },
                )
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            NavHost(navController = navController, startDestination = resolved) {
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
                composable(Routes.Settings) {
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                        onManageRecurring = { navController.navigate(Routes.Recurring) },
                        onLockNow = { context.relaunchToLock() },
                        onReplayIntro = { navController.navigate(Routes.Intro) },
                        onChangePin = { navController.navigate(Routes.ChangePin) },
                        onOpenHelp = { navController.navigate(Routes.Help) },
                    )
                }
                composable(Routes.Help) {
                    HelpScreen(
                        onBack = { navController.popBackStack() },
                        onAction = { action ->
                            when (action) {
                                HelpAction.QUICK_ADD -> showQuickAdd = true
                                HelpAction.STATS -> navController.navigateToTab(Routes.Stats)
                                HelpAction.CATEGORIES -> navController.navigateToTab(Routes.Categories)
                            }
                        },
                    )
                }
                composable(Routes.ChangePin) {
                    ChangePinScreen(
                        onBack = { navController.popBackStack() },
                        // Re-keying closed and reopened the DBs in-session, leaving the
                        // persistence singletons stale: restart into the lock screen so
                        // the user re-enters with the new PIN against a fresh graph.
                        onDone = { context.relaunchToLock() },
                    )
                }
                composable(Routes.Recurring) {
                    RecurringRulesScreen(
                        onBack = { navController.popBackStack() },
                        onEdit = { id -> navController.navigate(Routes.recurringEdit(id)) },
                    )
                }
                composable(
                    route = Routes.RecurringEdit,
                    arguments = listOf(navArgument("ruleId") { type = NavType.StringType }),
                ) { entry ->
                    RecurringEditScreen(
                        ruleId = entry.arguments?.getString("ruleId").orEmpty(),
                        onBack = { navController.popBackStack() },
                        onFinished = { navController.popBackStack() },
                    )
                }
                composable(Routes.Intro) {
                    IntroCarousel(onDone = { navController.popBackStack() })
                }
            }

            // Top-right entries on the main tabs: a discoverable "?" help affordance
            // next to Settings (the design puts these on the avatar/topbar; this keeps
            // them reachable until Home is redesigned). Help is also reachable from
            // within Settings, so it lives in both places on purpose.
            if (showChrome) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 6.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // "?" — tonal circle, deliberately distinct from the grey gear.
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { navController.navigate(Routes.Help) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.HelpOutline,
                            contentDescription = "Ayuda",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    IconButton(onClick = { navController.navigate(Routes.Settings) }) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "Ajustes",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (showQuickAdd) {
                QuickAddSheet(
                    onDismiss = { showQuickAdd = false },
                    onSaved = { android.widget.Toast.makeText(context, "Movimiento guardado", android.widget.Toast.LENGTH_SHORT).show() },
                )
            }
        }
    }
}

/**
 * Bottom navigation matching the design: four destinations split around a
 * prominent center [+] action. The [+] is the [FloatingActionButton] docked
 * centered by the Scaffold; the bar itself leaves a gap for it.
 */
@Composable
private fun WMBottomBar(
    currentRoute: String?,
    onTab: (String) -> Unit,
    onAdd: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            NavBarItem(BottomTab.HOME, currentRoute, onTab)
            NavBarItem(BottomTab.TRANSACTIONS, currentRoute, onTab)
            // center [+]
            FloatingActionButton(
                onClick = onAdd,
                shape = RoundedCornerShape(18.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(56.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Añadir", modifier = Modifier.size(28.dp))
            }
            NavBarItem(BottomTab.STATS, currentRoute, onTab)
            NavBarItem(BottomTab.CATEGORIES, currentRoute, onTab)
        }
    }
}

@Composable
private fun NavBarItem(
    tab: BottomTab,
    currentRoute: String?,
    onTab: (String) -> Unit,
) {
    val selected = currentRoute == tab.route
    val color = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            // Clip first so the tap ripple is a rounded pill, not a square box.
            // The active tab isn't re-tappable, which avoids unexpected re-navigation.
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = !selected) { onTab(tab.route) }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(tab.icon, contentDescription = tab.label, tint = color, modifier = Modifier.size(23.dp))
        Text(
            tab.label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
        )
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
