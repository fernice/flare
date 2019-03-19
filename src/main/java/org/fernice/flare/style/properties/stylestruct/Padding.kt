/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.properties.stylestruct

import org.fernice.flare.style.MutStyleStruct
import org.fernice.flare.style.StyleStruct
import org.fernice.flare.style.properties.longhand.padding.PaddingBottomDeclaration
import org.fernice.flare.style.properties.longhand.padding.PaddingLeftDeclaration
import org.fernice.flare.style.properties.longhand.padding.PaddingRightDeclaration
import org.fernice.flare.style.properties.longhand.padding.PaddingTopDeclaration
import org.fernice.flare.style.value.computed.NonNegativeLengthOrPercentage

interface Padding : StyleStruct<MutPadding> {

    val top: NonNegativeLengthOrPercentage
    val right: NonNegativeLengthOrPercentage
    val bottom: NonNegativeLengthOrPercentage
    val left: NonNegativeLengthOrPercentage

    override fun clone(): MutPadding {
        return MutPadding(
            top,
            right,
            bottom,
            left
        )
    }

    companion object {

        val initial: Padding by lazy {
            StaticPadding(
                PaddingTopDeclaration.initialValue,
                PaddingRightDeclaration.initialValue,
                PaddingBottomDeclaration.initialValue,
                PaddingLeftDeclaration.initialValue
            )
        }
    }
}

data class StaticPadding(
    override val top: NonNegativeLengthOrPercentage,
    override val right: NonNegativeLengthOrPercentage,
    override val bottom: NonNegativeLengthOrPercentage,
    override val left: NonNegativeLengthOrPercentage
) : Padding

data class MutPadding(
    override var top: NonNegativeLengthOrPercentage,
    override var right: NonNegativeLengthOrPercentage,
    override var bottom: NonNegativeLengthOrPercentage,
    override var left: NonNegativeLengthOrPercentage
) : Padding, MutStyleStruct
