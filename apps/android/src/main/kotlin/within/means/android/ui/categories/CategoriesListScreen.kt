package within.means.android.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import within.means.android.ui.components.CatIcon
import within.means.android.ui.components.WmChip
import within.means.android.ui.theme.WmRadii
import within.means.android.ui.theme.categoryColor
import within.means.categories.application.CategoryResponse

@Composable
fun CategoriesListScreen(
    onCreate: () -> Unit,
    onEdit: (categoryId: String) -> Unit,
) {
    val viewModel: CategoriesListViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val visible = viewModel.visibleItems()

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Categorías", fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(end = 44.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    kindTabs.forEach { (tab, label) ->
                        WmChip(label, state.selectedTab == tab, { viewModel.selectTab(tab) })
                    }
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(visible, key = { it.id }) { category ->
                    CategoryGridCard(category) { onEdit(category.id) }
                }
                item { NewCategoryCard(onCreate) }
            }
        }
    }
}

private val kindTabs = listOf(
    CategoryKindTab.EXPENSE to "Gastos",
    CategoryKindTab.INCOME to "Ingresos",
)

@Composable
private fun CategoryGridCard(category: CategoryResponse, onClick: () -> Unit) {
    val shape = RoundedCornerShape(WmRadii.lg)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top) {
            CatIcon(iconFor(category.icon), categoryColor(category.color), boxSize = 44.dp, iconSize = 23.dp)
            category.nature?.let { NatureBadge(it) }
        }
        Column {
            Text(category.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
            val sub = category.essentiality?.lowercase()?.replaceFirstChar { it.uppercase() }
            Text(sub ?: "—", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun NatureBadge(nature: String) {
    val label = when (nature.uppercase()) {
        "FIXED" -> "FIJO"
        "VARIABLE" -> "VARIABLE"
        else -> nature.uppercase()
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(WmRadii.pill))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun NewCategoryCard(onClick: () -> Unit) {
    val shape = RoundedCornerShape(WmRadii.lg)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(116.dp)
            .clip(shape)
            .border(1.5.dp, MaterialTheme.colorScheme.outline, shape)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Filled.Add, contentDescription = "Nueva categoría",
            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(6.dp))
        Text("Nueva", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary)
    }
}
