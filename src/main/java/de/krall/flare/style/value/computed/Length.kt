package de.krall.flare.style.value.computed

import de.krall.flare.std.max
import de.krall.flare.style.value.ComputedValue
import modern.std.Some
import modern.std.unwrap

data class PixelLength(val value: Float) : ComputedValue {

    fun px(): Float {
        return value
    }

    operator fun unaryMinus(): PixelLength {
        return PixelLength(-value)
    }

    operator fun plus(length: PixelLength): PixelLength {
        return PixelLength(value + length.value)
    }

    operator fun minus(length: PixelLength): PixelLength {
        return PixelLength(value - length.value)
    }

    operator fun times(length: PixelLength): PixelLength {
        return PixelLength(value * length.value)
    }

    operator fun div(length: PixelLength): PixelLength {
        return PixelLength(value / length.value)
    }

    companion object {

        private val zero: PixelLength by lazy { PixelLength(0f) }

        fun zero(): PixelLength {
            return zero
        }
    }
}

typealias Length = PixelLength

fun PixelLength.into(): Au {
    return Au.fromPx(this.value)
}

fun Au.into(): PixelLength {
    return PixelLength(this.toPx())
}

data class NonNegativeLength(val length: PixelLength) : ComputedValue {

    fun scaleBy(factor: Float): NonNegativeLength {
        return new(length.px() * factor.max(0f))
    }

    operator fun plus(other: NonNegativeLength): NonNegativeLength {
        return new(length.px() + other.length.px())
    }

    companion object {

        fun new(px: Float): NonNegativeLength {
            return NonNegativeLength(PixelLength(px.max(0f)))
        }

        private val zero: NonNegativeLength by lazy { NonNegativeLength(PixelLength.zero()) }

        fun zero(): NonNegativeLength {
            return zero
        }
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

data class Au(val value: Int) {

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

data class Percentage(val value: Float) : ComputedValue {

    companion object {

        private val hundred: Percentage by lazy { Percentage(1f) }

        fun hundred(): Percentage {
            return hundred
        }
    }
}

sealed class LengthOrPercentage : ComputedValue {

    data class Length(val length: PixelLength) : LengthOrPercentage()

    data class Percentage(val percentage: de.krall.flare.style.value.computed.Percentage) : LengthOrPercentage()

    data class Calc(val calc: CalcLengthOrPercentage) : LengthOrPercentage()

    fun toPixelLength(containingLength: Au): PixelLength {
        return when (this) {
            is LengthOrPercentage.Length -> {
                length
            }
            is LengthOrPercentage.Percentage -> {
                containingLength.scaleBy(percentage.value).into()
            }
            is LengthOrPercentage.Calc -> {
                calc.toPixelLength(Some(containingLength)).unwrap()
            }
        }
    }

    companion object {

        private val zero: LengthOrPercentage by lazy { LengthOrPercentage.Length(PixelLength.zero()) }
        private val fifty: LengthOrPercentage by lazy { LengthOrPercentage.Length(PixelLength(0.5f)) }

        fun zero(): LengthOrPercentage {
            return zero
        }

        fun fifty(): LengthOrPercentage {
            return fifty
        }
    }
}

data class NonNegativeLengthOrPercentage(val value: LengthOrPercentage) : ComputedValue {

    fun toPixelLength(containingLength: Au): PixelLength {
        return value.toPixelLength(containingLength)
    }

    companion object {

        private val zero: NonNegativeLengthOrPercentage by lazy { NonNegativeLengthOrPercentage(LengthOrPercentage.zero()) }

        fun zero(): NonNegativeLengthOrPercentage {
            return zero
        }
    }
}

sealed class LengthOrPercentageOrAuto : ComputedValue {

    data class Length(val length: PixelLength) : LengthOrPercentageOrAuto()

    data class Percentage(val percentage: de.krall.flare.style.value.computed.Percentage) : LengthOrPercentageOrAuto()

    data class Calc(val calc: CalcLengthOrPercentage) : LengthOrPercentageOrAuto()

    object Auto : LengthOrPercentageOrAuto()

    fun toPixelLength(containingLength: Au): PixelLength {
        return when (this) {
            is LengthOrPercentageOrAuto.Length -> {
                length
            }
            is LengthOrPercentageOrAuto.Percentage -> {
                containingLength.scaleBy(percentage.value).into()
            }
            is LengthOrPercentageOrAuto.Calc -> {
                calc.toPixelLength(Some(containingLength)).unwrap()
            }
            is LengthOrPercentageOrAuto.Auto -> {
                PixelLength.zero()
            }
        }
    }

    companion object {

        private val zero: LengthOrPercentageOrAuto by lazy { LengthOrPercentageOrAuto.Length(PixelLength.zero()) }

        fun zero(): LengthOrPercentageOrAuto {
            return zero
        }
    }
}

data class NonNegativeLengthOrPercentageOrAuto(val value: LengthOrPercentageOrAuto) : ComputedValue {

    fun toPixelLength(containingLength: Au): PixelLength {
        return value.toPixelLength(containingLength)
    }

    companion object {
        private val auto: NonNegativeLengthOrPercentageOrAuto by lazy { NonNegativeLengthOrPercentageOrAuto(LengthOrPercentageOrAuto.Auto) }

        fun auto(): NonNegativeLengthOrPercentageOrAuto {
            return auto
        }
    }
}

sealed class LengthOrPercentageOrNone : ComputedValue {

    data class Length(val length: PixelLength) : LengthOrPercentageOrNone()

    data class Percentage(val percentage: de.krall.flare.style.value.computed.Percentage) : LengthOrPercentageOrNone()

    data class Calc(val calc: CalcLengthOrPercentage) : LengthOrPercentageOrNone()

    object None : LengthOrPercentageOrNone()
}

data class NonNegativeLengthOrPercentageOrNone(val value: LengthOrPercentageOrNone) : ComputedValue