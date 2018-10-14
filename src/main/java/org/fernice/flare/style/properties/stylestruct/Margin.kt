/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.properties.stylestruct

import org.fernice.flare.style.MutStyleStruct
import org.fernice.flare.style.StyleStruct
import org.fernice.flare.style.properties.longhand.MarginBottomDeclaration
import org.fernice.flare.style.properties.longhand.MarginLeftDeclaration
import org.fernice.flare.style.properties.longhand.MarginRightDeclaration
import org.fernice.flare.style.properties.longhand.MarginTopDeclaration
import org.fernice.flare.style.value.computed.LengthOrPercentageOrAuto

interface Margin : StyleStruct<MutMargin> {

    val top: LengthOrPercentageOrAuto
    val right: LengthOrPercentageOrAuto
    val bottom: LengthOrPercentageOrAuto
    val left: LengthOrPercentageOrAuto

    override fun clone(): MutMargin {
        return MutMargin(
            top,
            right,
            bottom,
            left
        )
    }

    companion object {

        val initial: Margin by lazy {
            StaticMargin(
                MarginTopDeclaration.initialValue,
                MarginRightDeclaration.initialValue,
                MarginBottomDeclaration.initialValue,
                MarginLeftDeclaration.initialValue
            )
        }
    }
}

class StaticMargin(
    override val top: LengthOrPercentageOrAuto,
    override val right: LengthOrPercentageOrAuto,
    override val bottom: LengthOrPercentageOrAuto,
    override val left: LengthOrPercentageOrAuto
) : Margin

class MutMargin(
    override var top: LengthOrPercentageOrAuto,
    override var right: LengthOrPercentageOrAuto,
    override var bottom: LengthOrPercentageOrAuto,
    override var left: LengthOrPercentageOrAuto
) : Margin, MutStyleStruct
