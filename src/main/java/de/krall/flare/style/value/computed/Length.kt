package de.krall.flare.style.value.computed

import de.krall.flare.style.value.ComputedValue
import de.krall.flare.std.max

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

fun PixelLength.into(): Au {
    return Au.fromPx(this.value)
}

fun Au.into(): PixelLength {
    return PixelLength(this.toPx())
}

class NonNegativeLength(val length: PixelLength) : ComputedValue {

    companion object {

        fun new(px: Float): NonNegativeLength {
            return NonNegativeLength(PixelLength(px.max(0f)))
        }
    }

    fun scaleBy(factor: Float): NonNegativeLength {
        return new(length.px() * factor.max(0f))
    }

    operator fun plus(other: NonNegativeLength): NonNegativeLength {
        return new(length.px() + other.length.px())
    }
}

fun NonNegativeLength.into(): Au {
    return this.length.into()
}

fun Au.intoNonNegative(): NonNegativeLength {
    return NonNegativeLength(this.into())
}

fun PixelLength.intoNonNegative(): NonNegativeLength {
    return NonNegativeLength(this)
}

class Au(val value: Int) {

    fun toPx(): Float {
        return value / AU_PER_PX.toFloat()
    }

    fun scaleBy(float: Float): Au {
        return Au((value * float).toInt())
    }

    fun toFloat(): Float {
        return value / AU_PER_PX.toFloat()
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

    operator fun plus(scalar: Int): Au {
        return Au(value + scalar)
    }

    operator fun minus(scalar: Int): Au {
        return Au(value - scalar)
    }

    operator fun times(scalar: Int): Au {
        return Au(value * scalar)
    }

    operator fun div(scalar: Int): Au {
        return Au(value / scalar)
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

        fun fromPx(px: Float): Au {
            return Au((px * AU_PER_PX).toInt())
        }

        fun fromPx(px: Int): Au {
            return Au((px * AU_PER_PX))
        }

        fun from(px: PixelLength): Au {
            return fromPx(px.value)
        }

        private const val AU_PER_PX = 60
    }
}

class Percentage(val value: Float) : ComputedValue

sealed class LengthOrPercentage : ComputedValue {

    data class Length(val length: PixelLength) : LengthOrPercentage()

    data class Percentage(val percentage: de.krall.flare.style.value.computed.Percentage) : LengthOrPercentage()

    data class Calc(val calc: CalcLengthOrPercentage) : LengthOrPercentage()
}

class NonNegativeLengthOrPercentage(val value: LengthOrPercentage) : ComputedValue

sealed class LengthOrPercentageOrAuto : ComputedValue {

    data class Length(val length: PixelLength) : LengthOrPercentageOrAuto()

    data class Percentage(val percentage: de.krall.flare.style.value.computed.Percentage) : LengthOrPercentageOrAuto()

    data class Calc(val calc: CalcLengthOrPercentage) : LengthOrPercentageOrAuto()

    class Auto : LengthOrPercentageOrAuto()
}

class NonNegativeLengthOrPercentageOrAuto(val value: LengthOrPercentageOrAuto) : ComputedValue

sealed class LengthOrPercentageOrNone : ComputedValue {

    data class Length(val length: PixelLength) : LengthOrPercentageOrNone()

    data class Percentage(val percentage: de.krall.flare.style.value.computed.Percentage) : LengthOrPercentageOrNone()

    data class Calc(val calc: CalcLengthOrPercentage) : LengthOrPercentageOrNone()

    class None : LengthOrPercentageOrNone()
}

class NonNegativeLengthOrPercentageOrNone(val value: LengthOrPercentageOrNone) : ComputedValue