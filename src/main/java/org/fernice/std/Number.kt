/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.std

fun Int.checkedSub(other: Int): Int? {
    val result = this - other
    if (((this xor other) and (this xor result)) < 0) {
        return null
    }
    return result
}

fun Int.checkedDiv(other: Int): Int? {
    if (other == 0 || (this == Int.MIN_VALUE && other == -1)) {
        return null
    }
    return this / other
}