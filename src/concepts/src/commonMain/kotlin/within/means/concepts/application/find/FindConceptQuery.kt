package within.means.concepts.application.find

import within.means.shared.domain.bus.query.Query

data class FindConceptQuery(val conceptId: String) : Query
