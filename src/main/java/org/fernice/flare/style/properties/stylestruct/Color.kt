/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.properties.stylestruct

import org.fernice.flare.cssparser.RGBA
import org.fernice.flare.style.MutStyleStruct
import org.fernice.flare.style.StyleStruct
import org.fernice.flare.style.properties.longhand.color.ColorDeclaration
import org.fernice.flare.style.properties.longhand.color.FillDeclaration
import org.fernice.flare.style.value.computed.Fill
import org.fernice.flare.style.value.computed.Color as ComputedColor

interface Color : StyleStruct<MutColor> {

    val color: RGBA
    val fill: Fill

    override fun clone(): MutColor {
        return MutColor(
            color,
            fill
        )
    }

    companion object {
        val Initial: Color by lazy {
            StaticColor(
                ColorDeclaration.InitialValue,
                FillDeclaration.InitialValue
            )
        }
    }
}

data class StaticColor(
    override val color: RGBA,
    override val fill: Fill
) : Color

data class MutColor(
    override var color: RGBA,
    override var fill: Fill
) : Color, MutStyleStruct
