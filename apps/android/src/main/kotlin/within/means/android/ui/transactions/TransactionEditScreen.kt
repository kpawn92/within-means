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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
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
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

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
        state.type == "INCOME" -> WmTheme.colors.pos
        state.type == "TRANSFER" -> WmTheme.colors.savings
        else -> WmTheme.colors.neg
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

            WmSegmented(
                options = typeOptions,
                selected = state.type,
                onSelect = { if (!viewModel.isEditMode) viewModel.onTypeChanged(it) },
            )

            // Giant editable amount, tinted by the chosen category (or type fallback).
            AmountField(
                symbol = sym,
                value = state.amountText,
                onValueChange = viewModel::onAmountTextChanged,
                color = accent,
            )

            // Category picker: horizontal chips with CatIcon.
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

            DateField(
                value = state.date,
                onValueChange = viewModel::onDateChanged,
                modifier = Modifier.fillMaxWidth(),
            )

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

            Spacer(Modifier.size(8.dp))
        }
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
    value: String,
    onValueChange: (String) -> Unit,
    color: androidx.compose.ui.graphics.Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(symbol, fontSize = 52.sp, fontWeight = FontWeight.Bold, color = color)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Start,
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            cursorBrush = SolidColor(color),
            modifier = Modifier.widthIn(min = 40.dp),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        "0",
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold,
                        color = color.copy(alpha = 0.35f),
                    )
                }
                inner()
            },
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
