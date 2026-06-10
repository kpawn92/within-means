package within.means.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import within.means.android.ui.CategoryView
import within.means.android.ui.components.CatIcon
import within.means.android.ui.components.DonutSegment
import within.means.android.ui.components.WmBar
import within.means.android.ui.components.WmCard
import within.means.android.ui.components.WmDonut
import within.means.android.ui.components.WmEyebrow
import within.means.android.ui.categories.iconFor
import within.means.android.ui.format.currencySymbol
import within.means.android.ui.format.formatAmount
import within.means.android.ui.motion.countUpCents
import within.means.android.ui.motion.enterUp
import within.means.android.ui.motion.rememberReducedMotion
import within.means.android.ui.theme.WmTheme
import within.means.android.ui.theme.categoryColor
import within.means.transactions.application.TransactionResponse

private val MONTHS_LONG = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
)

/** "2026-06" → "Junio". */
private fun monthTitle(yearMonth: String?): String {
    val m = yearMonth?.substringAfter('-')?.toIntOrNull() ?: return ""
    return MONTHS_LONG.getOrNull(m - 1)?.replaceFirstChar { it.uppercase() } ?: ""
}

private const val MASK = "••••"

/** Formats an amount, or masks it with dots when [masked] (hide-amounts pref). */
private fun money(
    cents: Long,
    sym: String,
    masked: Boolean,
    signed: Boolean = false,
    decimals: Boolean = false,
): String = if (masked) MASK else formatAmount(cents, sym, signed = signed, decimals = decimals)

