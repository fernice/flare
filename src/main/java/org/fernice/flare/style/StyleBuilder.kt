/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style

import org.fernice.flare.cssparser.RGBA
import org.fernice.flare.dom.Device
import org.fernice.flare.style.properties.CustomPropertiesList
import org.fernice.flare.style.properties.PropertiesList
import org.fernice.flare.style.properties.longhand.background.Attachment
import org.fernice.flare.style.properties.longhand.background.Clip
import org.fernice.flare.style.properties.longhand.background.Origin
import org.fernice.flare.style.properties.stylestruct.Background
import org.fernice.flare.style.properties.stylestruct.Border
import org.fernice.flare.style.properties.stylestruct.Color
import org.fernice.flare.style.properties.stylestruct.Font
import org.fernice.flare.style.properties.stylestruct.Margin
import org.fernice.flare.style.properties.stylestruct.MutBackground
import org.fernice.flare.style.properties.stylestruct.MutBorder
import org.fernice.flare.style.properties.stylestruct.MutColor
import org.fernice.flare.style.properties.stylestruct.MutFont
import org.fernice.flare.style.properties.stylestruct.MutMargin
import org.fernice.flare.style.properties.stylestruct.MutPadding
import org.fernice.flare.style.properties.stylestruct.Padding
import org.fernice.flare.style.ruletree.RuleNode
import org.fernice.flare.style.value.computed.BackgroundRepeat
import org.fernice.flare.style.value.computed.BackgroundSize
import org.fernice.flare.style.value.computed.BorderCornerRadius
import org.fernice.flare.style.value.computed.Fill
import org.fernice.flare.style.value.computed.FontFamily
import org.fernice.flare.style.value.computed.FontSize
import org.fernice.flare.style.value.computed.FontWeight
import org.fernice.flare.style.value.computed.HorizontalPosition
import org.fernice.flare.style.value.computed.ImageLayer
import org.fernice.flare.style.value.computed.LengthOrPercentageOrAuto
import org.fernice.flare.style.value.computed.NonNegativeLength
import org.fernice.flare.style.value.computed.NonNegativeLengthOrPercentage
import org.fernice.flare.style.value.computed.BorderStyle
import org.fernice.flare.style.value.computed.VerticalPosition
import org.fernice.flare.style.value.computed.Color as ComputedColor

interface StyleStruct<T : MutStyleStruct> {

    /**
     * Clones the style struct returning a owned, mutable reference.
     */
    fun clone(): T
}

interface MutStyleStruct

/**
 * A reference to a [StyleStruct] in different states. The referenced StyleStruct might be borrowed, owned or vacated.
 * As long as a StyleStruct is borrowed the original reference is being used. Once mutate has been called the state
 * will change to owned as the immutable StyleStruct will be cloned to acquire a mutable reference. The reference is
 * vacated, if the StyleStruct has been taken.
 * [T] must be assignable from [M].
 */
