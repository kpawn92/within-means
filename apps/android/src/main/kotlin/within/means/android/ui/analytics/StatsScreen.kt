package within.means.android.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import within.means.android.ui.components.DonutSegment
import within.means.android.ui.components.WmBar
import within.means.android.ui.components.WmCard
import within.means.android.ui.components.WmDonut
import within.means.android.ui.components.WmEyebrow
import within.means.android.ui.components.WmSegmented
import within.means.android.ui.format.currencySymbol
import within.means.android.ui.format.formatAmount
import within.means.android.ui.theme.WmTheme
import within.means.android.ui.theme.categoryColor
import within.means.analytics.application.MonthlyEvolutionResponse
import within.means.analytics.application.MonthlySummaryResponse

private val MONTHS_LONG = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
)

private fun monthTitle(yearMonth: String?): String {
    val m = yearMonth?.substringAfter('-')?.toIntOrNull() ?: return ""
    return MONTHS_LONG.getOrNull(m - 1)?.replaceFirstChar { it.uppercase() } ?: ""
}

@Composable
fun StatsScreen() {
    val viewModel: StatsViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var lens by remember { mutableStateOf("categoria") }
    val sym = currencySymbol(state.baseCurrency)

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(Modifier.padding(end = 44.dp)) {
                    WmEyebrow(state.periodLabel.ifBlank { monthTitle(state.yearMonth) })
                    Text("Análisis", fontSize = 24.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface)
                }
            }

            item {
                WmSegmented(
                    options = listOf(
                        StatsPeriod.WEEK.name to "Semana",
                        StatsPeriod.MONTH.name to "Mes",
                        StatsPeriod.YEAR.name to "Año",
                    ),
                    selected = state.period.name,
                    onSelect = { viewModel.selectPeriod(StatsPeriod.valueOf(it)) },
                )
            }

            val summary = state.summary
            if (summary == null) {
                item { Text("Sin datos en este periodo", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                item { SummaryTrio(summary, sym) }
                item { SavingsRateCard(summary, state.previousSummary, state.comparisonLabel, sym) }
                item { EvolutionCard(state.evolution, sym) }
                item { BreakdownCard(state, lens, { lens = it }, sym) }
            }
        }
    }
}

@Composable
private fun SummaryTrio(summary: MonthlySummaryResponse, sym: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCell("Ingresos", summary.totalIncomeCents, WmTheme.colors.income, sym, Modifier.weight(1f))
        StatCell("Gastos", summary.totalExpenseCents, WmTheme.colors.expense, sym, Modifier.weight(1f))
        StatCell("Ahorro", summary.balanceCents, WmTheme.colors.savings, sym, Modifier.weight(1f))
    }
}

@Composable
private fun StatCell(label: String, cents: Long, accent: Color, sym: String, modifier: Modifier = Modifier) {
    WmCard(modifier = modifier, contentPadding = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(accent))
            Text(formatAmount(cents, sym, decimals = false), fontSize = 15.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Savings rate = balance / income, clamped to [0,1]. Returns null when no income. */
private fun savingsRate(summary: MonthlySummaryResponse?): Float? {
    val income = summary?.totalIncomeCents ?: return null
    if (income <= 0L) return null
    return (summary.balanceCents.toFloat() / income).coerceIn(0f, 1f)
}

@Composable
private fun SavingsRateCard(
    summary: MonthlySummaryResponse,
    previous: MonthlySummaryResponse?,
    comparisonLabel: String,
    sym: String,
) {
    val income = summary.totalIncomeCents
    val saved = summary.balanceCents
    val pct = if (income > 0) (saved.toFloat() / income).coerceIn(0f, 1f) else 0f
    val pctInt = (pct * 100).toInt()
    val prevRate = savingsRate(previous)
    WmCard(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primaryContainer) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            WmDonut(
                segments = listOf(
                    DonutSegment(pct, MaterialTheme.colorScheme.primary),
                    DonutSegment(1f - pct, MaterialTheme.colorScheme.surfaceContainerHigh),
                ),
                size = 68.dp, thickness = 10.dp, gapDegrees = 0f,
            ) {
                Text("$pctInt%", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(if (pctInt >= 0) "Tasa de ahorro" else "Periodo en números rojos",
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(
                    if (saved >= 0) "Guardaste ${formatAmount(saved, sym, decimals = false)} de lo que entró."
                    else "Gastaste ${formatAmount(-saved, sym, decimals = false)} más de lo que entró.",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
                // Real previous-period comparison (replaces the mock phrase).
                if (prevRate != null) {
                    val deltaPts = pctInt - (prevRate * 100).toInt()
                    val (text, color) = when {
                        deltaPts > 0 -> "▲ $deltaPts pts $comparisonLabel" to WmTheme.colors.pos
                        deltaPts < 0 -> "▼ ${-deltaPts} pts $comparisonLabel" to WmTheme.colors.neg
                        else -> "Igual $comparisonLabel" to MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    }
                    Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = color)
                }
            }
        }
    }
}

@Composable
private fun EvolutionCard(evolution: MonthlyEvolutionResponse?, sym: String) {
    val points = evolution?.points.orEmpty()
    if (points.isEmpty()) return
    val max = points.maxOf { it.totalExpenseCents }.coerceAtLeast(1L)
    val avg = points.map { it.totalExpenseCents }.average().toLong()
    WmCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Evolución del gasto", fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface)
                Text("media ${formatAmount(avg, sym, decimals = false)}", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(modifier = Modifier.fillMaxWidth().height(130.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                points.forEachIndexed { i, p ->
                    val frac = (p.totalExpenseCents.toFloat() / max).coerceIn(0.02f, 1f)
                    val highlight = i == points.lastIndex
                    Column(Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .width(26.dp)
                                .fillMaxHeight(frac)
                                .clip(RoundedCornerShape(7.dp))
                                .background(if (highlight) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceContainerHigh),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(p.label.ifBlank { p.yearMonth.takeLast(2) }, fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
            }
        }
    }
}

private data class BreakdownRow(val name: String, val cents: Long, val color: Color)

@Composable
private fun BreakdownCard(state: StatsUiState, lens: String, onLens: (String) -> Unit, sym: String) {
    val summary = state.summary ?: return
    val rows: List<BreakdownRow> = if (lens == "categoria") {
        state.breakdown?.items.orEmpty()
            .sortedByDescending { it.totalCents }
            .map { BreakdownRow(it.categoryName, it.totalCents, categoryColor(it.color)) }
    } else {
        listOf(
            BreakdownRow("Esencial", summary.essentialExpenseCents, WmTheme.colors.pos),
            BreakdownRow("Discrecional", summary.discretionaryExpenseCents, WmTheme.colors.savings),
        )
    }
    val max = (rows.maxOfOrNull { it.cents } ?: 1L).coerceAtLeast(1L)
    WmCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Desglose", fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface)
            WmSegmented(
                options = listOf("categoria" to "Categoría", "tipo" to "Tipo"),
                selected = lens, onSelect = onLens,
            )
            if (rows.isEmpty()) {
                Text("Sin gastos en el mes", fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                rows.forEach { r ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(r.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatAmount(r.cents, sym, decimals = false), fontSize = 13.sp,
                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        WmBar(fraction = r.cents.toFloat() / max, color = r.color, height = 8.dp)
                    }
                }
            }
        }
    }
}
