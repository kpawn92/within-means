package within.means.android.ui.quickadd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import within.means.android.ui.calculator.CalcKeypad
import within.means.android.ui.components.WmChip
import within.means.android.ui.components.WmPrimaryButton
import within.means.android.ui.components.WmSegmented
import within.means.android.ui.format.currencySymbol
import within.means.android.ui.format.formatAmount
import within.means.android.ui.format.formatMoney
import within.means.android.ui.theme.WmTheme

private val typeOptions = listOf("EXPENSE" to "Gasto", "INCOME" to "Ingreso", "TRANSFER" to "Ahorro")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickAddSheet(
    onDismiss: () -> Unit,
    onSaved: (amountCents: Long) -> Unit,
) {
    val viewModel: QuickAddViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) { viewModel.reset() }

    LaunchedEffect(state.savedAmountCents) {
        state.savedAmountCents?.let { onSaved(it); onDismiss() }
    }

    val sym = currencySymbol(state.baseCurrency)
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val amountColor = when (state.type) {
        "INCOME" -> WmTheme.colors.income
        "TRANSFER" -> WmTheme.colors.savings
        else -> WmTheme.colors.expense
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            WmSegmented(typeOptions, state.type, viewModel::onTypeChanged)

            // Expression line + giant display. Like the editor's calculator:
            // show the raw number as you type (native feel), and switch to the
            // live formatted result only once an operator is in play.
            val hasOperator = state.expression.any { it in "+-*/" }
            Column(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (hasOperator) {
                    Text(
                        displayExpression(state.expression),
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                Text(
                    if (hasOperator) "$sym${formatMoney(state.amountCents)}"
                    else "$sym${state.expression.ifEmpty { "0" }}",
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (state.expression.isEmpty()) amountColor.copy(alpha = 0.35f) else amountColor,
                    maxLines = 1,
                )
            }

            // Concepts: "en qué fue". The category is inferred from these, so it
            // sits below as an optional override.
            if (state.conceptsApply) {
                if (state.selectedConcepts.isNotEmpty()) {
                    Text(
                        "en ${state.selectedConcepts.joinToString(" · ")}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                val unpicked = state.conceptSuggestions
                    .map { it.label }
                    .filterNot { s -> state.selectedConcepts.any { it.equals(s, ignoreCase = true) } }
                if (state.selectedConcepts.isNotEmpty() || unpicked.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.selectedConcepts.forEach { label ->
                            WmChip(label, true, { viewModel.onRemoveConcept(label) })
                        }
                        unpicked.forEach { label ->
                            WmChip(label, false, { viewModel.onToggleConcept(label) })
                        }
                    }
                }
                ConceptInputField(
                    value = state.conceptInput,
                    onValueChange = viewModel::onConceptInputChanged,
                    onCommit = viewModel::onCommitTypedConcept,
                )
            }

            // Category override (optional — inferred from the concept otherwise).
            if (state.availableCategories.isEmpty()) {
                Text(
                    "No hay categorías de este tipo. Crea una primero.",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.availableCategories.forEach { c ->
                        WmChip(c.name, state.categoryId == c.id, { viewModel.onCategoryChanged(c.id) })
                    }
                }
            }

            // Extras: note + when (date) + time.
            NoteField(state.note, viewModel::onNoteChanged)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WmChip("Hoy", state.whenChoice == QuickWhen.TODAY, { viewModel.onWhenChanged(QuickWhen.TODAY) })
                WmChip("Ayer", state.whenChoice == QuickWhen.YESTERDAY, { viewModel.onWhenChanged(QuickWhen.YESTERDAY) })
                WmChip("📅 ${prettyDate(state.date)}", state.whenChoice == null, { showDatePicker = true })
                WmChip("🕐 ${state.time.ifBlank { "--:--" }}", false, { showTimePicker = true })
            }

            CalcKeypad(
                onDigit = viewModel::onDigit,
                onDot = viewModel::onDot,
                onOperator = viewModel::onOperator,
                onBackspace = viewModel::onBackspace,
            )

            // Validation feedback (mirrors the full editor): the button stays
            // tappable and tells the user what's missing instead of going dead.
            state.errorMessage?.let { msg ->
                Text(
                    msg,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            WmPrimaryButton(
                text = if (state.saving) "Guardando…" else "Guardar ${formatAmount(state.amountCents, sym)}",
                onClick = viewModel::save,
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = remember(state.date) { isoToUtcMillis(state.date) },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { viewModel.onDateChanged(utcMillisToIso(it)) }
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } },
        ) { DatePicker(state = pickerState) }
    }

    if (showTimePicker) {
        val (h, m) = remember(state.time) { parseHourMinute(state.time) }
        val tpState = rememberTimePickerState(initialHour = h, initialMinute = m, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val hh = tpState.hour.toString().padStart(2, '0')
                    val mm = tpState.minute.toString().padStart(2, '0')
                    viewModel.onTimeChanged("$hh:$mm")
                    showTimePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancelar") } },
            text = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = tpState)
                }
            },
        )
    }
}

/** "2026-06-11" → "11 jun"; falls back to the raw string. */
private fun prettyDate(iso: String): String {
    val parts = iso.split("-")
    if (parts.size != 3) return iso
    val months = listOf("ene", "feb", "mar", "abr", "may", "jun",
        "jul", "ago", "sep", "oct", "nov", "dic")
    val m = parts[1].toIntOrNull() ?: return iso
    val d = parts[2].toIntOrNull() ?: return iso
    return "$d ${months.getOrElse(m - 1) { parts[1] }}"
}

/** Parses `HH:mm` into (hour, minute); noon fallback for blank/invalid. */
private fun parseHourMinute(value: String): Pair<Int, Int> {
    val parts = value.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 12
    val m = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
    return h to m
}

private const val MILLIS_PER_DAY = 86_400_000L

/** ISO `yyyy-MM-dd` → UTC midnight millis (for the M3 date picker). */
private fun isoToUtcMillis(iso: String): Long {
    val d = runCatching { kotlinx.datetime.LocalDate.parse(iso) }.getOrNull()
        ?: return 0L
    return d.toEpochDays().toLong() * MILLIS_PER_DAY
}

/** UTC millis from the picker → ISO `yyyy-MM-dd`. */
private fun utcMillisToIso(millis: Long): String =
    kotlinx.datetime.LocalDate.fromEpochDays((millis / MILLIS_PER_DAY).toInt()).toString()

/** Maps a canonical expression to math glyphs for display (`* / -` → `× ÷ −`). */
private fun displayExpression(expr: String): String =
    expr.replace('*', '×').replace('/', '÷').replace('-', '−')

@Composable
private fun ConceptInputField(
    value: String,
    onValueChange: (String) -> Unit,
    onCommit: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        if (value.isEmpty()) {
            Text("¿En qué fue?  (cerveza, papá, bus…)", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onCommit() }),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun NoteField(value: String, onValueChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        if (value.isEmpty()) {
            Text("Nota (opcional)", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

