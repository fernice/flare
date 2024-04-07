/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.value.specified

import org.fernice.std.Err
import org.fernice.std.Ok
import org.fernice.std.Result
import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.ParseErrorKind
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.cssparser.ToCss
import org.fernice.flare.cssparser.Token
import org.fernice.flare.cssparser.newError
import org.fernice.flare.cssparser.newUnexpectedTokenError
import org.fernice.flare.font.FontMetricsQueryResult
import org.fernice.std.trunc
import org.fernice.flare.style.AllowQuirks
import org.fernice.flare.style.ClampingMode
import org.fernice.flare.style.ParserContext
import org.fernice.flare.style.value.Context
import org.fernice.flare.style.value.FontBaseSize
import org.fernice.flare.style.value.SpecifiedValue
import org.fernice.flare.style.value.computed.Au
import org.fernice.flare.style.value.computed.PixelLength
import org.fernice.flare.style.value.computed.toAu
import org.fernice.flare.style.value.generic.Size2D
import org.fernice.std.map
import org.fernice.std.mapErr
import java.io.Writer
import org.fernice.flare.style.value.computed.LengthOrPercentage as ComputedLengthOrPercentage
import org.fernice.flare.style.value.computed.LengthOrPercentageOrAuto as ComputedLengthOrPercentageOrAuto
import org.fernice.flare.style.value.computed.LengthOrPercentageOrNone as ComputedLengthOrPercentageOrNone
import org.fernice.flare.style.value.computed.NonNegativeLength as ComputedNonNegativeLength
import org.fernice.flare.style.value.computed.NonNegativeLengthOrPercentage as ComputedNonNegativeLengthOrPercentage
import org.fernice.flare.style.value.computed.NonNegativeLengthOrPercentageOrAuto as ComputedNonNegativeLengthOrPercentageOrAuto
import org.fernice.flare.style.value.computed.NonNegativeLengthOrPercentageOrNone as ComputedNonNegativeLengthOrPercentageOrNone
import org.fernice.flare.style.value.computed.Percentage as ComputedPercentage

/**
 * Parse error kinds specific to the length parsing.
 */
sealed class LengthParseErrorKind : ParseErrorKind() {

    object ForbiddenNumeric : LengthParseErrorKind()
    object UnitlessNumber : LengthParseErrorKind()

    override fun toString(): String {
        return "LengthParseErrorKind::${javaClass.simpleName}"
    }
}

/*
 * Constants for converting length units into Au.
 *
 * NOTE: Au might be be dropped from flare completely as our peers do not onto such units,
 * but rather on pixels. Also Au is primarily a unit for layouting.
 */
private const val AU_PER_PX = 60
private const val AU_PER_IN = AU_PER_PX * 96
private const val AU_PER_CM = AU_PER_IN / 2.54f
private const val AU_PER_MM = AU_PER_IN / 25.4f
private const val AU_PER_Q = AU_PER_MM / 4
private const val AU_PER_PT = AU_PER_IN / 72
private const val AU_PER_PC = AU_PER_PT * 12

/**
 * Represents a absolute length specified under [CSS Values and Units Module Level 3](https://www.w3.org/TR/css3-values/#absolute-lengths).
 *
 * The parsing of this type is done in [NoCalcLength].
 */
sealed class AbsoluteLength : SpecifiedValue<PixelLength>, ToCss {

    data class Px(val value: Float) : AbsoluteLength()
    data class In(val value: Float) : AbsoluteLength()
    data class Cm(val value: Float) : AbsoluteLength()
    data class Mm(val value: Float) : AbsoluteLength()
    data class Q(val value: Float) : AbsoluteLength()
    data class Pt(val value: Float) : AbsoluteLength()
    data class Pc(val value: Float) : AbsoluteLength()

    /**
     * Returns the pixel value of the absolute length.
     */
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

    final override fun toComputedValue(context: Context): PixelLength {
        return PixelLength(toPx())
    }

    final override fun toCss(writer: Writer) {
        writer.append(
            when (this) {
                is AbsoluteLength.Px -> "${value}px"
                is AbsoluteLength.In -> "${value}in"
                is AbsoluteLength.Cm -> "${value}cm"
                is AbsoluteLength.Mm -> "${value}mm"
                is AbsoluteLength.Q -> "${value}q"
                is AbsoluteLength.Pt -> "${value}pt"
                is AbsoluteLength.Pc -> "${value}pc"
            }
        )
    }

    /**
     * Adds two AbsoluteLength and returns a AbsoluteLength. After the addition the type will automatically be
     * [AbsoluteLength.Px] independent of the input types.
     */
    operator fun plus(length: AbsoluteLength): AbsoluteLength {
        return AbsoluteLength.Px(toPx() + length.toPx())
    }

