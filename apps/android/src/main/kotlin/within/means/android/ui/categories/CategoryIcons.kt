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

/** Warm brand palette from the design (13 hex), shared by the list cards and editor. */
val CategoryColorPalette: List<String> = listOf(
    "#3F8F6B", "#C8783C", "#C0504D", "#5B7FB4",
    "#8A6BB1", "#D4A12E", "#4FA3A3", "#B5556E",
    "#6E8B3D", "#A36A4F", "#717A8C", "#C77FA6",
    "#3E7A8C",
)
