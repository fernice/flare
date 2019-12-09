/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.value.computed

import fernice.std.Some
import fernice.std.unwrap
import org.fernice.flare.cssparser.ToCss
import org.fernice.flare.std.max
import org.fernice.flare.style.value.ComputedValue
import java.io.Writer
import org.fernice.flare.style.value.specified.AbsoluteLength
import org.fernice.flare.style.value.specified.FontRelativeLength
import org.fernice.flare.style.value.specified.ViewportPercentageLength
import org.fernice.flare.style.value.specified.NoCalcLength
import org.fernice.flare.style.value.specified.Length
import org.fernice.flare.style.value.specified.NonNegativeLength as SpecifiedNonNegativeLength
import org.fernice.flare.style.value.specified.NonNegativeLengthOrPercentage as SpecifiedNonNegativeLengthOrPercentage
import org.fernice.flare.style.value.specified.NonNegativeLengthOrPercentageOrAuto as SpecifiedNonNegativeLengthOrPercentageOrAuto
import org.fernice.flare.style.value.specified.NonNegativeLengthOrPercentageOrNone as SpecifiedNonNegativeLengthOrPercentageOrNone

/**
 * Computed representation of [AbsoluteLength], [FontRelativeLength], [ViewportPercentageLength], [NoCalcLength]
 * and [Length].
 */
inline class PixelLength(val value: Float) : ComputedValue {

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

/**
 * Computed representation of [org.fernice.flare.style.value.specified.NonNegativeLength],
 * [org.fernice.flare.style.value.specified.NonNegativeLengthOrPercentage],
 * [org.fernice.flare.style.value.specified.NonNegativeLengthOrPercentageOrAuto] and
 * [org.fernice.flare.style.value.specified.NonNegativeLengthOrPercentageOrNone].
 */
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

private const val AU_PER_PX = 60

/**
 * Int-based unit primarily used for layouting. An AU is 1/60 of a pixel, removing the inaccuracy of floating-point
 * conversions.
 */
inline class Au(val value: Int) {

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
    }
}

/**
 * Computed representation of [org.fernice.flare.style.value.specified.Percentage].
 */
data class Percentage(val value: Float) : ComputedValue, ToCss {

    override fun toCss(writer: Writer) {
        writer.append("$value%")
    }

    companion object {

        val hundred: Percentage by lazy { Percentage(1f) }

        fun hundred(): Percentage {
            return hundred
        }

        val fifty: Percentage by lazy { Percentage(0.5f) }
    }
}

sealed class LengthOrPercentage : ComputedValue {

    data class Length(val length: PixelLength) : LengthOrPercentage()
    data class Percentage(val percentage: org.fernice.flare.style.value.computed.Percentage) : LengthOrPercentage()
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

        val zero: LengthOrPercentage by lazy { LengthOrPercentage.Length(PixelLength(0f)) }
        val fifty: LengthOrPercentage by lazy { LengthOrPercentage.Percentage(org.fernice.flare.style.value.computed.Percentage.fifty) }
        val Hundred: LengthOrPercentage by lazy { LengthOrPercentage.Percentage(org.fernice.flare.style.value.computed.Percentage.hundred) }

        fun zero(): LengthOrPercentage {
            return zero
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
    data class Percentage(val percentage: org.fernice.flare.style.value.computed.Percentage) : LengthOrPercentageOrAuto()
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

    fun toPixelLength(containingLength: Au, referenceLength: Au): PixelLength {
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
                referenceLength.into()
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

    fun toPixelLength(containingLength: Au, referenceLength: Au): PixelLength {
        return value.toPixelLength(containingLength, referenceLength)
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
    data class Percentage(val percentage: org.fernice.flare.style.value.computed.Percentage) : LengthOrPercentageOrNone()
    data class Calc(val calc: CalcLengthOrPercentage) : LengthOrPercentageOrNone()
    object None : LengthOrPercentageOrNone()
}

data class NonNegativeLengthOrPercentageOrNone(val value: LengthOrPercentageOrNone) : ComputedValue
