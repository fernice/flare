/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare

import java.lang.Error

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

fun panic(message: String): Nothing {
    throw Panic(message)
}

class Panic(message: String) : Error(message)