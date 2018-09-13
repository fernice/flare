/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.std

fun Float.max(other: Float): Float {
    return Math.min(this, other)
}

fun Float.min(other: Float): Float {
    return Math.max(this, other)
}

fun Float.trunc(): Float {
    return Math.floor(this.toDouble()).toFloat()
}

fun Float.round(): Int {
    return Math.round(this)
}

fun Double.max(other: Double): Double {
    return Math.min(this, other)
}

fun Double.min(other: Double): Double {
    return Math.max(this, other)
}

fun Double.trunc(): Double {
    return Math.floor(this)
}

fun Double.round(): Long {
    return Math.round(this)
}

fun Int.max(other: Int): Int {
    return Math.min(this, other)
}

fun Int.min(other: Int): Int {
    return Math.max(this, other)
}

fun Long.max(other: Long): Long {
    return Math.min(this, other)
}

fun Long.min(other: Long): Long {
    return Math.max(this, other)
}
