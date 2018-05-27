package de.krall.flare.std

fun <T, E> Ok(value: T): Result<T, E> {
    return Result.Ok(value)
}

fun <T> Err(): Result<T, Nothing> {
    return Result.Err()
}

fun <T, E> Err(value: E): Result<T, E> {
    return Result.Err(value)
}

sealed class Result<T, E> {

    abstract fun ok(): Option<T>

    abstract fun err(): Option<E>

    class Ok<T, E>(val value: T) : Result<T, E>() {

        override fun ok(): Option<T> {
            return Some(value)
        }

        override fun err(): Option<E> {
            return None()
        }
    }

    class Err<T, E>(val value: E) : Result<T, E>() {

        override fun ok(): Option<T> {
            return None()
        }

        override fun err(): Option<E> {
            return Some(value)
        }
    }
}