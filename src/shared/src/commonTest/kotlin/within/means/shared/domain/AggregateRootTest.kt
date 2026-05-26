package within.means.shared.domain

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.datetime.Instant
import within.means.shared.domain.bus.event.DomainEvent
import kotlin.test.Test

class AggregateRootTest {

    private data class Created(
        override val eventId: String,
        override val aggregateId: String,
        override val occurredOn: Instant,
    ) : DomainEvent {
        override val eventName: String = "test.created"
    }

    private class TestAggregate(val id: String) : AggregateRoot() {
        fun doSomething() {
            record(Created(eventId = "evt-1", aggregateId = id, occurredOn = Instant.fromEpochSeconds(0)))
        }
    }

    @Test
    fun `records and pulls events`() {
        val a = TestAggregate(id = "aggregate-1")
        a.doSomething()
        a.doSomething()

        val events = a.pullDomainEvents()
        events.size shouldBe 2
    }

    @Test
    fun `pulling clears the internal buffer`() {
        val a = TestAggregate(id = "aggregate-1")
        a.doSomething()
        a.pullDomainEvents()

        a.pullDomainEvents().shouldBeEmpty()
    }
}
