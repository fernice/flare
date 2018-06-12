package de.krall.flare.std

sealed class Either<out A, out B>

data class First<A>(val value: A) : Either<A, Nothing>()

data class Second<B>(val value: B) : Either<Nothing, B>()