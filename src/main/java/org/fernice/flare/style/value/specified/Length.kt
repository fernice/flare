/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.value.specified

import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.ParseErrorKind
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.cssparser.Token
import org.fernice.flare.cssparser.newError
import org.fernice.flare.cssparser.newUnexpectedTokenError
import org.fernice.flare.font.FontMetricsQueryResult
import org.fernice.flare.std.max
import org.fernice.flare.std.min
import org.fernice.flare.std.trunc
import org.fernice.flare.style.parser.AllowQuirks
import org.fernice.flare.style.parser.ClampingMode
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.value.Context
import org.fernice.flare.style.value.FontBaseSize
import org.fernice.flare.style.value.SpecifiedValue
import org.fernice.flare.style.value.computed.Au
import org.fernice.flare.style.value.computed.PixelLength
import org.fernice.flare.style.value.computed.into
import org.fernice.flare.style.value.generic.Size2D
import fernice.std.Empty
import fernice.std.Err
import fernice.std.Ok
import fernice.std.Result
import org.fernice.flare.style.value.computed.LengthOrPercentage as ComputedLengthOrPercentage
import org.fernice.flare.style.value.computed.LengthOrPercentageOrAuto as ComputedLengthOrPercentageOrAuto
import org.fernice.flare.style.value.computed.LengthOrPercentageOrNone as ComputedLengthOrPercentageOrNone
import org.fernice.flare.style.value.computed.NonNegativeLength as ComputedNonNegativeLength
import org.fernice.flare.style.value.computed.NonNegativeLengthOrPercentage as ComputedNonNegativeLengthOrPercentage
import org.fernice.flare.style.value.computed.NonNegativeLengthOrPercentageOrAuto as ComputedNonNegativeLengthOrPercentageOrAuto
import org.fernice.flare.style.value.computed.NonNegativeLengthOrPercentageOrNone as ComputedNonNegativeLengthOrPercentageOrNone
import org.fernice.flare.style.value.computed.Percentage as ComputedPercentage

sealed class LengthParseErrorKind : ParseErrorKind() {

    object ForbiddenNumeric : LengthParseErrorKind()
    object UnitlessNumber : LengthParseErrorKind()
}

private const val AU_PER_PX = 60
private const val AU_PER_IN = AU_PER_PX * 96
private const val AU_PER_CM = AU_PER_IN / 2.54f
private const val AU_PER_MM = AU_PER_IN / 25.4f
private const val AU_PER_Q = AU_PER_MM / 4
private const val AU_PER_PT = AU_PER_IN / 72
private const val AU_PER_PC = AU_PER_PT * 12

sealed class AbsoluteLength : SpecifiedValue<PixelLength> {

    data class Px(val value: Float) : AbsoluteLength()

    data class In(val value: Float) : AbsoluteLength()

    data class Cm(val value: Float) : AbsoluteLength()

    data class Mm(val value: Float) : AbsoluteLength()

    data class Q(val value: Float) : AbsoluteLength()

    data class Pt(val value: Float) : AbsoluteLength()

    data class Pc(val value: Float) : AbsoluteLength()

    fun toPx(): Float {
        return when (this) {
            is AbsoluteLength.Px -> value
            is AbsoluteLength.In -> value * (AU_PER_IN / AU_PER_PX)
            is AbsoluteLength.Cm -> value * (AU_PER_CM / AU_PER_PX)
            is AbsoluteLength.Mm -> value * (AU_PER_MM / AU_PER_PX)
            is AbsoluteLength.Q -> value * (AU_PER_Q / AU_PER_PX)
            is AbsoluteLength.Pt -> value * (AU_PER_PT / AU_PER_PX)
            is AbsoluteLength.Pc -> value * (AU_PER_PC / AU_PER_PX)
        }
    }

    override fun toComputedValue(context: Context): PixelLength {
        return PixelLength(toPx())
    }

