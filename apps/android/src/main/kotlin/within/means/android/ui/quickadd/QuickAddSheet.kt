package within.means.android.ui.quickadd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
    val amountColor = when (state.type) {
        "INCOME" -> WmTheme.colors.pos
        "TRANSFER" -> WmTheme.colors.savings
        else -> WmTheme.colors.neg
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

            // Expression line + giant live result.
            Column(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (state.expression.any { it in "+-*/" }) {
                    Text(
                        displayExpression(state.expression),
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                Text(
                    "$sym${formatMoney(state.amountCents)}",
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (state.expression.isEmpty()) amountColor.copy(alpha = 0.35f) else amountColor,
                )
            }

            // Category chips for the current type.
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

            // Extras: note + when.
            NoteField(state.note, viewModel::onNoteChanged)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WmChip("Hoy", state.whenChoice == QuickWhen.TODAY, { viewModel.onWhenChanged(QuickWhen.TODAY) })
                WmChip("Ayer", state.whenChoice == QuickWhen.YESTERDAY, { viewModel.onWhenChanged(QuickWhen.YESTERDAY) })
            }

            CalcKeypad(
                onDigit = viewModel::onDigit,
                onDot = viewModel::onDot,
                onOperator = viewModel::onOperator,
                onBackspace = viewModel::onBackspace,
            )

            WmPrimaryButton(
                text = if (state.saving) "Guardando…" else "Guardar ${formatAmount(state.amountCents, sym)}",
                onClick = viewModel::save,
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Maps a canonical expression to math glyphs for display (`* / -` → `× ÷ −`). */
private fun displayExpression(expr: String): String =
    expr.replace('*', '×').replace('/', '÷').replace('-', '−')

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

