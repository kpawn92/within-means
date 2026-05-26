package within.means.shared.domain

interface ValueObject

abstract class StringValueObject(val value: String) : ValueObject {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        return value == (other as StringValueObject).value
    }

    override fun hashCode(): Int = 31 * value.hashCode() + this::class.hashCode()

    override fun toString(): String = "${this::class.simpleName}($value)"
}

abstract class IntValueObject(val value: Int) : ValueObject {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        return value == (other as IntValueObject).value
    }

    override fun hashCode(): Int = 31 * value.hashCode() + this::class.hashCode()

    override fun toString(): String = "${this::class.simpleName}($value)"
}