    operator fun plus(length: AbsoluteLength): AbsoluteLength {
        return AbsoluteLength.Px(toPx() + length.toPx())
    }

    operator fun times(scalar: Float): AbsoluteLength {
        return AbsoluteLength.Px(toPx() + scalar)
    }
}

sealed class FontRelativeLength {

    data class Em(val value: Float) : FontRelativeLength()

    data class Ex(val value: Float) : FontRelativeLength()

    data class Ch(val value: Float) : FontRelativeLength()

    data class Rem(val value: Float) : FontRelativeLength()

    fun toComputedValue(context: Context, baseSize: FontBaseSize): PixelLength {
        val (referencedSize, factor) = referencedSizeAndFactor(context, baseSize)
        val pixel = (referencedSize.toFloat() * factor)
                .min(Float.MIN_VALUE)
                .max(Float.MAX_VALUE)
        return PixelLength(pixel)
    }

    internal fun referencedSizeAndFactor(context: Context, baseSize: FontBaseSize): Pair<Au, Float> {
        return when (this) {
            is FontRelativeLength.Em -> {
                if (baseSize is FontBaseSize.InheritStyleButStripEmUnits) {
                    Pair(Au(0), value)
                } else {
                    val referencedFontSize = baseSize.resolve(context)

                    Pair(referencedFontSize, value)
                }
            }
            is FontRelativeLength.Ex -> {
                val referencedFontSize = baseSize.resolve(context)
                val metricsResult = queryFontMetrics(context, referencedFontSize)

                val referenceSize = when (metricsResult) {
                    is FontMetricsQueryResult.Available -> metricsResult.metrics.xHeight
                    is FontMetricsQueryResult.NotAvailable -> referencedFontSize.scaleBy(0.5f)
                }

                Pair(referenceSize, value)
            }
            is FontRelativeLength.Ch -> {
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

                Pair(referenceSize, value)
            }
            is FontRelativeLength.Rem -> {
                val referencedSize = if (context.isRootElement()) {
                    baseSize.resolve(context)
                } else {
                    context.device().rootFontSize()
                }

                Pair(referencedSize, value)
            }
        }
    }

    internal fun queryFontMetrics(context: Context, fontSize: Au): FontMetricsQueryResult {
        return context.fontMetricsProvider.query(
                context.style().getFont(),
                fontSize,
                context.device()
        )
    }
}

sealed class ViewportPercentageLength {

    data class Vw(val value: Float) : ViewportPercentageLength()

    data class Vh(val value: Float) : ViewportPercentageLength()

    data class Vmin(val value: Float) : ViewportPercentageLength()

    data class Vmax(val value: Float) : ViewportPercentageLength()

    fun toComputedValue(context: Context, viewportSize: Size2D<Au>): PixelLength {
        return when (this) {
            is ViewportPercentageLength.Vw -> {
                val au = (viewportSize.width.value * value / 100.0).trunc()

                Au.fromAu64(au).into()
            }
            is ViewportPercentageLength.Vh -> {
                val au = (viewportSize.height.value * value / 100.0).trunc()

                Au.fromAu64(au).into()
            }
            is ViewportPercentageLength.Vmin -> {
                val au = (viewportSize.width.max(viewportSize.height).value * value / 100.0).trunc()

                Au.fromAu64(au).into()
            }
            is ViewportPercentageLength.Vmax -> {
                val au = (viewportSize.width.min(viewportSize.height).value * value / 100.0).trunc()

                Au.fromAu64(au).into()
            }
        }
    }
}

sealed class NoCalcLength : SpecifiedValue<PixelLength> {

    data class Absolute(val length: AbsoluteLength) : NoCalcLength()

    data class FontRelative(val length: FontRelativeLength) : NoCalcLength()

    data class ViewportPercentage(val length: ViewportPercentageLength) : NoCalcLength()

