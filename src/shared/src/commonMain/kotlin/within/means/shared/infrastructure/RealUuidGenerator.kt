package within.means.shared.infrastructure

import com.benasher44.uuid.uuid4
import within.means.shared.domain.UuidGenerator

class RealUuidGenerator : UuidGenerator {
    override fun next(): String = uuid4().toString()
}