    /**
     * Multiplies two AbsoluteLength and returns a AbsoluteLength. After the multiplication the type will automatically be
     * [AbsoluteLength.Px] independent of the input types.
     */
    operator fun times(scalar: Float): AbsoluteLength {
        return AbsoluteLength.Px(toPx() + scalar)
    }
}

/**
 * Represents a font relative length as specified under [CSS Values and Units Module Level 3](https://www.w3.org/TR/css3-values/#font-relative-lengths)
 */
sealed class FontRelativeLength : ToCss {

    data class Em(val value: Float) : FontRelativeLength()
    data class Ex(val value: Float) : FontRelativeLength()
    data class Ch(val value: Float) : FontRelativeLength()
    data class Rem(val value: Float) : FontRelativeLength()

    /**
     * Returns a value representing the sign of the value. If the sign is negative the returns value be
     * negative value. There is no guarantee that the return value will be within a concrete range.
     */
    fun sign(): Float {
        return when (this) {
            is FontRelativeLength.Em -> value
            is FontRelativeLength.Ex -> value
            is FontRelativeLength.Ch -> value
            is FontRelativeLength.Rem -> value
        }
    }

    /**
     * Converts this value its computed representation relative to the specified [FontBaseSize].
     */
    fun toComputedValue(context: Context, baseSize: FontBaseSize): PixelLength {
        val (referencedSize, factor) = referencedSizeAndFactor(context, baseSize)
        val pixel = (referencedSize.toFloat() * factor)
            .coerceAtLeast(Float.MIN_VALUE)
            .coerceAtMost(Float.MAX_VALUE)
        return PixelLength(pixel)
    }

    /**
     * Internal implementation for converting the type into it computed representation relative to the
     * specified [FontBaseSize].
     */
    private fun referencedSizeAndFactor(context: Context, baseSize: FontBaseSize): Pair<Au, Float> {
        fun queryFontMetrics(context: Context, fontSize: Au): FontMetricsQueryResult {
            return context.fontMetricsProvider.query(
                context.style().getFont(),
                fontSize,
                context.device()
            )
        }

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

                val referenceSize = when (val metricsResult = queryFontMetrics(context, referencedFontSize)) {
                    is FontMetricsQueryResult.Available -> metricsResult.metrics.xHeight
                    is FontMetricsQueryResult.NotAvailable -> referencedFontSize.scaleBy(0.5f)
                }

                Pair(referenceSize, value)
            }
            is FontRelativeLength.Ch -> {
                val referencedFontSize = baseSize.resolve(context)

                val referenceSize = when (val metricsResult = queryFontMetrics(context, referencedFontSize)) {
                    is FontMetricsQueryResult.Available -> metricsResult.metrics.zeroAdvanceMeasure
                    is FontMetricsQueryResult.NotAvailable -> referencedFontSize.scaleBy(0.5f)
                }

                Pair(referenceSize, value)
            }
            is FontRelativeLength.Rem -> {
                val referencedSize = if (context.isRootElement()) {
                    baseSize.resolve(context)
                } else {
                    context.device().rootFontSize
                }

                Pair(referencedSize, value)
            }
        }
    }

    final override fun toCss(writer: Writer) {
        writer.append(
            when (this) {
                is FontRelativeLength.Em -> "${value}em"
                is FontRelativeLength.Ex -> "${value}ex"
                is FontRelativeLength.Ch -> "${value}ch"
                is FontRelativeLength.Rem -> "${value}rem"
            }
        )
    }
}

/**
 * Represents a viewport percentage length as specified under
 * [CSS Values and Units Module Level 3](https://www.w3.org/TR/css3-values/#viewport-relative-lengths)
 */
sealed class ViewportPercentageLength : ToCss {

    data class Vw(val value: Float) : ViewportPercentageLength()
    data class Vh(val value: Float) : ViewportPercentageLength()
    data class Vmin(val value: Float) : ViewportPercentageLength()
    data class Vmax(val value: Float) : ViewportPercentageLength()

    /**
     * Returns a value representing the sign of the value. If the sign is negative the returns value be
     * negative value. There is no guarantee that the return value will be within a concrete range.
     */
    fun sign(): Float {
        return when (this) {
            is ViewportPercentageLength.Vw -> value
            is ViewportPercentageLength.Vh -> value
            is ViewportPercentageLength.Vmin -> value
            is ViewportPercentageLength.Vmax -> value
        }
    }

