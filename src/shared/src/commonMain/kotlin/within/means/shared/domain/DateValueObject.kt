package within.means.shared.domain

import kotlinx.datetime.LocalDate

abstract class DateValueObject(val value: LocalDate) : ValueObject {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        return value == (other as DateValueObject).value
    }

    override fun hashCode(): Int = 31 * value.hashCode() + this::class.hashCode()

    override fun toString(): String = "${this::class.simpleName}($value)"
}
