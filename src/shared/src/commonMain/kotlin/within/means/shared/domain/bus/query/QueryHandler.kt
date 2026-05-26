package within.means.shared.domain.bus.query

import kotlin.reflect.KClass

interface QueryHandler<Q : Query, R : Response> {
    val queryType: KClass<Q>
    suspend fun handle(query: Q): R
}