class StyleStructRef<T, M> private constructor(private var state: State<T, M>)
        where T : StyleStruct<M>, M : MutStyleStruct {

    fun mutate(): M {
        val (value, state) = state.mutate()

        this.state = state

        return value
    }

    fun take(): M {
        val (value, state) = state.mutate()

        this.state = state

        return value
    }

    fun put(styleStruct: M) {
        state = state.put(styleStruct)
    }

    fun build(): T {
        return state.build()
    }

    private sealed class State<T : StyleStruct<M>, M : MutStyleStruct> {

        abstract fun mutate(): Pair<M, State<T, M>>

        abstract fun take(): Pair<M, State<T, M>>

        abstract fun put(value: M): State<T, M>

        abstract fun build(): T

        class Owned<T : StyleStruct<M>, M : MutStyleStruct>(val value: M) : State<T, M>() {
            override fun mutate(): Pair<M, State<T, M>> {
                return Pair(value, this)
            }

            override fun take(): Pair<M, State<T, M>> {
                return Pair(value, State.Vacated())
            }

            override fun put(value: M): State<T, M> {
                throw IllegalStateException("not vacated")
            }

            override fun build(): T {
                @Suppress("UNCHECKED_CAST")
                return value as T
            }
        }

        class Borrowed<T : StyleStruct<M>, M : MutStyleStruct>(val value: T) : State<T, M>() {
            override fun mutate(): Pair<M, State<T, M>> {
                val mut = value.clone()

                return Pair(mut, State.Owned(mut))
            }

            override fun take(): Pair<M, State<T, M>> {
                return Pair(value.clone(), State.Vacated())
            }

            override fun put(value: M): State<T, M> {
                throw IllegalStateException("not vacated")
            }

            override fun build(): T {
                return value
            }
        }

        class Vacated<T : StyleStruct<M>, M : MutStyleStruct> : State<T, M>() {
            override fun mutate(): Pair<M, State<T, M>> {
                throw IllegalStateException("vacated")
            }

            override fun take(): Pair<M, State<T, M>> {
                throw IllegalStateException("vacated")
            }

            override fun put(value: M): State<T, M> {
                return State.Owned(value)
            }

            override fun build(): T {
                throw IllegalStateException("vacated")
            }
        }
    }

    companion object {

        fun <T, M> owned(styleStruct: M): StyleStructRef<T, M>
                where T : StyleStruct<M>, M : MutStyleStruct {
            return StyleStructRef(State.Owned(styleStruct))
        }

        fun <T, M> borrowed(styleStruct: T): StyleStructRef<T, M>
                where T : StyleStruct<M>, M : MutStyleStruct {
            return StyleStructRef(State.Borrowed(styleStruct))
        }

        fun <T, M> vacated(): StyleStructRef<T, M>
                where T : StyleStruct<M>, M : MutStyleStruct {
            return StyleStructRef(State.Vacated())
        }
    }
}

