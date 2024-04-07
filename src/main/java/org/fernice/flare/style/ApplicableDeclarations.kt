/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style

import org.fernice.flare.style.properties.PropertyDeclaration
import org.fernice.flare.style.ruletree.CascadeLevel
import org.fernice.flare.style.ruletree.CascadePriority
import org.fernice.flare.style.source.StyleSource

typealias ApplicableDeclarationList = MutableList<ApplicableDeclarationBlock>

private const val SOURCE_ORDER_BITS = 24
private const val SOURCE_ORDER_MAX = (1 shl SOURCE_ORDER_BITS) - 1
private const val SOURCE_ORDER_MASK = SOURCE_ORDER_MAX

class ApplicableDeclarationBlock(
    val source: StyleSource,
    val sourceOrder: Int,
    val specificity: Int,
    val cascadePriority: CascadePriority,
) : StyleSourceAndCascadePriority {

    val level: CascadeLevel
        get() = cascadePriority.level

    val layerOrder: LayerOrder
        get() = cascadePriority.layerOrder

    override fun component1(): StyleSource = source
    override fun component2(): CascadePriority = cascadePriority

    fun forRuleTree(): StyleSourceAndCascadePriority = this

    companion object {
        fun from(
            source: StyleSource,
            sourceOrder: Int,
            specificity: Int,
            level: CascadeLevel,
            layerOrder: LayerOrder,
        ): ApplicableDeclarationBlock {
            return ApplicableDeclarationBlock(
                source,
                sourceOrder and SOURCE_ORDER_MASK,
                specificity,
                CascadePriority.of(level, layerOrder)
            )
        }

        fun fromStyleAttribute(
            source: StyleSource,
        ): ApplicableDeclarationBlock {
            return ApplicableDeclarationBlock(
                source,
                sourceOrder = 0,
                specificity = 0,
                CascadePriority.of(CascadeLevel.AuthorNormal, LayerOrder.StyleAttribute)
            )
        }
    }
}

interface StyleSourceAndCascadePriority {
    operator fun component1(): StyleSource
    operator fun component2(): CascadePriority
}
