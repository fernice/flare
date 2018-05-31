package de.krall.flare.css.value.computed

import de.krall.flare.css.value.ClampingMode
import de.krall.flare.css.value.ComputedValue
import de.krall.flare.std.Option
import de.krall.flare.std.Some

class PixelLength(val value: Float) : ComputedValue {

    fun px(): Float {
        return value
    }

    companion object {

        private val zero: PixelLength by lazy { PixelLength(0f) }

        fun zero(): PixelLength {
            return zero
        }
    }
}

sealed class Length {

}

class Percentage(val value: Float)

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

class Au(val value: Int) {

    fun toPx(): PixelLength {
        return PixelLength((value / AU_PER_PX).toFloat())
    }

    operator fun plus(au: Au): Au {
        return Au(value + au.value)
    }

    operator fun minus(au: Au): Au {
        return Au(value - au.value)
    }

    operator fun times(au: Au): Au {
        return Au(value * au.value)
    }

    operator fun div(au: Au): Au {
        return Au(value / au.value)
    }

    fun max(au: Au): Au {
        return if (value >= au.value) {
            this
        } else {
            au
        }
    }

    fun min(au: Au): Au {
        return if (value <= au.value) {
            this
        } else {
            au
        }
    }

    companion object {

        fun fromAu64(double: Double): Au {
            return Au(double.toInt())
        }

        private const val AU_PER_PX = 60
    }
}