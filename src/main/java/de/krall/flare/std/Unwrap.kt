package de.krall.flare.std

class UnwrapException(message: String) : Exception(message)

fun <T> Option<T>.unwrap(): T {
    return when(this){
        is Some -> this.value
        is None -> throw UnwrapException("option is none")
    }
}

fun <T, E> Result<T, E>.unwrap(): T {
    return when (this) {
        is Ok -> this.value
        is Err -> throw UnwrapException("result was err: $value")
    }
}

fun <T, E> Result<T, E>.unwrapErr(): E {
    return when (this) {
        is Ok -> throw UnwrapException("result was ok: $value")
        is Err -> this.value
    }
}