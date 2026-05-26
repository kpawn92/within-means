package within.means.shared.domain.bus.event

import kotlinx.datetime.Instant

data class DomainEventRecord(
    val eventId: String,
    val eventName: String,
    val aggregateId: String,
    val aggregateType: String,
    val occurredOn: Instant,
    val payloadJson: String,
    val schemaVersion: Int = 1,
)

interface DomainEventStore {
    suspend fun append(record: DomainEventRecord)
    suspend fun appendAll(records: List<DomainEventRecord>)
    suspend fun findById(eventId: String): DomainEventRecord?
    suspend fun findByAggregate(aggregateId: String): List<DomainEventRecord>
    suspend fun findByName(eventName: String): List<DomainEventRecord>
    suspend fun findAllSince(instant: Instant): List<DomainEventRecord>
    suspend fun findAll(): List<DomainEventRecord>
}