    /**
     * Converts this type into its computed value relative to the specified viewport size.
     */
    fun toComputedValue(context: Context, viewportSize: Size2D<Au>): PixelLength {
        return when (this) {
            is ViewportPercentageLength.Vw -> {
                val au = (viewportSize.width.value * value / 100.0).trunc()

                Au.fromAu64(au).toAu()
            }
            is ViewportPercentageLength.Vh -> {
                val au = (viewportSize.height.value * value / 100.0).trunc()

                Au.fromAu64(au).toAu()
            }
            is ViewportPercentageLength.Vmin -> {
                val au = (viewportSize.width.max(viewportSize.height).value * value / 100.0).trunc()

                Au.fromAu64(au).toAu()
            }
            is ViewportPercentageLength.Vmax -> {
                val au = (viewportSize.width.min(viewportSize.height).value * value / 100.0).trunc()

                Au.fromAu64(au).toAu()
            }
        }
    }

    final override fun toCss(writer: Writer) {
        writer.append(
            when (this) {
                is ViewportPercentageLength.Vw -> "${value}vw"
                is ViewportPercentageLength.Vh -> "${value}vh"
                is ViewportPercentageLength.Vmin -> "${value}vmin"
                is ViewportPercentageLength.Vmax -> "${value}vmax"
            }
        )
    }
}

/**
 * Represents a length that does not involve a calc expression. It may only be a absolute, font relative, viewport
 * percentage length.
 */
sealed class NoCalcLength : SpecifiedValue<PixelLength>, ToCss {

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

    final override fun toCss(writer: Writer) {
        when (this) {
            is NoCalcLength.Absolute -> length.toCss(writer)
            is NoCalcLength.FontRelative -> length.toCss(writer)
            is ViewportPercentage -> length.toCss(writer)
        }
    }

