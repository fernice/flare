package de.krall.flare.style.value.specified

import de.krall.flare.style.parser.*
import de.krall.flare.style.value.Context
import de.krall.flare.style.value.FontBaseSize
import de.krall.flare.style.value.SpecifiedValue
import de.krall.flare.style.value.computed.Au
import de.krall.flare.style.value.computed.PixelLength
import de.krall.flare.style.value.computed.into
import de.krall.flare.style.value.generic.Size2D
import de.krall.flare.cssparser.*
import de.krall.flare.font.FontMetricsQueryResult
import de.krall.flare.std.*
import de.krall.flare.style.value.computed.Percentage as ComputedPercentage
import de.krall.flare.style.value.computed.NonNegativeLength as ComputedNonNegativeLength
import de.krall.flare.style.value.computed.LengthOrPercentage as ComputedLengthOrPercentage
import de.krall.flare.style.value.computed.NonNegativeLengthOrPercentage as ComputedNonNegativeLengthOrPercentage
import de.krall.flare.style.value.computed.LengthOrPercentageOrAuto as ComputedLengthOrPercentageOrAuto
import de.krall.flare.style.value.computed.NonNegativeLengthOrPercentageOrAuto as ComputedNonNegativeLengthOrPercentageOrAuto
import de.krall.flare.style.value.computed.LengthOrPercentageOrNone as ComputedLengthOrPercentageOrNone
import de.krall.flare.style.value.computed.NonNegativeLengthOrPercentageOrNone as ComputedNonNegativeLengthOrPercentageOrNone

sealed class LengthParseErrorKind : ParseErrorKind() {

    class ForbiddenNumeric : LengthParseErrorKind()
    class UnitlessNumber : LengthParseErrorKind()
}

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

    fun toComputedValue(context: Context, baseSize: FontBaseSize): PixelLength {
        val (referencedSize, factor) = referencedSizeAndFactor(context, baseSize)
        val pixel = (referencedSize.toFloat() * factor)
                .min(Float.MIN_VALUE)
                .max(Float.MAX_VALUE)
        return PixelLength(pixel)
    }

    internal abstract fun referencedSizeAndFactor(context: Context, baseSize: FontBaseSize): Pair<Au, Float>

    internal fun queryFontMetrics(context: Context, fontSize: Au): FontMetricsQueryResult {
        return context.fontMetricsProvider.query(
                context.style().getFont(),
                fontSize,
                context.device()
        )
    }

    class Em(val value: Float) : FontRelativeLength() {
        override fun referencedSizeAndFactor(context: Context, baseSize: FontBaseSize): Pair<Au, Float> {
            return if (baseSize is FontBaseSize.InheritStyleButStripEmUnits) {
                Pair(Au(0), value)
            } else {
                val referencedFontSize = baseSize.resolve(context)

                Pair(referencedFontSize, value)
            }
        }
    }

    class Ex(val value: Float) : FontRelativeLength() {
        override fun referencedSizeAndFactor(context: Context, baseSize: FontBaseSize): Pair<Au, Float> {
            val referencedFontSize = baseSize.resolve(context)
            val metricsResult = queryFontMetrics(context, referencedFontSize)

            val referenceSize = when (metricsResult) {
                is FontMetricsQueryResult.Available -> metricsResult.metrics.xHeight
                is FontMetricsQueryResult.NotAvailable -> referencedFontSize.scaleBy(0.5f)
            }

            return Pair(referenceSize, value)
        }
    }

    class Ch(val value: Float) : FontRelativeLength() {
        override fun referencedSizeAndFactor(context: Context, baseSize: FontBaseSize): Pair<Au, Float> {
            val referencedFontSize = baseSize.resolve(context)
            val metricsResult = queryFontMetrics(context, referencedFontSize)

            val referenceSize = when (metricsResult) {
                is FontMetricsQueryResult.Available -> metricsResult.metrics.zeroAdvanceMeasure
                is FontMetricsQueryResult.NotAvailable -> {
                    if (context.style().writingMode.isVertical()) {
                        referencedFontSize
                    } else {
                        referencedFontSize.scaleBy(0.5f)
                    }
                }
            }

            return Pair(referenceSize, value)
        }
    }

    class Rem(val value: Float) : FontRelativeLength() {
        override fun referencedSizeAndFactor(context: Context, baseSize: FontBaseSize): Pair<Au, Float> {
            val referencedSize = if (context.isRootElement()) {
                baseSize.resolve(context)
            } else {
                context.device().rootFontSize()
            }

            return Pair(referencedSize, value)
        }
    }
}

