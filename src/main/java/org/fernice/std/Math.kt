/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.std

/**
 * Truncates the receiver by any digit after the comma.
 */
fun Float.trunc(): Float = Math.floor(this.toDouble()).toFloat()

/**
 * Truncates the receiver by any digit after the comma.
 */
fun Double.trunc(): Double {
    return Math.floor(this)
}
