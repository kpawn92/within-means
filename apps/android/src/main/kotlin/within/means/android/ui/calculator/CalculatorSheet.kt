package within.means.android.ui.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import within.means.android.ui.components.WmPrimaryButton
import within.means.android.ui.format.formatMoney

/**
 * Minimalist calculator bottom sheet. The amount field opens this; the user
 * types an arithmetic expression and confirms, applying the result back.
 *
 * @param initialAmount decimal string to seed the calculator (e.g. "15.50").
 * @param onConfirm receives the resulting decimal string for the amount field.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorSheet(
    initialAmount: String,
    currencySymbol: String,
    accent: Color,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val calc = remember { AmountCalculator(initialAmount.trim()) }
    // Local mirror so recomposition tracks the mutable calculator. Reading it
    // here (via remember keys below) subscribes the whole composable, so the
    // result/CTA recompute on every keystroke — not just the expression line.
    var expression by remember { mutableStateOf(calc.displayExpression()) }
    fun sync() { expression = calc.displayExpression() }

    val resultCents = remember(expression) { calc.resultCents() }
    val canApply = (resultCents ?: 0L) > 0L

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val hasOperator = expression.any { it in "×÷−+" }

            // Expression line (only while doing arithmetic) + C (clear).
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    if (hasOperator) expression else " ",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                ClearButton(onClick = { calc.clear(); sync() })
            }

            // Big display: the raw number as you type it (native feel) for plain
            // entry; the live result once an operator is in play.
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    if (hasOperator) {
                        "$currencySymbol${formatMoney(resultCents ?: 0L)}"
                    } else {
                        "$currencySymbol${expression.ifEmpty { "0" }}"
                    },
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (expression.isEmpty()) accent.copy(alpha = 0.35f) else accent,
                    maxLines = 1,
                )
            }

            CalcKeypad(
                onDigit = { calc.onDigit(it); sync() },
                onDot = { calc.onDot(); sync() },
                onOperator = { calc.onOperator(it); sync() },
                onBackspace = { calc.onBackspace(); sync() },
            )

            WmPrimaryButton(
                text = "Usar $currencySymbol${formatMoney(resultCents ?: 0L)}",
                onClick = { calc.resultText()?.let(onConfirm) },
                enabled = canApply,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Small round "C" button that clears the whole expression. */
@Composable
private fun ClearButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("C", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

private val keyRows = listOf(
    listOf("7", "8", "9", "÷"),
    listOf("4", "5", "6", "×"),
    listOf("1", "2", "3", "−"),
    listOf(".", "0", "⌫", "+"),
)

/** Reusable calculator keypad: digits + `.` + `⌫` and an operator column. The
 *  result is shown live by the caller, so there is no `=` key — the caller's CTA
 *  (e.g. "Usar"/"Guardar") commits the value. */
@Composable
fun CalcKeypad(
    onDigit: (Char) -> Unit,
    onDot: () -> Unit,
    onOperator: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        keyRows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { key ->
                    CalcKey(
                        key = key,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            when (key) {
                                "÷" -> onOperator('/')
                                "×" -> onOperator('*')
                                "−" -> onOperator('-')
                                "+" -> onOperator('+')
                                "." -> onDot()
                                "⌫" -> onBackspace()
                                else -> onDigit(key[0])
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CalcKey(
    key: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val isOperator = key in listOf("÷", "×", "−", "+")
    val bg = if (isOperator) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
    val fg = if (isOperator) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (key == "⌫") {
            Icon(
                Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "Borrar",
                tint = fg,
            )
        } else {
            Text(key, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = fg, textAlign = TextAlign.Center)
        }
    }
}
