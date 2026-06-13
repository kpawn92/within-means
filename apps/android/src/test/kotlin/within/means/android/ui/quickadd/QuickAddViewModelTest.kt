package within.means.android.ui.quickadd

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
}