sealed class ViewportPercentageLength {

    abstract fun toComputedValue(context: Context, viewportSize: Size2D<Au>): PixelLength

    class Vw(val value: Float) : ViewportPercentageLength() {
        override fun toComputedValue(context: Context, viewportSize: Size2D<Au>): PixelLength {
            val au = (viewportSize.width.value * value / 100.0).trunc()

            return Au.fromAu64(au).into()
        }
    }

    class Vh(val value: Float) : ViewportPercentageLength() {
        override fun toComputedValue(context: Context, viewportSize: Size2D<Au>): PixelLength {
            val au = (viewportSize.height.value * value / 100.0).trunc()

            return Au.fromAu64(au).into()
        }
    }

    class Vmin(val value: Float) : ViewportPercentageLength() {
        override fun toComputedValue(context: Context, viewportSize: Size2D<Au>): PixelLength {
            val au = (viewportSize.width.max(viewportSize.height).value * value / 100.0).trunc()

            return Au.fromAu64(au).into()
        }
    }

    class Vmax(val value: Float) : ViewportPercentageLength() {
        override fun toComputedValue(context: Context, viewportSize: Size2D<Au>): PixelLength {
            val au = (viewportSize.width.min(viewportSize.height).value * value / 100.0).trunc()

            return Au.fromAu64(au).into()
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

    class Calc(val calc: CalcLengthOrPercentage) : Length() {
        override fun toComputedValue(context: Context): PixelLength {
            return calc.toComputedValue(context).length()
        }
    }

    companion object {

        fun parse(context: ParserContext, input: Parser): Result<Length, ParseError> {
            return parseQuirky(context, input, AllowQuirks.No())
        }

        fun parseQuirky(context: ParserContext,
                        input: Parser,
                        allowQuirks: AllowQuirks): Result<Length, ParseError> {
            return parseInternal(context, input, ClampingMode.All(), allowQuirks)
        }

        fun parseNonNegative(context: ParserContext, input: Parser): Result<Length, ParseError> {
            return parseNonNegativeQuirky(context, input, AllowQuirks.No())
        }

        fun parseNonNegativeQuirky(context: ParserContext,
                                   input: Parser,
                                   allowQuirks: AllowQuirks): Result<Length, ParseError> {
            return parseInternal(context, input, ClampingMode.NonNegative(), allowQuirks)
        }

        private fun parseInternal(context: ParserContext,
                                  input: Parser,
                                  clampingMode: ClampingMode,
                                  allowQuirks: AllowQuirks): Result<Length, ParseError> {
            val location = input.sourceLocation()
            val tokenResult = input.next()

            val token = when (tokenResult) {
                is Ok -> tokenResult.value
                is Err -> return tokenResult
            }

            when (token) {
                is Token.Dimension -> {
                    if (!clampingMode.isAllowed(context.parseMode, token.number.float())) {
                        return Err(location.newError(LengthParseErrorKind.ForbiddenNumeric()))
                    }

                    return NoCalcLength.parseDimension(context, token.number.float(), token.unit)
                            .map(Length::NoCalc)
                            .mapErr { location.newUnexpectedTokenError(token) }
                }
                is Token.Number -> {
                    if (!clampingMode.isAllowed(context.parseMode, token.number.float())) {
                        return Err(location.newError(LengthParseErrorKind.ForbiddenNumeric()))
                    }

                    if (token.number.float() != 0f
                            && !context.parseMode.allowsUnitlessNumbers()
                            && !allowQuirks.allowed(context.quirksMode)) {
                        return Err(location.newError(LengthParseErrorKind.UnitlessNumber()))
                    }

                    return Ok(Length.NoCalc(NoCalcLength.Absolute(AbsoluteLength.Px(token.number.float()))))
                }
                is Token.Function -> {
                    if (!token.name.equals("calc", true)) {
                        return Err(location.newUnexpectedTokenError(token))
                    }

                    return input.parseNestedBlock { input ->
                        CalcNode.parseLength(context, input, clampingMode)
                                .map(Length::Calc)
                    }
                }
                else -> {
                    return Err(location.newUnexpectedTokenError(token))
                }
            }
        }
    }
}

class NonNegativeLength(val length: Length) : SpecifiedValue<ComputedNonNegativeLength> {
    companion object {

        fun parse(context: ParserContext, input: Parser): Result<NonNegativeLength, ParseError> {
            return Length.parseNonNegative(context, input)
                    .map(::NonNegativeLength)
        }

        fun parseQuirky(context: ParserContext,
                        input: Parser,
                        allowQuirks: AllowQuirks): Result<NonNegativeLength, ParseError> {
            return Length.parseQuirky(context, input, allowQuirks)
                    .map(::NonNegativeLength)
        }
    }

