package within.means.android.ui.transactions

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionEditScreen(
    transactionId: String?,
    onBack: () -> Unit,
    onFinished: () -> Unit,
) {
    val viewModel: TransactionEditViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isEditMode) "Editar transacción" else "Nueva transacción") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            Text("Tipo")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("EXPENSE", "INCOME").forEach { t ->
                    FilterChip(
                        selected = state.type == t,
                        onClick = { if (!viewModel.isEditMode) viewModel.onTypeChanged(t) },
                        label = { Text(if (t == "EXPENSE") "Gasto" else "Ingreso") },
                        enabled = !viewModel.isEditMode,
                    )
                }
            }

            OutlinedTextField(
                value = state.amountText,
                onValueChange = viewModel::onAmountTextChanged,
                label = { Text("Monto (ej: 12.50)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            DateField(
                value = state.date,
                onValueChange = viewModel::onDateChanged,
                modifier = Modifier.fillMaxWidth(),
            )

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

            Text("Categoría")
            if (state.availableCategories.isEmpty()) {
                Text(
                    "No hay categorías de tipo ${if (state.type == "INCOME") "ingreso" else "gasto"}. " +
                        "Crea una primero.",
                )
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.availableCategories.forEach { c ->
                        FilterChip(
                            selected = state.categoryId == c.id,
                            onClick = { viewModel.onCategoryChanged(c.id) },
                            label = { Text(c.name) },
                        )
                    }
                }
            }

            Button(
                onClick = viewModel::save,
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.saving) "Guardando…" else "Guardar")
            }

            if (viewModel.isEditMode) {
                AssistChip(
                    onClick = { /* no-op informational */ },
                    label = { Text("Tipo no editable") },
                )
            }
        }
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

private fun isoToUtcMillis(iso: String): Long? =
    runCatching {
        LocalDate.parse(iso).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
    }.getOrNull()

private fun utcMillisToIso(millis: Long): String =
    Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date.toString()