    final override fun toComputedValue(context: Context): PixelLength {
        return when (this) {
            is NoCalcLength.Absolute -> {
                length.toComputedValue(context)
            }
            is NoCalcLength.FontRelative -> {
                length.toComputedValue(context, FontBaseSize.CurrentStyle)
            }
            is NoCalcLength.ViewportPercentage -> {
                val viewportSize = context.viewportSizeForViewportUnitResolution()

                length.toComputedValue(context, viewportSize)
            }
        }
    }

    companion object {

        fun parseDimension(context: ParserContext, value: Float, unit: String): Result<NoCalcLength, Empty> {
            return when (unit.toLowerCase()) {
                "px" -> Ok(Absolute(AbsoluteLength.Px(value)))
                "in" -> Ok(Absolute(AbsoluteLength.In(value)))
                "cm" -> Ok(Absolute(AbsoluteLength.Cm(value)))
                "mm" -> Ok(Absolute(AbsoluteLength.Mm(value)))
                "q" -> Ok(Absolute(AbsoluteLength.Q(value)))
                "pt" -> Ok(Absolute(AbsoluteLength.Pt(value)))
                "pc" -> Ok(Absolute(AbsoluteLength.Pc(value)))

                "em" -> Ok(FontRelative(FontRelativeLength.Em(value)))
                "ex" -> Ok(FontRelative(FontRelativeLength.Ex(value)))
                "ch" -> Ok(FontRelative(FontRelativeLength.Ch(value)))
                "rem" -> Ok(FontRelative(FontRelativeLength.Rem(value)))

                "vw" -> Ok(ViewportPercentage(ViewportPercentageLength.Vw(value)))
                "vh" -> Ok(ViewportPercentage(ViewportPercentageLength.Vh(value)))
                "vmin" -> Ok(ViewportPercentage(ViewportPercentageLength.Vmin(value)))
                "vmax" -> Ok(ViewportPercentage(ViewportPercentageLength.Vmax(value)))

                else -> Err()
            }
        }
    }
}

sealed class Length : SpecifiedValue<PixelLength> {

    data class NoCalc(val length: NoCalcLength) : Length()

    data class Calc(val calc: CalcLengthOrPercentage) : Length()

    final override fun toComputedValue(context: Context): PixelLength {
        return when (this) {
            is Length.NoCalc -> {
                length.toComputedValue(context)
            }
            is Length.Calc -> {
                calc.toComputedValue(context).length()
            }
        }
    }

