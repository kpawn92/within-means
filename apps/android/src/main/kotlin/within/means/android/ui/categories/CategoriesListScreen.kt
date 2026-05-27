package within.means.android.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import within.means.categories.application.CategoryResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesListScreen(
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onEdit: (categoryId: String) -> Unit,
) {
    val viewModel: CategoriesListViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDelete by remember { mutableStateOf<CategoryResponse?>(null) }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categorías") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreate) {
                Icon(Icons.Filled.Add, contentDescription = "Crear categoría")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = state.selectedTab.ordinal) {
                CategoryKindTab.entries.forEach { tab ->
                    Tab(
                        selected = state.selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = { Text(tabLabel(tab)) },
                    )
                }
            }

            val visible = viewModel.visibleItems()
            if (visible.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Sin categorías de tipo ${tabLabel(state.selectedTab).lowercase()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items = visible, key = { it.id }) { item ->
                        CategoryRow(
                            category = item,
                            onClick = { onEdit(item.id) },
                            onDelete = { pendingDelete = item },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    pendingDelete?.let { category ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Borrar categoría") },
            text = { Text("¿Seguro que quieres borrar \"${category.name}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(category.id)
                    pendingDelete = null
                }) { Text("Borrar") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun CategoryRow(
    category: CategoryResponse,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(category.name) },
        supportingContent = {
            val parts = buildList {
                category.nature?.let { add(it.lowercase().replaceFirstChar { c -> c.uppercase() }) }
                category.essentiality?.let { add(it.lowercase().replaceFirstChar { c -> c.uppercase() }) }
                category.engelGroup?.let { add(it.lowercase().replaceFirstChar { c -> c.uppercase() }) }
                if (category.productive) add("Productive")
            }
            if (parts.isNotEmpty()) Text(parts.joinToString(" · "))
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(parseHex(category.color), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = iconFor(category.icon),
                    contentDescription = null,
                    tint = Color.White,
                )
            }
        },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Borrar")
            }
        },
    )
}

private fun tabLabel(tab: CategoryKindTab): String = when (tab) {
    CategoryKindTab.EXPENSE -> "Gastos"
    CategoryKindTab.INCOME -> "Ingresos"
    CategoryKindTab.TRANSFER -> "Transferencias"
}

private fun parseHex(hex: String): Color {
    val argb = "FF" + hex.removePrefix("#").uppercase()
    return Color(argb.toLong(16))
}
