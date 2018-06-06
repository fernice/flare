package de.krall.flare.std

sealed class Either<out A, out B>

class First<A>(val value: A) : Either<A, Nothing>()

class Second<B>(val value: B) : Either<Nothing, B>()