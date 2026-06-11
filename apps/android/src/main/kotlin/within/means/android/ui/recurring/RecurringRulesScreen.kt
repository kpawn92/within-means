package within.means.android.ui.recurring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import within.means.android.ui.components.WmCard
import within.means.android.ui.components.WmGhostButton
import within.means.android.ui.format.currencySymbol
import within.means.android.ui.format.formatAmount
import within.means.android.ui.theme.WmTheme

@Composable
fun RecurringRulesScreen(onBack: () -> Unit, onEdit: (id: String) -> Unit = {}) {
    val viewModel: RecurringRulesViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 20.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.size(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver",
                        tint = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(Modifier.size(4.dp))
                Text("Recurrentes", fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface)
            }

            if (state.loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

            if (!state.loading && state.rules.isEmpty()) {
                WmCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Sin recurrentes activos", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text("Marca «Recurrente» al crear un movimiento para que se repita cada semana o mes.",
                            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            val symbol = currencySymbol(state.baseCurrency)
            state.rules.forEach { rule ->
                RecurringRuleCard(
                    rule, symbol,
                    onEdit = { onEdit(rule.id) },
                    onDeactivate = { viewModel.deactivate(rule.id) },
                )
            }

            Spacer(Modifier.size(8.dp))
        }
    }
}

@Composable
private fun RecurringRuleCard(
    rule: RecurringRuleRow,
    symbol: String,
    onEdit: () -> Unit,
    onDeactivate: () -> Unit,
) {
    val accent = when (rule.type) {
        "INCOME" -> WmTheme.colors.income
        "TRANSFER" -> WmTheme.colors.savings
        else -> WmTheme.colors.expense
    }
    val prefix = when (rule.type) {
        "INCOME" -> "+"
        "TRANSFER" -> "→"
        else -> "−"
    }
    val cadence = if (rule.frequency == "WEEKLY") "Semanal" else "Mensual"

    WmCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        rule.description.ifBlank { rule.categoryName },
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface, maxLines = 1,
                    )
                    Text("$cadence · ${rule.categoryName}", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                Text(
                    "$prefix${formatAmount(rule.amountCents, symbol = symbol, decimals = false)}",
                    fontSize = 17.sp, fontWeight = FontWeight.Bold, color = accent,
                )
            }
            Text("Próximo: ${rule.nextOccurrence}", fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WmGhostButton("Editar", onClick = onEdit, modifier = Modifier.weight(1f))
                WmGhostButton("Desactivar", onClick = onDeactivate, modifier = Modifier.weight(1f))
            }
        }
    }
}
