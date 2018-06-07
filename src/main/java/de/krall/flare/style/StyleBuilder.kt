package de.krall.flare.style

import de.krall.flare.dom.Device
import de.krall.flare.font.WritingMode
import de.krall.flare.std.Option
import de.krall.flare.std.unwrapOr
import de.krall.flare.style.properties.longhand.Attachment
import de.krall.flare.style.properties.stylestruct.Background
import de.krall.flare.style.properties.stylestruct.Font
import de.krall.flare.style.properties.stylestruct.MutBackground
import de.krall.flare.style.properties.stylestruct.MutFont
import de.krall.flare.style.value.computed.Color
import de.krall.flare.style.value.computed.FontFamily
import de.krall.flare.style.value.computed.FontSize

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

class StyleBuilder(val device: Device,
                   var writingMode: WritingMode,
                   private val inheritStyle: ComputedValues,
                   private val inheritStyleIgnoringFirstLine: ComputedValues,
                   private val resetStyle: ComputedValues,
                   private val background: StyleStructRef<Background, MutBackground>,
                   private val font: StyleStructRef<Font, MutFont>) {

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
                    StyleStructRef.borrowed(resetStyle.background),
                    StyleStructRef.borrowed(inheritStyle.font)
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
        return inheritStyleIgnoringFirstLine.background
    }

    // background-color

    fun setBackgroundColor(color: Color) {
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

    fun build(): ComputedValues {
        return ComputedValues(
                background.build(),
                font.build()
        )
    }
}