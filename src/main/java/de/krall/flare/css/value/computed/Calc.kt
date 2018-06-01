package de.krall.flare.css.value.computed

import de.krall.flare.css.parser.ClampingMode
import de.krall.flare.css.value.ComputedValue
import de.krall.flare.std.Option
import de.krall.flare.std.Some

class CalcLengthOrPercentage(private val clampingMode: ClampingMode,
                             private val length: PixelLength,
                             private val percentage: Option<Percentage>) : ComputedValue {

    fun length(): PixelLength {
        if (percentage is Some) {
            throw IllegalStateException()
        }

        return lengthComponent()
    }

    fun lengthComponent(): PixelLength {
        return PixelLength((clampingMode.clamp(length.px())))
    }

    fun unclampedLength(): PixelLength {
        return length
    }
}