class StyleBuilder(
    val device: Device,
    val ruleNode: RuleNode,

    val customProperties: CustomPropertiesList?,
    val properties: PropertiesList,

    private val inheritStyle: ComputedValues,
    private val inheritStyleIgnoringFirstLine: ComputedValues,
    private val resetStyle: ComputedValues,

    private val font: StyleStructRef<Font, MutFont>,
    private val color: StyleStructRef<Color, MutColor>,
    private val background: StyleStructRef<Background, MutBackground>,
    private val border: StyleStructRef<Border, MutBorder>,
    private val margin: StyleStructRef<Margin, MutMargin>,
    private val padding: StyleStructRef<Padding, MutPadding>,
) {

    companion object {

        fun from(
            device: Device,

            customProperties: CustomPropertiesList?,
            properties: PropertiesList,

            ruleNode: RuleNode,

            parentStyle: ComputedValues?,
            parentStyleIgnoringFirstLine: ComputedValues?,
        ): StyleBuilder {
            val resetStyle = device.defaultComputedValues()
            val inheritStyle = parentStyle ?: resetStyle
            val inheritStyleIgnoringFirstList = parentStyleIgnoringFirstLine ?: resetStyle

            return StyleBuilder(
                device,
                ruleNode,

                customProperties,
                properties,

                inheritStyle,
                inheritStyleIgnoringFirstList,
                resetStyle,

                StyleStructRef.borrowed(inheritStyle.font),
                StyleStructRef.borrowed(inheritStyle.color),
                StyleStructRef.borrowed(resetStyle.background),
                StyleStructRef.borrowed(resetStyle.border),
                StyleStructRef.borrowed(resetStyle.margin),
                StyleStructRef.borrowed(resetStyle.padding)
            )
        }
    }


    // *****************************************************
    //  Font
    // *****************************************************

    fun getFont(): Font {
        return font.build()
    }

    fun getParentFont(): Font {
        return inheritStyle.font
    }

    // font-family

    fun setFontFamily(fontFamily: FontFamily) {
        font.mutate().fontFamily = fontFamily
    }

    fun inheritFontFamily() {
        val inheritStruct = inheritStyle.font

        font.mutate().fontFamily = inheritStruct.fontFamily
    }

    fun resetFontFamily() {
        val resetStruct = resetStyle.font

        font.mutate().fontFamily = resetStruct.fontFamily
    }

    // font-size

    fun setFontSize(fontSize: FontSize) {
        font.mutate().fontSize = fontSize
    }

    fun inheritFontSize() {
        val inheritStruct = inheritStyle.font

        font.mutate().fontSize = inheritStruct.fontSize
    }

    fun resetFontSize() {
        val resetStruct = resetStyle.font

        font.mutate().fontSize = resetStruct.fontSize
    }

    // font-weight

    fun setFontWeight(fontWeight: FontWeight) {
        font.mutate().fontWeight = fontWeight
    }

    fun inheritFontWeight() {
        val inheritStruct = inheritStyle.font

        font.mutate().fontWeight = inheritStruct.fontWeight
    }

    fun resetFontWeight() {
        val resetStruct = resetStyle.font

        font.mutate().fontWeight = resetStruct.fontWeight
    }

    // *****************************************************
    // Color
    // *****************************************************

    fun getColor(): Color {
        return color.build()
    }

    fun getParentColor(): Color {
        return inheritStyle.color
    }

    // color

    fun setColor(color: RGBA) {
        this.color.mutate().color = color
    }

    fun inheritColor() {
        val inheritStruct = inheritStyle.color

        this.color.mutate().color = inheritStruct.color
    }

    fun resetColor() {
        val resetStruct = resetStyle.color

        this.color.mutate().color = resetStruct.color
    }

    // fill

    fun setFill(fill: Fill) {
        this.color.mutate().fill = fill
    }

    fun inheritFill() {
        val inheritStruct = inheritStyle.color

        this.color.mutate().fill = inheritStruct.fill
    }

    fun resetFill() {
        val resetStruct = resetStyle.color

        this.color.mutate().fill = resetStruct.fill
    }

    // *****************************************************
    // Background
    // *****************************************************

    fun getBackground(): Background {
        return background.build()
    }

    fun getParentBackground(): Background {
        return inheritStyleIgnoringFirstLine.background
    }

    // background-color

    fun setBackgroundColor(color: ComputedColor) {
        background.mutate().color = color
    }

    fun inheritBackgroundColor() {
        val inheritStruct = inheritStyleIgnoringFirstLine.background

        background.mutate().color = inheritStruct.color
    }

    fun resetBackgroundColor() {
        val resetStruct = resetStyle.background

        background.mutate().color = resetStruct.color
    }

    // background-image

    fun setBackgroundImage(image: List<ImageLayer>) {
        background.mutate().image = image
    }

    fun inheritBackgroundImage() {
        val inheritStruct = inheritStyleIgnoringFirstLine.background

        background.mutate().image = inheritStruct.image
    }

    fun resetBackgroundImage() {
        val resetStruct = resetStyle.background

        background.mutate().image = resetStruct.image
    }

    // background-attachment

    fun setBackgroundAttachment(attachment: List<Attachment>) {
        background.mutate().attachment = attachment
    }

    fun inheritBackgroundAttachment() {
        val inheritStruct = inheritStyleIgnoringFirstLine.background

        background.mutate().attachment = inheritStruct.attachment
    }

    fun resetBackgroundAttachment() {
        val resetStruct = resetStyle.background

        background.mutate().attachment = resetStruct.attachment
    }

    // background-position-x

    fun setBackgroundPositionX(positionX: List<HorizontalPosition>) {
        background.mutate().positionX = positionX
    }

    fun inheritBackgroundPositionX() {
        val inheritStruct = inheritStyleIgnoringFirstLine.background

        background.mutate().positionX = inheritStruct.positionX
    }

    fun resetBackgroundPositionX() {
        val resetStruct = resetStyle.background

        background.mutate().positionX = resetStruct.positionX
    }

    // background-position-y

    fun setBackgroundPositionY(positionY: List<VerticalPosition>) {
        background.mutate().positionY = positionY
    }

    fun inheritBackgroundPositionY() {
        val inheritStruct = inheritStyleIgnoringFirstLine.background

        background.mutate().positionY = inheritStruct.positionY
    }

    fun resetBackgroundPositionY() {
        val resetStruct = resetStyle.background

        background.mutate().positionY = resetStruct.positionY
    }

    // background-size

    fun setBackgroundSize(size: List<BackgroundSize>) {
        background.mutate().size = size
    }

    fun inheritBackgroundSize() {
        val inheritStruct = inheritStyleIgnoringFirstLine.background

        background.mutate().size = inheritStruct.size
    }

    fun resetBackgroundSize() {
        val resetStruct = resetStyle.background

        background.mutate().size = resetStruct.size
    }

    // background-repeat

    fun setBackgroundRepeat(repeat: List<BackgroundRepeat>) {
        background.mutate().repeat = repeat
    }

    fun inheritBackgroundRepeat() {
        val inheritStruct = inheritStyleIgnoringFirstLine.background

        background.mutate().repeat = inheritStruct.repeat
    }

    fun resetBackgroundRepeat() {
        val resetStruct = resetStyle.background

        background.mutate().repeat = resetStruct.repeat
    }

    // background-origin

    fun setBackgroundOrigin(origin: List<Origin>) {
        background.mutate().origin = origin
    }

    fun inheritBackgroundOrigin() {
        val inheritStruct = inheritStyleIgnoringFirstLine.background

        background.mutate().origin = inheritStruct.origin
    }

    fun resetBackgroundOrigin() {
        val resetStruct = resetStyle.background

        background.mutate().origin = resetStruct.origin
    }

    // background-clip

    fun setBackgroundClip(clip: Clip) {
        background.mutate().clip = clip
    }

    fun inheritBackgroundClip() {
        val inheritStruct = inheritStyleIgnoringFirstLine.background

        background.mutate().clip = inheritStruct.clip
    }

    fun resetBackgroundClip() {
        val resetStruct = resetStyle.background

        background.mutate().clip = resetStruct.clip
    }

    // *****************************************************
    // Border
    // *****************************************************

    // border-top-width

    fun setBorderTopWidth(width: NonNegativeLength) {
        border.mutate().topWidth = width
    }

    fun inheritBorderTopWidth() {
        val inheritStruct = inheritStyleIgnoringFirstLine.border

        border.mutate().topWidth = inheritStruct.topWidth
    }

    fun resetBorderTopWidth() {
        val resetStruct = resetStyle.border

        border.mutate().topWidth = resetStruct.topWidth
    }

    // border-top-color

    fun setBorderTopColor(color: ComputedColor) {
        border.mutate().topColor = color
    }

    fun inheritBorderTopColor() {
        val inheritStruct = inheritStyleIgnoringFirstLine.border

        border.mutate().topColor = inheritStruct.topColor
    }

    fun resetBorderTopColor() {
        val resetStruct = resetStyle.border

        border.mutate().topColor = resetStruct.topColor
    }

    // border-top-style

    fun setBorderTopStyle(style: BorderStyle) {
        border.mutate().topStyle = style
    }

    fun inheritBorderTopStyle() {
        val inheritStruct = inheritStyleIgnoringFirstLine.border

        border.mutate().topStyle = inheritStruct.topStyle
    }

    fun resetBorderTopStyle() {
        val resetStruct = resetStyle.border

        border.mutate().topStyle = resetStruct.topStyle
    }

    // border-top-left-radius

    fun setBorderTopLeftRadius(radius: BorderCornerRadius) {
        border.mutate().topLeftRadius = radius
    }

    fun inheritBorderTopLeftRadius() {
        val inheritStruct = inheritStyleIgnoringFirstLine.border

        border.mutate().topLeftRadius = inheritStruct.topLeftRadius
    }

    fun resetBorderTopLeftRadius() {
        val resetStruct = resetStyle.border

        border.mutate().topLeftRadius = resetStruct.topLeftRadius
    }

    // border-top-left-radius

    fun setBorderTopRightRadius(radius: BorderCornerRadius) {
        border.mutate().topRightRadius = radius
    }

    fun inheritBorderTopRightRadius() {
        val inheritStruct = inheritStyleIgnoringFirstLine.border

        border.mutate().topRightRadius = inheritStruct.topRightRadius
    }

    fun resetBorderTopRightRadius() {
        val resetStruct = resetStyle.border

        border.mutate().topRightRadius = resetStruct.topRightRadius
    }

    // border-right-width

    fun setBorderRightWidth(width: NonNegativeLength) {
        border.mutate().rightWidth = width
    }

    fun inheritBorderRightWidth() {
        val inheritStruct = inheritStyleIgnoringFirstLine.border

        border.mutate().rightWidth = inheritStruct.rightWidth
    }

    fun resetBorderRightWidth() {
        val resetStruct = resetStyle.border

        border.mutate().rightWidth = resetStruct.rightWidth
    }

    // border-right-color

    fun setBorderRightColor(color: ComputedColor) {
        border.mutate().rightColor = color
    }

    fun inheritBorderRightColor() {
        val inheritStruct = inheritStyleIgnoringFirstLine.border

        border.mutate().rightColor = inheritStruct.rightColor
    }

    fun resetBorderRightColor() {
        val resetStruct = resetStyle.border

        border.mutate().rightColor = resetStruct.rightColor
    }

    // border-right-style

    fun setBorderRightStyle(style: BorderStyle) {
        border.mutate().rightStyle = style
    }

    fun inheritBorderRightStyle() {
        val inheritStruct = inheritStyleIgnoringFirstLine.border

        border.mutate().rightStyle = inheritStruct.rightStyle
    }

    fun resetBorderRightStyle() {
        val resetStruct = resetStyle.border

        border.mutate().rightStyle = resetStruct.rightStyle
    }

    // border-bottom-width

    fun setBorderBottomWidth(width: NonNegativeLength) {
        border.mutate().bottomWidth = width
    }

    fun inheritBorderBottomWidth() {
        val inheritStruct = inheritStyleIgnoringFirstLine.border

        border.mutate().bottomWidth = inheritStruct.bottomWidth
    }

    fun resetBorderBottomWidth() {
        val resetStruct = resetStyle.border

        border.mutate().bottomWidth = resetStruct.bottomWidth
    }

    // border-bottom-color

    fun setBorderBottomColor(color: ComputedColor) {
        border.mutate().bottomColor = color
    }

    fun inheritBorderBottomColor() {
        val inheritStruct = inheritStyleIgnoringFirstLine.border

        border.mutate().bottomColor = inheritStruct.bottomColor
    }

    fun resetBorderBottomColor() {
        val resetStruct = resetStyle.border

        border.mutate().bottomColor = resetStruct.bottomColor
    }

    // border-bottom-style

    fun setBorderBottomStyle(style: BorderStyle) {
        border.mutate().bottomStyle = style
    }

    fun inheritBorderBottomStyle() {
        val inheritStruct = inheritStyleIgnoringFirstLine.border

        border.mutate().bottomStyle = inheritStruct.bottomStyle
    }

    fun resetBorderBottomStyle() {
        val resetStruct = resetStyle.border

        border.mutate().bottomStyle = resetStruct.bottomStyle
    }

    // border-top-left-radius

    fun setBorderBottomLeftRadius(radius: BorderCornerRadius) {
        border.mutate().bottomLeftRadius = radius
    }

    fun inheritBorderBottomLeftRadius() {
        val inheritStruct = inheritStyleIgnoringFirstLine.border

        border.mutate().bottomLeftRadius = inheritStruct.bottomLeftRadius
    }

    fun resetBorderBottomLeftRadius() {
        val resetStruct = resetStyle.border

        border.mutate().bottomLeftRadius = resetStruct.bottomLeftRadius
    }

    // border-top-left-radius

    fun setBorderBottomRightRadius(radius: BorderCornerRadius) {
        border.mutate().bottomRightRadius = radius
    }

    fun inheritBorderBottomRightRadius() {
        val inheritStruct = inheritStyleIgnoringFirstLine.border

        border.mutate().bottomRightRadius = inheritStruct.bottomRightRadius
    }

    fun resetBorderBottomRightRadius() {
        val resetStruct = resetStyle.border

        border.mutate().bottomRightRadius = resetStruct.bottomRightRadius
    }

    // border-left-width

    fun setBorderLeftWidth(width: NonNegativeLength) {
        border.mutate().leftWidth = width
    }

    fun inheritBorderLeftWidth() {
        val inheritStruct = inheritStyleIgnoringFirstLine.border

        border.mutate().leftWidth = inheritStruct.leftWidth
    }

    fun resetBorderLeftWidth() {
        val resetStruct = resetStyle.border

        border.mutate().leftWidth = resetStruct.leftWidth
    }

    // border-left-color

    fun setBorderLeftColor(color: ComputedColor) {
        border.mutate().leftColor = color
    }

    fun inheritBorderLeftColor() {
        val inheritStruct = inheritStyleIgnoringFirstLine.border

        border.mutate().leftColor = inheritStruct.leftColor
    }

    fun resetBorderLeftColor() {
        val resetStruct = resetStyle.border

        border.mutate().leftColor = resetStruct.leftColor
    }

    // border-bottom-style

    fun setBorderLeftStyle(style: BorderStyle) {
        border.mutate().leftStyle = style
    }

    fun inheritBorderLeftStyle() {
        val inheritStruct = inheritStyleIgnoringFirstLine.border

        border.mutate().leftStyle = inheritStruct.leftStyle
    }

    fun resetBorderLeftStyle() {
        val resetStruct = resetStyle.border

        border.mutate().leftStyle = resetStruct.leftStyle
    }

    // *****************************************************
    //  Margin
    // *****************************************************

    // margin-top

    fun setMarginTop(length: LengthOrPercentageOrAuto) {
        margin.mutate().top = length
    }

    fun inheritMarginTop() {
        val inheritStruct = inheritStyleIgnoringFirstLine.margin

        margin.mutate().top = inheritStruct.top
    }

    fun resetMarginTop() {
        val resetStruct = resetStyle.margin

        margin.mutate().top = resetStruct.top
    }

    // margin-top

    fun setMarginRight(length: LengthOrPercentageOrAuto) {
        margin.mutate().right = length
    }

    fun inheritMarginRight() {
        val inheritStruct = inheritStyleIgnoringFirstLine.margin

        margin.mutate().right = inheritStruct.right
    }

    fun resetMarginRight() {
        val resetStruct = resetStyle.margin

        margin.mutate().right = resetStruct.right
    }

    // margin-bottom

    fun setMarginBottom(length: LengthOrPercentageOrAuto) {
        margin.mutate().bottom = length
    }

    fun inheritMarginBottom() {
        val inheritStruct = inheritStyleIgnoringFirstLine.margin

        margin.mutate().bottom = inheritStruct.bottom
    }

    fun resetMarginBottom() {
        val resetStruct = resetStyle.margin

        margin.mutate().bottom = resetStruct.bottom
    }

    // margin-left

    fun setMarginLeft(length: LengthOrPercentageOrAuto) {
        margin.mutate().left = length
    }

    fun inheritMarginLeft() {
        val inheritStruct = inheritStyleIgnoringFirstLine.margin

        margin.mutate().left = inheritStruct.left
    }

    fun resetMarginLeft() {
        val resetStruct = resetStyle.margin

        margin.mutate().left = resetStruct.left
    }

    // *****************************************************
    //  Margin
    // *****************************************************

    // padding-top

    fun setPaddingTop(length: NonNegativeLengthOrPercentage) {
        padding.mutate().top = length
    }

    fun inheritPaddingTop() {
        val inheritStruct = inheritStyleIgnoringFirstLine.padding

        padding.mutate().top = inheritStruct.top
    }

    fun resetPaddingTop() {
        val resetStruct = resetStyle.padding

        padding.mutate().top = resetStruct.top
    }

    // padding-right

    fun setPaddingRight(length: NonNegativeLengthOrPercentage) {
        padding.mutate().right = length
    }

    fun inheritPaddingRight() {
        val inheritStruct = inheritStyleIgnoringFirstLine.padding

        padding.mutate().right = inheritStruct.right
    }

    fun resetPaddingRight() {
        val resetStruct = resetStyle.padding

        padding.mutate().right = resetStruct.right
    }

    // padding-bottom

    fun setPaddingBottom(length: NonNegativeLengthOrPercentage) {
        padding.mutate().bottom = length
    }

    fun inheritPaddingBottom() {
        val inheritStruct = inheritStyleIgnoringFirstLine.padding

        padding.mutate().bottom = inheritStruct.bottom
    }

    fun resetPaddingBottom() {
        val resetStruct = resetStyle.padding

        padding.mutate().bottom = resetStruct.bottom
    }

    // padding-bottom

    fun setPaddingLeft(length: NonNegativeLengthOrPercentage) {
        padding.mutate().left = length
    }

    fun inheritPaddingLeft() {
        val inheritStruct = inheritStyleIgnoringFirstLine.padding

        padding.mutate().left = inheritStruct.left
    }

    fun resetPaddingLeft() {
        val resetStruct = resetStyle.padding

        padding.mutate().left = resetStruct.left
    }

    fun build(): ComputedValues {
        return ComputedValues(
            font.build(),
            color.build(),
            background.build(),
            border.build(),
            margin.build(),
            padding.build(),
            customProperties,
            properties,
            ruleNode,
        )
    }
}
