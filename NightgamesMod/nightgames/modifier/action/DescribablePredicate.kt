package nightgames.modifier.action

import java.util.function.Predicate

class DescribablePredicate<T>(private val description: String, private val predicate: (T) -> Boolean) {
    fun test(act: T): Boolean {
        return predicate(act)
    }

    fun and(other: DescribablePredicate<T>): DescribablePredicate<T> {
        return DescribablePredicate("(and $this $other)") { act -> predicate(act) && other.predicate(act) }
    }

    fun negate(): DescribablePredicate<T> {
        return DescribablePredicate("(not $this)") { act -> !predicate(act) }
    }

    fun or(other: DescribablePredicate<T>): DescribablePredicate<T> {
        return DescribablePredicate("(or $this $other)") { act -> predicate(act) || other.predicate(act) }
    }

    override fun toString(): String {
        return description
    }

    companion object {
        @JvmStatic
        fun <T> True(): DescribablePredicate<T> {
            return DescribablePredicate("true") { true }
        }
    }
}