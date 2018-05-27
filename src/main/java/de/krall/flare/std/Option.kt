package de.krall.flare.std

fun <T> Some(value: T): Option<T> {
    return Option.Some(value)
}

fun <T> None(): Option<T> {
    return Option.None()
}

sealed class Option<T> {

    class None<T> : Option<T>()

    class Some<T>(val value: T) : Option<T>()
}