    override fun toComputedValue(context: Context): ComputedNonNegativeLength {
        return ComputedNonNegativeLength(length.toComputedValue(context))
    }
}

data class Percentage(val value: Float) : SpecifiedValue<ComputedPercentage> {
    override fun toComputedValue(context: Context): ComputedPercentage {
        return ComputedPercentage(value)
    }

    companion object {

        fun parse(context: ParserContext, input: Parser): Result<Percentage, ParseError> {
            val location = input.sourceLocation()
            val tokenResult = input.next()

            val token = when (tokenResult) {
                is Ok -> tokenResult.value
                is Err -> return tokenResult
            }

            return when (token) {
                is Token.Percentage -> Ok(Percentage(token.number.float()))
                else -> Err(location.newUnexpectedTokenError(token))
            }
        }

        private val zero: Percentage by lazy { Percentage(0f) }
        private val fifty: Percentage by lazy { Percentage(0.5f) }
        private val hundred: Percentage by lazy { Percentage(1f) }

        fun zero(): Percentage {
            return zero
        }

        fun fifty(): Percentage {
            return fifty
        }

        fun hundred(): Percentage {
            return hundred
        }
    }
}

fun Percentage.intoLengthOrPercentage(): LengthOrPercentage {
    return LengthOrPercentage.Percentage(this)
}

sealed class LengthOrPercentage : SpecifiedValue<ComputedLengthOrPercentage> {

