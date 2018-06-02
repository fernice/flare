package de.krall.flare.css.value.computed

import de.krall.flare.css.parser.ClampingMode
import de.krall.flare.css.value.ComputedValue
import de.krall.flare.std.None
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

    fun toUsedValue(containingLength: Option<Au>): Option<Au> {
        return toPixelLength(containingLength).map(Au.Companion::from)
    }

    fun toPixelLength(containingLength: Option<Au>): Option<PixelLength> {
        return if (percentage is Some && containingLength is Some) {
            val value = length.px() + containingLength.value.scaleBy(percentage.value.value).toFloat()

            Some(PixelLength(clampingMode.clamp(value)))
        } else if (percentage.isNone()) {
            Some(length)
        } else {
            None()
        }
    }
}