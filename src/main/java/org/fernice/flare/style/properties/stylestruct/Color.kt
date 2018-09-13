/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.properties.stylestruct

import org.fernice.flare.cssparser.RGBA
import org.fernice.flare.style.MutStyleStruct
import org.fernice.flare.style.StyleStruct
import org.fernice.flare.style.value.computed.Color as ComputedColor

interface Color : StyleStruct<MutColor> {

    val color: RGBA

    override fun clone(): MutColor {
        return MutColor(
                color
        )
    }

    companion object {
        val initial: Color by lazy {
            StaticColor(
                    RGBA(0, 0, 0, 255)
            )
        }
    }
}

class StaticColor(override val color: RGBA) : Color

class MutColor(override var color: RGBA) : Color, MutStyleStruct
