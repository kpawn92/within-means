package within.means.transactions.application.recurring

import within.means.shared.domain.bus.query.QueryHandler
import within.means.transactions.domain.RecurringRuleRepository
import kotlin.reflect.KClass

class ListActiveRecurringRulesQueryHandler(
    private val rules: RecurringRuleRepository,
) : QueryHandler<ListActiveRecurringRulesQuery, RecurringRulesResponse> {

    override val queryType: KClass<ListActiveRecurringRulesQuery> = ListActiveRecurringRulesQuery::class

    override suspend fun handle(query: ListActiveRecurringRulesQuery): RecurringRulesResponse =
        RecurringRulesResponse(rules.allActive().map { it.toResponse() })
}
