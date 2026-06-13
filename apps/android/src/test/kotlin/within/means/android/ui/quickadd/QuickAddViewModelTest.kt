package within.means.android.ui.quickadd

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import within.means.android.ui.categories.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class QuickAddViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    @Test
    fun `keypad builds the amount and derives cents`() = runTest {
        val vm = QuickAddViewModel()
        vm.onDigit('1'); vm.onDigit('2')
        vm.onDot()
        vm.onDigit('5')

        vm.state.value.expression shouldBe "12.5"
        vm.state.value.amountCents shouldBe 1250L
    }

    @Test
    fun `decimals are capped at two`() = runTest {
        val vm = QuickAddViewModel()
        vm.onDigit('9'); vm.onDot(); vm.onDigit('9'); vm.onDigit('9')
        vm.onDigit('9') // ignored — already two decimals

        vm.state.value.expression shouldBe "9.99"
        vm.state.value.amountCents shouldBe 999L
    }

    @Test
    fun `leading zero is replaced by the next digit`() = runTest {
        val vm = QuickAddViewModel()
        vm.onDigit('0'); vm.onDigit('5')
        vm.state.value.expression shouldBe "5"
    }

    @Test
    fun `backspace removes the last character`() = runTest {
        val vm = QuickAddViewModel()
        vm.onDigit('1'); vm.onDigit('2'); vm.onBackspace()
        vm.state.value.expression shouldBe "1"
    }

    @Test
    fun `operators drive arithmetic and the live result`() = runTest {
        val vm = QuickAddViewModel()
        vm.onDigit('2'); vm.onOperator('+'); vm.onDigit('3'); vm.onOperator('*'); vm.onDigit('4')

        vm.state.value.expression shouldBe "2+3*4"
        vm.state.value.amountCents shouldBe 1400L // 2 + (3*4), precedence respected
    }

    @Test
    fun `canSave requires only a positive amount now`() = runTest {
        val vm = QuickAddViewModel()
        vm.state.value.canSave shouldBe false
        vm.onDigit('5')
        // Category is no longer required: it's inferred from the concept / "Otros".
        vm.state.value.canSave shouldBe true
    }

    @Test
    fun `typing a concept selects it and clears the input`() = runTest {
        val vm = QuickAddViewModel()
        vm.onConceptInputChanged("Cerveza")
        vm.onCommitTypedConcept()

        vm.state.value.selectedConcepts shouldBe listOf("Cerveza")
        vm.state.value.conceptInput shouldBe ""
    }

    @Test
    fun `list mode parses 'concepto monto' into a row and clears the field`() = runTest {
        val vm = QuickAddViewModel()
        vm.onModeChanged(QuickAddMode.LIST)
        vm.onBatchInputChanged("carro ruta1 a ruta2 78")
        vm.onCommitBatchLine()

        val line = vm.state.value.batchLines.single()
        line.label shouldBe "carro ruta1 a ruta2"
        line.amountCents shouldBe 7800L
        vm.state.value.batchInput shouldBe ""
    }

    @Test
    fun `list mode keeps a running total across rows`() = runTest {
        val vm = QuickAddViewModel()
        vm.onModeChanged(QuickAddMode.LIST)
        vm.onBatchInputChanged("pan 15"); vm.onCommitBatchLine()
        vm.onBatchInputChanged("leche 1,50"); vm.onCommitBatchLine()

        vm.state.value.batchLines shouldHaveSize 2
        vm.state.value.batchTotalCents shouldBe 1650L
        vm.state.value.canSaveBatch shouldBe true
    }

    @Test
    fun `list mode rejects a line with no amount`() = runTest {
        val vm = QuickAddViewModel()
        vm.onModeChanged(QuickAddMode.LIST)
        vm.onBatchInputChanged("solo texto")
        vm.onCommitBatchLine()

        vm.state.value.batchLines shouldHaveSize 0
        vm.state.value.errorMessage shouldBe "Escribe concepto y monto, p. ej. \"pan 15\""
    }

    @Test
    fun `removing a batch line drops it from the basket`() = runTest {
        val vm = QuickAddViewModel()
        vm.onModeChanged(QuickAddMode.LIST)
        vm.onBatchInputChanged("pan 15"); vm.onCommitBatchLine()
        val id = vm.state.value.batchLines.single().id
        vm.onRemoveBatchLine(id)

        vm.state.value.batchLines shouldHaveSize 0
    }
}
