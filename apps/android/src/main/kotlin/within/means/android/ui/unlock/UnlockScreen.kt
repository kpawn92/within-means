package within.means.android.ui.unlock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import within.means.android.persistence.BiometricAvailability
import within.means.android.ui.theme.WmTheme

@Composable
fun UnlockScreen(onUnlocked: () -> Unit) {
    val viewModel: UnlockViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val pinLength = UnlockViewModel.PIN_LENGTH

    val availability: BiometricAvailability = koinInject()
    val biometric = rememberBiometricSession()
    var biometricUsable by remember { mutableStateOf(biometric.isEnrolled && availability.isAvailable) }
    var biometricNotice by remember { mutableStateOf<String?>(null) }

    fun promptBiometric() {
        biometric.unlock { result ->
            when (result) {
                is BiometricUnlockResult.Recovered -> viewModel.unlockWith(result.pin, onUnlocked)
                BiometricUnlockResult.Invalidated -> {
                    biometricUsable = false
                    biometricNotice = "Tus huellas cambiaron. Entra con tu PIN y vuelve a " +
                        "activar el desbloqueo rápido si quieres."
                }
                is BiometricUnlockResult.Failed -> biometricNotice = result.message
                BiometricUnlockResult.Cancelled -> Unit
            }
        }
    }

    // Offer the fingerprint sheet automatically on entry; the keypad stays behind it.
    LaunchedEffect(Unit) {
        if (biometricUsable) promptBiometric()
    }

    // auto-submit once the PIN is complete
    LaunchedEffect(state.pin) {
        if (state.pin.length == pinLength && !state.isWorking) {
            viewModel.submit(onUnlocked)
        }
    }

    val error = state.errorMessage != null

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text("Hola de nuevo", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(6.dp))
            Text("Introduce tu PIN para desbloquear", fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(32.dp))
            PinDots(filled = state.pin.length, total = pinLength, error = error)

            if (error) {
                Spacer(Modifier.height(14.dp))
                Text(state.errorMessage!!, color = WmTheme.colors.neg, fontSize = 13.sp)
            } else biometricNotice?.let { notice ->
                Spacer(Modifier.height(14.dp))
                Text(notice, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }

            Spacer(Modifier.height(36.dp))
            if (state.isWorking) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else {
                Keypad(
                    onDigit = { viewModel.updatePin(state.pin + it) },
                    onBackspace = { if (state.pin.isNotEmpty()) viewModel.updatePin(state.pin.dropLast(1)) },
                )
                if (biometricUsable) {
                    Spacer(Modifier.height(24.dp))
                    FingerprintButton(onClick = ::promptBiometric)
                }
            }
        }
    }
}

@Composable
private fun FingerprintButton(onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Fingerprint,
                contentDescription = "Desbloquear con huella",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text("Usar huella", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PinDots(filled: Int, total: Int, error: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(total) { i ->
            val on = i < filled
            val color = when {
                error -> WmTheme.colors.neg
                on -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.surfaceContainerHigh
            }
            Box(Modifier.size(14.dp).clip(RoundedCornerShape(7.dp)).background(color))
        }
    }
}

@Composable
private fun Keypad(onDigit: (String) -> Unit, onBackspace: () -> Unit) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "⌫"),
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                row.forEach { key ->
                    when (key) {
                        "" -> Spacer(Modifier.size(64.dp))
                        "⌫" -> KeyButton(onClick = onBackspace) {
                            Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Borrar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                        }
                        else -> KeyButton(onClick = { onDigit(key) }) {
                            Text(key, fontSize = 26.sp, fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}