    companion object {

        fun parse(context: ParserContext, input: Parser): Result<LengthOrPercentage, ParseError> {
            return parseQuirky(context, input, AllowQuirks.No())
        }

        fun parseQuirky(context: ParserContext,
                        input: Parser,
                        allowQuirks: AllowQuirks): Result<LengthOrPercentage, ParseError> {
            return parseInternal(context, input, ClampingMode.All(), allowQuirks)
        }

        fun parseNonNegative(context: ParserContext, input: Parser): Result<LengthOrPercentage, ParseError> {
            return parseNonNegativeQuirky(context, input, AllowQuirks.No())
        }

        fun parseNonNegativeQuirky(context: ParserContext,
                                   input: Parser,
                                   allowQuirks: AllowQuirks): Result<LengthOrPercentage, ParseError> {
            return parseInternal(context, input, ClampingMode.NonNegative(), allowQuirks)
        }

        private fun parseInternal(context: ParserContext,
                                  input: Parser,
                                  clampingMode: ClampingMode,
                                  allowQuirks: AllowQuirks): Result<LengthOrPercentage, ParseError> {
            val location = input.sourceLocation()
            val tokenResult = input.next()

            val token = when (tokenResult) {
                is Ok -> tokenResult.value
                is Err -> return tokenResult
            }

            when (token) {
                is Token.Dimension -> {
                    if (!clampingMode.isAllowed(context.parseMode, token.number.float())) {
                        return Err(location.newError(LengthParseErrorKind.ForbiddenNumeric()))
                    }

                    return NoCalcLength.parseDimension(context, token.number.float(), token.unit)
                            .map(LengthOrPercentage::Length)
                            .mapErr { location.newUnexpectedTokenError(token) }
                }
                is Token.Percentage -> {
                    if (!clampingMode.isAllowed(context.parseMode, token.number.float())) {
                        return Err(location.newError(LengthParseErrorKind.ForbiddenNumeric()))
                    }

                    return Ok(LengthOrPercentage.Percentage(de.krall.flare.style.value.specified.Percentage(token.number.float())))
                }
                is Token.Number -> {
                    if (!clampingMode.isAllowed(context.parseMode, token.number.float())) {
                        return Err(location.newError(LengthParseErrorKind.ForbiddenNumeric()))
                    }

                    if (token.number.float() != 0f
                            && !context.parseMode.allowsUnitlessNumbers()
                            && !allowQuirks.allowed(context.quirksMode)) {
                        return Err(location.newError(LengthParseErrorKind.UnitlessNumber()))
                    }

                    return Ok(LengthOrPercentage.Length(NoCalcLength.Absolute(AbsoluteLength.Px(token.number.float()))))
                }
                is Token.Function -> {
                    if (!token.name.equals("calc", true)) {
                        return Err(location.newUnexpectedTokenError(token))
                    }

                    return input.parseNestedBlock { input ->
                        CalcNode.parseLength(context, input, clampingMode)
                                .map(LengthOrPercentage::Calc)
                    }
                }
                else -> {
                    return Err(location.newUnexpectedTokenError(token))
                }
            }
        }
    }

    data class Length(val length: NoCalcLength) : LengthOrPercentage() {
        override fun toComputedValue(context: Context): ComputedLengthOrPercentage {
            return ComputedLengthOrPercentage.Length(length.toComputedValue(context))
        }
    }

    data class Percentage(val percentage: de.krall.flare.style.value.specified.Percentage) : LengthOrPercentage() {
        override fun toComputedValue(context: Context): ComputedLengthOrPercentage {
            return ComputedLengthOrPercentage.Percentage(percentage.toComputedValue(context))
        }
    }

    data class Calc(val calc: CalcLengthOrPercentage) : LengthOrPercentage() {
        override fun toComputedValue(context: Context): ComputedLengthOrPercentage {
            return ComputedLengthOrPercentage.Calc(calc.toComputedValue(context))
        }
    }
}

class NonNegativeLengthOrPercentage(val value: LengthOrPercentage) : SpecifiedValue<ComputedNonNegativeLengthOrPercentage> {

    companion object {

        fun parse(context: ParserContext, input: Parser): Result<NonNegativeLengthOrPercentage, ParseError> {
            return LengthOrPercentage.parseNonNegative(context, input)
                    .map(::NonNegativeLengthOrPercentage)
        }

        fun parseQuirky(context: ParserContext,
                        input: Parser,
                        allowQuirks: AllowQuirks): Result<NonNegativeLengthOrPercentage, ParseError> {
            return LengthOrPercentage.parseQuirky(context, input, allowQuirks)
                    .map(::NonNegativeLengthOrPercentage)
        }
    }

