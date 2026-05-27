package within.means.transactions.domain

import within.means.shared.domain.Identifier

/**
 * Cross-context reference to a category. Modelled as a local
 * UUID-validated value object instead of depending on `:categories` to
 * keep the bounded contexts decoupled at the Gradle level — the actual
 * category lookup happens in application services / read models, not in
 * the domain.
 */
class CategoryRef(value: String) : Identifier(value)
