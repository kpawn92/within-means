package within.means.shared.infrastructure.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import within.means.shared.domain.bus.event.DomainEvent
import within.means.shared.domain.bus.event.DomainEventRecord

/**
 * Serializes/deserializes domain events to/from the Event Store payload (JSON).
 *
 * The registry maps an event name (e.g. "transactions.registered") to the
 * KSerializer of its concrete subtype. Each bounded context registers its
 * own events when wiring Koin in apps/android.
 */
class DomainEventJsonSerializer(
    private val json: Json = DEFAULT_JSON,
    private val serializers: Map<String, KSerializer<out DomainEvent>>,
) {

    fun toRecord(event: DomainEvent): DomainEventRecord {
        val serializer = serializers[event.eventName]
            ?: error("No serializer registered for event '${event.eventName}'")

        @Suppress("UNCHECKED_CAST")
        val payload = json.encodeToString(serializer as KSerializer<DomainEvent>, event)

        return DomainEventRecord(
            eventId = event.eventId,
            eventName = event.eventName,
            aggregateId = event.aggregateId,
            aggregateType = event::class.simpleName ?: "Unknown",
            occurredOn = event.occurredOn,
            payloadJson = payload,
        )
    }

    fun fromRecord(record: DomainEventRecord): DomainEvent {
        val serializer = serializers[record.eventName]
            ?: error("No serializer registered for event '${record.eventName}'")
        return json.decodeFromString(serializer, record.payloadJson)
    }

    companion object {
        val DEFAULT_JSON = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}
