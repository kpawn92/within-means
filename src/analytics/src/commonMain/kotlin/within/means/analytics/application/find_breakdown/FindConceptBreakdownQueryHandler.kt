package within.means.analytics.application.find_breakdown

import kotlinx.datetime.LocalDate
import within.means.analytics.application.ConceptBreakdownItem
import within.means.analytics.application.ConceptBreakdownResponse
import within.means.analytics.application.YearMonth
import within.means.concepts.domain.ConceptRepository
import within.means.shared.domain.bus.query.QueryHandler
import within.means.transactions.domain.Transaction
import within.means.transactions.domain.TransactionRepository
import within.means.transactions.domain.TransactionType
import kotlin.reflect.KClass

class FindConceptBreakdownQueryHandler(
    private val transactions: TransactionRepository,
    private val concepts: ConceptRepository,
) : QueryHandler<FindConceptBreakdownQuery, ConceptBreakdownResponse> {

    override val queryType: KClass<FindConceptBreakdownQuery> = FindConceptBreakdownQuery::class

    override suspend fun handle(query: FindConceptBreakdownQuery): ConceptBreakdownResponse {
        val ym = YearMonth.parse(query.yearMonth)
        val type = TransactionType.valueOf(query.type)
        return conceptBreakdown(
            label = ym.text,
            type = type,
            matching = { YearMonth.of(it.date.value) == ym },
            transactions = transactions,
            concepts = concepts,
        )
    }
}

class FindConceptBreakdownInRangeQueryHandler(
    private val transactions: TransactionRepository,
    private val concepts: ConceptRepository,
) : QueryHandler<FindConceptBreakdownInRangeQuery, ConceptBreakdownResponse> {

    override val queryType: KClass<FindConceptBreakdownInRangeQuery> =
        FindConceptBreakdownInRangeQuery::class

    override suspend fun handle(query: FindConceptBreakdownInRangeQuery): ConceptBreakdownResponse {
        val start = LocalDate.parse(query.startDate)
        val end = LocalDate.parse(query.endDate)
        val type = TransactionType.valueOf(query.type)
        return conceptBreakdown(
            label = query.startDate,
            type = type,
            matching = { it.date.value in start..end },
            transactions = transactions,
            concepts = concepts,
        )
    }
}

private suspend fun conceptBreakdown(
    label: String,
    type: TransactionType,
    matching: (Transaction) -> Boolean,
    transactions: TransactionRepository,
    concepts: ConceptRepository,
): ConceptBreakdownResponse {
    val labelsById = concepts.all().associate { it.id.value to it.label.value }

    val matchingTx = transactions.all().filter { it.type == type && matching(it) }
    // Period total is the real (category-partitioned) total: each movement once.
    val periodTotal = matchingTx.sumOf { it.amount.cents }

    // Non-partition: a movement adds its full amount to EVERY concept it carries.
    val totalsByConcept = mutableMapOf<String, Long>()
    matchingTx.forEach { tx ->
        tx.conceptRefs.ids.forEach { conceptId ->
            totalsByConcept[conceptId] = (totalsByConcept[conceptId] ?: 0L) + tx.amount.cents
        }
    }

    val items = totalsByConcept
        .map { (conceptId, cents) ->
            ConceptBreakdownItem(
                conceptId = conceptId,
                conceptLabel = labelsById[conceptId] ?: "(desconocido)",
                totalCents = cents,
                share = if (periodTotal > 0L) cents.toDouble() / periodTotal else 0.0,
            )
        }
        .sortedByDescending { it.totalCents }

    return ConceptBreakdownResponse(
        periodLabel = label,
        type = type.name,
        totalCents = periodTotal,
        items = items,
    )
}
