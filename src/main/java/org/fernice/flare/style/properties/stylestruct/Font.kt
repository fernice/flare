/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.properties.stylestruct

import org.fernice.flare.style.MutStyleStruct
import org.fernice.flare.style.StyleStruct
import org.fernice.flare.style.properties.longhand.FontFamilyDeclaration
import org.fernice.flare.style.properties.longhand.FontSizeDeclaration
import org.fernice.flare.style.value.computed.FontFamily
import org.fernice.flare.style.value.computed.FontSize

interface Font : StyleStruct<MutFont> {

    val fontFamily: FontFamily
    val fontSize: FontSize

    companion object {

        val initial: Font by lazy {
            StaticFont(
                FontFamilyDeclaration.initialValue,
                FontSizeDeclaration.initialValue
            )
        }
    }
}

class StaticFont(
    override val fontFamily: FontFamily,
    override val fontSize: FontSize
) : Font {

    override fun clone(): MutFont {
        return MutFont(
            fontFamily,
            fontSize
        )
    }
}

class MutFont(
    override var fontFamily: FontFamily,
    override var fontSize: FontSize
) : Font, MutStyleStruct {

    override fun clone(): MutFont {
        return MutFont(
            fontFamily,
            fontSize
        )
    }
}
