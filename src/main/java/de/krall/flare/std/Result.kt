package de.krall.flare.std

fun Err(): Result<Nothing, Empty> {
    return Err(Empty.instance)
}

fun Ok(): Result<Empty, Nothing> {
    return Ok(Empty.instance)
}

sealed class Result<out T, out E> {

    abstract fun expect(message: String): T

    abstract fun <U> map(mapper: (T) -> U): Result<U, E>

    abstract fun <F> mapErr(mapper: (E) -> F): Result<T, F>
}

data class Ok<out T>(val value: T) : Result<T, Nothing>() {

    override fun <U> map(mapper: (T) -> U): Result<U, Nothing> {
        return Ok(mapper(value))
    }

    override fun expect(message: String): T {
        return value
    }

    override fun <F> mapErr(mapper: (Nothing) -> F): Result<T, F> {
        return Ok(value)
    }
}

data class Err<out E>(val value: E) : Result<Nothing, E>() {

    override fun expect(message: String): Nothing {
        throw IllegalStateException("result is err")
    }

    override fun <U> map(mapper: (Nothing) -> U): Result<U, E> {
        return Err(value)
    }

    override fun <F> mapErr(mapper: (E) -> F): Result<Nothing, F> {
        return Err(mapper(value))
    }
}

class Empty private constructor() {
    companion object {
        val instance: Empty by lazy { Empty() }
    }
}