package de.krall.flare.std

sealed class Option<out T> {

    abstract fun isSome(): Boolean

    abstract fun isNone(): Boolean

    abstract fun expect(message: String): T

    abstract fun <R> map(mapper: (T) -> R): Option<R>
}

class None<T> : Option<T>() {

    override fun isSome(): Boolean {
        return false
    }

    override fun isNone(): Boolean {
        return true
    }

    override fun expect(message: String): T {
        throw IllegalStateException(message)
    }

    override fun toString(): String {
        return "None"
    }

    override fun <R> map(mapper: (T) -> R): Option<R> {
        return None()
    }
}

data class Some<T>(val value: T) : Option<T>() {

    override fun isSome(): Boolean {
        return true
    }

    override fun isNone(): Boolean {
        return false
    }

    override fun expect(message: String): T {
        return value
    }

    override fun toString(): String {
        return "Some($value)"
    }

    override fun <R> map(mapper: (T) -> R): Option<R> {
        return Some(mapper(value))
    }
}

inline fun <T> Option<T>.let(block: (T) -> Unit) {
    if (this is Some) {
        block(this.value)
    }
}

inline fun <T> Option<T>.ifLet(block: (T) -> Unit) {
    if (this is Some) {
        block(this.value)
    }
}

inline fun <T> Option<T>.ifLet(precondition: (T) -> Boolean, block: (T) -> Unit) {
    if (this is Some && precondition(this.value)) {
        block(this.value)
    }
}

inline fun <T> T?.into(): Option<T> {
    return if (this != null) {
        Some(this)
    } else {
        None()
    }
}