package within.means.transactions

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import within.means.shared.domain.UuidGenerator

class SequentialUuidGenerator : UuidGenerator {
    private var counter = 0
    override fun next(): String {
        counter++
        val n = counter.toString().padStart(12, '0')
        return "00000000-0000-4000-8000-$n"
    }
}

class FixedClock(private val moment: Instant) : Clock {
    override fun now(): Instant = moment
}
