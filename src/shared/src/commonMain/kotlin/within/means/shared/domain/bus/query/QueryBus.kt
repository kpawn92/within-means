package within.means.shared.domain.bus.query

interface QueryBus {
    suspend fun <Q : Query, R : Response> ask(query: Q): R
}
