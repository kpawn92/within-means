package within.means.android.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import within.means.android.ui.calculator.CalculatorSheet
import within.means.android.ui.categories.iconFor
import within.means.android.ui.components.CatIcon
import within.means.android.ui.components.WmPrimaryButton
import within.means.android.ui.components.WmSegmented
import within.means.android.ui.components.WmToggle
import within.means.android.ui.format.currencySymbol
import within.means.android.ui.theme.WmRadii
import within.means.android.ui.theme.WmTheme
import within.means.android.ui.theme.categoryColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEditScreen(
    transactionId: String?,
    onBack: () -> Unit,
    onFinished: () -> Unit,
) {
    val viewModel: TransactionEditViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showCalculator by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(transactionId) {
        if (transactionId != null && state.transactionId != transactionId) {
            viewModel.loadExisting(transactionId)
        }
    }

    LaunchedEffect(state.isFinished) {
        if (state.isFinished) onFinished()
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    val sym = currencySymbol(state.baseCurrency)
    val selectedCategory = state.availableCategories.firstOrNull { it.id == state.categoryId }
    val accent = when {
        selectedCategory != null -> categoryColor(selectedCategory.color)
        state.type == "INCOME" -> WmTheme.colors.income
        state.type == "TRANSFER" -> WmTheme.colors.savings
        else -> WmTheme.colors.expense
    }
    val busy = state.saving || state.deleting

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isEditMode) "Editar movimiento" else "Nuevo movimiento") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                    }
                },
                actions = {
                    if (viewModel.isEditMode) {
                        IconButton(onClick = {
                            viewModel.duplicate()
                            scope.launch {
                                snackbarHostState.showSnackbar("Copia lista: revisa y pulsa Añadir")
                            }
                        }) {
                            Icon(
                                Icons.Outlined.ContentCopy,
                                contentDescription = "Duplicar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Outlined.DeleteOutline,
                                contentDescription = "Borrar",
                                tint = WmTheme.colors.neg,
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                WmPrimaryButton(
                    text = when {
                        state.saving -> "Guardando…"
                        viewModel.isEditMode -> "Guardar"
                        else -> "Añadir"
                    },
                    onClick = viewModel::save,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 20.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Spacer(Modifier.size(4.dp))

            // Type is fixed once a movement exists (you can't turn an expense into
            // savings after the fact), so it's shown locked rather than fake-tappable.
            WmSegmented(
                options = typeOptions,
                selected = state.type,
                onSelect = viewModel::onTypeChanged,
                enabled = !viewModel.isEditMode,
            )
            if (viewModel.isEditMode) {
                Text(
                    "El tipo no se puede cambiar al editar.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Giant amount; tapping opens the calculator. Tinted by the chosen
            // category (or type fallback).
            AmountField(
                symbol = sym,
                cents = state.amountCents,
                isEmpty = state.amountText.isBlank(),
                color = accent,
                onClick = { showCalculator = true },
            )

            // "en ___" — what the movement was about (the chosen concepts).
            if (state.conceptsApply && state.selectedConcepts.isNotEmpty()) {
                Text(
                    "en ${state.selectedConcepts.joinToString(" · ")}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Concepts: tap a frequent one or type your own. The category is
            // inferred from these, so it isn't asked for up front.
            if (state.conceptsApply) {
                ConceptSection(
                    selected = state.selectedConcepts,
                    suggestions = state.conceptSuggestions,
                    input = state.conceptInput,
                    accent = accent,
                    onToggle = viewModel::onToggleConcept,
                    onRemove = viewModel::onRemoveConcept,
                    onInputChange = viewModel::onConceptInputChanged,
                    onCommit = viewModel::onCommitTypedConcept,
                )
            }

            // Everything advanced lives behind "Detalles": category override,
            // date/time, description, income source, recurring.
            DetailsHeader(open = state.showDetails, onToggle = viewModel::onToggleDetails)

            if (state.showDetails) {
                // Category override (optional — inferred from the concept otherwise).
                if (state.availableCategories.isEmpty()) {
                    Text(
                        "No hay categorías de tipo ${typeLabel(state.type)}. Crea una primero.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.availableCategories.forEach { c ->
                            CategoryChip(
                                name = c.name,
                                color = categoryColor(c.color),
                                icon = c.icon,
                                selected = state.categoryId == c.id,
                                onClick = { viewModel.onCategoryChanged(c.id) },
                            )
                        }
                    }
                }

                // Re-learn offer: re-point the concept's inferred category (§8.4).
                state.relearnPrompt?.let { p ->
                    RelearnPromptCard(
                        conceptLabel = p.conceptLabel,
                        categoryName = p.categoryName,
                        onAccept = viewModel::applyRelearn,
                        onDismiss = viewModel::dismissRelearn,
                    )
                }

                OutlinedTextField(
                    value = state.description,
                    onValueChange = viewModel::onDescriptionChanged,
                    label = { Text("Descripción (opcional)") },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (state.type == "INCOME") {
                    OutlinedTextField(
                        value = state.incomeSource,
                        onValueChange = viewModel::onIncomeSourceChanged,
                        label = { Text("Fuente del ingreso (opcional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DateField(
                        value = state.date,
                        onValueChange = viewModel::onDateChanged,
                        modifier = Modifier.weight(1f),
                    )
                    TimeField(
                        value = state.time,
                        onValueChange = viewModel::onTimeChanged,
                        modifier = Modifier.weight(1f),
                    )
                }

                if (!viewModel.isEditMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (state.activeRecurringCount > 0) {
                                "Recurrente · ${state.activeRecurringCount} activos"
                            } else {
                                "Recurrente"
                            },
                        )
                        WmToggle(state.recurring, viewModel::onRecurringChanged)
                    }
                    if (state.recurring) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("MONTHLY" to "Mensual", "WEEKLY" to "Semanal").forEach { (f, label) ->
                                within.means.android.ui.components.WmChip(
                                    label = label,
                                    selected = state.frequency == f,
                                    onClick = { viewModel.onFrequencyChanged(f) },
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.size(8.dp))
        }
    }

    if (showCalculator) {
        CalculatorSheet(
            initialAmount = state.amountText,
            currencySymbol = sym,
            accent = accent,
            onConfirm = { result ->
                viewModel.onAmountTextChanged(result)
                showCalculator = false
            },
            onDismiss = { showCalculator = false },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Borrar movimiento") },
            text = { Text("Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.delete()
                    },
                ) { Text("Borrar", color = WmTheme.colors.neg) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun AmountField(
    symbol: String,
    cents: Long,
    isEmpty: Boolean,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(WmRadii.lg))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "$symbol${within.means.android.ui.format.formatMoney(cents)}",
            fontSize = 52.sp,
            fontWeight = FontWeight.Bold,
            color = if (isEmpty) color.copy(alpha = 0.35f) else color,
        )
    }
}

@Composable
private fun ConceptSection(
    selected: List<String>,
    suggestions: List<ConceptChipUi>,
    input: String,
    accent: androidx.compose.ui.graphics.Color,
    onToggle: (String) -> Unit,
    onRemove: (String) -> Unit,
    onInputChange: (String) -> Unit,
    onCommit: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val unpicked = suggestions
            .map { it.label }
            .filterNot { s -> selected.any { it.equals(s, ignoreCase = true) } }

        if (selected.isNotEmpty() || unpicked.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                selected.forEach { label ->
                    ConceptChip(label = label, selected = true, accent = accent, onClick = { onRemove(label) })
                }
                unpicked.forEach { label ->
                    ConceptChip(label = label, selected = false, accent = accent, onClick = { onToggle(label) })
                }
            }
        }

        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            label = { Text("¿En qué fue?") },
            placeholder = { Text("cerveza, papá, bus…") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onCommit() }),
            trailingIcon = {
                if (input.isNotBlank()) {
                    IconButton(onClick = onCommit) {
                        Icon(Icons.Filled.Add, contentDescription = "Añadir concepto")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ConceptChip(
    label: String,
    selected: Boolean,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(WmRadii.pill)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) accent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface)
            .border(1.dp, if (selected) accent else MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            color = if (selected) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (selected) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Quitar",
                modifier = Modifier.size(14.dp),
                tint = accent,
            )
        }
    }
}

@Composable
private fun RelearnPromptCard(
    conceptLabel: String,
    categoryName: String,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(WmRadii.md))
            .background(WmTheme.colors.posSoft)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "¿Usar $categoryName para «$conceptLabel» a partir de ahora?",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onAccept) { Text("Sí, usarla") }
            TextButton(onClick = onDismiss) {
                Text("Ahora no", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DetailsHeader(open: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(WmRadii.md))
            .clickable(onClick = onToggle)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Detalles",
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = if (open) "Ocultar detalles" else "Mostrar detalles",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CategoryChip(
    name: String,
    color: androidx.compose.ui.graphics.Color,
    icon: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(WmRadii.pill)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) color.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                if (selected) color else MaterialTheme.colorScheme.outlineVariant,
                shape,
            )
            .clickable(onClick = onClick)
            .padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CatIcon(icon = iconFor(icon), color = color, boxSize = 28.dp, iconSize = 16.dp)
        Text(
            name,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            color = if (selected) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    LaunchedEffect(pressed) { if (pressed) showDialog = true }

    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text("Fecha") },
        trailingIcon = {
            IconButton(onClick = { showDialog = true }) {
                Icon(Icons.Filled.DateRange, contentDescription = "Elegir fecha")
            }
        },
        interactionSource = interactionSource,
        singleLine = true,
        modifier = modifier,
    )

    if (showDialog) {
        val initialMillis = remember(value) { isoToUtcMillis(value) }
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            onValueChange(utcMillisToIso(millis))
                        }
                        showDialog = false
                    },
                ) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    LaunchedEffect(pressed) { if (pressed) showDialog = true }

    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text("Hora") },
        placeholder = { Text("--:--") },
        trailingIcon = {
            IconButton(onClick = { showDialog = true }) {
                Icon(Icons.Filled.Schedule, contentDescription = "Elegir hora")
            }
        },
        interactionSource = interactionSource,
        singleLine = true,
        modifier = modifier,
    )

    if (showDialog) {
        val (initH, initM) = remember(value) { parseHourMinute(value) }
        val pickerState = rememberTimePickerState(initialHour = initH, initialMinute = initM, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val hh = pickerState.hour.toString().padStart(2, '0')
                        val mm = pickerState.minute.toString().padStart(2, '0')
                        onValueChange("$hh:$mm")
                        showDialog = false
                    },
                ) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancelar") }
            },
            text = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = pickerState)
                }
            },
        )
    }
}

/** Parses `HH:mm` into (hour, minute); falls back to noon for blank/invalid. */
private fun parseHourMinute(value: String): Pair<Int, Int> {
    val parts = value.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 12
    val m = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
    return h to m
}

private val typeOptions = listOf(
    "EXPENSE" to "Gasto",
    "INCOME" to "Ingreso",
    "TRANSFER" to "Ahorro",
)

private fun typeLabel(type: String): String = when (type) {
    "INCOME" -> "ingreso"
    "TRANSFER" -> "ahorro"
    else -> "gasto"
}

private fun isoToUtcMillis(iso: String): Long? =
    runCatching {
        LocalDate.parse(iso).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
    }.getOrNull()

private fun utcMillisToIso(millis: Long): String =
    Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date.toString()
