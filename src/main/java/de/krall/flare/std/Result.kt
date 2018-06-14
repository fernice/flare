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

    abstract fun ok(): Option<T>

    abstract fun err(): Option<E>

    abstract fun isOk(): Boolean

    abstract fun isErr(): Boolean
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

    override fun ok(): Option<T> {
        return Some(value)
    }

    override fun err(): Option<Nothing> {
        return None()
    }

    override fun isOk(): Boolean {
        return true
    }

    override fun isErr(): Boolean {
        return false
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

    override fun ok(): Option<Nothing> {
        return None()
    }

    override fun err(): Option<E> {
        return Some(value)
    }

    override fun isOk(): Boolean {
        return false
    }

    override fun isErr(): Boolean {
        return true
    }
}

class Empty private constructor() {
    companion object {
        val instance: Empty by lazy { Empty() }
    }
}