    override fun toComputedValue(context: Context): ComputedNonNegativeLengthOrPercentage {
        return ComputedNonNegativeLengthOrPercentage(value.toComputedValue(context))
    }
}

sealed class LengthOrPercentageOrAuto : SpecifiedValue<ComputedLengthOrPercentageOrAuto> {

    companion object {

        fun parse(context: ParserContext, input: Parser): Result<LengthOrPercentageOrAuto, ParseError> {
            return parseQuirky(context, input, AllowQuirks.No())
        }

        fun parseQuirky(context: ParserContext,
                        input: Parser,
                        allowQuirks: AllowQuirks): Result<LengthOrPercentageOrAuto, ParseError> {
            return parseInternal(context, input, ClampingMode.All(), allowQuirks)
        }

        fun parseNonNegative(context: ParserContext, input: Parser): Result<LengthOrPercentageOrAuto, ParseError> {
            return parseNonNegativeQuirky(context, input, AllowQuirks.No())
        }

        fun parseNonNegativeQuirky(context: ParserContext,
                                   input: Parser,
                                   allowQuirks: AllowQuirks): Result<LengthOrPercentageOrAuto, ParseError> {
            return parseInternal(context, input, ClampingMode.NonNegative(), allowQuirks)
        }

        private fun parseInternal(context: ParserContext,
                                  input: Parser,
                                  clampingMode: ClampingMode,
                                  allowQuirks: AllowQuirks): Result<LengthOrPercentageOrAuto, ParseError> {
            val location = input.sourceLocation()
            val tokenResult = input.next()

            val token = when (tokenResult) {
                is Ok -> tokenResult.value
                is Err -> return tokenResult
            }

            when (token) {
                is Token.Dimension -> {
                    if (!clampingMode.isAllowed(context.parseMode, token.number.float())) {
                        return Err(location.newError(LengthParseErrorKind.ForbiddenNumeric()))
                    }

                    return NoCalcLength.parseDimension(context, token.number.float(), token.unit)
                            .map(LengthOrPercentageOrAuto::Length)
                            .mapErr { location.newUnexpectedTokenError(token) }
                }
                is Token.Percentage -> {
                    if (!clampingMode.isAllowed(context.parseMode, token.number.float())) {
                        return Err(location.newError(LengthParseErrorKind.ForbiddenNumeric()))
                    }

                    return Ok(LengthOrPercentageOrAuto.Percentage(de.krall.flare.style.value.specified.Percentage(token.number.float())))
                }
                is Token.Number -> {
                    if (!clampingMode.isAllowed(context.parseMode, token.number.float())) {
                        return Err(location.newError(LengthParseErrorKind.ForbiddenNumeric()))
                    }

                    if (token.number.float() != 0f
                            && !context.parseMode.allowsUnitlessNumbers()
                            && !allowQuirks.allowed(context.quirksMode)) {
                        return Err(location.newError(LengthParseErrorKind.UnitlessNumber()))
                    }

                    return Ok(LengthOrPercentageOrAuto.Length(NoCalcLength.Absolute(AbsoluteLength.Px(token.number.float()))))
                }
                is Token.Function -> {
                    if (!token.name.equals("calc", true)) {
                        return Err(location.newUnexpectedTokenError(token))
                    }

                    return input.parseNestedBlock { input ->
                        CalcNode.parseLength(context, input, clampingMode)
                                .map(LengthOrPercentageOrAuto::Calc)
                    }
                }
                is Token.Identifier -> {
                    if (!token.name.equals("auto", true)) {
                        return Err(location.newUnexpectedTokenError(token))
                    }

                    return Ok(LengthOrPercentageOrAuto.Auto())
                }
                else -> {
                    return Err(location.newUnexpectedTokenError(token))
                }
            }
        }
    }

    data class Length(val length: NoCalcLength) : LengthOrPercentageOrAuto() {
        override fun toComputedValue(context: Context): ComputedLengthOrPercentageOrAuto {
            return ComputedLengthOrPercentageOrAuto.Length(length.toComputedValue(context))
        }
    }

