package de.krall.flare.style.properties.stylestruct

import de.krall.flare.style.MutStyleStruct
import de.krall.flare.style.StyleStruct
import de.krall.flare.style.properties.longhand.*
import de.krall.flare.style.value.computed.Color
import de.krall.flare.style.value.computed.LengthOrPercentage
import de.krall.flare.style.value.computed.NonNegativeLength
import de.krall.flare.style.value.computed.Style

interface Border : StyleStruct<MutBorder> {

    val topWidth: NonNegativeLength
    val topColor: Color
    val topStyle: Style
    val topLeftRadius: LengthOrPercentage
    val topRightRadius: LengthOrPercentage

    val rightWidth: NonNegativeLength
    val rightColor: Color
    val rightStyle: Style

    val bottomWidth: NonNegativeLength
    val bottomColor: Color
    val bottomStyle: Style
    val bottomLeftRadius: LengthOrPercentage
    val bottomRightRadius: LengthOrPercentage

    val leftWidth: NonNegativeLength
    val leftColor: Color
    val leftStyle: Style

    override fun clone(): MutBorder {
        return MutBorder(
                topWidth,
                topColor,
                topStyle,
                topLeftRadius,
                topRightRadius,

                rightWidth,
                rightColor,
                rightStyle,

                bottomWidth,
                bottomColor,
                bottomStyle,
                bottomLeftRadius,
                bottomRightRadius,

                leftWidth,
                leftColor,
                leftStyle
        )
    }

    companion object {

        val initial: Border by lazy {
            StaticBorder(
                    BorderTopWidthDeclaration.initialValue,
                    BorderTopColorDeclaration.initialValue,
                    BorderTopStyleDeclaration.initialValue,
                    BorderTopLeftRadiusDeclaration.initialValue,
                    BorderTopRightRadiusDeclaration.initialValue,

                    BorderRightWidthDeclaration.initialValue,
                    BorderRightColorDeclaration.initialValue,
                    BorderRightStyleDeclaration.initialValue,

                    BorderBottomWidthDeclaration.initialValue,
                    BorderBottomColorDeclaration.initialValue,
                    BorderBottomStyleDeclaration.initialValue,
                    BorderTopLeftRadiusDeclaration.initialValue,
                    BorderTopRightRadiusDeclaration.initialValue,

                    BorderLeftWidthDeclaration.initialValue,
                    BorderLeftColorDeclaration.initialValue,
                    BorderLeftStyleDeclaration.initialValue
            )
        }
    }
}

class StaticBorder(override val topWidth: NonNegativeLength,
                   override val topColor: Color,
                   override val topStyle: Style,
                   override val topLeftRadius: LengthOrPercentage,
                   override val topRightRadius: LengthOrPercentage,

                   override val rightWidth: NonNegativeLength,
                   override val rightColor: Color,
                   override val rightStyle: Style,

                   override val bottomWidth: NonNegativeLength,
                   override val bottomColor: Color,
                   override val bottomStyle: Style,
                   override val bottomLeftRadius: LengthOrPercentage,
                   override val bottomRightRadius: LengthOrPercentage,

                   override val leftWidth: NonNegativeLength,
                   override val leftColor: Color,
                   override val leftStyle: Style) : Border

class MutBorder(override var topWidth: NonNegativeLength,
                override var topColor: Color,
                override var topStyle: Style,
                override var topLeftRadius: LengthOrPercentage,
                override var topRightRadius: LengthOrPercentage,

                override var rightWidth: NonNegativeLength,
                override var rightColor: Color,
                override var rightStyle: Style,

                override var bottomWidth: NonNegativeLength,
                override var bottomColor: Color,
                override var bottomStyle: Style,
                override var bottomLeftRadius: LengthOrPercentage,
                override var bottomRightRadius: LengthOrPercentage,

                override var leftWidth: NonNegativeLength,
                override var leftColor: Color,
                override var leftStyle: Style) : Border, MutStyleStruct