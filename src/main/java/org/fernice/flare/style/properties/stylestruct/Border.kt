/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.properties.stylestruct

import org.fernice.flare.style.MutStyleStruct
import org.fernice.flare.style.StyleStruct
import org.fernice.flare.style.properties.longhand.border.BorderBottomColorDeclaration
import org.fernice.flare.style.properties.longhand.border.BorderBottomLeftRadiusDeclaration
import org.fernice.flare.style.properties.longhand.border.BorderBottomRightRadiusDeclaration
import org.fernice.flare.style.properties.longhand.border.BorderBottomStyleDeclaration
import org.fernice.flare.style.properties.longhand.border.BorderBottomWidthDeclaration
import org.fernice.flare.style.properties.longhand.border.BorderLeftColorDeclaration
import org.fernice.flare.style.properties.longhand.border.BorderLeftStyleDeclaration
import org.fernice.flare.style.properties.longhand.border.BorderLeftWidthDeclaration
import org.fernice.flare.style.properties.longhand.border.BorderRightColorDeclaration
import org.fernice.flare.style.properties.longhand.border.BorderRightStyleDeclaration
import org.fernice.flare.style.properties.longhand.border.BorderRightWidthDeclaration
import org.fernice.flare.style.properties.longhand.border.BorderTopColorDeclaration
import org.fernice.flare.style.properties.longhand.border.BorderTopLeftRadiusDeclaration
import org.fernice.flare.style.properties.longhand.border.BorderTopRightRadiusDeclaration
import org.fernice.flare.style.properties.longhand.border.BorderTopStyleDeclaration
import org.fernice.flare.style.properties.longhand.border.BorderTopWidthDeclaration
import org.fernice.flare.style.value.computed.BorderCornerRadius
import org.fernice.flare.style.value.computed.NonNegativeLength
import org.fernice.flare.style.value.computed.BorderStyle
import org.fernice.flare.style.value.computed.Color as ComputedColor

interface Border : StyleStruct<MutBorder> {

    val topWidth: NonNegativeLength
    val topColor: ComputedColor
    val topStyle: BorderStyle
    val topLeftRadius: BorderCornerRadius
    val topRightRadius: BorderCornerRadius

    val rightWidth: NonNegativeLength
    val rightColor: ComputedColor
    val rightStyle: BorderStyle

    val bottomWidth: NonNegativeLength
    val bottomColor: ComputedColor
    val bottomStyle: BorderStyle
    val bottomLeftRadius: BorderCornerRadius
    val bottomRightRadius: BorderCornerRadius

    val leftWidth: NonNegativeLength
    val leftColor: ComputedColor
    val leftStyle: BorderStyle

    fun shapeHash(): Int {
        var hash = topWidth.hashCode() * 31
        hash *= topColor.hashCode()
        hash *= topLeftRadius.hashCode()
        hash *= topRightRadius.hashCode()
        hash *= rightWidth.hashCode()
        hash *= rightColor.hashCode()
        hash *= bottomWidth.hashCode()
        hash *= bottomColor.hashCode()
        hash *= bottomLeftRadius.hashCode()
        hash *= bottomRightRadius.hashCode()
        hash *= leftWidth.hashCode()
        hash *= leftColor.hashCode()

        return hash
    }


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
                BorderTopWidthDeclaration.InitialValue,
                BorderTopColorDeclaration.InitialValue,
                BorderTopStyleDeclaration.initialValue,
                BorderTopLeftRadiusDeclaration.InitialValue,
                BorderTopRightRadiusDeclaration.InitialValue,

                BorderRightWidthDeclaration.InitialValue,
                BorderRightColorDeclaration.InitialValue,
                BorderRightStyleDeclaration.InitialValue,

                BorderBottomWidthDeclaration.InitialValue,
                BorderBottomColorDeclaration.InitialValue,
                BorderBottomStyleDeclaration.InitialValue,
                BorderBottomLeftRadiusDeclaration.InitialValue,
                BorderBottomRightRadiusDeclaration.InitialValue,

                BorderLeftWidthDeclaration.InitialValue,
                BorderLeftColorDeclaration.InitialValue,
                BorderLeftStyleDeclaration.InitialValue
            )
        }
    }
}

private data class StaticBorder(
    override val topWidth: NonNegativeLength,
    override val topColor: ComputedColor,
    override val topStyle: BorderStyle,
    override val topLeftRadius: BorderCornerRadius,
    override val topRightRadius: BorderCornerRadius,

    override val rightWidth: NonNegativeLength,
    override val rightColor: ComputedColor,
    override val rightStyle: BorderStyle,

    override val bottomWidth: NonNegativeLength,
    override val bottomColor: ComputedColor,
    override val bottomStyle: BorderStyle,
    override val bottomLeftRadius: BorderCornerRadius,
    override val bottomRightRadius: BorderCornerRadius,

    override val leftWidth: NonNegativeLength,
    override val leftColor: ComputedColor,
    override val leftStyle: BorderStyle
) : Border

data class MutBorder(
    override var topWidth: NonNegativeLength,
    override var topColor: ComputedColor,
    override var topStyle: BorderStyle,
    override var topLeftRadius: BorderCornerRadius,
    override var topRightRadius: BorderCornerRadius,

    override var rightWidth: NonNegativeLength,
    override var rightColor: ComputedColor,
    override var rightStyle: BorderStyle,

    override var bottomWidth: NonNegativeLength,
    override var bottomColor: ComputedColor,
    override var bottomStyle: BorderStyle,
    override var bottomLeftRadius: BorderCornerRadius,
    override var bottomRightRadius: BorderCornerRadius,

    override var leftWidth: NonNegativeLength,
    override var leftColor: ComputedColor,
    override var leftStyle: BorderStyle
) : Border, MutStyleStruct
