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
import org.fernice.flare.style.properties.longhand.FontWeightDeclaration
import org.fernice.flare.style.value.computed.FontFamily
import org.fernice.flare.style.value.computed.FontSize
import org.fernice.flare.style.value.computed.FontWeight

interface Font : StyleStruct<MutFont> {

    val fontFamily: FontFamily
    val fontSize: FontSize
    val fontWeight: FontWeight

    companion object {

        val initial: Font by lazy {
            StaticFont(
                FontFamilyDeclaration.initialValue,
                FontSizeDeclaration.initialValue,
                FontWeightDeclaration.InitialValue
            )
        }
    }
}

data class StaticFont(
    override val fontFamily: FontFamily,
    override val fontSize: FontSize,
    override val fontWeight: FontWeight
) : Font {

    override fun clone(): MutFont {
        return MutFont(
            fontFamily,
            fontSize,
            fontWeight
        )
    }
}

data class MutFont(
    override var fontFamily: FontFamily,
    override var fontSize: FontSize,
    override var fontWeight: FontWeight
) : Font, MutStyleStruct {

    override fun clone(): MutFont {
        return MutFont(
            fontFamily,
            fontSize,
            fontWeight
        )
    }
}
