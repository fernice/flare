package de.krall.flare.css

import de.krall.flare.css.properties.longhand.Attachment
import de.krall.flare.css.properties.stylestruct.Background
import de.krall.flare.css.value.computed.Color
import de.krall.flare.dom.Device
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

class StyleBuilder(private val device: Device,
                   private val inheritStyle: ComputedValues,
                   private val inheritStyleIgnoringFirstLine: ComputedValues,
                   private val resetStyle: ComputedValues,
                   private val background: StyleStructRef<Background>) {

    companion object {

        fun new(device: Device,
                parentStyle: Option<ComputedValues>,
                parentStyleIgnoringFirstLine: Option<ComputedValues>): StyleBuilder {
            val resetStyle = device.defaultComputedValues()
            val inheritStyle = parentStyle.unwrapOr(resetStyle)
            val inheritStyleIgnoringFirstList = parentStyleIgnoringFirstLine.unwrapOr(resetStyle)

            return StyleBuilder(
                    device,
                    inheritStyle,
                    inheritStyleIgnoringFirstList,
                    resetStyle,
                    StyleStructRef.borrowed(resetStyle.getBackground())
            )
        }
    }

    // *****************************************************
    // background-color
    // *****************************************************

    fun setBackgroundColor(color: Color) {
        background.mutate().setColor(color)
    }

    fun inheritBackgroundColor() {
        val inheritStruct = inheritStyle.getBackground()

        background.mutate().setColor(inheritStruct.getColor())
    }

    fun resetBackgroundColor() {
        val resetStruct = resetStyle.getBackground()

        background.mutate().setColor(resetStruct.getColor())
    }

    // *****************************************************
    // background-attachment
    // *****************************************************

    fun setBackgroundAttachment(attachment: List<Attachment>) {
        background.mutate().setAttachment(attachment)
    }

    fun inheritBackgroundAttachment() {
        val inheritStruct = inheritStyle.getBackground()

        background.mutate().setColor(inheritStruct.getColor())
    }

    fun resetBackgroundAttachment() {
        val resetStruct = resetStyle.getBackground()

        background.mutate().setColor(resetStruct.getColor())
    }

    fun build(): ComputedValues {
        return ComputedValues(background.build())
    }
}