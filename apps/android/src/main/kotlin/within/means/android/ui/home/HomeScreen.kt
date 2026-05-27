package within.means.android.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import within.means.android.ui.format.formatMoney
import within.means.transactions.application.TransactionResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNewTransaction: () -> Unit,
    onOpenAllTransactions: () -> Unit,
    onOpenTransaction: (id: String) -> Unit,
) {
    val viewModel: HomeViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Hola, ${state.displayName}") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewTransaction) {
                Icon(Icons.Filled.Add, contentDescription = "Registrar transacción")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.loading && state.summary == null) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            MonthlySummaryCard(state)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Últimos movimientos", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Ver todo",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onOpenAllTransactions),
                )
            }

            if (state.recentTransactions.isEmpty()) {
                EmptyTransactionsHint()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    items(items = state.recentTransactions, key = { it.id }) { tx ->
                        TransactionPreviewRow(
                            transaction = tx,
                            categoryName = state.categoryNames[tx.categoryId],
                            onClick = { onOpenTransaction(tx.id) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlySummaryCard(state: HomeUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Resumen del mes", style = MaterialTheme.typography.titleMedium)
            val summary = state.summary
            if (summary == null) {
                Text("Aún sin datos", style = MaterialTheme.typography.bodyMedium)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    SummaryAmount("Ingresos", summary.totalIncomeCents, MaterialTheme.colorScheme.primary)
                    SummaryAmount("Gastos", summary.totalExpenseCents, MaterialTheme.colorScheme.error)
                    SummaryAmount(
                        label = "Saldo",
                        cents = summary.balanceCents,
                        color = if (summary.balanceCents >= 0)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error,
                    )
                }
                Text(
                    "Moneda base: ${state.baseCurrency}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SummaryAmount(label: String, cents: Long, color: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(formatMoney(cents), style = MaterialTheme.typography.titleLarge, color = color)
    }
}

@Composable
private fun TransactionPreviewRow(
    transaction: TransactionResponse,
    categoryName: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(categoryName ?: "(sin categoría)", style = MaterialTheme.typography.bodyLarge)
            Text(
                transaction.date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val prefix = if (transaction.type == "EXPENSE") "−" else "+"
        Text(
            "$prefix ${formatMoney(transaction.amountCents)}",
            style = MaterialTheme.typography.titleMedium,
            color = if (transaction.type == "EXPENSE")
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun EmptyTransactionsHint() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "Aún no hay movimientos. Pulsa + para registrar el primero.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