    companion object {

        fun parse(context: ParserContext, input: Parser): Result<Length, ParseError> {
            return parseQuirky(context, input, AllowQuirks.No)
        }

        fun parseQuirky(context: ParserContext,
                        input: Parser,
                        allowQuirks: AllowQuirks): Result<Length, ParseError> {
            return parseInternal(context, input, ClampingMode.All, allowQuirks)
        }

        fun parseNonNegative(context: ParserContext, input: Parser): Result<Length, ParseError> {
            return parseNonNegativeQuirky(context, input, AllowQuirks.No)
        }

        fun parseNonNegativeQuirky(context: ParserContext,
                                   input: Parser,
                                   allowQuirks: AllowQuirks): Result<Length, ParseError> {
            return parseInternal(context, input, ClampingMode.NonNegative, allowQuirks)
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
                        return Err(location.newError(LengthParseErrorKind.ForbiddenNumeric))
                    }

                    return NoCalcLength.parseDimension(context, token.number.float(), token.unit)
                            .map(Length::NoCalc)
                            .mapErr { location.newUnexpectedTokenError(token) }
                }
                is Token.Number -> {
                    if (!clampingMode.isAllowed(context.parseMode, token.number.float())) {
                        return Err(location.newError(LengthParseErrorKind.ForbiddenNumeric))
                    }

                    if (token.number.float() != 0f
                            && !context.parseMode.allowsUnitlessNumbers()
                            && !allowQuirks.allowed(context.quirksMode)) {
                        return Err(location.newError(LengthParseErrorKind.UnitlessNumber))
                    }

                    return Ok(NoCalc(NoCalcLength.Absolute(AbsoluteLength.Px(token.number.float()))))
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

data class NonNegativeLength(val length: Length) : SpecifiedValue<ComputedNonNegativeLength> {

    override fun toComputedValue(context: Context): ComputedNonNegativeLength {
        return ComputedNonNegativeLength(length.toComputedValue(context))
    }

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

    data class Length(val length: NoCalcLength) : LengthOrPercentage()

    data class Percentage(val percentage: org.fernice.flare.style.value.specified.Percentage) : LengthOrPercentage()

    data class Calc(val calc: CalcLengthOrPercentage) : LengthOrPercentage()

    final override fun toComputedValue(context: Context): ComputedLengthOrPercentage {
        return when (this) {
            is LengthOrPercentage.Length -> {
                ComputedLengthOrPercentage.Length(length.toComputedValue(context))
            }
            is LengthOrPercentage.Percentage -> {
                ComputedLengthOrPercentage.Percentage(percentage.toComputedValue(context))
            }
            is LengthOrPercentage.Calc -> {
                ComputedLengthOrPercentage.Calc(calc.toComputedValue(context))
            }
        }
    }

    companion object {

        fun parse(context: ParserContext, input: Parser): Result<LengthOrPercentage, ParseError> {
            return parseQuirky(context, input, AllowQuirks.No)
        }

        fun parseQuirky(context: ParserContext,
                        input: Parser,
                        allowQuirks: AllowQuirks): Result<LengthOrPercentage, ParseError> {
            return parseInternal(context, input, ClampingMode.All, allowQuirks)
        }

        fun parseNonNegative(context: ParserContext, input: Parser): Result<LengthOrPercentage, ParseError> {
            return parseNonNegativeQuirky(context, input, AllowQuirks.No)
        }

        fun parseNonNegativeQuirky(context: ParserContext,
                                   input: Parser,
                                   allowQuirks: AllowQuirks): Result<LengthOrPercentage, ParseError> {
            return parseInternal(context, input, ClampingMode.NonNegative, allowQuirks)
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
                        return Err(location.newError(LengthParseErrorKind.ForbiddenNumeric))
                    }

                    return NoCalcLength.parseDimension(context, token.number.float(), token.unit)
                            .map(LengthOrPercentage::Length)
                            .mapErr { location.newUnexpectedTokenError(token) }
                }
                is Token.Percentage -> {
                    if (!clampingMode.isAllowed(context.parseMode, token.number.float())) {
                        return Err(location.newError(LengthParseErrorKind.ForbiddenNumeric))
                    }

                    return Ok(Percentage(Percentage(token.number.float())))
                }
                is Token.Number -> {
                    if (!clampingMode.isAllowed(context.parseMode, token.number.float())) {
                        return Err(location.newError(LengthParseErrorKind.ForbiddenNumeric))
                    }

                    if (token.number.float() != 0f
                            && !context.parseMode.allowsUnitlessNumbers()
                            && !allowQuirks.allowed(context.quirksMode)) {
                        return Err(location.newError(LengthParseErrorKind.UnitlessNumber))
                    }

                    return Ok(Length(NoCalcLength.Absolute(AbsoluteLength.Px(token.number.float()))))
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
}

data class NonNegativeLengthOrPercentage(val value: LengthOrPercentage) : SpecifiedValue<ComputedNonNegativeLengthOrPercentage> {

    override fun toComputedValue(context: Context): ComputedNonNegativeLengthOrPercentage {
        return ComputedNonNegativeLengthOrPercentage(value.toComputedValue(context))
    }

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
}

sealed class LengthOrPercentageOrAuto : SpecifiedValue<ComputedLengthOrPercentageOrAuto> {

    data class Length(val length: NoCalcLength) : LengthOrPercentageOrAuto()

    data class Percentage(val percentage: org.fernice.flare.style.value.specified.Percentage) : LengthOrPercentageOrAuto()

    data class Calc(val calc: CalcLengthOrPercentage) : LengthOrPercentageOrAuto()

    object Auto : LengthOrPercentageOrAuto()

    final override fun toComputedValue(context: Context): ComputedLengthOrPercentageOrAuto {
        return when (this) {
            is LengthOrPercentageOrAuto.Length -> {
                ComputedLengthOrPercentageOrAuto.Length(length.toComputedValue(context))
            }
            is LengthOrPercentageOrAuto.Percentage -> {
                ComputedLengthOrPercentageOrAuto.Percentage(percentage.toComputedValue(context))
            }
            is LengthOrPercentageOrAuto.Calc -> {
                ComputedLengthOrPercentageOrAuto.Calc(calc.toComputedValue(context))
            }
            is LengthOrPercentageOrAuto.Auto -> {
                ComputedLengthOrPercentageOrAuto.Auto
            }
        }
    }

    companion object {

        fun parse(context: ParserContext, input: Parser): Result<LengthOrPercentageOrAuto, ParseError> {
            return parseQuirky(context, input, AllowQuirks.No)
        }

        fun parseQuirky(context: ParserContext,
                        input: Parser,
                        allowQuirks: AllowQuirks): Result<LengthOrPercentageOrAuto, ParseError> {
            return parseInternal(context, input, ClampingMode.All, allowQuirks)
        }

        fun parseNonNegative(context: ParserContext, input: Parser): Result<LengthOrPercentageOrAuto, ParseError> {
            return parseNonNegativeQuirky(context, input, AllowQuirks.No)
        }

        fun parseNonNegativeQuirky(context: ParserContext,
                                   input: Parser,
                                   allowQuirks: AllowQuirks): Result<LengthOrPercentageOrAuto, ParseError> {
            return parseInternal(context, input, ClampingMode.NonNegative, allowQuirks)
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
                        return Err(location.newError(LengthParseErrorKind.ForbiddenNumeric))
                    }

                    return NoCalcLength.parseDimension(context, token.number.float(), token.unit)
                            .map(LengthOrPercentageOrAuto::Length)
                            .mapErr { location.newUnexpectedTokenError(token) }
                }
                is Token.Percentage -> {
                    if (!clampingMode.isAllowed(context.parseMode, token.number.float())) {
                        return Err(location.newError(LengthParseErrorKind.ForbiddenNumeric))
                    }

                    return Ok(Percentage(Percentage(token.number.float())))
                }
                is Token.Number -> {
                    if (!clampingMode.isAllowed(context.parseMode, token.number.float())) {
                        return Err(location.newError(LengthParseErrorKind.ForbiddenNumeric))
                    }

                    if (token.number.float() != 0f
                            && !context.parseMode.allowsUnitlessNumbers()
                            && !allowQuirks.allowed(context.quirksMode)) {
                        return Err(location.newError(LengthParseErrorKind.UnitlessNumber))
                    }

                    return Ok(Length(NoCalcLength.Absolute(AbsoluteLength.Px(token.number.float()))))
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

                    return Ok(Auto)
                }
                else -> {
                    return Err(location.newUnexpectedTokenError(token))
                }
            }
        }
    }
}

