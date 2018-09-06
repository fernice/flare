package de.krall.flare.style.value.computed

import de.krall.flare.style.parser.ClampingMode
import de.krall.flare.style.value.ComputedValue
import modern.std.None
import modern.std.Option
import modern.std.Some

data class CalcLengthOrPercentage(val clampingMode: ClampingMode,
                             val length: PixelLength,
                             val percentage: Option<Percentage>) : ComputedValue {

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
            None
        }
    }

    companion object {

        fun new(length: Length, percentage: Option<Percentage>): CalcLengthOrPercentage {
            return withClampingMode(length, percentage, ClampingMode.All)
        }

        fun withClampingMode(
                length: Length,
                percentage: Option<Percentage>,
                clampingMode: ClampingMode
        ): CalcLengthOrPercentage {
            return CalcLengthOrPercentage(
                    clampingMode,
                    length,
                    percentage
            )
        }
    }
}