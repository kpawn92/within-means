package within.means.android.ui.transactions

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import within.means.android.ui.CategoryView
import within.means.android.ui.categories.iconFor
import within.means.android.ui.components.CatIcon
import within.means.android.ui.components.WmCard
import within.means.android.ui.components.WmChip
import within.means.android.ui.components.WmEyebrow
import within.means.android.ui.format.currencySymbol
import within.means.android.ui.format.formatAmount
import within.means.android.ui.format.relativeDayLabel
import within.means.android.ui.theme.WmTheme
import within.means.android.ui.theme.categoryColor
import within.means.transactions.application.TransactionResponse

private data class DayGroup(val label: String, val items: List<TransactionResponse>, val totalCents: Long)

private fun signedCents(tx: TransactionResponse): Long = when (tx.type) {
    "INCOME" -> tx.amountCents
    "EXPENSE" -> -tx.amountCents
    else -> 0L // TRANSFER moves money between buckets — neutral to the day's net.
}

@Composable
fun TransactionsListScreen(
    onCreate: () -> Unit,
    onEdit: (transactionId: String) -> Unit,
) {
    val viewModel: TransactionsListViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val visible = viewModel.visibleItems()
    val monthExpense = state.items.filter { it.type == "EXPENSE" }.sumOf { it.amountCents }
    val sym = currencySymbol(state.baseCurrency)

    // group by relative day label, preserving date order
    val groups = remember(visible) {
        visible.groupBy { relativeDayLabel(it.date) }
            .map { (label, items) -> DayGroup(label, items, items.sumOf(::signedCents)) }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // header
            Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(end = 44.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Movimientos", fontSize = 24.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(formatAmount(monthExpense, sym, decimals = false),
                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        IconButton(onClick = onCreate) {
                            Icon(Icons.Filled.Add, contentDescription = "Nuevo movimiento",
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                SearchField(state.query, viewModel::setQuery)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    chipFilters.forEach { (filter, label) ->
                        WmChip(label, state.typeFilter == filter, { viewModel.selectTypeFilter(filter) })
                    }
                }
            }

            if (groups.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Sin resultados", fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    state = rememberLazyListState(),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    items(groups, key = { it.label }) { group ->
                        Column {
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                WmEyebrow(group.label)
                                Text(formatAmount(group.totalCents, sym, signed = true, decimals = false),
                                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                    color = if (group.totalCents >= 0) WmTheme.colors.income
                                        else WmTheme.colors.expense)
                            }
                            WmCard(contentPadding = 6.dp) {
                                Column {
                                    group.items.forEachIndexed { i, tx ->
                                        TransactionRow(tx, state.categories[tx.categoryId], sym) { onEdit(tx.id) }
                                        if (i < group.items.lastIndex) Spacer(Modifier.height(2.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private val chipFilters = listOf(
    TransactionTypeFilter.ALL to "Todos",
    TransactionTypeFilter.EXPENSE to "Gastos",
    TransactionTypeFilter.INCOME to "Ingresos",
    TransactionTypeFilter.TRANSFER to "Ahorro",
)

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(10.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text("Buscar movimiento…", fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun TransactionRow(
    transaction: TransactionResponse,
    category: CategoryView?,
    sym: String,
    onClick: () -> Unit,
) {
    val income = transaction.type == "INCOME"
    val transfer = transaction.type == "TRANSFER"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CatIcon(iconFor(category?.icon ?: ""), categoryColor(category?.color),
            boxSize = 38.dp, iconSize = 19.dp)
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(transaction.description.ifBlank { category?.name ?: "Movimiento" },
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
            Text(listOfNotNull(category?.name, transaction.incomeSource).joinToString(" · "),
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        val amount = formatAmount(transaction.amountCents, sym)
        Text(
            when {
                income -> "+$amount"
                transfer -> "→$amount"
                else -> "−$amount"
            },
            fontSize = 15.sp, fontWeight = FontWeight.Bold,
            color = when {
                income -> WmTheme.colors.income
                transfer -> WmTheme.colors.savings
                else -> WmTheme.colors.expense
            },
        )
    }
}
