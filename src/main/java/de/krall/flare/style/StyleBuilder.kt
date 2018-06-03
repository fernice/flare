package de.krall.flare.style

import de.krall.flare.style.properties.longhand.Attachment
import de.krall.flare.style.properties.stylestruct.Background
import de.krall.flare.style.properties.stylestruct.Font
import de.krall.flare.style.value.computed.Color
import de.krall.flare.style.value.computed.FontFamily
import de.krall.flare.style.value.computed.FontSize
import de.krall.flare.dom.Device
import de.krall.flare.font.WritingMode
import de.krall.flare.std.None
import de.krall.flare.std.Option
import de.krall.flare.std.Some
import de.krall.flare.std.unwrapOr

interface StyleStruct<T> {

    fun clone(): T
}

class StyleStructRef<T : StyleStruct<T>> private constructor(private var styleStruct: T,
                                                             private var state: State) {

    fun mutate(): T {
        return when (state) {
            State.OWNED -> styleStruct
            State.BORROWED -> {
                styleStruct = styleStruct.clone()
                state = State.OWNED

                styleStruct
            }
            State.VACATED -> throw IllegalStateException("vacated")
        }
    }

    fun take(): T {
        return when (state) {
            State.OWNED -> {
                state = State.VACATED

                styleStruct
            }
            State.BORROWED -> {
                styleStruct = styleStruct.clone()
                state = State.VACATED

                styleStruct
            }
            State.VACATED -> throw IllegalStateException("vacated")
        }
    }

    fun put(styleStruct: T) {
        when (state) {
            State.OWNED, State.BORROWED -> throw IllegalStateException("not vacated")
            State.VACATED -> {
                this.styleStruct = styleStruct
                state = State.OWNED
            }
        }
    }

    fun getIfMutated(): Option<T> {
        return when (state) {
            State.OWNED -> Some(styleStruct)
            State.BORROWED -> None()
            State.VACATED -> throw IllegalStateException("vacated")
        }
    }

    fun build(): T {
        return when (state) {
            State.OWNED -> styleStruct
            State.BORROWED -> styleStruct
            State.VACATED -> throw IllegalStateException("vacated")
        }
    }

    private enum class State {

        OWNED, BORROWED, VACATED
    }

    companion object {

        fun <T : StyleStruct<T>> owned(styleStruct: T): StyleStructRef<T> {
            return StyleStructRef(styleStruct, State.OWNED)
        }

        fun <T : StyleStruct<T>> borrowed(styleStruct: T): StyleStructRef<T> {
            return StyleStructRef(styleStruct, State.BORROWED)
        }

        fun <T : StyleStruct<T>> vacated(styleStruct: T): StyleStructRef<T> {
            return StyleStructRef(styleStruct, State.VACATED)
        }
    }
}

class StyleBuilder(val device: Device,
                   var writingMode: WritingMode,
                   private val inheritStyle: ComputedValues,
                   private val inheritStyleIgnoringFirstLine: ComputedValues,
                   private val resetStyle: ComputedValues,
                   private val background: StyleStructRef<Background>,
                   private val font: StyleStructRef<Font>) {

    companion object {

        fun new(device: Device,
                writingMode: WritingMode,
                parentStyle: Option<ComputedValues>,
                parentStyleIgnoringFirstLine: Option<ComputedValues>): StyleBuilder {
            val resetStyle = device.defaultComputedValues()
            val inheritStyle = parentStyle.unwrapOr(resetStyle)
            val inheritStyleIgnoringFirstList = parentStyleIgnoringFirstLine.unwrapOr(resetStyle)

            return StyleBuilder(
                    device,
                    writingMode,
                    inheritStyle,
                    inheritStyleIgnoringFirstList,
                    resetStyle,
                    StyleStructRef.borrowed(resetStyle.getBackground()),
                    StyleStructRef.borrowed(inheritStyle.getFont())
            )
        }
    }

    // *****************************************************
    // Background
    // *****************************************************

    fun getBackground(): Background {
        return background.build()
    }

    fun getParentBackground(): Background {
        return inheritStyleIgnoringFirstLine.getBackground()
    }

    // background-color

    fun setBackgroundColor(color: Color) {
        background.mutate().setColor(color)
    }

    fun inheritBackgroundColor() {
        val inheritStruct = inheritStyleIgnoringFirstLine.getBackground()

        background.mutate().setColor(inheritStruct.getColor())
    }

    fun resetBackgroundColor() {
        val resetStruct = resetStyle.getBackground()

        background.mutate().setColor(resetStruct.getColor())
    }

    // background-attachment

    fun setBackgroundAttachment(attachment: List<Attachment>) {
        background.mutate().setAttachment(attachment)
    }

    fun inheritBackgroundAttachment() {
        val inheritStruct = inheritStyleIgnoringFirstLine.getBackground()

        background.mutate().setColor(inheritStruct.getColor())
    }

    fun resetBackgroundAttachment() {
        val resetStruct = resetStyle.getBackground()

        background.mutate().setColor(resetStruct.getColor())
    }

    // *****************************************************
    //  Font
    // *****************************************************

    fun getFont(): Font {
        return font.build()
    }

    fun getParentFont(): Font {
        return inheritStyle.getFont()
    }

    // font-family

    fun setFontFamily(fontFamily: FontFamily) {
        font.mutate().setFontFamily(fontFamily)
    }

    fun inheritFontFamily() {
        val inheritStruct = inheritStyle.getFont()

        font.mutate().setFontFamily(inheritStruct.getFontFamily())
    }

    fun resetFontFamily() {
        val resetStruct = resetStyle.getFont()

        font.mutate().setFontFamily(resetStruct.getFontFamily())
    }

    // font-size

    fun setFontSize(fontSize: FontSize) {
        font.mutate().setFontSize(fontSize)
    }

    fun inheritFontSize() {
        val inheritStruct = inheritStyle.getFont()

        font.mutate().setFontSize(inheritStruct.getFontSize())
    }

    fun resetFontSize() {
        val resetStruct = resetStyle.getFont()

        font.mutate().setFontSize(resetStruct.getFontSize())
    }

    fun build(): ComputedValues {
        return ComputedValues(
                background.build(),
                font.build()
        )
    }
}