package de.krall.flare

annotation class Experimental

private const val debug = true

fun debugAssert(assert: Boolean, message: String = "Assert failed") {
    if (debug && !assert) {
        throw AssertionError(message)
    }
}

fun debugAssert(assert: () -> Boolean, message: String = "Assert failed") {
    if (debug && !assert()) {
        throw AssertionError(message)
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun assert(assert: Boolean, message: String) {
    if (!assert) {
        throw AssertionError(message)
    }
}

inline fun assert(assert: () -> Boolean, message: String = "Assert failed") {
    if (!assert()) {
        throw AssertionError(message)
    }
}