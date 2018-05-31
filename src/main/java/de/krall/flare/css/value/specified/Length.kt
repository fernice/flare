package de.krall.flare.css.value.specified

import de.krall.flare.css.ParserContext
import de.krall.flare.css.value.Context
import de.krall.flare.css.value.FontBaseSize
import de.krall.flare.css.value.SpecifiedValue
import de.krall.flare.css.value.computed.Au
import de.krall.flare.css.value.computed.PixelLength
import de.krall.flare.css.value.generic.Size2D
import de.krall.flare.std.*
import de.krall.flare.css.value.computed.Length as ComputedLength

sealed class AbsoluteLength : SpecifiedValue<PixelLength> {

    companion object Constants {

        private const val AU_PER_PX = 60
        private const val AU_PER_IN = AU_PER_PX * 96
        private const val AU_PER_CM = AU_PER_IN / 2.54f
        private const val AU_PER_MM = AU_PER_IN / 25.4f
        private const val AU_PER_Q = AU_PER_MM / 4
        private const val AU_PER_PT = AU_PER_IN / 72
        private const val AU_PER_PC = AU_PER_PT * 12
    }

    abstract fun toPx(): Float

    override fun toComputedValue(context: Context): PixelLength {
        return PixelLength(toPx())
    }

    class Px(val value: Float) : AbsoluteLength() {
        override fun toPx(): Float {
            return value
        }
    }

    class In(val value: Float) : AbsoluteLength() {
        override fun toPx(): Float {
            return value * (AU_PER_IN / AU_PER_PX)
        }
    }

    class Cm(val value: Float) : AbsoluteLength() {
        override fun toPx(): Float {
            return value * (AU_PER_CM / AU_PER_PX)
        }
    }

    class Mm(val value: Float) : AbsoluteLength() {
        override fun toPx(): Float {
            return value * (AU_PER_MM / AU_PER_PX)
        }
    }

    class Q(val value: Float) : AbsoluteLength() {
        override fun toPx(): Float {
            return value * (AU_PER_Q / AU_PER_PX)
        }
    }

    class Pt(val value: Float) : AbsoluteLength() {
        override fun toPx(): Float {
            return value * (AU_PER_PT / AU_PER_PX)
        }
    }

    class Pc(val value: Float) : AbsoluteLength() {
        override fun toPx(): Float {
            return value * (AU_PER_PC / AU_PER_PX)
        }
    }

    operator fun plus(length: AbsoluteLength): AbsoluteLength {
        return AbsoluteLength.Px(toPx() + length.toPx())
    }

    operator fun times(scalar: Float): AbsoluteLength {
        return AbsoluteLength.Px(toPx() + scalar)
    }
}

sealed class FontRelativeLength {

    abstract fun toComputedValue(context: Context, baseSize: FontBaseSize): PixelLength

    class Em(val value: Float) : FontRelativeLength() {
        override fun toComputedValue(context: Context, baseSize: FontBaseSize): PixelLength {
            return PixelLength.zero()
        }
    }

    class Ex(val value: Float) : FontRelativeLength() {
        override fun toComputedValue(context: Context, baseSize: FontBaseSize): PixelLength {
            return PixelLength.zero()
        }
    }

    class Ch(val value: Float) : FontRelativeLength() {
        override fun toComputedValue(context: Context, baseSize: FontBaseSize): PixelLength {
            return PixelLength.zero()
        }
    }

    class Rem(val value: Float) : FontRelativeLength() {
        override fun toComputedValue(context: Context, baseSize: FontBaseSize): PixelLength {
            return PixelLength.zero()
        }
    }
}

sealed class ViewportPercentageLength {

    abstract fun toComputedValue(context: Context, viewportSize: Size2D<Au>): PixelLength

    class Vw(val value: Float) : ViewportPercentageLength() {
        override fun toComputedValue(context: Context, viewportSize: Size2D<Au>): PixelLength {
            val au = (viewportSize.width.value * value / 100.0).trunc()

            return Au.fromAu64(au).toPx()
        }
    }

