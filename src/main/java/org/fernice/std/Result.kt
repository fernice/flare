/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.std

fun Err(): Result<Nothing, Unit> {
    return Err(Unit)
}

fun Ok(): Result<Unit, Nothing> {
    return Ok(Unit)
}

/**
 * Immutable monad representing the outcome of an operation by bearing either a value or an error.
 * The positive outcome is represented by [Ok] and a negative outcome by [Err].
 */
sealed class Result<out T, out E> {

    /**
     * Expects the outcome to be [Ok], otherwise throws an [IllegalStateException] bearing the
     * specified [message].
     */
    abstract fun expect(message: String): T

    /**
     * Turns the Result into a nullable value under the premise that [Ok] is expected. If the Result
     * is [Err], the function will return [None] instead.
     */
    abstract fun ok(): T?

    /**
     * Turns the Result into a nullable value under the premise that [Err] is expected. If the Result
     * is [Ok], the function will return [None] instead.
     */
    abstract fun err(): E?

    /**
     * Returns true if this Result is of type [Ok].
     */
    abstract fun isOk(): Boolean

    /**
     * Returns true if this Result is of type [Err].
     */
    abstract fun isErr(): Boolean
}

/**
 * Concrete representation of a positive outcome of a [Result] bearing the value of the operation.
 */
data class Ok<out T>(val value: T) : Result<T, Nothing>() {

    override fun expect(message: String): T {
        return value
    }

    override fun ok(): T {
        return value
    }

    override fun err(): Nothing? {
        return null
    }

    override fun isOk(): Boolean {
        return true
    }

    override fun isErr(): Boolean {
        return false
    }
}

/**
 * Concrete representation of a negative outcome of a [Result] bearing the error of the operation.
 */
data class Err<out E>(val value: E) : Result<Nothing, E>() {

    override fun expect(message: String): Nothing {
        throw IllegalStateException("result is err")
    }

    override fun ok(): Nothing? {
        return null
    }

    override fun err(): E {
        return value
    }

    override fun isOk(): Boolean {
        return false
    }

    override fun isErr(): Boolean {
        return true
    }
}

/**
 * Maps the value of this Result using the specified [mapper] function and returns a Result
 * containing it. If the Result is [Err], the result of this function will also be [Err].
 */
inline fun <T, R, E> Result<T, E>.map(mapper: (T) -> R): Result<R, E> = when (this) {
    is Ok -> Ok(mapper(value))
    is Err -> this
}

/**
 * Maps the error of this Result using the specified [mapper] function and returns a Result
 * containing it. If the Result is [Ok], the result of this function will also be [Ok].
 */
inline fun <T, E, F> Result<T, E>.mapErr(mapper: (E) -> F): Result<T, F> = when (this) {
    is Ok -> this
    is Err -> Err(mapper(value))
}

fun <T, E> Result<T, E>.unwrap(): T {
    return when (this) {
        is Ok -> this.value
        is Err -> error("result was err: $value")
    }
}

inline fun <T, E> Result<T, E>.unwrap(block: (Err<E>) -> Nothing): T = when (this) {
    is Ok -> value
    is Err -> block(this)
}

fun <T, E> Result<T, E>.unwrapOr(alternative: T): T {
    return when (this) {
        is Ok -> this.value
        is Err -> alternative
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun <T, E> Result<T, E>.unwrapOrNull(): T? {
    return when (this) {
        is Ok -> this.value
        is Err -> null
    }
}

inline fun <T, E> Result<T, E>.unwrapOrElse(closure: (E) -> T): T {
    return when (this) {
        is Ok -> this.value
        is Err -> closure(this.value)
    }
}

inline fun <T, E> Result<T, E>.unwrapErr(block: (Ok<T>) -> Nothing): E = when (this) {
    is Ok -> block(this)
    is Err -> value
}

fun <T, E> Result<T, E>.unwrapErr(): E {
    return when (this) {
        is Ok -> error("result was ok: $value")
        is Err -> this.value
    }
}

fun <T, E> Result<T, E>.unwrapErrOr(alternative: E): E {
    return when (this) {
        is Ok -> alternative
        is Err -> this.value
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun <T, E> Result<T, E>.unwrapErrOrNull(): E? {
    return when (this) {
        is Ok -> null
        is Err -> this.value
    }
}

inline fun <T, E> Result<T, E>.unwrapErrOrElse(closure: (T) -> E): E {
    return when (this) {
        is Ok -> closure(this.value)
        is Err -> this.value
    }
}

inline fun <T, E, F> Result<T, E>.orElse(block: (E) -> Result<T, F>): Result<T, F> {
    if (this is Err) return block(this.value)
    return this as Ok
}

