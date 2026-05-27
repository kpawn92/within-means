package within.means.categories.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import within.means.categories.SequentialUuidGenerator
import kotlin.test.Test

class CategoryTest {

    private val uuids = SequentialUuidGenerator()

    private fun newExpense(name: String = "Alquiler") = Category.create(
        id = CategoryId(uuids.next()),
        kind = CategoryKind.EXPENSE,
        name = CategoryName(name),
        color = CategoryColor("#1976D2"),
        icon = CategoryIcon("home"),
        classifiers = CategoryClassifiers(
            nature = CategoryNature.FIXED,
            essentiality = CategoryEssentiality.ESSENTIAL,
            productive = false,
            engelGroup = EngelGroup.HOUSING,
        ),
        uuids = uuids,
    )

    @Test
    fun `create emits CategoryCreated`() {
        val c = newExpense()
        val events = c.pullDomainEvents()
        events shouldHaveSize 1
        events.first().shouldBeInstanceOf<CategoryCreated>()
    }

    @Test
    fun `rename mutates and emits an event`() {
        val c = newExpense().also { it.pullDomainEvents() }
        c.rename(CategoryName("Alquiler piso"), uuids)
        c.name.value shouldBe "Alquiler piso"
        c.pullDomainEvents().first().shouldBeInstanceOf<CategoryRenamed>()
    }

    @Test
    fun `rename to same name is a no-op`() {
        val c = newExpense().also { it.pullDomainEvents() }
        c.rename(c.name, uuids)
        c.pullDomainEvents() shouldHaveSize 0
    }

    @Test
    fun `restyle changes color and icon`() {
        val c = newExpense().also { it.pullDomainEvents() }
        c.restyle(CategoryColor("#000000"), CategoryIcon("house"), uuids)
        c.color.value shouldBe "#000000"
        c.icon.value shouldBe "house"
        c.pullDomainEvents().first().shouldBeInstanceOf<CategoryRestyled>()
    }

    @Test
    fun `reclassify replaces classifiers and emits event`() {
        val c = newExpense().also { it.pullDomainEvents() }
        c.reclassify(
            CategoryClassifiers(
                nature = CategoryNature.VARIABLE,
                essentiality = CategoryEssentiality.DISCRETIONARY,
                productive = true,
                engelGroup = EngelGroup.LEISURE,
            ),
            uuids,
        )
        c.classifiers.nature shouldBe CategoryNature.VARIABLE
        c.classifiers.essentiality shouldBe CategoryEssentiality.DISCRETIONARY
        c.classifiers.productive shouldBe true
        c.classifiers.engelGroup shouldBe EngelGroup.LEISURE
        c.pullDomainEvents().first().shouldBeInstanceOf<CategoryReclassified>()
    }

    @Test
    fun `markDeleted emits CategoryDeleted`() {
        val c = newExpense().also { it.pullDomainEvents() }
        c.markDeleted(uuids)
        c.pullDomainEvents().first().shouldBeInstanceOf<CategoryDeleted>()
    }

    @Test
    fun `INCOME categories cannot have nature or engel group`() {
        shouldThrow<IllegalArgumentException> {
            Category.create(
                id = CategoryId(uuids.next()),
                kind = CategoryKind.INCOME,
                name = CategoryName("Nómina"),
                color = CategoryColor("#2E7D32"),
                icon = CategoryIcon("work"),
                classifiers = CategoryClassifiers(
                    nature = CategoryNature.FIXED,
                    essentiality = null,
                    productive = false,
                    engelGroup = null,
                ),
                uuids = uuids,
            )
        }
    }
}
