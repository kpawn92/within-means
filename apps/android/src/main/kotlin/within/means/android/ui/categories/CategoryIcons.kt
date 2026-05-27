package within.means.android.ui.categories

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.Home
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Maps the domain icon identifier (a stable lowercase string) to a
 * concrete Compose [ImageVector]. Unknown ids fall back to a label icon.
 */
val CategoryIconCatalog: Map<String, ImageVector> = mapOf(
    "work" to Icons.Filled.Work,
    "computer" to Icons.Filled.Computer,
    "home" to Icons.Filled.Home,
    "house" to Icons.Outlined.Home,
    "shopping_cart" to Icons.Filled.ShoppingCart,
    "restaurant" to Icons.Filled.Restaurant,
    "directions_bus" to Icons.Filled.DirectionsBus,
    "local_gas_station" to Icons.Filled.LocalGasStation,
    "favorite" to Icons.Filled.Favorite,
    "school" to Icons.Filled.School,
    "subscriptions" to Icons.Filled.Subscriptions,
    "celebration" to Icons.Filled.Celebration,
    "swap_horiz" to Icons.Filled.SwapHoriz,
)

fun iconFor(name: String): ImageVector = CategoryIconCatalog[name] ?: Icons.Filled.Label

val CategoryColorPalette: List<String> = listOf(
    "#2E7D32", "#388E3C", "#1976D2", "#1565C0",
    "#F9A825", "#F57C00", "#0288D1", "#0277BD",
    "#C2185B", "#7B1FA2", "#5E35B1", "#D81B60",
    "#616161", "#455A64", "#E64A19",
)
