package de.krall.flare.style.properties.stylestruct

import de.krall.flare.style.MutStyleStruct
import de.krall.flare.style.StyleStruct
import de.krall.flare.style.properties.longhand.BorderBottomColorDeclaration
import de.krall.flare.style.properties.longhand.BorderBottomLeftRadiusDeclaration
import de.krall.flare.style.properties.longhand.BorderBottomRightRadiusDeclaration
import de.krall.flare.style.properties.longhand.BorderBottomStyleDeclaration
import de.krall.flare.style.properties.longhand.BorderBottomWidthDeclaration
import de.krall.flare.style.properties.longhand.BorderLeftColorDeclaration
import de.krall.flare.style.properties.longhand.BorderLeftStyleDeclaration
import de.krall.flare.style.properties.longhand.BorderLeftWidthDeclaration
import de.krall.flare.style.properties.longhand.BorderRightColorDeclaration
import de.krall.flare.style.properties.longhand.BorderRightStyleDeclaration
import de.krall.flare.style.properties.longhand.BorderRightWidthDeclaration
import de.krall.flare.style.properties.longhand.BorderTopColorDeclaration
import de.krall.flare.style.properties.longhand.BorderTopLeftRadiusDeclaration
import de.krall.flare.style.properties.longhand.BorderTopRightRadiusDeclaration
import de.krall.flare.style.properties.longhand.BorderTopStyleDeclaration
import de.krall.flare.style.properties.longhand.BorderTopWidthDeclaration
import de.krall.flare.style.value.computed.BorderCornerRadius
import de.krall.flare.style.value.computed.NonNegativeLength
import de.krall.flare.style.value.computed.Style
import de.krall.flare.style.value.computed.Color as ComputedColor

interface Border : StyleStruct<MutBorder> {

    val topWidth: NonNegativeLength
    val topColor: ComputedColor
    val topStyle: Style
    val topLeftRadius: BorderCornerRadius
    val topRightRadius: BorderCornerRadius

    val rightWidth: NonNegativeLength
    val rightColor: ComputedColor
    val rightStyle: Style

    val bottomWidth: NonNegativeLength
    val bottomColor: ComputedColor
    val bottomStyle: Style
    val bottomLeftRadius: BorderCornerRadius
    val bottomRightRadius: BorderCornerRadius

    val leftWidth: NonNegativeLength
    val leftColor: ComputedColor
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
                    BorderBottomLeftRadiusDeclaration.initialValue,
                    BorderBottomRightRadiusDeclaration.initialValue,

                    BorderLeftWidthDeclaration.initialValue,
                    BorderLeftColorDeclaration.initialValue,
                    BorderLeftStyleDeclaration.initialValue
            )
        }
    }
}

class StaticBorder(override val topWidth: NonNegativeLength,
                   override val topColor: ComputedColor,
                   override val topStyle: Style,
                   override val topLeftRadius: BorderCornerRadius,
                   override val topRightRadius: BorderCornerRadius,

                   override val rightWidth: NonNegativeLength,
                   override val rightColor: ComputedColor,
                   override val rightStyle: Style,

                   override val bottomWidth: NonNegativeLength,
                   override val bottomColor: ComputedColor,
                   override val bottomStyle: Style,
                   override val bottomLeftRadius: BorderCornerRadius,
                   override val bottomRightRadius: BorderCornerRadius,

                   override val leftWidth: NonNegativeLength,
                   override val leftColor: ComputedColor,
                   override val leftStyle: Style) : Border

class MutBorder(override var topWidth: NonNegativeLength,
                override var topColor: ComputedColor,
                override var topStyle: Style,
                override var topLeftRadius: BorderCornerRadius,
                override var topRightRadius: BorderCornerRadius,

                override var rightWidth: NonNegativeLength,
                override var rightColor: ComputedColor,
                override var rightStyle: Style,

                override var bottomWidth: NonNegativeLength,
                override var bottomColor: ComputedColor,
                override var bottomStyle: Style,
                override var bottomLeftRadius: BorderCornerRadius,
                override var bottomRightRadius: BorderCornerRadius,

                override var leftWidth: NonNegativeLength,
                override var leftColor: ComputedColor,
                override var leftStyle: Style) : Border, MutStyleStruct