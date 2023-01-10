/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.value.computed

import org.fernice.flare.cssparser.RGBA

sealed class Color {

    data class RGBA(val rgba: RGBAColor) : Color()

    object CurrentColor : Color()

    fun toRGBA(currentColor: RGBAColor): RGBAColor {
        return when (this) {
            is Color.RGBA -> rgba
            is Color.CurrentColor -> currentColor
        }
    }

    companion object {
        val Transparent: Color  =  RGBA(RGBAColor(0f, 0f, 0f, 0f))
    }
}

typealias RGBAColor = RGBA

typealias ColorPropertyValue = RGBA