data class NonNegativeLengthOrPercentageOrAuto(val value: LengthOrPercentageOrAuto) : SpecifiedValue<ComputedNonNegativeLengthOrPercentageOrAuto> {

    override fun toComputedValue(context: Context): ComputedNonNegativeLengthOrPercentageOrAuto {
        return ComputedNonNegativeLengthOrPercentageOrAuto(value.toComputedValue(context))
    }

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

        private val auto: NonNegativeLengthOrPercentageOrAuto by lazy {
            NonNegativeLengthOrPercentageOrAuto(
                    LengthOrPercentageOrAuto.Auto
            )
        }

        fun auto(): NonNegativeLengthOrPercentageOrAuto {
            return auto
        }
    }
}

sealed class LengthOrPercentageOrNone : SpecifiedValue<ComputedLengthOrPercentageOrNone> {

    data class Length(val length: NoCalcLength) : LengthOrPercentageOrNone()

    data class Percentage(val percentage: org.fernice.flare.style.value.specified.Percentage) : LengthOrPercentageOrNone()

    data class Calc(val calc: CalcLengthOrPercentage) : LengthOrPercentageOrNone()

    object None : LengthOrPercentageOrNone()

    final override fun toComputedValue(context: Context): ComputedLengthOrPercentageOrNone {
        return when (this) {
            is LengthOrPercentageOrNone.Length -> {
                ComputedLengthOrPercentageOrNone.Length(length.toComputedValue(context))
            }
            is LengthOrPercentageOrNone.Percentage -> {
                ComputedLengthOrPercentageOrNone.Percentage(percentage.toComputedValue(context))
            }
            is LengthOrPercentageOrNone.Calc -> {
                ComputedLengthOrPercentageOrNone.Calc(calc.toComputedValue(context))
            }
            is LengthOrPercentageOrNone.None -> {
                ComputedLengthOrPercentageOrNone.None
            }
        }
    }

