package within.means.analytics

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import within.means.categories.domain.Category
import within.means.categories.domain.CategoryClassifiers
import within.means.categories.domain.CategoryColor
import within.means.categories.domain.CategoryEssentiality
import within.means.categories.domain.CategoryIcon
import within.means.categories.domain.CategoryId
import within.means.categories.domain.CategoryKind
import within.means.categories.domain.CategoryName
import within.means.categories.domain.CategoryNature
import within.means.categories.domain.CategoryRepository
import within.means.categories.infrastructure.persistence.InMemoryCategoryRepository
import within.means.concepts.domain.Concept
import within.means.concepts.domain.ConceptId
import within.means.concepts.domain.ConceptKind
import within.means.concepts.domain.ConceptLabel
import within.means.concepts.domain.ConceptRepository
import within.means.concepts.infrastructure.persistence.InMemoryConceptRepository
import within.means.shared.domain.UuidGenerator
import within.means.transactions.domain.Amount
import within.means.transactions.domain.CategoryRef
import within.means.transactions.domain.ConceptRefs
import within.means.transactions.domain.Transaction
import within.means.transactions.domain.TransactionDate
import within.means.transactions.domain.TransactionDescription
import within.means.transactions.domain.TransactionId
import within.means.transactions.domain.TransactionRepository
import within.means.transactions.domain.TransactionType
import within.means.transactions.infrastructure.persistence.InMemoryTransactionRepository

class SequentialUuidGenerator(start: Int = 0) : UuidGenerator {
    private var counter = start
    override fun next(): String {
        counter++
        return "00000000-0000-4000-8000-${counter.toString().padStart(12, '0')}"
    }
}

class FixedClock(private val moment: Instant) : Clock {
    override fun now(): Instant = moment
}

class AnalyticsTestEnv(
    val transactions: TransactionRepository = InMemoryTransactionRepository(),
    val categories: CategoryRepository = InMemoryCategoryRepository(),
    val concepts: ConceptRepository = InMemoryConceptRepository(),
    val zone: TimeZone = TimeZone.UTC,
    val clockMoment: Instant = Instant.parse("2026-05-27T12:00:00Z"),
) {
    val clock: Clock = FixedClock(clockMoment)
    private val uuids = SequentialUuidGenerator()

    suspend fun expenseCategory(
        name: String,
        nature: CategoryNature = CategoryNature.VARIABLE,
        essentiality: CategoryEssentiality = CategoryEssentiality.DISCRETIONARY,
    ): String {
        val id = CategoryId(uuids.next())
        val cat = Category.rehydrate(
            id = id,
            kind = CategoryKind.EXPENSE,
            name = CategoryName(name),
            color = CategoryColor("#FF0000"),
            icon = CategoryIcon("label"),
            classifiers = CategoryClassifiers(
                nature = nature,
                essentiality = essentiality,
                productive = false,
                engelGroup = null,
            ),
            parentId = null,
            createdAt = clockMoment,
        )
        categories.save(cat)
        return id.value
    }

    suspend fun incomeCategory(name: String): String {
        val id = CategoryId(uuids.next())
        val cat = Category.rehydrate(
            id = id,
            kind = CategoryKind.INCOME,
            name = CategoryName(name),
            color = CategoryColor("#00FF00"),
            icon = CategoryIcon("label"),
            classifiers = CategoryClassifiers(
                nature = null,
                essentiality = null,
                productive = false,
                engelGroup = null,
            ),
            parentId = null,
            createdAt = clockMoment,
        )
        categories.save(cat)
        return id.value
    }

    suspend fun expense(cents: Long, on: LocalDate, categoryId: String) {
        transactions.save(
            Transaction.register(
                id = TransactionId(uuids.next()),
                type = TransactionType.EXPENSE,
                amount = Amount(cents),
                date = TransactionDate(on),
                description = TransactionDescription.EMPTY,
                categoryRef = CategoryRef(categoryId),
                uuids = uuids,
                clock = clock,
                timeZone = zone,
            )
        )
    }

    suspend fun concept(label: String, kind: ConceptKind = ConceptKind.EXPENSE): String {
        val c = Concept.create(
            id = ConceptId(uuids.next()),
            kind = kind,
            label = ConceptLabel(label),
            uuids = uuids,
            clock = clock,
        )
        concepts.save(c)
        return c.id.value
    }

    suspend fun expense(cents: Long, on: LocalDate, categoryId: String, conceptIds: List<String>) {
        transactions.save(
            Transaction.register(
                id = TransactionId(uuids.next()),
                type = TransactionType.EXPENSE,
                amount = Amount(cents),
                date = TransactionDate(on),
                description = TransactionDescription.EMPTY,
                categoryRef = CategoryRef(categoryId),
                conceptRefs = ConceptRefs(conceptIds),
                uuids = uuids,
                clock = clock,
                timeZone = zone,
            )
        )
    }

    suspend fun income(cents: Long, on: LocalDate, categoryId: String) {
        transactions.save(
            Transaction.register(
                id = TransactionId(uuids.next()),
                type = TransactionType.INCOME,
                amount = Amount(cents),
                date = TransactionDate(on),
                description = TransactionDescription.EMPTY,
                categoryRef = CategoryRef(categoryId),
                uuids = uuids,
                clock = clock,
                timeZone = zone,
            )
        )
    }

    suspend fun transferCategory(name: String): String {
        val id = CategoryId(uuids.next())
        val cat = Category.rehydrate(
            id = id,
            kind = CategoryKind.TRANSFER,
            name = CategoryName(name),
            color = CategoryColor("#0000FF"),
            icon = CategoryIcon("label"),
            classifiers = CategoryClassifiers(
                nature = null,
                essentiality = null,
                productive = false,
                engelGroup = null,
            ),
            parentId = null,
            createdAt = clockMoment,
        )
        categories.save(cat)
        return id.value
    }

    suspend fun transfer(cents: Long, on: LocalDate, categoryId: String) {
        transactions.save(
            Transaction.register(
                id = TransactionId(uuids.next()),
                type = TransactionType.TRANSFER,
                amount = Amount(cents),
                date = TransactionDate(on),
                description = TransactionDescription.EMPTY,
                categoryRef = CategoryRef(categoryId),
                uuids = uuids,
                clock = clock,
                timeZone = zone,
            )
        )
    }
}
