/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare

import org.fernice.flare.style.source.StyleSource
import org.fernice.flare.style.ruletree.CascadeLevel
import kotlin.math.min

data class RuleTreeValues(val source: StyleSource, val cascadeLevel: CascadeLevel)

private const val SOURCE_ORDER_SHIFT = 0
private const val SOURCE_ORDER_BITS = 24
private const val SOURCE_ORDER_MAX = (1 shl SOURCE_ORDER_BITS) - 1
private const val SOURCE_ORDER_MASK = SOURCE_ORDER_MAX shl SOURCE_ORDER_SHIFT

private const val CASCADE_LEVEL_SHIFT = SOURCE_ORDER_BITS
private const val CASCADE_LEVEL_BITS = 4
private const val CASCADE_LEVEL_MAX = (1 shl CASCADE_LEVEL_BITS) - 1
private const val CASCADE_LEVEL_MASK = CASCADE_LEVEL_MAX shl CASCADE_LEVEL_SHIFT

class ApplicableDeclarationBits private constructor(private val bits: Int) {

    companion object {
        fun new(sourceOrder: Int, cascadeLevel: CascadeLevel): ApplicableDeclarationBits {
            var bits = min(sourceOrder, SOURCE_ORDER_MAX)
            bits = bits or (min(cascadeLevel.ordinal, CASCADE_LEVEL_MAX) shl CASCADE_LEVEL_SHIFT)
            return ApplicableDeclarationBits(bits)
        }
    }

    fun sourceOrder(): Int {
        return (bits and SOURCE_ORDER_MASK) shr SOURCE_ORDER_SHIFT
    }

    fun cascadeLevel(): CascadeLevel {
        val ordinal = (bits and CASCADE_LEVEL_MASK) shr CASCADE_LEVEL_SHIFT

        return CascadeLevel.values()[ordinal]
    }

    override fun equals(other: Any?): Boolean {
        return other is ApplicableDeclarationBits && bits == other.bits
    }

    override fun hashCode(): Int {
        return bits.hashCode()
    }
}
