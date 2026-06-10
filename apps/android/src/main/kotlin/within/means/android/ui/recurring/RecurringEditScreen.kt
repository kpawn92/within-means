package within.means.android.ui.recurring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.OutlinedTextField
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
import within.means.android.ui.components.WmChip
import within.means.android.ui.components.WmEyebrow
import within.means.android.ui.components.WmPrimaryButton

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecurringEditScreen(ruleId: String, onBack: () -> Unit, onFinished: () -> Unit) {
    val viewModel: RecurringEditViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(ruleId) { viewModel.load(ruleId) }
    LaunchedEffect(state.isFinished) { if (state.isFinished) onFinished() }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val typeLabel = when (state.type) {
        "INCOME" -> "Ingreso"
        "TRANSFER" -> "Ahorro"
        else -> "Gasto"
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
                Text("Editar recurrente", fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface)
            }

            if (state.loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

            WmCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    WmEyebrow("$typeLabel · recurrente")
                    OutlinedTextField(
                        value = state.amountText,
                        onValueChange = viewModel::onAmountTextChanged,
                        label = { Text("Monto") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Categoría", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            state.availableCategories.forEach { cat ->
                                WmChip(cat.name, state.categoryId == cat.id, { viewModel.onCategoryChanged(cat.id) })
                            }
                        }
                    }
                    OutlinedTextField(
                        value = state.description,
                        onValueChange = viewModel::onDescriptionChanged,
                        label = { Text("Nota (opcional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Frecuencia", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            WmChip("Mensual", state.frequency == "MONTHLY", { viewModel.onFrequencyChanged("MONTHLY") })
                            WmChip("Semanal", state.frequency == "WEEKLY", { viewModel.onFrequencyChanged("WEEKLY") })
                        }
                    }
                    WmPrimaryButton(
                        text = if (state.saving) "Guardando…" else "Guardar cambios",
                        onClick = viewModel::save,
                        enabled = !state.saving && !state.loading,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(Modifier.size(8.dp))
        }
    }
}
