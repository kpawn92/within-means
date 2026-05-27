package within.means.android.ui.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import org.koin.compose.viewmodel.koinViewModel
import within.means.android.ui.format.formatMoney
import within.means.transactions.application.TransactionResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsListScreen(
    onCreate: () -> Unit,
    onEdit: (transactionId: String) -> Unit,
) {
    val viewModel: TransactionsListViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDelete by remember { mutableStateOf<TransactionResponse?>(null) }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Transacciones") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreate) {
                Icon(Icons.Filled.Add, contentDescription = "Registrar transacción")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = state.typeFilter.ordinal) {
                TransactionTypeFilter.entries.forEach { tab ->
                    Tab(
                        selected = state.typeFilter == tab,
                        onClick = { viewModel.selectTypeFilter(tab) },
                        text = { Text(filterLabel(tab)) },
                    )
                }
            }

            val visible = viewModel.visibleItems()
            if (visible.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Sin transacciones",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items = visible, key = { it.id }) { item ->
                        TransactionRow(
                            transaction = item,
                            categoryName = state.categoryNames[item.categoryId],
                            onClick = { onEdit(item.id) },
                            onDelete = { pendingDelete = item },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    pendingDelete?.let { tx ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Borrar transacción") },
            text = { Text("¿Seguro que quieres borrar este movimiento?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(tx.id)
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
private fun TransactionRow(
    transaction: TransactionResponse,
    categoryName: String?,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = {
            val prefix = if (transaction.type == "EXPENSE") "−" else "+"
            Text("$prefix ${formatMoney(transaction.amountCents)}")
        },
        supportingContent = {
            val parts = buildList {
                add(transaction.date)
                categoryName?.let { add(it) }
                if (transaction.description.isNotBlank()) add(transaction.description)
            }
            Text(parts.joinToString(" · "))
        },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Borrar")
            }
        },
    )
}

private fun filterLabel(filter: TransactionTypeFilter): String = when (filter) {
    TransactionTypeFilter.ALL -> "Todas"
    TransactionTypeFilter.INCOME -> "Ingresos"
    TransactionTypeFilter.EXPENSE -> "Gastos"
}

