package within.means.android.ui.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import within.means.android.ui.format.formatMoney
import within.means.analytics.application.CategoryBreakdownItem
import within.means.analytics.application.MonthlyEvolutionResponse
import within.means.analytics.application.MonthlySummaryResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen() {
    val viewModel: StatsViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Estadísticas · ${state.yearMonth}") })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = state.tab.ordinal) {
                StatsTab.entries.forEach { tab ->
                    Tab(
                        selected = state.tab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = { Text(tabLabel(tab)) },
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                when (state.tab) {
                    StatsTab.SUMMARY -> SummaryPanel(state.summary)
                    StatsTab.BREAKDOWN -> BreakdownPanel(state.breakdown?.items.orEmpty())
                    StatsTab.EVOLUTION -> EvolutionPanel(state.evolution)
                }
            }
        }
    }
}

@Composable
private fun SummaryPanel(summary: MonthlySummaryResponse?) {
    if (summary == null) {
        Text("Sin datos para este mes")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TotalsCard("Ingresos", summary.totalIncomeCents, MaterialTheme.colorScheme.primary)
        TotalsCard("Gastos", summary.totalExpenseCents, MaterialTheme.colorScheme.error)
        TotalsCard(
            label = "Saldo",
            cents = summary.balanceCents,
            color = if (summary.balanceCents >= 0)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.error,
        )

        HorizontalDivider()
        Text("Gastos por naturaleza", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniCard("Fijos", summary.fixedExpenseCents, Modifier.weight(1f))
            MiniCard("Variables", summary.variableExpenseCents, Modifier.weight(1f))
        }
        Text("Gastos por esencialidad", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniCard("Esenciales", summary.essentialExpenseCents, Modifier.weight(1f))
            MiniCard("Discrecionales", summary.discretionaryExpenseCents, Modifier.weight(1f))
        }
    }
}

@Composable
private fun TotalsCard(label: String, cents: Long, color: Color) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(
                formatMoney(cents),
                style = MaterialTheme.typography.headlineMedium,
                color = color,
            )
        }
    }
}

@Composable
private fun MiniCard(label: String, cents: Long, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(formatMoney(cents), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun BreakdownPanel(items: List<CategoryBreakdownItem>) {
    if (items.isEmpty()) {
        Text("Sin gastos en el mes seleccionado")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(parseHex(item.color), CircleShape),
                )
                Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(item.categoryName, modifier = Modifier.weight(1f))
                        Text(formatMoney(item.totalCents))
                    }
                    // Simple proportional bar.
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                    ) {
                        val w = size.width * item.share.toFloat()
                        drawRect(color = Color.LightGray, size = Size(size.width, size.height))
                        drawRect(color = parseHex(item.color), size = Size(w, size.height))
                    }
                }
            }
        }
    }
}

@Composable
private fun EvolutionPanel(evolution: MonthlyEvolutionResponse?) {
    val points = evolution?.points.orEmpty()
    if (points.isEmpty()) {
        Text("Sin datos suficientes para la evolución")
        return
    }

    val maxValue = (points.maxOfOrNull {
        maxOf(it.totalIncomeCents, it.totalExpenseCents)
    } ?: 1L).coerceAtLeast(1L)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Evolución de los últimos ${points.size} meses", style = MaterialTheme.typography.titleMedium)

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
        ) {
            val w = size.width
            val h = size.height
            val stepX = w / (points.size - 1).coerceAtLeast(1).toFloat()

            val incomePath = Path().apply {
                points.forEachIndexed { i, p ->
                    val x = i * stepX
                    val y = h - (p.totalIncomeCents.toFloat() / maxValue * h)
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
            }
            val expensePath = Path().apply {
                points.forEachIndexed { i, p ->
                    val x = i * stepX
                    val y = h - (p.totalExpenseCents.toFloat() / maxValue * h)
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
            }

            drawPath(incomePath, color = Color(0xFF2E7D32), style = Stroke(width = 4f))
            drawPath(expensePath, color = Color(0xFFC62828), style = Stroke(width = 4f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendDot(Color(0xFF2E7D32), "Ingresos")
            LegendDot(Color(0xFFC62828), "Gastos")
        }

        // X-axis labels.
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            points.forEach { Text(it.yearMonth.takeLast(2), style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
        Text(" $label", style = MaterialTheme.typography.bodySmall)
    }
}

private fun tabLabel(tab: StatsTab): String = when (tab) {
    StatsTab.SUMMARY -> "Resumen"
    StatsTab.BREAKDOWN -> "Por categoría"
    StatsTab.EVOLUTION -> "Evolución"
}

private fun parseHex(hex: String): Color {
    val argb = "FF" + hex.removePrefix("#").uppercase()
    return Color(argb.toLong(16))
}
