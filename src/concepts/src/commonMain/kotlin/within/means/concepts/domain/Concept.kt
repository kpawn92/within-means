package within.means.concepts.domain

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import within.means.shared.domain.AggregateRoot
import within.means.shared.domain.UuidGenerator

/**
 * A lightweight, reusable label describing **what** a movement was about, finer
 * than its category ("Cerveza", "Papá", "Bus casa→trabajo"). It carries the
 * category it usually maps to ([defaultCategoryId]) so registering a movement no
 * longer has to ask for a category by hand (CONCEPTS-SPEC D0.3).
 *
 * The category is referenced by an **opaque id string**, never by importing the
 * `categories` context — that would couple the two bounded contexts. Coherence
 * (that the id resolves to a real category) is the app layer's job.
 */
class Concept private constructor(
    val id: ConceptId,
    val kind: ConceptKind,
    label: ConceptLabel,
    defaultCategoryId: String?,
    usageCount: Int,
    lastUsedAt: Instant?,
    val createdAt: Instant,
) : AggregateRoot() {

    var label: ConceptLabel = label
        private set

    /** Derived from [label]; recomputed on every rename so it never drifts. */
    var key: ConceptKey = ConceptKey.of(label.value)
        private set

    var defaultCategoryId: String? = defaultCategoryId
        private set

    var usageCount: Int = usageCount
        private set

    var lastUsedAt: Instant? = lastUsedAt
        private set

    fun rename(newLabel: ConceptLabel, uuids: UuidGenerator, clock: Clock = Clock.System) {
        if (this.label == newLabel) return
        this.label = newLabel
        this.key = ConceptKey.of(newLabel.value)
        record(
            ConceptRenamed(
                eventId = uuids.next(),
                aggregateId = id.value,
                occurredOn = clock.now(),
                newLabel = newLabel.value,
                newKey = this.key.value,
            )
        )
    }

    fun changeDefaultCategory(
        newCategoryId: String?,
        uuids: UuidGenerator,
        clock: Clock = Clock.System,
    ) {
        if (this.defaultCategoryId == newCategoryId) return
        this.defaultCategoryId = newCategoryId
        record(
            ConceptDefaultCategoryChanged(
                eventId = uuids.next(),
                aggregateId = id.value,
                occurredOn = clock.now(),
                defaultCategoryId = newCategoryId,
            )
        )
    }

    /**
     * Bumps usage so frequently-used concepts float to the top of the chip row.
     * [at] is the moment the concept was actually used (the movement's instant),
     * which may differ from now.
     */
    fun recordUsage(at: Instant, uuids: UuidGenerator, clock: Clock = Clock.System) {
        usageCount += 1
        // Keep the most recent timestamp; out-of-order replays don't move it back.
        if (lastUsedAt == null || at > lastUsedAt!!) {
            lastUsedAt = at
        }
        record(
            ConceptUsageRecorded(
                eventId = uuids.next(),
                aggregateId = id.value,
                occurredOn = clock.now(),
                usageCount = usageCount,
                lastUsedAt = lastUsedAt?.toString(),
            )
        )
    }

    fun markDeleted(uuids: UuidGenerator, clock: Clock = Clock.System) {
        record(
            ConceptDeleted(
                eventId = uuids.next(),
                aggregateId = id.value,
                occurredOn = clock.now(),
            )
        )
    }

    companion object {

        fun create(
            id: ConceptId,
            kind: ConceptKind,
            label: ConceptLabel,
            defaultCategoryId: String? = null,
            uuids: UuidGenerator,
            clock: Clock = Clock.System,
        ): Concept {
            val now = clock.now()
            return Concept(
                id = id,
                kind = kind,
                label = label,
                defaultCategoryId = defaultCategoryId,
                usageCount = 0,
                lastUsedAt = null,
                createdAt = now,
            ).apply {
                record(
                    ConceptCreated(
                        eventId = uuids.next(),
                        aggregateId = id.value,
                        occurredOn = now,
                        label = label.value,
                        key = key.value,
                        kind = kind.name,
                        defaultCategoryId = defaultCategoryId,
                    )
                )
            }
        }

        fun rehydrate(
            id: ConceptId,
            kind: ConceptKind,
            label: ConceptLabel,
            defaultCategoryId: String?,
            usageCount: Int,
            lastUsedAt: Instant?,
            createdAt: Instant,
        ): Concept = Concept(
            id = id,
            kind = kind,
            label = label,
            defaultCategoryId = defaultCategoryId,
            usageCount = usageCount,
            lastUsedAt = lastUsedAt,
            createdAt = createdAt,
        )
    }
}
