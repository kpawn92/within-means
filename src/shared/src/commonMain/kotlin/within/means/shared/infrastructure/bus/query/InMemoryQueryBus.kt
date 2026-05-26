package within.means.shared.infrastructure.bus.query

import within.means.shared.domain.bus.query.Query
import within.means.shared.domain.bus.query.QueryBus
import within.means.shared.domain.bus.query.QueryHandler
import within.means.shared.domain.bus.query.Response

class InMemoryQueryBus(handlers: List<QueryHandler<out Query, out Response>>) : QueryBus {

    private val registry = buildMap {
        handlers.forEach { handler ->
            val existing = put(handler.queryType, handler)
            require(existing == null) {
                "Duplicate handler registered for query ${handler.queryType.simpleName}"
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <Q : Query, R : Response> ask(query: Q): R {
        val handler = registry[query::class] as? QueryHandler<Q, R>
            ?: error("No handler registered for query ${query::class.simpleName}")
        return handler.handle(query)
    }
}