@Composable
fun HomeScreen(
    onNewTransaction: () -> Unit,
    onOpenAllTransactions: () -> Unit,
    onOpenTransaction: (id: String) -> Unit,
) {
    val viewModel: HomeViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val reduced = rememberReducedMotion()
    var revealed by remember { mutableStateOf(false) }
    val masked = state.hideAmounts && !revealed

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Box(Modifier.enterUp(0, reduced)) {
                    GreetingHeader(state.displayName, state.summary?.yearMonth)
                }
            }

            if (state.loading && state.summary == null) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            }

            item {
                Box(Modifier.enterUp(1, reduced)) {
                    MonthHero(
                        state = state,
                        reduced = reduced,
                        masked = masked,
                        canReveal = state.hideAmounts,
                        revealed = revealed,
                        onToggleReveal = { revealed = !revealed },
                    )
                }
            }

            if ((state.breakdown?.items?.isNotEmpty() == true)) {
                item { Box(Modifier.enterUp(2, reduced)) { SpendingDonutCard(state, masked) } }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Reciente", fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text("Ver todo", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(onClick = onOpenAllTransactions))
                }
            }

            if (state.recentTransactions.isEmpty()) {
                item { EmptyTransactionsHint() }
            } else {
                item {
                    WmCard(contentPadding = 6.dp) {
                        Column {
                            state.recentTransactions.forEachIndexed { i, tx ->
                                TransactionRow(
                                    transaction = tx,
                                    category = state.categories[tx.categoryId],
                                    currency = state.baseCurrency,
                                    masked = masked,
                                    onClick = { onOpenTransaction(tx.id) },
                                )
                                if (i < state.recentTransactions.lastIndex) {
                                    Spacer(Modifier.height(2.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GreetingHeader(name: String, yearMonth: String?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(end = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            WmEyebrow(listOfNotNull(monthTitle(yearMonth).takeIf { it.isNotBlank() }, name).joinToString(" · "))
            Text("Hola, $name 👋", fontSize = 24.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun MonthHero(
    state: HomeUiState,
    reduced: Boolean,
    masked: Boolean,
    canReveal: Boolean,
    revealed: Boolean,
    onToggleReveal: () -> Unit,
) {
    val budget = state.budget
    if (budget != null) {
        BudgetHero(budget, currencySymbol(state.baseCurrency), reduced, masked, canReveal, revealed, onToggleReveal)
    } else {
        BalanceHero(state, reduced, masked, canReveal, revealed, onToggleReveal)
    }
}

@Composable
private fun RevealToggle(revealed: Boolean, tint: Color, onToggle: () -> Unit) {
    IconButton(onClick = onToggle, modifier = Modifier.size(28.dp)) {
        Icon(
            if (revealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
            contentDescription = if (revealed) "Ocultar importes" else "Mostrar importes",
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun BudgetHero(
    budget: BudgetView,
    sym: String,
    reduced: Boolean,
    masked: Boolean,
    canReveal: Boolean,
    revealed: Boolean,
    onToggleReveal: () -> Unit,
) {
    val onBrand = MaterialTheme.colorScheme.onPrimaryContainer
    val fraction = if (budget.planCents > 0) budget.spentCents.toFloat() / budget.planCents else 0f
    val available = countUpCents(budget.availableCents, reduced)
    WmCard(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentPadding = 22.dp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WmEyebrow("Disponible", color = onBrand.copy(alpha = 0.7f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BudgetBadge(budget.withinPlan)
                    if (canReveal) {
                        Spacer(Modifier.size(4.dp))
                        RevealToggle(revealed, onBrand.copy(alpha = 0.8f), onToggleReveal)
                    }
                }
            }
            Text(
                money(available, sym, masked, signed = false, decimals = false),
                fontSize = 34.sp, fontWeight = FontWeight.Bold, color = onBrand,
            )
            WmBar(
                fraction = fraction,
                color = if (budget.withinPlan) WmTheme.colors.pos else WmTheme.colors.neg,
                track = onBrand.copy(alpha = 0.18f),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Gastado ${money(budget.spentCents, sym, masked, decimals = false)}",
                    fontSize = 12.sp, color = onBrand.copy(alpha = 0.8f))
                Text("Plan ${money(budget.planCents, sym, masked, decimals = false)}",
                    fontSize = 12.sp, color = onBrand.copy(alpha = 0.8f))
            }
            if (budget.withinPlan && budget.perDayCents > 0) {
                Text(
                    "${money(budget.perDayCents, sym, masked, decimals = false)}/día · " +
                        "${budget.daysRemaining} días restantes",
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = onBrand,
                )
            }
        }
    }
}

@Composable
private fun BudgetBadge(withinPlan: Boolean) {
    val bg = if (withinPlan) WmTheme.colors.posSoft else WmTheme.colors.negSoft
    val fg = if (withinPlan) WmTheme.colors.pos else WmTheme.colors.neg
    Box(
        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(if (withinPlan) "Dentro del plan" else "Atención", fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold, color = fg)
    }
}

@Composable
private fun BalanceHero(
    state: HomeUiState,
    reduced: Boolean,
    masked: Boolean,
    canReveal: Boolean,
    revealed: Boolean,
    onToggleReveal: () -> Unit,
) {
    val sym = currencySymbol(state.baseCurrency)
    val summary = state.summary
    val balance = countUpCents(summary?.balanceCents ?: 0L, reduced)
    val onBrand = MaterialTheme.colorScheme.onPrimaryContainer
    WmCard(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentPadding = 22.dp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WmEyebrow("Balance del mes", color = onBrand.copy(alpha = 0.7f))
                if (canReveal) RevealToggle(revealed, onBrand.copy(alpha = 0.8f), onToggleReveal)
            }
            if (summary == null) {
                Text("Aún sin datos", color = onBrand)
            } else {
                Text(
                    money(balance, sym, masked, signed = true, decimals = false),
                    fontSize = 34.sp, fontWeight = FontWeight.Bold,
                    color = onBrand,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    HeroStat("Ingresos", summary.totalIncomeCents, WmTheme.colors.pos, sym, masked)
                    HeroStat("Gastos", summary.totalExpenseCents, WmTheme.colors.neg, sym, masked)
                }
            }
        }
    }
}

@Composable
private fun HeroStat(label: String, cents: Long, color: Color, sym: String, masked: Boolean) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
            Spacer(Modifier.size(6.dp))
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
        }
        Text(money(cents, sym, masked, decimals = false), fontSize = 16.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun SpendingDonutCard(state: HomeUiState, masked: Boolean) {
    val breakdown = state.breakdown ?: return
    val sym = currencySymbol(state.baseCurrency)
    val top = breakdown.items.sortedByDescending { it.totalCents }
    val segments = top.map { DonutSegment(it.totalCents.toFloat(), categoryColor(it.color)) }
    WmCard(modifier = Modifier.fillMaxWidth(), contentPadding = 18.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("En qué va el mes", fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface)
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                WmDonut(
                    segments = segments,
                    size = 116.dp,
                    thickness = 16.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(money(breakdown.totalCents, sym, masked, decimals = false),
                            fontSize = 15.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text("gastado", fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    top.take(4).forEach { item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp))
                                .background(categoryColor(item.color)))
                            Spacer(Modifier.size(8.dp))
                            Text(item.categoryName, fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f), maxLines = 1)
                            Text("${(item.share * 100).toInt()}%", fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(
    transaction: TransactionResponse,
    category: CategoryView?,
    currency: String,
    masked: Boolean,
    onClick: () -> Unit,
) {
    val income = transaction.type == "INCOME"
    val transfer = transaction.type == "TRANSFER"
    val sym = currencySymbol(currency)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CatIcon(
            icon = iconFor(category?.icon ?: ""),
            color = categoryColor(category?.color),
            boxSize = 38.dp, iconSize = 19.dp,
        )
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(transaction.description.ifBlank { category?.name ?: "Movimiento" },
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
            Text(
                listOfNotNull(category?.name, transaction.incomeSource).joinToString(" · "),
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        val amount = money(transaction.amountCents, sym, masked, signed = false, decimals = true)
        Text(
            when {
                income -> "+$amount"
                transfer -> "→$amount"
                else -> "−$amount"
            },
            fontSize = 15.sp, fontWeight = FontWeight.Bold,
            color = when {
                income -> WmTheme.colors.pos
                transfer -> WmTheme.colors.savings
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

@Composable
private fun EmptyTransactionsHint() {
    WmCard(modifier = Modifier.fillMaxWidth(), contentPadding = 28.dp) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                "Aún no hay movimientos. Pulsa + para registrar el primero.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
