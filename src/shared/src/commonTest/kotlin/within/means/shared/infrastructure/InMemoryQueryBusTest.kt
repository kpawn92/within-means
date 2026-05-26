package within.means.shared.infrastructure

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import within.means.shared.domain.bus.query.Query
import within.means.shared.domain.bus.query.QueryHandler
import within.means.shared.domain.bus.query.Response
import within.means.shared.infrastructure.bus.query.InMemoryQueryBus
import kotlin.reflect.KClass
import kotlin.test.Test

class InMemoryQueryBusTest {

    private data class GreetQuery(val name: String) : Query
    private data class GreetResponse(val message: String) : Response

    private class GreetHandler : QueryHandler<GreetQuery, GreetResponse> {
        override val queryType: KClass<GreetQuery> = GreetQuery::class
        override suspend fun handle(query: GreetQuery): GreetResponse = GreetResponse("Hello, ${query.name}")
    }

    @Test
    fun `routes a query to its handler and returns the response`() = runTest {
        val bus = InMemoryQueryBus(listOf(GreetHandler()))
        val response: GreetResponse = bus.ask(GreetQuery("Ada"))
        response.message shouldBe "Hello, Ada"
    }

    @Test
    fun `throws when there is no handler for a query`() = runTest {
        val bus = InMemoryQueryBus(emptyList())
        shouldThrow<IllegalStateException> { bus.ask<GreetQuery, GreetResponse>(GreetQuery("Ada")) }
    }
}
