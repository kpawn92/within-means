package within.means.users.application

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import within.means.shared.domain.bus.event.DomainEvent
import within.means.shared.domain.bus.event.EventBus
import within.means.users.SequentialUuidGenerator
import within.means.users.application.ensure_default.DefaultUserBootstrap
import within.means.users.domain.UserDefaultCreated
import within.means.users.infrastructure.persistence.InMemoryUserProfileRepository
import kotlin.test.Test

class DefaultUserBootstrapTest {

    private class RecordingEventBus : EventBus {
        val published = mutableListOf<DomainEvent>()
        override suspend fun publish(events: List<DomainEvent>) {
            published.addAll(events)
        }
    }

    @Test
    fun `creates a default user and publishes UserDefaultCreated`() = runTest {
        val repo = InMemoryUserProfileRepository()
        val bus = RecordingEventBus()
        val uuids = SequentialUuidGenerator()
        val bootstrap = DefaultUserBootstrap(repo, uuids, bus)

        bootstrap.ensure()

        val user = repo.searchDefault()
            ?: error("default user not created")
        user.displayName.value shouldBe "Yo"
        bus.published shouldHaveSize 1
        bus.published.first().shouldBeInstanceOf<UserDefaultCreated>()
    }

    @Test
    fun `does nothing when a default user already exists`() = runTest {
        val repo = InMemoryUserProfileRepository()
        val bus = RecordingEventBus()
        val uuids = SequentialUuidGenerator()
        val bootstrap = DefaultUserBootstrap(repo, uuids, bus)

        bootstrap.ensure()   // first time creates
        val firstUserId = repo.searchDefault()!!.id

        bootstrap.ensure()   // second time idempotent

        repo.searchDefault()!!.id shouldBe firstUserId
        bus.published shouldHaveSize 1
    }
}
