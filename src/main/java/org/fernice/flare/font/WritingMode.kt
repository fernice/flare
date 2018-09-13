/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.font

import kotlin.experimental.and

class WritingMode(private val flags: Byte) {

    companion object {
        const val RTL: Byte = 1 shl 0

        const val VERTICAL: Byte = 1 shl 1
        const val VERTICAL_LR: Byte = 1 shl 2

        const val LINE_INVERTED: Byte = 1 shl 3
        const val SIDEWAYS: Byte = 1 shl 4
        const val UPRIGHT: Byte = 1 shl 5

        private const val ZERO: Byte = 0
    }

    fun isVertical(): Boolean {
        return (flags and VERTICAL) != ZERO
    }
}
