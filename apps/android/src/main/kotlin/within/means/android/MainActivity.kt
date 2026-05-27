package within.means.android

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.koin.compose.koinInject
import within.means.android.persistence.DatabaseUnlocker
import within.means.android.persistence.OnboardingState
import within.means.android.ui.categories.CategoriesListScreen
import within.means.android.ui.categories.CategoryEditScreen
import within.means.android.ui.home.HomePlaceholderScreen
import within.means.android.ui.onboarding.OnboardingScreen
import within.means.android.ui.unlock.UnlockScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument

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

    fun categoryEdit(categoryId: String): String = "categories/edit/$categoryId"
}

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
            // Home is a root destination — back here must exit the app,
            // otherwise popBackStack empties the NavHost and leaves a
            // blank Surface on screen.
            exitOnBack()
            HomePlaceholderScreen(
                onOpenCategories = { navController.navigate(Routes.Categories) },
            )
        }
        composable(Routes.Categories) {
            CategoriesListScreen(
                onBack = { navController.popBackStack() },
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