    companion object {

        fun parse(context: ParserContext, input: Parser): Result<LengthOrPercentageOrNone, ParseError> {
            return parseQuirky(context, input, AllowQuirks.No)
        }

        fun parseQuirky(context: ParserContext,
                        input: Parser,
                        allowQuirks: AllowQuirks): Result<LengthOrPercentageOrNone, ParseError> {
            return parseInternal(context, input, ClampingMode.All, allowQuirks)
        }

        fun parseNonNegative(context: ParserContext, input: Parser): Result<LengthOrPercentageOrNone, ParseError> {
            return parseNonNegativeQuirky(context, input, AllowQuirks.No)
        }

        fun parseNonNegativeQuirky(context: ParserContext,
                                   input: Parser,
                                   allowQuirks: AllowQuirks): Result<LengthOrPercentageOrNone, ParseError> {
            return parseInternal(context, input, ClampingMode.NonNegative, allowQuirks)
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
                        return Err(location.newError(LengthParseErrorKind.ForbiddenNumeric))
                    }

                    return NoCalcLength.parseDimension(context, token.number.float(), token.unit)
                            .map(LengthOrPercentageOrNone::Length)
                            .mapErr { location.newUnexpectedTokenError(token) }
                }
                is Token.Percentage -> {
                    if (!clampingMode.isAllowed(context.parseMode, token.number.float())) {
                        return Err(location.newError(LengthParseErrorKind.ForbiddenNumeric))
                    }

                    return Ok(Percentage(Percentage(token.number.float())))
                }
                is Token.Number -> {
                    if (!clampingMode.isAllowed(context.parseMode, token.number.float())) {
                        return Err(location.newError(LengthParseErrorKind.ForbiddenNumeric))
                    }

                    if (token.number.float() != 0f
                            && !context.parseMode.allowsUnitlessNumbers()
                            && !allowQuirks.allowed(context.quirksMode)) {
                        return Err(location.newError(LengthParseErrorKind.UnitlessNumber))
                    }

                    return Ok(Length(NoCalcLength.Absolute(AbsoluteLength.Px(token.number.float()))))
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

                    return Ok(None)
                }
                else -> {
                    return Err(location.newUnexpectedTokenError(token))
                }
            }
        }
    }
}

data class NonNegativeLengthOrPercentageOrNone(val value: LengthOrPercentageOrNone) : SpecifiedValue<ComputedNonNegativeLengthOrPercentageOrNone> {

    override fun toComputedValue(context: Context): ComputedNonNegativeLengthOrPercentageOrNone {
        return ComputedNonNegativeLengthOrPercentageOrNone(value.toComputedValue(context))
    }

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
}
