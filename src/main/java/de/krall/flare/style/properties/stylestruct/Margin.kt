package de.krall.flare.style.properties.stylestruct

import de.krall.flare.style.MutStyleStruct
import de.krall.flare.style.StyleStruct
import de.krall.flare.style.properties.longhand.MarginBottomDeclaration
import de.krall.flare.style.properties.longhand.MarginLeftDeclaration
import de.krall.flare.style.properties.longhand.MarginRightDeclaration
import de.krall.flare.style.properties.longhand.MarginTopDeclaration
import de.krall.flare.style.value.computed.LengthOrPercentageOrAuto

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
        override val left: LengthOrPercentageOrAuto) : Margin

class MutMargin(
        override var top: LengthOrPercentageOrAuto,
        override var right: LengthOrPercentageOrAuto,
        override var bottom: LengthOrPercentageOrAuto,
        override var left: LengthOrPercentageOrAuto) : Margin, MutStyleStruct