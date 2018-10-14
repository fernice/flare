/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.std

/**
 * Bounds the receiver to at most the specified [limit].
 */
@Deprecated(message = "non standard library", replaceWith = ReplaceWith("min(limit)"))
fun Float.atMost(limit: Float): Float = Math.min(this, limit)

/**
 * Bounds the receiver to at least the specified [limit].
 */
@Deprecated(message = "non standard library", replaceWith = ReplaceWith("max(limit)"))
fun Float.atLeast(limit: Float): Float = Math.max(this, limit)

/**
 * Returns the minimum of the two numbers.
 */
fun Float.min(other: Float): Float = Math.min(this, other)

/**
 * Returns the maximum of the two numbers.
 */
fun Float.max(other: Float): Float = Math.max(this, other)

/**
 * Truncates the receiver by any digit after the comma.
 */
fun Float.trunc(): Float = Math.floor(this.toDouble()).toFloat()


/**
 * Bounds the receiver to at most the specified [limit].
 */
@Deprecated(message = "non standard library", replaceWith = ReplaceWith("min(limit)"))
fun Double.atMost(limit: Double): Double {
    return Math.min(this, limit)
}

/**
 * Bounds the receiver to at least the specified [limit].
 */
@Deprecated(message = "non standard library", replaceWith = ReplaceWith("max(limit)"))
fun Double.atLeast(limit: Double): Double {
    return Math.max(this, limit)
}

/**
 * Returns the minimum of the two numbers.
 */
fun Double.min(other: Double): Double = Math.min(this, other)

/**
 * Returns the maximum of the two numbers.
 */
fun Double.max(other: Double): Double = Math.max(this, other)

fun Double.trunc(): Double {
    return Math.floor(this)
}

/**
 * Bounds the receiver to at most the specified [limit].
 */
@Deprecated(message = "non standard library", replaceWith = ReplaceWith("min(limit)"))
fun Int.atMost(limit: Int): Int {
    return Math.min(this, limit)
}

/**
 * Bounds the receiver to at least the specified [limit].
 */
@Deprecated(message = "non standard library", replaceWith = ReplaceWith("max(limit)"))
fun Int.atLeast(limit: Int): Int {
    return Math.max(this, limit)
}

/**
 * Returns the minimum of the two numbers.
 */
fun Int.min(other: Int): Int = Math.min(this, other)

/**
 * Returns the maximum of the two numbers.
 */
fun Int.max(other: Int): Int = Math.max(this, other)

/**
 * Bounds the receiver to at most the specified [limit].
 */
@Deprecated(message = "non standard library", replaceWith = ReplaceWith("min(limit)"))
fun Long.atMost(limit: Long): Long {
    return Math.min(this, limit)
}

/**
 * Bounds the receiver to at least the specified [limit].
 */
@Deprecated(message = "non standard library", replaceWith = ReplaceWith("max(limit)"))
fun Long.atLeast(limit: Long): Long {
    return Math.max(this, limit)
}

/**
 * Returns the minimum of the two numbers.
 */
fun Long.min(other: Long): Long = Math.min(this, other)

/**
 * Returns the maximum of the two numbers.
 */
fun Long.max(other: Long): Long = Math.max(this, other)