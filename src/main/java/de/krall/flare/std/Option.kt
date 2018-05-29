package de.krall.flare.std

sealed class Option<out T> {

    abstract fun expect(message: String): T
}

class None<T> : Option<T>() {
    override fun expect(message: String): T {
        throw IllegalStateException(message)
    }

    override fun toString(): String {
        return "None"
    }
}

data class Some<T>(val value: T) : Option<T>() {
    override fun expect(message: String): T {
        return value
    }

    override fun toString(): String {
        return "Some($value)"
    }
}

inline fun <T> Option<T>.let(block: (T) -> Unit) {
    if (this is Some) {
        block(this.value)
    }
}