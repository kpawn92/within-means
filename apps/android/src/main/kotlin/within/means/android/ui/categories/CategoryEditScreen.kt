package within.means.android.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CategoryEditScreen(
    categoryId: String?,
    onBack: () -> Unit,
    onFinished: () -> Unit,
) {
    val viewModel: CategoryEditViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(categoryId) {
        if (categoryId != null) viewModel.loadExisting(categoryId)
    }

    LaunchedEffect(state.isFinished) {
        if (state.isFinished) onFinished()
    }

    val title = if (viewModel.isEditMode) "Editar categoría" else "Nueva categoría"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
            )
        },
    ) { padding ->
        if (state.loading) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChanged,
                label = { Text("Nombre") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(20.dp))
            Text("Tipo", style = MaterialTheme.typography.titleSmall)
            ChipRow(
                options = listOf("EXPENSE" to "Gasto", "INCOME" to "Ingreso", "TRANSFER" to "Transferencia"),
                selected = state.kind,
                onSelected = viewModel::onKindChanged,
                enabled = !viewModel.isEditMode,
            )

            Spacer(Modifier.height(20.dp))
            Text("Color", style = MaterialTheme.typography.titleSmall)
            ColorRow(selected = state.color, onSelected = viewModel::onColorChanged)

            Spacer(Modifier.height(20.dp))
            Text("Icono", style = MaterialTheme.typography.titleSmall)
            IconRow(selected = state.icon, onSelected = viewModel::onIconChanged, color = state.color)

            if (state.kind == "EXPENSE") {
                Spacer(Modifier.height(20.dp))
                Text("Naturaleza", style = MaterialTheme.typography.titleSmall)
                ChipRow(
                    options = listOf("FIXED" to "Fijo", "VARIABLE" to "Variable"),
                    selected = state.nature.orEmpty(),
                    onSelected = viewModel::onNatureChanged,
                )

                Spacer(Modifier.height(20.dp))
                Text("Esencialidad", style = MaterialTheme.typography.titleSmall)
                ChipRow(
                    options = listOf("ESSENTIAL" to "Esencial", "DISCRETIONARY" to "Discrecional"),
                    selected = state.essentiality.orEmpty(),
                    onSelected = viewModel::onEssentialityChanged,
                )

                Spacer(Modifier.height(20.dp))
                Text("Grupo (Engel)", style = MaterialTheme.typography.titleSmall)
                ChipRow(
                    options = listOf(
                        "FOOD" to "Alimentación",
                        "HOUSING" to "Vivienda",
                        "TRANSPORT" to "Transporte",
                        "HEALTH" to "Salud",
                        "EDUCATION" to "Educación",
                        "LEISURE" to "Ocio",
                        "OTHER" to "Otros",
                    ),
                    selected = state.engelGroup.orEmpty(),
                    onSelected = viewModel::onEngelGroupChanged,
                )

                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = state.productive, onCheckedChange = viewModel::onProductiveChanged)
                    Spacer(Modifier.size(12.dp))
                    Column {
                        Text("Productivo", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Genera valor futuro (educación, salud, herramientas).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            state.errorMessage?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(28.dp))
            Button(
                onClick = viewModel::save,
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(if (viewModel.isEditMode) "Guardar cambios" else "Crear")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(
    options: List<Pair<String, String>>,
    selected: String,
    onSelected: (String) -> Unit,
    enabled: Boolean = true,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(top = 8.dp),
    ) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelected(value) },
                label = { Text(label) },
                enabled = enabled,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorRow(selected: String, onSelected: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 8.dp),
    ) {
        CategoryColorPalette.forEach { hex ->
            val isSelected = hex.equals(selected, ignoreCase = true)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(parseHex(hex), CircleShape)
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                        shape = CircleShape,
                    )
                    .clickable { onSelected(hex) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IconRow(selected: String, onSelected: (String) -> Unit, color: String) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 8.dp),
    ) {
        CategoryIconCatalog.keys.toList().forEach { id ->
            val isSelected = id == selected
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (isSelected) parseHex(color) else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                    )
                    .clickable { onSelected(id) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = iconFor(id),
                    contentDescription = id,
                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

private fun parseHex(hex: String): Color {
    val argb = "FF" + hex.removePrefix("#").uppercase()
    return Color(argb.toLong(16))
}
