/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style

import org.fernice.flare.style.properties.stylestruct.*

data class ComputedValues(
    val font: Font,
    val color: Color,
    val background: Background,
    val border: Border,
    val margin: Margin,
    val padding: Padding
) {

    fun borderShapeHash(): Int {
        var hash = background.shapeHash() * 31
        hash *= margin.hashCode()
        hash *= padding.hashCode()
        return hash
    }

    fun backgroundShapeHash(): Int {
        var hash = background.shapeHash() * 31
        hash *= border.shapeHash()
        hash *= margin.hashCode()
        hash *= padding.hashCode()
        return hash
    }

    companion object {

        val initial: ComputedValues by lazy {
            ComputedValues(
                Font.initial,
                Color.initial,
                Background.initial,
                Border.initial,
                Margin.initial,
                Padding.initial
            )
        }
    }
}