    companion object {

        /**
         * Parses a length by of specified [unit] and returns the appropriate type representing the type.
         */
        fun parseDimension(context: ParserContext, value: Float, unit: String): Result<NoCalcLength, Unit> {
            return when (unit.lowercase()) {
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

        val Zero by lazy { NoCalcLength.Absolute(AbsoluteLength.Px(0f)) }
    }
}

/**
 * Represents a length, consisting either out of a concrete value or a calc expression.
 */
sealed class Length : SpecifiedValue<PixelLength>, ToCss {

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

    final override fun toCss(writer: Writer) {
        when (this) {
            is Length.NoCalc -> length.toCss(writer)
            is Length.Calc -> calc.toCss(writer)
        }
    }

    companion object {

        /**
         * Tries to parse a length, returns [Ok] bearing the length if the parsing is successful, otherwise returns [Err]
         * containing the parse error.
         */
        fun parse(context: ParserContext, input: Parser): Result<Length, ParseError> {
            return parseQuirky(context, input, AllowQuirks.No)
        }

        /**
         * Tries to parse a length with potential quirks, returns [Ok] bearing the length if the parsing is successful,
         * otherwise returns [Err] containing the parse error.
         */
        fun parseQuirky(
            context: ParserContext,
            input: Parser,
            allowQuirks: AllowQuirks
        ): Result<Length, ParseError> {
            return parseInternal(context, input, ClampingMode.All, allowQuirks)
        }

        /**
         * Tries to parse a non negative length, returns [Ok] bearing the length if the parsing is successful, otherwise
         * returns [Err] containing the parse error.
         */
        fun parseNonNegative(context: ParserContext, input: Parser): Result<Length, ParseError> {
            return parseNonNegativeQuirky(context, input, AllowQuirks.No)
        }

        /**
         * Tries to parse a non negative length with potential quirks, returns [Ok] bearing the length if the parsing is
         * successful, otherwise returns [Err] containing the parse error.
         */
        fun parseNonNegativeQuirky(
            context: ParserContext,
            input: Parser,
            allowQuirks: AllowQuirks
        ): Result<Length, ParseError> {
            return parseInternal(context, input, ClampingMode.NonNegative, allowQuirks)
        }

        /**
         * Internal implementation of parsing a length with potential quirks and numeric [ClampingMode], returns [Ok]
         * bearing the length if the parsing is successful, otherwise returns [Err] containing the parse error.
         */
        private fun parseInternal(
            context: ParserContext,
            input: Parser,
            clampingMode: ClampingMode,
            allowQuirks: AllowQuirks
        ): Result<Length, ParseError> {
            val location = input.sourceLocation()

            val token = when (val tokenResult = input.next()) {
                is Ok -> tokenResult.value
                is Err -> return tokenResult
            }

            return when (token) {
                is Token.Dimension -> {
                    if (!clampingMode.isAllowed(context.parseMode, token.number.float())) {
                        return Err(location.newError(LengthParseErrorKind.ForbiddenNumeric))
                    }

                    NoCalcLength.parseDimension(context, token.number.float(), token.unit)
                        .map(Length::NoCalc)
                        .mapErr { location.newUnexpectedTokenError(token) }
                }
                is Token.Number -> {
                    if (!clampingMode.isAllowed(context.parseMode, token.number.float())) {
                        return Err(location.newError(LengthParseErrorKind.ForbiddenNumeric))
                    }

                    if (token.number.float() != 0f
                        && !context.parseMode.allowsUnitlessNumbers()
                        && !allowQuirks.allowed(context.quirksMode)
                    ) {
                        return Err(location.newError(LengthParseErrorKind.UnitlessNumber))
                    }

                    Ok(NoCalc(NoCalcLength.Absolute(AbsoluteLength.Px(token.number.float()))))
                }
                is Token.Function -> {
                    if (!token.name.equals("calc", true)) {
                        return Err(location.newUnexpectedTokenError(token))
                    }

                    input.parseNestedBlock { nestedParser ->
                        CalcNode.parseLength(context, nestedParser, clampingMode)
                            .map(Length::Calc)
                    }
                }
                else -> Err(location.newUnexpectedTokenError(token))
            }
        }
    }
}

/**
 * Represents a non negative length, consisting either out of a concrete value or a calc expression.
 */
data class NonNegativeLength(val length: Length) : SpecifiedValue<ComputedNonNegativeLength>, ToCss {

    override fun toComputedValue(context: Context): ComputedNonNegativeLength {
        return ComputedNonNegativeLength(length.toComputedValue(context))
    }

    override fun toCss(writer: Writer) = length.toCss(writer)

    companion object {

        /**
         * Tries to parse a non negative length, returns [Ok] bearing the length if the parsing is successful, otherwise
         * returns [Err] containing the parse error.
         */
        fun parse(context: ParserContext, input: Parser): Result<NonNegativeLength, ParseError> {
            return Length.parseNonNegative(context, input)
                .map(::NonNegativeLength)
        }

        /**
         * Tries to parse a non negative length with potential quirks, returns [Ok] bearing the length if the parsing is
         * successful, otherwise returns [Err] containing the parse error.
         */
        fun parseQuirky(
            context: ParserContext,
            input: Parser,
            allowQuirks: AllowQuirks
        ): Result<NonNegativeLength, ParseError> {
            return Length.parseQuirky(context, input, allowQuirks)
                .map(::NonNegativeLength)
        }
    }
}

/**
 * Represents a percentage length. The percentage is represented by value from 0 to 1 translating to 0% to 100%
 * respectively The value may exceed the the range.
 */
data class Percentage(val value: Float) : SpecifiedValue<ComputedPercentage>, ToCss {

    override fun toComputedValue(context: Context): ComputedPercentage {
        return ComputedPercentage(value)
    }

    override fun toCss(writer: Writer) {
        writer.append("${value * 100f}%")
    }

    companion object {

        /**
         * Tries to parse a percentage length, returns [Ok] bearing the percentage if the parsing is successful, otherwise
         * returns [Err] containing the parse error.
         */
        fun parse(context: ParserContext, input: Parser): Result<Percentage, ParseError> {
            val location = input.sourceLocation()

            val token = when (val token = input.next()) {
                is Ok -> token.value
                is Err -> return token
            }

            return when (token) {
                is Token.Percentage -> Ok(Percentage(token.number.float()))
                else -> Err(location.newUnexpectedTokenError(token))
            }
        }

        // lazy instance to prevent allocation per call
        private val zero: Percentage by lazy { Percentage(0f) }
        private val fifty: Percentage by lazy { Percentage(0.5f) }
        private val hundred: Percentage by lazy { Percentage(1f) }

        /**
         * Returns a percentage that is set to zero.
         */
        fun zero(): Percentage {
            return zero
        }

        /**
         * Returns a percentage that is det to fifty.
         */
        fun fifty(): Percentage {
            return fifty
        }

        /**
         * Returns a percentage that is det to Hundred.
         */
        fun hundred(): Percentage {
            return hundred
        }
    }
}

fun Percentage.intoLengthOrPercentage(): LengthOrPercentage {
    return LengthOrPercentage.Percentage(this)
}

/**
 * Represents a length or percentage, that consists either out of a concrete value, a percentage or a calc expression.
 */
sealed class LengthOrPercentage : SpecifiedValue<ComputedLengthOrPercentage>, ToCss {

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

    final override fun toCss(writer: Writer) {
        when (this) {
            is LengthOrPercentage.Length -> length.toCss(writer)
            is LengthOrPercentage.Percentage -> percentage.toCss(writer)
            is LengthOrPercentage.Calc -> calc.toCss(writer)
        }
    }

    companion object {

        /**
         * Tries to parse a length or percentage, returns [Ok] bearing the length if the parsing is successful, otherwise
         * returns [Err] containing the parse error.
         */
        fun parse(context: ParserContext, input: Parser): Result<LengthOrPercentage, ParseError> {
            return parseQuirky(context, input, AllowQuirks.No)
        }

        /**
         * Tries to parse a length or percentage with potential quirks, returns [Ok] bearing the length if the parsing is
         * successful, otherwise returns [Err] containing the parse error.
         */
        fun parseQuirky(
            context: ParserContext,
            input: Parser,
            allowQuirks: AllowQuirks
        ): Result<LengthOrPercentage, ParseError> {
            return parseInternal(context, input, ClampingMode.All, allowQuirks)
        }

        /**
         * Tries to parse a non negative length or percentage, returns [Ok] bearing the length if the parsing is successful,
         * otherwise returns [Err] containing the parse error.
         */
        fun parseNonNegative(context: ParserContext, input: Parser): Result<LengthOrPercentage, ParseError> {
            return parseNonNegativeQuirky(context, input, AllowQuirks.No)
        }

        /**
         * Tries to parse a non negative length or percentage with potential quirks, returns [Ok] bearing the
         * length if the parsing is successful, otherwise returns [Err] containing the parse error.
         */
        fun parseNonNegativeQuirky(
            context: ParserContext,
            input: Parser,
            allowQuirks: AllowQuirks
        ): Result<LengthOrPercentage, ParseError> {
            return parseInternal(context, input, ClampingMode.NonNegative, allowQuirks)
        }

        /**
         * Internal implementation of parsing a length or percentage with potential quirks and numeric [ClampingMode],
         * returns [Ok] bearing the length if the parsing is successful, otherwise returns [Err] containing the parse error.
         */
        private fun parseInternal(
            context: ParserContext,
            input: Parser,
            clampingMode: ClampingMode,
            allowQuirks: AllowQuirks
        ): Result<LengthOrPercentage, ParseError> {
            val location = input.sourceLocation()

            val token = when (val token = input.next()) {
                is Ok -> token.value
                is Err -> return token
            }

            return when (token) {
                is Token.Dimension -> {
                    if (!clampingMode.isAllowed(context.parseMode, token.number.float())) {
                        return Err(location.newError(LengthParseErrorKind.ForbiddenNumeric))
                    }

                    NoCalcLength.parseDimension(context, token.number.float(), token.unit)
                        .map(LengthOrPercentage::Length)
                        .mapErr { location.newUnexpectedTokenError(token) }
                }
                is Token.Percentage -> {
                    if (!clampingMode.isAllowed(context.parseMode, token.number.float())) {
                        return Err(location.newError(LengthParseErrorKind.ForbiddenNumeric))
                    }

                    Ok(Percentage(Percentage(token.number.float())))
                }
                is Token.Number -> {
                    if (!clampingMode.isAllowed(context.parseMode, token.number.float())) {
                        return Err(location.newError(LengthParseErrorKind.ForbiddenNumeric))
                    }

                    if (token.number.float() != 0f
                        && !context.parseMode.allowsUnitlessNumbers()
                        && !allowQuirks.allowed(context.quirksMode)
                    ) {
                        return Err(location.newError(LengthParseErrorKind.UnitlessNumber))
                    }

                    Ok(Length(NoCalcLength.Absolute(AbsoluteLength.Px(token.number.float()))))
                }
                is Token.Function -> {
                    if (!token.name.equals("calc", true)) {
                        return Err(location.newUnexpectedTokenError(token))
                    }

                    input.parseNestedBlock { parser ->
                        CalcNode.parseLength(context, parser, clampingMode)
                            .map(LengthOrPercentage::Calc)
                    }
                }
                else -> Err(location.newUnexpectedTokenError(token))

            }
        }

        val Zero by lazy { LengthOrPercentage.Length(NoCalcLength.Zero) }
    }
}

/**
 * Represents a non negative length or percentage, consisting out of either a concrete value, a percentage or a calc expression.
 */
data class NonNegativeLengthOrPercentage(val value: LengthOrPercentage) : SpecifiedValue<ComputedNonNegativeLengthOrPercentage>, ToCss {

    override fun toComputedValue(context: Context): ComputedNonNegativeLengthOrPercentage {
        return ComputedNonNegativeLengthOrPercentage(value.toComputedValue(context))
    }

    override fun toCss(writer: Writer) = value.toCss(writer)

    companion object {

        /**
         * Tries to parse a non negative length or percentage, returns [Ok] bearing the length if the parsing is successful,
         * otherwise returns [Err] containing the parse error.
         */
        fun parse(context: ParserContext, input: Parser): Result<NonNegativeLengthOrPercentage, ParseError> {
            return LengthOrPercentage.parseNonNegative(context, input)
                .map(::NonNegativeLengthOrPercentage)
        }

        /**
         * Tries to parse a non negative length or percentage or percentage with potential quirks, returns [Ok] bearing the
         * length if the parsing is successful, otherwise returns [Err] containing the parse error.
         */
        fun parseQuirky(
            context: ParserContext,
            input: Parser,
            allowQuirks: AllowQuirks
        ): Result<NonNegativeLengthOrPercentage, ParseError> {
            return LengthOrPercentage.parseQuirky(context, input, allowQuirks)
                .map(::NonNegativeLengthOrPercentage)
        }
    }
}

/**
 * Represents a length, percentage or auto, consisting out of either a concrete value, a percentage, a calc
 * expression or the keyword `auto`.
 */
sealed class LengthOrPercentageOrAuto : SpecifiedValue<ComputedLengthOrPercentageOrAuto>, ToCss {

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

    final override fun toCss(writer: Writer) {
        when (this) {
            is LengthOrPercentageOrAuto.Length -> length.toCss(writer)
            is LengthOrPercentageOrAuto.Percentage -> percentage.toCss(writer)
            is LengthOrPercentageOrAuto.Calc -> calc.toCss(writer)
            is LengthOrPercentageOrAuto.Auto -> writer.append("auto")
        }
    }

    companion object {

        /**
         * Tries to parse a length, percentage or auto, returns [Ok] bearing the length if the parsing is successful, otherwise
         * returns [Err] containing the parse error.
         */
        fun parse(context: ParserContext, input: Parser): Result<LengthOrPercentageOrAuto, ParseError> {
            return parseQuirky(context, input, AllowQuirks.No)
        }

        /**
         * Tries to parse a length, percentage or auto with potential quirks, returns [Ok] bearing the length if the parsing is
         * successful, otherwise returns [Err] containing the parse error.
         */
        fun parseQuirky(
            context: ParserContext,
            input: Parser,
            allowQuirks: AllowQuirks
        ): Result<LengthOrPercentageOrAuto, ParseError> {
            return parseInternal(context, input, ClampingMode.All, allowQuirks)
        }

        /**
         * Tries to parse a non negative length, percentage or auto, returns [Ok] bearing the length if the parsing is successful,
         * otherwise returns [Err] containing the parse error.
         */
        fun parseNonNegative(context: ParserContext, input: Parser): Result<LengthOrPercentageOrAuto, ParseError> {
            return parseNonNegativeQuirky(context, input, AllowQuirks.No)
        }

        /**
         * Tries to parse a non negative length, percentage or auto with potential quirks, returns [Ok] bearing the
         * length if the parsing is successful, otherwise returns [Err] containing the parse error.
         */
        fun parseNonNegativeQuirky(
            context: ParserContext,
            input: Parser,
            allowQuirks: AllowQuirks
        ): Result<LengthOrPercentageOrAuto, ParseError> {
            return parseInternal(context, input, ClampingMode.NonNegative, allowQuirks)
        }

        /**
         * Internal implementation of parsing a length, percentage or auto with potential quirks and numeric [ClampingMode],
         * returns [Ok] bearing the length if the parsing is successful, otherwise returns [Err] containing the parse error.
         */
        private fun parseInternal(
            context: ParserContext,
            input: Parser,
            clampingMode: ClampingMode,
            allowQuirks: AllowQuirks
        ): Result<LengthOrPercentageOrAuto, ParseError> {
            val location = input.sourceLocation()

            val token = when (val token = input.next()) {
                is Ok -> token.value
                is Err -> return token
            }

            return when (token) {
                is Token.Dimension -> {
                    if (!clampingMode.isAllowed(context.parseMode, token.number.float())) {
                        return Err(location.newError(LengthParseErrorKind.ForbiddenNumeric))
                    }

                    NoCalcLength.parseDimension(context, token.number.float(), token.unit)
                        .map(LengthOrPercentageOrAuto::Length)
                        .mapErr { location.newUnexpectedTokenError(token) }
                }
                is Token.Percentage -> {
                    if (!clampingMode.isAllowed(context.parseMode, token.number.float())) {
                        return Err(location.newError(LengthParseErrorKind.ForbiddenNumeric))
                    }

                    Ok(Percentage(Percentage(token.number.float())))
                }
                is Token.Number -> {
                    if (!clampingMode.isAllowed(context.parseMode, token.number.float())) {
                        return Err(location.newError(LengthParseErrorKind.ForbiddenNumeric))
                    }

                    if (token.number.float() != 0f
                        && !context.parseMode.allowsUnitlessNumbers()
                        && !allowQuirks.allowed(context.quirksMode)
                    ) {
                        return Err(location.newError(LengthParseErrorKind.UnitlessNumber))
                    }

                    Ok(Length(NoCalcLength.Absolute(AbsoluteLength.Px(token.number.float()))))
                }
                is Token.Function -> {
                    if (!token.name.equals("calc", true)) {
                        return Err(location.newUnexpectedTokenError(token))
                    }

                    input.parseNestedBlock { parser ->
                        CalcNode.parseLength(context, parser, clampingMode)
                            .map(LengthOrPercentageOrAuto::Calc)
                    }
                }
                is Token.Identifier -> {
                    if (!token.name.equals("auto", true)) {
                        return Err(location.newUnexpectedTokenError(token))
                    }

                    Ok(Auto)
                }
                else -> Err(location.newUnexpectedTokenError(token))
            }
        }
    }
}

/**
 * Represents a non negative length, percentage or auto, consisting out of either a concrete value, a percentage, a calc
 * expression or the keyword `auto`.
 */
data class NonNegativeLengthOrPercentageOrAuto(val value: LengthOrPercentageOrAuto) : SpecifiedValue<ComputedNonNegativeLengthOrPercentageOrAuto>, ToCss {

    override fun toComputedValue(context: Context): ComputedNonNegativeLengthOrPercentageOrAuto {
        return ComputedNonNegativeLengthOrPercentageOrAuto(value.toComputedValue(context))
    }

    override fun toCss(writer: Writer) = value.toCss(writer)

    companion object {

        /**
         * Tries to parse a non negative length, percentage or auto, returns [Ok] bearing the length if the parsing is successful,
         * otherwise returns [Err] containing the parse error.
         */
        fun parse(context: ParserContext, input: Parser): Result<NonNegativeLengthOrPercentageOrAuto, ParseError> {
            return LengthOrPercentageOrAuto.parseNonNegative(context, input)
                .map(::NonNegativeLengthOrPercentageOrAuto)
        }

        /**
         * Tries to parse a non negative length, percentage or auto with potential quirks, returns [Ok] bearing the
         * length if the parsing is successful, otherwise returns [Err] containing the parse error.
         */
        fun parseQuirky(
            context: ParserContext,
            input: Parser,
            allowQuirks: AllowQuirks
        ): Result<NonNegativeLengthOrPercentageOrAuto, ParseError> {
            return LengthOrPercentageOrAuto.parseQuirky(context, input, allowQuirks)
                .map(::NonNegativeLengthOrPercentageOrAuto)
        }

        // lazy instance to prevent allocating per call
        private val auto: NonNegativeLengthOrPercentageOrAuto by lazy { NonNegativeLengthOrPercentageOrAuto(LengthOrPercentageOrAuto.Auto) }

        fun auto(): NonNegativeLengthOrPercentageOrAuto {
            return auto
        }
    }
}

/**
 * Represents a length, percentage or none, consisting out of either a concrete value, a percentage, a calc
 * expression or the keyword `none`.
 */
sealed class LengthOrPercentageOrNone : SpecifiedValue<ComputedLengthOrPercentageOrNone>, ToCss {

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

    final override fun toCss(writer: Writer) {
        when (this) {
            is LengthOrPercentageOrNone.Length -> length.toCss(writer)
            is LengthOrPercentageOrNone.Percentage -> percentage.toCss(writer)
            is LengthOrPercentageOrNone.Calc -> calc.toCss(writer)
            is LengthOrPercentageOrNone.None -> writer.append("none")
        }
    }

    companion object {

        /**
         * Tries to parse a length, percentage or none, returns [Ok] bearing the length if the parsing is successful, otherwise
         * returns [Err] containing the parse error.
         */
        fun parse(context: ParserContext, input: Parser): Result<LengthOrPercentageOrNone, ParseError> {
            return parseQuirky(context, input, AllowQuirks.No)
        }

        /**
         * Tries to parse a length, percentage or none with potential quirks, returns [Ok] bearing the length if the parsing is
         * successful, otherwise returns [Err] containing the parse error.
         */
        fun parseQuirky(
            context: ParserContext,
            input: Parser,
            allowQuirks: AllowQuirks
        ): Result<LengthOrPercentageOrNone, ParseError> {
            return parseInternal(context, input, ClampingMode.All, allowQuirks)
        }

        /**
         * Tries to parse a non negative length, percentage or none, returns [Ok] bearing the length if the parsing is successful,
         * otherwise returns [Err] containing the parse error.
         */
        fun parseNonNegative(context: ParserContext, input: Parser): Result<LengthOrPercentageOrNone, ParseError> {
            return parseNonNegativeQuirky(context, input, AllowQuirks.No)
        }

        /**
         * Tries to parse a non negative length, percentage or none with potential quirks, returns [Ok] bearing the
         * length if the parsing is successful, otherwise returns [Err] containing the parse error.
         */
        fun parseNonNegativeQuirky(
            context: ParserContext,
            input: Parser,
            allowQuirks: AllowQuirks
        ): Result<LengthOrPercentageOrNone, ParseError> {
            return parseInternal(context, input, ClampingMode.NonNegative, allowQuirks)
        }

        /**
         * Internal implementation of parsing a length, percentage or none with potential quirks and numeric [ClampingMode],
         * returns [Ok] bearing the length if the parsing is successful, otherwise returns [Err] containing the parse error.
         */
        private fun parseInternal(
            context: ParserContext,
            input: Parser,
            clampingMode: ClampingMode,
            allowQuirks: AllowQuirks
        ): Result<LengthOrPercentageOrNone, ParseError> {
            val location = input.sourceLocation()

            val token = when (val token = input.next()) {
                is Ok -> token.value
                is Err -> return token
            }

            return when (token) {
                is Token.Dimension -> {
                    if (!clampingMode.isAllowed(context.parseMode, token.number.float())) {
                        return Err(location.newError(LengthParseErrorKind.ForbiddenNumeric))
                    }

                    NoCalcLength.parseDimension(context, token.number.float(), token.unit)
                        .map(LengthOrPercentageOrNone::Length)
                        .mapErr { location.newUnexpectedTokenError(token) }
                }
                is Token.Percentage -> {
                    if (!clampingMode.isAllowed(context.parseMode, token.number.float())) {
                        return Err(location.newError(LengthParseErrorKind.ForbiddenNumeric))
                    }

                    Ok(Percentage(Percentage(token.number.float())))
                }
                is Token.Number -> {
                    if (!clampingMode.isAllowed(context.parseMode, token.number.float())) {
                        return Err(location.newError(LengthParseErrorKind.ForbiddenNumeric))
                    }

                    if (token.number.float() != 0f
                        && !context.parseMode.allowsUnitlessNumbers()
                        && !allowQuirks.allowed(context.quirksMode)
                    ) {
                        return Err(location.newError(LengthParseErrorKind.UnitlessNumber))
                    }

                    Ok(Length(NoCalcLength.Absolute(AbsoluteLength.Px(token.number.float()))))
                }
                is Token.Function -> {
                    if (!token.name.equals("calc", true)) {
                        return Err(location.newUnexpectedTokenError(token))
                    }

                    input.parseNestedBlock { parser ->
                        CalcNode.parseLength(context, parser, clampingMode)
                            .map(LengthOrPercentageOrNone::Calc)
                    }
                }
                is Token.Identifier -> {
                    if (!token.name.equals("none", true)) {
                        return Err(location.newUnexpectedTokenError(token))
                    }

                    Ok(None)
                }
                else -> Err(location.newUnexpectedTokenError(token))
            }
        }
    }
}

/**
 * Represents a non negative length, percentage or none, consisting out of either a concrete value, a percentage, a calc
 * expression or the keyword `none`.
 */
data class NonNegativeLengthOrPercentageOrNone(val value: LengthOrPercentageOrNone) : SpecifiedValue<ComputedNonNegativeLengthOrPercentageOrNone>, ToCss {

    override fun toComputedValue(context: Context): ComputedNonNegativeLengthOrPercentageOrNone {
        return ComputedNonNegativeLengthOrPercentageOrNone(value.toComputedValue(context))
    }

    override fun toCss(writer: Writer) = value.toCss(writer)

    companion object {

        /**
         * Tries to parse a non negative length, percentage or none, returns [Ok] bearing the length if the parsing is successful,
         * otherwise returns [Err] containing the parse error.
         */
        fun parse(context: ParserContext, input: Parser): Result<NonNegativeLengthOrPercentageOrNone, ParseError> {
            return LengthOrPercentageOrNone.parseNonNegative(context, input)
                .map(::NonNegativeLengthOrPercentageOrNone)
        }

        /**
         * Tries to parse a non negative length, percentage or none with potential quirks, returns [Ok] bearing the
         * length if the parsing is successful, otherwise returns [Err] containing the parse error.
         */
        fun parseQuirky(
            context: ParserContext,
            input: Parser,
            allowQuirks: AllowQuirks
        ): Result<NonNegativeLengthOrPercentageOrNone, ParseError> {
            return LengthOrPercentageOrNone.parseQuirky(context, input, allowQuirks)
                .map(::NonNegativeLengthOrPercentageOrNone)
        }
    }
}
