package de.krall.flare.style.properties.stylestruct

import de.krall.flare.style.MutStyleStruct
import de.krall.flare.style.StyleStruct
import de.krall.flare.style.properties.longhand.PaddingBottomDeclaration
import de.krall.flare.style.properties.longhand.PaddingLeftDeclaration
import de.krall.flare.style.properties.longhand.PaddingRightDeclaration
import de.krall.flare.style.properties.longhand.PaddingTopDeclaration
import de.krall.flare.style.value.computed.NonNegativeLengthOrPercentage

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

class StaticPadding(
        override val top: NonNegativeLengthOrPercentage,
        override val right: NonNegativeLengthOrPercentage,
        override val bottom: NonNegativeLengthOrPercentage,
        override val left: NonNegativeLengthOrPercentage) : Padding

class MutPadding(
        override var top: NonNegativeLengthOrPercentage,
        override var right: NonNegativeLengthOrPercentage,
        override var bottom: NonNegativeLengthOrPercentage,
        override var left: NonNegativeLengthOrPercentage) : Padding, MutStyleStruct