    class Vh(val value: Float) : ViewportPercentageLength() {
        override fun toComputedValue(context: Context, viewportSize: Size2D<Au>): PixelLength {
            val au = (viewportSize.height.value * value / 100.0).trunc()

            return Au.fromAu64(au).toPx()
        }
    }

    class Vmin(val value: Float) : ViewportPercentageLength() {
        override fun toComputedValue(context: Context, viewportSize: Size2D<Au>): PixelLength {
            val au = (viewportSize.width.max(viewportSize.height).value * value / 100.0).trunc()

            return Au.fromAu64(au).toPx()
        }
    }

    class Vmax(val value: Float) : ViewportPercentageLength() {
        override fun toComputedValue(context: Context, viewportSize: Size2D<Au>): PixelLength {
            val au = (viewportSize.width.min(viewportSize.height).value * value / 100.0).trunc()

            return Au.fromAu64(au).toPx()
        }
    }
}

sealed class NoCalcLength : SpecifiedValue<PixelLength> {

    class Absolute(val length: AbsoluteLength) : NoCalcLength() {
        override fun toComputedValue(context: Context): PixelLength {
            return length.toComputedValue(context)
        }
    }

    class FontRelative(val length: FontRelativeLength) : NoCalcLength() {
        override fun toComputedValue(context: Context): PixelLength {
            return length.toComputedValue(context, FontBaseSize.CurrentStyle())
        }
    }

    class ViewportPercentage(val length: ViewportPercentageLength) : NoCalcLength() {
        override fun toComputedValue(context: Context): PixelLength {
            val viewportSize = context.viewportSizeForViewportUnitResolution()

            return length.toComputedValue(context, viewportSize)
        }
    }

    companion object {

        fun parseDimension(context: ParserContext, value: Float, unit: String): Result<NoCalcLength, Empty> {
            return when (unit.toLowerCase()) {
                "px" -> Ok(NoCalcLength.Absolute(AbsoluteLength.Px(value)))
                "in" -> Ok(NoCalcLength.Absolute(AbsoluteLength.In(value)))
                "cm" -> Ok(NoCalcLength.Absolute(AbsoluteLength.Cm(value)))
                "mm" -> Ok(NoCalcLength.Absolute(AbsoluteLength.Mm(value)))
                "q" -> Ok(NoCalcLength.Absolute(AbsoluteLength.Q(value)))
                "pt" -> Ok(NoCalcLength.Absolute(AbsoluteLength.Pt(value)))
                "pc" -> Ok(NoCalcLength.Absolute(AbsoluteLength.Pc(value)))

                "em" -> Ok(NoCalcLength.FontRelative(FontRelativeLength.Em(value)))
                "ex" -> Ok(NoCalcLength.FontRelative(FontRelativeLength.Ex(value)))
                "ch" -> Ok(NoCalcLength.FontRelative(FontRelativeLength.Ch(value)))
                "rem" -> Ok(NoCalcLength.FontRelative(FontRelativeLength.Rem(value)))

                "vw" -> Ok(NoCalcLength.ViewportPercentage(ViewportPercentageLength.Vw(value)))
                "vh" -> Ok(NoCalcLength.ViewportPercentage(ViewportPercentageLength.Vh(value)))
                "vmin" -> Ok(NoCalcLength.ViewportPercentage(ViewportPercentageLength.Vmin(value)))
                "vmax" -> Ok(NoCalcLength.ViewportPercentage(ViewportPercentageLength.Vmax(value)))

                else -> Err()
            }
        }
    }
}

sealed class Length : SpecifiedValue<PixelLength> {

    class NoCalc(val length: NoCalcLength) : Length() {
        override fun toComputedValue(context: Context): PixelLength {
            return length.toComputedValue(context)
        }
    }

    class Calc(val calc: CalcLengthOrPercentage): Length() {
        override fun toComputedValue(context: Context): PixelLength {
            return calc.toComputedValue(context).length()
        }
    }
}