package within.means.android.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import within.means.android.persistence.ThemeMode
import within.means.android.persistence.ThemePreference
import within.means.android.ui.components.WmCard
import within.means.android.ui.components.WmChip
import within.means.android.ui.components.WmEyebrow
import within.means.android.ui.components.WmGhostButton
import within.means.android.ui.components.WmPrimaryButton
import within.means.android.ui.components.WmSegmented
import within.means.android.ui.components.WmToggle
import within.means.android.ui.format.currencySymbol

@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onManageRecurring: () -> Unit = {},
    onLockNow: () -> Unit = {},
    onReplayIntro: () -> Unit = {},
    onChangePin: () -> Unit = {},
) {
    val viewModel: SettingsViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val themePreference: ThemePreference = koinInject()
    val themeMode by themePreference.mode.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    LaunchedEffect(state.savedAck) {
        if (state.savedAck) {
            snackbarHostState.showSnackbar("Preferencias guardadas")
            viewModel.clearAck()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 20.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.size(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Atrás",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.size(8.dp))
                Text("Ajustes", fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface)
            }

            if (state.loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

            // profile
            WmCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(52.dp).clip(RoundedCornerShape(26.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(state.displayName.take(1).uppercase().ifBlank { "?" },
                            fontSize = 20.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Spacer(Modifier.size(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(state.displayName.ifBlank { "Sin nombre" }, fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Cuenta local · cifrada", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // preferences
            WmCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    WmEyebrow("Preferencias")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Apariencia", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        WmSegmented(
                            options = listOf(
                                ThemeMode.SYSTEM.name to "Sistema",
                                ThemeMode.LIGHT.name to "Claro",
                                ThemeMode.DARK.name to "Oscuro",
                            ),
                            selected = themeMode.name,
                            onSelect = { themePreference.setMode(ThemeMode.valueOf(it)) },
                        )
                    }
                    OutlinedTextField(
                        value = state.displayName,
                        onValueChange = viewModel::onDisplayNameChanged,
                        label = { Text("Nombre") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Idioma", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("es" to "Español", "en" to "English").forEach { (code, label) ->
                                WmChip(label, state.locale == code, { viewModel.onLocaleChanged(code) })
                            }
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Moneda base", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("EUR", "USD", "CUP").forEach { code ->
                                WmChip(code, state.baseCurrency == code, { viewModel.onBaseCurrencyChanged(code) })
                            }
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Inicio del mes", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                if (state.monthStartDay == 1) "Día 1 (mes natural)" else "Día ${state.monthStartDay}",
                                fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                StepButton("−", enabled = state.monthStartDay > 1) {
                                    viewModel.onMonthStartDayChanged(state.monthStartDay - 1)
                                }
                                StepButton("+", enabled = state.monthStartDay < 28) {
                                    viewModel.onMonthStartDayChanged(state.monthStartDay + 1)
                                }
                            }
                        }
                    }
                    WmPrimaryButton(
                        text = if (state.saving) "Guardando…" else "Guardar",
                        onClick = viewModel::save,
                        enabled = !state.saving && state.userId != null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // budget
            WmCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    WmEyebrow("Presupuesto")
                    OutlinedTextField(
                        value = state.monthlyBudgetText,
                        onValueChange = viewModel::onMonthlyBudgetChanged,
                        label = { Text("Plan mensual (${currencySymbol(state.baseCurrency)})") },
                        placeholder = { Text("Sin plan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Alertas de gasto", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface)
                            Text("Avisa cuando superes el plan", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        WmToggle(state.spendingAlertsEnabled, viewModel::onSpendingAlertsChanged)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onManageRecurring)
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Recurrentes", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface)
                            Text("Gestiona los movimientos que se repiten", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // security
            WmCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    WmEyebrow("Seguridad")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Ocultar importes al abrir", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface)
                            Text("Enmascara las cifras hasta que pulses el ojo", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        WmToggle(state.hideAmounts, viewModel::onHideAmountsChanged)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onChangePin)
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Cambiar PIN", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface)
                            Text("Vuelve a cifrar la base de datos con una nueva clave", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // actions
            WmPrimaryButton(
                text = "Bloquear ahora",
                onClick = onLockNow,
                modifier = Modifier.fillMaxWidth(),
            )
            WmGhostButton(
                text = "Ver introducción",
                onClick = onReplayIntro,
                modifier = Modifier.fillMaxWidth(),
            )

            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Within Means · cuenta local cifrada", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.size(8.dp))
        }
    }
}

@Composable
private fun StepButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    val fg = if (enabled) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = fg)
    }
}
