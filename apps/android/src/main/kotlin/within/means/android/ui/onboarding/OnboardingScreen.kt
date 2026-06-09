package within.means.android.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import within.means.android.ui.components.WmPrimaryButton

@Composable
fun OnboardingScreen(onCompleted: () -> Unit) {
    val viewModel: OnboardingViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (state.step) {
            OnboardingStep.Welcome -> WelcomeStep(onContinue = viewModel::goToPin)
            OnboardingStep.Pin -> PinStep(state = state, viewModel = viewModel)
            OnboardingStep.Preferences -> PreferencesStep(
                state = state,
                viewModel = viewModel,
                onCompleted = onCompleted,
            )
            OnboardingStep.Done -> CenteredText("¡Listo!")
        }
    }
}

@Composable
private fun WelcomeStep(onContinue: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("within means", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(16.dp))
        Text(
            "Lleva tus ingresos y gastos diarios para entender de dónde viene y en qué se va tu dinero.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(32.dp))
        WmPrimaryButton(text = "Empezar", onClick = onContinue, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun PinStep(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Establece tu PIN", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Tu PIN cifra los datos de la app con AES-256. " +
                "Si lo olvidas, no se podrán recuperar los datos.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = state.pin,
            onValueChange = viewModel::updatePin,
            label = { Text("PIN (${OnboardingViewModel.PIN_LENGTH} dígitos)") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.pinConfirm,
            onValueChange = viewModel::updatePinConfirm,
            label = { Text("Confirma el PIN") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
        )

        if (state.errorMessage != null) {
            Spacer(Modifier.height(12.dp))
            Text(state.errorMessage, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = viewModel::submitPin,
            enabled = !state.isWorking,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isWorking) {
                CircularProgressIndicator(
                    modifier = Modifier.height(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text("Continuar")
            }
        }
    }
}

@Composable
private fun PreferencesStep(
    state: OnboardingUiState,
    viewModel: OnboardingViewModel,
    onCompleted: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Tus preferencias", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = state.displayName,
            onValueChange = viewModel::updateDisplayName,
            label = { Text("¿Cómo te llamamos?") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(20.dp))
        Text("Idioma", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        ChipRow(
            options = listOf("es" to "Español", "en" to "English"),
            selected = state.locale,
            onSelected = viewModel::updateLocale,
        )

        Spacer(Modifier.height(20.dp))
        Text("Moneda base", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        ChipRow(
            options = listOf("USD" to "USD ($)", "EUR" to "EUR (€)", "CUP" to "CUP (₱)"),
            selected = state.baseCurrency,
            onSelected = viewModel::updateBaseCurrency,
        )

        if (state.errorMessage != null) {
            Spacer(Modifier.height(12.dp))
            Text(state.errorMessage, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(28.dp))
        Button(
            onClick = { viewModel.finish(onCompleted) },
            enabled = !state.isWorking,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isWorking) {
                CircularProgressIndicator(
                    modifier = Modifier.height(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text("Finalizar")
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
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (code, label) ->
            AssistChip(
                onClick = { onSelected(code) },
                label = { Text(label) },
                colors = if (code == selected) {
                    AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                } else {
                    AssistChipDefaults.assistChipColors()
                },
            )
        }
    }
}

@Composable
private fun CenteredText(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.headlineMedium)
    }
}