    data class Percentage(val percentage: de.krall.flare.style.value.specified.Percentage) : LengthOrPercentageOrAuto() {
        override fun toComputedValue(context: Context): ComputedLengthOrPercentageOrAuto {
            return ComputedLengthOrPercentageOrAuto.Percentage(percentage.toComputedValue(context))
        }
    }

    data class Calc(val calc: CalcLengthOrPercentage) : LengthOrPercentageOrAuto() {
        override fun toComputedValue(context: Context): ComputedLengthOrPercentageOrAuto {
            return ComputedLengthOrPercentageOrAuto.Calc(calc.toComputedValue(context))
        }
    }

    class Auto : LengthOrPercentageOrAuto() {
        override fun toComputedValue(context: Context): ComputedLengthOrPercentageOrAuto {
            return ComputedLengthOrPercentageOrAuto.Auto()
        }
    }
}

class NonNegativeLengthOrPercentageOrAuto(val value: LengthOrPercentageOrAuto) : SpecifiedValue<ComputedNonNegativeLengthOrPercentageOrAuto> {

    companion object {

        fun parse(context: ParserContext, input: Parser): Result<NonNegativeLengthOrPercentageOrAuto, ParseError> {
            return LengthOrPercentageOrAuto.parseNonNegative(context, input)
                    .map(::NonNegativeLengthOrPercentageOrAuto)
        }

        fun parseQuirky(context: ParserContext,
                        input: Parser,
                        allowQuirks: AllowQuirks): Result<NonNegativeLengthOrPercentageOrAuto, ParseError> {
            return LengthOrPercentageOrAuto.parseQuirky(context, input, allowQuirks)
                    .map(::NonNegativeLengthOrPercentageOrAuto)
        }
    }

    override fun toComputedValue(context: Context): ComputedNonNegativeLengthOrPercentageOrAuto {
        return ComputedNonNegativeLengthOrPercentageOrAuto(value.toComputedValue(context))
    }
}

sealed class LengthOrPercentageOrNone : SpecifiedValue<ComputedLengthOrPercentageOrNone> {

    companion object {

        fun parse(context: ParserContext, input: Parser): Result<LengthOrPercentageOrNone, ParseError> {
            return parseQuirky(context, input, AllowQuirks.No())
        }

        fun parseQuirky(context: ParserContext,
                        input: Parser,
                        allowQuirks: AllowQuirks): Result<LengthOrPercentageOrNone, ParseError> {
            return parseInternal(context, input, ClampingMode.All(), allowQuirks)
        }

        fun parseNonNegative(context: ParserContext, input: Parser): Result<LengthOrPercentageOrNone, ParseError> {
            return parseNonNegativeQuirky(context, input, AllowQuirks.No())
        }

        fun parseNonNegativeQuirky(context: ParserContext,
                                   input: Parser,
                                   allowQuirks: AllowQuirks): Result<LengthOrPercentageOrNone, ParseError> {
            return parseInternal(context, input, ClampingMode.NonNegative(), allowQuirks)
        }

        private fun parseInternal(context: ParserContext,
                                  input: Parser,
                                  clampingMode: ClampingMode,
                                  allowQuirks: AllowQuirks): Result<LengthOrPercentageOrNone, ParseError> {
            val location = input.sourceLocation()
            val tokenResult = input.next()

            val token = when (tokenResult) {
                is Ok -> tokenResult.value
                is Err -> return tokenResult
            }

            when (token) {
                is Token.Dimension -> {
                    if (!clampingMode.isAllowed(context.parseMode, token.number.float())) {
                        return Err(location.newError(LengthParseErrorKind.ForbiddenNumeric()))
                    }

                    return NoCalcLength.parseDimension(context, token.number.float(), token.unit)
                            .map(LengthOrPercentageOrNone::Length)
                            .mapErr { location.newUnexpectedTokenError(token) }
                }
                is Token.Percentage -> {
                    if (!clampingMode.isAllowed(context.parseMode, token.number.float())) {
                        return Err(location.newError(LengthParseErrorKind.ForbiddenNumeric()))
                    }

                    return Ok(LengthOrPercentageOrNone.Percentage(de.krall.flare.style.value.specified.Percentage(token.number.float())))
                }
                is Token.Number -> {
                    if (!clampingMode.isAllowed(context.parseMode, token.number.float())) {
                        return Err(location.newError(LengthParseErrorKind.ForbiddenNumeric()))
                    }

                    if (token.number.float() != 0f
                            && !context.parseMode.allowsUnitlessNumbers()
                            && !allowQuirks.allowed(context.quirksMode)) {
                        return Err(location.newError(LengthParseErrorKind.UnitlessNumber()))
                    }

                    return Ok(LengthOrPercentageOrNone.Length(NoCalcLength.Absolute(AbsoluteLength.Px(token.number.float()))))
                }
                is Token.Function -> {
                    if (!token.name.equals("calc", true)) {
                        return Err(location.newUnexpectedTokenError(token))
                    }

                    return input.parseNestedBlock { input ->
                        CalcNode.parseLength(context, input, clampingMode)
                                .map(LengthOrPercentageOrNone::Calc)
                    }
                }
                is Token.Identifier -> {
                    if (!token.name.equals("none", true)) {
                        return Err(location.newUnexpectedTokenError(token))
                    }

                    return Ok(LengthOrPercentageOrNone.None())
                }
                else -> {
                    return Err(location.newUnexpectedTokenError(token))
                }
            }
        }
    }

