package within.means.shared.infrastructure.persistence

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import within.means.shared.db.SharedDatabase
import within.means.shared.db.Domain_events
import within.means.shared.domain.bus.event.DomainEventRecord
import within.means.shared.domain.bus.event.DomainEventStore

class SqlDelightDomainEventStore(
    private val db: SharedDatabase,
    private val ioDispatcher: CoroutineDispatcher,
) : DomainEventStore {

    override suspend fun append(record: DomainEventRecord): Unit = withContext(ioDispatcher) {
        db.domainEventsQueries.append(
            event_id = record.eventId,
            event_name = record.eventName,
            aggregate_id = record.aggregateId,
            aggregate_type = record.aggregateType,
            occurred_on = record.occurredOn.toString(),
            payload_json = record.payloadJson,
            schema_version = record.schemaVersion.toLong(),
        )
    }

    override suspend fun appendAll(records: List<DomainEventRecord>): Unit = withContext(ioDispatcher) {
        if (records.isEmpty()) return@withContext
        db.domainEventsQueries.transaction {
            records.forEach { record ->
                db.domainEventsQueries.append(
                    event_id = record.eventId,
                    event_name = record.eventName,
                    aggregate_id = record.aggregateId,
                    aggregate_type = record.aggregateType,
                    occurred_on = record.occurredOn.toString(),
                    payload_json = record.payloadJson,
                    schema_version = record.schemaVersion.toLong(),
                )
            }
        }
    }

    override suspend fun findById(eventId: String): DomainEventRecord? = withContext(ioDispatcher) {
        db.domainEventsQueries.findById(eventId).executeAsOneOrNull()?.toRecord()
    }

    override suspend fun findByAggregate(aggregateId: String): List<DomainEventRecord> =
        withContext(ioDispatcher) {
            db.domainEventsQueries.findByAggregate(aggregateId).executeAsList().map { it.toRecord() }
        }

    override suspend fun findByName(eventName: String): List<DomainEventRecord> =
        withContext(ioDispatcher) {
            db.domainEventsQueries.findByName(eventName).executeAsList().map { it.toRecord() }
        }

    override suspend fun findAllSince(instant: Instant): List<DomainEventRecord> =
        withContext(ioDispatcher) {
            db.domainEventsQueries.findAllSince(instant.toString()).executeAsList().map { it.toRecord() }
        }

    override suspend fun findAll(): List<DomainEventRecord> = withContext(ioDispatcher) {
        db.domainEventsQueries.findAll().executeAsList().map { it.toRecord() }
    }

    private fun Domain_events.toRecord(): DomainEventRecord = DomainEventRecord(
        eventId = event_id,
        eventName = event_name,
        aggregateId = aggregate_id,
        aggregateType = aggregate_type,
        occurredOn = Instant.parse(occurred_on),
        payloadJson = payload_json,
        schemaVersion = schema_version.toInt(),
    )
}
