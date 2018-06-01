package de.krall.flare.css.value.computed

import de.krall.flare.css.parser.ClampingMode
import de.krall.flare.css.value.ComputedValue
import de.krall.flare.std.Option
import de.krall.flare.std.Some

data class PixelLength(val value: Float) : ComputedValue {

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

class NonNegativeLength(val length: PixelLength) : ComputedValue

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

class Percentage(val value: Float) : ComputedValue

sealed class LengthOrPercentage : ComputedValue {

    data class Length(val length: PixelLength) : LengthOrPercentage()

    data class Percentage(val percentage: de.krall.flare.css.value.computed.Percentage) : LengthOrPercentage()

    data class Calc(val calc: CalcLengthOrPercentage) : LengthOrPercentage()
}

class NonNegativeLengthOrPercentage(val value: LengthOrPercentage) : ComputedValue

sealed class LengthOrPercentageOrAuto : ComputedValue {

    data class Length(val length: PixelLength) : LengthOrPercentageOrAuto()

    data class Percentage(val percentage: de.krall.flare.css.value.computed.Percentage) : LengthOrPercentageOrAuto()

    data class Calc(val calc: CalcLengthOrPercentage) : LengthOrPercentageOrAuto()

    class Auto : LengthOrPercentageOrAuto()
}

class NonNegativeLengthOrPercentageOrAuto(val value: LengthOrPercentageOrAuto) : ComputedValue

sealed class LengthOrPercentageOrNone : ComputedValue {

    data class Length(val length: PixelLength) : LengthOrPercentageOrNone()

    data class Percentage(val percentage: de.krall.flare.css.value.computed.Percentage) : LengthOrPercentageOrNone()

    data class Calc(val calc: CalcLengthOrPercentage) : LengthOrPercentageOrNone()

    class None : LengthOrPercentageOrNone()
}

class NonNegativeLengthOrPercentageOrNone(val value: LengthOrPercentageOrNone) : ComputedValue