    data class Length(val length: NoCalcLength) : LengthOrPercentageOrNone() {
        override fun toComputedValue(context: Context): ComputedLengthOrPercentageOrNone {
            return ComputedLengthOrPercentageOrNone.Length(length.toComputedValue(context))
        }
    }

    data class Percentage(val percentage: de.krall.flare.style.value.specified.Percentage) : LengthOrPercentageOrNone() {
        override fun toComputedValue(context: Context): ComputedLengthOrPercentageOrNone {
            return ComputedLengthOrPercentageOrNone.Percentage(percentage.toComputedValue(context))
        }
    }

    data class Calc(val calc: CalcLengthOrPercentage) : LengthOrPercentageOrNone() {
        override fun toComputedValue(context: Context): ComputedLengthOrPercentageOrNone {
            return ComputedLengthOrPercentageOrNone.Calc(calc.toComputedValue(context))
        }
    }

    class None : LengthOrPercentageOrNone() {
        override fun toComputedValue(context: Context): ComputedLengthOrPercentageOrNone {
            return ComputedLengthOrPercentageOrNone.None()
        }
    }
}

class NonNegativeLengthOrPercentageOrNone(val value: LengthOrPercentageOrNone) : SpecifiedValue<ComputedNonNegativeLengthOrPercentageOrNone> {

    companion object {

        fun parse(context: ParserContext, input: Parser): Result<NonNegativeLengthOrPercentageOrNone, ParseError> {
            return LengthOrPercentageOrNone.parseNonNegative(context, input)
                    .map(::NonNegativeLengthOrPercentageOrNone)
        }

        fun parseQuirky(context: ParserContext,
                        input: Parser,
                        allowQuirks: AllowQuirks): Result<NonNegativeLengthOrPercentageOrNone, ParseError> {
            return LengthOrPercentageOrNone.parseQuirky(context, input, allowQuirks)
                    .map(::NonNegativeLengthOrPercentageOrNone)
        }
    }

    override fun toComputedValue(context: Context): ComputedNonNegativeLengthOrPercentageOrNone {
        return ComputedNonNegativeLengthOrPercentageOrNone(value.toComputedValue(context))
    }
}